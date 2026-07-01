# Death Star Ruins

A NeoForge **1.21.1** mod that summons a scaled-down, battle-scarred **Death Star** as a real
**physics object** — not a static build. The wreck is generated procedurally, placed into the
world, and then handed to [**Sable**](https://modrinth.com/mod/sable) (the physics sub-level
engine behind Create Aeronautics), which takes over and simulates it as a rigid body that tumbles,
collides with terrain, and settles.

## How it works

1. **Generate** — `DeathStarBlueprint` builds a hollow, panelled sphere and carves the three
   features that make the silhouette read as a Death Star:
   - the **equatorial trench** (dark recessed band around the middle),
   - the **superlaser dish** (the concave "eye" on the upper hemisphere), and
   - **battle damage** — impact craters that punch holes clean through the hull and expose the
     charred internal frame around their rims, so it looks like *ruins*.
   Generation is deterministic from a seed.
2. **Place** — every voxel is written into the world around the target point.
3. **Assemble** — the exact set of placed positions is passed to
   `SubLevelAssemblyHelper.assembleBlocks(...)`. Sable lifts the blocks out of the world into a
   sub-level and registers a physics body for them. From that moment the Death Star is a moving,
   interactive physics object.

The server only does generation + assembly; **Sable owns the rendering and physics** of the
result, which is why this mod ships no custom entity or renderer.

## Requirements

- Minecraft **1.21.1** + NeoForge **21.1.x**
- **[Sable](https://modrinth.com/mod/sable) 2.0.0+** (required — this is where the physics body
  lives). Sable in turn pulls in Create and its dependencies.

## Usage

Summon with either:

- **Death Star Core** item (in the *Death Star Ruins* creative tab): right-click a block and the
  ruined Death Star assembles resting on that spot. Consumed on use outside creative.
- **Command:** `/deathstar summon [radius]` (op / permission level 2). Spawns it out in front of
  you, resting on the terrain there. `radius` is optional and overrides the config default.

## Configuration

`config/deathstar-server.toml`:

| Option | Default | Notes |
| --- | --- | --- |
| `radius` | `45` | Outer radius in blocks. Block count scales with radius²: ~32 → ~25k, ~45 → ~50k. |
| `shellThickness` | `2` | Hull skin thickness. |
| `maxBlocks` | `60000` | Hard safety cap; a summon over this is refused rather than locking the server. |
| `dropHeight` | `3` | Blocks above ground it spawns before physics takes over (0 = already resting). |
| `reactorCore` | `true` | Places a small solid glowing core at the centre (mass anchor). |

## Performance note

At the default `radius = 45` the Death Star is **~50,000 blocks in a single physics body**. That
is intentional (it's what was asked for) but it is genuinely heavy for the physics engine and the
initial assembly is a one-off spike. If you want something smoother to fly around, drop `radius`
to ~24–32 in the config.

## Building

```bash
./gradlew build
```

The output jar is `build/libs/deathstar-1.0.0.jar`. Sable is resolved from
`https://maven.ryanhcode.dev/releases` as a **compile-only** dependency (the API is compiled
against; the actual mod is provided at runtime by the player's install).

## Status / testing

The mod **compiles against Sable's real API** and builds a valid jar. It has **not** been
run inside a live Minecraft client in this environment (no game runtime here), so the in-game
behaviour — assembly of a 50k-block body, physics tuning, exact look — should be verified in a
dev instance with Sable + Create installed. The integration point (`assembleBlocks`) and all
NeoForge registrations are the parts to sanity-check first.
