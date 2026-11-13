# D13.1 — Acuerdos de renta + Mi renta (Mobile)

| Campo | Valor |
| --- | --- |
| ID | D13.1 |
| Documento | `spec.md` |
| Versión | 1.0.0 |
| Fecha | 2026-11-13 |
| Estado documental | DOCUMENTADA — **NO EJECUTAR** (sin autorización humana de ejecución) |
| Autoridad | `constitution.md` → `AGENTS.md` → `memoria.md` |
| Precondición de rama | `main` **NO** contiene D13 (PR #16 OPEN / `mergeStateStatus: BLOCKED`, CI SUCCESS, `mergeable: MERGEABLE`) |
| Branch prevista | `feature/agreements-my-rental` (**NO creada aún**) |
| Espejo Backend | `TheHouse-Backend@main`: `TheHouse/Http/RentalsEndpoint.cs`, `TheHouse.Application/UseCases/RentalServices.cs`, `TheHouse.Application/Contracts/Rentals/RentalContracts.cs`, `TheHouse.Domain/Entities/Rental.cs` |
| Alcance | Solo lectura del Backend. Mobile no modifica Backend, no implementa pagos, no comparte teléfono. |

## 1. Contrato real (fuente de verdad, verificado en el repo del Backend)

Prefijo `ApiConventions.Prefix = /api/v1`. JWT Bearer (usuario desde el token, nunca del
cliente), JSON camelCase, Problem Details con extensión `code`, `Cache-Control: no-store`,
paginación `items/page/pageSize/totalItems/totalPages`, tamaños `MaximumBodyBytes = 2048`.

### 1.1 Las 9 rutas nuevas (las únicas permitidas)

| # | Método | Ruta | Éxito | Notas |
| --- | --- | --- | --- | --- |
| 1 | POST | `/landlord/agreements` | **201** `LandlordAgreementResponse` | Body `CreateAgreementRequest` |
| 2 | GET | `/landlord/agreements?status&roomId&page&pageSize` | 200 `LandlordAgreementListResponse` | `status` opcional |
| 3 | GET | `/landlord/agreements/{id}` | 200 `LandlordAgreementResponse` | **`closingCode = null`** |
| 4 | POST | `/landlord/agreements/{id}/code` | 200 `LandlordAgreementResponse` con `closingCode` | Solo `pending` |
| 5 | POST | `/landlord/agreements/{id}/cancel` | 200 `LandlordAgreementResponse` | Solo `pending` |
| 6 | POST | `/landlord/agreements/{id}/end` | 200 `LandlordAgreementResponse` | Body `EndAgreementRequest` **opcional** |
| 7 | POST | `/agreements/preview` | 200 `AgreementPreviewResponse` | Body `ClosingCodeRequest` |
| 8 | POST | `/agreements/accept` | **200** `RentalResponse` | Body `AcceptAgreementRequest` |
| 9 | GET | `/users/me/rental` | 200 `RentalResponse` | 404 `no_active_rental` |

Sin cambios en contratos existentes (D3–D13 intactos).

### 1.2 Bodies exactos

```jsonc
// 1) POST /landlord/agreements
{ "requestId": "...", "monthlyRent": 4500, "deposit": 4500,
  "startDate": "2026-12-01", "durationMonths": 6, "paymentDay": 5 }
// JsonUnmappedMemberHandling.Disallow: campos extra → 400.
// NO se envían: studentId, roomId, status, ownerId, version.

// 6) POST /landlord/agreements/{id}/end   (ambos opcionales; cuerpo puede omitirse)
{ "reason": "contract_finished|early_termination|other", "note": "..." }

// 7) POST /agreements/preview  — EXACTAMENTE uno de los dos
{ "code": "k7pm-2qxa" }   |   { "qrToken": "..." }

// 8) POST /agreements/accept
{ "code": "k7pm-2qxa", "acceptedTerms": true }   |   { "qrToken": "...", "acceptedTerms": true }
```

### 1.3 Respuestas (tipos reales)

| Tipo | Campos |
| --- | --- |
| `AgreementRoomSummary` | `id, buildingId, title, city` |
| `AgreementTermsResponse` | `monthlyRent, deposit, startDate, durationMonths, endDate, paymentDay, currency` (`"MXN"`) |
| `AgreementStudentSummary` | `name?, university?` (solo arrendador) |
| `ClosingCodeResponse` | `qrToken, backupCode, expiresAt` (**solo en crear y `/code`**) |
| `LandlordAgreementResponse` | `id, room, student, terms, status, closingCode?, createdAt, updatedAt, activatedAt?, endedAt?, cancelledAt?` |
| `LandlordAgreementListResponse` | `items, page, pageSize, totalItems, totalPages` |
| `AgreementBuildingSummary` | `name, rules?` |
| `AgreementLandlordSummary` | `name` (nombre de perfil, sin apellidos) |
| `AgreementPreviewResponse` | `id, room, building, landlord, terms, codeExpiresAt` |
| `RentalNextPayment` | `dueDate, amount` (informativo) |
| `RentalAddress` | `street, number, neighborhood, city, state, postalCode` |
| `RentalLocation` | `lat, lng` |
| `RentalResponse` | `agreementId, room, building, terms, nextPayment?, address, location, landlord, conversationId, activatedAt?` |

`POST /agreements/accept` y `GET /users/me/rental` devuelven **el mismo** `RentalResponse`.

### 1.4 Estados y enums EXACTOS (no inventar)

- Convenio: `pending`, `active`, `ended`, `cancelled` (filtro `status` acepta solo estos; vacío/null = todos).
- `reason` de `end`: `contract_finished`, `early_termination`, `other` (opcional; si viene, debe pertenecer al set).
- `end.reason` **NO** exige `note` en el Backend (regla real: `note` opcional, ≤300, trim→null si vacío). Mobile NO añade esa obligación.

### 1.5 Validaciones del Backend (solo UX en Mobile; el Backend es autoridad)

| Campo | Regla real |
| --- | --- |
| `requestId` | id válido y **dueño**; solicitud en `accepted` |
| `monthlyRent` | 500 – 100000 |
| `deposit` | 0 – 2 × `monthlyRent` |
| `startDate` | `YYYY-MM-DD`, entre −30 y +90 días desde hoy (hora CDMX) |
| `durationMonths` | 1 – 24 |
| `paymentDay` | 1 – 28 |
| `note` | ≤ 300 caracteres |
| `page` / `pageSize` | `page ≥ 1`; `pageSize` **1 – 50** (default 1/20) |
| `code` | 8 símbolos de `ABCDEFGHJKLMNPQRSTUVWXYZ23456789`; acepta `-`, espacios y minúsculas (`k7pm-2qxa` → `K7PM2QXA`); otro formato → **400** |
| `qrToken` | 43 chars `[A-Za-z0-9-_]` (base64url) |
| `code` vs `qrToken` | debe enviarse **exactamente uno**; ambos o ninguno → **400** |
| `acceptedTerms` | debe ser `true`; distinto → **400** |

### 1.6 Código de cierre

- Vigencia **10 minutos** (`ClosingCode.Lifetime`).
- Solo se guarda **SHA-256** de `code` y `qrToken`; el valor en claro sale **una sola vez** (crear y `/code`).
- `FailedAttempts` máx. **5** por convenio → bloqueado → 404 `code_invalid`.
- `POST /code` invalida el código anterior. 409 `agreement_state` si el convenio no está `pending`.
- **El detalle (ruta 3) y la lista (ruta 2) devuelven `closingCode = null`.** No asumir que el código inicial sigue disponible.

### 1.7 Rate limits reales (RNF-17)

| Límite | Ventana | `Retry-After` | `code` |
| --- | --- | --- | --- |
| Escrituras/lecturas de convenios (`RentalsPerWindow`) | 60 / 60 s por usuario | 60 | (sin `code`) |
| Preview/accept por usuario | 10 / 60 s | 60 | (sin `code`) |
| Preview/accept por IP | 30 / 60 s | 60 | (sin `code`) |
| **Intentos con código por cuenta** | **5 / 15 min** | **900** | **`too_many_attempts`** |

El contador de 5 cuenta **todos** los intentos (no solo los fallidos). **No hay retry automático.**

### 1.8 Mapeo de errores real (`UserEndpointSupport.RunAsync`)

| HTTP | `code` | Origen |
| --- | --- | --- |
| 400 | (sin `code`; `errors.request = ["Entrada inválida."]`) | validación / JSON malformado / 415 / 413 |
| 401 | — | sesión |
| 403 | `landlord_required` | sin perfil de arrendador |
| 404 | `request` · `agreement` · `code_invalid` · `no_active_rental` | no encontrado / ajeno (BOLA) |
| 409 | `request_state` · `room_unavailable` · `agreement_open` · `agreement_state` · `version_conflict` | conflicto de estado |
| 410 | `code_expired` · `code_used` | `GoneException` |
| 429 | `too_many_attempts` · (genérico) | `RateLimitExceededException` + header `Retry-After` |
| 503 | — | dependencia no disponible |

`room_unavailable` también aparece en **accept** (compensación: el cuarto cambió a mitad y el
convenio vuelve a `pending` **sin código**).

## 2. Alcance Mobile

### 2.1 Arrendador

- **Crear acuerdo desde una solicitud aceptada (D9.1).** CTA solo visible si `status == ACCEPTED`.
  Body exacto §1.2; anti doble submit; loading; éxito → muestra términos + `backupCode` + `expiresAt`.
  409 `request_state` / `room_unavailable` / `agreement_open` con mensaje amigable.
- **Lista** con filtro `status` (`pending|active|ended|cancelled`), `roomId` opcional, paginación
  (pageSize ≤ 50) y estados `Loading / Empty / Content / Error`.
- **Detalle** con `closingCode = null` (no asumir código disponible), estados del convenio,
  `student`, `terms` y timestamps reales.
- **Regenerar código** (`POST /code`): muestra `backupCode` + `expiresAt` (10 min).
  409 `agreement_state`.
- **Cancelar** (solo `pending`) y **Finalizar** (solo `active`) con `reason`+`note`.
  409 `agreement_state` en ambos.

### 2.2 Estudiante

- **Introducir código** → `POST /agreements/preview` → **preview** con `room`, `building{name,rules}`,
  `landlord{name}`, `terms`, `codeExpiresAt` → revisar → **checkbox de aceptación explícito** →
  `POST /agreements/accept` (`acceptedTerms: true`) → 200 `RentalResponse` → **Mi renta**.
- **Nunca aceptar automáticamente**: el CTA está deshabilitado hasta marcar la aceptación.
- **Mi renta** (`GET /users/me/rental`): habitación, edificio, términos, dirección, arrendador,
  `activatedAt`, `nextPayment` **solo informativo**, y acceso a Chat vía `conversationId`.
- 404 `no_active_rental` → estado Empty con mensaje.

### 2.3 Entradas UX (sin romper la navegación existente)

| Entrada | Destino | Guard |
| --- | --- | --- |
| Home → «Mi renta» | `mi_renta` | sesión (guest → Login) |
| Home → «Introducir código de renta» | `codigo_renta` | sesión (guest → Login) |
| Detalle de solicitud arrendador (`ACCEPTED`) → «Crear acuerdo» | `acuerdo/crear/{requestId}` | sesión |
| Home → «Mis acuerdos (arrendador)» | `acuerdos_arrendador` | sesión |
| Mi renta → «Hablar con arrendador» | `ConversationRoute(conversationId)` | sesión |

`bottomTabs` (inicio/explorar/smart_match/favoritos) **no se modifica**.

## 3. Pagos — PROHIBIDO

`nextPayment {dueDate, amount}` es **informativo** (el Backend lo calcula; no cobra — RES-03).

- NO existen ni se crean: API de pagos, checkout, Stripe, Mercado Pago, tarjeta,
  transferencia, simulación de pago ni CTA «Pagar».
- Si en el futuro aparece un botón «Pagar»: **ocultarlo** o **deshabilitarlo** con
  «Pago no disponible desde la aplicación.»
- No hay fake payment, ni estados locales de pago, ni persistencia de `nextPayment`.

## 4. Privacidad y contacto

- **NO** se muestra ni se solicita teléfono del arrendador (el contrato solo expone `landlord.name`).
- El único canal de contacto es `conversationId` → **Chat D9.1 existente** (`ConversationRoute`).
  **NO** se duplica Chat, **NO** se crea segundo flujo de mensajes.
- No se muestran `ownerId`, `studentId`, `requestId` salvo como parámetro interno de navegación.

## 5. QR

- El Backend entrega `qrToken`; Mobile **no** lo muestra en UI (no existe UX segura aprobada
  para secretos) ni lo persiste.
- **NO** existen dependencias de cámara/QR aprobadas (verificado en `app/build.gradle.kts`:
  sin zxing/camera/mlkit). Por lo tanto:
  - Ingreso **manual de `backupCode`** (formato `k7pm-2qxa`, con `-`/espacios y minúsculas aceptados).
  - **NO** se agrega SDK, librería ni permiso de cámara en esta fase.
  - Escaneo de QR y representación QR → **pendiente documentado** (§10).

## 6. Arquitectura (patrón existente, sin refactor global)

```
Api (Retrofit) → DTO (kotlinx.serialization) → mapper → repository (Result + ProblemDetails)
  → use case / ViewModel (BaseViewModel + UiState/Event/Effect) → Compose (D11)
```

Reutiliza: **un único Retrofit** existente (`AppContainer.retrofit`), `BearerTokenInterceptor`,
`RefreshAuthenticator`, `Json`, `ProblemDetailsDto`, `DomainError`/`RequestException`-style,
`BaseViewModel`, `StateViews` (Loading/Empty/Error), tema/`Dimens` D11, Chat D9.1, Landlord D12.

Archivos previstos (agregados, no reemplazos):

```
data/remote/api/AgreementsApi.kt
data/remote/dto/agreements/AgreementDtos.kt
data/agreements/mapper/AgreementMappers.kt
data/agreements/repository/AgreementsRepositoryImpl.kt
domain/model/Agreement.kt
domain/repository/AgreementsRepository.kt
domain/error/AgreementError.kt            (AgreementErrorCode + AgreementException)
domain/usecase/AgreementUseCases.kt
presentation/landlord/AgreementErrorMessages.kt
presentation/landlord/LandlordAgreementsScreen.kt (+ ViewModel)
presentation/landlord/LandlordAgreementDetailScreen.kt (+ ViewModel)
presentation/landlord/CreateAgreementScreen.kt (+ ViewModel)
presentation/rental/RentalCodeScreen.kt (+ ViewModel)
presentation/rental/AgreementPreviewScreen.kt (+ ViewModel)
presentation/rental/MyRentalScreen.kt (+ ViewModel)
presentation/navigation/AgreementRoutes.kt
```

Modificaciones puntuales **autorizadas por la orden D13.1** (registrado en §12):
`AppContainer`, `U2ViewModelFactory`, `MainActivity` (solo registro de `composable`),
`HomeScreen`/`HomeViewModel` (entradas), `LandlordRequestDetailScreen`/`ViewModel` (CTA).

## 7. Errores UX (nunca HTTP, ProblemDetails crudo ni stack trace)

Fuente: textos exactos de la orden D13.1 o `UserResourceProblems.DetailFor` (texto real del Backend).

| Condición | Mensaje Mobile |
| --- | --- |
| 404 `code_invalid` | **«El código no es válido.»** |
| 410 `code_expired` | **«El código expiró. Solicita uno nuevo al arrendador.»** |
| 410 `code_used` | **«Este código ya fue utilizado.»** |
| 429 `too_many_attempts` | **«Realizaste demasiados intentos. Inténtalo más tarde.»** (+ `Retry-After` = 900 s) |
| 404 `no_active_rental` | «No tienes una renta activa.» |
| 409 `request_state` | «La solicitud ya no puede realizar esta acción.» (existente D9) |
| 409 `room_unavailable` | «El cuarto ya no está disponible.» (existente D9) |
| 409 `agreement_open` | «El cuarto ya tiene un convenio pendiente o activo.» |
| 409 `agreement_state` | «El convenio no se puede cambiar en su estado actual.» |
| 409 `version_conflict` | «Los datos cambiaron desde la última lectura; vuelve a cargarlos.» (existente D12) |
| 403 `landlord_required` | «Necesitas activar el modo arrendador para realizar esta acción.» (existente D12) |
| 400 validación | «Revisa los datos ingresados.» |
| 401 | «Tu sesión expiró. Inicia sesión de nuevo.» |
| 429 genérico | «Demasiadas solicitudes. Espera un momento e intenta de nuevo.» (+ `Retry-After`) |
| 503 | «El servicio no está disponible ahora. Intenta más tarde.» |
| Network / Timeout | mensajes existentes D9 |

Reglas: **respetar `Retry-After`** (mostrar espera restante, CTA bloqueado mientras dure),
**sin retry automático**, sin crash, sin exponer status HTTP ni `code` interno.

## 8. Seguridad

- Nunca loggear: JWT, refresh token, `backupCode`, `qrToken`, PII innecesaria.
  Logging seguro existente (`SafeHttpLoggingInterceptor`) intacto; **cero logs nuevos** con secretos.
- `closingCode` solo en estado de ViewModel en memoria; se limpia al salir de la pantalla.
  **No** se persiste en DataStore/Room/archivos.
- El usuario sale del **JWT**; no se envía `userId`/`studentId`/`ownerId` en bodies.
- Mobile **no** es autoridad: todo estado lo decide el Backend; Mobile solo refleja.
- Sin permisos nuevos, sin secretos, sin dependencias nuevas.

## 9. No-alcance

- Pagos/checkout/pasarelas (§3).
- Escaneo de QR, librerías de cámara, permisos nuevos (§5).
- Modificar Backend, contratos existentes D3–D13, o specs ajenas.
- Segundo Retrofit, Hilt/Koin, módulos Gradle nuevos, refactor global.
- Historial de convenios del estudiante, reseñas, notificaciones FCM, mapa interactivo.
- Admin UI, panel interno `/admin/*`.

## 10. Riesgos y pendientes

| ID | Riesgo / deuda | Estado |
| --- | --- | --- |
| `BLOCKED-PR-16` | `main` sin D13 → sin rama ni implementación | **ABIERTO** |
| `QR-SCAN-PENDING` | Escaneo/representación QR sin infraestructura aprobada | **PENDIENTE** |
| `MAP-PENDING` | `location{lat,lng}` se parsea pero no se renderiza (no hay mapa en la app) | **PENDIENTE** |
| `RUNTIME-PENDING` | Flujo punta a punta requiere emulador/ADB + credenciales + solicitud aceptada de prueba | **PENDIENTE** |
| `END-NOTE-RULE` | El Backend **no** exige `note` con `reason=other`; Mobile no debe inventar esa obligación | CERRADO (regla real adoptada) |

## 11. Pruebas mínimas (orden §18)

**Repository (MockWebServer, estilo `RequestsRepositoryIntegrationTest`)**

- Landlord: create **201** con body exacto; 409 `request_state`; 409 `room_unavailable`;
  409 `agreement_open`; list + filtro `status` + paginación (pageSize ≤ 50);
  detail con **`closingCode == null`**; `POST /code` 200 con `closingCode`;
  409 `agreement_state` en `/code`; cancel; end (`reason`+`note`); 403 `landlord_required`.
- Estudiante: preview por `code`; preview por `qrToken`; 400 si se envían ambos/ninguno;
  accept `acceptedTerms=true` → 200 `RentalResponse`; 400 si `acceptedTerms != true`;
  404 `code_invalid`; 410 `code_expired`; 410 `code_used`;
  429 `too_many_attempts` **+ header `Retry-After: 900`**; 429 genérico + `Retry-After: 60`;
  `GET /users/me/rental` 200; 404 `no_active_rental`; `conversationId` presente → Chat D9.1.
- `Retry-After` leído del header; sin reintento automático.

**ViewModel / UX**

- Create: anti doble submit (segundo tap no reenvía), loading, errores mapeados.
- Preview/Accept: CTA deshabilitado hasta marcar aceptación explícito; nunca auto-accept;
  bloqueo por `Retry-After`; mensajes exactos §7.
- Mi renta: `nextPayment` **solo informativo**, **sin acción de pago**;
  botón de Chat usa `conversationId`; sin teléfono.
- Lista: Loading/Empty/Content/Error; paginación.

**Regresión:** D3–D13 en verde (requiere `main` con D13).

## 12. Trazabilidad

| Req | Plan | Tasks | Criterio de aceptación |
| --- | --- | --- | --- |
| R01 Landlord: crear acuerdo | F1/F2 | T05/T11 | Body exacto §1.2, 201, CTA solo desde solicitud `ACCEPTED`, anti doble submit, 409 `request_state`/`room_unavailable`/`agreement_open` |
| R02 Landlord: lista/detalle | F1/F2 | T06/T12 | Filtro `status`, paginación ≤50, Loading/Empty/Content/Error, **`closingCode == null` en detail** |
| R03 Landlord: código | F1/F2 | T07/T13 | `POST /code` 200 con `backupCode`+`expiresAt` (10 min), 409 `agreement_state`, sin logs ni persistencia |
| R04 Landlord: cancelar/finalizar | F1/F2 | T08/T14 | `cancel` solo `pending`, `end` solo `active` con `reason`/`note` reales, 409 `agreement_state` |
| R05 Estudiante: preview | F1/F3 | T09/T15 | `code` **o** `qrToken`, 200 con `room/building/landlord/terms/codeExpiresAt`, sin aceptación automática |
| R06 Estudiante: aceptar | F1/F3 | T10/T16 | `acceptedTerms` **true** solo con checkbox explícito; 200 → Mi renta |
| R07 Errores de código | F1/F5 | T17 | `code_invalid`/`code_expired`/`code_used`/`too_many_attempts` con mensajes exactos + `Retry-After` 900, sin retry automático |
| R08 Mi renta | F4 | T18 | `GET /users/me/rental`, datos reales, `no_active_rental` → Empty, acceso a Chat |
| R09 Pagos | F4 | T18 | `nextPayment` informativo; **cero** UI/acción de pago |
| R10 Privacidad/chat | F4 | T18 | Sin teléfono; contacto solo vía `conversationId` → Chat D9.1 |
| R11 QR | F5 | T17 | Ingreso manual; sin SDK/cámara/permisos; `QR-SCAN-PENDING` documentado |
| R12 Navegación | F2/F3/F4 | T11–T18 | Entradas Home/detalle de solicitud; `bottomTabs` intacto; guest → Login |
| R13 Error mapping | F1–F4 | T17 | Tabla §7 completa; nunca HTTP/ProblemDetails crudo/stack trace |
| R14 Seguridad | F1–F4 | T17 | Sin logs de secretos; `closingCode` en memoria y limpiado; sin permisos nuevos |
| R15 Calidad/evidencia | F6 | T19/T20 | `compileDebugKotlin` → set completo una vez; máx. 1 ciclo correctivo; evidencia real |

## 13. OWASP MASVS y COST-001

- **MASVS-AUTH / ACCESS:** 403 `landlord_required` manejado; estado siempre decidido por el Backend; sin bypass local.
- **MASVS-STORAGE:** ningún secreto persistido; `backupCode`/`qrToken` solo en memoria de UI.
- **MASVS-NETWORK:** HTTPS + cliente OkHttp existente; sin segundo cliente; Problem Details inspeccionado sin consumir cuerpo dos veces.
- **MASVS-CODE / PRIVACY:** sin PII de más, sin teléfono, sin logs de tokens ni códigos.
- **COST-001 ($0):** sin proveedores, planes, dependencias ni recursos cloud nuevos. Free tier: Backend propio ya desplegado. Tarjeta: no. Riesgo de cobro: ninguno. Mitigación: cero integraciones externas. Alternativa: N/A.
