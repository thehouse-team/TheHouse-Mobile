# D21 — Auditoría contractual dirigida del flujo integral del arrendador

| Campo | Valor |
|---|---|
| Fecha | 2026-11-27 |
| Rama | `feature/platform-security-automation` |
| PR | #37 (OPEN / MERGEABLE, verificado) |
| Base HEAD | `6ab949e75018aa269b426ce9dc119e126af71e5c` |
| Backend | `C:\THEHOUSE\TheHouse-backend` — SOLO LECTURA |
| Autorización | Instrucción humana individual (adenda D21), sin merge |

## 1. Matriz de endpoints auditados (arrendador + comunicación)

Contrato leído del código real del Backend (no de OpenAPI supuesta). Fuentes:
`TheHouse.Http/LandlordEndpoint.cs`, `TheHouse.Http/MessagingEndpoint.cs`,
`TheHouse.Http/MediaEndpoint.cs`, `TheHouse.Application/UseCases/MessagingServices.cs`,
`TheHouse.Application/Contracts/Messaging/MessagingContracts.cs`,
`TheHouse.Domain/Entities/Listings.cs`, `TheHouse.Application/UseCases/LandlordService.cs`,
`TheHouse.Http/AdminEndpoint.cs`, `TheHouse.Domain/Entities/Notification.cs`.

