using System;
using System.Collections.Generic;
using MelonLoader;

[assembly: MelonInfo(typeof(OperatorEcotiThermal.Reflect.Main), "ECOTI Thermal Overlay", "1.0.0-reflect", "velocityspider26-art")]
[assembly: MelonGame(null, null)]

namespace OperatorEcotiThermal.Reflect
{
    /// <summary>Quad-tube state off the NvgLensFollower statics.</summary>
    internal static class NvgProbe
    {
        private static Type _lensFollower;
        private static bool _resolved;

        public static int LensCount { get; private set; }
        public static bool NvgActive { get; private set; }

        public static void Sample()
        {
            if (!_resolved)
            {
                _resolved = true;
                _lensFollower = Probe.FindType("Il2Cpp.NvgLensFollower") ?? Probe.FindType("NvgLensFollower");
            }

            if (_lensFollower == null) { NvgActive = false; LensCount = 0; return; }

            NvgActive = Probe.Static(_lensFollower, "AnyActive", false);
            LensCount = NvgActive ? Probe.Static(_lensFollower, "LensCount", 0) : 0;
        }

        public static bool IsQuadTube(int required) => NvgActive && LensCount >= required;

        public static string Describe()
        {
            if (_lensFollower == null) return "NvgLensFollower not resolved";
            if (!NvgActive) return "NVGs up or unpowered";
            return $"NVGs down, {LensCount} lens{(LensCount == 1 ? "" : "es")}";
        }
    }

    /// <summary>ECOTI clip-on detection plus the unfiltered kit dump.</summary>
    internal static class EcotiDetector
    {
        public enum State { Unknown, Absent, Present }

        private static Type _playerMaster;
        private static bool _resolved;
        private static bool _loggedUnreached;
        private static float _lastCheck = -999f;

        public static State Current { get; private set; } = State.Unknown;

        public static void Reset() { Current = State.Unknown; _lastCheck = -999f; }

        private static object LocalCustomisation()
        {
            if (!_resolved)
            {
                _resolved = true;
                _playerMaster = Probe.FindType("Il2Cpp.PlayerMaster") ?? Probe.FindType("PlayerMaster");
            }
            if (_playerMaster == null) return null;

            var me = Probe.Static<object>(_playerMaster, "MyPlayerMaster");
            if (me == null) return null;

            var found = Probe.FindComponent(me, "CharacterCustomisation");
            if (found != null) return found;

            var spawned = Probe.Instance<object>(me, "playerNetworking")
                          ?? Probe.Instance<object>(me, "PlayerNetworking")
                          ?? Probe.Instance<object>(me, "myPlayerNetworking");

            return spawned != null ? Probe.FindComponent(spawned, "CharacterCustomisation") : null;
        }

        public static void Tick(string needle, float interval = 1f)
        {
            if (U.UnscaledTime - _lastCheck < interval) return;
            _lastCheck = U.UnscaledTime;

            var customisation = LocalCustomisation();
            if (customisation == null)
            {
                Current = State.Unknown;
                if (!_loggedUnreached)
                {
                    _loggedUnreached = true;
                    MelonLogger.Warning("[EcotiThermal] CharacterCustomisation not reached; ECOTI state unknown. "
                                      + "Press the dump key in a mission, or set RequireEcoti=false.");
                }
                return;
            }

            Current = HasMod(customisation, needle) ? State.Present : State.Absent;
        }

        private static bool HasMod(object customisation, string needle)
        {
            if (string.IsNullOrEmpty(needle)) return false;

            var parents = Probe.Instance<object>(customisation, "modParents");
            if (parents == null) return false;

            var count = Probe.Instance(parents, "Count", 0);
            for (int i = 0; i < count; i++)
            {
                var socket = Probe.Call<object>(parents, "get_Item", null, i);
                if (socket == null) continue;

                var mod = Probe.Instance<object>(socket, "currentMod")
                          ?? Probe.Instance<object>(socket, "CurrentMod")
                          ?? Probe.Instance<object>(socket, "spawnedMod");

                if (mod != null && ModName(mod).IndexOf(needle, StringComparison.OrdinalIgnoreCase) >= 0)
                    return true;

                var tf = U.TransformOf(socket);
                var children = U.ChildCount(tf);
                for (int c = 0; c < children; c++)
                {
                    var child = U.GetChild(tf, c);
                    if (child == null) continue;
                    if (U.NameOf(child).IndexOf(needle, StringComparison.OrdinalIgnoreCase) >= 0) return true;
                }
            }

            return false;
        }

