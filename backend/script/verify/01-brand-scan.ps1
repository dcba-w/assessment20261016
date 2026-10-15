# Brand scan. Exit 0 when violations = 0.
param(
    [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path,
    [string]$BootDir = 'simple-boot-new'
)

$ErrorActionPreference = 'Stop'
$outDir = Join-Path $PSScriptRoot 'out'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$reportPath = Join-Path $outDir 'brand-report.txt'

$roots = @(
    (Join-Path $RepoRoot $BootDir),
    (Join-Path $RepoRoot 'simple-boot-web'),
    (Join-Path $RepoRoot 'simple-boot-uniapp')
)

# 中文关键词用字符码拼接。直接 [char]+[char] 会按数值相加，关键词会失效。
$kwYudao = -join ([char]0x828B, [char]0x9053)
$kwYunai = -join ([char]0x828B, [char]0x828E)
$pattern = ($kwYudao + '|' + $kwYunai + '|yudaocode|YunaiV|\byudao\b|\bYudao\b|\bruoyi\b|cn\.iocoder|iocoder\.cn|doc\.iocoder|cloud\.iocoder|shopro\.sheepjs|YOUR_ACCESS_KEY')

$excludeDirNames = @(
    'node_modules', 'dist', 'dist-prod', 'dist-test', 'dist-stage', 'target',
    '.git', '.idea', 'uni_modules', 'out', '.cache'
)

function Test-ExcludedPath([string]$fullPath) {
    $norm = $fullPath.Replace('\', '/')
    foreach ($d in $excludeDirNames) {
        if ($norm -match ('/' + [regex]::Escape($d) + '/')) { return $true }
    }
    if ($norm -match '/script/verify/') { return $true }
    if ($norm -match '/script/tools/sanitize-scaffold.py$') { return $true }
    if ($norm -match '\.flattened-pom\.xml$') { return $true }
    return $false
}

function Test-AllowedLine([string]$line) {
    $t = $line.Trim()
    # MIT 义务：LICENSE 保留上游版权行
    if ($t -match 'Copyright \(c\) 2021 ruoyi-vue-pro') { return $true }
    # 文档中的开源来源说明（须带 UPSTREAM-COPYRIGHT 标记）
    if ($t -match 'UPSTREAM-COPYRIGHT') { return $true }
    if ($t -match 'cn\.simple\.boot') { return $true }
    if ($t -match 'base-package:\s*cn\.simple\.boot') { return $true }
    if ($t -match '^simple:\s*$') { return $true }
    if ($t -match '\$\{simple\.') { return $true }
    if ($t -match 'Simple[A-Z]') { return $true }
    if ($t -match '<artifactId>simple') { return $true }
    if ($t -match '<module>simple-') { return $true }
    if ($t -match 'simple-server\.jar|/simple-server') { return $true }
    return $false
}

$violations = New-Object System.Collections.Generic.List[string]

foreach ($root in $roots) {
    if (-not (Test-Path $root)) {
        $violations.Add("MISSING_ROOT: $root")
        continue
    }
    $files = Get-ChildItem -Path $root -Recurse -File -ErrorAction SilentlyContinue |
        Where-Object {
            $n = $_.Name
            $extOk = $false
            foreach ($e in @('.java', '.xml', '.yaml', '.yml', '.vue', '.ts', '.tsx', '.js', '.json', '.md', '.html', '.properties', '.txt', '.sql', '.http')) {
                if ($n.EndsWith($e)) { $extOk = $true; break }
            }
            if ($n.StartsWith('.env')) { $extOk = $true }
            $extOk -and -not (Test-ExcludedPath $_.FullName)
        }

    foreach ($f in $files) {
        if ($f.Length -gt 5MB) { continue }
        try {
            $lines = [System.IO.File]::ReadAllLines($f.FullName)
        } catch {
            continue
        }
        $rel = $f.FullName.Substring($RepoRoot.Length).TrimStart('\', '/')
        for ($i = 0; $i -lt $lines.Length; $i++) {
            $line = $lines[$i]
            if ($line -notmatch $pattern) { continue }
            if (Test-AllowedLine $line) { continue }
            $violations.Add(('{0}:{1}: {2}' -f $rel, ($i + 1), $line.Trim()))
        }
    }
}

$report = New-Object System.Collections.Generic.List[string]
$report.Add("Brand scan report $(Get-Date -Format o)")
$report.Add("RepoRoot=$RepoRoot")
$report.Add("BootDir=$BootDir")
$report.Add("Result=$(if ($violations.Count -eq 0) { 'PASS' } else { 'FAIL' })")
$report.Add("Violations=$($violations.Count)")
$report.Add('')
if ($violations.Count -eq 0) {
    $report.Add('No brand violations.')
} else {
    foreach ($v in $violations) { $report.Add($v) }
}
[System.IO.File]::WriteAllLines($reportPath, $report)

Write-Host ("T-AUTO-01 brand-scan: {0} violation(s). Report: {1}" -f $violations.Count, $reportPath)
if ($violations.Count -gt 0) {
    $violations | Select-Object -First 50 | ForEach-Object { Write-Host $_ }
    if ($violations.Count -gt 50) {
        Write-Host ("... and {0} more" -f ($violations.Count - 50))
    }
    exit 1
}
exit 0
