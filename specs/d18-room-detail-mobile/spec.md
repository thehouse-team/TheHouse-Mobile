# SPEC D18 — Room Detail Mobile

| Campo | Valor |
|---|---|
| ID | D18-Room-Detail-Mobile |
| Versión | 1.0.0 |
| Fecha | 2026-11-26 |
| Estado | APROBADA (autorización humana explícita) |
| Rama | `feature/room-detail-mobile` desde `origin/main` @ `1ab64eb` |
| Independencia | NO mezclar con D16 (#32). D16 ya mergeado en `main`; D18 es spec nueva. |

## 1. Problema

D16 dejó un único bloqueo: el click de una recomendación Smart Match navega a
`PropertyDetailRoute(result.property.id)` y el ViewModel consulta
`GET /api/v1/properties/{id}`. El Backend confirmó que el `roomId` de Smart
Match identifica un **cuarto real** y su detalle se obtiene con
`GET /api/v1/rooms/{roomId}`. Mobile no tiene `RoomDetailRoute`,
`RoomDetailScreen`, `RoomDetailViewModel` ni `RoomApi.getRoom(id)`.

## 2. Contrato Backend auditado (solo lectura, `origin/main` @ `bfa393d`)

Fuente: `TheHouse/Http/SearchEndpoint.cs`, `TheHouse.Application/Contracts/Search/SearchContracts.cs`, `TheHouse.Application/UseCases/SearchService.cs`, `TheHouse.Application/Contracts/Media/MediaContracts.cs`.

| Campo | Valor confirmado |
|---|---|
| ROOM_DETAIL_ENDPOINT | `GET /api/v1/rooms/{id}` |
| METHOD | GET |
| AUTH_REQUIRED | Sí — `UserEndpointSupport.RunAsync`; `.AllowAnonymous().RequireTokenNotRevoked()` (401 sin token válido) |
| PATH_PARAM | `id` (ObjectId válido; `ModerationRules.ObjectId`) |
| SUCCESS_STATUS | 200 |
| SUCCESS_RESPONSE_DTO | `RoomDetailResponse` |
| 404 | Sí — cuarto inexistente, no `Available`, edificio no `Verified` |
| 401 | Sí — token ausente/revocado |
| 403 | No declarado en este endpoint |
| 429 | Sí — `RateLimitKeys.RoomDetailUser(userId)`, `Retry-After` |
| 503 | Sí — `DependencyUnavailableException` |
| 400 | Sí — id inválido (`ProducesValidationProblem(400)`) |
| 500 | Sí |

### Campos reales de `RoomDetailResponse`

```
id: String
buildingId: String
title: String
price: Int            (MXN)
currency: String
roomType: String?
bathroom: String?
sizeM2: Double?       (decimal? → Double?)
furniture: List<String>
availableFrom: String?   (yyyy-MM-dd)
minContract: String?
distanceKm: Double
walkingMinutes: Int
approxLocation: { lat: Double, lng: Double }   (celda aproximada, NO dirección real)
nearestUniversity: { id, code, name }?
building: { id, name, type, allowedTenants, services: List<String>, rules: String? }
thumbnailUrl: String?
demo: Boolean
photos: List<PhotoResponse>?  { id, url, thumbUrl, width, height, bytes, createdAt }
```

- No hay campo de arrendador (nombre/contacto) en el detalle público.
- `RULES` está en `building.rules` (texto opcional del edificio).
- `PRICE` es `int` + `currency` fijo `"MXN"` en el servicio.
- `AVAILABILITY` = `availableFrom` + `minContract`.
- `SERVICES` = `building.services` (catálogo cerrado del edificio) y `furniture` del cuarto.
- `IMAGES` = `photos` con URLs relativas a la API + `thumbnailUrl`.

### Búsqueda de cuartos (auditoría FASE 11.A)

`GET /api/v1/rooms/search` existe y es público (`SearchEndpoint.SearchAsync`):
query `universityId,minPrice,maxPrice,roomType,bathroom,buildingType,allowedTenants,services,maxDistanceKm,sort,bbox,page,pageSize`; response `RoomSearchResponse(items,page,pageSize,totalItems,totalPages)`; `RoomSearchItem` ya está replicado en Mobile como `SmartMatchRoomSearchDto` (NV-23). Mobile `ExploreScreen` hoy usa `GET /properties`. Migración requiere spec nueva.

## 3. Alcance D18

### Incluido

- `RoomApi.getRoom(id)` (Retrofit, mismo singleton existente).
- DTOs de detalle serializables espejo del contrato, sin campos inventados.
- `RoomRepository` + `GetRoomDetailUseCase` (Domain puro, `Result`).
- Modelos de dominio mínimos (`RoomDetail`, `RoomPhoto`, `RoomBuilding`, `RoomUniversity`, `ApproxLocation`) con `roomId` como identidad.
- `RoomDetailRoute(roomId)`, `RoomDetailScreen`, `RoomDetailViewModel` con UDF y estados Loading/Content/NotFound/RequiresAuth/Error.
- Navegación Smart Match: `result.roomId` → `RoomDetailRoute` → `GET /rooms/{roomId}`. Eliminar uso de `PropertyDetailRoute` y `/properties/{roomId}` desde Smart Match.
- Validación de `roomId` en la ruta, back funcional, guard de auth (el endpoint lo exige).
- Tests 1–12 del pedido, gates y PR separado.

### Excluido

- Cambiar `GET /properties/{id}` para las demás pantallas (Property Detail de Explore sigue válido).
- Migrar Explore a `/rooms/search` (solo auditoría; spec futura).
- Push registration, delete account y legal notices (solo auditoría; specs futuras).
- Modificar D16/#32, Onnx, secretos ni Backend.

## 4. Criterios de aceptación

1. `roomId` válido → `GET /api/v1/rooms/{roomId}` exactamente 1 vez.
2. 200 → `Content` con campos reales del DTO.
3. 404 → `NotFound`.
4. 401 → `RequiresAuth`.
5. 429 → `RateLimited`/error tipado.
6. Network/timeout → error tipado.
7. Click Smart Match usa `roomId`.
8. Click Smart Match NO usa `PropertyDetail`.
9. Click Smart Match NO llama `/properties/{roomId}`.
10. Back funciona; bottom nav no se rompe.
11. DTO mapping sin campos inventados.
12. Screen renderiza solo info disponible.

## 5. Seguridad / COST-001

- `INTERNET` ya declarado y justificado; sin permisos nuevos.
- El token se envía solo por el interceptor existente; nunca se loguea.
- Fotos: URLs relativas de la API; se载荷 con Coil existente, sin llaves nuevas.
- COST-001: $0. Sin servicios externos nuevos; se reutilizan Retrofit, OkHttp, Coil y el Backend contratado.
- OWASP MASVS-NETWORK/AUTH: 401/429/503 tipados, sin fallback local inventado para este detalle.

## 6. Evidencia esperada

`specs/d18-room-detail-mobile/evidence.md` con contrato auditado, comandos, resultados, gates, CI y estado runtime.
