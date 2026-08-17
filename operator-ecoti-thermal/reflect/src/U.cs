using System;
using System.Collections.Generic;
using System.IO;
using System.Reflection;
using MelonLoader;

namespace OperatorEcotiThermal.Reflect
{
    /// <summary>
    /// Every Unity call in this build goes through here, by reflection, at runtime.
    ///
    /// Why: a normal MelonMod compiles against the Il2CppInterop-generated UnityEngine assemblies,
    /// which are produced from the installed game and do not exist on a machine without it. This
    /// variant references nothing but MelonLoader.dll, so it can be compiled anywhere — at the cost
    /// of resolving all of UnityEngine by name on first use.
    ///
    /// Il2CppInterop keeps the managed type and member names intact (UnityEngine.Camera really is
    /// UnityEngine.Camera), so name resolution works. What it does NOT keep is signatures: arrays
    /// come back as Il2CppReferenceArray&lt;T&gt; rather than T[], which is exactly why compiling
    /// against stock Unity assemblies produces a DLL that dies on load. Reflection sidesteps that
    /// because it never bakes a signature into IL — it asks the runtime what is actually there.
    ///
    /// Everything is cached on first resolve; a miss is logged once and leaves the accessor null,
    /// and every caller treats null as "feature off". SelfTest() at startup reports what resolved,
    /// so a single log paste identifies anything that needs fixing.
    /// </summary>
    internal static class U
    {
        /// <summary>Everything resolved: tracking works and the overlay can draw.</summary>
        public static bool Ready { get; private set; }

        /// <summary>Camera, transforms and Vector3 resolved — gates and tracking can run.</summary>
        public static bool CoreReady { get; private set; }

        /// <summary>IMGUI resolved — draw calls will land.</summary>
        public static bool CanDraw { get; private set; }

        // Types
        private static Type _tInput, _tTime, _tScreen, _tCamera, _tGUI, _tTexture2D,
                            _tComponent, _tTransform, _tRenderer, _tVector3, _tRect,
                            _tColor, _tBounds, _tKeyCode, _tObject;

        // Members
        private static MethodInfo _getKeyDown, _worldToScreen, _drawTexture, _label, _getComponentInChildren;
        private static PropertyInfo _unscaledTime, _screenW, _screenH, _cameraMain, _guiColor,
                                    _whiteTexture, _camTransform, _tfPosition, _rendBounds,
                                    _boundsCenter, _boundsMin, _boundsMax, _boundsSize,
                                    _tfChildCount, _objName;
        private static MethodInfo _tfGetChild;
        private static FieldInfo _vx, _vy, _vz;
        private static ConstructorInfo _ctorVector3, _ctorRect, _ctorColor;

        private static readonly HashSet<string> _warned = new HashSet<string>();
        private static readonly List<string> _report = new List<string>();

        private static void Warn(string what)
        {
            if (_warned.Add(what)) MelonLogger.Warning($"[EcotiThermal] unresolved: {what}");
        }

        /// <summary>
        /// Unity engine modules this mod needs, in load order of importance.
        ///
        /// These are force-loaded before any type resolution. AppDomain.GetAssemblies() only
        /// returns assemblies already loaded, and an Il2CppInterop assembly is loaded lazily — the
        /// first time something references it. UnityEngine.CoreModule is up early because
        /// MelonLoader itself uses it, but IMGUIModule (which owns UnityEngine.GUI) has no reason
        /// to be loaded in a game that draws its UI with uGUI canvases, as this one does. Without
        /// an explicit Assembly.Load, every drawing member resolves to null and the overlay can
        /// never become ready.
        /// </summary>
        private static readonly string[] UnityModules =
        {
            "UnityEngine.CoreModule",
            "UnityEngine.IMGUIModule",
            "UnityEngine.InputLegacyModule",
            "UnityEngine.TextRenderingModule",
            "UnityEngine",
        };

        private static readonly List<string> _moduleReport = new List<string>();

