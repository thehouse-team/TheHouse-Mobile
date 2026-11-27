# SPEC D20 — Rooms, Discovery and Interactions (FASE B)

| Campo | Valor |
|---|---|
| ID | D20-Rooms-Discovery-Interactions |
| Versión | 1.0.0 |
| Fecha | 2026-11-27 |
| Estado | APROBADA (autorización humana explícita, mensaje Fase B) |
| Rama | `feature/rooms-discovery-interactions` desde `origin/main` @ `b4283a6` |
| Baseline | PR #35 MERGED; `74c329c` integrado; 547 tests unitarios; working tree limpio |
| Independencia | Spec nueva. No sobrescribe D18/D19.1. Sin Fase C. Sin merge a main. |

## 1. Problema

Home y Explore todavía consumen `GET /api/v1/properties` (legacy). Smart Match y
D18 ya usan `roomId` real. Faltan: migración de descubrimiento a `rooms/search`,
detalle unificado, favoritos reales, solicitudes, conversaciones y visitas con
`roomId`.

## 2. Contrato Backend auditado (solo lectura)

Fuentes: `TheHouse/Http/SearchEndpoint.cs`, `FavoritesEndpoint.cs`,
`MessagingEndpoint.cs`, `VisitsEndpoint.cs`; DTOs en
`TheHouse.Application/Contracts/{Search,Favorites,Messaging,Visits}`.

### 2.1 `GET /api/v1/rooms/search` (público)

- Parámetros: `universityId` (requerido, 24-hex), `minPrice`/`maxPrice` (500..50000),
  `roomType`, `bathroom`, `buildingType`, `allowedTenants`, `services` (csv),
  `maxDistanceKm` (default 10, 0.5..30), `sort` (default `distance`; `distance`,`price`,`newest`),
  `page` (default 1), `pageSize` (default 20, 1..50).
- **Códigos reales (fuente de verdad, difieren del enunciado):** `roomType` =
  `single|double|shared`; `buildingType` = `rooms_building|house_rooms|full_apartment`;
  `bathroom` = `private|shared`; `allowedTenants` = `women|men|any`;
  `sort` = `distance|price|newest`. El enunciado intercambió roomType/buildingType;
  se sigue el Backend y se documenta la divergencia.
- `RoomSearchItem`: `id`, `buildingId`, `title`, `price`, `currency`, `roomType?`,
  `bathroom?`, `buildingType`, `allowedTenants`, `services`, `distanceKm`,
  `walkingMinutes`, `approxLocation{lat,lng}`, `thumbnailUrl?`, `demo`.
- `RoomSearchResponse`: `{items, page, pageSize, totalItems, totalPages}`.
- Texto: `rooms/search` NO admite `q`. Existe `GET /api/v1/places/search?q=` (lugares,
  no habitaciones) y `GET /api/v1/universities/search?q=`. La búsqueda textual de
  habitaciones en Mobile será **local sobre resultados cargados**, etiquetada como tal.

### 2.2 `GET /api/v1/rooms/{id}` (auth requerida)

Contrato ya implementado en D18. `RoomDetailResponse` con `photos: List<PhotoResponse>?`,
`building.rules?`, `sizeM2?`, `nearestUniversity?`. Sin dirección exacta.

### 2.3 Favoritos

- `GET /users/me/favorites?page&pageSize` → `FavoriteListResponse{items,page,pageSize,totalItems,totalPages}`;
  item: `roomId`, `buildingId`, `title`, `price`, `currency`, `roomType?`, `bathroom?`,
  `city`, `approxLocation`, `demo`, `favoritedAt`, `propertyId?`.
- `GET /users/me/favorites/ids` → `{roomIds:[...], propertyIds:[...]}` (identidad principal: `roomIds`).
- `PUT|DELETE /users/me/favorites/{roomId}` → 204; 409 `favorites_limit` (200), 404, 401.

### 2.4 Solicitudes

