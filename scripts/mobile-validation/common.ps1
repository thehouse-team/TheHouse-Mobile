# D21 — Funciones compartidas para validación móvil.
# Sin secretos, sin rutas hardcodeadas de usuario, sin borrado de datos.

$ErrorActionPreference = 'Stop'

function Resolve-Adb {
    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }

    $sdk = $env:ANDROID_HOME
    if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
    if ($sdk) {
        $candidate = Join-Path $sdk 'platform-tools/adb.exe'
        if (Test-Path $candidate) { return $candidate }
    }

    $localProperties = Join-Path (Get-RepoRoot) 'local.properties'
    if (Test-Path $localProperties) {
        foreach ($line in Get-Content $localProperties) {
            if ($line -match '^sdk\.dir=(.+)$') {
                $path = $Matches[1].Replace('\\', '\').Trim('"')
                $candidate = Join-Path $path 'platform-tools/adb.exe'
                if (Test-Path $candidate) { return $candidate }
            }
        }
    }
    throw 'adb no encontrado. Instale Android SDK platform-tools o defina ANDROID_HOME.'
}

function Get-RepoRoot {
    return (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
}

function Get-DeviceInfo {
    param([string]$Adb)
    $output = & $Adb devices -l 2>&1
    if ($LASTEXITCODE -ne 0) { throw 'adb devices falló.' }
    $lines = $output | Where-Object { $_ -match '^\S+\s+(device|unauthorized|offline)' }
    $devices = @()
    foreach ($line in $lines) {
        $parts = $line -split '\s+'
        $state = if ($parts[1] -eq 'device') { 'OK' } else { $parts[1] }
        if ($state -ne 'OK') { continue }
        $model = if ($line -match 'model:(\S+)') { $Matches[1] } else { 'unknown' }
        $transport = if ($line -match 'transport_id:(\S+)') { $Matches[1] } else { $null }
        $emulator = $parts[0] -like 'emulator-*'
        $devices += [pscustomobject]@{
            Serial = $parts[0]
            Model = $model
            Emulator = $emulator
            Physical = -not $emulator
            TransportId = $transport
        }
    }
    return $devices
}

function Assert-OneDevice {
    param([string]$Adb, [switch]$RequirePhysical)
    $devices = @(Get-DeviceInfo -Adb $Adb)
    if ($devices.Count -eq 0) { throw 'No hay dispositivos autorizados conectados.' }
    if ($devices.Count -gt 1) { throw 'Hay más de un dispositivo; indique ANDROID_SERIAL para evitar ambigüedad.' }
    $device = $devices[0]
    if ($RequirePhysical -and -not $device.Physical) {
        throw 'El dispositivo conectado es un emulador; se requiere dispositivo físico.'
    }
    return $device
}

function Get-CommitUnderTest {
    $repo = Get-RepoRoot
    return (& git -C $repo rev-parse HEAD).Trim()
}

function Write-Evidence {
    param([string]$Name, [object]$Data)
    $dir = Join-Path (Get-RepoRoot) 'evidence/runtime'
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
    $file = Join-Path $dir ("$Name.json")
    $Data | ConvertTo-Json -Depth 6 | Set-Content -Path $file -Encoding UTF8
    Write-Host "Evidencia: $file"
}

function Invoke-Gradle {
    param([string[]]$Tasks)
    $repo = Get-RepoRoot
    Push-Location $repo
    try {
        & .\gradlew.bat @Tasks --no-daemon
        if ($LASTEXITCODE -ne 0) { throw "Gradle falló: $($Tasks -join ' ')" }
    } finally { Pop-Location }
}
