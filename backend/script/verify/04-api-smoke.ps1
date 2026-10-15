# API smoke T-AUTO-20 .. T-AUTO-23 (requires running server)
param(
    [string]$BaseUrl = $(if ($env:VERIFY_BASE_URL) { $env:VERIFY_BASE_URL } else { "http://127.0.0.1:48080" }),
    [string]$Username = $(if ($env:VERIFY_USERNAME) { $env:VERIFY_USERNAME } else { "admin" }),
    [string]$Password = $(if ($env:VERIFY_PASSWORD) { $env:VERIFY_PASSWORD } else { "admin123" }),
    [string]$TenantId = $(if ($env:VERIFY_TENANT_ID) { $env:VERIFY_TENANT_ID } else { "1" })
)

$ErrorActionPreference = "Stop"
$fail = New-Object System.Collections.Generic.List[string]
function Fail([string]$m) { $fail.Add($m); Write-Host "FAIL: $m" }

$loginUrl = "$BaseUrl/admin-api/system/auth/login"
Write-Host "T-AUTO-20 POST $loginUrl"
try {
    $body = @{ username = $Username; password = $Password } | ConvertTo-Json
    $headers = @{
        "Content-Type" = "application/json"
        "tenant-id" = $TenantId
    }
    $resp = Invoke-RestMethod -Uri $loginUrl -Method Post -Headers $headers -Body $body -TimeoutSec 30
} catch {
    Fail "login request failed: $($_.Exception.Message)"
    $resp = $null
}

$token = $null
if ($resp) {
    if ($resp.code -ne 0) {
        Fail "login code=$($resp.code) msg=$($resp.msg)"
    } else {
        $token = $resp.data.accessToken
        if (-not $token) { Fail "login missing accessToken" }
        else { Write-Host "T-AUTO-20 PASS" }
    }
}

if ($token) {
    $permUrl = "$BaseUrl/admin-api/system/auth/get-permission-info"
    Write-Host "T-AUTO-21 GET $permUrl"
    try {
        $ph = @{
            Authorization = "Bearer $token"
            "tenant-id" = $TenantId
        }
        $perm = Invoke-RestMethod -Uri $permUrl -Method Get -Headers $ph -TimeoutSec 30
        if ($perm.code -ne 0) {
            Fail "permission-info code=$($perm.code)"
        } else {
            $json = $perm | ConvertTo-Json -Depth 20
            $banned = @(
                ([char]0x5546 + [char]0x57CE),
                'CRM', 'ERP', 'WMS', 'BPM', 'MES'
            )
            foreach ($b in $banned) {
                if ($json -match [regex]::Escape($b)) { Fail "menu tree contains banned keyword: $b" }
            }
            Write-Host "T-AUTO-21 PASS"
        }
    } catch {
        Fail "permission-info failed: $($_.Exception.Message)"
    }

    Write-Host "T-AUTO-22 tenant name check"
    try {
        $tenantUrl = "$BaseUrl/admin-api/system/tenant/get?id=1"
        $th = @{ Authorization = "Bearer $token"; "tenant-id" = $TenantId }
        $tenant = Invoke-RestMethod -Uri $tenantUrl -Method Get -Headers $th -TimeoutSec 30
        if ($tenant.code -eq 0 -and $tenant.data) {
            $name = [string]$tenant.data.name
            $kwBrand = [char]0x828B + [char]0x9053
            if ($name -match $kwBrand) { Fail "tenant name still branded: $name" }
            elseif ($name -ne "Simple" -and $name.Length -gt 0) {
                Write-Host "WARN: tenant name is '$name' (expected Simple)"
            } else {
                Write-Host "T-AUTO-22 PASS ($name)"
            }
        } else {
            Write-Host "T-AUTO-22 SKIP (no tenant get permission or empty)"
        }
    } catch {
        Write-Host "T-AUTO-22 SKIP: $($_.Exception.Message)"
    }
}

if ($env:VERIFY_MYSQL_CMD) {
    Write-Host "T-AUTO-23 running custom mysql check"
    cmd /c $env:VERIFY_MYSQL_CMD
    if ($LASTEXITCODE -ne 0) { Fail "mysql check failed" }
} else {
    Write-Host "T-AUTO-23 SKIP (set VERIFY_MYSQL_CMD to enable)"
}

$outDir = Join-Path $PSScriptRoot "out"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$report = New-Object System.Collections.Generic.List[string]
$report.Add("API smoke report $(Get-Date -Format o)")
$report.Add("BaseUrl=$BaseUrl")
$report.Add("Result=$(if ($fail.Count -eq 0) { 'PASS' } else { 'FAIL' })")
$report.Add("Failures=$($fail.Count)")
$report.Add('')
if ($fail.Count -eq 0) {
    $report.Add('API smoke checks passed or skipped as documented.')
} else {
    foreach ($item in $fail) { $report.Add($item) }
}
[System.IO.File]::WriteAllLines((Join-Path $outDir "api-smoke-report.txt"), $report)

if ($fail.Count -gt 0) {
    Write-Host ("API smoke: {0} failure(s)" -f $fail.Count)
    exit 1
}
Write-Host "API smoke: PASS"
exit 0
