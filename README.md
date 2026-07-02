# Death Star Ruins

A NeoForge **1.21.1** mod that summons the **Death Star wreckage** as real **physics objects** —
styled after the Kef Bir sea-wreck in *The Rise of Skywalker*: not one intact sphere, but a
**scattered field of separate broken pieces**. Each shard is generated procedurally, placed into
the world, and handed to [**Sable**](https://modrinth.com/mod/sable) (the physics sub-level engine
behind Create Aeronautics), which simulates **each piece as its own rigid body** that falls,
collides and settles.

## How it works

1. **Generate** — `DeathStarBlueprint.buildWreckField(...)` builds a field of independent pieces
   (deterministic from a seed):
   - a **big "hero" hull shard** — a great curved slab of two-layer panelled hull with a torn,
     charred edge, a slice of the **equatorial trench**, and a chunk of exposed **interior** still
     clinging to it (reactor stub, structural girder, decks, and the Easter-egg rooms),
   - the **superlaser-dish section** (the concave green "eye") torn free,
   - a few **medium curved hull shards** with ragged edges and exposed frame, and
   - smaller **twisted debris** chunks.
   Everything is built from the mod's **own blocks/textures** (hull plating, greebles, reinforced
   frame, scorched hull, Imperial interior walls/floors, reactor core/casing, superlaser lens,
   power conduit, control panels) — no re-used vanilla iron/concrete.
2. **Place & assemble each piece** — every piece is placed at its own scattered offset and its
   positions passed to `SubLevelAssemblyHelper.assembleBlocks(...)`, which lifts them out of the
   world into a **separate sub-level** with its own physics body. Pieces are done one at a time, so
   their blocks never coexist in the world.

The server only does generation + assembly; **Sable owns the rendering and physics** of each piece,
which is why this mod ships no custom entity or renderer.

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
| `radius` | `40` | Outer radius in blocks. Two-layer hull, so it scales fast: ~32 → ~30k, ~40 → ~48k, ~45 → ~58k blocks. |
| `shellThickness` | `2` | Hull skin thickness. |
| `maxBlocks` | `90000` | Hard safety cap; a summon over this is refused rather than locking the server. |
| `dropHeight` | `3` | Blocks above ground it spawns before physics takes over (0 = already resting). |

## Easter eggs

Tucked into the exposed interior clinging to the **big hero hull shard** are little Star Wars nods,
labelled with signs:

- **Detention Block AA-23** with barred cells and **cell 1138** (the THX-1138 / *A New Hope* cell).
- The **garbage masher 3263827** (the number Han yells).
- **Tractor beam control** ("1 of 7") — Obi-Wan's objective.
- The **Emperor's throne room** (a *Return of the Jedi* / Death Star II nod).
- Scattered quips: **"IT'S A TRAP!"**, **"I have a bad feeling about this..."**, and a
  **"THAT'S NO MOON."** plaque near the trench.
- The **thermal exhaust port** on the trench, wired down a conduit shaft to the reactor.

## Performance note

At the default `radius = 40` the wreck is **~24,000 blocks spread across ~9 separate pieces** (the
biggest ~13k, a few ~3k shards, plus small debris). Splitting it into pieces is easier on the
physics engine than one giant body, but the initial assembly is still a one-off spike. Drop
`radius` to ~24–32 for smaller shards, or push it higher for bigger ones. The debris field spreads
roughly `1.5 × radius` blocks around the summon point, so give it some open, flat space.

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
