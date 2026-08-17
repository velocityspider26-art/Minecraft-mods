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

## Harness

`harness/run.sh` runs the reflection layer against mock UnityEngine and OPERATOR assemblies shaped
like Il2CppInterop's output — same type names, same member kinds (field vs property), same
signatures. Needs only the .NET SDK; no game, no MelonLoader.

```bash
cd harness && ./run.sh      # 30/30 expected
```

It verifies:

- module force-load, **including the by-path fallback** — `UnityEngine.IMGUIModule` is copied into
  the output without being referenced, so finding it is the thing under test
- type and member resolution, boxed-struct construction and field reads through reflection
- the solo gate across all four connection states, including a player joining and leaving mid-session
- quad-tube discrimination (2 lenses vs 4), dead and friendly filtering, ECOTI name matching
- the projection maths: a 1.8 m contact at 10 m yields a 144 px box (1.8 / 10 × 800 focal),
  at 40 m a 36 px box, both centred correctly; contacts behind the camera are rejected

It cannot verify that real Il2CppInterop names things this way, or that native marshalling behaves.
Those still need a live build.

Note `EnableDefaultCompileItems` is **false** in the csproj on purpose. The default glob would pull
`harness/mlmock/Ml.cs` — a mock MelonLoader that exists only so the harness can run outside the
loader — into the shipped assembly, which would then bind to fake types and fail to load.
