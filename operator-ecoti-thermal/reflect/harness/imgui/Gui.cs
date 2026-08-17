using System.Collections.Generic;
namespace UnityEngine
{
    public static class GUI
    {
        public static Color color { get; set; }
        // Every draw call is recorded so the harness can assert on what the overlay produced.
        public static readonly List<string> Calls = new List<string>();
        public static void DrawTexture(Rect position, Texture image) => Calls.Add($"DrawTexture {position} {color}");
        public static void Label(Rect position, string text) => Calls.Add($"Label {position} \"{text}\"");
    }
}
