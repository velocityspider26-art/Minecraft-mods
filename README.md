# Death Star Ruins

A NeoForge **1.21.1** mod that summons a scaled-down, battle-scarred **Death Star** as a real
**physics object** — not a static build. The wreck is generated procedurally, placed into the
world, and then handed to [**Sable**](https://modrinth.com/mod/sable) (the physics sub-level
engine behind Create Aeronautics), which takes over and simulates it as a rigid body that tumbles,
collides with terrain, and settles.

## How it works

1. **Generate** — `DeathStarBlueprint` builds a wreck (deterministic from a seed):
   - a **two-layer panelled hull** with the **equatorial trench** and the concave **superlaser
     dish** ("eye"),
   - a **huge blown-open section** with a jagged, charred rim that tears away ~15% of the sphere and
     exposes a cutaway of the interior — this is what makes it read as *ruins* rather than a moon,
   - an **accurate-feeling interior**: a glowing **main reactor** at the core, the vertical
     **reactor/exhaust shaft**, the **superlaser focusing tube** running out to the dish, exposed
     **decks** and structural **girders**, and
   - **battle-damage craters** across the rest of the skin.
   It's built from the mod's **own blocks/textures** (hull plating, greebles, reinforced frame,
   scorched hull, Imperial interior walls/floors, reactor core/casing, superlaser lens, power
   conduit, control panels) — no re-used vanilla iron/concrete.
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
| `radius` | `40` | Outer radius in blocks. Two-layer hull, so it scales fast: ~32 → ~30k, ~40 → ~48k, ~45 → ~58k blocks. |
| `shellThickness` | `2` | Hull skin thickness. |
| `maxBlocks` | `90000` | Hard safety cap; a summon over this is refused rather than locking the server. |
| `dropHeight` | `3` | Blocks above ground it spawns before physics takes over (0 = already resting). |

## Easter eggs

Tucked into the exposed decks (visible through the blown-open section) are little Star Wars nods,
labelled with signs:

- **Detention Block AA-23** with barred cells and **cell 1138** (the THX-1138 / *A New Hope* cell).
- The **garbage masher 3263827** (the number Han yells).
- **Tractor beam control** ("1 of 7") — Obi-Wan's objective.
- The **Emperor's throne room** (a *Return of the Jedi* / Death Star II nod).
- Scattered quips: **"IT'S A TRAP!"**, **"I have a bad feeling about this..."**, and a
  **"THAT'S NO MOON."** plaque near the trench.
- The **thermal exhaust port** on the trench, wired down a conduit shaft to the reactor.

## Performance note

At the default `radius = 40` the Death Star is **~48,000 blocks in a single physics body** — right
around the size that was asked for. It is still genuinely heavy for the physics engine and the
initial assembly is a one-off spike. If you want something smoother to fly around, drop `radius`
to ~24–32 in the config; push it to 45+ for a ~58k-block monster.

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