| Método | Ruta | Rol | Payload | Respuesta | HTTP | Consumidor Mobile | Estado | Pruebas Backend | Defectos |
|---|---|---|---|---|---|---|---|---|---|
| POST | `/api/v1/landlord/profile` | Autenticado sin perfil | `fullName, phone, rentalMode, rfc?, acceptedTermsVersion` | `LandlordProfileResponse` | 201 / 409 `landlord_exists` | `LandlordProfileViewModel` → `ActivateLandlordUseCase` | Implementado | `LandlordService` (terms `2026-11`) | Ninguno; doble-submit guardado en VM |
| GET | `/api/v1/landlord/profile` | Arrendador | — | perfil | 200 / 404 | `LandlordDashboardViewModel` | Implementado | — | Dashboard no se recarga al volver de activación |
| GET | `/api/v1/landlord/buildings` | Arrendador | `page,pageSize` | `items,totalItems,summary` | 200 | `LandlordDashboardViewModel` | Implementado | — | Sin invalidación tras crear/editar edificio |
| POST | `/api/v1/landlord/buildings` | Arrendador | `name,type,address,location,universityId,allowedTenants,services,rules` | `BuildingDto` | 201 | `BuildingFormViewModel` | Implementado | `LandlordHttpTests` | Sin refresco inmediato del listado al volver |
| GET | `/api/v1/landlord/buildings/{id}` | Propietario | — | `BuildingDto` | 200 / 403 / 404 | `BuildingDetailViewModel` | Implementado | — | — |
| PATCH | `/api/v1/landlord/buildings/{id}` | Propietario | campos + `version` | `BuildingDto` | 200 / 409 `version_conflict` | `BuildingFormViewModel` | Implementado | — | — |
| DELETE | `/api/v1/landlord/buildings/{id}?version=` | Propietario | `version` | — | 204 / 409 | `BuildingDetailViewModel` | Implementado | — | — |
| PUT / DELETE | `/api/v1/landlord/buildings/{id}/photo` | Propietario | multipart `file` ≤5 MiB | `PhotoResponse` | 200 / 422 / 503 | `BuildingDetailViewModel` | Implementado | `ContentModerationHttpTests` | — |
| POST / GET / DELETE | `/api/v1/landlord/buildings/{id}/documents` | Propietario | multipart | `BuildingDocuments` | 200 / 422 / 503 | `BuildingDetailViewModel` | Implementado | — | — |
| GET | `/api/v1/landlord/buildings/{id}/verification` | Propietario | — | `VerificationInfo` | 200 | `BuildingDetailViewModel` | Implementado | — | Solo lectura; sin acciones staff |
| POST | `/api/v1/landlord/buildings/{id}/publish` | Propietario | — | `PublicationStatus` | 202 → `in_review` | `BuildingDetailViewModel` | Implementado | — | — |
| GET | `/api/v1/landlord/buildings/{id}/publication` | Propietario | — | `PublicationStatus` | 200 | `BuildingDetailViewModel` | Implementado | — | — |
| GET | `/api/v1/landlord/rooms?buildingId=` | Propietario | `buildingId,page,pageSize` | `RoomsListResponse` | 200 | `BuildingDetailViewModel`, `RoomFormViewModel` | Implementado | — | Edición de cuarto lee solo página 1 |
| POST | `/api/v1/landlord/rooms/generate` | Propietario | `buildingId,roomBase,count,numbering` | `RoomsListResponse` | 201 | `RoomFormViewModel` | Implementado | — | Sin refresco inmediato del edificio |
| PATCH | `/api/v1/landlord/rooms/{id}` | Propietario | campos + `version` | `RoomDto` | 200 / 409 | `RoomFormViewModel` | Implementado | — | `availableFrom` obligatorio en UI aunque contrato es nullable |
| DELETE | `/api/v1/landlord/rooms/{id}?version=` | Propietario | `version` | — | 204 | `RoomFormViewModel` | Implementado | — | — |
| POST / DELETE | `/api/v1/landlord/rooms/{id}/photos` | Propietario | multipart `file` ≤5 MiB, máx 4 | `PhotoResponse` | 201 / 204 / 422 / 503 | `RoomFormViewModel` | Implementado | `ContentModerationHttpTests` | Límite 4 solo en VM |
| POST | `/api/v1/rooms/{roomId}/requests` | Estudiante autenticado | `quickQuestions[≤5],message?` | `RentalRequestResponse` con `conversationId` | **201** / 400 / 404 / 409 / 422 / 503 | `CreateRequestViewModel` | Implementado | `MessagingTests.Create_validates_questions_and_message` | Ver §3 |
| GET | `/api/v1/users/me/requests` | Estudiante | `status,page,pageSize` | lista | 200 | `RequestsViewModel` | Implementado | — | — |
| GET | `/api/v1/landlord/requests` | Arrendador | `status,roomId,page,pageSize` | lista con `student{name,university}` | 200 / 403 | `LandlordRequestsViewModel` | Implementado | — | Sin refresco tras aceptar/rechazar (sí `loadQuiet`) |
| POST | `/api/v1/landlord/requests/{id}/accept` | Arrendador | — | solicitud | 200 / 409 | `LandlordRequestDetailViewModel` | Implementado | — | — |
| POST | `/api/v1/landlord/requests/{id}/reject` | Arrendador | `reason,note?` | solicitud | 200 / 409 | `LandlordRequestDetailViewModel` | Implementado | — | — |
| GET | `/api/v1/conversations` | Autenticado | — | lista + `unreadCount` | 200 | `ConversationsViewModel` | Implementado | — | Sin ETag en este endpoint (correcto) |
| GET | `/api/v1/conversations/{id}/messages` | Participante | `after,limit` + `If-None-Match` | `MessagesResponse` / 304 | 200 / 304 | `ConversationViewModel` | Implementado | `MessagingHttpTests` | — |
| POST | `/api/v1/conversations/{id}/messages` | Participante | `text,clientMessageId` (UUID D) | `MessageResponse` | 201 / 200 replay | `ConversationViewModel` | Implementado | `MessagingSecurityTests` | — |
| POST | `/api/v1/conversations/{id}/read` | Participante | `lastMessageId` | — | 204 | `ConversationViewModel` | Implementado | — | — |
| POST | `/api/v1/visits` | Estudiante | `roomId,scheduledAt,notes?,Idempotency-Key` | visita | 201 / 422 | `ScheduleVisitViewModel` | Implementado | `VisitsHttpTests` | — |
| GET/POST | `/api/v1/landlord/visits`, `/visits/{id}/confirm|cancel` | Arrendador/Estudiante | — | visita | 200 / 409 | `VisitsRepositoryImpl` | Implementado | — | Sin acción `reject` (Backend no la expone) |
| GET/POST | `/api/v1/landlord/agreements`, `/agreements/{id}/...` | Arrendador/Estudiante | — | convenio | 200 / 409 | `AgreementsRepositoryImpl` | Implementado | — | — |
| GET | `/api/v1/users/me/rental` | Estudiante | — | arrendamiento activo | 200 | `MyRentalViewModel` | Implementado | — | — |

