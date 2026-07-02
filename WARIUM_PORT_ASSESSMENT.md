# Warium → NeoForge 1.21.1 Port: Feasibility Assessment

**Date:** 2026-07-02
**Requested task:** Fully port the mod *Warium* from Minecraft Forge 1.20.1 to NeoForge 1.21.1.
**Outcome:** The port **cannot be performed by a third party** under the mod's current licensing. This document records the research findings, the exact blockers, and the realistic paths forward.

---

## 1. What Warium is

| Question | Finding |
|---|---|
| Author | **NovaManGood2110 / "Novum"** (`buymeacoffee.com/novum33`) |
| Distribution | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/warium) (900K+ downloads) and [Modrinth](https://modrinth.com/mod/warium) |
| Latest version | **1.2.8** (published 2026-06-07), Forge **1.20.1 only** |
| Open source? | **No.** No source repository exists on GitHub, GitLab, or anywhere else that could be found. `source_url` is empty on Modrinth. |
| License | **All Rights Reserved** on both CurseForge and Modrinth. (See §3 for a conflicting metadata field.) |
| Existing ports/forks | **None.** No NeoForge or 1.21.x version or fork exists. Related projects (Valkyrien Warium, Warium-Create Bridge, Recruits Warium) are third-party *bridge addons*, not ports, and depend on the original 1.20.1 jar. |
| Toolchain | **MCreator-generated.** The jar contains `net/mcreator/crustychunks/` (internal mod id: `crusty_chunks`) and MCreator's characteristic `# Start of user code block` markers in `mods.toml`. |

## 2. Scale of the mod (from jar metadata inspection only)

Inventory of the public `Warium 1.2.8.jar` (~12.6 MB), by file listing — no code or assets were extracted or reused:

- **4,712** compiled class files (all under `net.mcreator.crustychunks`)
- **1,317** textures, **94** sound files, **1,673** JSON models, **449** blockstates
- **83** GeckoLib geo/animation files (animated entities/weapons)
- **507** recipes, **437** loot tables, **210** tags, **14** advancements, **5** NBT structures

## 3. Licensing analysis — the blocker

- Both distribution pages declare **All Rights Reserved**. Under ARR, decompiling the jar to produce a derivative work (a port) and redistributing its assets (textures, sounds, models, animations, data files) is not permitted without the author's explicit consent.
- The jar's embedded `mods.toml` declares `license="Academic Free License v3.0"` — a **direct conflict** with the storefront listings. This field sits inside an MCreator-generated template and is very likely unreviewed boilerplate rather than a deliberate grant. An ambiguous, probably-accidental metadata field is not a sound basis for redistributing a full derivative of a work the author publicly marks ARR on both platforms.
- A "clean-room" reimplementation is not a viable workaround here either: the requested deliverable ("preserve the original appearance", all weapons/entities/sounds/textures) *is* the copyrighted creative content. Recreating and redistributing that content wholesale under the same name would still infringe, and anything less would be a placeholder shell, which was explicitly not wanted.

**Conclusion:** every route to the requested deliverable passes through content the license does not permit a third party to reuse.

## 4. Dependency findings (for whoever does the port legitimately)

| Dependency | Forge 1.20.1 status | NeoForge 1.21.1 status |
|---|---|---|
| GeckoLib | Declared in `mods.toml` (description says required) | ✅ Available — GeckoLib **4.9.2** for NeoForge 1.21.1 (verified on Modrinth, 26 builds) |
| Minecraft/Forge | `[1.20.1]`, loader `[47,)` | Target: NeoForge 21.1.x (e.g. 21.1.234, already configured in this repo's MDK template) |

No other mod dependencies are declared.

## 5. Realistic paths forward

1. **The author ports it (easiest by far).** Warium is an MCreator workspace. Current MCreator releases (2024.4+) target **NeoForge 1.21.1** directly — the author can open the workspace, switch the generator target, regenerate, and fix procedure/API deltas. For anyone without the `.mcreator` workspace file, this route does not exist. Contact: Discord `discord.gg/j9a6d2UDGJ` (from the jar metadata) or `discord.gg/V7gTZBqg` (Modrinth), or via CurseForge/`buymeacoffee.com/novum33`.
2. **Obtain written permission.** If the author grants porting rights (and ideally shares the workspace), the port becomes tractable and largely mechanical via MCreator's generator migration.
3. **An original mod.** An independently designed warfare-tech mod for NeoForge 1.21.1 with original assets, mechanics, and names is fully legal — but it would be a new mod, not Warium.

## 6. What was verified in this session

- ✅ Searched CurseForge, Modrinth, GitHub, and the open web for source repositories, forks, and existing ports — none exist.
- ✅ Confirmed author, license listings, version history, and platform metadata via the Modrinth API.
- ✅ Downloaded the public jar and inspected **metadata only** (`mods.toml`, file listing) to determine dependencies, toolchain, and scale.
- ✅ Verified GeckoLib availability for NeoForge 1.21.1.
- ❌ No decompilation, asset extraction, or code reuse was performed — the license does not permit it.

---

*This repository currently contains the unmodified NeoForge 1.21.1 MDK template. No Warium content has been added to it.*
