# D21 — Cierre técnico sin dispositivo Android (2026-11-27)

Rama `feature/platform-security-automation`, PR #37 (OPEN / MERGEABLE, sin merge).
Base visual y funcional congelada: `3bbd1dbe223ee2da7e11e05767dc842a7094552d`.
Autorización: instrucción humana explícita + adenda de geolocalización dentro del mismo PR.

## Contratos auditados (solo lectura en Backend)

- Fotos: `PUT /api/v1/landlord/buildings/{id}/photo` (200), `POST /api/v1/landlord/rooms/{id}/photos` (201), `DELETE /api/v1/landlord/rooms/{id}/photos/{photoId}` (204), `DELETE .../buildings/{id}/photo` (204). Multipart field `file`; máx 5 MiB; JPEG/PNG/WebP por firma de bytes; máx 4 fotos/cuarto (409 `photos_limit`); moderación 422 `content_rejected` (field+categories) y 503 `moderation_unavailable` (Retry-After 30); dueño 404 para ajeno. Evidencia: `MediaEndpoint.cs`, `MediaServices.cs`, `MediaContracts.cs`, `MediaHttpTests.cs`.
- Universidades cercanas: `GET /api/v1/universities/search?lat=&lng=` público, hasta 20, `{items:[{id,code,name,city,distanceKm}]}`, orden Backend por distancia; 400/429/503. Evidencia: `UniversitySearchEndpoint.cs`, `UniversitySearchService.cs`, `ProfileContracts.cs`.
- Edificio: `lat ∈ [14,33]`, `lng ∈ [-118.5,-86]`, finitos; JSON `lat`/`lng` numéricos; campos desconocidos → 400. Evidencia: `BuildingService.cs:327-337`, `FoundationHttpServices.cs`.

## Cambios de código

| Ámbito | Archivos | Resultado |
|---|---|---|
| Parser de coordenadas | `domain/location/CoordinateParser.kt` | Normaliza coma decimal, U+2212, U+2013 inicial, espacios/invisibles; rechaza ambiguos, NaN/Infinity y rangos fuera de México; entrada parcial sin error prematuro |
| Formulario Nuevo edificio | `BuildingFormViewModel.kt`, `BuildingFormScreen.kt` | Errores tras blur/envío; botón «Usar mi ubicación actual» SOLO en sección Ubicación; GPS puntual con `LocationManager`; debounce/cancelación; preselección de universidad cercana sin sobrescribir selección manual |
| Búsqueda cercana | `CatalogApis.kt`, `CatalogDtos.kt`, `CatalogRepositories.kt`, `CatalogRepositoriesImpl.kt`, `CatalogUseCases.kt` | Declaración Retrofit abstracta real; DTO espejo; sin Haversine local ni distancias inventadas |
| Permisos | `AndroidDeviceLocationProvider.kt`, `NotificationPermission.kt`, `NotificationPermissionStore.kt`, `DataStoreNotificationPermissionStore.kt`, `AndroidManifest.xml` | Ubicación contextual (sin background); POST_NOTIFICATIONS con decisión persistida y una sola solicitud contextual |
| Fotos | `LandlordApi.kt`, `LandlordDtos.kt`, `LandlordRepositoryImpl.kt`, `LandlordError.kt`, `LandlordErrorMessages.kt` | Respuestas de foto tipadas como `PhotoResponseDto` (antes BuildingDto/RoomDto); 422/503 mapeados a códigos de moderación con mensajes en español |
| Cobertura | `app/build.gradle.kts` | JaCoCo integrado con tarea `jacocoDebugTestReport` (sin exclusiones artificiales) |

La UI de gestión de fotos ya existía en `RoomFormScreen` (galería, límite 4, progress, error y borrado) y NO se duplicó ni rediseñó. `BuildingDetailScreen`, Home, Explore, Smart Match, Favoritos, Conversaciones, Perfil, Preferencias, Header, Drawer, tema y galerías públicas quedaron intactos.

## Gates reales

| Comando | Resultado |
|---|---|
| `.\gradlew.bat clean compileDebugKotlin` | BUILD SUCCESSFUL |
| `.\gradlew.bat testDebugUnitTest` | BUILD SUCCESSFUL — **702 tests, 0 failures, 0 errors** (baseline 669 + 33 nuevos) |
| `.\gradlew.bat lintDebug` | BUILD SUCCESSFUL |
| `.\gradlew.bat assembleDebug` | BUILD SUCCESSFUL |
| `.\gradlew.bat compileDebugAndroidTestKotlin` | BUILD SUCCESSFUL |
| `.\gradlew.bat assembleDebugAndroidTest` | BUILD SUCCESSFUL |
| `git diff --check` | exit 0 |
| `.\gradlew.bat jacocoDebugTestReport` | BUILD SUCCESSFUL |

Cobertura real (JaCoCo, `jacocoDebugTestReport.xml`): **líneas 7262/21902 = 33.16 %**, **ramas 2810/8923 = 31.49 %**. Áreas nuevas con cobertura dirigida: `domain/location` 43/45 líneas, `data/catalog/repository` 103/147, `data/landlord/repository` 224/280. Principales áreas sin cobertura: composables y navegación (requieren dispositivo/emulador), capas legacy no modificadas y flujos autenticados sin cuenta desechable. No se contabilizan pruebas no ejecutadas ni se infla cobertura con exclusiones.

## Estado de evidencias humanas aportadas

- `ACCOUNT_DELETION_RUNTIME=PASS`: GET 200, POST 403 rechazado y luego 204, requestId `9d1dc271075f46e4b7539a196b619ae3`, login 401 posterior; funcionalidad en Privacidad (no Mi perfil).
- GenAI: GET explicaciones 200 en 8237 ms (requestId `9a170e8db113424692b5b64342e4dcb4`) y 200 en 2220 ms tras calentamiento (requestId `c45db7b43beb401ab5ab707f95dacf93`). Mobile muestra las explicaciones. `generatedBy=genai` queda **pendiente de confirmación Backend**: no se certifica generación real solo por HTTP 200.
- FCM registro 201 y desvinculación 204 previamente validados en dispositivo; no se repitieron mutaciones.

## Riesgos y pendientes

- Sin dispositivo Android: `connectedDebugAndroidTest`, GPS físico y renderizado de la nueva sección quedan `PENDING`. No se simula evidencia física.
- Push real: `EXTERNAL_BLOCKER` (secreto de servidor fuera de alcance).
- OWASP: 2 WARN documentados (CODE-1, CODE-3), sin convertir a NOT_APPLICABLE.
- Scripts PowerShell: 6 auditados, sin credenciales, sin cuentas reales, sin borrado de datos, con evidencia y exit codes 0/1. Procedimiento final disponible en `scripts/mobile-validation/`.

**NO MERGEAR PR #37. NO DECLARAR RUNTIME-CERTIFIED.**
