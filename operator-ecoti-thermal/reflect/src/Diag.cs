using System;
using System.IO;
using System.Text;
using MelonLoader;

namespace OperatorEcotiThermal.Reflect
{
    /// <summary>
    /// Writes a diagnostic report to a plain text file beside the game.
    ///
    /// This exists because the MelonLoader log is the wrong channel for the one thing that matters
    /// when the mod does nothing: it is buried in the install, interleaved with every other melon,
    /// and asking someone to go find it and identify the relevant block is a step that does not
    /// happen. A single named file at the game root, containing the whole state in one place, does.
    ///
    /// Deliberately uses nothing but System.IO. If the Unity reflection layer has failed completely
    /// this still writes, which is exactly the case where the report is most needed.
    /// </summary>
    internal static class Diag
    {
        private const string FileName = "EcotiThermal-diagnostic.txt";

        private static string _path;
        private static bool _pathResolved;
        private static bool _warnedFailure;

        private static string Path_()
        {
            if (_pathResolved) return _path;
            _pathResolved = true;

            // The game root is the base directory of the process, which is where OPERATOR.exe and
            // the MelonLoader folder live. Falls back to the temp directory if that is not writable.
            try
            {
                var baseDir = AppDomain.CurrentDomain.BaseDirectory;
                if (!string.IsNullOrEmpty(baseDir))
                {
                    _path = System.IO.Path.Combine(baseDir, FileName);
                    return _path;
                }
            }
            catch { }

            try { _path = System.IO.Path.Combine(System.IO.Path.GetTempPath(), FileName); }
            catch { _path = null; }

            return _path;
        }

        /// <summary>
        /// Write the full report. Cheap enough to call on startup, on the dump key, and once when
        /// the overlay first activates; not called per frame.
        /// </summary>
        public static void Write(string reason, string gateStatus, int contacts)
        {
            var path = Path_();
            if (path == null) return;

            try
            {
                var sb = new StringBuilder();
                sb.AppendLine("ECOTI Thermal Overlay — diagnostic report");
                sb.AppendLine("Paste this whole file when reporting a problem.");
                sb.AppendLine();
                sb.AppendLine($"written        : {DateTime.Now:yyyy-MM-dd HH:mm:ss} ({reason})");
                sb.AppendLine($"mod version    : 1.0.0-reflect");
                sb.AppendLine($"process        : {AppDomain.CurrentDomain.FriendlyName}");
                sb.AppendLine($"base directory : {AppDomain.CurrentDomain.BaseDirectory}");
                sb.AppendLine($"runtime        : {Environment.Version}");
                sb.AppendLine($"os             : {Environment.OSVersion}");
                sb.AppendLine();

                sb.AppendLine("── readiness ─────────────────────────────────────────");
                sb.AppendLine($"tracking usable (CoreReady) : {U.CoreReady}");
                sb.AppendLine($"drawing usable  (CanDraw)   : {U.CanDraw}");
                sb.AppendLine();
                if (!U.CoreReady)
                    sb.AppendLine("=> Unity reflection failed. See the member table below for which one.");
                else if (!U.CanDraw)
                    sb.AppendLine("=> Tracking works but IMGUI did not resolve, so nothing can be drawn.");
                else
                    sb.AppendLine("=> Reflection is fine. If nothing appears, the gate status below says why.");
                sb.AppendLine();

                sb.AppendLine("── gate status ───────────────────────────────────────");
                sb.AppendLine($"current  : {gateStatus}");
                sb.AppendLine($"session  : {SessionGate.Reason}");
                sb.AppendLine($"nvg      : {NvgProbe.Describe()} (active={NvgProbe.NvgActive}, lenses={NvgProbe.LensCount})");
                sb.AppendLine($"ecoti    : {EcotiDetector.Current}");
                sb.AppendLine($"contacts : {contacts}");
                sb.AppendLine();

                sb.AppendLine("── reflection self-test ──────────────────────────────");
                foreach (var line in U.ReportLines()) sb.AppendLine(line);
                sb.AppendLine();

                sb.AppendLine("── loaded assemblies (UnityEngine* / Il2Cpp* / Assembly-CSharp) ──");
                foreach (var asm in AppDomain.CurrentDomain.GetAssemblies())
                {
                    string n;
                    try { n = asm.GetName().Name; } catch { continue; }
                    if (n == null) continue;
                    if (!n.StartsWith("UnityEngine", StringComparison.OrdinalIgnoreCase)
                        && !n.StartsWith("Il2Cpp", StringComparison.OrdinalIgnoreCase)
                        && !n.StartsWith("Assembly-CSharp", StringComparison.OrdinalIgnoreCase)) continue;
                    sb.AppendLine("  " + n);
                }

                File.WriteAllText(path, sb.ToString());
            }
            catch (Exception e)
            {
                if (!_warnedFailure)
                {
                    _warnedFailure = true;
                    MelonLogger.Warning($"[EcotiThermal] could not write {FileName}: {e.Message}");
                }
            }
        }

        public static string Location => Path_() ?? "<unavailable>";
    }
}