        private static string ModName(object mod)
        {
            var info = Probe.Instance<object>(mod, "ModInfo");
            if (info != null)
            {
                var n = Probe.Instance<string>(info, "ModName");
                if (!string.IsNullOrEmpty(n)) return n;
            }
            return U.NameOf(mod);
        }

        /// <summary>Unfiltered dump. Gating a diagnostic on the suspect condition defeats it.</summary>
        public static void Dump()
        {
            MelonLogger.Msg("──── EcotiThermal dump ────");
            MelonLogger.Msg($"NVG: {NvgProbe.Describe()} (AnyActive={NvgProbe.NvgActive}, LensCount={NvgProbe.LensCount})");
            MelonLogger.Msg($"Session: {SessionGate.Reason}");
            MelonLogger.Msg($"Tracked hostiles: {EnemyTracker.Count}");
            U.SelfTest();

            var customisation = LocalCustomisation();
            if (customisation == null)
            {
                MelonLogger.Msg("CharacterCustomisation: NOT REACHED (in a mission and spawned?)");
                MelonLogger.Msg("───────────────────────────");
                return;
            }

            foreach (var slot in new[] { "HeadGear", "EyeWear", "EarPro", "Face", "Shirt", "Gloves",
                                         "Wrist", "Pants", "Shoes", "Vest", "Belt", "Backpack" })
            {
                var item = Probe.Instance<object>(customisation, slot);
                MelonLogger.Msg($"  garment {slot,-10} = {(item == null ? "<empty>" : ModName(item))}");
            }

            var parents = Probe.Instance<object>(customisation, "modParents");
            var count = Probe.Instance(parents, "Count", 0);
            MelonLogger.Msg($"Sockets (modParents, {count}):");

            for (int i = 0; i < count; i++)
            {
                var socket = Probe.Call<object>(parents, "get_Item", null, i);
                if (socket == null) { MelonLogger.Msg($"  [{i}] <null>"); continue; }

                var mod = Probe.Instance<object>(socket, "currentMod")
                          ?? Probe.Instance<object>(socket, "CurrentMod")
                          ?? Probe.Instance<object>(socket, "spawnedMod");

                MelonLogger.Msg($"  [{i}] '{U.NameOf(socket)}' -> {(mod == null ? "<empty>" : ModName(mod))}");

                var tf = U.TransformOf(socket);
                for (int c = 0; c < U.ChildCount(tf); c++)
                    MelonLogger.Msg($"        child: {U.NameOf(U.GetChild(tf, c))}");
            }

            MelonLogger.Msg("───────────────────────────");
        }
    }

    internal sealed class Enemy
    {
        public object Transform;
        public object Renderer;
    }

    /// <summary>Walks the BrainAI.AllBrainAI registry on a throttle.</summary>
    internal static class EnemyTracker
    {
        private static Type _brainAi, _playerMaster;
        private static bool _resolved;
        private static readonly List<Enemy> _enemies = new List<Enemy>();
        private static float _lastRebuild = -999f;
        private static int _localTeam = int.MinValue;

        public static IReadOnlyList<Enemy> Enemies => _enemies;
        public static int Count => _enemies.Count;

        public static void Reset() { _enemies.Clear(); _lastRebuild = -999f; _localTeam = int.MinValue; }

        public static void Tick(float interval)
        {
            if (!_resolved)
            {
                _resolved = true;
                _brainAi = Probe.FindType("Il2Cpp.BrainAI") ?? Probe.FindType("BrainAI");
                _playerMaster = Probe.FindType("Il2Cpp.PlayerMaster") ?? Probe.FindType("PlayerMaster");
                if (_brainAi == null) MelonLogger.Warning("[EcotiThermal] BrainAI not resolved — no enemies tracked.");
            }
            if (_brainAi == null) return;

            if (U.UnscaledTime - _lastRebuild < interval) return;
            _lastRebuild = U.UnscaledTime;   // advanced before any early return

            try { Rebuild(); }
            catch (Exception e) { MelonLogger.Warning($"[EcotiThermal] rebuild failed: {e.Message}"); _enemies.Clear(); }
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
                var brain = Probe.Call<object>(registry, "get_Item", null, i);
                if (brain == null) continue;

                var health = Probe.Instance<object>(brain, "health");
                if (health != null && Probe.Instance(health, "isDead", false)) continue;

                var team = ReadTeam(brain);
                if (_localTeam != int.MinValue && team != int.MinValue && team == _localTeam) continue;

                var tf = U.TransformOf(brain);
                if (tf == null) continue;

                var rend = U.FindComponentInChildren(brain, "SkinnedMeshRenderer");
                _enemies.Add(new Enemy { Transform = tf, Renderer = rend });
            }
        }

