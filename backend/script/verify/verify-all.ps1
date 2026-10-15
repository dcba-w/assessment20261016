# Entry: static + optional build + optional API
param(
    [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path,
    [string]$BootDir = "simple-boot-new",
    [switch]$SkipBuild,
    [switch]$SkipWebBuild,
    [switch]$SkipApi,
    [string]$BaseUrl = $(if ($env:VERIFY_BASE_URL) { $env:VERIFY_BASE_URL } else { "http://127.0.0.1:48080" })
)

$ErrorActionPreference = "Stop"
$failed = $false

function Run-Step([string]$name, [scriptblock]$block) {
    Write-Host ""
    Write-Host "==== $name ===="
    & $block
    if ($LASTEXITCODE -ne 0) {
        Write-Host "==== $name FAILED (exit $LASTEXITCODE) ===="
        $script:failed = $true
    } else {
        Write-Host "==== $name OK ===="
    }
}

Run-Step "01-brand-scan" { & (Join-Path $PSScriptRoot "01-brand-scan.ps1") -RepoRoot $RepoRoot -BootDir $BootDir }
Run-Step "02-structure" { & (Join-Path $PSScriptRoot "02-structure.ps1") -RepoRoot $RepoRoot -BootDir $BootDir }

if (-not $SkipBuild) {
    if ($SkipWebBuild) {
        Run-Step "03-build" { & (Join-Path $PSScriptRoot "03-build.ps1") -RepoRoot $RepoRoot -BootDir $BootDir -SkipWeb }
    } else {
        Run-Step "03-build" { & (Join-Path $PSScriptRoot "03-build.ps1") -RepoRoot $RepoRoot -BootDir $BootDir }
    }
} else {
    Write-Host "==== 03-build SKIPPED ===="
}

if (-not $SkipApi) {
    Run-Step "04-api-smoke" { & (Join-Path $PSScriptRoot "04-api-smoke.ps1") -BaseUrl $BaseUrl }
} else {
    Write-Host "==== 04-api-smoke SKIPPED ===="
}

Write-Host ""
if ($failed) {
    Write-Host "verify-all: FAILED"
    exit 1
}
Write-Host "verify-all: PASS"
exit 0
