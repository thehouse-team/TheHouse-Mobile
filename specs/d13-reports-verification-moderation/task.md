# Tasks D13

| ID | Task | Fase | Estado |
| --- | --- | --- | --- |
| T01 | Reports Api/DTO/mapper/repo/use case + errores reales | A | DONE — `ReportsRepositoryIntegrationTest` 15/15 |
| T02 | Verificación del dueño + estados `rejected\|suspended` | A | DONE — verificación 200/404/403 + `BuildingStatus.fromApi` probados |
| T03 | Suspensión global, login/refresh y Session, sin logout implícito | A | DONE — `AccountSuspensionIntegrationTest` 9/9 |
| T04 | ReportViewModel + ReportScreen + ruta validada | B/C | DONE — `ReportViewModelTest` 8/8 + `ReportRoute.PATTERN` registrada en `MainActivity` |
| T05 | Verificación integrada en Building Detail (D12) | B/C | DONE — `getVerification` inyectado, `VerificationCard`, `when` con `rejected`/`suspended` |
| T06 | Reportar cuarto/edificio desde los IDs reales de Request Detail | C | DONE — botones `request.room.id` / `request.room.buildingId` → `ReportRoute(ROOM\|BUILDING, id)` |
| T07 | Tests reports/verification/suspensión + regresión | D | DONE — 206 tests / 0 failures / 0 errors (33 suites) |
| T08 | `compileDebugKotlin` y set completo, máximo un ciclo correctivo | D | DONE — `testDebugUnitTest lintDebug assembleDebug` BUILD SUCCESSFUL |
| T09 | Comprobar disponibilidad de runtime y registrar resultado real | D | DONE — `adb`/`emulator` no disponibles → RUNTIME-BLOCKED registrado en `evidence.md` |
| T10 | Moderación de texto/imágenes/videos y uploads | — | BLOCKED: NV-13 Backend 1.0.0 es borrador/no ejecutada; video fuera de alcance; no hay contrato ejecutable de moderación automática |

Cierre D13: 2026-11-12 en `feature/reports-verification-moderation`. Evidencia en `evidence.md`. T10 permanece BLOCKED sin evidencia de cierre.
