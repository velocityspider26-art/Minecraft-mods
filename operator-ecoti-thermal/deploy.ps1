<#
    Build and deploy EcotiThermal into the game's Mods folder.

        .\deploy.ps1
        .\deploy.ps1 -GameDir "D:\Steam\steamapps\common\OPERATOR"

    The DLL is locked while the game is running. This script refuses to copy over a locked file
    rather than failing silently, because the toolkit's field notes flag shipping a stale build as
    a reliable way to waste an entire test cycle. It prints the deployed timestamp so you can
    confirm what actually landed.
#>

param(
    [string]$GameDir = "C:\Program Files (x86)\Steam\steamapps\common\OPERATOR",
    [string]$Configuration = "Release"
)

$ErrorActionPreference = "Stop"
$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$project    = Join-Path $projectDir "EcotiThermal.csproj"

if (-not (Test-Path $GameDir)) {
    Write-Error "Game directory not found: $GameDir`nPass -GameDir with the correct path."
}

$interop = Join-Path $GameDir "MelonLoader\Il2CppAssemblies"
if (-not (Test-Path $interop)) {
    Write-Error @"
Interop assemblies not found at:
  $interop

Install MelonLoader (0.7.3 or later) and launch OPERATOR once. MelonLoader generates these
assemblies from your game build on that first run; the project references them and cannot
compile without them.
"@
}

Write-Host "Building..." -ForegroundColor Cyan
dotnet build $project -c $Configuration -p:GameDir="$GameDir"
if ($LASTEXITCODE -ne 0) { Write-Error "Build failed." }

$built = Join-Path $projectDir "bin\$Configuration\net6.0\EcotiThermal.dll"
if (-not (Test-Path $built)) { Write-Error "Build reported success but $built is missing." }

if (Get-Process -Name "OPERATOR" -ErrorAction SilentlyContinue) {
    Write-Error "OPERATOR is running and the DLL is locked. Close the game and re-run."
}

$modsDir = Join-Path $GameDir "Mods"
New-Item -ItemType Directory -Force -Path $modsDir | Out-Null

$target = Join-Path $modsDir "EcotiThermal.dll"
Copy-Item $built $target -Force

$stamp = (Get-Item $target).LastWriteTime
Write-Host "Deployed to $target" -ForegroundColor Green
Write-Host "Timestamp:   $stamp" -ForegroundColor Green
Write-Host ""
Write-Host "F7 toggle - F8 status HUD - F9 inventory dump" -ForegroundColor DarkGray
Write-Host "Solo sessions only; the overlay stays off whenever another player is connected." -ForegroundColor DarkGray
