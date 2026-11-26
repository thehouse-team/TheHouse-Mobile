# EVIDENCIA D18 — Room Detail Mobile

| Campo | Valor |
|---|---|
| Fecha | 2026-11-26 |
| Rama | `feature/room-detail-mobile` desde `origin/main` @ `1ab64eb` (PR #32 ya mergeado) |
| Autorización | Instrucción humana explícita |
| Estado | **D18-ROOM-DETAIL-CODE-READY** · RUNTIME PENDIENTE · PR #33 |

## 1. Gobernanza

```text
SPEC ACTIVA: D18-Room-Detail-Mobile
PLAN ACTIVO: D18 FASE A–F
TASK ACTIVO: D18-T01..T10
ESTADO AUTORIZADO: APROBADO
```

Nota: `origin/main` avanzó a `1ab64eb` = Merge PR #32. El usuario indicó mantener #32 abierto, pero el repo muestra el merge ya realizado. No se revertió nada ni se modificó D16 en esta rama.

## 2. ROOM_DETAIL_BACKEND_CONTRACT (auditoría solo lectura)

Fuente: `TheHouse-backend origin/main` @ `bfa393d`.

| Campo | Valor |
|---|---|
| ROOM_DETAIL_ENDPOINT | `GET /api/v1/rooms/{id}` |
| METHOD | GET |
| AUTH_REQUIRED | Sí — `UserEndpointSupport.RunAsync`; `.AllowAnonymous().RequireTokenNotRevoked()` |
| PATH_PARAM | `id` (ObjectId) |
| SUCCESS_STATUS | 200 |
| SUCCESS_RESPONSE_DTO | `RoomDetailResponse` |
| 404 | Sí — cuarto inexistente/no `Available` o edificio no `Verified` |
| 401 | Sí — token ausente/revocado |
| 403 | No declarado |
| 429 | Sí — `RateLimitKeys.RoomDetailUser` + `Retry-After` |
| 503 | Sí — `DependencyUnavailableException` |
| 400 / 500 | Sí |
| FIELDS | id, buildingId, title, price(Int), currency, roomType?, bathroom?, sizeM2?, furniture[], availableFrom?, minContract?, distanceKm, walkingMinutes, approxLocation, nearestUniversity?, building, thumbnailUrl?, demo, photos? |
| RELATED_PROPERTY_FIELDS | Ninguno: no se reutiliza `PropertyDetailDto` |
| IMAGES | `photos[]` + `thumbnailUrl` (URLs relativas de la API) |
| LANDLORD_DATA | No expuesto en el detalle |
| AVAILABILITY | `availableFrom`, `minContract` |
| PRICE | `price: Int` + `currency` (`"MXN"`) |
| SERVICES | `building.services[]` + `furniture[]` |
| RULES | `building.rules?` |

## 3. Implementación Mobile

| Capa | Archivo |
|---|---|
| API | `data/remote/api/RoomApi.kt` — `@GET("api/v1/rooms/{id}")` |
| DTO | `data/remote/dto/room/RoomDetailDtos.kt` — espejo exacto |
| Repository | `data/room/RoomRepository.kt` — `Result`, errores tipados |
| Domain | `domain/room/RoomDetail.kt`, `domain/usecase/GetRoomDetailUseCase.kt` |
| UI | `presentation/roomdetail/RoomDetailViewModel.kt`, `RoomDetailScreen.kt` |
| Navigation | `presentation/navigation/RoomDetailRoute.kt`, `MainActivity.kt`, `U2ViewModelFactory.kt`, `AppContainer.kt` |
| Smart Match | `SmartMatchResult.roomId`, `SmartMatchScreen.onRoomClick`, click → `RoomDetailRoute` |

Estados: Loading, Content, NotFound, RequiresAuth, Error. Sin fallback local ni datos inventados.

## 4. Tests D18 (12/12)

| # | Test | Resultado |
|---|---|---|
| 1 | roomId válido → GET room detail ×1 | PASS |
| 2 | 200 → Content | PASS |
| 3 | 404 → NotFound | PASS |
| 4 | 401 → RequiresAuth | PASS |
| 5 | 429 → RateLimited tipado | PASS |
| 6 | network → Network tipado | PASS (y 503 → ServiceUnavailable) |
| 7 | Smart Match click usa roomId | PASS |
| 8 | Smart Match click NO usa PropertyDetail | PASS |
| 9 | Smart Match click NO llama `/properties/{roomId}` | PASS |
| 10 | Back / validación de ruta | PASS |
| 11 | DTO mapping | PASS |
| 12 | Screen rendering de campos reales | PASS |

Archivo: `app/src/test/java/com/thehouse/app/presentation/roomdetail/D18RoomDetailTest.kt`.

## 5. Gates

```text
.\gradlew.bat clean compileDebugKotlin   → BUILD SUCCESSFUL
.\gradlew.bat testDebugUnitTest            → BUILD SUCCESSFUL — 494 tests / 0 failures / 0 errors
.\gradlew.bat lintDebug                    → BUILD SUCCESSFUL — 0 errores
.\gradlew.bat assembleDebug                → BUILD SUCCESSFUL
git diff --check                           → exit 0
```

CI remoto: Android quality gates PASS — run `45882455060` (6m25s) y `45882498433` (5m28s), PR #33 MERGEABLE/Open.

## 6. Auditoría gaps de evaluación (FASE 11, solo lectura)

| Ítem | BACKEND_READY | MOBILE_READY | SPEC_REQUIRED | BLOCKER |
|---|---|---|---|---|
| ROOM_SEARCH | Sí — `GET /api/v1/rooms/search`, público, `RoomSearchResponse` | NO — Explore usa `GET /properties`; `SmartMatchRoomSearchDto` ya espejea `RoomSearchItem` | Sí, nueva spec de Explore→rooms/search | Ninguno técnico; requiere autorización |
| PUSH_REGISTRATION | Sí — `PUT /api/v1/users/me/devices/{installationId}`, body `PushDeviceRequest{fcmToken,platform,appVersion,locale}`, auth, 200/201, 409/413/415; `DELETE` mismo path → 204; `GET` lista sin tokens | NO — Mobile tiene FCM sin registrar token | Sí, spec de registro FCM | `FCM-TOKEN-BACKEND-BLOCKED` resuelto por el contrato; falta `google-services.json` real |
| DELETE_ACCOUNT | Sí — `POST /api/v1/users/me/deletion` (password + palabra `ELIMINAR`), 204; `GET` preview 200; `PUT /api/v1/users/me/legal`; `GET /api/v1/legal` público | PARCIAL — UI + purga local, sin endpoint | Sí, spec de integración | Requiere contrato de body exacto y validación en dispositivo |
| LEGAL_NOTICES | Sí — `GET /api/v1/legal` → `LegalResponse{termsVersion,privacyVersion,landlordTermsVersion,termsUrl?,privacyUrl?,accountDeletionUrl?,updatedAt}` | PARCIAL — ruta `privacidad` con `BuildConfig.PRIVACY_POLICY_URL` vacía | Sí | `BLOCKED-PRIVACY-URL`: URL pública real pendiente |

Detalles:
- ROOM_SEARCH_BACKEND_CONTRACT: `GET /api/v1/rooms/search?universityId=&minPrice=&maxPrice=&roomType=&bathroom=&buildingType=&allowedTenants=&services=&maxDistanceKm=&sort=&bbox=&page=&pageSize=` → 200 `RoomSearchResponse(items,page,pageSize,totalItems,totalPages)`, 304, 400/404/429/503/500, público con rate limit por IP.
- EXPLORE_CURRENT_ENDPOINT: `GET /api/v1/properties` (catálogo de propiedades, no cuartos).
- MIGRATION_REQUIRED: Sí, para búsqueda real de cuartos.
- PUSH_ENDPOINT: `/api/v1/users/me/devices/{installationId}`; METHOD: PUT (register) / DELETE (unregister) / GET (list); AUTH: Bearer; TOKEN_FIELD: `fcmToken`; PLATFORM_FIELD: `platform`; DEVICE_ID: `installationId` en path; BODY: `fcmToken,platform,appVersion,locale` (sin campos extra).
- DELETE_ACCOUNT_ENDPOINT: `/api/v1/users/me/deletion`; METHOD: POST (DELETE equivalente no existe); AUTH: Bearer; REAUTH_REQUIRED: password + `ELIMINAR`; STATUS: 204; CASCADE_BEHAVIOR: participantes en `AccountDeletionServices`.
- PRIVACY_URL / TERMS_URL: `null` en configuración.Backend (`LegalOptions` defaults vacíos); PUBLICLY_ACCESSIBLE: endpoint sí, contenido no; MOBILE_ROUTE: `privacidad`.

## 7. Deuda / runtime

- Sin dispositivo/ADB: no se certificó el recorrido visual. `RUNTIME-PENDING`.
- No se agregó ONNX Runtime; `model_v1.json` intacto con "Estimación local".
- No se implementó push/delete/legal ni migración de Explore: solo auditoría.
