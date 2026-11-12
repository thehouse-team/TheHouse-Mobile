# D12 — Modo arrendador, edificios y cuartos (Mobile)

| Campo | Valor |
| --- | --- |
| ID | D12 |
| Documento | `spec.md` |
| Versión | 1.0.0 |
| Fecha | 2026-11-12 |
| Estado documental | APROBADA (autorización humana explícita «EJECUTA D12» en la orden de trabajo) |
| Autoridad | `constitution.md` → `AGENTS.md` → `memoria.md` |
| Espejo Backend | `specs/nv-12-landlord-buildings-rooms/spec.md` v1.1.0 (§6 y §6.1) del repo `TheHouse-Backend` |
| Alcance | Solo lectura del Backend. Mobile no modifica Backend. |

## 1. Objetivo

Implementar en Mobile el flujo REAL de arrendador: modo arrendador, edificios y
cuartos (crear, editar, eliminar, publicar/revisión) con concurrencia optimista
mediante `version`, reutilizando Retrofit/auth/DomainError/Design System D11.
NO inventar endpoints, campos, enums ni estados.

## 2. Contrato real (fuente de verdad: Backend §6.1)

Prefijo `/api/v1`, JWT Bearer, JSON camelCase, Problem Details en español con
`code`, `Cache-Control: no-store`, paginación `page`/`pageSize` con sobre
`items/page/pageSize/totalItems/totalPages`.

| Método | Ruta | Notas |
| --- | --- | --- |
| POST | `/landlord/profile` | Activa modo arrendador. 201 / 409 `landlord_exists`. |
| GET | `/landlord/profile` | 200 / 404 si aún no es arrendador. |
| POST | `/landlord/buildings` | 201 edificio `draft` v1 / 403 `landlord_required` / 404 universidad / 409 `buildings_limit`. |
| GET | `/landlord/buildings?page&pageSize` | Sobre NV-01 + `summary{buildings,rooms,available,occupancyPercent,monthlyIncome}`. |
| GET | `/landlord/buildings/{id}` | Detalle (dueño). |
| PATCH | `/landlord/buildings/{id}` | Campos a cambiar + `version` en cuerpo. 409 `version_conflict`. |
| DELETE | `/landlord/buildings/{id}?version=N` | 204. 409 `version_conflict`/`building_has_rented_rooms`. |
| POST | `/landlord/buildings/{id}/rooms/generate` | `{floors, roomsPerFloor, numbering:"floor_prefix", base}`. 409 `rooms_limit`/`room_number_taken`/`building_in_review`. |
| GET | `/landlord/buildings/{id}/rooms?page&pageSize` | Orden floor asc, number asc. |
| PATCH | `/landlord/rooms/{id}` | Campos + `version`. 409 `version_conflict`/`room_rented`/`room_number_taken`. |
| DELETE | `/landlord/rooms/{id}?version=N` | 204. |
| POST | `/landlord/rooms/{id}/duplicate` | 201 cuarto nuevo. |
| POST | `/landlord/rooms/{id}/copy-from` | `{sourceRoomId, version}`. |
| POST | `/landlord/rooms/{id}/pause` | `available` → `paused`. 409 `room_state`. |
| POST | `/landlord/rooms/{id}/resume` | `paused` → `available`. 409 `room_state`. |
| GET | `/landlord/buildings/{id}/publication` | `{canPublish, steps:[{id,done}]}`. |
| POST | `/landlord/buildings/{id}/publish` | 202 edificio `in_review`. 409 `publication_incomplete`/`building_state`. |

Enums EXACTOS del Backend:

