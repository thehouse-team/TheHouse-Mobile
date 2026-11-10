# NV-07 — EVIDENCIA

## D10 — Busqueda y mapa reales (2026-11-09, branch `feature/real-search-map`)

Base: `4eabb4c` (incluye D3, D4, D5, D6, D7.1, D8.1). Sin merge, sin PR, sin cambios en Backend.

### Contrato verificado (solo lectura)
Fuente: refs remotas de `C:\THEHOUSE\TheHouse-backend` (`origin/main`, `origin/feat/nv-14-search`). URL desplegada probada desde emulador.

- `GET /api/v1/rooms/search` (publico): `universityId` obligatorio (ObjectId), `minPrice`, `maxPrice`, `roomType`, `bathroom`, `buildingType`, `allowedTenants`, `services` CSV, `maxDistanceKm`, `sort=distance|price|newest`, `page`, `pageSize<=50`. No existe `q` de texto para cuartos.
- `GET /api/v1/rooms/map` (publico): mismos filtros + `bbox=minLng,minLat,maxLng,maxLat`; maximo 0.3° por lado y 300 marcadores.
- `GET /api/v1/places/search` (publico): `q` 2–60, `universityId`; insensible a acentos; maximo 10.
- `GET /api/v1/rooms/{id}` (Bearer): incluye edificio, universidad cercana, mobiliario, tamano, disponibilidad y contrato minimo. Nunca direccion exacta, dueno, telefono ni coordenadas reales.

### Implementacion
- `domain/model/Search.kt`, `domain/usecase/SearchUseCases.kt`: modelos del contrato y validacion previa (ObjectId, precios 500–50,000, distancia 0.5–30, catalogos, pagina/pageSize, bbox <= 0.3°).
- `data/remote/api/SearchApi.kt`, `data/remote/dto/search/SearchDtos.kt`, `data/search/mapper/SearchMappers.kt`: solo campos y parametros confirmados.
- `PropertyRepository` extendido (sin segundo Retrofit): `searchRooms`, `getRoomMapMarkers`, `searchPlaces`, `getRoomById`, con mapeo seguro de 400/401/404/429/5xx, Network, Timeout y rethrow de cancelacion.
- `ExploreViewModel` + `ExploreScreen`: universidad obligatoria, filtros reales, sort, paginacion, lista/mapa, autocompletado de colonias con debounce, viewport con debounce, estados Loading/Empty/Content/Error, preview de marcador y guard Guest.
- `RoomDetailViewModel` + `RoomDetailScreen`: detalle real `/rooms/{id}`, 404, 401 → Login, reintento. Sin datos inventados.
- Mapa de referencia propio (Compose Canvas) con marcadores reales, pan/zoom y bbox; sin Google Maps/osmdroid, sin tiles, sin API key, sin permiso de ubicacion. `M-MOB-017`/COST-001 del SDK siguen pendientes.

### Tests
- Nuevos: `SearchRepositoryIntegrationTest` (contrato/filtros/bbox/400/401/429/503/Network/Timeout/cancelacion), `SearchUseCasesTest` (validacion), `ExploreViewModelTest` (busqueda, filtros, paginacion, empty, error, invalid filter, mapa, viewport, colonias, guest/auth, timeout) y `RoomDetailViewModelTest`.
- Regresion D3–D8 incluida en la suite.
- Resultado: **140 tests, 0 failures, 0 errors, 27 clases**.

### Validacion ejecutada
Comando: `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug`

- Resultado final: **BUILD SUCCESSFUL**.
- `lintDebug`: 0 errores; 19 warnings no bloqueantes (UnusedResources, versiones, etc.).
- APK: `app/build/outputs/apk/debug/app-debug.apk` (22,267,639 bytes).
- Durante la primera corrida se detectaron 2 errores de sintaxis (`emptyList`) y 1 asercion de test; se corrigieron y la corrida final quedo verde.

### Runtime (emulador `emulator-5554`)
- APK instalado y app abierta sin crash.
- Universidad real `UTTT` (`6aeecc11f3c9f3f76294628f`) seleccionada desde Backend desplegado.
- `/rooms/search`: **5 rentas encontradas**, tarjetas con precio, distancia, tipo, servicios y demo.
- `/rooms/map`: marcadores reales visibles; tap abre preview (`$2200 MXN/mes`, `0.7 km`) con “Ver detalle” y “Cerrar”.
- Guest: “Ver detalle” redirige a Login (sin sesion falsa).
- `/places/search`: “Tula” devuelve “Tula · Ciudad · 4.7 km” y “Atotonilco de Tula · 13.1 km”; al seleccionar, el mapa se recentra con nuevos marcadores.
- Detalle autenticado de cuarto no se ejecuto en runtime por no disponer de credenciales autorizadas; esta cubierto por tests 200/401 y por el guard Guest verificado.

### Riesgos / deuda
- SDK de mapas real y permiso de ubicacion siguen pendientes (`NV-21`/`M-MOB-017`); la visualizacion actual es de referencia y usa coordenadas aproximadas.
- El listado de universidades consume la primera pagina del contrato existente; no se agrego busqueda de universidades porque no esta en D10.
- No se modifico Backend, no se agregaron secretos y no se publico nada.
