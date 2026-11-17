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
| **Fase A** | Notificaciones in-app reales | **IMPLEMENTED & TESTED (unit)** | **APROBADA / EJECUTADA** (runtime E2E pendiente: RUNTIME-BLOCKED; emulador detectado por ADB sin framework Android operativo y sin credenciales) |
| **Fase B** | FCM / Push remoto | **IMPLEMENTED & TESTED (unit/build)** | **APROBADA / EJECUTADA** (runtime **FCM-RUNTIME-PARTIAL**: sin `google-services.json`, sin sender Backend; emulador ADB sin `system_server`/package service) |

> Fase A se mantiene sin regresiones como fuente funcional del Notification Center. FCM (Fase B) actúa como evento/señal; el estado real se consulta al Backend.
> **FCM-TOKEN-BACKEND-BLOCKED:** no existe contrato real de registro de token/device en Backend; no se inventó endpoint.

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

## Fase B — FCM / Push remoto (APROBADA / IMPLEMENTADA — runtime parcial)

> Autorización humana explícita (2026-11-17) para implementar Mobile, tests/build/lint, commit y push. Sin merge, force push, Docker, secretos ni endpoints inventados.

### B1. Configuración Firebase/FCM
- Integración Android oficial: Firebase BoM `35.2.0`, `firebase-messaging`, plugin `com.google.gms.google-services` `4.6.0`.
- El plugin se aplica **solo** si existe `app/google-services.json`; sin ese archivo el build es reproducible y `BuildConfig.FCM_CONFIGURED=false`.
- `google-services.json` **no existe** en el repositorio y **no se genera un archivo falso**.
- Estado runtime: **FCM-RUNTIME-PARTIAL** (no hay proyecto Firebase ni sender).

### B2. Token FCM
- `FirebaseMessagingService.onNewToken()` y consulta de token al activarse la sesión.
- Token validado (longitud/espacios/controles) y mantenido **solo en memoria**.
- Nunca se loguea el token completo; estado público solo con fingerprint SHA-256 truncado.
- Persistencia local descartada: no es necesaria sin contrato Backend.
- **FCM-TOKEN-BACKEND-BLOCKED:** no se implementa `POST /devices`, `POST /fcm-token` ni similares. No existe contrato real en Backend/spec disponible.

### B3. Permiso Android 13+
- Permiso `POST_NOTIFICATIONS` declarado y solicitado **solo por acción del usuario** desde el Notification Center (nunca al startup).
- La tarjeta contextual explica el beneficio antes de pedir (rationale).
- Rechazo definitivo → acción "Abrir ajustes"; la app sigue operativa sin notificaciones.
- Android < 13 no requiere runtime permission y no muestra la tarjeta.

### B4. Canales
- Canales estables y mínimos: `thehouse_requests` (Solicitudes), `thehouse_visits` (Visitas), `thehouse_system` (Sistema).
- Importancia `DEFAULT`, descripción visible, sin sonidos ni recursos personalizados.
- Canal por categoría; categoría desconocida cae a Sistema.

### B5. Recepción foreground/background
- Mensajes FCM **data-only** (el bloque `notification` delegaría el render al SDK; el sender debe evitarlo).
- App foreground: parser seguro + señal de refresco; **no** se publica notificación de sistema para no duplicar el Notification Center de Fase A.
- App background/terminada: notificación local con canal, contenido del Backend y `PendingIntent` validado.
- Sin sesión válida (incluida restauración persistente fallida): el push se descarta y no se muestra contenido privado.
- Al pasar de sesión activa a logout/suspensión se cancelan notificaciones locales y target pendiente.

### B6. Payload FCM (contrato Mobile)
Claves data-only esperadas: `notificationId`, `category` (`requests|visits|system`), `title`, `body`, `targetKind`, `targetId`.
- `notificationId`: obligatorio, `[A-Za-z0-9][A-Za-z0-9._:-]{0,127}`.
- `title`: obligatorio, ≤200; `body`: opcional, ≤1000; sin caracteres de control.
- `targetKind`: `request|conversation|visit|agreement|rental|building`; desconocido → fallback seguro.
- `targetId`: validado con el mismo patrón; inválido → fallback.
- El payload nunca se interpreta como instrucciones, URLs o intents.

