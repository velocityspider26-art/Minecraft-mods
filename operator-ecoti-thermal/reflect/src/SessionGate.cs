using System;
using MelonLoader;

namespace OperatorEcotiThermal.Reflect
{
    /// <summary>
    /// Decides whether this session is a solo one.
    ///
    /// This is the gate that keeps the mod a single-player mod. OPERATOR runs Lone Wolf, co-op PvE
    /// and PvP "Force on Force" out of one client binary over one Mirror stack, so nothing about
    /// the overlay itself distinguishes the modes. This class is what does.
    ///
    /// Two conditions, both required:
    ///
    ///   1. We are the server. Mirror runs the host as server+client in one process, so a solo
    ///      session has NetworkServer.active true. A player who joined someone else's lobby is a
    ///      pure client and has it false. That single read excludes every session hosted by
    ///      somebody else.
    ///
    ///   2. Nobody else is connected. NetworkServer.connections holds one entry per client
    ///      including the host's own local connection, so solo is exactly 1. The moment a second
    ///      connection appears the count rises and the overlay drops on that frame.
    ///
    /// Evaluated every frame rather than latched at mission start, because a lobby can gain a
    /// player mid-session and a latched answer would go stale in the one direction that matters.
    /// Two dictionary reads per frame is nothing next to the round-robin schedulers already
    /// running in GameManager.Update.
    ///
    /// Fails closed everywhere. A missing type, a null NetworkManager, a throw inside the read,
    /// an unexpected connection count, an ambiguous state during scene transitions: every one of
    /// those returns false and the overlay stays off. The failure mode of a wrong "no" is that
    /// you do not get outlines in Lone Wolf. The failure mode of a wrong "yes" is a wallhack in
    /// a PvP match. Those are not symmetric, so the code is not symmetric either.
    /// </summary>
    internal static class SessionGate
    {
        private static Type _networkServer;
        private static bool _typeResolved;

        private static bool _lastVerdict;
        private static string _lastReason = "not yet evaluated";
        private static bool _everLogged;

        /// <summary>Human-readable explanation of the most recent verdict, for the HUD and log.</summary>
        public static string Reason => _lastReason;

        /// <summary>True only when this is a solo session with no other players connected.</summary>
        public static bool IsSolo()
        {
            bool verdict;
            string reason;

            try
            {
                Evaluate(out verdict, out reason);
            }
            catch (Exception e)
            {
                // A throw anywhere in the evaluation is treated as "cannot prove solo".
                verdict = false;
                reason = "gate threw, failing closed: " + e.Message;
            }

            if (verdict != _lastVerdict || !_everLogged)
            {
                _everLogged = true;
                MelonLogger.Msg($"[EcotiThermal] session gate: {(verdict ? "SOLO" : "NOT SOLO")} — {reason}");
            }

            _lastVerdict = verdict;
            _lastReason = reason;
            return verdict;
        }

        private static void Evaluate(out bool verdict, out string reason)
        {
            verdict = false;

            if (!_typeResolved)
            {
                _typeResolved = true;
                _networkServer = Probe.FindType("Il2CppMirror.NetworkServer") ?? Probe.FindType("Mirror.NetworkServer");
            }

            if (_networkServer == null)
            {
                reason = "Mirror.NetworkServer type not resolved";
                return;
            }

            // Condition 1: we must be the server. A joined client fails here.
            var serverActive = Probe.Static(_networkServer, "active", false);
            if (!serverActive)
            {
                var clientActive = Probe.Static(_networkServer, "activeHost", false);
                reason = clientActive
                    ? "connected to a remote host — this is somebody else's session"
                    : "no server running (menu, loading, or joined as a client)";
                return;
            }

            // Condition 2: nobody else connected. Mirror counts the host's own local connection,
            // so a solo host is exactly 1. Anything else, including an unreadable count, is a no.
            var connections = Probe.Static<object>(_networkServer, "connections");
            if (connections == null)
            {
                reason = "hosting but connection table unreadable — cannot prove solo";
                return;
            }

            var count = Probe.Instance(connections, "Count", -1);
            if (count < 0)
            {
                reason = "hosting but connection count unreadable — cannot prove solo";
                return;
            }

            if (count > 1)
            {
                reason = $"hosting with {count - 1} other player(s) connected";
                return;
            }

            verdict = true;
            reason = count == 1
                ? "hosting alone (1 local connection)"
                : "hosting alone (offline transport, no connections)";
        }
    }
}