        /// <summary>
        /// Directories worth probing for an interop assembly, derived from ones already loaded.
        ///
        /// Assembly.Load(simpleName) is not enough on its own: on .NET Core it resolves through the
        /// host's dependency context, not by scanning a directory, so it throws FileNotFoundException
        /// for an assembly sitting right next to the ones already loaded. Taking the directory of a
        /// loaded interop assembly and loading siblings by path is what actually works.
        /// </summary>
        private static IEnumerable<string> ProbeDirectories()
        {
            var seen = new HashSet<string>(StringComparer.OrdinalIgnoreCase);

            foreach (var asm in AppDomain.CurrentDomain.GetAssemblies())
            {
                string dir = null;
                try
                {
                    var n = asm.GetName().Name;
                    if (n == null) continue;
                    if (!n.StartsWith("UnityEngine", StringComparison.OrdinalIgnoreCase)
                        && !n.StartsWith("Il2Cpp", StringComparison.OrdinalIgnoreCase)
                        && !n.StartsWith("Assembly-CSharp", StringComparison.OrdinalIgnoreCase)) continue;

                    if (string.IsNullOrEmpty(asm.Location)) continue;   // dynamic or in-memory
                    dir = Path.GetDirectoryName(asm.Location);
                }
                catch { }

                if (!string.IsNullOrEmpty(dir) && seen.Add(dir)) yield return dir;
            }

            var baseDir = AppDomain.CurrentDomain.BaseDirectory;
            if (!string.IsNullOrEmpty(baseDir) && seen.Add(baseDir)) yield return baseDir;

            // MelonLoader's standard layout, relative to the game root.
            if (!string.IsNullOrEmpty(baseDir))
            {
                var interop = Path.Combine(baseDir, "MelonLoader", "Il2CppAssemblies");
                if (seen.Add(interop)) yield return interop;
            }
        }

        private static void ForceLoadUnityModules()
        {
            _moduleReport.Clear();

            foreach (var name in UnityModules)
            {
                var already = false;
                foreach (var asm in AppDomain.CurrentDomain.GetAssemblies())
                {
                    if (string.Equals(asm.GetName().Name, name, StringComparison.OrdinalIgnoreCase))
                    { already = true; break; }
                }

                if (already) { _moduleReport.Add($"  loaded    {name}"); continue; }

                if (TryLoadByName(name)) { _moduleReport.Add($"  by-name   {name}"); continue; }

                var loadedPath = TryLoadByPath(name);
                if (loadedPath != null) { _moduleReport.Add($"  by-path   {name}  <- {loadedPath}"); continue; }

                _moduleReport.Add($"  NOT FOUND {name}");
            }
        }

        private static bool TryLoadByName(string name)
        {
            try { return Assembly.Load(name) != null; }
            catch { return false; }
        }

        private static string TryLoadByPath(string name)
        {
            foreach (var dir in ProbeDirectories())
            {
                string file;
                try
                {
                    file = Path.Combine(dir, name + ".dll");
                    if (!File.Exists(file)) continue;
                }
                catch { continue; }

                try
                {
                    if (Assembly.LoadFrom(file) != null) return dir;
                }
                catch { /* wrong architecture, native stub, or already loaded under another identity */ }
            }
            return null;
        }

        private static Type Find(string full)
        {
            var t = Type.GetType(full, false);
            if (t != null) return t;

            foreach (var asm in AppDomain.CurrentDomain.GetAssemblies())
            {
                try
                {
                    t = asm.GetType(full, false);
                    if (t != null) return t;
                }
                catch { }
            }

            // Last resort: some interop assemblies expose the type only via a full search of their
            // exported types, e.g. when the namespace differs from the assembly name.
            foreach (var asm in AppDomain.CurrentDomain.GetAssemblies())
            {
                var n = asm.GetName().Name;
                if (n == null || (!n.StartsWith("UnityEngine", StringComparison.OrdinalIgnoreCase)
                                  && !n.StartsWith("Il2Cpp", StringComparison.OrdinalIgnoreCase))) continue;

                try
                {
                    foreach (var candidate in asm.GetTypes())
                        if (candidate.FullName == full) return candidate;
                }
                catch { }
            }

            return null;
        }

