using System;
using UnityEngine;

namespace OperatorEcotiThermal
{
    /// <summary>
    /// Draws the thermal outlines.
    ///
    /// This is deliberately an IMGUI screen-space overlay rather than a material swap or an
    /// injected HDRP custom pass, and the reason is testability rather than taste. A chams-style
    /// second material has to fight HDRP's ZTest setup and the toolkit warns that blind-swapping
    /// onto a Shader Graph material loses its maps; an injected CustomPass subclass needs
    /// ClassInjector and lands in a render stack that already runs eleven-plus passes including a
    /// compute-shader NVG autogate. Both are real options and both need a live build to tune. This
    /// path needs no shader, no asset and no pipeline surgery, so it is the one that works the
    /// first time you launch. Options.cs documents where to go if you want true 3D chams later.
    ///
    /// Corners are projected rather than centre-plus-height so the box follows posture: prone bots
    /// give a flat wide box, standing ones a tall narrow box, and it stays correct as the renderer
    /// bounds change with animation.
    /// </summary>
    internal static class Overlay
    {
        private static Texture2D _pixel;
        private static Camera _camera;
        private static float _lastCameraLookup = -999f;
        private static GUIStyle _labelStyle;

        private static readonly Vector3[] _corners = new Vector3[8];

        public static void Reset()
        {
            _camera = null;
            _lastCameraLookup = -999f;
        }

        private static Texture2D Pixel()
        {
            if (_pixel != null) return _pixel;

            _pixel = new Texture2D(1, 1, TextureFormat.RGBA32, false);
            _pixel.SetPixel(0, 0, Color.white);
            _pixel.Apply();
            _pixel.hideFlags = HideFlags.HideAndDontSave;
            return _pixel;
        }

        /// <summary>
        /// The camera to project against. Looked up on a throttle rather than per frame, and never
        /// via Resources.FindObjectsOfTypeAll. MainCameraSingleton is the game's own handle; the
        /// Camera.main fallback covers the window before the singleton is populated.
        /// </summary>
        private static Camera ResolveCamera()
        {
            if (_camera != null) return _camera;
            if (Time.unscaledTime - _lastCameraLookup < 1f) return null;
            _lastCameraLookup = Time.unscaledTime;

            try
            {
                var singletonType = Probe.FindType("Il2Cpp.MainCameraSingleton") ?? Probe.FindType("MainCameraSingleton");
                if (singletonType != null)
                {
                    var instance = Probe.Static<object>(singletonType, "instance");
                    if (instance != null)
                    {
                        var cam = Probe.Instance<Camera>(instance, "mainCamera")
                                  ?? Probe.Instance<Camera>(instance, "MainCamera")
                                  ?? Probe.Instance<Camera>(instance, "camera");
                        if (cam != null) { _camera = cam; return _camera; }

                        var component = instance as Component;
                        if (component != null)
                        {
                            cam = component.GetComponentInChildren<Camera>();
                            if (cam != null) { _camera = cam; return _camera; }
                        }
                    }
                }

                _camera = Camera.main;
            }
            catch { _camera = null; }

            return _camera;
        }

        public static void Draw(Options opt)
        {
            var cam = ResolveCamera();
            if (cam == null) return;

            var pixel = Pixel();
            var origin = cam.transform.position;
            var maxRangeSq = opt.MaxRange.Value * opt.MaxRange.Value;

            var outline = new Color(opt.OutlineR.Value, opt.OutlineG.Value, opt.OutlineB.Value, 1f);
            var thickness = Mathf.Max(1f, opt.OutlineThickness.Value);

            var previous = GUI.color;

            foreach (var enemy in EnemyTracker.Enemies)
            {
                if (!enemy.TryGetBounds(out var bounds)) continue;

                var toEnemy = bounds.center - origin;
                var distSq = toEnemy.sqrMagnitude;
                if (distSq > maxRangeSq) continue;

                if (!TryProjectBounds(cam, bounds, out var rect)) continue;

                // Fade with distance so a far contact reads as fainter, the way a weak thermal
                // signature would, instead of every contact being equally loud.
                var fade = opt.DistanceFade.Value
                    ? Mathf.Lerp(1f, 0.35f, Mathf.Clamp01(Mathf.Sqrt(distSq) / opt.MaxRange.Value))
                    : 1f;

                if (opt.FillOpacity.Value > 0.001f)
                {
                    GUI.color = new Color(outline.r, outline.g, outline.b, opt.FillOpacity.Value * fade);
                    GUI.DrawTexture(rect, pixel);
                }

                GUI.color = new Color(outline.r, outline.g, outline.b, fade);
                DrawBox(rect, thickness, pixel);

                if (opt.ShowDistance.Value)
                {
                    var metres = Mathf.Sqrt(distSq);
                    DrawLabel(rect, $"{metres:0}m", outline, fade);
                }
            }

            GUI.color = previous;
        }

