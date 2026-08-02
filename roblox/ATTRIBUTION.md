# Attribution

## SWAT Operator character model and rifle

**"S.W.A.T. Operator – Remastered"** by **SpatialNeglect** (Sketchfab user `@jeandiz`)

- Source: https://skfb.ly/oATOX
- Licence: **Creative Commons Attribution (CC-BY 4.0)**

Everything under `assets/swat_operator.fbx`, and everything generated from it
(`src/ReplicatedStorage/Modules/SoldierRigData.lua`,
`src/ReplicatedStorage/Modules/KSVRMesh.lua`), derives from this model.

### What CC-BY requires of the game

Attribution has to be **visible to players**, not just present in the repo. Put a
credit somewhere a player can reach — the main menu, a settings/credits panel, or
the game's Roblox description page:

> Character model: "S.W.A.T. Operator – Remastered" by SpatialNeglect (@jeandiz),
> licensed under CC-BY 4.0 — https://skfb.ly/oATOX

`Hud.lua` is the natural place to surface it in-game.

### The "NoAI" tag

The Sketchfab listing carries Sketchfab's **NoAI** marker, which states the model
"may not be used in datasets for, in the development of, or as inputs to
generative AI programs."

Worth knowing what was and was not done here. The conversion pipeline in `tools/`
is deterministic geometry processing — an FBX parser, matrix math, and mesh
decimation. The model was not used as training data and no part of it was fed to
a model to generate anything. But the tag exists and it is the creator's stated
wish, so the call on whether this use sits inside it is yours to make, not one to
be made silently on your behalf.

If you would rather not ship this model, the code does not depend on it. Every
module reads its proportions from `SoldierRigData.lua`; regenerate that file from
a different rigged FBX with `python3 tools/convert_soldier.py --fbx yourmodel.fbx`
and the animation, IK and grip systems carry over unchanged.

## Textures

Textures are not in the FBX and are not redistributed here. Download them from the
Sketchfab page and apply them after importing (see `README.md`).
