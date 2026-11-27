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

## D20 HOTFIX — Catálogo, imágenes, favoritos y Smart Match (2026-11-27)

Autorización humana individual sobre el mismo PR #36 (base `7e6cc27`). Sin merge, sin Backend/GenAI/ML/ONNX/infra, sin Fase C.

### HOME_ROOT_CAUSE / EXPLORE_ROOT_CAUSE (demostrados)
- Inyección de producción correcta: `U2ViewModelFactory` pasa `container.searchRooms`, `container.selectedUniversityRepository`, `container.profileRepository.universityState` y `container.sessionStore.state`.
- `HomeViewModel` solo observaba `profileUniversityState` **dentro de un `if (sessionState.value is Active)` evaluado en `init`**. Como la sesión se restaura de forma asíncrona, el VM se creaba como Anonymous y nunca se suscribía al Perfil; su única universidad era la preferencia local de guest (null en el usuario autenticado). Ninguna llamada a `rooms/search`.
- `ExploreViewModel` no observaba ninguna fuente de universidad: dependía de un parámetro de `MainActivity` leído de la preferencia local. Al ser null, `showRooms=false` y entraba en la rama legacy con `getProperties=null`, cuyo `load()` retorna anticipadamente dejando `properties` en `Loading` eterno («Buscando rentas...»). No se emitía `GET /rooms/search`.

### CATALOG_LOAD_FIX
- Home: observa SIEMPRE `profileUniversityState` (sin condición de sesión inicial), observa `selectedUniversityRepository.selectedUniversityId` como StateFlow y observa `sessionState` (refresh de perfil al pasar a Active). `applyUniversity()` dispara `loadRooms(refresh=true)`; ninguna validación deja Loading: sin universidad escribe `Empty`.
- Explore: mismas tres fuentes + `getUniversities` para el selector; `applyUniversity()` dispara búsqueda real. Sin universidad → `Empty` con selector. Sin branch legacy en producción.
- Ambos: single-flight por generación, `filtersKey` anti-duplicados, reintento explícito (`RefreshRoomsClicked`/`RetryClicked`), paginación con acumulación, logs DEBUG `HomeFlow`/`ExploreFlow`/`RoomSearchFlow` sin PII.

### LOGS DEBUG
`viewmodel_created`, `load_trigger`, `session_state`, `university_available`, `university_source`, `repository_available`, `search_start`, `http_status` (vía `TheHouseHttp` existente), `result_count`, `deserialization_result`, `search_cancelled`, `exception_type`, `state_transition`, `requestId`. `CatalogFlowLogger` usa `runCatching` para no fallar en tests JVM (`android.util.Log` no mocked).

### FILTROS_UI / FILTROS_HTTP
Filtros siempre accesibles (Chip «Filtros» + panel): universidad (catálogo real), precio mín/máx (500..50000), distancia (0.5..30, default 10), tipo de cuarto (`single|double|shared`), tipo de inmueble (`rooms_building|house_rooms|full_apartment`), baño (`private|shared`), convivencia (`women|men|any`), servicios, orden (`distance|price|newest`) y «Limpiar filtros». Códigos confirmados de nuevo contra `SearchContracts.cs`/`ListingRules.cs`. La búsqueda textual está etiquetada «limitada a lo cargado»: `rooms/search` no admite `q`.

### FOTOGRAFÍAS — DIAGNÓSTICO REAL (solo lectura, Backend productivo)
- `GET /api/v1/universities` → 10 universidades; `GET /api/v1/rooms/search?universityId=<id>&pageSize=50` para las 10 → **27 habitaciones, 0 con `thumbnailUrl` no nulo, 27 con `demo=true`**.
- El placeholder azul es correcto: el Backend no publica fotos en los registros disponibles. No se inventaron URLs ni se desactivó TLS; se mantiene el cargador Coil compartido. `IMAGE_EXTERNAL_BLOCKER` = dependencia de datos DEMO sin fotografías (no defecto Mobile).

