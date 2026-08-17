// Mock of the Il2CppInterop-generated UnityEngine.CoreModule: same type names, same member
// shapes (fields vs properties, exact signatures). Behaviour is only as real as it needs to be
// for the reflection layer to be exercised end to end.
using System.Collections.Generic;

namespace UnityEngine
{
    public struct Vector3
    {
        public float x, y, z;                                  // public FIELDS, as in Unity
        public Vector3(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
        public override string ToString() => $"({x:0.##},{y:0.##},{z:0.##})";
    }

    public struct Color
    {
        public float r, g, b, a;
        public Color(float r, float g, float b, float a) { this.r = r; this.g = g; this.b = b; this.a = a; }
        public override string ToString() => $"rgba({r:0.##},{g:0.##},{b:0.##},{a:0.##})";
    }

    public struct Rect
    {
        private float m_x, m_y, m_w, m_h;
        public Rect(float x, float y, float width, float height) { m_x = x; m_y = y; m_w = width; m_h = height; }
        public float x => m_x; public float y => m_y;
        public float width => m_w; public float height => m_h;
        public override string ToString() => $"[{m_x:0.#},{m_y:0.#} {m_w:0.#}x{m_h:0.#}]";
    }

    public struct Bounds
    {
        private Vector3 m_Center, m_Extents;
        public Bounds(Vector3 center, Vector3 size)
        { m_Center = center; m_Extents = new Vector3(size.x / 2, size.y / 2, size.z / 2); }
        public Vector3 center => m_Center;                                          // properties, as in Unity
        public Vector3 size => new Vector3(m_Extents.x * 2, m_Extents.y * 2, m_Extents.z * 2);
        public Vector3 min => new Vector3(m_Center.x - m_Extents.x, m_Center.y - m_Extents.y, m_Center.z - m_Extents.z);
        public Vector3 max => new Vector3(m_Center.x + m_Extents.x, m_Center.y + m_Extents.y, m_Center.z + m_Extents.z);
    }

    public enum KeyCode { None = 0, F7 = 286, F8 = 287, F9 = 288 }

    public class Object { public string name { get; set; } = ""; }

    public class Component : Object
    {
        public Transform transform { get; set; }
        public List<Component> Children = new List<Component>();   // stand-in for the scene graph
        public T GetComponentInChildren<T>() where T : Component
        {
            if (this is T self) return self;
            foreach (var c in Children) { if (c is T hit) return hit; var deep = c.GetComponentInChildren<T>(); if (deep != null) return deep; }
            return null;
        }
    }

    public class Transform : Component
    {
        public Vector3 position { get; set; }
        private readonly List<Transform> _kids = new List<Transform>();
        public int childCount => _kids.Count;
        public Transform GetChild(int i) => _kids[i];
        public void Add(Transform t) => _kids.Add(t);
    }

    public class Renderer : Component { public Bounds bounds { get; set; } }
    public class SkinnedMeshRenderer : Renderer { }

    public class Texture : Object { }
    public class Texture2D : Texture { public static Texture2D whiteTexture { get; } = new Texture2D { name = "white" }; }

    public class Camera : Component
    {
        public static Camera main { get; set; }
        // Pinhole projection, camera at its transform looking down +Z. Screen origin bottom-left,
        // which is what the real WorldToScreenPoint returns and what Overlay compensates for.
        public Vector3 WorldToScreenPoint(Vector3 world)
        {
            var o = transform != null ? transform.position : new Vector3(0, 0, 0);
            float dx = world.x - o.x, dy = world.y - o.y, dz = world.z - o.z;
            if (dz <= 0f) return new Vector3(0, 0, dz);
            return new Vector3(960f + (dx / dz) * 800f, 540f + (dy / dz) * 800f, dz);
        }
    }

    public static class Time { public static float unscaledTime { get; set; } }
    public static class Screen { public static int width => 1920; public static int height => 1080; }
    public static class Input
    {
        public static readonly HashSet<KeyCode> Down = new HashSet<KeyCode>();
        public static bool GetKeyDown(KeyCode k) => Down.Contains(k);
    }
}
