# D21 — Auditoría y mejora del flujo de publicación del arrendador (2026-11-28)

Autorización humana individual sobre el MISMO PR #37 (rama `feature/platform-security-automation`,
base `d821049`, OPEN/MERGEABLE, CI SUCCESS). NO MERGEAR. Backend auditado en modo **solo lectura**.

## 1. Evidencia productiva nueva

`BUILDING_CREATION_RUNTIME=PASS` — registrado por evidencia humana en emulador conectado a producción:

- `POST /api/v1/landlord/buildings` → **201**.
- RequestId: `248c7019430449468e4570afd09644f6`.
- Edificio creado: **UD3**, BuildingId `6b0a6e113f6d5ad952b41873`.
- `GET` de detalle, rooms, verification, documents y publication → HTTP 200.
- No se repitió la creación ni se modificó UD3 durante esta ejecución.

## 2. Progreso de publicación — fuente real

Backend: `TheHouse.Application/UseCases/BuildingService.cs:253-272` (`StepsAsync`).

| id (literal Backend) | Regla real | Etiqueta Mobile |
|---|---|---|
| `building_data` | `Name.Length > 0 && Address.Street.Length > 0` | Datos del inmueble |
| `location` | `NearestUniversityId.Length > 0` | Ubicación / universidad |
| `services` | `Services.Count > 0` | Servicios |
| `room_ready` | existe cuarto con `Status == Ready` | Habitaciones |
| `building_photo` | existe foto a nivel edificio (si `PublicationRequiresFiles`, default `true`) | Fotografía del edificio |
| `documents` | existen ambos documentos (`ine` + `property_proof`) | Documentación de verificación |

`canPublish = building.Status == Draft && steps.All(done)` (`BuildingService.cs:271`).
Respuesta: `PublicationResponse(bool canPublish, steps[{id,done}])`
(`TheHouse.Application/Contracts/Landlord/LandlordContracts.cs:197-199`).
Mobile consume exclusivamente este endpoint (`GET /api/v1/landlord/buildings/{id}/publication`);
**no inventa requisitos ni porcentajes**. El progreso es `completados / total` derivado de esa lista.
`POST /publish` → 202 → `in_review`; 409 `publication_incomplete` cuando faltan pasos.
No existe aprobación administrativa en el namespace arrendador: no se simula.

## 3. Coordenadas y distancia

Rangos contractuales (`BuildingService.cs:327-338`, confirmados en `CoordinateParser.kt`):
latitud **14..33**, longitud **-118.5..-86**, `double.IsFinite` obligatorio, límites inclusivos.
Mobile ya normaliza coma decimal, signos Unicode (U+2212/U+2013), espacios y caracteres invisibles;
se preserva sin ampliar los límites. El formulario explica los rangos en los campos.

Distancia UTTT **1957.9 km**: Backend la calcula con Haversine
(`ListingRules.cs:89-104`, radio 6371.0088 km, `Math.Round(...,1)`) al crear/PATCH
usando la universidad seleccionada; Mobile **no calcula distancias** y sólo muestra el valor
devuelto (`distanceToUniversityKm`). Causa verificable: corresponde a las coordenadas enviadas
y a la universidad `nearestUniversityId` elegida, no a un cálculo Mobile. Se agrega advertencia
cuando la distancia es excesiva para alojamiento estudiantil; **sin bloqueos geográficos nuevos**.
`GET /universities/search` y GPS se preservan.

## 4. Documentos — HTTP 413 (contrato real)

Backend: `TheHouse/Http/MediaEndpoint.cs`, `TheHouse.Application/UseCases/MediaServices.cs`.
- Tamaño máximo por archivo: **10 MB = 10 485 760 bytes** (`MediaServices.cs:24`).
- Overhead multipart: **16 KB = 16 384 bytes** (`MediaEndpoint.cs:33-34`); límite efectivo de request
  **10 502 144 bytes** (`MediaEndpoint.cs:205-226`). Exceso → **413**.
- Campo multipart: **`file`**, exactamente un archivo, `form.Files.Count == 1` (`MediaEndpoint.cs:228-232`).
- Detección por **magic bytes**, no por extensión ni Content-Type (`MediaServices.cs:36-41`):
  - `%PDF-` → `application/pdf`
  - `FF D8 FF` → `image/jpeg`
  - `89 50 4E 47 0D 0A 1A 0A` → `image/png`
  - WebP **NO** válido para documentos → **415** `unsupported_media`.
- Respuesta de éxito: `DocumentResponse { id, kind, contentType, bytes, uploadedAt }` (200).
- Errores: 413 `file too large`, 415 `unsupported_media`, 400 `kind debe ser ine|property_proof`,
  409 `building_in_review`/`storage_limit`.
- Infraestructura: Kestrel `MaxRequestBodySize` global 64 KB (`FoundationConfiguration.cs:48-55`,
  `appsettings.json` 65536) con middleware global `RequestBodyLimitMiddleware`; la ruta de subida
  eleva su propio límite a 10 MB + 16 KB. No hay configuración IIS en el repositorio
  (despliegue Linux/Kestrel). Los requestIds reportados confirman 413 productivo.

**Root cause probable en Mobile**: `prepareDocument()` validaba el MIME declarado por
`ContentResolver.getType` y el tamaño, pero **no verificaba la firma de bytes**; un archivo
renombrado o con MIME falso pasaba la validación local y era rechazado por el Backend.
Además el mensaje único no distinguía 413/415. Se implementa validación de magic bytes
antes del envío y mensajes específicos. Compresión local de fotos: se conserva el flujo
existente (sin conversión automática de PDF, sin exponer contenido de identificación).

## 5. Estadísticas de cuartos — 400

Contrato real `LandlordStatsEndpoint.cs:49-61`, `LandlordStatsServices.cs:61-78,202`:
- Parámetros **opcionales**: `from`, `to`, `buildingId`, `page`, `pageSize`.
- Duplicados → 400. Formato exacto `yyyy-MM-dd` invariante.
- `to` no puede ser futuro **respecto a la fecha de Ciudad de México** (`LandlordStatsRules.Today`).
- Rango máximo 90 días; `from` dentro de los últimos 365 días.
- `page >= 1`, `1 <= pageSize <= 50` (`PropertyQueryValidation.cs:8-29`).
- `buildingId` opcional pero si presente debe ser ObjectId de 24 hex y pertenecer al arrendador.

Mobile envía `from = LocalDate.now().minusDays(30)`, `to = LocalDate.now()` usando la zona
**del dispositivo** (`LandlordDashboardViewModel.kt:103-108`). Si el dispositivo está en una zona
donde ya es "mañana" en CDMX, `to` es futuro para el Backend → **400**. No se muestra
estadística ficticia: el dashboard usa `getOrDefault(emptyList())` ante fallo. Corrección:
calcular la fecha con la zona de CDMX (`America/Mexico_City`) y acotar el rango a 30 días;
se añaden pruebas MockWebServer de ruta, query parameters y respuestas.

## 6. Privacidad

- Los documentos se copian a `cacheDir/prepared_docs` (ubicación privada de la app).
- No se registra contenido de identificación oficial; los logs DEBUG existentes sólo registran
  códigos/estados sin PII.
- No se usan INE reales en pruebas: los fixtures son sintéticos (bytes PDF/JPEG/PNG generados en memoria).
- No se modifica Backend, infraestructura, GenAI, ML/ONNX ni servicios externos; COST-001 = $0.