## 2. Estados de dominio reales (Backend)

- Edificio: `draft → in_review → verified | rejected`; `suspended` solo por staff (`AdminEndpoint`).
- Cuarto: `ready → available → rented | paused`; aprobación staff lo lleva a `available`.
- Solicitud: `pending → accepted | rejected | cancelled`.
- Visita (v1.3): `requested → confirmed | cancelled`. **No existe `rejected`**.
- Convenio: `proposed → accepted → active → finished | cancelled`.
- **No se exponen acciones de staff** al arrendador: `POST /admin/buildings/{id}/approve|request-changes|reject|suspend|unsuspend` queda fuera del alcance Mobile.

Verificados en `MessagingRules.QuickQuestions` (7 códigos), `MaximumQuickQuestions=5`,
`MaximumRequestMessage=500`, `RentalRequestResponse.ConversationId` (no nullable),
`CreateRentalRequest` con `[JsonUnmappedMemberHandling(Disallow)]`.

## 3. Brechas funcionales encontradas ANTES de esta adenda

| ID | Brecha | Evidencia | Clasificación |
|---|---|---|---|
| G1 | `CreateRequestEffect.OpenConversation` es no-op; `RequestDetailEffect.OpenConversation` es no-op | `CreateRequestScreen.kt:69`, `RequestDetailScreen.kt:53` | Defecto funcional |
| G2 | Preguntas rápidas se visualizan como códigos crudos (`available_now`) en el detalle | `RequestDetailScreen.kt:163` | Defecto de visualización |
| G3 | Errores de moderación `content_rejected` / `moderation_unavailable` no mapeados → mensaje genérico | `RequestError.kt:18-27`, `UserEndpointSupport.cs:107-119` | Defecto de manejo de errores |
| G4 | No hay catálogo compartido etiqueta↔código; duplicado en `CreateRequestScreen` | `CreateRequestScreen.kt:42-50` | Deuda / riesgo de divergencia |
| G5 | Dashboard no se recarga al volver de activación ni de crear/editar edificio (viewModel por `remember`, estado en `init`) | `LandlordDashboardViewModel.kt:48-50`, `MainActivity` wiring | Defecto de actualización |
| G6 | `BuildingDetailViewModel` no recarga rooms/publicación tras crear/editar un cuarto en otra pantalla | `BuildingDetailViewModel.kt:128-153` | Defecto de actualización |
| G7 | `availableFrom` se exige en UI aunque `RoomBaseDto.availableFrom` es nullable | `RoomFormViewModel.kt:61-67` | Defecto de validación |
| G8 | Retrofit interfaces con bodies Kotlin default (`NotificationsApi`, `VisitsApi`, `AgreementsApi`) — patrón peligroso ya corregido en `RoomApi` | `NotificationsApi.kt:36`, `VisitsApi.kt:38`, `AgreementsApi.kt:51` | Riesgo (no confirmado como fallo) |

## 4. Bloqueos externos

- **NINGUNO para el alcance de esta adenda**: todos los cambios se realizan con contratos existentes del Backend.
- Se documenta, sin bloquear: no existe endpoint de aprobación admin en namespace arrendador (correcto por diseño); no existe acción `reject` de visita en v1.3; ETag solo en mensajes, no en listado de conversaciones.

## 5. Congelación visual

No se modifica Home, Explore, Smart Match, Favoritos, RoomDetail, Perfil, Preferencias,
Header, Drawer ni tema. Solo cambios funcionales localizados en:
`presentation/requests/*`, `presentation/landlord/*`, `domain/error/RequestError.kt`,
`data/remote/dto/requests/*`, `MainActivity.kt` (solo cableado de navegación de conversación),
más pruebas. Se reutilizan colores/tipografías/componentes existentes.
