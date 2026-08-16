using System;
using MelonLoader;
using UnityEngine;

[assembly: MelonInfo(typeof(OperatorEcotiThermal.Main), "ECOTI Thermal Overlay", "1.0.0", "velocityspider26-art")]
[assembly: MelonGame(null, null)]

namespace OperatorEcotiThermal
{
    /// <summary>
    /// ECOTI Thermal Overlay — solo-only thermal outlining for OPERATOR.
    ///
    /// Outlines hostile AI in red while the local player is looking through quad-tube NVGs with an
    /// ECOTI clip-on mounted, and only in a session where no other player is connected.
    ///
    /// Four gates, checked in order every frame, all of which must pass:
    ///
    ///   1. Enabled              — the master switch and the F7 toggle
    ///   2. Solo session         — host, and nobody else connected (see SessionGate)
    ///   3. Quad tube active     — NvgLensFollower.LensCount >= 4 with NVGs down
    ///   4. ECOTI mounted        — a matching clip-on in an NVG mount socket
    ///
    /// Gate 2 is the one that matters and it is not configurable. OPERATOR runs Lone Wolf, co-op
    /// PvE and PvP out of a single client over a single Mirror stack, so an ungated overlay would
    /// be a PvP wallhack whatever the intent behind it. Gate 2 is what makes this a single-player
    /// mod in fact rather than by promise, and every failure path in it resolves to "off".
    ///
    /// Worth knowing about gate 4: the real ECOTI is a clip-on thermal imager that fuses a thermal
    /// image into the night vision tube. It is line-of-sight — thermal does not go through walls.
    /// This overlay does, so it is a game mod rather than a simulation of the device.
    /// </summary>
    public class Main : MelonMod
    {
        private Options _opt;
        private bool _runtimeToggle = true;
        private string _status = "starting";
        private bool _active;

        public override void OnInitializeMelon()
        {
            _opt = Options.Load();

            // The field notes warn that writing to the console during IL2CPP startup was enough to
            // stop the game loading, so the banner goes through the loader.
            LoggerInstance.Msg("ECOTI Thermal Overlay loaded.");
            LoggerInstance.Msg($"  {_opt.Toggle} toggle · {_opt.Status} status HUD · {_opt.Dump} inventory dump");
            LoggerInstance.Msg("  Solo sessions only. The overlay is off in any session with another player connected.");
        }

        public override void OnSceneWasLoaded(int buildIndex, string sceneName)
        {
            // Caches hold Transforms and Renderers from the outgoing scene. Dereferencing those
            // during teardown is exactly the case the field notes flag as an uncatchable native
            // crash, so everything is dropped on the transition rather than revalidated.
            EnemyTracker.Reset();
            EcotiDetector.Reset();
            Overlay.Reset();
        }

        public override void OnUpdate()
        {
            if (_opt == null) return;

            try { HandleKeys(); }
            catch (Exception e) { LoggerInstance.Error($"input handling failed: {e.Message}"); }

            try { _active = EvaluateGates(out _status); }
            catch (Exception e)
            {
                _active = false;
                _status = "gate evaluation threw, overlay off";
                LoggerInstance.Error($"gate evaluation failed: {e.Message}");
            }

            if (_active)
            {
                try { EnemyTracker.Tick(Mathf.Max(0.05f, _opt.RebuildInterval.Value)); }
                catch (Exception e) { LoggerInstance.Error($"enemy tick failed: {e.Message}"); }
            }
        }

        private void HandleKeys()
        {
            if (Input.GetKeyDown(_opt.Toggle))
            {
                _runtimeToggle = !_runtimeToggle;
                LoggerInstance.Msg($"overlay {( _runtimeToggle ? "enabled" : "disabled")} by hotkey");
            }

            if (Input.GetKeyDown(_opt.Status))
                _opt.ShowStatusHud.Value = !_opt.ShowStatusHud.Value;

            if (Input.GetKeyDown(_opt.Dump))
                EcotiDetector.Dump();
        }

        /// <summary>
        /// Run the gate chain. Returns true only when every gate passes, and reports the first
        /// gate that blocked so the status HUD can say why nothing is drawing.
        /// </summary>
        private bool EvaluateGates(out string status)
        {
            if (!_opt.Enabled.Value) { status = "disabled in config"; return false; }
            if (!_runtimeToggle) { status = $"toggled off ({_opt.Toggle})"; return false; }

            // Gate 2. Everything below is cosmetic; this one is the point.
            if (!SessionGate.IsSolo())
            {
                status = "SOLO ONLY — " + SessionGate.Reason;
                return false;
            }

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
                var state = EcotiDetector.Current;

                if (state != EcotiDetector.State.Present)
                {
                    status = state == EcotiDetector.State.Unknown
                        ? $"ECOTI state unknown — press {_opt.Dump} to dump kit names"
                        : "ECOTI not mounted";
                    return false;
                }
            }

            status = $"active — {EnemyTracker.Count} contact(s), {NvgProbe.Describe()}";
            return true;
        }

        public override void OnGUI()
        {
            if (_opt == null) return;

            try
            {
                if (_opt.ShowStatusHud.Value)
                    Overlay.DrawStatus("ECOTI: " + _status, _active);

                if (_active) Overlay.Draw(_opt);
            }
            catch (Exception e)
            {
                // An exception thrown out of OnGUI repeats every frame, so it is caught here and
                // the overlay simply does not draw this frame.
                LoggerInstance.Error($"draw failed: {e.Message}");
            }
        }
    }
}
