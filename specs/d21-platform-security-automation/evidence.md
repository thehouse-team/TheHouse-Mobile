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

## ADENDA NV-20 v1.3.0 — evidencia

- PR Backend #103: `MERGED`, head `d23bb3cd63ede2b515c01efd1e1317068ab8f51a`, título «docs(NV-20): 1.3.0 propuesta — 6 avisos nuevos». Documento leído en modo solo lectura (§6.1, §9.1).
- Contrato propuesto ≠ mergeado ≠ productivo: el documento marca 1.3.0 como PROPUESTA pendiente de T09–T12; Backend main expone `Type`/`Kind` como `string`; despliegue de los 6 tipos NO confirmado (`health/live` OK, sin credenciales para bandeja).
- Seis tipos literales extraídos del contrato (no de etiquetas): `agreement_proposed`, `agreement_cancelled`, `review_received`, `review_hidden`, `favorite_unavailable`, `report_resolved`.
- `targetKind` nuevos: `room` (targetId id cuarto → `GET /rooms/{id}`, `RoomDetailRoute`), `none` (bandeja, sin exigir targetId).
- Sin enums estrictos en DTO (`String`), por lo que no hay `SerializationException` por valores futuros; los tipos se mapean a destino tipado con fallback.
- Tests nuevos: 14 (10 compatibilidad NV-20 + 4 room target HTTP). Total 645 / 0 failures / 0 errors (631 + 14).
- Gates tras adenda: `compileDebugKotlin`, `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest` — todos SUCCESS.
