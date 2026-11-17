# NV-14 — CHECKLIST

## Fase A — In-App Notifications (APPROVED / IMPLEMENTED, RUNTIME PENDIENTE)
- [x] Contratos reales formalizados (list/unread-count/read/read-all) y DTOs/mappers sin campos inventados
- [x] ETag/If-None-Match/304: 304 no modifica estado
- [x] Polling unread ~30 s solo foreground + refresh al volver; se detiene cuando no aplica
- [x] Mark read y read-all (endpoint único, categoría opcional) tras confirmación; sin POST individual masivo
- [x] Targets mapeados a rutas reales (request/conversation/visit/agreement/rental/building) o bloqueo documentado
- [x] `/conversations/{id}/read` marca aviso leído; sin duplicados; unread count refrescado (best-effort)
- [x] `title/body` del backend sin reescritura; sin preview inventado de chat; sin exponer HTTP code/ProblemDetails/stack trace
- [x] Tests (contratos, ETag/304, polling, read/read-all, targets, chat) y regresión D3–NV17
- [x] Build/tests/lint verdes con evidencia real; runtime **RUNTIME-BLOCKED** (emulador detectado por ADB sin framework Android operativo y sin credenciales)

## Fase B — FCM / Push remoto (IMPLEMENTED, RUNTIME PARCIAL)
- [x] Contrato token/device revisado en Backend/specs: no existe → **FCM-TOKEN-BACKEND-BLOCKED** (sin endpoint inventado)
- [x] COST-001 FCM $0: free tier, límites, sin tarjeta/billing, mitigación y alternativa Fase A
- [x] Firebase BoM `35.2.0` + `firebase-messaging`; plugin `google-services` condicional
- [x] `google-services.json`: ausente → no se generó archivo falso; `FCM_CONFIGURED=false`
- [x] Token `onNewToken` + consulta al activar sesión; solo memoria; fingerprint; sin token en logs
- [x] Logout/suspensión: cancela notificaciones locales y target pendiente; no rompe Firebase
- [x] `POST_NOTIFICATIONS` Android 13+ contextual con rationale; rechazo no rompe app; Android <13 sin runtime
- [x] Canales estables requests/visits/system con nombre, descripción e importancia `DEFAULT`; sin sonidos propios
- [x] Foreground: señal de refresco sin notificación duplicada; Background: notificación local con payload validado
- [x] Sesión inválida: push suprimido (sin exponer datos privados); target pendiente sobrevive Login
- [x] Payload data-only validado: id/title/body/category/target; URLs e intents no ejecutados
- [x] Navegación única request/conversation/visit/agreement/rental/building; desconocido → Notification Center
- [x] 47 tests nuevos + regresión; `testDebugUnitTest lintDebug assembleDebug` verdes
- [ ] Prueba real de push FCM → **FCM-RUNTIME-PARTIAL**: falta `google-services.json`, sender Backend; emulador ADB sin `system_server`/package service
- [x] Fase A preservada sin regresiones (in-app sigue siendo fuente funcional)
