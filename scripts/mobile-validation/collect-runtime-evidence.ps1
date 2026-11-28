# D21 — Recolecta evidencia runtime SEGURA: commit, dispositivo, logs filtrados.
# No imprime tokens, credenciales ni contenido de notificaciones.
# No envía solicitudes, mensajes, visitas ni elimina cuentas.
param([int]$LogLines = 300, [switch]$RequirePhysical)

. (Join-Path $PSScriptRoot 'common.ps1')

try {
    $adb = Resolve-Adb
    $device = Assert-OneDevice -Adb $adb -RequirePhysical:$RequirePhysical
    $serial = @('-s', $device.Serial)

    $log = & $adb @serial logcat -d -t $LogLines 2>&1
    $allowed = $log | Where-Object {
        $_ -match '(TheHouse|HomeFlow|ExploreFlow|SmartMatchFlow|RoomSearchFlow|VisitFlow|TheHouseHttp|PERF|device_sync|push_)' -and
        $_ -notmatch '(?i)(authorization|bearer|refreshToken|fcmToken|password|api[_-]?key)'
    } | Select-Object -Last 120

    $report = [pscustomobject]@{
        commit = Get-CommitUnderTest
        serial = $device.Serial
        physical = [bool]$device.Physical
        collectedAtUtc = (Get-Date).ToUniversalTime().ToString('o')
        filteredLogLines = @($allowed).Count
        checks = @(
            'startup', 'guest-navigation', 'login', 'explore', 'smart-match',
            'drawer', 'privacy', 'permissions', 'favorites', 'logout', 'room-detail'
        )
    }
    Write-Evidence -Name 'runtime-evidence' -Data $report
    $allowed | ForEach-Object { Write-Host $_ }
    exit 0
} catch {
    Write-Error $_.Exception.Message
    exit 1
}
