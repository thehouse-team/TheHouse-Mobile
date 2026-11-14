# NV-14 — EVIDENCIA

## Estado actual
- **Fase A (In-App Notifications):** APPROVED FOR GOVERNANCE — **IMPLEMENTATION: DO NOT EXECUTE todavía**. Sin evidencia de implementación.
- **Fase B (FCM / Push remoto):** PLANNED — **DO NOT EXECUTE**. Sin evidencia de implementación.

## Preparación de gobernanza realizada (2026-11-13, documentación)
- Spec oficial preservada en `specs/nv-14-notifications-fcm/` (sin spec paralela).
- Alcance separado: Fase A (in-app real) vs Fase B (FCM/push). NV-14 ya no es solo FCM.
- Contratos Fase A formalizados: `GET /users/me/notifications` (unread/category/page/pageSize), `GET /users/me/notifications/unread-count` (ETag/If-None-Match/304), `POST /{id}/read` (204/404), `POST /read-all` (body categoría opcional, 204).
- Targets reales definidos: request→`RequestDetailRoute`, conversation→`ConversationRoute`, visit→`VisitDetailRoute`, agreement→Agreement Detail real, rental→Mi renta, building→`BuildingDetailRoute`.
- Tasks trazables Fase A T01–T09 creadas en estado **PENDING**; Fase B B-T01–B-T07 permanece **PLANNED/DO NOT EXECUTE**.
- Estado Git preservado: rama `feature/smart-match-ml`, `git status --short` vacío, sin switch/pull/rama nueva/commit/push.

## Hechos reales preservados
- Firebase/FCM no configurado; Firebase MCP deshabilitado hasta fase/spec autorizada (AGENTS.md §18).
- `URL_AVISO_PRIVACIDAD` sigue pendiente (BLOCKED-PRIVACY-URL, NV-03).
- Backend conocido: `https://ca-thehouse-u2.grayisland-cb722b5f.eastus2.azurecontainerapps.io` (CI 893/893, Azure HTTP 25/25).

## Evidencia futura esperada (Fase A, tras autorización de implementación)
- Comandos y salidas reales: `compileDebugKotlin`, `testDebugUnitTest`, `lintDebug`, `assembleDebug`.
- Pruebas de contratos/ETag-304/polling/foreground/read/read-all/targets/chat y regresión D3–NV17.
- Runtime (ADB/emulador) o **RUNTIME-BLOCKED** documentado con causa.
- Registro de archivos modificados, riesgos/deuda y resultados (BLOCKED nunca equivale a DONE).

## Evidencia futura esperada (Fase B, solo tras autorización explícita)
- Evaluación COST-001 FCM $0, flujo permiso/token/logout y prueba real de notificación.
