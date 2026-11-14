# NV-14 — TASKS

| Campo | Valor |
|---|---|
| Spec | NV-14 Notifications (`specs/nv-14-notifications-fcm/`) |
| Fase A | In-App Notifications — **APPROVED FOR GOVERNANCE** — **IMPLEMENTATION: DO NOT EXECUTE todavía** |
| Fase B | FCM / Push remoto — **PLANNED** — **DO NOT EXECUTE** |

## Fase A — Tasks trazables (APROBADA PARA IMPLEMENTACIÓN, EJECUTADA)

- [x] **NV-14-A-T01 — Contratos/DTOs**: modelar contratos reales (`GET /users/me/notifications`, `GET /users/me/notifications/unread-count`, `POST /{id}/read`, `POST /read-all`) con DTOs/mappers alineados a payloads reales (sin campos inventados).
- [x] **NV-14-A-T02 — Repository/use cases**: Api → DTO → mapper → repository → use cases para list/filtros/paginación, unread-count, read, read-all; errores mapeados a `DomainError`; reutilizar Retrofit/Session/JWT existentes.
- [x] **NV-14-A-T03 — Notification center UI**: Loading/Empty/Content/Error, filtros todas/requests/visits/system, unread, paginación, items (title/body/count/read/fecha/indicador accesible), badge global (oculto si unread=0). `title/body` del backend sin reescritura.
- [x] **NV-14-A-T04 — Unread count + ETag/304**: guardar `ETag`, enviar `If-None-Match`, `304` sin modificar estado.
- [x] **NV-14-A-T05 — Polling/foreground**: unread count ~30 s solo app activa/foreground, refresh al volver, detener cuando no aplique; `CancellationException` rethrow; sin requests desde Compose ni WorkManager cada 30 s.
- [x] **NV-14-A-T06 — Target navigation**: mapear `request→RequestDetailRoute`, `conversation→ConversationRoute`, `visit→VisitDetailRoute`, `agreement→Agreement Detail real`, `rental→Mi renta`, `building→BuildingDetailRoute`; si falta ruta, documentar bloqueo (no inventar).
- [x] **NV-14-A-T07 — Read/read-all + chat integration**: `POST /{id}/read` al abrir no leída (tras confirmación); `POST /read-all` único (+ body categoría opcional); `/conversations/{id}/read` marca aviso leído; evitar duplicados; refrescar unread count cuando corresponda.
- [x] **NV-14-A-T08 — Tests/regresión**: list/filtros/paginación, unread 200, ETag/If-None-Match/304 mantiene estado, read 204/404, read-all (+ categoría), polling ~30 s/start-stop, foreground refresh, targets, count agrupado, integración chat read. Regresión D3–NV17.
- [x] **NV-14-A-T09 — Runtime/evidence**: `compileDebugKotlin` y, si verde, `testDebugUnitTest lintDebug assembleDebug`; runtime ADB si disponible o RUNTIME-BLOCKED; evidencia real en `evidence.md`.

## Fase B — FCM / Push remoto (PLANNED, DO NOT EXECUTE)

- [ ] **NV-14-B-T01** — Confirmar contrato de eventos/token (NV-27) o BLOCKED. *(NO EJECUTAR)*
- [ ] **NV-14-B-T02** — Evaluación COST-001 Firebase/FCM (o BLOCKED). *(NO EJECUTAR)*
- [ ] **NV-14-B-T03** — Configurar Firebase/FCM (solo tras B-T01/B-T02 y autorización explícita). *(NO EJECUTAR)*
- [ ] **NV-14-B-T04** — Registro/desregistro de token con sesión/logout. *(NO EJECUTAR)*
- [ ] **NV-14-B-T05** — Manejo de notificaciones + deep links seguros (sin PII). *(NO EJECUTAR)*
- [ ] **NV-14-B-T06** — Tests y prueba real de notificación. *(NO EJECUTAR)*
- [ ] **NV-14-B-T07** — Build/tests/lint y evidence.md. *(NO EJECUTAR)*
