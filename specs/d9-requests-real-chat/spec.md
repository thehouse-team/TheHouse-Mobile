# D9 — REQUESTS + REAL CHAT

| Campo | Valor |
|---|---|
| Spec ID | D9 |
| Nombre | Requests + Real Chat |
| Versión | 1.0.0 |
| Fecha | 2026-11-12 |
| Estado | PLANNED → EXECUTED (ver evidence.md) |
| Autorización | APROBADA por instrucción humana explícita (orden D9.1, 2026-11-12) |
| Branch | `feature/requests-real-chat` |
| Módulo | `:app` |
| Relación NV | NO reutiliza nv-10-visits ni nv-11-chat; spec independiente. Reemplaza el estado CHAT-BACKEND-BLOCKED. |
| Backend | `https://ca-thehouse-u2.grayisland-cb722b5f.eastus2.azurecontainerapps.io` (CI 800/800, Azure HTTP 33/33) |

## Problema

No existe integración real de solicitudes (estudiante/arrendador), conversaciones ni mensajes.
El chat y el contacto son prototipos/bloqueados (CHAT-BACKEND-BLOCKED).

## Contrato (NO inventado; fuente: orden D9.1)

- `POST /api/v1/rooms/{roomId}/requests` (quickQuestions 0–5 valores del set confirmado, message 1–500, al menos uno; 201 con request; 400/401/404/409/422/429/503)
- `GET /api/v1/users/me/requests?status&page&pageSize`
- `GET /api/v1/users/me/requests/{id}`
- `POST /api/v1/users/me/requests/{id}/cancel` (200; 409 request_state)
- `GET /api/v1/landlord/requests?status&roomId&page&pageSize` (403 landlord_required)
- `GET /api/v1/landlord/requests/{id}`
- `POST /api/v1/landlord/requests/{id}/accept` (409 request_state/room_unavailable)
- `POST /api/v1/landlord/requests/{id}/reject` (reason: not_available|profile_mismatch|other; note ≤300; note obligatoria si reason=other)
- `GET /api/v1/conversations?page&pageSize` (paginada con unreadCount, open/closed)
- `GET /api/v1/conversations/{id}/messages?after&limit=50` (ETag; If-None-Match+after → 304 Not Modified; pollAfterSeconds)
- `POST /api/v1/conversations/{id}/messages` (text 1–1000, clientMessageId UUID; 201 nuevo / 200 replay; 409 conversation_closed)
- `POST /api/v1/conversations/{id}/read` (204)

Regla transversal: Bearer JWT; el usuario viene del token (nunca se envía userId); se reutilizan Retrofit/interceptor/refresh/session existentes.

## Alcance

- D9-REQ-001 Estudiante: crear solicitud desde Room Detail (quickQuestions/message), lista con filtro status, detalle, cancel; Loading/Empty/Content/Error.
- D9-REQ-002 Arrendador: lista, detalle, accept, reject (reason+note); mensaje amigable si 403 landlord_required.
- D9-REQ-003 Conversaciones: lista real paginada con unreadCount y open/closed; tap → conversación.
- D9-REQ-004 Mensajes: historial, mine/counterpart diferenciados (no solo color), composer con envío idempotente (UUID estable por mensaje en retry).
- D9-REQ-005 Polling: respeta pollAfterSeconds (default 10 s), envía If-None-Match y after=último mensaje; 304 no modifica lista; polling se cancela al salir de la pantalla; CancellationException → rethrow. Sin polling cada 1 s, sin recarga de historial completo, sin requests desde recomposición.
- D9-REQ-006 Read: POST read tras éxito; unread local actualizado solo con confirmación.
- D9-REQ-007 Errores: mapping 400/401/403/404/409/422/429/503/Network/Timeout; mensajes amigables exactos del contrato; nunca códigos técnicos; nunca crash.

## No-alcance

- WebSocket/FCM, edición de conversaciones, backend, refactor global, specs ajenas, datos fake.

## Criterios de aceptación

- `compileDebugKotlin` verde; luego `testDebugUnitTest lintDebug assembleDebug` (máx. 1 ciclo correctivo).
- Tests: repositorios (MockWebServer) cubriendo solicitudes + chat (ETag/304/UUID/polling/read) y ViewModels (polling se cancela al salir, UUID estable).
- Regresión D3–D11 en verde.
- Runtime punta a punta requiere emulador + credenciales autorizadas; si no, se reporta la causa exacta sin inventar credenciales.

## Riesgos

- Sin emulador/credenciales en esta sesión → runtime limitado (documentado en evidence.md).
- `landlord_required`: la app no persiste rol; el modo arrendador se valida por respuesta del backend (403) y la UI muestra el mensaje amigable. No se inventa endpoint de rol.

## Impacto seguridad

- Bearer en interceptor existente; sin userId inventado; sin secretos; sin PII en logs (logging seguro D3 intacto).

## COST-001 ($0)

- Sin servicios externos nuevos; backend propio gratuito. Tarjeta: no. Riesgo de cobro: ninguno. Alternativa: N/A.
