# NV-22 — EVIDENCIA

## Spec
- `NV-22 Multimedia / CDN / Content Moderation`
- Rama: `feature/permissions-multimedia`
- Autorización: humana explícita 2026-11-17.

## Precheck y auditoría (real)
- Sin flujos actuales de cámara, galería, Photo Picker ni upload multipart en `app/src`.
- Sin contrato backend ejecutable de moderación automática previa al upload (`/moderate`, `/scan-image`, `/content-check` no existen/confirmados en Mobile; NV-13 media/docs borrador, D13 moderación automática BLOCKED).
- Búsqueda de contratos/moderación: sin endpoint ejecutable → **`MULTIMEDIA-MODERATION-BACKEND-BLOCKED`**.
- Ubicación del dispositivo no requerida por flujos actuales → sin permisos de ubicación.
- `POST_NOTIFICATIONS` preservado (NV-14), sin regresión.

## Implementación móvil (real)
- `domain/multimedia/MediaValidation.kt`: validación local de imágenes (MIME permitido jpeg/png/webp, tamaño máximo configurable, archivo vacío/desconocido, normalización MIME, sin confiar solo en extensión) y texto (vacío/whitespace, longitud, caracteres de control inválidos) sin filtro IA.
- `domain/multimedia/MultimediaState.kt`: estados honestos `SELECTING/PROCESSING/READY/VALIDATION_ERROR/MODERATION_PENDING/MODERATION_REJECTED/UPLOAD_PENDING` y `ModerationUiState` bloqueado por contrato (`canClaimModerated=false`, `canClaimUploaded=false`, aviso `MULTIMEDIA-MODERATION-BACKEND-BLOCKED`).
- `data/multimedia/MediaInspector.kt`: lectura segura por `ContentResolver` (sin rutas absolutas), consulta de metadata, validación y saneamiento best-effort en IO (`Dispatchers.IO`) con corrección de orientación EXIF y recompresión JPEG a caché de app para no propagar EXIF GPS/metadatos innecesarios; rechaza procesados vacíos/excedidos; sin upload.
- `presentation/multimedia/MultimediaContracts.kt`: helpers Photo Picker (preferido, sin permisos amplios de almacenamiento), mapeo seguro de selección cancelada/seleccionada, y política pura de permiso cámara para uso futuro condicionado a flujo real.

## Tests agregados
- `app/src/test/java/com/thehouse/app/domain/multimedia/MediaValidationTest.kt`
- `app/src/test/java/com/thehouse/app/domain/multimedia/MultimediaStateTest.kt`
- `app/src/test/java/com/thehouse/app/presentation/multimedia/CameraPermissionPolicyTest.kt`
- Cobertura: MIME válido/inválido, tamaño excedido, vacío/desconocido, texto inválido/control, estados/moderación bloqueada sin falsa afirmación, política cámara y selección cancelada.

## Gates ejecutados (reales)
- `.\gradlew.bat testDebugUnitTest` → **BUILD SUCCESSFUL**
- `.\gradlew.bat lintDebug` → **BUILD SUCCESSFUL**
- `.\gradlew.bat assembleDebug` → **BUILD SUCCESSFUL**

## Runtime / ADB
- `adb` no disponible → validación runtime manual no ejecutada/no inventada. No hay flujo UI actual de cámara/Photo Picker/upload que ejercitar en app.

## Límites / bloqueos
- **`AUTOMATIC-MODERATION-NOT-PLANNED`**: no existe moderación automática; no se inventa. Revisión humana Backend existente.
- Video no soportado por contrato en este alcance.
- Saneamiento EXIF/metadatos: best-effort por recompresión; no se afirma eliminación total en todos los contenedores/casos.

---

## Integración multimedia real (2026-11-17, `feature/real-multimedia`)
- **Precheck:** `git status --short` vacío, rama `feature/permissions-multimedia` (commit `f5c2c20`), creada `feature/real-multimedia`. Sin cambios locales ajenos.
- **Contrato integrado (Mobile):**
  - `PUT/DELETE /api/v1/landlord/buildings/{id}/photo` (multipart `file`)
  - `POST /api/v1/landlord/rooms/{id}/photos`, `DELETE /api/v1/landlord/rooms/{id}/photos/{photoId}`
  - `PUT /api/v1/landlord/buildings/{id}/documents/ine`, `PUT .../documents/property_proof`, `GET .../documents`
  - DTOs/dominio aditivos: `Building.photo`, `Room.photos`, `Property.thumbnailUrl`, `Property.photos`, `MediaItem(id,url,thumbUrl,width,height,bytes,createdAt)`, metadatos de documentos.
  - `url`/`thumbUrl` preservados tal como llegan (sin construir URLs). Publicación incluye `building_photo` y `documents`.
- **Errores manejados:** `409 photos_limit/storage_limit/building_in_review/publication_incomplete/building_state`, `413`, `415 unsupported_media`, `400 image_too_large` (mensajes UX seguros, sin raw body).
- **Permisos/selección:** sin permisos nuevos en manifest; Photo Picker para imágenes y `OpenDocument` para documentos; sin CAMERA/ubicación/storage; cancelación/URI inválida/MIME/tamaño manejados localmente.
- **Privacidad:** sin log de URIs/rutas/contenido/documentos personales; documentos no descargados ni expuestos (solo metadatos).
- **Moderación:** `AUTOMATIC-MODERATION-NOT-PLANNED`; revisión humana Backend existente.
- **Gates finales (reales, una vez):**
  - `.\gradlew.bat testDebugUnitTest` → **BUILD SUCCESSFUL**
  - `.\gradlew.bat lintDebug` → **BUILD SUCCESSFUL**
  - `.\gradlew.bat assembleDebug` → **BUILD SUCCESSFUL**
- **Runtime/ADB:** `adb` no disponible → validación en dispositivo/emulador **NO EJECUTADA** (no inventada). No se usaron documentos personales reales.
