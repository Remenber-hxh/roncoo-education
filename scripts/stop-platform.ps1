# 内部培训平台 —— 停止
#
# 停掉 Redis、四个后端服务、两个前端。
# MySQL 是 Windows 服务、开机自启，默认不停（加 -StopMySQL 参数才停）。

param([switch]$StopMySQL)

function Stop-ByPort($port, $name, $expectProcess) {
    $conns = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue
    if (-not $conns) {
        Write-Host ('  [跳过] {0} 本来就没在运行 ({1})' -f $name, $port) -ForegroundColor DarkGray
        return
    }
    foreach ($pid_ in ($conns | Select-Object -ExpandProperty OwningProcess -Unique)) {
        $proc = Get-Process -Id $pid_ -ErrorAction SilentlyContinue
        if (-not $proc) { continue }
        # 防止误杀：只停预期类型的进程
        if ($expectProcess -and $proc.Name -notin $expectProcess) {
            Write-Host ('  [警告] {0} 端口 {1} 上是 {2}，不是预期进程，已跳过' -f $name, $port, $proc.Name) -ForegroundColor Yellow
            continue
        }
        Stop-Process -Id $pid_ -Force
        Write-Host ('  [停止] {0} ({1}) PID={2}' -f $name, $port, $pid_) -ForegroundColor Green
    }
}

Write-Host ''
Write-Host '======== 内部培训平台 停止 ========' -ForegroundColor Cyan
Write-Host ''

Write-Host '前端'
Stop-ByPort 9528 '管理后台' @('node')
Stop-ByPort 3000 '员工门户' @('node')

Write-Host '后端服务'
Stop-ByPort 7700 '网关'   @('java')
Stop-ByPort 7710 'system' @('java')
Stop-ByPort 7720 'user'   @('java')
Stop-ByPort 7730 'course' @('java')

Write-Host '缓存'
Stop-ByPort 6379 'Redis' @('redis-server')

if ($StopMySQL) {
    Write-Host '数据库'
    Stop-Service mysql -Force
    Write-Host '  [停止] MySQL' -ForegroundColor Green
} else {
    Write-Host '数据库'
    Write-Host '  [保留] MySQL 未停（需要停的话执行： .\停止平台.ps1 -StopMySQL）' -ForegroundColor DarkGray
}

Write-Host ''
Write-Host '  完成' -ForegroundColor Green
Write-Host ''
