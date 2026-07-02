# Death Star Ruins

A NeoForge **1.21.1** mod that summons the ruined **Death Star** as **one whole physics object**: a
battle-scarred sphere with a huge section of the top torn open, revealing a cutaway of the **second
Death Star**'s interior. It is generated procedurally, placed into the world, and handed to
[**Sable**](https://modrinth.com/mod/sable) (the physics sub-level engine behind Create
Aeronautics), which simulates the whole station as a single rigid body that falls, collides and
settles.

## How it works

1. **Generate** — `DeathStarBlueprint.build(...)` builds one connected station (deterministic from a
   seed): a **two-layer panelled hull** with the **equatorial trench** and the concave green
   **superlaser dish**, a **blown-open top** with a jagged charred rim, scattered **craters**, a
   patch of skeletal **under-construction superstructure**, and the interior below.
   Everything is built from the mod's **own blocks/textures** (hull plating, greebles, reinforced
   frame, scorched hull, Imperial interior walls/floors, reactor core/casing, superlaser lens,
   power conduit, control panels) — no re-used vanilla iron/concrete.
2. **Place & assemble** — the whole station is placed around the target point and passed to
   `SubLevelAssemblyHelper.assembleBlocks(...)` as a **single assembly**, which lifts it out of the
   world into one sub-level with one physics body.

### The Death Star II interior

Revealed through the blown-open top, laid out to match *Return of the Jedi*, top to bottom:

- the **Emperor's throne room** near the top — a circular room with a panoramic **viewport**, the
  raised **throne dais** with console arms, and the turbolift door in the floor,
- the **turbolift shaft** dropping down to the core,
- the **main reactor chamber** at the core — a hollow chamber, open at the top, with the glowing
  **reactor core** suspended on support **struts** and ringed by **catwalks**, and
- the **reactor-shaft tunnel** bored out through the chamber wall (the run the Falcon flew).

The server only does generation + assembly; **Sable owns the rendering and physics**, which is why
this mod ships no custom entity or renderer.

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

Signs scattered through the interior, themed to the Battle of Endor / Death Star II:

- the **throne room** plaque (*"Now, young Skywalker, you will die"*),
- **"MAIN REACTOR — aim for the core"** by the reactor,
- **"IT'S A TRAP!"** (Ackbar),
- **"MANY BOTHANS died..."** with a nod to Lucas's **1138**, and
- a **"DEATH STAR II — shield still operational"** sign.

## Performance note

The default `radius` is **26**: one whole body of **~19,000 blocks**. Because it is a single sphere
now (a two-layer shell), the count is higher than a shard field — drop `radius` to ~22–24 for a
lighter station (~13–16k), or push it up for a bigger, more detailed one (radius 30 ≈ 25k). The
initial assembly is a one-off spike.

**Note on a spherical body:** a hollow sphere can roll. It spawns already resting on the ground
with (almost) no velocity so it should settle in place, but on a slope or if nudged it may roll —
summon it on flat, open ground. If rolling is a problem, tell me and I can flatten the base or weld
it in place.

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