- `POST /rooms/{roomId}/requests`: `{quickQuestions?: List<String>(0..5), message?: String(1..500)}`;
  al menos uno. Códigos: `available_now`, `services_included`, `deposit`,
  `minimum_contract`, `visit_possible`, `pets_allowed`, `roommates`. Respuesta 201
  `RentalRequestResponse` con `conversationId` no-null. Errores: 400, 401, 404, 409
  (`active_request_exists`, `active_requests_limit`, ...), 422 (`own_room`,
  `content_rejected`), 429, 503 `moderation_unavailable`.

### 2.5 Conversaciones (módulo existente, sin rediseño)

`GET /conversations`, `GET /{id}/messages` (ETag+304 requires `after` AND ETag),
`POST /{id}/messages` (`clientMessageId` UUID, 201/200 replay), `POST /{id}/read` (204).
Sin crear conversaciones directamente.

### 2.6 Visitas

- `POST /visits`, header `Idempotency-Key` UUID obligatorio; body XOR `roomId`/`propertyId`;
  `scheduledAt` ISO 8601 con offset, `America/Mexico_City`, 08:00–20:00, pasos de 15 min,
  mínimo 2 h, máximo 60 días, `message` ≤300. 201 creación / 200 replay
  (`Idempotent-Replayed: true`); 409 `idempotency_key_reused`, 422 `too_soon`/`too_far`/
  `not_quarter_hour`/`outside_hours`/`content_rejected`, 503 `moderation_unavailable`.
- `VisitResponse.room` no-null; `property` **nullable**; `address`/`location` solo si confirmada.

## 3. Alcance autorizado

Home, Explore, RoomDetail unificado, Favoritos, Solicitudes, Conversaciones (solo
integración de defectos), Visitas, retirada controlada de legacy, tests, SDD/evidencia,
commit/push y un único PR. Sin merge, sin Backend/infra/GenAI/ONNX, sin Fase C, sin
androidTest global, sin costos.

## 4. Criterios de aceptación

1. Home y Explore consumen `rooms/search` con la universidad guardada/seleccionada;
   estados Loading/Content/Empty/Error/Refreshing/Pagination; sin duplicados ni
   respuestas obsoletas; paginación real; tarjetas con `roomId` real.
2. Home/Explore/SmartMatch/Favoritos convergen en `RoomDetailRoute(roomId)` →
   `GET /rooms/{roomId}`; guest sin petición protegida, con destino pendiente.
3. Detalle muestra solo campos reales (fotos, precio, servicios, reglas, etc.) sin
   inventar arrendador ni dirección; acciones reales conectadas.
4. Favoritos GET/PUT/DELETE reales con `roomIds` como identidad; estado compartido;
   rollback optimista; guest → login.
5. Solicitudes con `roomId`, validación contractual, `conversationId` → conversación real.
6. Visitas con `roomId`, `Idempotency-Key` UUID, validación horaria, `property` nullable.
7. Legacy: `LEGACY_CALLERS_BEFORE/AFTER/RETAINED_REASON` documentados; se conserva
   compatibilidad donde aún existen propertyId (rentas, visitas históricas, DTOs).
8. Gates: compile, tests (baseline 547 + nuevos), lint, assemble, diff-check.

## 5. Seguridad / COST-001

Sin secretos nuevos, sin permisos nuevos, sin servicios externos nuevos (mismo backend
propio, $0). Sin logging de tokens/PII/cuerpos sensibles. Se preservan TokenRefresher
single-flight, logout, eliminación de cuenta, D16/D18/D19.1. Moderación 422/503 se
propaga sin interpretaciones falsas; revisión integral queda para Fase C.

## 6. Riesgos

- Divergencia de códigos `roomType`/`buildingType` respecto al enunciado → se mitiga
  auditando el Backend (hecho).
- Falta de cuenta de prueba autorizada → runtime autenticado queda PENDING con mocks.
- Fotos con URLs protegidas → se usa el `imageLoader` compartido con Bearer.