        /// <summary>Resolve everything. Safe to call repeatedly; only the first call does work.</summary>
        public static bool Init()
        {
            if (Ready) return true;

            ForceLoadUnityModules();

            _tInput     = Find("UnityEngine.Input");
            _tTime      = Find("UnityEngine.Time");
            _tScreen    = Find("UnityEngine.Screen");
            _tCamera    = Find("UnityEngine.Camera");
            _tGUI       = Find("UnityEngine.GUI");
            _tTexture2D = Find("UnityEngine.Texture2D");
            _tComponent = Find("UnityEngine.Component");
            _tTransform = Find("UnityEngine.Transform");
            _tRenderer  = Find("UnityEngine.Renderer");
            _tVector3   = Find("UnityEngine.Vector3");
            _tRect      = Find("UnityEngine.Rect");
            _tColor     = Find("UnityEngine.Color");
            _tBounds    = Find("UnityEngine.Bounds");
            _tKeyCode   = Find("UnityEngine.KeyCode");
            _tObject    = Find("UnityEngine.Object");

            // Deliberately NOT returning early on a missing type.
            //
            // An earlier version bailed here if Camera, GUI or Vector3 was null, which meant one
            // missing type left every other member unresolved and reported as MISS. The report then
            // blamed twenty members when one had failed, and tracking died alongside drawing even
            // though it shares none of the same members. Resolve everything that can be resolved,
            // report honestly, and let the Ready/CoreReady split decide what is usable.
            const BindingFlags PubStatic = BindingFlags.Public | BindingFlags.Static;
            const BindingFlags PubInst = BindingFlags.Public | BindingFlags.Instance;

            if (_tInput != null && _tKeyCode != null)
                _getKeyDown = _tInput.GetMethod("GetKeyDown", PubStatic, null, new[] { _tKeyCode }, null);

            _unscaledTime = _tTime?.GetProperty("unscaledTime", PubStatic);
            _screenW      = _tScreen?.GetProperty("width", PubStatic);
            _screenH      = _tScreen?.GetProperty("height", PubStatic);
            _cameraMain   = _tCamera?.GetProperty("main", PubStatic);
            _guiColor     = _tGUI?.GetProperty("color", PubStatic);
            _whiteTexture = _tTexture2D?.GetProperty("whiteTexture", PubStatic);

            _worldToScreen = _tCamera?.GetMethod("WorldToScreenPoint", PubInst, null, new[] { _tVector3 }, null);
            _camTransform  = _tComponent?.GetProperty("transform", PubInst);
            _tfPosition    = _tTransform?.GetProperty("position", PubInst);
            _tfChildCount  = _tTransform?.GetProperty("childCount", PubInst);
            _tfGetChild    = _tTransform?.GetMethod("GetChild", PubInst, null, new[] { typeof(int) }, null);
            _rendBounds    = _tRenderer?.GetProperty("bounds", PubInst);
            _objName       = _tObject?.GetProperty("name", PubInst);

            if (_tBounds != null)
            {
                _boundsCenter = _tBounds.GetProperty("center", PubInst);
                _boundsMin    = _tBounds.GetProperty("min", PubInst);
                _boundsMax    = _tBounds.GetProperty("max", PubInst);
                _boundsSize   = _tBounds.GetProperty("size", PubInst);
            }

            // Null-conditional throughout now that a missing type no longer short-circuits Init.
            _vx = _tVector3?.GetField("x", PubInst);
            _vy = _tVector3?.GetField("y", PubInst);
            _vz = _tVector3?.GetField("z", PubInst);

            _ctorVector3 = _tVector3?.GetConstructor(new[] { typeof(float), typeof(float), typeof(float) });
            _ctorRect    = _tRect?.GetConstructor(new[] { typeof(float), typeof(float), typeof(float), typeof(float) });
            _ctorColor   = _tColor?.GetConstructor(new[] { typeof(float), typeof(float), typeof(float), typeof(float) });

            // Generic GetComponentInChildren<T>(). Using the generic form with MakeGenericMethod
            // avoids needing an Il2CppSystem.Type, which would drag in Il2CppInterop and another
            // layer of marshalling.
            if (_tComponent != null)
            {
                foreach (var m in _tComponent.GetMethods(PubInst))
                {
                    if (m.Name != "GetComponentInChildren" || !m.IsGenericMethodDefinition) continue;
                    if (m.GetParameters().Length != 0) continue;
                    _getComponentInChildren = m;
                    break;
                }
            }

            if (_tGUI != null && _tRect != null)
            {
                var tTexture = Find("UnityEngine.Texture");
                if (tTexture != null)
                    _drawTexture = _tGUI.GetMethod("DrawTexture", PubStatic, null, new[] { _tRect, tTexture }, null);

                _label = _tGUI.GetMethod("Label", PubStatic, null, new[] { _tRect, typeof(string) }, null);
            }

            // Two separate readiness answers, because the failures are independent and so are the
            // consequences. Tracking shares no members with drawing, so a missing GUI should still
            // leave the gate chain running and reporting contact counts to the log — that is what
            // turns "nothing happens" into "everything works except the draw call".
            CoreReady = _worldToScreen != null && _ctorVector3 != null && _vx != null
                        && _camTransform != null && _cameraMain != null;

            CanDraw = _ctorRect != null && _ctorColor != null && _drawTexture != null
                      && _whiteTexture != null && _guiColor != null;

            Ready = CoreReady && CanDraw;

            BuildReport();
            return CoreReady;
        }

