# D21 — Verifica estado del dispositivo sin modificar datos del usuario.
param([switch]$RequirePhysical)

. (Join-Path $PSScriptRoot 'common.ps1')

try {
    $adb = Resolve-Adb
    $device = Assert-OneDevice -Adb $adb -RequirePhysical:$RequirePhysical
    $info = [pscustomobject]@{
        commit = Get-CommitUnderTest
        serial = $device.Serial
        model = $device.Model
        emulator = [bool]$device.Emulator
        physical = [bool]$device.Physical
        checkedAtUtc = (Get-Date).ToUniversalTime().ToString('o')
    }
    Write-Host ("DISPOSITIVO: {0} ({1}) physical={2}" -f $device.Serial, $device.Model, $device.Physical)
    Write-Evidence -Name 'device-check' -Data $info
    exit 0
} catch {
    Write-Error $_.Exception.Message
    exit 1
}