        /// <summary>
        /// Project the eight corners of the world AABB and take the screen-space extent.
        ///
        /// WorldToScreenPoint returns z as distance along the camera forward axis, so z &lt;= 0 means
        /// the corner is behind the near plane and its x/y are mirrored garbage. Those corners are
        /// dropped; if the centre itself is behind the camera the enemy is skipped outright.
        /// </summary>
        private static bool TryProjectBounds(Camera cam, Bounds bounds, out Rect rect)
        {
            rect = default;

            var centre = cam.WorldToScreenPoint(bounds.center);
            if (centre.z <= 0f) return false;

            var min = bounds.min;
            var max = bounds.max;

            _corners[0] = new Vector3(min.x, min.y, min.z);
            _corners[1] = new Vector3(max.x, min.y, min.z);
            _corners[2] = new Vector3(min.x, max.y, min.z);
            _corners[3] = new Vector3(max.x, max.y, min.z);
            _corners[4] = new Vector3(min.x, min.y, max.z);
            _corners[5] = new Vector3(max.x, min.y, max.z);
            _corners[6] = new Vector3(min.x, max.y, max.z);
            _corners[7] = new Vector3(max.x, max.y, max.z);

            float left = float.MaxValue, right = float.MinValue;
            float bottom = float.MaxValue, top = float.MinValue;
            var projected = 0;

            for (int i = 0; i < 8; i++)
            {
                var p = cam.WorldToScreenPoint(_corners[i]);
                if (p.z <= 0f) continue;

                projected++;
                if (p.x < left) left = p.x;
                if (p.x > right) right = p.x;
                if (p.y < bottom) bottom = p.y;
                if (p.y > top) top = p.y;
            }

            if (projected == 0) return false;

            // Screen space is bottom-left origin, GUI space is top-left.
            var guiTop = Screen.height - top;
            var guiBottom = Screen.height - bottom;

            var w = right - left;
            var h = guiBottom - guiTop;
            if (w < 1f || h < 1f) return false;

            // Wholly offscreen contacts cost nothing to skip.
            if (right < 0f || left > Screen.width || guiBottom < 0f || guiTop > Screen.height) return false;

            rect = new Rect(left, guiTop, w, h);
            return true;
        }

        private static void DrawBox(Rect r, float t, Texture2D pixel)
        {
            GUI.DrawTexture(new Rect(r.xMin, r.yMin, r.width, t), pixel);              // top
            GUI.DrawTexture(new Rect(r.xMin, r.yMax - t, r.width, t), pixel);          // bottom
            GUI.DrawTexture(new Rect(r.xMin, r.yMin, t, r.height), pixel);             // left
            GUI.DrawTexture(new Rect(r.xMax - t, r.yMin, t, r.height), pixel);         // right
        }

        private static void DrawLabel(Rect box, string text, Color colour, float fade)
        {
            if (_labelStyle == null)
            {
                _labelStyle = new GUIStyle(GUI.skin.label)
                {
                    alignment = TextAnchor.UpperCenter,
                    fontSize = 11
                };
            }

            _labelStyle.normal.textColor = new Color(colour.r, colour.g, colour.b, fade);
            GUI.Label(new Rect(box.xMin, box.yMax + 1f, box.width, 16f), text, _labelStyle);
        }

        /// <summary>Status readout for the diagnostic HUD.</summary>
        public static void DrawStatus(string line, bool active)
        {
            if (_labelStyle == null)
            {
                _labelStyle = new GUIStyle(GUI.skin.label)
                {
                    alignment = TextAnchor.UpperCenter,
                    fontSize = 11
                };
            }

            var style = new GUIStyle(GUI.skin.label)
            {
                alignment = TextAnchor.UpperLeft,
                fontSize = 12
            };
            style.normal.textColor = active ? new Color(0.4f, 1f, 0.5f) : new Color(1f, 0.65f, 0.3f);

            var previous = GUI.color;
            GUI.color = new Color(0f, 0f, 0f, 0.55f);
            GUI.DrawTexture(new Rect(8f, 8f, 520f, 22f), Pixel());
            GUI.color = previous;

            GUI.Label(new Rect(14f, 10f, 510f, 20f), line, style);
        }
    }
}
