# EVIDENCIA D20 — Rooms, Discovery and Interactions

## Baseline
- `git status --short`: limpio.
- `git branch --show-current`: main → `b4283a620cc04bde10df83d270bca80874f20164`.
- `origin/main`: `b4283a620cc04bde10df83d270bca80874f20164`.
- PR #35: MERGED; `74c329c` integrado.
- Baseline tests: 547 / 0 failures / 0 errors.

## Contratos (Backend, solo lectura — auditados antes de implementar)
Fuentes: `TheHouse/Http/{SearchEndpoint,FavoritesEndpoint,MessagingEndpoint,VisitsEndpoint}.cs`,
DTOs en `TheHouse.Application/Contracts/{Search,Favorites,Messaging,Visits}`.
- `rooms/search`: `universityId` requerido; `roomType`=`single|double|shared`;
  `buildingType`=`rooms_building|house_rooms|full_apartment`; `maxDistanceKm` default 10 (0.5..30);
  `sort`=`distance|price|newest`; `page` 1, `pageSize` 20 (1..50).
- El enunciado de Fase B invertía roomType/buildingType; se sigue el Backend (fuente de verdad).
- `rooms/search` NO admite `q`; `places/search?q=` es de lugares. Búsqueda textual en Explore = LOCAL sobre resultados cargados, etiquetada en UI.
- Favoritos: `GET /favorites` + `/ids` (`roomIds`, `propertyIds`), PUT/DELETE → 204, 409 `favorites_limit`, 200 máx.
- Solicitudes: `POST /rooms/{roomId}/requests`, quick questions 0..5 de 7 códigos, message 1..500, 201 con `conversationId`, 422/503 moderación.
- Visitas: `POST /visits`, `Idempotency-Key` UUID, XOR `roomId`/`propertyId`, `America/Mexico_City`, 08–20 h, 15 min, ≥2 h, ≤60 días, ISO 8601 con offset, 201/200 replay, `property` nullable.

## Gates locales
- `./gradlew.bat clean compileDebugKotlin`: ejecutado (BUILD SUCCESSFUL; sin errores).
- `./gradlew.bat testDebugUnitTest`: **562 tests / 0 failures / 0 errors** (baseline 547 + 15 D20).
- `./gradlew.bat lintDebug`: BUILD SUCCESSFUL; 0 errores SARIF (`level=error`).
- `./gradlew.bat assembleDebug`: BUILD SUCCESSFUL; APK debug generado.
- `git diff --check`: exit 0 (sin trailing whitespace en cambios D20).

## Cambios D20
- F1: `RoomSearchDtos`, `RoomSearch` dominio, `RoomSearchCatalog`, `RoomApi.searchRooms`, `RoomSearchRepositoryImpl`, `SearchRoomsUseCase`, `RoomSearchState`, `RoomCard`; Home/Explore migrados a rooms/search con Loading/Content/Empty/Error/Refreshing/Pagination, single-flight + generación anti-respuestas obsoletas, paginación real.
- F2: RoomDetail unificado (`RoomDetailRoute(roomId)`), fotos reales (thumbnailUrl / photos / placeholder), acciones reales Contactar → `CreateRequestRoute(roomId)` y Agendar → `ScheduleRoomVisitRoute(roomId)`, favorito compartido, guest → login.
- F3: Favoritos reales con `roomIds` identidad, `/ids` reconcile, PUT/DELETE 204, toggle optimista con reversión y dedupe; pantalla con navegación a RoomDetail.
- F4: Solicitudes con roomId real, máximo 5 preguntas, mensaje ≤500, `conversationId` en el modelo (flujo existente sin rediseño).
- F5: Visitas con `roomId` (XOR, sin propertyId), `Idempotency-Key` UUID estable por intento, validador de zona/horas/anticipación ya contractual, `VisitResponse.property` nullable y dirección solo si confirmada (hasAddress).
- F6: Legacy documentado abajo; tests y gates.

## LEGACY_CALLERS_BEFORE
- `HomeViewModel.loadProperties()` → `GetPropertiesUseCase` → `GET /api/v1/properties`.
- `ExploreViewModel.load()` → `GetPropertiesUseCase` → `GET /api/v1/properties`.
- `SmartMatchViewModel.fetchCandidates()` → fallback local `GET /api/v1/properties` (D16, sin cambio).
- `PropertyDetailRoute` en Home/Explore → `GET /api/v1/properties/{id}`.
- Favoritos: DTOs `FavoriteItemDto.propertyId` / `FavoriteIdsResponseDto.propertyIds` como identidad.
- Visitas: `CreateVisitRequestDto.propertyId` obligatorio, `VisitDto.property` no-null.

## LEGACY_CALLERS_AFTER
- Home/Explore: `GET /api/v1/rooms/search` (producción). El branch legacy de `properties` solo se conserva para los tests de catálogo que inyectan `getProperties`.
- Smart Match D18/D19.1: sin cambios (`roomId` real).
- Favoritos: `roomIds` identidad; `propertyId` solo campo nullable de compatibilidad.
- Visitas nuevas: SOLO `roomId` vía `createRoomVisit`; `createVisit(propertyId)` queda para visitas/rentas históricas.
- `PropertyApi`/`PropertyDetailRoute` siguen declarados para rentas y visita histórica; no se eliminan módulos.

## LEGACY_RETAINED_REASON
Compatibilidad necessary con rentas (D13.1), visitas históricas y respuestas del Backend que aún incluyen `propertyId` (favoritos legacy, `VisitResponse.property` nullable). No se eliminan endpoints del Backend ni módulos completos.

## D19_1_REGRESSIONS
- `RoomDetailRoute.PATTERN = cuarto/{roomId}` y ruta concreta `cuarto/<id>` (tests 5 y 15).
- Sin doble navegación; Smart Match sigue usando roomId real.
- Header único, drawer, perfiles/preferencias/logout/eliminación sin cambios.

## ENDPOINTS_CONSUMED
`GET /api/v1/rooms/search`, `GET /api/v1/rooms/{id}`, `GET /users/me/favorites`, `GET /users/me/favorites/ids`, `PUT|DELETE /users/me/favorites/{roomId}`, `POST /rooms/{roomId}/requests`, `POST /visits` (roomId). Sin endpoints nuevos inventados.

## RUNTIME
Pendiente. No se inventan capturas ni respuestas; sin credenciales de prueba autorizadas para solicitudes/visitas reales en producción.
