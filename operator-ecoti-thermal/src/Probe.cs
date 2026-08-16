using System;
using System.Collections.Generic;
using System.Reflection;
using MelonLoader;

namespace OperatorEcotiThermal
{
    /// <summary>
    /// Late-bound reads against Il2Cpp types.
    ///
    /// The toolkit's field notes are blunt about this: "compiling is not evidence". A successful
    /// build proves an interop member exists, not that it is static at runtime, not that it is a
    /// property rather than a field, and not that it is populated when read. Interop generation
    /// also moves members between field and property form between game builds.
    ///
    /// So every member this mod does not strictly control is read by name, property first then
    /// field, with the accessor cached after the first resolve and the miss logged exactly once.
    /// A miss returns the caller's fallback. Since every gate in this mod fails closed, a miss
    /// turns the overlay off rather than leaving it on with a wrong answer.
    /// </summary>
    internal static class Probe
    {
        private static readonly Dictionary<string, PropertyInfo> _props = new Dictionary<string, PropertyInfo>();
        private static readonly Dictionary<string, FieldInfo> _fields = new Dictionary<string, FieldInfo>();
        private static readonly HashSet<string> _resolved = new HashSet<string>();
        private static readonly HashSet<string> _warned = new HashSet<string>();

        private const BindingFlags StaticFlags =
            BindingFlags.Static | BindingFlags.Public | BindingFlags.NonPublic | BindingFlags.FlattenHierarchy;

        private const BindingFlags InstanceFlags =
            BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic | BindingFlags.FlattenHierarchy;

        /// <summary>Read a static member. Returns <paramref name="fallback"/> on any miss.</summary>
        public static T Static<T>(Type type, string member, T fallback = default)
        {
            if (type == null) return fallback;
            var key = type.FullName + "::" + member;

            try
            {
                if (!_resolved.Contains(key))
                {
                    _resolved.Add(key);
                    var p = type.GetProperty(member, StaticFlags);
                    if (p != null && p.CanRead) _props[key] = p;
                    else
                    {
                        var f = type.GetField(member, StaticFlags);
                        if (f != null) _fields[key] = f;
                        else WarnOnce(key, $"no static property or field named '{member}' on {type.Name}");
                    }
                }

                object raw = null;
                if (_props.TryGetValue(key, out var prop)) raw = prop.GetValue(null);
                else if (_fields.TryGetValue(key, out var fld)) raw = fld.GetValue(null);
                else return fallback;

                return Coerce(raw, fallback);
            }
            catch (Exception e)
            {
                WarnOnce(key, $"read failed: {e.Message}");
                return fallback;
            }
        }

        /// <summary>Read an instance member off <paramref name="target"/>. Returns fallback on any miss.</summary>
        public static T Instance<T>(object target, string member, T fallback = default)
        {
            if (target == null) return fallback;
            var type = target.GetType();
            var key = type.FullName + "#" + member;

            try
            {
                if (!_resolved.Contains(key))
                {
                    _resolved.Add(key);
                    var p = type.GetProperty(member, InstanceFlags);
                    if (p != null && p.CanRead) _props[key] = p;
                    else
                    {
                        var f = type.GetField(member, InstanceFlags);
                        if (f != null) _fields[key] = f;
                        else WarnOnce(key, $"no instance property or field named '{member}' on {type.Name}");
                    }
                }

                object raw = null;
                if (_props.TryGetValue(key, out var prop)) raw = prop.GetValue(target);
                else if (_fields.TryGetValue(key, out var fld)) raw = fld.GetValue(target);
                else return fallback;

                return Coerce(raw, fallback);
            }
            catch (Exception e)
            {
                WarnOnce(key, $"read failed: {e.Message}");
                return fallback;
            }
        }

        /// <summary>Call an instance method by name. Returns fallback on any miss or throw.</summary>
        public static T Call<T>(object target, string method, T fallback, params object[] args)
        {
            if (target == null) return fallback;
            var type = target.GetType();
            var key = type.FullName + "()" + method + "/" + (args?.Length ?? 0);

            try
            {
                var types = new Type[args?.Length ?? 0];
                for (int i = 0; i < types.Length; i++) types[i] = args[i]?.GetType() ?? typeof(object);

                var mi = type.GetMethod(method, InstanceFlags, null, types, null)
                         ?? type.GetMethod(method, InstanceFlags);

                if (mi == null)
                {
                    WarnOnce(key, $"no instance method named '{method}' on {type.Name}");
                    return fallback;
                }

                return Coerce(mi.Invoke(target, args), fallback);
            }
            catch (Exception e)
            {
                WarnOnce(key, $"call failed: {e.Message}");
                return fallback;
            }
        }

        /// <summary>Resolve an Il2Cpp type by name across every loaded assembly. Null on miss.</summary>
        public static Type FindType(string fullName)
        {
            try
            {
                var t = Type.GetType(fullName, false);
                if (t != null) return t;

                foreach (var asm in AppDomain.CurrentDomain.GetAssemblies())
                {
                    t = asm.GetType(fullName, false);
                    if (t != null) return t;
                }
            }
            catch { /* assembly enumeration can throw on malformed native neighbours */ }

            WarnOnce("type::" + fullName, $"type not found: {fullName}");
            return null;
        }

        /// <summary>
        /// Find a component under <paramref name="root"/> by runtime type name.
        ///
        /// Deliberately not GetComponentInChildren(Il2CppType.From(t)). Going through Il2CppType
        /// means committing to an exact namespace at the call site, and the interop namespace for
        /// a given game type is a guess until you have read it off a live build. Matching the
        /// short type name off the components that are actually there sidesteps that, and it costs
        /// nothing because every caller caches the result.
        ///
        /// Written against Length/indexer rather than foreach so it compiles the same against a
        /// plain Component[] and against Il2CppReferenceArray&lt;Component&gt;.
        /// </summary>
        public static object FindComponent(object root, string typeName)
        {
            var component = root as UnityEngine.Component;
            if (component == null || string.IsNullOrEmpty(typeName)) return null;

            // Accept either "TeamIdentifier" or "Il2Cpp.TeamIdentifier" from the caller.
            var shortName = typeName;
            var dot = shortName.LastIndexOf('.');
            if (dot >= 0 && dot < shortName.Length - 1) shortName = shortName.Substring(dot + 1);

            try
            {
                var all = component.GetComponentsInChildren<UnityEngine.Component>();
                if (all == null) return null;

                for (int i = 0; i < all.Length; i++)
                {
                    var c = all[i];
                    if (c == null) continue;
                    if (string.Equals(c.GetType().Name, shortName, StringComparison.Ordinal)) return c;
                }
            }
            catch (Exception e)
            {
                WarnOnce("find::" + shortName, $"component search failed: {e.Message}");
            }

            return null;
        }

        private static T Coerce<T>(object raw, T fallback)
        {
            if (raw == null) return fallback;
            if (raw is T typed) return typed;

            try { return (T)Convert.ChangeType(raw, typeof(T)); }
            catch { return fallback; }
        }

        private static void WarnOnce(string key, string message)
        {
            if (_warned.Contains(key)) return;
            _warned.Add(key);
            MelonLogger.Warning($"[EcotiThermal] probe: {message}");
        }
    }
}
