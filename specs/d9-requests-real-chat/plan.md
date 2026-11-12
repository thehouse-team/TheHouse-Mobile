# D9 — Plan

| Campo | Valor |
|---|---|
| Spec | D9 Requests + Real Chat |
| Branch | `feature/requests-real-chat` |

## Fase 1 — Contratos + Data
- DTOs y APIs (RequestsApi, ChatApi) con contratos exactos.
- Mappers → Domain (RoomRequest, Conversation, ChatMessage, MessagesFetch con NotModified/etag).
- Repositorios con safeCall (CancellationException rethrow), parse de Problem Details y códigos de negocio.

## Fase 2 — Domain
- Modelos, errores (RequestErrorCode/ChatErrorCode), repos interfaces, use cases.

## Fase 3 — Presentation
- Estudiante: CreateRequest, Requests (lista+cancel), RequestDetail (+ abrir conversación).
- Arrendador: lista + detalle con accept/reject.
- Chat: Conversations + Conversation (mensajes, composer idempotente, polling, read).
- Rutas, factory, MainActivity/Home/PropertyDetail wiring.

## Fase 4 — Tests
- Repositorios con MockWebServer (201/304/409/422/403/429/200 replay/204 read/etag).
- ViewModels (UUID estable, 304 no duplica, polling cancelado al salir, read tras éxito).

## Fase 5 — Validación y evidencia
- compileDebugKotlin; testDebugUnitTest lintDebug assembleDebug (1 set completo).
- evidence.md, memoria.md, REQUIREMENTS_TRACEABILITY.md; commit y push.
