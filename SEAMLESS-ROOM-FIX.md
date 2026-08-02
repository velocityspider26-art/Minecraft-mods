# Seamless room transition fix 1

This build targets Minecraft 1.21.1 / NeoForge 21.1.234.

## What changed

- Removed the legacy `MinecraftMixin#setScreen` hijack that replaced NeoForge's
  registered seamless receiving screen with `TransitionScreen`.
- Removed the framebuffer capture from the dimension handoff path.
- Registered the invisible receiving screen as a conditional effect for every
  ordered pair of built-in Genesis rooms, including Earth (the overworld).
- Centralized the built-in room list in `SubDimensions` so renderer reuse and
  screen registration cannot silently drift apart.
- Kept the existing render-section grid reuse and added timings around
  `LevelRenderer.allChanged` and the complete `Minecraft.setLevel` handoff.

## Test jar

`dist/genesis-1.21.1-0.8.0-port-seamless-roomfix1.jar`

Remove the older Genesis jar from the mods folder before testing. Do not load
both jars together because they share the same mod id.

## Expected log lines

```text
[TRANSITION] registered 30 seamless room-to-room screen routes
[VIEWAREA] reused ... sections crossing into ...
[VIEWAREA] room-grid handoff completed in ... ms
[TRANSITION] client room handoff ... -> ... completed in ... ms
```

A clean crossing should not produce a `[WATCHDOG]` dump. If it does, preserve
`run/logs/latest.log`; the first watchdog dump is the evidence needed for the
next fix.

## Verification performed here

The four edited classes were compiled with Java 21 against the project's bundled
NeoForge 21.1.234 merged Minecraft jar. The normal Gradle wrapper could not run
because its Gradle 8.14 distribution was not cached and network access from the
build shell was unavailable. This is therefore a bytecode-validated hotfix, not
a completed in-game verification.
