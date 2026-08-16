using System;
using System.Collections.Generic;
using MelonLoader;
using UnityEngine;

namespace OperatorEcotiThermal
{
    /// <summary>One tracked hostile, with the components resolved once and reused.</summary>
    internal sealed class TrackedEnemy
    {
        public Transform Transform;
        public Renderer Renderer;
        public object Health;
        public int TeamId;

        /// <summary>
        /// World-space box around the body. Renderer bounds are used when available because they
        /// follow posture for free — a prone bot gives a flat wide box, a standing one a tall
        /// narrow box — where a fixed capsule off the root transform would not. Falls back to a
        /// nominal standing-height box when no renderer resolved.
        /// </summary>
        public bool TryGetBounds(out Bounds bounds)
        {
            bounds = default;

            if (Renderer != null)
            {
                try
                {
                    // A wrapper that passes != null can still be a destroyed native object, so the
                    // read is guarded and a throw simply drops this enemy for the frame.
                    bounds = Renderer.bounds;
                    if (bounds.size.sqrMagnitude > 0.01f) return true;
                }
                catch { return false; }
            }

            if (Transform == null) return false;

            try
            {
                var p = Transform.position;
                bounds = new Bounds(p + new Vector3(0f, 0.9f, 0f), new Vector3(0.6f, 1.8f, 0.6f));
                return true;
            }
            catch { return false; }
        }
    }

    /// <summary>
    /// Maintains the list of live hostiles.
    ///
    /// BrainAI.Awake joins every bot to the static BrainAI.AllBrainAI registry and BrainAI.OnDestroy
    /// removes it, so the registry is the cheap enumeration path and there is never a reason to call
    /// Resources.FindObjectsOfTypeAll here. The toolkit is explicit that scanning the object table
    /// per frame costs most of the frame rate.
    ///
    /// The registry walk still resolves components, so it runs on a throttle (default 250 ms) while
    /// positions come off the cached transforms every frame. The rebuild always advances its
    /// timestamp even when it finds nothing, because the field notes call out the early-return-that-
    /// skips-the-done-flag bug: a scene where the target never appears otherwise rescans forever.
    ///
    /// Note this only ever sees AI. Bots are the enemy in Lone Wolf, and reading another player's
    /// state is not something a client-side mod can do anyway — the architecture reference is clear
    /// that bot and player state is server-owned and only presentation SyncVars replicate.
    /// </summary>
    internal static class EnemyTracker
    {
        private static Type _brainAi;
        private static Type _playerMaster;
        private static bool _typesResolved;

        private static readonly List<TrackedEnemy> _enemies = new List<TrackedEnemy>();
        private static float _lastRebuild = -999f;
        private static int _localTeam = int.MinValue;

        public static IReadOnlyList<TrackedEnemy> Enemies => _enemies;
        public static int Count => _enemies.Count;

        public static void Reset()
        {
            _enemies.Clear();
            _lastRebuild = -999f;
            _localTeam = int.MinValue;
        }

        private static void ResolveTypes()
        {
            if (_typesResolved) return;
            _typesResolved = true;

            // BrainAI is the shipping brain. BrainAIV2 is a two-method NavMeshAgent prototype that
            // never runs — the toolkit warns that targeting the newest-sounding class binds to a
            // corpse, and the V-suffixes record history rather than intent.
            _brainAi = Probe.FindType("Il2Cpp.BrainAI") ?? Probe.FindType("BrainAI");
            _playerMaster = Probe.FindType("Il2Cpp.PlayerMaster") ?? Probe.FindType("PlayerMaster");

            if (_brainAi == null)
                MelonLogger.Warning("[EcotiThermal] BrainAI not resolved — no enemies will be tracked.");
        }

        /// <summary>Rebuild the tracked list if the throttle has elapsed.</summary>
        public static void Tick(float rebuildIntervalSeconds)
        {
            ResolveTypes();
            if (_brainAi == null) return;

            if (Time.unscaledTime - _lastRebuild < rebuildIntervalSeconds) return;
            _lastRebuild = Time.unscaledTime;   // advanced before any early return below

            try { Rebuild(); }
            catch (Exception e)
            {
                MelonLogger.Warning($"[EcotiThermal] enemy rebuild failed: {e.Message}");
                _enemies.Clear();
            }
        }

        private static void Rebuild()
        {
            _enemies.Clear();
            ResolveLocalTeam();

            var registry = Probe.Static<object>(_brainAi, "AllBrainAI");
            if (registry == null) return;

            var count = Probe.Instance(registry, "Count", 0);
            for (int i = 0; i < count; i++)
            {
                var brain = Probe.Call<object>(registry, "get_Item", null, i)
                            ?? Probe.Call<object>(registry, "Item", null, i);
                if (brain == null) continue;

                var entry = BuildEntry(brain);
                if (entry != null) _enemies.Add(entry);
            }
        }

        private static TrackedEnemy BuildEntry(object brain)
        {
            // BrainAI.Awake caches health and the team back-pointer, so both are already resolved
            // on the brain and there is no need to walk the GameObject for them.
            var health = Probe.Instance<object>(brain, "health");
            if (health != null && Probe.Instance(health, "isDead", false)) return null;

            var component = brain as Component;
            if (component == null) return null;

            Transform tf;
            Renderer rend = null;
            try
            {
                tf = component.transform;
                if (tf == null) return null;

                // SkinnedMeshRenderer on the body. Bots are ordinary networked GameObjects, unlike
                // the BRG-drawn vegetation and props which have no GameObject or Renderer at all.
                rend = component.GetComponentInChildren<SkinnedMeshRenderer>();
                if (rend == null) rend = component.GetComponentInChildren<Renderer>();
            }
            catch { return null; }

            var team = ReadTeam(brain);
            if (_localTeam != int.MinValue && team != int.MinValue && team == _localTeam) return null;

            return new TrackedEnemy { Transform = tf, Renderer = rend, Health = health, TeamId = team };
        }

        private static int ReadTeam(object brain)
        {
            // TeamIdentifier.TeamID is the shared int-keyed friend/foe model for AI and players.
            // BrainAI reaches it through MyTeamIdentifierReference; TeamId is also exposed directly.
            var direct = Probe.Instance(brain, "TeamId", int.MinValue);
            if (direct != int.MinValue) return direct;

            var reference = Probe.Instance<object>(brain, "MyTeamIdentifierReference");
            if (reference == null) return int.MinValue;

            var identifier = Probe.Instance<object>(reference, "teamIdentifier")
                             ?? Probe.Instance<object>(reference, "TeamIdentifier");
            if (identifier == null) return int.MinValue;

            return Probe.Instance(identifier, "TeamID", int.MinValue);
        }

        /// <summary>
        /// Resolve the local player's team so friendlies are skipped. When it cannot be resolved
        /// every AI is treated as hostile, which is the right answer in Lone Wolf where the bots
        /// are the opposition, and the team filter simply becomes a no-op.
        /// </summary>
        private static void ResolveLocalTeam()
        {
            if (_localTeam != int.MinValue || _playerMaster == null) return;

            var me = Probe.Static<object>(_playerMaster, "MyPlayerMaster");
            if (me == null) return;

            var component = me as Component;
            if (component == null) return;

            try
            {
                var identifier = Probe.FindComponent(component, "TeamIdentifier");
                if (identifier == null) return;

                var team = Probe.Instance(identifier, "TeamID", int.MinValue);
                if (team == int.MinValue) return;

                _localTeam = team;
                MelonLogger.Msg($"[EcotiThermal] local team resolved: {team}");
            }
            catch { /* leave unresolved; all AI stay hostile */ }
        }
    }
}
