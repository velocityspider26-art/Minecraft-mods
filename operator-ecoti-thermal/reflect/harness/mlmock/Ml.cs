// Mock MelonLoader for the harness only. Signatures mirror the real MelonLoader 0.7.3 surface
// (verified by dumping it): lifecycle methods live on MelonBase, CreateEntry takes the
// display_name/description optionals. The shipped DLL is compiled against the real assembly;
// this exists purely so the logic can be executed outside the loader, which the real one refuses.
using System;
using System.Collections.Generic;

namespace MelonLoader
{
    public class MelonLogger
    {
        public static void Msg(string txt) => Console.WriteLine("    | " + txt);
        public static void Msg(object obj) => Msg(obj?.ToString() ?? "");
        public static void Warning(string txt) => Console.WriteLine("    | WARN  " + txt);
        public static void Error(string txt) => Console.WriteLine("    | ERROR " + txt);
        public static void Error(string txt, Exception ex) => Console.WriteLine("    | ERROR " + txt + " :: " + ex.Message);

        public class Instance
        {
            public void Msg(string txt) => MelonLogger.Msg(txt);
            public void Warning(string txt) => MelonLogger.Warning(txt);
            public void Error(string txt) => MelonLogger.Error(txt);
        }
    }

    public class MelonPreferences_Entry<T> { public T Value { get; set; } }

    public class MelonPreferences_Category
    {
        private readonly Dictionary<string, object> _entries = new Dictionary<string, object>();
        public MelonPreferences_Entry<T> CreateEntry<T>(string identifier, T default_value,
            string display_name = null, string description = null, bool is_hidden = false,
            bool dont_save_default = false, object validator = null)
        {
            var e = new MelonPreferences_Entry<T> { Value = default_value };
            _entries[identifier] = e;
            return e;
        }
    }

    public static class MelonPreferences
    {
        public static MelonPreferences_Category CreateCategory(string identifier, string display_name = null)
            => new MelonPreferences_Category();
    }

    public abstract class MelonBase
    {
        public MelonLogger.Instance LoggerInstance { get; } = new MelonLogger.Instance();
        public virtual void OnInitializeMelon() { }
        public virtual void OnUpdate() { }
        public virtual void OnGUI() { }
    }

    public abstract class MelonMod : MelonBase
    {
        public virtual void OnSceneWasLoaded(int buildIndex, string sceneName) { }
    }

    [AttributeUsage(AttributeTargets.Assembly, AllowMultiple = true)]
    public class MelonInfoAttribute : Attribute
    { public MelonInfoAttribute(Type type, string name, string version, string author = null) { } }

    [AttributeUsage(AttributeTargets.Assembly, AllowMultiple = true)]
    public class MelonGameAttribute : Attribute
    { public MelonGameAttribute(string developer = null, string gameName = null) { } }
}
