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
- [x] Build/tests/lint verdes con evidencia real; runtime **RUNTIME-BLOCKED** (sin ADB/emulador)

## Fase B — FCM / Push remoto (PLANNED, DO NOT EXECUTE)
- [ ] Contrato de eventos/token confirmado o BLOCKED
- [ ] COST-001 FCM $0 demostrado (o BLOCKED)
- [ ] Token solo con sesión; logout lo desregistra
- [ ] Sin PII en payloads/bloqueo
- [ ] Prueba real de notificación
- [ ] Build/tests/lint verdes con evidencia real
- [ ] Firebase/permisos push/token FCM NO agregados en Fase A
