using MelonLoader;
using UnityEngine;

namespace OperatorEcotiThermal
{
    /// <summary>
    /// MelonPreferences config. Written to UserData/MelonPreferences.cfg on first run.
    ///
    /// Note what is deliberately absent: there is no setting that disables the solo gate. Every
    /// other behaviour here is tunable. That one is not, because it is the thing that keeps this a
    /// single-player mod rather than a PvP cheat, and a config toggle would make it neither.
    /// </summary>
    internal sealed class Options
    {
        public MelonPreferences_Entry<bool> Enabled;

        public MelonPreferences_Entry<bool> RequireQuadTube;
        public MelonPreferences_Entry<int> RequiredLenses;
        public MelonPreferences_Entry<bool> RequireEcoti;
        public MelonPreferences_Entry<string> EcotiNameContains;

        public MelonPreferences_Entry<float> MaxRange;
        public MelonPreferences_Entry<float> OutlineR;
        public MelonPreferences_Entry<float> OutlineG;
        public MelonPreferences_Entry<float> OutlineB;
        public MelonPreferences_Entry<float> OutlineThickness;
        public MelonPreferences_Entry<float> FillOpacity;
        public MelonPreferences_Entry<bool> DistanceFade;
        public MelonPreferences_Entry<bool> ShowDistance;

        public MelonPreferences_Entry<float> RebuildInterval;
        public MelonPreferences_Entry<bool> ShowStatusHud;

        public MelonPreferences_Entry<int> ToggleKey;
        public MelonPreferences_Entry<int> StatusKey;
        public MelonPreferences_Entry<int> DumpKey;

        public KeyCode Toggle => (KeyCode)ToggleKey.Value;
        public KeyCode Status => (KeyCode)StatusKey.Value;
        public KeyCode Dump => (KeyCode)DumpKey.Value;

        public static Options Load()
        {
            var o = new Options();
            var cfg = MelonPreferences.CreateCategory("EcotiThermal", "ECOTI Thermal Overlay");

            o.Enabled = cfg.CreateEntry("Enabled", true,
                description: "Master switch. The solo gate still applies when this is on.");

            o.RequireQuadTube = cfg.CreateEntry("RequireQuadTube", true,
                description: "Only draw while looking through a quad-tube device. Off means any active NVG.");
            o.RequiredLenses = cfg.CreateEntry("RequiredLenses", 4,
                description: "Lens count that counts as a quad tube. Read from NvgLensFollower.LensCount.");
            o.RequireEcoti = cfg.CreateEntry("RequireEcoti", true,
                description: "Also require an ECOTI clip-on mounted. If the overlay never appears, press the "
                           + "dump key to list your equipped mod names and set EcotiNameContains to match, "
                           + "or set this to false.");
            o.EcotiNameContains = cfg.CreateEntry("EcotiNameContains", "ecoti",
                description: "Case-insensitive substring matched against equipped CharacterMod names.");

            o.MaxRange = cfg.CreateEntry("MaxRange", 250f,
                description: "Metres. Contacts beyond this are not drawn.");
            o.OutlineR = cfg.CreateEntry("OutlineR", 1f, description: "Outline red, 0-1.");
            o.OutlineG = cfg.CreateEntry("OutlineG", 0.12f, description: "Outline green, 0-1.");
            o.OutlineB = cfg.CreateEntry("OutlineB", 0.12f, description: "Outline blue, 0-1.");
            o.OutlineThickness = cfg.CreateEntry("OutlineThickness", 1.5f, description: "Pixels.");
            o.FillOpacity = cfg.CreateEntry("FillOpacity", 0.10f,
                description: "Translucent fill inside the box, 0 for outline only. Reads like a thermal blob.");
            o.DistanceFade = cfg.CreateEntry("DistanceFade", true,
                description: "Fade distant contacts, the way a weaker thermal signature would read.");
            o.ShowDistance = cfg.CreateEntry("ShowDistance", false,
                description: "Print range in metres under each box.");

            o.RebuildInterval = cfg.CreateEntry("RebuildInterval", 0.25f,
                description: "Seconds between walks of the BrainAI registry. Positions update every frame "
                           + "regardless; this only controls how quickly new spawns are picked up.");
            o.ShowStatusHud = cfg.CreateEntry("ShowStatusHud", true,
                description: "Show the gate status line. Tells you which gate is blocking when nothing draws.");

            o.ToggleKey = cfg.CreateEntry("ToggleKey", (int)KeyCode.F7, description: "Toggle the overlay.");
            o.StatusKey = cfg.CreateEntry("StatusKey", (int)KeyCode.F8, description: "Toggle the status HUD.");
            o.DumpKey = cfg.CreateEntry("DumpKey", (int)KeyCode.F9,
                description: "Dump equipped character mods and NVG state to the log, for finding the ECOTI name.");

            return o;
        }
    }
}
