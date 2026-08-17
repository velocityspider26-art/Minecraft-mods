#!/usr/bin/env bash
# Integration harness for the reflection build. Needs only the .NET SDK — no game, no MelonLoader.
#
# Drives the real reflect/src sources against mock UnityEngine + OPERATOR assemblies shaped like
# Il2CppInterop's output: same type names, same member kinds (field vs property), same signatures.
#
# Verifies: module force-load (including the by-path fallback, since IMGUIModule is deliberately
# copied to the output without being referenced), type and member resolution, boxed-struct
# construction and field reads, the solo gate in all four connection states, quad-tube
# discrimination, dead/friendly filtering, ECOTI matching, and the projection maths.
#
# Cannot verify: that real Il2CppInterop names things this way, or that native marshalling behaves.
set -euo pipefail
cd "$(dirname "$0")"
SRC="$(cd .. && pwd)/src"

dotnet build core/core.csproj         -c Release -v q --nologo
dotnet build imgui/imgui.csproj       -c Release -v q --nologo
dotnet build fakegame/fakegame.csproj -c Release -v q --nologo
dotnet build mlmock/mlmock.csproj     -c Release -v q --nologo
dotnet build run/run.csproj           -c Release -v q --nologo \
  -p:ReflectSrc="$SRC" \
  -p:MelonDll="$PWD/mlmock/bin/Release/net6.0/MelonLoader.dll"

OUT=run/bin/Release/net6.0
# Unreferenced on purpose: finding this is what the force-load path is being tested on.
cp imgui/bin/Release/net6.0/UnityEngine.IMGUIModule.dll "$OUT/"

cd "$OUT" && DOTNET_ROLL_FORWARD=LatestMajor dotnet harness.dll
