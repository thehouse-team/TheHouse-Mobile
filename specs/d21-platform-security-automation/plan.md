# D21 — Plan

| Fase | Task | Descripción | Verificación |
|---|---|---|---|
| P0 | D21-T00 | Gobernanza: spec/plan/task, rama desde origin/main, verificación PR #36 | `git status`, `gh pr view 36` |
| P1 | D21-T01 | `InstallationIdStore`: UUID D persistente en DataStore, estable, sin datos de hardware | Tests de persistencia y formato |
| P2 | D21-T02 | Capa devices: `DeviceApi`, DTOs, `DeviceRepository`, domain models/errors, UseCases | MockWebServer: PUT 200/201/409/400/404, DELETE 204/404, GET 200 |
| P3 | D21-T03 | `DeviceSyncCoordinator`: single-flight, eventos (login/restore/onNewToken/red), guest, logout y cambio de cuenta | Tests de dedup, guest, switch, fallos de red |
| P4 | D21-T04 | Integración DI en `AppContainer` + `TheHouseFirebaseMessagingService.onNewToken` + purga local | `compileDebugKotlin`, tests de coordinator |
| P5 | D21-T05 | Moderación centralizada: mapper de 422/503 con `field`/`categories`/`Retry-After`, conservación de texto | Tests de contrato y comportamiento |
| P6 | D21-T06 | Auditoría dirigida OWASP/MASVS, permisos, ML/GenAI, eliminación, sesión; documentar matrices | Matrices en `docs/sdd/reports/` |
| P7 | D21-T07 | Reparación androidTest: imports, dependencias, firmas, tags, sincronización | `compileDebugAndroidTestKotlin`, `assembleDebugAndroidTest` |
| P8 | D21-T08 | Scripts PowerShell + CI preventivo | Scripts ejecutables (sintaxis), workflow actualizado |
| P9 | D21-T09 | Gates completos, trazabilidad, memoria, commit/push/PR | 8 gates, PR OPEN/MERGEABLE, sin merge |

## Reglas de ejecución

- Un solo ciclo de implementación; máximo 2 correcciones si hay inconsistencia (AGENTS §2).
- No tocar Backend/GenAI/ML/ONNX/infra.
- No reducir thresholds de CI.
- Si no hay dispositivo autorizado (`adb` no disponible), marcar RUNTIME_PENDING sin simular execución.

## ADENDA NV-20 v1.3.0 (misma spec)

| Fase | Task | Descripción | Verificación |
|---|---|---|---|
| P10 | D21-T10 | Enums tolerantes: ROOM/NONE en `NotificationTargetKind`, `NotificationDestination.Room`, resolver centralizado con validación de id | Tests de 6 tipos + desconocidos |
| P11 | D21-T11 | MainActivity: ruta `RoomDetailRoute` desde notificación (in-app y push), sin `{roomId}` literal, sin propertyId | Tests de navegación y regresión |
| P12 | D21-T12 | Presentación: iconos coherentes, texto del Backend, fallback genérico sin exponer enums | Tests de bandeja mixta |
| P13 | D21-T13 | Pruebas HTTP locales (MockWebServer) para room 200/404 y regresiones; evidence y reporte adicional | Gates + reporte §8 |
