# D21 — Compila y ejecuta instrumented tests si hay dispositivo autorizado.
# Nunca borra datos ni desinstala la app.
param([switch]$RequirePhysical, [switch]$CompileOnly)

. (Join-Path $PSScriptRoot 'common.ps1')

try {
    Invoke-Gradle -Tasks @('compileDebugAndroidTestKotlin', 'assembleDebugAndroidTest')
    if ($CompileOnly) {
        Write-Evidence -Name 'androidtest-compile' -Data ([pscustomobject]@{
            commit = Get-CommitUnderTest
            result = 'COMPILATION_PASS'
            execution = 'SKIPPED_COMPILE_ONLY'
        })
        exit 0
    }

    $adb = Resolve-Adb
    $device = Assert-OneDevice -Adb $adb -RequirePhysical:$RequirePhysical
    Invoke-Gradle -Tasks @('connectedDebugAndroidTest')
    Write-Evidence -Name 'androidtest-execution' -Data ([pscustomobject]@{
        commit = Get-CommitUnderTest
        serial = $device.Serial
        physical = [bool]$device.Physical
        result = 'TEST_EXECUTION_PASS'
    })
    exit 0
} catch {
    Write-Error $_.Exception.Message
    exit 1
}
