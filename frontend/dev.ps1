# Start frontend. Prefer portable Node 24 when system Node < 20.19.
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

function Resolve-Node {
    $candidates = @()
    $cmd = Get-Command node -ErrorAction SilentlyContinue
    if ($cmd) {
        $candidates += $cmd.Source
    }
    $portable = Join-Path $env:LOCALAPPDATA "node-v24.21.0\node-v24.21.0-win-x64\node.exe"
    if (Test-Path $portable) {
        $candidates += $portable
    }
    foreach ($exe in $candidates) {
        $verText = & $exe -v
        if ($verText -match '^v(\d+)\.(\d+)') {
            $major = [int]$Matches[1]
            $minor = [int]$Matches[2]
            if ($major -gt 20 -or ($major -eq 20 -and $minor -ge 19)) {
                return $exe
            }
        }
    }
    throw "Node.js >= 20.19 not found. Install Node 20+ and retry."
}

$node = Resolve-Node
$nodeDir = Split-Path $node
$env:PATH = "$nodeDir;$env:PATH"
$env:COREPACK_ENABLE_DOWNLOAD_PROMPT = "0"

if (-not (Test-Path ".\node_modules\vite\bin\vite.js")) {
    Write-Host "Dependencies missing, running pnpm install..."
    $corepack = Join-Path $nodeDir "corepack.cmd"
    if (-not (Test-Path $corepack)) {
        throw "corepack not found. Install Node 20+ and retry."
    }
    & $corepack pnpm@9 install
    if ($LASTEXITCODE -ne 0) {
        throw "pnpm install failed, exit code $LASTEXITCODE"
    }
}

$port = 5173
if ($args.Count -gt 0 -and $args[0] -match '^\d+$') {
    $port = $args[0]
}

Write-Host "Node: $(& $node -v)"
Write-Host "URL: http://localhost:$port/"
& $node ".\node_modules\vite\bin\vite.js" --mode env.local --port $port --strictPort