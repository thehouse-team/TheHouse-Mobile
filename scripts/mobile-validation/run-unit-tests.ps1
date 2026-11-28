# D21 — Ejecuta unit tests y quality gates JVM (sin dispositivo).
param([switch]$IncludeAndroidTestCompile)

. (Join-Path $PSScriptRoot 'common.ps1')

try {
    $tasks = @('clean', 'compileDebugKotlin', 'testDebugUnitTest', 'lintDebug', 'assembleDebug')
    if ($IncludeAndroidTestCompile) {
        $tasks += @('compileDebugAndroidTestKotlin', 'assembleDebugAndroidTest')
    }
    Invoke-Gradle -Tasks $tasks
    Write-Evidence -Name 'unit-tests' -Data ([pscustomobject]@{
        commit = Get-CommitUnderTest
        tasks = $tasks
        result = 'SUCCESS'
        finishedAtUtc = (Get-Date).ToUniversalTime().ToString('o')
    })
    exit 0
} catch {
    Write-Error $_.Exception.Message
    exit 1
}
