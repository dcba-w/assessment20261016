# Structure + targeted assertions (T-AUTO-02 .. 06, 12)
param(
    [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path,
    [string]$BootDir = 'simple-boot-new'
)

$ErrorActionPreference = 'Stop'
$fail = New-Object System.Collections.Generic.List[string]
function Fail([string]$msg) { $fail.Add($msg); Write-Host "FAIL: $msg" }

$web = Join-Path $RepoRoot 'simple-boot-web'
$boot = Join-Path $RepoRoot $BootDir
$app = Join-Path $RepoRoot 'simple-boot-uniapp'
$kwSimpleZh = [char]0x828B + [char]0x9053

$biz = @('ai','bpm','crm','erp','fms','hrm','im','iot','mall','member','mes','mp','oa','pay','pms','report','wms')
foreach ($d in $biz) {
    if (Test-Path (Join-Path $web "src\views\$d")) { Fail "views/$d still exists" }
    if (Test-Path (Join-Path $web "src\api\$d")) { Fail "api/$d still exists" }
}
Write-Host 'T-AUTO-02 views/api dirs: checked'

$pagesPath = Join-Path $app 'pages.json'
if (-not (Test-Path $pagesPath)) {
    Fail 'pages.json missing'
} else {
    $pagesJson = [System.IO.File]::ReadAllText($pagesPath)
    $stripped = [regex]::Replace($pagesJson, '(?m)^\s*//.*$', '')
    $stripped = [regex]::Replace($stripped, '/\*[\s\S]*?\*/', '')
    try {
        $pj = $stripped | ConvertFrom-Json
    } catch {
        Fail ("pages.json parse failed: {0}" -f $_.Exception.Message)
        $pj = $null
    }
    if ($pj) {
        $tabTexts = @($pj.tabBar.list | ForEach-Object { $_.text })
        $badTabs = @([char]0x5206+[char]0x7C7B, [char]0x8D2D+[char]0x7269+[char]0x8F66)
        foreach ($bad in $badTabs) {
            if ($tabTexts -contains $bad) { Fail "tabBar still has $bad" }
        }
        if ($tabTexts.Count -gt 3) { Fail ("tabBar too many items: {0}" -f ($tabTexts -join ',')) }
        foreach ($sr in @($pj.subPackages | ForEach-Object { $_.root })) {
            if ($sr -match 'goods|order|coupon|commission|pay|activity|chat|mall') {
                Fail "mall subPackage remains: $sr"
            }
        }
        foreach ($page in $pj.pages) {
            $path = $page.path
            $ok = (Test-Path (Join-Path $app "$path.vue")) -or (Test-Path (Join-Path $app "$path.nvue"))
            if (-not $ok) { Fail "page file missing for $path" }
        }
        foreach ($sp in $pj.subPackages) {
            foreach ($page in $sp.pages) {
                $path = ($sp.root + '/' + $page.path).Replace('\', '/')
                $ok = (Test-Path (Join-Path $app "$path.vue")) -or (Test-Path (Join-Path $app "$path.nvue"))
                if (-not $ok) { Fail "subPackage page missing for $path" }
            }
        }
        Write-Host 'T-AUTO-03/12 pages.json: checked'
    }
}

foreach ($f in @(
    (Join-Path $boot 'script\docker\docker.env'),
    (Join-Path $boot 'script\docker\docker-compose.yml')
)) {
    if (-not (Test-Path $f)) { Fail "missing $f"; continue }
    $c = [System.IO.File]::ReadAllText($f)
    if ($c.Contains($kwSimpleZh)) { Fail "docker file still has brand: $f" }
    if ($c -notmatch 'Simple Boot') { Fail "docker file missing Simple Boot: $f" }
}
Write-Host 'T-AUTO-04 docker title: checked'

foreach ($rel in @(
    'src\layout\components\UserInfo\src\UserInfo.vue',
    'src\config\axios\service.ts'
)) {
    $p = Join-Path $web $rel
    if (-not (Test-Path $p)) { Fail "missing $rel"; continue }
    $c = [System.IO.File]::ReadAllText($p)
    if ($c -match 'iocoder\.cn') { Fail "banned iocoder.cn in $rel" }
}
$viewRoot = Join-Path $web 'src\views'
$viewsHits = Get-ChildItem -Path $viewRoot -Recurse -Include *.vue,*.ts,*.tsx -ErrorAction SilentlyContinue |
    Select-String -Pattern 'doc\.iocoder\.cn'
if ($viewsHits) {
    Fail ("doc.iocoder.cn still in views count={0}" -f @($viewsHits).Count)
}
$envHits = Get-ChildItem -Path $web -Filter '.env*' -File -ErrorAction SilentlyContinue |
    Select-String -Pattern 'yudao\.iocoder\.cn|simple\.iocoder\.cn'
if ($envHits) {
    Fail '.env still has banned external domain'
}
Write-Host 'T-AUTO-05 web targeted: checked'

$appYaml = Join-Path $boot 'simple-server\src\main\resources\application.yaml'
if (-not (Test-Path $appYaml)) {
    Fail "missing $appYaml"
} else {
    $yaml = [System.IO.File]::ReadAllText($appYaml)
    if ($yaml -notmatch '(?m)^\s*name:\s*simple-boot\s*$') {
        Fail 'spring.application.name is not simple-boot'
    }
}
$reactor = Join-Path $boot 'simple-server\src\test\java\cn\simple\boot\ProjectReactor.java'
if (Test-Path $reactor) {
    $rc = [System.IO.File]::ReadAllText($reactor)
    if ($rc.Contains($kwSimpleZh + [char]0x7BA1 + [char]0x7406 + [char]0x7CFB + [char]0x7EDF)) {
        Fail 'ProjectReactor still branded'
    }
}
$dc = Get-ChildItem -Path (Join-Path $boot 'simple-server') -Recurse -Filter 'DefaultController.java' -ErrorAction SilentlyContinue |
    Select-Object -First 1
if ($dc) {
    $dcc = [System.IO.File]::ReadAllText($dc.FullName)
    if ($dcc -match 'doc\.iocoder\.cn' -or $dcc.Contains($kwSimpleZh)) {
        Fail 'DefaultController still branded'
    }
}
Write-Host 'T-AUTO-06 backend targeted: checked'

$outDir = Join-Path $PSScriptRoot 'out'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$report = New-Object System.Collections.Generic.List[string]
$report.Add("Structure report $(Get-Date -Format o)")
$report.Add("RepoRoot=$RepoRoot")
$report.Add("BootDir=$BootDir")
$report.Add("Result=$(if ($fail.Count -eq 0) { 'PASS' } else { 'FAIL' })")
$report.Add("Failures=$($fail.Count)")
$report.Add('')
if ($fail.Count -eq 0) {
    $report.Add('All structure checks passed.')
} else {
    foreach ($f in $fail) { $report.Add($f) }
}
[System.IO.File]::WriteAllLines((Join-Path $outDir 'structure-report.txt'), $report)

if ($fail.Count -gt 0) {
    Write-Host ("T-AUTO structure: {0} failure(s)" -f $fail.Count)
    exit 1
}
Write-Host 'T-AUTO structure: PASS'
exit 0
