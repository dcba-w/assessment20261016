# One-click MySQL init: schema + quartz + menu cleanup + seed sanitize + feature tables.
param(
    [string]$MysqlBin = 'mysql',
    # Avoid $Host: it is a built-in read-only variable in PowerShell.
    [string]$MysqlHost = '127.0.0.1',
    [int]$Port = 3306,
    [string]$User = 'root',
    [string]$Password = 'root',
    [string]$Database = 'simple-project'
)

$ErrorActionPreference = 'Stop'
$sqlDir = $PSScriptRoot

if (-not $Password) {
    $secure = Read-Host "MySQL password for $User" -AsSecureString
    $Password = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    )
}

$createSql = @"
CREATE DATABASE IF NOT EXISTS ``$Database``
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
"@

& $MysqlBin -h $MysqlHost -P $Port -u $User "-p$Password" -e $createSql
if ($LASTEXITCODE -ne 0) { throw "Failed to create database $Database" }

$files = @(
    'init.sql'
)
foreach ($f in $files) {
    $path = Join-Path $sqlDir $f
    Write-Host "Importing $f ..."
    # Do not pipe through PowerShell: it re-encodes UTF-8 and turns Chinese into ???.
    # Use cmd file redirect so mysql reads the SQL bytes as-is.
    $cmd = "`"$MysqlBin`" -h $MysqlHost -P $Port -u $User -p$Password --default-character-set=utf8mb4 $Database < `"$path`""
    cmd /c $cmd
    if ($LASTEXITCODE -ne 0) { throw "Failed importing $f" }
}

Write-Host "MySQL init completed for database $Database"
Write-Host "Default login: tenant=Simple / username=admin / password=admin123"