        private static void BuildReport()
        {
            _report.Clear();
            void Add(string n, object o) => _report.Add($"  {(o != null ? "ok  " : "MISS")}  {n}");

            Add("UnityEngine.Input", _tInput);
            Add("UnityEngine.Time", _tTime);
            Add("UnityEngine.Screen", _tScreen);
            Add("UnityEngine.Texture2D", _tTexture2D);
            Add("UnityEngine.Component", _tComponent);
            Add("UnityEngine.Transform", _tTransform);
            Add("UnityEngine.Renderer", _tRenderer);
            Add("UnityEngine.Vector3", _tVector3);
            Add("UnityEngine.Rect", _tRect);
            Add("UnityEngine.Color", _tColor);
            Add("UnityEngine.Bounds", _tBounds);
            Add("UnityEngine.KeyCode", _tKeyCode);
            Add("UnityEngine.Camera", _tCamera);
            Add("UnityEngine.GUI", _tGUI);
            Add("Camera.main", _cameraMain);
            Add("Camera.WorldToScreenPoint", _worldToScreen);
            Add("Component.transform", _camTransform);
            Add("Component.GetComponentInChildren<T>", _getComponentInChildren);
            Add("Transform.position", _tfPosition);
            Add("Transform.childCount", _tfChildCount);
            Add("Renderer.bounds", _rendBounds);
            Add("Bounds.center/min/max", _boundsCenter);
            Add("Vector3 ctor + x/y/z", _ctorVector3);
            Add("Rect ctor", _ctorRect);
            Add("Color ctor", _ctorColor);
            Add("GUI.color", _guiColor);
            Add("GUI.DrawTexture", _drawTexture);
            Add("GUI.Label", _label);
            Add("Texture2D.whiteTexture", _whiteTexture);
            Add("Input.GetKeyDown", _getKeyDown);
            Add("Time.unscaledTime", _unscaledTime);
            Add("Screen.width", _screenW);
        }

        /// <summary>
        /// Log what resolved. This is the diagnostic that matters: this build cannot be tested
        /// before shipping, so if something is wrong, this table says which member it was.
        /// </summary>
        public static void SelfTest()
        {
            MelonLogger.Msg("════ EcotiThermal reflection self-test ════");

            MelonLogger.Msg("Unity modules:");
            if (_moduleReport.Count == 0) MelonLogger.Msg("  (Init has not run yet)");
            foreach (var line in _moduleReport) MelonLogger.Msg(line);

            MelonLogger.Msg("Members:");
            if (_report.Count == 0) MelonLogger.Msg("  (nothing resolved yet)");
            foreach (var line in _report) MelonLogger.Msg(line);

            // If resolution failed, the list of assemblies that ARE present is the thing that
            // identifies why. Printed only on failure, since it is long.
            if (!CoreReady || !CanDraw)
            {
                MelonLogger.Msg("Loaded assemblies (UnityEngine* / Il2Cpp*):");
                var names = new List<string>();
                foreach (var asm in AppDomain.CurrentDomain.GetAssemblies())
                {
                    var n = asm.GetName().Name;
                    if (n == null) continue;
                    if (n.StartsWith("UnityEngine", StringComparison.OrdinalIgnoreCase)
                        || n.StartsWith("Il2Cpp", StringComparison.OrdinalIgnoreCase))
                        names.Add(n);
                }
                names.Sort();
                if (names.Count == 0) MelonLogger.Msg("  (none — Unity is not up yet)");
                foreach (var n in names) MelonLogger.Msg("  " + n);
            }

            MelonLogger.Msg($"  => tracking {(CoreReady ? "OK" : "BROKEN")}, drawing {(CanDraw ? "OK" : "BROKEN")}");
            if (CoreReady && !CanDraw)
                MelonLogger.Msg("     IMGUI is unavailable. Gates and tracking still run and report to this log.");
            MelonLogger.Msg("══════════════════════════════════════════");
        }

