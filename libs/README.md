# Dependency jars

These are the **real distributed builds**, used so the addon compiles and runs against actual APIs
instead of hand-written stubs. They are `compileOnly` + `localRuntime` in `build.gradle`; none of
their classes are packaged into `create-jet-engines-*.jar`.

| File | Version | Where it came from |
|---|---|---|
| `sable-neoforge-1.21.1-2.0.3.jar` | 2.0.3 | Modrinth project `sable` |
| `sable-companion-common-1.21.1-1.6.0.jar` | 1.6.0 | JarJar'd inside the Sable jar |
| `veil-neoforge-1.21.1-4.1.4.jar` | 4.1.4 | JarJar'd inside the Sable jar |
| `sable-rapier-1.21.1-2.0.3.jar` | 2.0.3 | JarJar'd inside the Sable jar (physics backend) |
| `create-aeronautics-bundled-1.21.1-1.3.0.jar` | 1.3.0 | Modrinth project `create-aeronautics` — reference only, not on the compile classpath |

Veil is on the compile classpath because `ForceGroups.PROPULSION` is typed as
`foundry.veil.platform.registry.RegistryObject`, so calling `.get()` on it needs Veil present.

To refresh them, download the same versions from Modrinth and extract the nested jars from
`META-INF/jarjar/` inside the Sable jar.

**Licences:** Sable is PolyForm Shield 1.0.0; Create Aeronautics / Simulated code is MIT with
All-Rights-Reserved assets. Nothing from either is redistributed here — these files are local build
inputs only and are not published with the mod.
