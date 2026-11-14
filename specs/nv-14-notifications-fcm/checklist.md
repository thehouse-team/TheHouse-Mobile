# NV-14 — CHECKLIST

## Fase A — In-App Notifications (APPROVED FOR GOVERNANCE, IMPLEMENTATION: DO NOT EXECUTE)
- [ ] Contratos reales formalizados (list/unread-count/read/read-all) y DTOs/mappers sin campos inventados
- [ ] ETag/If-None-Match/304: 304 no modifica estado
- [ ] Polling unread ~30 s solo foreground + refresh al volver; se detiene cuando no aplica
- [ ] Mark read y read-all (endpoint único, categoría opcional) tras confirmación; sin POST individual masivo
- [ ] Targets mapeados a rutas reales (request/conversation/visit/agreement/rental/building) o bloqueo documentado
- [ ] `/conversations/{id}/read` marca aviso leído; sin duplicados; unread count refrescado
- [ ] `title/body` del backend sin reescritura; sin preview inventado de chat; sin exponer HTTP code/ProblemDetails/stack trace
- [ ] Tests (contratos, ETag/304, polling, read/read-all, targets, chat) y regresión D3–NV17
- [ ] Build/tests/lint/runtime con evidencia real (o RUNTIME-BLOCKED documentado)

## Fase B — FCM / Push remoto (PLANNED, DO NOT EXECUTE)
- [ ] Contrato de eventos/token confirmado o BLOCKED
- [ ] COST-001 FCM $0 demostrado (o BLOCKED)
- [ ] Token solo con sesión; logout lo desregistra
- [ ] Sin PII en payloads/bloqueo
- [ ] Prueba real de notificación
- [ ] Build/tests/lint verdes con evidencia real
- [ ] Firebase/permisos push/token FCM NO agregados en Fase A