        // ── Scalars ───────────────────────────────────────────────────────────────────────────

        public static float UnscaledTime
        {
            get { try { return _unscaledTime == null ? 0f : (float)_unscaledTime.GetValue(null); } catch { return 0f; } }
        }

        public static int ScreenWidth
        {
            get { try { return _screenW == null ? 1920 : (int)_screenW.GetValue(null); } catch { return 1920; } }
        }

        public static int ScreenHeight
        {
            get { try { return _screenH == null ? 1080 : (int)_screenH.GetValue(null); } catch { return 1080; } }
        }

        public static bool GetKeyDown(int keyCode)
        {
            if (_getKeyDown == null || _tKeyCode == null) return false;
            try { return (bool)_getKeyDown.Invoke(null, new[] { Enum.ToObject(_tKeyCode, keyCode) }); }
            catch { Warn("Input.GetKeyDown"); return false; }
        }

        // ── Structs ───────────────────────────────────────────────────────────────────────────

        /// <summary>Boxed UnityEngine.Vector3. Boxed because reflection speaks in objects.</summary>
        public static object Vec3(float x, float y, float z)
        {
            try { return _ctorVector3?.Invoke(new object[] { x, y, z }); }
            catch { Warn("Vector3 ctor"); return null; }
        }

        public static bool ReadVec3(object v, out float x, out float y, out float z)
        {
            x = y = z = 0f;
            if (v == null || _vx == null) return false;
            try
            {
                x = (float)_vx.GetValue(v);
                y = (float)_vy.GetValue(v);
                z = (float)_vz.GetValue(v);
                return true;
            }
            catch { Warn("Vector3 fields"); return false; }
        }

        public static object MakeRect(float x, float y, float w, float h)
        {
            try { return _ctorRect?.Invoke(new object[] { x, y, w, h }); }
            catch { Warn("Rect ctor"); return null; }
        }

        public static object MakeColor(float r, float g, float b, float a)
        {
            try { return _ctorColor?.Invoke(new object[] { r, g, b, a }); }
            catch { Warn("Color ctor"); return null; }
        }

        // ── Scene graph ───────────────────────────────────────────────────────────────────────

        public static object CameraMain()
        {
            try { return _cameraMain?.GetValue(null); }
            catch { Warn("Camera.main"); return null; }
        }

        public static object TransformOf(object component)
        {
            if (component == null || _camTransform == null) return null;
            try { return _camTransform.GetValue(component); }
            catch { return null; }
        }

        public static bool PositionOf(object transform, out float x, out float y, out float z)
        {
            x = y = z = 0f;
            if (transform == null || _tfPosition == null) return false;
            try { return ReadVec3(_tfPosition.GetValue(transform), out x, out y, out z); }
            catch { return false; }
        }

        public static string NameOf(object unityObject)
        {
            if (unityObject == null || _objName == null) return string.Empty;
            try { return _objName.GetValue(unityObject) as string ?? string.Empty; }
            catch { return string.Empty; }
        }

        public static int ChildCount(object transform)
        {
            if (transform == null || _tfChildCount == null) return 0;
            try { return (int)_tfChildCount.GetValue(transform); }
            catch { return 0; }
        }

        public static object GetChild(object transform, int index)
        {
            if (transform == null || _tfGetChild == null) return null;
            try { return _tfGetChild.Invoke(transform, new object[] { index }); }
            catch { return null; }
        }

