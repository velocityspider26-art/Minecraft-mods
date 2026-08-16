<#
    Build EcotiThermal and drop it into OPERATOR's Mods folder.

        .\deploy.ps1

    Finds the game itself by reading Steam's library config, so a non-default install path or a
    second drive needs no arguments. Override with -GameDir if you have moved things by hand.

    Every prerequisite is checked before the build runs and each failure says exactly what to do
    about it, because the alternative is a wall of MSBuild errors that mean nothing.
#>

param(
    [string]$GameDir,
    [string]$Configuration = "Release"
)

$ErrorActionPreference = "Stop"
$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$project    = Join-Path $projectDir "EcotiThermal.csproj"

function Fail($message) { Write-Host "`n$message`n" -ForegroundColor Red; exit 1 }
function Step($message) { Write-Host $message -ForegroundColor Cyan }

# ── Locate the game ───────────────────────────────────────────────────────────────────────────
function Find-Operator {
    $candidates = @()

    # Steam's own record of where it is, then every library folder it knows about.
    $steam = $null
    foreach ($key in @("HKCU:\Software\Valve\Steam", "HKLM:\SOFTWARE\WOW6432Node\Valve\Steam")) {
        try {
            $path = (Get-ItemProperty -Path $key -ErrorAction Stop).SteamPath
            if ($path) { $steam = $path.Replace('/', '\'); break }
        } catch { }
    }

    if ($steam) {
        $candidates += Join-Path $steam "steamapps\common\OPERATOR"

        $vdf = Join-Path $steam "steamapps\libraryfolders.vdf"
        if (Test-Path $vdf) {
            # Each library block carries a "path" line; appid 1913370 marks the one holding OPERATOR,
            # but checking every library on disk is cheaper than parsing the block structure.
            foreach ($line in Get-Content $vdf) {
                if ($line -match '"path"\s+"(.+?)"') {
                    $lib = $matches[1].Replace('\\', '\')
                    $candidates += Join-Path $lib "steamapps\common\OPERATOR"
                }
            }
        }
    }

    # Last resort: the usual spots on every fixed drive.
    foreach ($drive in (Get-PSDrive -PSProvider FileSystem | Where-Object { $_.Used -ne $null })) {
        $candidates += "$($drive.Name):\Program Files (x86)\Steam\steamapps\common\OPERATOR"
        $candidates += "$($drive.Name):\Steam\steamapps\common\OPERATOR"
        $candidates += "$($drive.Name):\SteamLibrary\steamapps\common\OPERATOR"
    }

    foreach ($c in ($candidates | Select-Object -Unique)) {
        if (Test-Path (Join-Path $c "OPERATOR.exe")) { return $c }
    }
    return $null
}

if (-not $GameDir) {
    Step "Looking for OPERATOR..."
    $GameDir = Find-Operator
    if (-not $GameDir) {
        Fail @"
Could not find OPERATOR automatically.

Right-click it in Steam -> Manage -> Browse local files, copy the folder path, and pass it:
  .\deploy.ps1 -GameDir "D:\SteamLibrary\steamapps\common\OPERATOR"
"@
    }
    Write-Host "  found: $GameDir" -ForegroundColor DarkGray
}

if (-not (Test-Path (Join-Path $GameDir "OPERATOR.exe"))) {
    Fail "No OPERATOR.exe in:`n  $GameDir`n`nThat is not the game folder."
}

# ── Prerequisites ─────────────────────────────────────────────────────────────────────────────
if (-not (Get-Command dotnet -ErrorAction SilentlyContinue)) {
    Fail @"
The .NET SDK is not installed. Install it, reopen the terminal, and re-run:

  winget install Microsoft.DotNet.SDK.8

(or download from https://dotnet.microsoft.com/download/dotnet/8.0)
"@
}

$sdks = & dotnet --list-sdks 2>$null
if (-not ($sdks | Where-Object { $_ -match '^([89]|[1-9]\d)\.' })) {
    Fail @"
A .NET SDK was found but it is older than 8.0, which this project needs.

  winget install Microsoft.DotNet.SDK.8

Installed: $($sdks -join '; ')
"@
}

if (-not (Test-Path (Join-Path $GameDir "MelonLoader"))) {
    Fail @"
MelonLoader is not installed in:
  $GameDir

Get it from https://github.com/LavaGang/MelonLoader/releases, point its installer at OPERATOR.exe,
then LAUNCH THE GAME ONCE and quit. That first launch is what generates the assemblies this mod
compiles against.
"@
}

$interop = Join-Path $GameDir "MelonLoader\Il2CppAssemblies"
if (-not (Test-Path $interop) -or -not (Get-ChildItem $interop -Filter *.dll -ErrorAction SilentlyContinue)) {
    Fail @"
MelonLoader is installed but has not generated its interop assemblies yet:
  $interop

LAUNCH OPERATOR ONCE and quit, then re-run this script. The first run can take several minutes
while MelonLoader processes the game; let it finish and reach the main menu before quitting.
"@
}

if (Get-Process -Name "OPERATOR" -ErrorAction SilentlyContinue) {
    Fail "OPERATOR is running, which locks the mod DLL. Close the game and re-run."
}

# ── Build ─────────────────────────────────────────────────────────────────────────────────────
Step "Building..."
& dotnet build $project -c $Configuration -p:GameDir="$GameDir" --nologo -v minimal
if ($LASTEXITCODE -ne 0) { Fail "Build failed. Paste the output above if you want a hand with it." }

$built = Join-Path $projectDir "bin\$Configuration\net6.0\EcotiThermal.dll"
if (-not (Test-Path $built)) { Fail "Build reported success but $built is missing." }

# ── Deploy ────────────────────────────────────────────────────────────────────────────────────
$modsDir = Join-Path $GameDir "Mods"
New-Item -ItemType Directory -Force -Path $modsDir | Out-Null

$target = Join-Path $modsDir "EcotiThermal.dll"
Copy-Item $built $target -Force

# Always report the deployed timestamp. Shipping a stale build because a copy silently failed is
# the single most reliable way to waste a test cycle.
$info = Get-Item $target
Write-Host ""
Write-Host "Deployed  $target" -ForegroundColor Green
Write-Host "Size      $([math]::Round($info.Length / 1KB, 1)) KB" -ForegroundColor Green
Write-Host "Built     $($info.LastWriteTime)" -ForegroundColor Green
Write-Host ""
Write-Host "F7 toggle - F8 status HUD - F9 kit dump" -ForegroundColor DarkGray
Write-Host "Solo sessions only; the overlay stays off whenever another player is connected." -ForegroundColor DarkGray