### SMART_MATCH
- `onFavoriteClick`/`onContactClick` reciben `result.roomId` (antes `result.property.id`).
- `SmartMatchViewModel` inyecta `FavoriteStateStore` + `ToggleFavoriteUseCase`; `SmartMatchResult.isFavorite` se deriva del store compartido. Botones habilitados: «Guardar favorito»/«Quitar favorito» y «Contactar» → `CreateRequestRoute(roomId)` real. Sin tocar modelo, score, breakdown, indicador, autoridad Backend ni fallback D16.

### FAVORITOS DEDUPLICACIÓN
- `AppContainer.syncFavorites()` usa `Job` single-flight con propietario `userId`; `LaunchedEffect(isAuthenticated)` y recomposiciones no generan dos `GET /favorites/ids`.
- `FavoritesViewModel.load()` usa `Job` single-flight con propietario; `init` + `LaunchedEffect` no duplican `GET /favorites`; se eliminó el reconcile extra que hacía un segundo `GET /ids`.

### GATES
`clean compileDebugKotlin`, `testDebugUnitTest` **567 tests / 0 failures / 0 errors** (baseline 562 + 5 regresiones D20 hotfix), `lintDebug` 0 errores SARIF, `assembleDebug`, `git diff --check` exit 0.

### TEST DE REGRESIÓN P0
`D20HotfixCatalogTest.P0 home dispara search cuando perfil emite universidad con sesion restaurada despues`: crea el VM con sesión Anonymous, emite la universidad del Perfil después de la restauración y exige `calls > 0` con `RoomSearchState.Content`. Antes del fix: `calls == 0` (falla). Después: pasa.

## D20 HOTFIX CRÍTICO — Retrofit, StateFlow y regresiones runtime (2026-11-27)

Autorización humana individual sobre el mismo PR #36 (base `c6d4db0`). Sin merge, sin Backend/GenAI/ML/ONNX/infra, sin Fase C.

### SMART_MATCH_CRASH_ROOT_CAUSE / UNINITIALIZED_STATEFLOW
`SmartMatchViewModel` incorporó en el hotfix anterior un bloque `init { favoriteStateStore.roomIds.collect { _uiState.value = ... } }` **antes** de la declaración `private val _uiState = MutableStateFlow(...)`. Con `Main.immediate`/Unconfined durante la construcción, el collector emite de inmediato y `_uiState` aún es null → NPE en `SmartMatchViewModel.kt:63`. Confirmado por código y reproducido por `SmartMatchImmediateEmissionTest` (guest y Active, store ya emitido, UnconfinedTestDispatcher).

### SMART_MATCH_CRASH_FIX
Todas las propiedades de estado (`_uiState`, `uiState`, `_profileMissing`, `profileMissing`) se declaran antes del bloque `init`; el collector de favoritos se movió después de ellas. Sin lateinit, sin delays, sin null-checks para ocultar fallos, sin eliminar favoritos. Revisión dirigida: `HomeViewModel`, `ExploreViewModel`, `FavoritesViewModel` tenían sus StateFlow declarados antes del `init` (verificado por número de línea); sin el mismo patrón.

### CATALOG_ROOT_CAUSE / ROOM_API_PRODUCTION_IMPLEMENTATION
`RoomApi.searchRooms` tenía un **cuerpo Kotlin por defecto** (`= RoomSearchResponseDto()`) añadido para que los fakes de tests compilaran. Retrofit trata los métodos default de Kotlin como default methods de plataforma y **no ejecuta la petición HTTP**: devuelve la respuesta vacía del `DefaultImpls` en 1–10 ms. Coincide exactamente con la evidencia (`deserialization_result success=true`, `result_count count=0`, sin `GET /rooms/search`). Corrección: declaración abstracta real (sin cuerpo) + `SearchStubRoomApi` SOLO en `src/test` para los fakes de detalle; la prueba de transporte usa el proxy Retrofit real.