        /// <summary>
        /// GetComponentInChildren&lt;T&gt; where T is resolved at runtime by short type name.
        /// The type is searched across every loaded assembly rather than assumed to sit in a
        /// particular interop namespace, because that namespace is a guess until read off a build.
        /// </summary>
        public static object FindComponentInChildren(object root, string shortTypeName)
        {
            if (root == null || _getComponentInChildren == null || _tComponent == null) return null;

            var target = ResolveComponentType(shortTypeName);
            if (target == null) return null;

            try { return _getComponentInChildren.MakeGenericMethod(target).Invoke(root, null); }
            catch { Warn($"GetComponentInChildren<{shortTypeName}>"); return null; }
        }

        private static readonly Dictionary<string, Type> _componentTypes = new Dictionary<string, Type>();

        private static Type ResolveComponentType(string shortName)
        {
            if (string.IsNullOrEmpty(shortName)) return null;
            if (_componentTypes.TryGetValue(shortName, out var cached)) return cached;

            Type found = Find("Il2Cpp." + shortName) ?? Find(shortName);

            if (found == null)
            {
                foreach (var asm in AppDomain.CurrentDomain.GetAssemblies())
                {
                    try
                    {
                        foreach (var t in asm.GetTypes())
                        {
                            if (t.Name != shortName) continue;
                            if (_tComponent != null && !_tComponent.IsAssignableFrom(t)) continue;
                            found = t;
                            break;
                        }
                    }
                    catch { /* ReflectionTypeLoadException on malformed neighbours */ }
                    if (found != null) break;
                }
            }

            if (found != null && _tComponent != null && !_tComponent.IsAssignableFrom(found)) found = null;
            if (found == null) Warn($"component type '{shortName}'");

            _componentTypes[shortName] = found;
            return found;
        }

        // ── Bounds ────────────────────────────────────────────────────────────────────────────

        /// <summary>Renderer world bounds as centre + size. False when unavailable.</summary>
        public static bool RendererBounds(object renderer, out float cx, out float cy, out float cz,
                                          out float sx, out float sy, out float sz)
        {
            cx = cy = cz = sx = sy = sz = 0f;
            if (renderer == null || _rendBounds == null || _boundsCenter == null || _boundsSize == null) return false;

            try
            {
                var b = _rendBounds.GetValue(renderer);
                if (b == null) return false;
                if (!ReadVec3(_boundsCenter.GetValue(b), out cx, out cy, out cz)) return false;
                if (!ReadVec3(_boundsSize.GetValue(b), out sx, out sy, out sz)) return false;
                return true;
            }
            catch { return false; }
        }

        // ── Projection and drawing ────────────────────────────────────────────────────────────

        /// <summary>World point to screen point. z is depth; z &lt;= 0 means behind the camera.</summary>
        public static bool WorldToScreen(object camera, float wx, float wy, float wz,
                                         out float sx, out float sy, out float sz)
        {
            sx = sy = sz = 0f;
            if (camera == null || _worldToScreen == null) return false;

            try
            {
                var v = Vec3(wx, wy, wz);
                if (v == null) return false;
                var r = _worldToScreen.Invoke(camera, new[] { v });
                return ReadVec3(r, out sx, out sy, out sz);
            }
            catch { Warn("WorldToScreenPoint"); return false; }
        }

        public static void SetColor(float r, float g, float b, float a)
        {
            if (_guiColor == null) return;
            try
            {
                var c = MakeColor(r, g, b, a);
                if (c != null) _guiColor.SetValue(null, c);
            }
            catch { Warn("GUI.color"); }
        }

        private static object _white;

        public static void FillRect(float x, float y, float w, float h)
        {
            if (_drawTexture == null || _whiteTexture == null) return;

            try
            {
                if (_white == null) _white = _whiteTexture.GetValue(null);
                if (_white == null) return;

                var rect = MakeRect(x, y, w, h);
                if (rect == null) return;

                _drawTexture.Invoke(null, new[] { rect, _white });
            }
            catch { Warn("GUI.DrawTexture"); }
        }

        public static void Label(float x, float y, float w, float h, string text)
        {
            if (_label == null) return;
            try
            {
                var rect = MakeRect(x, y, w, h);
                if (rect != null) _label.Invoke(null, new[] { rect, text });
            }
            catch { Warn("GUI.Label"); }
        }
    }
}
