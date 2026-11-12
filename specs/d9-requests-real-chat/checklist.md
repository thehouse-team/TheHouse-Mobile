# D9 — Checklist

- [x] Branch `feature/requests-real-chat` creada desde main (base D11 integrada).
- [x] Contratos exactos (sin inventar): requests estudiante/arrendador, conversaciones, mensajes, read.
- [x] Bearer por interceptor existente; sin userId; un solo Retrofit.
- [x] Api → DTO → mapper → repository → use case → ViewModel → Compose.
- [x] Validación rápida quickQuestions/message (0–5 valores, message ≤500, al menos uno).
- [x] Cancel/accept/reject con estados reales y códigos de negocio.
- [x] Chat: lista real, unread, open/closed, paginación por re-entrada; no fake.
- [x] Mensajes: historial, mine/counterpart por alineación+etiqueta, hora, composer (disabled vacío/enviando/cerrado).
- [x] Idempotencia: UUID estable en retry; UUID nuevo por acción nueva; doble submit bloqueado.
- [x] Polling: pollAfterSeconds del backend (default 10 s), If-None-Match, after=último mensaje, 304 no modifica, stop al salir, CancellationException rethrow.
- [x] Read 204 tras éxito.
- [x] Errores amigables del contrato (sin códigos técnicos), nunca crash.
- [x] Tests repositorios + ViewModels + regresión D3–D11.
- [x] Validación: compileDebugKotlin; testDebugUnitTest lintDebug assembleDebug.
- [x] Evidencia + memoria + commit + push (sin merge/PR/force).