### RETROFIT_REAL_REQUEST_TEST
`RoomSearchHttpIntegrationTest` (9 pruebas): `RoomApi` de producción + `JsonProvider` + `RoomRepositoryImpl` + MockWebServer. Verifica `RecordedRequest`: `GET`, `/api/v1/rooms/search`, `universityId=6aeecc11f3c9f3f762946293`, `page=1`, `pageSize=20`, `maxDistanceKm=10.0`, `sort=distance`, filtros opcionales (`minPrice`, `maxPrice`, `roomType`, `bathroom`, `buildingType`, `allowedTenants`, `services`) y **ausencia de `q`**. Cubre 200 con 1 habitación (roomId conservado), 200 vacío → Empty, 400 → ValidationError, 503 → ServiceUnavailable, fallo de red, JSON inválido y cancelación. Además Home y Explore con el repositorio real sobre MockWebServer alcanzan `Content` y verifican la ruta HTTP.

### LOGGING VERAZ
`RoomSearchRepositoryImpl` registra `network_request(source=network)`, `deserialization_result(success=true)` solo tras deserializar una respuesta real, `error_type` en excepciones y `state_transition/result_count` en los VMs. Se eliminó el `deserialization_result success=true` de los ViewModels, que afirmaba deserialización sin petición. No se registra HTTP 200 inventado ni se transforma fallo en Empty.

### BACKEND_TEPEJI_COUNT (solo lectura, productivo)
`GET /universities` → UAEH-ESTE «Universidad Autónoma del Estado de Hidalgo, Escuela Superior de Tepeji del Río» = `6aeecc11f3c9f3f762946290`. Con los mismos parámetros efectivos de Mobile (`maxDistanceKm=10`, `sort=distance`, `page=1`, `pageSize=20`): `rooms/search totalItems=4, items=4`; `rooms/count = 4`. Coherentes.

### FILTER_DEDUPLICATION / FILTERS_VISUAL
Explore usa `draftFilters` (edición sin petición) + CTA «Aplicar filtros» y «Limpiar filtros». Los chips rápidos de tipo/orden aplican una sola búsqueda controlada por single-flight. Texto visible: «Encuentra alojamientos cerca de tu universidad» y «Buscar entre los alojamientos mostrados», con aclaración discreta de resultado local. Sin terminología técnica (`rooms/search`) en UI.

### DUPLICATE_HAMBURGER_ORIGIN
Búsqueda dirigida: una sola occurrence de `Icons.Rounded.Menu` en código de la app (`MainActivity`, header global, `testTag=home_menu_button`). No hay segundo hamburger en Compose; el observado en la captura corresponde a capa del dispositivo/accesibilidad. Sin cambios de código.

### GATES
`clean compileDebugKotlin`, `testDebugUnitTest` **578 tests / 0 failures / 0 errors** (baseline 567 + 2 regresiones inmediatas + 9 integración HTTP), `lintDebug` 0 errores SARIF, `assembleDebug`, `git diff --check` exit 0.

### RUNTIME
Sin `adb` disponible en la sesión: no se instaló APK ni se capturaron pantallas. Queda pendiente la validación humana en dispositivo: apertura guest de Smart Match sin FATAL, y selección de universidad en guest comprobando `source=network`, `result_count count=N` y tarjetas.

## D20 CIERRE FINAL — Filtros y agendamiento de visitas (2026-11-27)

Autorización humana individual sobre el mismo PR #36 (verificado OPEN / MERGEABLE antes de implementar; base `e72ba9c0b99fc887865b9b1b919b56ed1c736c54`). Sin merge, sin Backend/GenAI/ML/ONNX/infra, sin Fase C, sin PR nuevo, sin auditorías maestras repetidas.

### EVIDENCIA RUNTIME HUMANA REGISTRADA (14/17 aprobadas)
Funcionan: inicio y Explorar con habitaciones reales; scroll del catálogo; apertura de RoomDetail desde Inicio y Explore; Smart Match sin crash con diseño/recomendaciones/favoritos y persistencia tras reinicio; formulario Contactar con preguntas y mensajes; creación real de solicitudes; Mis solicitudes; acceso a conversaciones. HTTP observado por el usuario: `rooms/search` 200 count=4, `smart-match/recommendations` 200 count=11, `rooms/{id}` 200, `PUT favorites/{roomId}` 204, `POST rooms/{roomId}/requests` 201, `GET users/me/requests` 200, `GET|POST conversations/{id}/messages` 200/201, `POST conversations/{id}/read` 204. Fallan: introducir precios, aplicar/limpiar filtros, seleccionar fecha/hora de visita y confirmar visita. Sin `POST /visits` en los logs compartidos; NO se asumió rechazo Backend sin evidencia.

