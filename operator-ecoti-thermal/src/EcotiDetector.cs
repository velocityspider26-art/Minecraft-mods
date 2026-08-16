using System;
using System.Text;
using MelonLoader;
using UnityEngine;

namespace OperatorEcotiThermal
{
    /// <summary>
    /// Detects whether an ECOTI clip-on is mounted.
    ///
    /// This is the least certain part of the mod, and it is worth being straight about why. The
    /// toolkit documents the character system thoroughly — CharacterCustomisation holds the worn
    /// kit, modParents carries the child and attachment sockets including night vision mounts, and
    /// an item is a CharacterMod identified by ModInfo.ModName — but it does not name the ECOTI
    /// item specifically. So the item name is matched as a configurable substring rather than
    /// hardcoded, and the dump below exists to find the real one on your build.
    ///
    /// The field notes prescribe exactly this: when a filter returns nothing, stop theorising and
    /// bind a log-only inventory dump to a key. Dump() prints every socket and every equipped mod
    /// name regardless of the filter, because gating the diagnostic on the suspect condition
    /// defeats the point.
    ///
    /// Result is tri-state. Unknown is treated as "not present" by the gate when RequireEcoti is
    /// on, so an unresolved detector fails closed like everything else.
    /// </summary>
    internal static class EcotiDetector
    {
        public enum State { Unknown, Absent, Present }

        private static Type _playerMaster;
        private static bool _typeResolved;
        private static bool _loggedUnresolved;

        private static State _state = State.Unknown;
        private static float _lastCheck = -999f;

        public static State Current => _state;

        public static void Reset()
        {
            _state = State.Unknown;
            _lastCheck = -999f;
        }

        private static void ResolveTypes()
        {
            if (_typeResolved) return;
            _typeResolved = true;
            _playerMaster = Probe.FindType("Il2Cpp.PlayerMaster") ?? Probe.FindType("PlayerMaster");
        }

        /// <summary>Re-check on a throttle. Kit only changes in the armoury, so this is cheap.</summary>
        public static void Tick(string needle, float intervalSeconds = 1f)
        {
            if (Time.unscaledTime - _lastCheck < intervalSeconds) return;
            _lastCheck = Time.unscaledTime;

            var customisation = FindLocalCustomisation();
            if (customisation == null)
            {
                _state = State.Unknown;
                if (!_loggedUnresolved)
                {
                    _loggedUnresolved = true;
                    MelonLogger.Warning(
                        "[EcotiThermal] could not reach the local CharacterCustomisation. ECOTI state is unknown; "
                      + "press the dump key in a mission, or set RequireEcoti=false.");
                }
                return;
            }

            _state = ContainsMod(customisation, needle) ? State.Present : State.Absent;
        }

        /// <summary>
        /// Walk the local player's attachment sockets looking for a name match.
        ///
        /// modParents holds only child and attachment sockets, never the top-level garments — the
        /// field notes call out that searching a collection which structurally cannot hold the
        /// target is a classic wasted rewrite here. An NVG clip-on is a child mod, so the socket
        /// list is the right place for it.
        /// </summary>
        private static bool ContainsMod(object customisation, string needle)
        {
            if (string.IsNullOrEmpty(needle)) return false;

            try
            {
                var parents = Probe.Instance<object>(customisation, "modParents");
                if (parents == null) return false;

                var count = Probe.Instance(parents, "Count", 0);
                for (int i = 0; i < count; i++)
                {
                    var socket = Probe.Call<object>(parents, "get_Item", null, i);
                    if (socket == null) continue;

                    if (SocketHasMatch(socket, needle)) return true;
                }
            }
            catch (Exception e)
            {
                MelonLogger.Warning($"[EcotiThermal] ECOTI scan failed: {e.Message}");
            }

            return false;
        }

        private static bool SocketHasMatch(object socket, string needle)
        {
            var mod = Probe.Instance<object>(socket, "currentMod")
                      ?? Probe.Instance<object>(socket, "CurrentMod")
                      ?? Probe.Instance<object>(socket, "spawnedMod");

            if (mod != null && NameOf(mod).IndexOf(needle, StringComparison.OrdinalIgnoreCase) >= 0) return true;

            // Some sockets expose their occupant only through the child GameObject.
            var component = socket as Component;
            if (component == null) return false;

            try
            {
                var tf = component.transform;
                for (int c = 0; c < tf.childCount; c++)
                {
                    var child = tf.GetChild(c);
                    if (child == null) continue;
                    if (child.name.IndexOf(needle, StringComparison.OrdinalIgnoreCase) >= 0) return true;
                }
            }
            catch { /* destroyed mid-walk */ }

            return false;
        }

