using System;
using UnityEngine;

namespace OperatorEcotiThermal
{
    /// <summary>
    /// Answers "is the local player looking through quad tubes right now".
    ///
    /// NvgLensFollower carries three statics the toolkit calls the cheapest NVG probe in the game,
    /// and the NVG autogate custom pass already reads the same registry to project its per-lens
    /// meter circles, so these are live values rather than something inferred from the loadout:
    ///
    ///   AnyActive          — local player has NVGs down and powered
    ///   LensCount          — number of lenses on the active device: 2 for a binocular, 4 for a quad
    ///   PhosphorColorValue — tube phosphor colour, used here to tint the overlay to match
    ///
    /// LensCount is what makes the "quad tube only" requirement real rather than cosmetic. Flip to
    /// a dual-tube device and the count drops to 2 and the overlay goes away on the same frame.
    /// </summary>
    internal static class NvgProbe
    {
        private static Type _lensFollower;
        private static bool _typeResolved;

        /// <summary>Lenses on the currently active NVG device. 0 when NVGs are up or off.</summary>
        public static int LensCount { get; private set; }

        /// <summary>True when NVGs are down and powered.</summary>
        public static bool NvgActive { get; private set; }

        /// <summary>Phosphor colour of the active tubes, for tinting the overlay to match.</summary>
        public static Color PhosphorColor { get; private set; } = new Color(0.35f, 1f, 0.55f);

        private static void ResolveType()
        {
            if (_typeResolved) return;
            _typeResolved = true;
            _lensFollower = Probe.FindType("Il2Cpp.NvgLensFollower") ?? Probe.FindType("NvgLensFollower");
        }

        /// <summary>Refresh the cached readings. Called once per frame before the gates are checked.</summary>
        public static void Sample()
        {
            ResolveType();

            if (_lensFollower == null)
            {
                NvgActive = false;
                LensCount = 0;
                return;
            }

            NvgActive = Probe.Static(_lensFollower, "AnyActive", false);
            LensCount = NvgActive ? Probe.Static(_lensFollower, "LensCount", 0) : 0;

            // PhosphorColorValue may come back as a Color, a Vector4 or a packed float depending on
            // how the interop generator handled it. Only accept a real Color; otherwise keep the
            // last good value rather than tinting the overlay with garbage.
            var phosphor = Probe.Static<Color>(_lensFollower, "PhosphorColorValue", Color.clear);
            if (phosphor != Color.clear && phosphor.a > 0f) PhosphorColor = phosphor;
        }

        /// <summary>True when the active device is a quad tube (or wider).</summary>
        public static bool IsQuadTube(int requiredLenses) => NvgActive && LensCount >= requiredLenses;

        /// <summary>Short status string for the HUD.</summary>
        public static string Describe()
        {
            if (_lensFollower == null) return "NvgLensFollower not resolved";
            if (!NvgActive) return "NVGs up or unpowered";
            return $"NVGs down, {LensCount} lens{(LensCount == 1 ? "" : "es")}";
        }
    }
}