### FILTER_PRICE_ROOT_CAUSE (demostrado por código)
`ExploreScreen.FilterBar` convertía cada pulsación con `value.toIntOrNull()` y exigía `500..50000` en la misma pulsación: los valores parciales («5», «50», «500» → este último sí válido pero los dos primeros no) se convertían en `null`, por lo que el campo volvía a quedar vacío y el usuario no podía escribir. `filters.minPrice` era `Int?`, así que no podía representar texto parcial. Además no había `KeyboardOptions(keyboardType = Number)`. El panel inline era un `Column` sin `modifier` ni scroll: «Aplicar/Limpiar filtros» quedaban por encima del `LazyColumn(weight(1f))` y podían salir del viewport; el teclado los ocultaba.

### FILTER_PRICE_FIX / FILTER_PANEL_DESIGN / FILTER_SCROLL
- `FilterDraftUi` guarda `minPriceRaw`, `maxPriceRaw` y `distanceRaw` como **String**: campo vacío, dígitos parciales y borrado funcionan; `KeyboardType.Number`/`Decimal`; sin conversión durante la edición y sin consultas (`DraftFilterChanged` no llama a `loadRooms`).
- `FilterDraftParser` (Domain, puro) convierte y valida **una sola vez**, al aplicar; rechaza valores fuera de 500..50000 y 0.5..30, y la inversión min>max, sin reemplazar entradas parciales por cero.
- Panel rediseñado como `ModalBottomSheet` de Material 3, altura acotada al 92 %, contenido con `verticalScroll` y acciones fijas abajo con `navigationBarsPadding` (sin duplicar insets). Incluye universidad, tipo de cuarto, tipo de inmueble, baño, convivencia, servicios multi-selección, precios, distancia y orden, con etiquetas españolas y códigos reales del Backend. Se mantiene el azul TheHouse vía `colorScheme`.

### FILTER_APPLY_HTTP / FILTER_CLEAR_HTTP / FILTER_DUPLICATION
- `ApplyFilters`: valida → si hay error no consulta y lo muestra; si es válido cierra el panel, copia el borrador a `filters`, reinicia paginación con `refresh=true` y emite **una sola** consulta real. La generación anti-stale y `filtersKey` se conservan. Estados Loading/Content/Empty/Error intactos.
- `ClearFilters`: restaura defaults preservando la universidad válida, sincroniza el borrador y evita la petición duplicada si no había criterios opcionales que limpiar.
- Los chips rápidos ahora se basan en los filtros **aplicados** (`filters`), no en un borrador sucio. Cerrar sin aplicar (incluido `onDismissRequest`) no modifica la búsqueda activa. Perfil/Preferencias no se tocan.

### VISIT_CONFIRM_ROOT_CAUSE (demostrado por código)
`ScheduleVisitDestination` hacía `runCatching { LocalDate.parse(text) }.getOrNull()?.let { ... }`: cualquier texto inválido se descartaba en silencio y el ViewModel nunca recibía fecha/hora. `submit()` hacía `val date = state.date ?: return` y `val time = state.time ?: return`: sin mensaje de error, sin petición. Por eso no aparecía `POST /visits`. El botón Confirmar además estaba dentro del `verticalScroll`.

### VISIT_DATE_PICKER / VISIT_TIME_PICKER / VISIT_TIMEZONE / VISIT_DATE_VALIDATION / VISIT_TIME_VALIDATION
- `DatePickerDialog` de Material 3 con `SelectableDates` que deshabilita días pasados y fuera de 60 días; fecha mostrada en español («30 de noviembre de 2026») calculada dinámicamente. `selectedDateMillis` se convierte con `Instant.ofEpochMilli(...).atOffset(ZoneOffset.UTC).toLocalDate()` (patrón confirmado por compose-ui-agent): evita el desplazamiento al día anterior al aplicar America/Mexico_City.
- Cuadrícula de horarios permitidos en pasos de 15 min, 08:00–20:00 inclusivos, etiquetada «Horarios permitidos para solicitar una visita» (no disponibilidad confirmada; el Backend mantiene autoridad con 409 `schedule_overlap`). Se excluyen horarios que incumplen la anticipación de 2 h el día actual y los que exceden 60 días en el último día. Cambiar la fecha revalida y limpia una hora incompatible con mensaje visible. Zona mostrada como «Horario de Ciudad de México».
- `VisitScheduleOptions` (Domain, puro) concentra todas estas reglas y es verificable sin Compose.

