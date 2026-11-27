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

## ADENDA VISUAL (misma rama / PR #37) — ALCANCE ESTRICTO CORREGIDO

| Fase | Task | Descripción | Verificación |
|---|---|---|---|
| P14 | D21-T14 | Home: SOLO mover el botón original "Publicar mi renta" antes del listado; SOLO retirar la tarjeta de Privacidad del final | Diff contra `735a697`: 2 bloques, sin otros cambios |
| P15 | D21-T15 | Favoritos: polish moderado exclusivamente en `FavoriteCard` (título, precio, ubicación, distribución, acción quitar) | Diff contra `735a697`: solo `FavoriteCard` |
| P16 | D21-T16 | Conversación: SOLO burbujas, hora, campo de entrada y botón enviar | Diff contra `735a697`: 4 bloques; header/estructura intactos |
| P17 | D21-T17 | Restaurar cambios no autorizados desde `735a697`, documentar, gates locales + CI remoto del PR #37 | `git diff 735a697`, 645 tests, `gh pr checks 37` |

Reglas: sin rama nueva, sin PR nuevo, sin merge, sin force push ni reescritura de historial;
sin Backend/GenAI/ML/ONNX/infra; sin mover "Nuevo edificio"; sin componentes compartidos ni tema
global; sin mejoras por iniciativa propia; esperar aprobación humana de la validación visual.

## ADENDA FINAL (misma rama / PR #37)

| Fase | Task | Descripción | Verificación |
|---|---|---|---|
| P18 | D21-T18 | Parser de coordenadas + pruebas (coma, U+2212, U+2013, invisibles, límites, parcial) | 11 tests |
| P19 | D21-T19 | Búsqueda `universities/search` real + DTO + repositorio + usecase + HTTP test | 3 tests MockWebServer |
| P20 | D21-T20 | Ubicación contextual (`LocationManager`) + VM UDF + UI solo en sección Ubicación | 8 tests VM + permisos |
| P21 | D21-T21 | Fotos: decidir UI existente, corregir DTO de respuesta, moderación 422/503 en formularios | 8 tests MockWebServer |
| P22 | D21-T22 | POST_NOTIFICATIONS persistente + solicitud contextual única | 7 tests de política + 1 store |
| P23 | D21-T23 | JaCoCo, matrices, trazabilidad, evidencia, gates y push al PR #37 | 702 tests, `git diff --check`, CI SUCCESS |
