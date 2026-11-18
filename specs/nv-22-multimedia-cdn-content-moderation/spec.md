# NV-22 — MULTIMEDIA / CDN / CONTENT MODERATION

| Campo | Valor |
|---|---|
| Spec ID | NV-22 |
| STATUS | PARTIAL/INTEGRATED (multimedia real de landlord integrada con contrato desplegado; moderación automática no planificada) |
| AUTHORIZATION | AUTORIZADO por instrucción humana explícita 2026-11-17 (sin Backend, sin endpoints inventados, sin merge) |
| Dependencias | NV-01 (Coil), NV-21 (permisos), NV-14/NV-15/NV-16 (no regresión), NV-27 (contrato backend) |

## Problema
La selección/captura/subida futura de multimedia y el contenido generado por usuarios requieren mínimo privilegio, validación local, protección de privacidad y estados honestos frente a contratos de moderación no disponibles.

## Estado real (actualizado)
- **MULTIMEDIA REAL = autorizado e integrado** (contrato Backend desplegado `ca-thehouse-u2...`): foto de edificio, fotos de cuarto, documentos INE/comprobante de propiedad.
- **Moderación automática = NOT PLANNED/BLOCKED** (`AUTOMATIC-MODERATION-NOT-PLANNED`). **Revisión humana Backend = existente**. No se muestra "contenido aprobado por IA".
- Selección con Photo Picker/Activity Result (`OpenDocument` para documentos) sin permisos amplios de almacenamiento. Cámara no declarada/no solicitada (sin flujo real de captura).
- URLs/media se usan exactamente como las entrega Backend (`url`, `thumbUrl`), incluidas rutas privadas/públicas según estado. Coach/Coil reutilizado.

## Alcance ejecutado (Mobile)
- **Edificio:** `PUT/DELETE /api/v1/landlord/buildings/{id}/photo` (multipart `file`, JPEG/PNG/WebP, <=5MB), visualización con `photo.url/thumbUrl` tal como llegan.
- **Cuartos:** `POST /api/v1/landlord/rooms/{id}/photos`, `DELETE /api/v1/landlord/rooms/{id}/photos/{photoId}` (máx 4, orden backend antigua→reciente, primera foto principal, fallback a foto de edificio cuando aplique en backend). UI de gestión en edición de cuarto.
- **Documentos:** `PUT /ine`, `PUT /property_proof` (PDF/JPEG/PNG, <=10MB), `GET /documents` solo metadatos. Sin descarga/exposición de contenido INE/property proof.
- **Publicación:** pasos reales incluyen `building_data`, `location`, `services`, `room_ready`, `building_photo`, `documents` (ine+property proof). Manejo de `409 publication_incomplete`, `409 building_in_review` y estados reales.
- **Errores mapeados:** `409 photos_limit/storage_limit/building_in_review/publication_incomplete/building_state`, `413`, `415 unsupported_media`, `400 image_too_large` con mensajes UX seguros.
- **Catálogo/visualización:** mapeo aditivo `Building.photo`, `Room.photos`, `Property.thumbnailUrl`, `Property.photos`; PropertyCard/Detail y landlord usan `url/thumbUrl` sin construir URLs.
- **Validación local:** MIME/tamaño/archivo vacío antes de upload (backend autoridad final). Imágenes saneadas best-effort (orientación/recompresión, sin EXIF GPS/metadata innecesaria) en caché; documentos copiados temporalmente para multipart sin exponer contenido.
- **Privacidad:** sin log de URI completa/rutas privadas/contenido; sin OCR; sin persistencia innecesaria de blobs.
- **Moderación:** `AUTOMATIC-MODERATION-NOT-PLANNED`. Validación técnica backend + revisión humana edificio + reportes/admin existentes. Sin simulación.

## No-alcance
- Integrar/cambiar Backend, inventar endpoints/moderación, subir contenido real sin contrato aprobado.
- CDN comercial o moderación IA de pago (COST-001).
- Agregar soporte de video sin requisito contractual real.

## Requisitos
- NV22-REQ-001 (pipeline imágenes/cache): **PARTIAL** (Coil existente; sin flujos/listas de upload nuevas en este alcance).
- NV22-REQ-002 (validación local + moderación real antes de publicar): **PARTIAL** local PASS; moderación remota **BLOCKED** sin contrato.
- NV22-REQ-003 (CDN $0 / no nuevo): **PASS** (sin CDN nuevo ni servicios pagos).

## Criterios de aceptación
- Sin permisos/storage innecesarios; Photo Picker preferido. **PASS (preparado/auditado)**.
- URI/archivo inválido o inválido por MIME/tamaño/vacío manejado sin crash. **PASS (validadores/tests)**.
- No se simula upload/moderación/seguridad de contenido. **PASS**.
- Integración de upload real: **BLOCKED** (sin contrato ejecutable).

## Riesgos
- Metadatos EXIF: eliminación best-effort por recompresión puede no cubrir todos los contenedores/casos; no se afirma borrado total.
- Sin flujo UI de cámara/upload actual, validación runtime E2E limitada a utilidades y gates automatizados.

## COST-001 ($0)
- Solo APIs/framework Android y librerías ya presentes. Sin servicios externos.

## Moderación (contrato)
- **Estado:** `MULTIMEDIA-MODERATION-BACKEND-BLOCKED`
- **Evidencia de ausencia:** sin endpoint/contrato ejecutable confirmado en Mobile; NV-13 borrador/no ejecutable; D13 automático bloqueado.

## Evidencia
- Ver `evidence.md` y tests/gates ejecutados.
