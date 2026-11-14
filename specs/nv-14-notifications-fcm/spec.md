# NV-14 — NOTIFICATIONS

| Campo | Valor |
|---|---|
| Spec ID | NV-14 |
| Spec oficial | `specs/nv-14-notifications-fcm/` (no duplicar spec paralela) |
| Dependencias | NV-01, NV-04, NV-27, NV-03 (Data Safety/permisos) |
| Backend | `https://ca-thehouse-u2.grayisland-cb722b5f.eastus2.azurecontainerapps.io` |
| Estado backend conocido | CI 893/893, Azure HTTP 25/25, 4 rutas nuevas, sin breaking changes |

## Estado de fases

| Fase | Alcance | Status | Autorización |
|---|---|---|---|
| **Fase A** | Notificaciones in-app reales | **IMPLEMENTED & TESTED (unit)** | **APROBADA / EJECUTADA** (runtime E2E pendiente: RUNTIME-BLOCKED sin ADB/emulador) |
| **Fase B** | FCM / Push remoto | **PLANNED** | **DO NOT EXECUTE** |

> FCM/push remoto permanece separado y no autorizado. No agregar Firebase, no pedir permisos push ni crear token FCM en Fase A.

## Problema
El usuario necesita un centro de notificaciones in-app confiable (lista, estado leído/no leído, conteo, navegación por target y actualización periódica en foreground) basado en contratos reales del backend, sin implementar push remoto todavía.

## Fase A — Notificaciones in-app reales (alcance autorizado para gobernanza)

### Contratos reales a formalizar
- `GET /api/v1/users/me/notifications?unread=true&category=requests|visits|system&page&pageSize`
  - 200:
    ```json
    {
      "items": [{
        "id": "...",
        "type": "...",
        "category": "...",
        "title": "...",
        "body": "...",
        "target": { "kind": "...", "id": "..." },
        "count": 0,
        "read": false,
        "createdAt": "...",
        "updatedAt": "..."
      }],
      "page": 0,
      "pageSize": 0,
      "totalItems": 0,
      "totalPages": 0
    }
    ```
  - Orden: más reciente primero.
- `GET /api/v1/users/me/notifications/unread-count`
  - 200: `{ "unread": 0, "requests": 0, "visits": 0, "system": 0 }`
  - Guardar `ETag`; enviar después `If-None-Match`; `304` = **NO modificar estado**.
- `POST /api/v1/users/me/notifications/{id}/read` → `204` (ajena/inexistente → `404`)
- `POST /api/v1/users/me/notifications/read-all`
  - Body opcional: `{ "category": "requests|visits|system" }`
  - → `204`

### Funcionalidad planificada (Fase A)
- Lista de notificaciones con estados UI: Loading, Empty, Content, Error.
- Filtros: todas, requests, visits, system.
- Filtro `unread`, paginación.
- Mostrar: `title`, `body`, `count` cuando aplique, read/unread, fecha, indicador accesible.
- Unread count real y badge global (si `unread=0`, ocultarlo según diseño).
- `ETag` + `If-None-Match` + manejo `304` sin cambiar estado.
- Mark read (`POST /{id}/read`) al abrir una notificación no leída; actualizar estado tras confirmación.
- Read all (`POST /read-all`, endpoint único) y read all por categoría (body opcional) cuando aplique.
- Polling de unread count ~cada 30 s **solo con app activa/foreground**; refrescar al volver a foreground; detener polling cuando no aplique.
- Navegación mediante `target` usando rutas reales existentes.
- Integración con chat read: `/conversations/{id}/read` también marca su aviso como leído; evitar llamadas duplicadas y refrescar unread count cuando corresponda.
- Tests, runtime y evidencia.

### Targets reales y navegación
Mapear usando rutas reales existentes:
- `request` → `RequestDetailRoute`
- `conversation` → `ConversationRoute`
- `visit` → `VisitDetailRoute`
- `agreement` → Agreement Detail real
- `rental` → Mi renta
- `building` → `BuildingDetailRoute`

Si una ruta no existe: documentar bloqueo. **NO inventar rutas.**

