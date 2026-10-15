# Build checks T-AUTO-10 / T-AUTO-11
param(
    [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path,
    [string]$BootDir = 'simple-boot-new',
    [switch]$SkipWeb
)

$ErrorActionPreference = "Stop"
$boot = Join-Path $RepoRoot $BootDir
$web = Join-Path $RepoRoot "simple-boot-web"
$outDir = Join-Path $PSScriptRoot "out"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("Build report $(Get-Date -Format o)")
$lines.Add("RepoRoot=$RepoRoot")
$lines.Add("BootDir=$BootDir")

Write-Host "T-AUTO-10 mvn package..."
Push-Location $boot
try {
    & mvn -pl simple-server -am clean package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw "mvn failed with $LASTEXITCODE" }
    $lines.Add("T-AUTO-10=PASS")
} finally {
    Pop-Location
}
Write-Host "T-AUTO-10 PASS"

if ($SkipWeb) {
    $lines.Add("T-AUTO-11=SKIP")
    $lines.Add("Result=PASS")
    [System.IO.File]::WriteAllLines((Join-Path $outDir "build-report.txt"), $lines)
    Write-Host "T-AUTO-11 skipped (-SkipWeb)"
    exit 0
}

Write-Host "T-AUTO-11 pnpm ts:check + build:local..."
Push-Location $web
try {
    if (-not (Test-Path "node_modules")) {
        & pnpm i
        if ($LASTEXITCODE -ne 0) { throw "pnpm i failed" }
    }
    & pnpm ts:check
    if ($LASTEXITCODE -ne 0) { throw "ts:check failed" }
    & pnpm build:local
    if ($LASTEXITCODE -ne 0) { throw "build:local failed" }
    $lines.Add("T-AUTO-11=PASS")
} finally {
    Pop-Location
}
$lines.Add("Result=PASS")
[System.IO.File]::WriteAllLines((Join-Path $outDir "build-report.txt"), $lines)
Write-Host "T-AUTO-11 PASS"
exit 0