### VISIT_POST_HTTP_TEST / VISIT_IDEMPOTENCY / VISIT_ERROR_HANDLING
- `VisitPostHttpIntegrationTest` usa `VisitsApi` de producción + Retrofit real + MockWebServer y verifica `RecordedRequest`: `POST /api/v1/visits`, header `Idempotency-Key`, cuerpo con `roomId` real, **sin `propertyId`** (gracias a `@EncodeDefault(Mode.NEVER)` en el DTO), `scheduledAt` con offset `-06:00` y `message`. Cubre 201, 200 replay con `Idempotent-Replayed` y 422 `too_soon`.
- `scheduledAt = LocalDate + LocalTime + ZoneId.of("America/Mexico_City")`; nunca la zona del dispositivo. Idempotency-Key UUID estable por intento lógico (mismo payload/reintento) y nueva cuando cambian los datos; se limpia tras éxito. Doble envío bloqueado por `status == SUBMITTING` y por el botón `loading`.
- Errores 400/401/409/422/429/503 se mapean a `VisitErrorCode`/`DomainError` y se muestran con texto concreto; ya no hay mensajes genéricos que oculten la causa. Instrumentación DEBUG segura `VisitFlow` con sink no-op en JVM (patrón D19.1): `submit_blocked`, `submit_start`, `submit_success`, `submit_failure` sin tokens/PII.

### GATES FINALES
`clean compileDebugKotlin` OK; `testDebugUnitTest` **602 tests / 0 failures / 0 errors** (baseline 578 + 24 nuevos); `lintDebug` 0 errores; `assembleDebug` OK; `git diff --check` exit 0. Sin dependencias nuevas (Material3 DatePicker/ModalBottomSheet ya incluidos en el BOM aprobado). Sin cambios en Backend, GenAI, ML/ONNX, infra, MainActivity, tema, D18/D19.1, favoritos, solicitudes, conversaciones ni TokenRefresher.

### RUNTIME PENDIENTE
No hay `adb` en la sesión: no se instaló APK ni se enviaron visitas reales. Instrucciones para el usuario: (1) instalar `app/build/outputs/apk/debug/app-debug.apk` con `adb install -r`; (2) en guest con universidad seleccionada abrir Explorar → Filtros, escribir «3500» y «5000» en precio, desplazar hasta «Ordenar por», pulsar Aplicar y verificar una sola petición `GET /api/v1/rooms/search` con `minPrice=3500&maxPrice=5000`; probar Limpiar; (3) con sesión autorizada abrir RoomDetail → Agendar visita, elegir fecha con el calendario, horario de 15 min, mensaje opcional y Confirmar, verificando `POST /api/v1/visits` con `Idempotency-Key` y `roomId`. NO se envían visitas reales automáticamente sin autorización específica.

### REMOTE_CI
PR #36 OPEN / MERGEABLE (mergeStateStatus `BLOCKED` = política de protección, sin conflictos; NO se mergeó). Commit final `66d6772`. El primer run remoto falló SOLO en `gitleaks` por un literal de UUID de prueba con alta entropía (`generic-api-key`, falso positivo en `VisitPostHttpIntegrationTest`); se corrigió derivando el UUID con `UUID.nameUUIDFromBytes("visit-post-http-fixture")` sin tocar `.gitleaks.toml` ni umbrales. Run final `45992634848`: `Android quality gates` SUCCESS — unit tests, Android Lint, assemble debug, gitleaks, trivy y artefactos; solo avisos de deprecación de Node.js 20/setup-java v4 del workflow (fuera de alcance D20). Commits: `06dfea9` (cierre funcional) + `66d6772` (fix CI). Working tree limpio.
