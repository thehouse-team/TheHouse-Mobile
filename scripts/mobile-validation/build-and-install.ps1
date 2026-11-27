# D21 — Compila el APK debug y lo instala conservando datos (-r sin -d).
param([switch]$RequirePhysical)

. (Join-Path $PSScriptRoot 'common.ps1')

try {
    $adb = Resolve-Adb
    $device = Assert-OneDevice -Adb $adb -RequirePhysical:$RequirePhysical
    Invoke-Gradle -Tasks @('assembleDebug')

    $repo = Get-RepoRoot
    $apk = Get-ChildItem (Join-Path $repo 'app/build/outputs/apk/debug') -Filter '*.apk' |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $apk) { throw 'No se generó APK debug.' }

    $serial = @('-s', $device.Serial)
    & $adb @serial install -r $apk.FullName
    if ($LASTEXITCODE -ne 0) { throw 'adb install -r falló.' }

    $package = (& $adb @serial shell pm list packages com.thehouse.app) -join ''
    if ($package -notmatch 'com\.thehouse\.app') { throw 'Application ID no verificado tras la instalación.' }

    $evidence = [pscustomobject]@{
        commit = Get-CommitUnderTest
        apk = $apk.Name
        applicationId = 'com.thehouse.app'
        preservedData = $true
        serial = $device.Serial
        physical = [bool]$device.Physical
        installedAtUtc = (Get-Date).ToUniversalTime().ToString('o')
    }
    Write-Evidence -Name 'install' -Data $evidence
    exit 0
} catch {
    Write-Error $_.Exception.Message
    exit 1
}