- Edificio `type`: `rooms_building`, `house_rooms`, `full_apartment`.
- `allowedTenants`: `women`, `men`, `any`.
- `services` (catálogo cerrado): `wifi`, `water`, `electricity`, `gas`, `hot_water`, `laundry`, `kitchen`, `furnished`, `parking`, `security`, `cleaning`.
- Estados del edificio: `draft`, `in_review`, `verified`.
- Cuarto `type`: `single`, `double`, `shared`; `bathroom`: `private`, `shared`; `furniture`: `bed`, `desk`, `chair`, `closet`, `dresser`, `tv`, `fridge`, `microwave`; `minContract`: `four_months`, `six_months`, `annual`.
- Estados del cuarto: `setup`, `ready`, `in_review`, `available`, `paused`, `rented`.
- `price` entero 500–50000 MXN; `sizeM2` 4–200; `availableFrom` `YYYY-MM-DD`; `rules` ≤2000; teléfono E.164 México (+52 + 10 dígitos); `acceptedTermsVersion` = versión vigente (`2026-11`, `LandlordTerms:Version` del Backend).
- Perfil: `{fullName, phone, rentalMode(individual|agency), rfc?, acceptedTermsVersion, createdAt}`; `rfc` obligatorio si `agency`.
- Campos calculados (`distanceToUniversityKm`, `walkingMinutes`), `status`, `version`, `ownerId` nunca se envían al Backend (400).
- Rate limits Backend: 60 lecturas / 30 escrituras por minuto; `generate` 10/hora; 429 con `Retry-After`.

## 3. Reglas de producto Mobile

1. **Landlord mode:** acceso desde Home (usuario autenticado). Si `GET /landlord/profile` devuelve 404 → pantalla de alta del perfil. Si un endpoint devuelve 403 `landlord_required` → mensaje: «Necesitas activar el modo arrendador para realizar esta acción.» con navegación al alta.
2. **Version/concurrencia:** cada pantalla guarda la `version` entregada por el Backend; `PATCH` envía `version` en el cuerpo, `DELETE` en query. Tras éxito se actualiza con la versión devuelta. 409 `version_conflict` → mensaje «Esta información fue actualizada en otro lugar. Actualiza los datos e inténtalo de nuevo.» + botón recargar. PROHIBIDO: retry automático, sobrescribir silenciosamente o reenviar la versión vieja.
3. **Estados reales:** el edificio tras `publish` queda `in_review` → se muestra «Tu publicación está en revisión.»; no se muestra «Publicado». No se inventan ratings, ocupación falsa, verificación ni dirección pública; se muestran únicamente los campos del contrato.
4. **Errores:** 400 validación, 401 sesión, 403 `landlord_required` específico, 404 recurso inexistente, 409 `version_conflict` refresh, 429 rate limit, 503 temporal, Network/Timeout recuperables. Nunca se muestran status HTTP, `code` interno ni ProblemDetails crudo. Nunca crash.
5. **Formularios:** campos agrupados, validación local básica, errores junto al campo, CTA deshabilitado si inválido, loading mientras envía, protección contra doble submit. El Backend es autoridad final; no se envían campos desconocidos.
6. **Multimedia:** el contrato de NV-12 no incluye fotos → NO se implementa upload (D13/NV-13).
7. **D9.1:** no se duplica la gestión de solicitudes; el dashboard enlaza a la pantalla D9.1 existente.

## 4. Arquitectura

Patrón existente: Api → DTO → mapper → repository → use case/ViewModel → Compose.
Reutiliza Retrofit/`BearerTokenInterceptor`/`RefreshAuthenticator`, `ProblemDetailsDto`,
`DomainError`, `Session`, `BaseViewModel`, `StateViews`, `Dimens` y tema D11.
Sin segundo Retrofit, sin nuevas dependencias, sin módulos Gradle.

## 5. Fuera de alcance

- Verificación/aprobación (NV-21), fotos/documentos (NV-13), búsqueda pública (NV-14), `rented` (NV-18).
- Pasos del prototipo sin soporte Backend (p. ej. elementos que no estén en el contrato).
- Acciones `duplicate`/`copy-from` exponen contrato en Api/Repository; su UI se documenta como deuda (no es requisito de la orden D12).

## 6. Pruebas mínimas

Repository (MockWebServer, estilo `VisitsRepositoryIntegrationTest`):
- Profile: 201, 404, 409 `landlord_exists`.
- 403 `landlord_required` en buildings.
- Buildings: list+summary, detail, create, PATCH envía `version`, PATCH 200 actualiza versión, DELETE envía `?version`.
- 409 `version_conflict`, 409 `publication_incomplete`.
- Rooms: list, create (generate), PATCH con `version`, DELETE con `?version`, pause/resume.
- 400, 401, 404, 429, 503 mapeados a `LandlordException` con `LandlordErrorCode`.

Regresión: tests D3–D11 intactos y verdes.
