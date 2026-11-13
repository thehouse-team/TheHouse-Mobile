# D13 — Reportes, verificación y moderación (Mobile)

| Campo | Valor |
| --- | --- |
| ID | D13 |
| Documento | `spec.md` |
| Versión | 1.0.0 |
| Fecha | 2026-11-12 |
| Estado documental | APROBADA (autorización humana explícita «EJECUTA D13») |
| Autoridad | `constitution.md` → `AGENTS.md` → `memoria.md` |
| Espejo Backend | `specs/nv-21-verification-moderation/spec.md` v1.1.0 (§6 y §6.1) |
| Alcance | Solo lectura del Backend. No modifica Backend ni specs ajenas. |

## 1. Contratos reales detectados (fuente de verdad)

1. **POST `/api/v1/reports`** (Bearer; el usuario sale del token; NUNCA `userId` del cliente):
   - Request `{ targetType, targetId, reason, details? }`.
   - `targetType`: `building | room | user` (SOLO estos tres; NV-16/NV-19 agregarán mensajes y reseñas).
   - `reason`: `fake | fraud | inappropriate | spam | other`.
   - `details`: opcional, ≤ 500 caracteres.
   - Respuesta 201 `{ id, status: "open", createdAt }`.
   - Errores: 400 validación / reportarse a sí mismo (target `user` propio), 401, 404 objetivo inexistente, 409 `report_exists` (no se repite reporte abierto del mismo objetivo), 429 (10 reportes/día, 60/min), 503.
2. **GET `/api/v1/landlord/buildings/{id}/verification`** (dueño; ajeno = 404):
   - 200 `{ status, lastDecision: { decision, reason, decidedAt } | null }`.
   - `status` = estado real del edificio; NV-21 agrega `rejected` y `suspended` a los de NV-12.
   - `decision`: `approved | changes_requested | rejected`; no revela quién revisó.
3. **`/api/v1/auth/login` y `/auth/refresh`**: sin cambio de contrato; una cuenta suspendida recibe **403 `code=account_suspended`**.
4. **`/admin/*`**: panel interno con roles `reviewer`/`admin`. NO es contrato de la app móvil (se usa con cliente HTTP del equipo). Mobile NO implementa UI admin.
5. **Moderación de contenido (académico):** en NV-21 la moderación real es la revisión de edificios (publicación oculta hasta `verified`; `suspended`/`rejected` no son públicos). No define moderación automática de texto/imágenes/videos. La spec directamente referenciada `nv-13-media-documents/spec.md` 1.0.0-borrador sí propone fotos/documentos, pero está **BORRADOR / NO EJECUTAR**, con almacenamiento/cifrado/COST-001 pendientes; video está expresamente fuera de alcance. No es un contrato ejecutable: **BLOCKED**, sin uploads, proveedores, claves ni aprobación local ficticia.

## 2. Alcance Mobile

- **Reports:** Api/DTO/mapper/repo/use case + formulario (razones reales, `details` con contador 0–500, CTA deshabilitado si inválido, loading, anti doble-submit, confirmación previa, mensaje de éxito). Entradas reales: Request Detail del estudiante (`request.room.id` y `request.room.buildingId`). No hay id de usuario expuesto en Chat/Solicitudes → el target `user` queda soportado por contrato/API sin inventar un ID ni entrada. Property Detail todavía usa `properties` sin mapping probado hacia `room/building`: no se equiparan IDs ni se inventa `targetType=property`.
- **Verification:** integrado en D12 sin duplicar: `GET verification` en el detalle de edificio del arrendador; muestra `status` real y `lastDecision.reason` amigable. Extendemos `BuildingStatus` con `rejected` y `suspended` (definidos por NV-21 con esos nombres exactos).
- **Suspensión de cuenta:** 403 `account_suspended` → `DomainError.AccountSuspended`, estado local de sesión suspendida y pantalla global «Tu cuenta está suspendida.». Se observa desde el cliente OkHttp existente y desde login/refresh/restauración. No se borra automáticamente identidad/tokens ni se reintenta refresh/acción tras suspensión. El usuario puede cerrar sesión explícitamente; la UI protegida se retira de composición y se bloquean nuevas llamadas autenticadas. No cambia el contrato HTTP ni se fabrican respuestas del servidor. Admin UI fuera de alcance.

## 3. Errores (sin exponer HTTP/códigos crudos)

400 validación · 401 sesión · 403 `account_suspended` → suspensión · 403 `own_building` (no aplica a app) · 404 recurso · 409 `report_exists` → «Ya reportaste esto y sigue en revisión.» · 429 rate limit · 503 temporal · Network/Timeout recuperables.

## 4. Seguridad

Nunca se envía `userId` (viene del JWT), ni JWT/tokens en logs (logging seguro existente), ni PII extra. Los motivos/detalles de reportes no se registran en logs. La validación Mobile es UX, no seguridad: el Backend es autoridad final.

## 5. Pruebas mínimas

- Reports: 201 válido con body exacto; 400 auto-reporte; 404 objetivo; 409 `report_exists`; 429 (+`Retry-After`); 503; 401; 403 `account_suspended` → AccountSuspended; details >500 bloqueado localmente.
- Verification: 200 con `status` y `lastDecision` (incluida `rejected` con reason); 404 ajeno.
- Estados nuevos de edificio parseados (`rejected`, `suspended`).
- Login/refresh: 403 `account_suspended` → `DomainError.AccountSuspended`, sin sesión activa ni logout destructivo.
- Regresión D3–D12 completa.

## 6. Trazabilidad y criterios de aceptación

| Requisito | Plan | Tasks | Aceptación |
| --- | --- | --- | --- |
| R01 Reportes reales | A/B/C | T01/T04/T06 | Body estricto sin userId, targets/motivos exactos, confirmación, éxito/duplicado/validación y anti doble envío |
| R02 Verificación del dueño | A/B | T02/T05 | Estado y decisión reales, razón solo desde respuesta de verificación; ningún aprobado/pending inventado |
| R03 Suspensión | A/B/C | T03 | Login, refresh y acciones notifican suspensión global sin limpiar tokens automáticamente ni continuar acciones protegidas |
| R04 Moderación | A/D | T05/T10 | Revisión de edificio real; no bypass por selección de archivo ni fake moderation; ausencia de contrato ejecutable documentada |
| R05 Calidad/evidencia | D | T07/T08/T09 | Tests/build/lint reales; runtime separado de DONE; no más de un ciclo correctivo |

## 7. Riesgos, OWASP MASVS y COST-001

- MASVS-AUTH: suspensión debe prevalecer sobre respuestas en vuelo y detener refresh/polling; autorización final siempre en Backend.
- MASVS-STORAGE/PRIVACY: reporte solo en memoria, sin logs ni persistencia sensible; no se añaden permisos, datos privados ni tokens a DTOs.
- MASVS-NETWORK: HTTPS y cliente existente; inspección acotada de Problem Details sin consumir el cuerpo que usan los repositorios.
- COST-001: ningún proveedor, dependencia, plan, subida o recurso cloud nuevo. Se usa únicamente el Backend existente con los límites documentados (reportes 10/día y 60/min, no retry automático). La viabilidad de media no está aprobada y se mantiene BLOCKED.
