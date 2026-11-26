# PLAN D18 — Room Detail Mobile

| Fase | Objetivo | Verificación |
|---|---|---|
| A | Auditoría Backend y spec | Contrato confirmado en spec.md |
| B | Data layer: RoomApi, DTOs, repository, use case | Tests DTO + repository |
| C | Domain: modelos mínimos con roomId | Tests de mapping |
| D | UI: ruta, screen, viewmodel, estados | Tests ViewModel + render |
| E | Navegación Smart Match con roomId | Test contractual click |
| F | Gates, PR y auditoría gaps | CI verde |

## A — Auditoría
- A1 `origin/main` Backend: `SearchEndpoint`, `SearchContracts`, `SearchService`, `MediaContracts`. ✅
- A2 Documentar `/rooms/search` para futura spec de Explore. ✅

## B — Data
- B1 `RoomApi` con `@GET("api/v1/rooms/{id}")` (Bearer por interceptor existente).
- B2 DTOs `@Serializable` espejo exacto: `RoomDetailResponseDto`, `ApproxLocationDto`, `SearchUniversityDto`, `SearchBuildingDto`, `RoomPhotoDto`.
- B3 `RoomRepository` con `Result<RoomDetailResponseDto>` y mapeo de errores existente (`DataError`/`DomainError`, 400/401/404/429/503).

## C — Domain
- C1 `RoomDetail`, `RoomBuilding`, `RoomUniversity`, `RoomPhoto`, `ApproxLocation`, con `roomId: String` como identidad.
- C2 `GetRoomDetailUseCase` → `Result<RoomDetail>`.

## D — UI
- D1 `RoomDetailRoute(id)` (typed route, validación de id no vacío).
- D2 `RoomDetailViewModel` con `StateFlow<RoomDetailUiState>` y eventos Load/Retry/Back.
- D3 `RoomDetailScreen` con `LoadingView`, `Content`, `NotFound`, `RequiresAuth`, `ErrorStateView`; Material 3 y componentes TheHouse existentes. Sin datos inventados.

## E — Smart Match
- E1 `SmartMatchResult` expone `roomId` real (viene de `room.id` del DTO AUTH / `roomId` del DTO GUEST).
- E2 `onPropertyClick` → `onRoomClick(roomId)` → `RoomDetailRoute(roomId)`.
- E3 Test: click usa roomId, NO PropertyDetail, NO `/properties/{roomId}`.

## F — Cierre
- F1 Gates: clean compileDebugKotlin, testDebugUnitTest, lintDebug, assembleDebug, git diff --check.
- F2 PR `feature/room-detail-mobile`, CI verde, sin merge.
- F3 Auditoría solo lectura: rooms/search, push devices, deletion, legal.

## Trazabilidad
| Req | Fase | Test |
|---|---|---|
| 1 GET room detail 1 vez | B/E | RoomDetailRepositoryTest |
| 2 200 → Content | C/D | RoomDetailViewModelTest |
| 3 404 → NotFound | B/D | RoomDetailViewModelTest |
| 4 401 → RequiresAuth | B/D | RoomDetailViewModelTest |
| 5 429 tipado | B | RoomDetailRepositoryTest |
| 6 network tipado | B | RoomDetailRepositoryTest |
| 7 click usa roomId | E | SmartMatchRoomNavigationTest |
| 8 NO PropertyDetail | E | SmartMatchRoomNavigationTest |
| 9 NO /properties/{id} | E | SmartMatchRoomNavigationTest |
| 10 back | D/E | RoomDetailRouteTest |
| 11 DTO mapping | B/C | RoomDetailDtoTest |
| 12 screen rendering | D | RoomDetailViewModelTest / preview |