### Reglas obligatorias (Fase A)
- `title` y `body` vienen en español desde Backend: **NO reescribir contenido**.
- Chat puede venir agrupado mediante `count`: **NO inventar texto/preview del mensaje** si Backend no lo entrega.
- Unread-count usa `ETag`; `304` = sin cambio de estado.
- Polling aproximado 30 s, solo foreground + refresh al volver a app.
- **NO** requests desde Compose.
- **NO** `WorkManager` cada 30 s.
- **NO** segundo Retrofit; reutilizar Retrofit, Session/JWT, `DomainError`, Design System y Navigation existentes.
- Seguir patrón actual: `Api → DTO → mapper → repository → ViewModel/use case → Compose`; **NO refactor global**.
- Al abrir notificación no leída: `POST /{id}/read`; read all usa endpoint único (**NO** POST individual por cada notificación).
- Errores: 401 sesión, 404 inexistente/ajena, 429/503 temporal, Network/Timeout recuperable. **Nunca** exponer HTTP code, ProblemDetails ni stack trace.

### Criterios de aceptación (Fase A, para futura implementación autorizada)
- Lista/filtros/paginación/unread operan contra contratos reales.
- `unread-count` respeta `ETag/If-None-Match/304` sin mutar estado en 304.
- `read` y `read-all` (global/categoría) actualizan estado tras confirmación del backend.
- Polling ~30 s solo en foreground, se detiene cuando no aplica y refresca al volver.
- Navegación por target usa rutas reales; rutas faltantes quedan bloqueadas/documentadas.
- Integración chat: leer conversación marca aviso leído sin duplicar llamadas y refresca unread count.
- Sin FCM, sin permisos push, sin token FCM.

## Fase B — FCM / Push remoto (PLANNED, DO NOT EXECUTE)

> Conservada documentalmente y separada de Fase A. **No ejecutar.**

- Firebase / FCM token / permisos de notificación / push remoto.
- Configuración Firebase/FCM solo tras autorización explícita de Fase B y COST-001 aprobado.
- Token FCM se registra solo con sesión válida y se desregistra en logout.
- Datos sensibles nunca en payloads de notificación; deep links validados.
- Sin permiso, la app funciona sin notificaciones; logout desregistra token.
- Firebase MCP permanece deshabilitado hasta spec/fase autorizada (AGENTS.md §18).
- **Prohibido ahora:** agregar Firebase, pedir permisos push, crear/registrar token FCM.

### COST-001 ($0) — Fase B
- Free tier: FCM sin costo en su tier gratuito documentado. Límites: cuota de mensajes/dispositivos del proveedor.
- Tarjeta: documentar si Firebase la exige (si la exige para este uso y no hay alternativa $0, BLOCKED).
- Riesgo cobro: exceder cuota con envíos masivos. Mitigación: envíos solo transaccionales, sin campañas.
- Alternativa: polling local o bandeja in-app sin push (Fase A cubre bandeja/polling in-app).

## Riesgos
- Contratos backend divergentes o rutas/targets no existentes en app.
- Condiciones de carrera entre polling, read/read-all y navegación.
- Filtrado accidental de detalles técnicos en UI/errores.
- Fase B: costo/límites FCM, spam, PII en payloads, permiso denegado.

## Impacto seguridad
- Fase A: HTTPS, sesión/JWT existente, sin secretos versionados, sin exponer PII/errores internos, deep links/targets validados contra rutas reales.
- Fase B: token FCM tratado como identificador sensible; Data Safety actualizada (NV-03) solo cuando se autorice.

## Relación con otras specs
- NV-10/NV-11/NV-12 (eventos/visitas/chat/cierre), NV-03 (Data Safety/release), NV-15 (privacidad), NV-27 (contrato).

## Evidencia esperada
- Fase A (futura, tras autorización de implementación): evidencia de contratos, ETag/304, polling/foreground, read/read-all, navegación por targets, integración chat, tests y runtime en `evidence.md`.
- Fase B: solo tras autorización explícita y COST-001 aprobado (flujo permiso/token/logout y prueba real). Hoy: sin evidencia de implementación.