        private static string NameOf(object mod)
        {
            var info = Probe.Instance<object>(mod, "ModInfo");
            if (info != null)
            {
                var name = Probe.Instance<string>(info, "ModName");
                if (!string.IsNullOrEmpty(name)) return name;
            }

            var obj = mod as UnityEngine.Object;
            return obj != null ? obj.name : string.Empty;
        }

        private static object FindLocalCustomisation()
        {
            ResolveTypes();
            if (_playerMaster == null) return null;

            try
            {
                var me = Probe.Static<object>(_playerMaster, "MyPlayerMaster");
                var component = me as Component;
                if (component == null) return null;

                // PlayerMaster and the avatar are separate networked objects, so the customisation
                // may hang off either the master or the spawned PlayerNetworking below it.
                var found = Probe.FindComponent(component, "CharacterCustomisation");
                if (found != null) return found;

                var spawned = Probe.Instance<object>(me, "playerNetworking")
                              ?? Probe.Instance<object>(me, "PlayerNetworking")
                              ?? Probe.Instance<object>(me, "myPlayerNetworking");

                if (spawned != null) return Probe.FindComponent(spawned, "CharacterCustomisation");
            }
            catch { /* fall through */ }

            return null;
        }

        /// <summary>
        /// Log-only inventory dump. Prints every socket and its occupant plus the NVG statics, so
        /// the real ECOTI item name can be read off and put in EcotiNameContains. Deliberately
        /// unfiltered — dump everything, then look for the correlation.
        /// </summary>
        public static void Dump()
        {
            MelonLogger.Msg("──── EcotiThermal dump ────");
            MelonLogger.Msg($"NVG: {NvgProbe.Describe()}  (AnyActive={NvgProbe.NvgActive}, LensCount={NvgProbe.LensCount})");
            MelonLogger.Msg($"Session: {SessionGate.Reason}");
            MelonLogger.Msg($"Tracked hostiles: {EnemyTracker.Count}");

            var customisation = FindLocalCustomisation();
            if (customisation == null)
            {
                MelonLogger.Msg("CharacterCustomisation: NOT REACHED (are you in a mission and spawned?)");
                MelonLogger.Msg("───────────────────────────");
                return;
            }

            // Top-level garments are direct properties, not entries in modParents.
            var garments = new[]
            {
                "HeadGear", "EyeWear", "EarPro", "Face", "Voice", "Shirt", "Gloves",
                "Wrist", "Pants", "Shoes", "Vest", "Belt", "Backpack", "Tattoo"
            };

            var sb = new StringBuilder();
            foreach (var slot in garments)
            {
                var item = Probe.Instance<object>(customisation, slot);
                sb.AppendLine($"  garment {slot,-10} = {(item == null ? "<empty>" : NameOf(item))}");
            }
            MelonLogger.Msg("Garments (direct properties):\n" + sb);

            try
            {
                var parents = Probe.Instance<object>(customisation, "modParents");
                var count = Probe.Instance(parents, "Count", 0);
                MelonLogger.Msg($"Sockets (modParents, {count} entries):");

                for (int i = 0; i < count; i++)
                {
                    var socket = Probe.Call<object>(parents, "get_Item", null, i);
                    if (socket == null) { MelonLogger.Msg($"  [{i}] <null>"); continue; }

                    var socketName = (socket as UnityEngine.Object)?.name ?? "<unnamed>";
                    var mod = Probe.Instance<object>(socket, "currentMod")
                              ?? Probe.Instance<object>(socket, "CurrentMod")
                              ?? Probe.Instance<object>(socket, "spawnedMod");

                    MelonLogger.Msg($"  [{i}] socket '{socketName}' -> {(mod == null ? "<empty>" : NameOf(mod))}");

                    var component = socket as Component;
                    if (component == null) continue;

                    var tf = component.transform;
                    for (int c = 0; c < tf.childCount; c++)
                        MelonLogger.Msg($"         child: {tf.GetChild(c).name}");
                }
            }
            catch (Exception e) { MelonLogger.Error($"socket dump failed: {e.Message}"); }

            MelonLogger.Msg("───────────────────────────");
        }
    }
}