### B7. Navegación (reutiliza Fase A)
- Un único mapper `NotificationDestinationResolver` compartido por in-app y push:
  - `request → RequestDetailRoute`, `conversation → ConversationRoute`, `visit → VisitDetailRoute`, `agreement → Mi renta`, `rental → Mi renta`, `building → BuildingDetailRoute`.
  - Desconocido/ausente → Notification Center.
- `MainActivity` (`singleTop`) procesa intent cold start y `onNewIntent`; el target pendiente sobrevive a Login y se consume tras navegar.
- No se duplica lógica de target mapping ni se inventan rutas.

### B8. Seguridad y datos sensibles
- Logs solo con eventos/reason codes controlados: sin token, JWT, refresh token, títulos, cuerpos, ids, códigos de acuerdo, QR ni secretos.
- Notificación local con `VISIBILITY_PRIVATE` y versión pública genérica para lockscreen.
- Sin URLs, intents externos ni comandos desde payload.
- Firebase MCP permanece deshabilitado; no se agregaron secretos.

### Criterios de aceptación (Fase B)
- Integración FCM compila y no rompe el build sin configuración Firebase.
- Token obtenido/actualizado sin exponerlo en logs; registro Backend marcado BLOCKED.
- Permiso 13+ contextual, rechazo no rompe la app; <13 sin runtime permission.
- Canales requests/visits/system con nombres/descripciones/importancia.
- Foreground no duplica notificación in-app; background/terminada muestra notificación local.
- Navegación por target reutiliza rutas reales; target desconocido cae a Notification Center.
- Tests unitarios de parsing, targets, token, permiso, foreground/background y sesión inválida.
- Runtime real de push: **FCM-RUNTIME-PARTIAL** (falta `google-services.json`, sender Backend y dispositivo/ADB).

### COST-001 ($0) — Fase B
- **Free tier:** FCM sin costo en el tier gratuito documentado de Firebase; no se habilita billing ni servicios de pago.
- **Límites:** cuotas del proveedor para mensajes/instalaciones; uso transaccional, no campañas.
- **Tarjeta:** no se agregó método de pago; si Firebase lo exigiera para este uso y no hubiera alternativa $0, quedaría BLOCKED.
- **Riesgo de cobro:** envíos masivos o servicios adicionales (Analytics/Cloud Functions) fuera de alcance. Mitigación: solo FCM messaging, sender transaccional, sin campañas y sin servicios externos nuevos.
- **Alternativa gratuita:** Fase A (bandeja + polling in-app) sigue operando si push no está configurado o el usuario rechaza el permiso.

## Riesgos
- Contratos backend divergentes o rutas/targets no existentes en app.
- Condiciones de carrera entre polling, read/read-all y navegación.
- Filtrado accidental de detalles técnicos en UI/errores.
- Fase B: `google-services.json` ausente (runtime parcial), sender Backend inexistente, deprecación de token FCM clásico frente a FID, spam, PII en payloads, permiso denegado.

## Impacto seguridad
- Fase A: HTTPS, sesión/JWT existente, sin secretos versionados, sin exponer PII/errores internos, deep links/targets validados contra rutas reales.
- Fase B: token FCM tratado como identificador sensible (solo memoria + fingerprint); payload no confiable validado; logs sin PII; notificación local privada en lockscreen. Data Safety debe actualizarse en NV-03 antes de release con push real.

## Relación con otras specs
- NV-10/NV-11/NV-12 (eventos/visitas/chat/cierre), NV-03 (Data Safety/release), NV-15 (privacidad), NV-27 (contrato Backend).

## Evidencia esperada
- Fase A: contratos, ETag/304, polling/foreground, read/read-all, targets, chat, tests y runtime en `evidence.md`.
- Fase B: integración FCM, token, permiso, canales, foreground/background, fallback de navegación, tests/build/lint y estado `FCM-RUNTIME-PARTIAL` en `evidence.md`.