        private static int ReadTeam(object brain)
        {
            var direct = Probe.Instance(brain, "TeamId", int.MinValue);
            if (direct != int.MinValue) return direct;

            var reference = Probe.Instance<object>(brain, "MyTeamIdentifierReference");
            if (reference == null) return int.MinValue;

            // The toolkit describes TeamIdentifierReference as a back-pointer wrapper, but whether
            // MyTeamIdentifierReference hands back the wrapper or the TeamIdentifier itself is not
            // documented. Try TeamID directly first, then unwrap — one of the two is right and this
            // costs a single failed lookup either way.
            var direct2 = Probe.Instance(reference, "TeamID", int.MinValue);
            if (direct2 != int.MinValue) return direct2;

            var identifier = Probe.Instance<object>(reference, "teamIdentifier")
                             ?? Probe.Instance<object>(reference, "TeamIdentifier");
            return identifier == null ? int.MinValue : Probe.Instance(identifier, "TeamID", int.MinValue);
        }

        private static void ResolveLocalTeam()
        {
            if (_localTeam != int.MinValue || _playerMaster == null) return;

            var me = Probe.Static<object>(_playerMaster, "MyPlayerMaster");
            if (me == null) return;

            var identifier = Probe.FindComponent(me, "TeamIdentifier");
            if (identifier == null) return;

            var team = Probe.Instance(identifier, "TeamID", int.MinValue);
            if (team == int.MinValue) return;

            _localTeam = team;
            MelonLogger.Msg($"[EcotiThermal] local team resolved: {team}");
        }
    }

    /// <summary>
    /// Screen-space boxes.
    ///
    /// Two projections per enemy (feet and head) rather than eight AABB corners. Under reflection
    /// every projection costs a boxed Vector3 construction, an Invoke and three field reads, so
    /// eight corners per enemy per frame would be roughly four times the cost for a box that is
    /// only slightly better. Width is derived from projected height instead.
    /// </summary>
    internal static class Overlay
    {
        private static object _camera;
        private static float _lastLookup = -999f;

        public static void Reset() { _camera = null; _lastLookup = -999f; }

        private static object Camera()
        {
            if (_camera != null) return _camera;
            if (U.UnscaledTime - _lastLookup < 1f) return null;
            _lastLookup = U.UnscaledTime;

            var singletonType = Probe.FindType("Il2Cpp.MainCameraSingleton") ?? Probe.FindType("MainCameraSingleton");
            if (singletonType != null)
            {
                var instance = Probe.Static<object>(singletonType, "instance");
                if (instance != null)
                {
                    _camera = Probe.Instance<object>(instance, "mainCamera")
                              ?? Probe.Instance<object>(instance, "MainCamera")
                              ?? Probe.Instance<object>(instance, "camera")
                              ?? U.FindComponentInChildren(instance, "Camera");
                    if (_camera != null) return _camera;
                }
            }

            _camera = U.CameraMain();
            return _camera;
        }

        public static void Draw(Options opt)
        {
            var cam = Camera();
            if (cam == null) return;

            if (!U.PositionOf(U.TransformOf(cam), out var ox, out var oy, out var oz)) return;

            var maxRange = opt.MaxRange.Value;
            var maxRangeSq = maxRange * maxRange;
            var thickness = Math.Max(1f, opt.OutlineThickness.Value);
            var screenH = U.ScreenHeight;
            var screenW = U.ScreenWidth;

            float r = opt.OutlineR.Value, g = opt.OutlineG.Value, b = opt.OutlineB.Value;

            foreach (var enemy in EnemyTracker.Enemies)
            {
                float fx, fy, fz, height;

                if (U.RendererBounds(enemy.Renderer, out var cx, out var cy, out var cz,
                                     out _, out var sy, out _))
                {
                    fx = cx; fy = cy - sy * 0.5f; fz = cz;
                    height = sy;
                }
                else
                {
                    if (!U.PositionOf(enemy.Transform, out fx, out fy, out fz)) continue;
                    height = 1.8f;
                }

                var dx = fx - ox; var dy = fy - oy; var dz = fz - oz;
                var distSq = dx * dx + dy * dy + dz * dz;
                if (distSq > maxRangeSq) continue;

                if (!U.WorldToScreen(cam, fx, fy, fz, out var bxs, out var bys, out var bzs)) continue;
                if (bzs <= 0f) continue;
                if (!U.WorldToScreen(cam, fx, fy + height, fz, out _, out var tys, out var tzs)) continue;
                if (tzs <= 0f) continue;

                var top = screenH - tys;
                var bottom = screenH - bys;
                var h = bottom - top;
                if (h < 2f) continue;

                var w = h * 0.45f;
                var left = bxs - w * 0.5f;

                if (left + w < 0f || left > screenW || bottom < 0f || top > screenH) continue;

                var fade = opt.DistanceFade.Value
                    ? 1f - 0.65f * Math.Min(1f, (float)Math.Sqrt(distSq) / maxRange)
                    : 1f;

                if (opt.FillOpacity.Value > 0.001f)
                {
                    U.SetColor(r, g, b, opt.FillOpacity.Value * fade);
                    U.FillRect(left, top, w, h);
                }

                U.SetColor(r, g, b, fade);
                U.FillRect(left, top, w, thickness);
                U.FillRect(left, bottom - thickness, w, thickness);
                U.FillRect(left, top, thickness, h);
                U.FillRect(left + w - thickness, top, thickness, h);
            }

            U.SetColor(1f, 1f, 1f, 1f);
        }

        public static void DrawStatus(string line)
        {
            U.SetColor(0f, 0f, 0f, 0.55f);
            U.FillRect(8f, 8f, 540f, 22f);
            U.SetColor(1f, 1f, 1f, 1f);
            U.Label(14f, 10f, 530f, 20f, line);
        }
    }

    internal sealed class Options
    {
        public MelonPreferences_Entry<bool> Enabled, RequireQuadTube, RequireEcoti, DistanceFade, ShowStatusHud;
        public MelonPreferences_Entry<int> RequiredLenses, ToggleKey, StatusKey, DumpKey;
        public MelonPreferences_Entry<string> EcotiNameContains;
        public MelonPreferences_Entry<float> MaxRange, OutlineR, OutlineG, OutlineB, OutlineThickness,
                                             FillOpacity, RebuildInterval;

        public static Options Load()
        {
            var o = new Options();
            var c = MelonPreferences.CreateCategory("EcotiThermal", "ECOTI Thermal Overlay");

            o.Enabled           = c.CreateEntry("Enabled", true);
            o.RequireQuadTube   = c.CreateEntry("RequireQuadTube", true);
            o.RequiredLenses    = c.CreateEntry("RequiredLenses", 4);
            o.RequireEcoti      = c.CreateEntry("RequireEcoti", true);
            o.EcotiNameContains = c.CreateEntry("EcotiNameContains", "ecoti");
            o.MaxRange          = c.CreateEntry("MaxRange", 250f);
            o.OutlineR          = c.CreateEntry("OutlineR", 1f);
            o.OutlineG          = c.CreateEntry("OutlineG", 0.12f);
            o.OutlineB          = c.CreateEntry("OutlineB", 0.12f);
            o.OutlineThickness  = c.CreateEntry("OutlineThickness", 1.5f);
            o.FillOpacity       = c.CreateEntry("FillOpacity", 0.10f);
            o.DistanceFade      = c.CreateEntry("DistanceFade", true);
            o.RebuildInterval   = c.CreateEntry("RebuildInterval", 0.25f);
            o.ShowStatusHud     = c.CreateEntry("ShowStatusHud", true);
            o.ToggleKey         = c.CreateEntry("ToggleKey", 286);   // F7
            o.StatusKey         = c.CreateEntry("StatusKey", 287);   // F8
            o.DumpKey           = c.CreateEntry("DumpKey", 288);     // F9

            return o;
        }
    }

    /// <summary>
    /// Reflection-only build of the ECOTI Thermal Overlay.
    ///
    /// Functionally identical to the standard build; the difference is that every Unity call goes
    /// through U by reflection so the assembly links against MelonLoader alone and can be compiled
    /// without the game present. SessionGate is byte-identical between the two builds — the solo
    /// gate is the same code either way.
    /// </summary>
    public class Main : MelonMod
    {
        private Options _opt;
        private bool _toggle = true;
        private bool _active;
        private string _status = "starting";
        private bool _selfTested;
        private readonly System.Diagnostics.Stopwatch _clock = new System.Diagnostics.Stopwatch();

        public override void OnInitializeMelon()
        {
            _opt = Options.Load();
            _clock.Start();

            LoggerInstance.Msg("=== ECOTI Thermal Overlay (reflection build) INITIALISED ===");
            LoggerInstance.Msg("  If you can read this line, the melon loaded correctly.");
            LoggerInstance.Msg("  F7 toggle · F8 status HUD · F9 dump + reflection self-test");
            LoggerInstance.Msg("  Solo sessions only. Off whenever another player is connected.");
        }

        public override void OnSceneWasLoaded(int buildIndex, string sceneName)
        {
            EnemyTracker.Reset();
            EcotiDetector.Reset();
            Overlay.Reset();
        }

        public override void OnUpdate()
        {
            if (_opt == null) return;

            if (!U.CoreReady)
            {
                // Unity's assemblies are not loaded at melon init, so resolution is retried until
                // it takes, then reported once.
                if (!U.Init())
                {
                    // If it is still failing well after startup, that is not "too early" any more —
                    // it is the failure mode. Report it once, unprompted, so the log carries the
                    // diagnosis without the user having to know about the dump key.
                    //
                    // Timed off a managed stopwatch, not U.UnscaledTime: when Unity has not resolved
                    // that property returns its fallback and never advances, so a Unity-based timer
                    // would guarantee this branch never fires in exactly the case it exists for.
                    if (!_selfTested && _clock.Elapsed.TotalSeconds > 10.0)
                    {
                        _selfTested = true;
                        LoggerInstance.Error("Unity reflection did not resolve after 10s. Self-test follows; "
                                           + "send this whole block if you are reporting it.");
                        U.SelfTest();
                    }
                    return;
                }

                if (!_selfTested) { _selfTested = true; U.SelfTest(); }
            }

            try
            {
                if (U.GetKeyDown(_opt.ToggleKey.Value))
                {
                    _toggle = !_toggle;
                    LoggerInstance.Msg($"overlay {(_toggle ? "enabled" : "disabled")}");
                }
                if (U.GetKeyDown(_opt.StatusKey.Value)) _opt.ShowStatusHud.Value = !_opt.ShowStatusHud.Value;
                if (U.GetKeyDown(_opt.DumpKey.Value)) EcotiDetector.Dump();
            }
            catch (Exception e) { LoggerInstance.Error($"input failed: {e.Message}"); }

            try { _active = Gates(out _status); }
            catch (Exception e)
            {
                _active = false;
                _status = "gate evaluation threw, overlay off";
                LoggerInstance.Error($"gates failed: {e.Message}");
            }

            if (_active)
            {
                try { EnemyTracker.Tick(Math.Max(0.05f, _opt.RebuildInterval.Value)); }
                catch (Exception e) { LoggerInstance.Error($"tracker failed: {e.Message}"); }
            }
        }

        private bool Gates(out string status)
        {
            if (!_opt.Enabled.Value) { status = "disabled in config"; return false; }
            if (!_toggle) { status = "toggled off (F7)"; return false; }

            if (!SessionGate.IsSolo()) { status = "SOLO ONLY — " + SessionGate.Reason; return false; }

            NvgProbe.Sample();

            if (_opt.RequireQuadTube.Value)
            {
                if (!NvgProbe.IsQuadTube(_opt.RequiredLenses.Value))
                {
                    status = $"waiting on quad tubes — {NvgProbe.Describe()}";
                    return false;
                }
            }
            else if (!NvgProbe.NvgActive)
            {
                status = $"waiting on NVGs — {NvgProbe.Describe()}";
                return false;
            }

            if (_opt.RequireEcoti.Value)
            {
                EcotiDetector.Tick(_opt.EcotiNameContains.Value);
                if (EcotiDetector.Current != EcotiDetector.State.Present)
                {
                    status = EcotiDetector.Current == EcotiDetector.State.Unknown
                        ? "ECOTI state unknown — press F9 to dump kit names"
                        : "ECOTI not mounted";
                    return false;
                }
            }

            status = $"active — {EnemyTracker.Count} contact(s), {NvgProbe.Describe()}";
            return true;
        }

        public override void OnGUI()
        {
            if (_opt == null || !U.CanDraw) return;

            try
            {
                if (_opt.ShowStatusHud.Value) Overlay.DrawStatus("ECOTI: " + _status);
                if (_active) Overlay.Draw(_opt);
            }
            catch (Exception e) { LoggerInstance.Error($"draw failed: {e.Message}"); }
        }
    }
}
