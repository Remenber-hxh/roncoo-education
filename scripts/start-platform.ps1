# 内部培训平台 —— 一键启动
#
# 用法：在本文件上右键 →「使用 PowerShell 运行」
#       或在 PowerShell 里执行： .\启动平台.ps1
#
# 已在运行的服务会自动跳过，重复执行不会启动两份。

$ErrorActionPreference = 'Continue'
$Base = 'D:\视频培训'
$Java = 'C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot\bin\java.exe'
$Redis = 'D:\redis\redis-server.exe'
$NodeDir = 'C:\Users\33793\AppData\Local\nvm\v22.23.1'
$LogDir = "$Base\logs"

if (-not (Test-Path $LogDir)) { New-Item -ItemType Directory -Force $LogDir | Out-Null }

function Test-Port($port) {
    $null -ne (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue)
}

function Wait-Port($port, $name, $timeoutSec = 120) {
    $deadline = (Get-Date).AddSeconds($timeoutSec)
    while ((Get-Date) -lt $deadline) {
        if (Test-Port $port) {
            Write-Host ("  [OK]   {0} 已就绪 ({1})" -f $name, $port) -ForegroundColor Green
            return $true
        }
        Start-Sleep -Seconds 3
    }
    Write-Host ("  [失败] {0} 在 {1} 秒内没起来，看日志 {2}\{3}.out" -f $name, $timeoutSec, $LogDir, $name) -ForegroundColor Red
    return $false
}

Write-Host ''
Write-Host '======== 内部培训平台 启动 ========' -ForegroundColor Cyan
Write-Host ''

# --- 1. MySQL（Windows 服务） ---
Write-Host '[1/4] 数据库'
$mysql = Get-Service -Name 'mysql' -ErrorAction SilentlyContinue
if ($null -eq $mysql) {
    Write-Host '  [失败] 找不到名为 mysql 的服务' -ForegroundColor Red
} elseif ($mysql.Status -ne 'Running') {
    Write-Host '  MySQL 未运行，正在启动...'
    Start-Service mysql
    Wait-Port 3306 'MySQL' 60 | Out-Null
} else {
    Write-Host '  [跳过] MySQL 已在运行' -ForegroundColor DarkGray
}

# --- 2. Redis ---
Write-Host '[2/4] 缓存'
if (Test-Port 6379) {
    Write-Host '  [跳过] Redis 已在运行' -ForegroundColor DarkGray
} else {
    Start-Process -FilePath $Redis -ArgumentList '--port', '6379' -WindowStyle Hidden `
        -RedirectStandardOutput "$LogDir\redis.out" -RedirectStandardError "$LogDir\redis.err"
    Wait-Port 6379 'Redis' 30 | Out-Null
}

# --- 3. 后端四个服务 ---
Write-Host '[3/4] 后端服务（首次启动约需 30-60 秒）'
$services = [ordered]@{
    'system'  = @{ Port = 7710; Jar = "$Base\roncoo-education\roncoo-education-service\roncoo-education-service-system\target\system.jar" }
    'user'    = @{ Port = 7720; Jar = "$Base\roncoo-education\roncoo-education-service\roncoo-education-service-user\target\user.jar" }
    'course'  = @{ Port = 7730; Jar = "$Base\roncoo-education\roncoo-education-service\roncoo-education-service-course\target\course.jar" }
    'gateway' = @{ Port = 7700; Jar = "$Base\roncoo-education\roncoo-education-gateway\target\gateway.jar" }
}
foreach ($name in $services.Keys) {
    $svc = $services[$name]
    if (Test-Port $svc.Port) {
        Write-Host ('  [跳过] {0} 已在运行 ({1})' -f $name, $svc.Port) -ForegroundColor DarkGray
        continue
    }
    if (-not (Test-Path $svc.Jar)) {
        Write-Host ('  [失败] 找不到 {0}，需要先构建：见文末说明' -f $svc.Jar) -ForegroundColor Red
        continue
    }
    Start-Process -FilePath $Java -ArgumentList '-jar', $svc.Jar -WindowStyle Hidden `
        -WorkingDirectory "$Base\roncoo-education" `
        -RedirectStandardOutput "$LogDir\$name.out" -RedirectStandardError "$LogDir\$name.err"
    Write-Host ('  启动 {0} ...' -f $name)
}
foreach ($name in $services.Keys) {
    if (-not (Test-Port $services[$name].Port)) { Wait-Port $services[$name].Port $name 120 | Out-Null }
}

# --- 4. 两个前端 ---
Write-Host '[4/4] 前端（首次编译较慢，门户可能要 1-2 分钟）'
$front = [ordered]@{
    'admin' = @{ Port = 9528; Dir = "$Base\roncoo-education-admin" }
    'web'   = @{ Port = 3000; Dir = "$Base\roncoo-education-web" }
}
foreach ($name in $front.Keys) {
    $f = $front[$name]
    if (Test-Port $f.Port) {
        Write-Host ('  [跳过] {0} 已在运行 ({1})' -f $name, $f.Port) -ForegroundColor DarkGray
        continue
    }
    if (-not (Test-Path "$($f.Dir)\node_modules")) {
        Write-Host ('  [失败] {0} 缺 node_modules，先在该目录执行: npm install --legacy-peer-deps' -f $name) -ForegroundColor Red
        continue
    }
    $env:PATH = "$NodeDir;$env:PATH"
    Start-Process -FilePath "$NodeDir\npm.cmd" -ArgumentList 'run', 'dev' -WindowStyle Hidden `
        -WorkingDirectory $f.Dir `
        -RedirectStandardOutput "$LogDir\$name.out" -RedirectStandardError "$LogDir\$name.err"
    Write-Host ('  启动 {0} ...' -f $name)
}
foreach ($name in $front.Keys) {
    if (-not (Test-Port $front[$name].Port)) { Wait-Port $front[$name].Port $name 180 | Out-Null }
}

# --- 汇总 ---
Write-Host ''
Write-Host '======== 启动结果 ========' -ForegroundColor Cyan
$all = [ordered]@{
    'MySQL' = 3306; 'Redis' = 6379; '网关' = 7700; 'system' = 7710
    'user' = 7720; 'course' = 7730; '管理后台' = 9528; '员工门户' = 3000
}
$down = 0
foreach ($k in $all.Keys) {
    if (Test-Port $all[$k]) {
        Write-Host ('  {0,-10} {1,-6} 正常' -f $k, $all[$k]) -ForegroundColor Green
    } else {
        Write-Host ('  {0,-10} {1,-6} 未启动' -f $k, $all[$k]) -ForegroundColor Red
        $down++
    }
}

Write-Host ''
if ($down -eq 0) {
    Write-Host '  全部就绪' -ForegroundColor Green
    Write-Host ''
    Write-Host '  管理后台  http://localhost:9528   admin / 88775560'
    Write-Host '  员工门户  http://localhost:3000   13900139000 / 123456'
} else {
    Write-Host ('  有 {0} 个服务没起来，日志在 {1}' -f $down, $LogDir) -ForegroundColor Yellow
}
Write-Host ''
Write-Host ('  日志目录：{0}' -f $LogDir) -ForegroundColor DarkGray
Write-Host '  改过后端 Java 代码的话，启动前要先构建：'
Write-Host '    cd D:\视频培训\roncoo-education; D:\apache-maven-3.9.9\bin\mvn.cmd -DskipTests install' -ForegroundColor DarkGray
Write-Host ''
