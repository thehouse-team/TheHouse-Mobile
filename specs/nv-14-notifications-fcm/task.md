# NV-14 — TASKS

| Campo | Valor |
|---|---|
| Spec | NV-14 Notifications (`specs/nv-14-notifications-fcm/`) |
| Fase A | In-App Notifications — **APPROVED / IMPLEMENTED & TESTED (unit)** |
| Fase B | FCM / Push remoto — **APPROVED / IMPLEMENTED & TESTED (unit/build)** — runtime **FCM-RUNTIME-PARTIAL** |

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

## Fase B — FCM / Push remoto (APROBADA / EJECUTADA, runtime parcial)

- [x] **NV-14-B-T01 — Contrato token/device**: búsqueda en Backend/specs disponible sin endpoint real → **FCM-TOKEN-BACKEND-BLOCKED**; no se inventó `POST /devices` ni similar.
- [x] **NV-14-B-T02 — COST-001**: FCM free tier $0, sin billing, límites/riesgo/alternativa documentados.
- [x] **NV-14-B-T03 — Configuración Firebase/FCM**: BoM `35.2.0`, `firebase-messaging`, plugin `com.google.gms.google-services` `4.6.0` aplicado condicionalmente; **sin `google-services.json`** (no se generó archivo falso).
- [x] **NV-14-B-T04 — Token/sesión/logout**: `onNewToken` + consulta al activar sesión; validación y almacenamiento solo en memoria; fingerprint truncado; logout cancela notificaciones locales/target pendiente sin romper Firebase.
- [x] **NV-14-B-T05 — Foreground/background**: payload data-only parseado; foreground solo señal de refresco (sin duplicar in-app); background notificación local; sin sesión válida se suprime.
- [x] **NV-14-B-T06 — Canales**: `thehouse_requests`, `thehouse_visits`, `thehouse_system` con nombre, descripción e importancia.
- [x] **NV-14-B-T07 — Permiso Android 13+**: `POST_NOTIFICATIONS` contextual en Notification Center; rationale visible; rechazo seguro; Android <13 sin runtime permission.
- [x] **NV-14-B-T08 — Navegación segura**: mapper único `NotificationDestinationResolver`; cold start + `onNewIntent`; target pendiente sobrevive Login; fallback Notification Center.
- [x] **NV-14-B-T09 — Tests/regresión**: 47 tests nuevos (parsing, 6 targets, fallback, onNewToken, permiso, foreground/background, sesión inválida, logs sin token) + regresión.
- [x] **NV-14-B-T10 — Build/evidencia**: `testDebugUnitTest lintDebug assembleDebug` ejecutado; runtime real **FCM-RUNTIME-PARTIAL** (sin `google-services.json` ni sender Backend; emulador detectado por ADB sin `system_server`/package service).
