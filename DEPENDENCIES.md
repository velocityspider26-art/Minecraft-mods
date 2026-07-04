# Dependencies

All coordinates were verified to exist on the listed mavens before use. No versions are invented.

## Platform

| Dependency | Coordinate | Version | Maven | Scope |
|-----------|-----------|---------|-------|-------|
| Minecraft | — | 1.21.1 | (moddev) | — |
| NeoForge | `net.neoforged:neoforge` | 21.1.234 | maven.neoforged.net | provided |
| Java | — | 21 | — | — |
| Parchment | `org.parchmentmc.data` | 1.21.1 / 2024.11.17 | maven.parchmentmc.org | mappings |

## Create Aeronautics stack (replaces Valkyrien Skies)

| Dependency | Coordinate | Version | Maven | Scope |
|-----------|-----------|---------|-------|-------|
| Sable (physics engine) | `dev.ryanhcode.sable:sable-neoforge-1.21.1` | 2.0.3 | maven.ryanhcode.dev/releases | implementation |
| Sable companion (math) | `dev.ryanhcode.sable-companion:sable-companion-common-1.21.1` | 1.6.0 | maven.ryanhcode.dev/releases | (transitive) |
| Create Aeronautics — simulated | `dev.simulated_team.simulated:simulated-neoforge-1.21.1` | 1.3.0 | maven.ryanhcode.dev/releases | localRuntime |
| Create Aeronautics — aeronautics | `dev.eriksonn.aeronautics:aeronautics-neoforge-1.21.1` | 1.3.0 | maven.ryanhcode.dev/releases | localRuntime |
| Create | `com.simibubi.create:create-1.21.1` | 6.0.10-280 | maven.createmod.net | compileOnly (`:slim`) + localRuntime |
| Ponder | `net.createmod.ponder:ponder-neoforge` | 1.0.82+mc1.21.1 | maven.createmod.net | compileOnly |
| Flywheel | `dev.engine-room.flywheel:flywheel-neoforge-api-1.21.1` | 1.0.6 | maven.createmod.net | compileOnly |
| Veil (Sable dep) | `foundry.veil:veil-neoforge-1.21.1` | 4.1.4 | maven.blamejared.com | (transitive) |

## Other Genesis dependencies

| Dependency | Coordinate | Version | Maven | Scope |
|-----------|-----------|---------|-------|-------|
| Registrate (fluids) | `com.tterrag.registrate:Registrate` | MC1.21-1.3.0+67 | mvn.devos.one/snapshots | implementation (jarJar) |
| Pehkui (entity scaling) | `maven.modrinth:pehkui` | 3.8.3+1.21-neoforge | api.modrinth.com/maven | implementation |
| Lodestone (client) | `maven.modrinth:lodestonelib` | 1.8.2 | api.modrinth.com/maven | implementation |
| MixinSquared | `com.github.bawnorton.mixinsquared:mixinsquared-neoforge` | 0.3.7-beta.3 | maven.bawnorton.com/releases | implementation (jarJar) |
| JOML primitives | `org.joml:joml-primitives` | 1.10.0 | Maven Central | implementation (jarJar) |
| Earcut4j | `org.maplibre:earcut4j` | 3.0.0 | Maven Central | implementation (jarJar) |

## Removed (were in the 1.20.1 / VS build)

* `org.valkyrienskies:*` (VS2 + VS-core) — **Valkyrien Skies, replaced by Create Aeronautics**
* `maven.modrinth:vlib`, `maven.modrinth:zps`, `maven.modrinth:zpl` — VS-ecosystem addons
* `thedarkcolour:kotlinforforge`, `dev.architectury:architectury` — VS/vlib transitive requirements
* `maven.modrinth:curios`, `ace.actually.radios:radios` — declared but unused in Genesis code
* `mekanism` / `mekanism-generators` — optional compat, removed (compat mixins dropped)

## Runtime dependency exclusions

Create, `simulated` and `aeronautics` over-declare optional integration mods (`top.theillusivec4.curios`,
`cc.tweaked`, `dev.ftb.mods`, `info.journeymap`, `dev.architectury`) in their POMs. Genesis does not
use any of them, so they are excluded from the dev runtime classpath in `build.gradle`.
