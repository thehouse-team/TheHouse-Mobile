# D21 — Evidencia

## Baseline
- `git rev-parse HEAD` = `54ece77747d5f3416bdf189ee1ede597c6a40f8f`
- `gh pr view 36 --json state,mergeCommit` = `MERGED`, mergeCommit `54ece77...`
- Working tree limpio antes de crear la rama.

## Gates ejecutados (salida real)

| Comando | Resultado |
|---|---|
| `.\gradlew.bat clean compileDebugKotlin --no-daemon` | BUILD SUCCESSFUL |
| `.\gradlew.bat compileDebugAndroidTestKotlin --no-daemon` | BUILD SUCCESSFUL |
| `.\gradlew.bat testDebugUnitTest --no-daemon` | BUILD SUCCESSFUL |
| `.\gradlew.bat lintDebug assembleDebugAndroidTest --no-daemon` | BUILD SUCCESSFUL |
| `git diff --check` | exit 0 (verificado antes del commit) |

Conteo real desde `app/build/test-results/testDebugUnitTest/TEST-*.xml` (93 suites): **631 tests, 0 failures, 0 errors**.
Baseline 602 → +29 nuevos (27 D21 device/moderation/coordinator + 2 GenAI).

## Hallazgos corregidos con evidencia

1. `androidTest` no compilaba: 378 errores en 4 archivos. Reescritos con ActivityScenario/Compose reales → `compileDebugAndroidTestKotlin` SUCCESS.
2. `dexBuilderDebugAndroidTest` fallaba por nombre de método con espacios en `LoginScreenInstrumentedTest.kt` → renombrado → `assembleDebugAndroidTest` SUCCESS.
3. `DeviceSyncCoordinator` reintentaba inmediatamente si la red ya estaba online → se exige transición offline→online.
4. FCM token se obtenía pero nunca se enviaba al Backend (`FCM-TOKEN-BLOCKED`) → DeviceApi + coordinador implementados.

## Estado runtime

- `adb` no disponible en el entorno → sin dispositivo físico/emulador.
- `connectedDebugAndroidTest`: **RUNTIME_PENDING**.
- FCM real delivery: **EXTERNAL_BLOCKER** (Backend no expone endpoint de envío; requiere `FIREBASE_SERVICE_ACCOUNT`).
- Eliminación real: **RUNTIME_PENDING** (sin cuenta desechable autorizada).
