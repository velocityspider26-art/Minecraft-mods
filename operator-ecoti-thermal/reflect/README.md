# Reflection build

`EcotiThermal.dll` in the parent folder is built from this project.

Same mod, same gates, same `SessionGate.cs` (byte-identical to the standard build). The only
difference is that every UnityEngine call is resolved at runtime by reflection through `U.cs`
instead of being compiled against the Il2Cpp interop assemblies.

That exists for one reason: the interop assemblies are generated from an installed copy of the
game, so the standard build can only be compiled on a machine that has OPERATOR. This one
references `MelonLoader.dll` and nothing else, so it can be built anywhere — including by someone
without the game.

**This build has never been run.** It compiles, it loads (verified: it references only MelonLoader,
System.Runtime and System.Collections), and the game-facing logic is the same code as the tested
build. What is unverified is the reflection layer in `U.cs` — whether every UnityEngine member
resolves and whether the boxed-struct marshalling across the Il2CppInterop boundary behaves.

`U.SelfTest()` runs at startup and on the dump key, and prints an ok/MISS table for every member it
needs. If the overlay does not draw, that table names the failure.

Prefer the standard build in `../` if you have the game — it is the normal, faster, better-tested
path. This one is the fallback.

## Building

MelonLoader.dll is the only reference and is not committed here. Fetch it once:

```bash
curl -sSL -o /tmp/ml.zip \
  https://github.com/LavaGang/MelonLoader/releases/latest/download/MelonLoader.x64.zip
unzip -j /tmp/ml.zip 'MelonLoader/net6/MelonLoader.dll' -d lib/
```

Then:

```bash
dotnet build -c Release
```

No game required. Output lands in `bin/Release/net6.0/EcotiThermal.dll`.
