# NV-22 — MULTIMEDIA / CDN / CONTENT MODERATION

| Campo | Valor |
|---|---|
| Spec ID | NV-22 |
| STATUS | PARTIAL (hardening local móvil ejecutado; upload/moderación remota/backend y flujos de captura no existentes) |
| AUTHORIZATION | AUTORIZADO por instrucción humana explícita 2026-11-17 (sin Backend, sin endpoints inventados, sin merge) |
| Dependencias | NV-01 (Coil), NV-21 (permisos), NV-14/NV-15/NV-16 (no regresión), NV-27 (contrato backend) |

## Problema
La selección/captura/subida futura de multimedia y el contenido generado por usuarios requieren mínimo privilegio, validación local, protección de privacidad y estados honestos frente a contratos de moderación no disponibles.

## Estado real auditado
- No existen flujos actuales de cámara, galería, Photo Picker ni upload multipart en la app.
- No existe contrato backend ejecutable de moderación automática previa al upload (`/moderate`, `/scan-image`, `/content-check` o equivalentes: **no inventar**). NV-13 media/docs es borrador/no ejecutable; D13 moderación automática bloqueada.
- Coil boundary presente (NV-01); sin pipeline de thumbs/upload de negocio implementado.

## Alcance ejecutado (Mobile)
- **Selección multimedia (preparación):** preferencia por Android Photo Picker / Activity Result APIs (sin `READ_EXTERNAL_STORAGE`/`READ_MEDIA_*` innecesarios). Helpers/contratos reutilizables sin forzar flujos inexistentes.
- **Cámara:** sin permiso/solicitud declarada porque no hay función real; política preparada para solicitud contextual solo al usar captura futura.
- **Ubicación:** no requerida; sin permisos de ubicación.
- **Validación local antes de upload futuro:** MIME permitido (imágenes), tamaño máximo razonable, archivo vacío/corrupto/inaccesible, selección cancelada, URI inválida/no disponible; validación defensiva sin confiar solo en extensión.
- **Procesamiento seguro (cuando aplique):** lectura por `ContentResolver`, compresión razonable preparada en background, sin bloquear UI thread, sin recomprimir repetidamente por diseño de API.
- **Privacidad/metadatos:** saneamiento best-effort de imágenes (redecodificación con orientación + recompresión) para no propagar EXIF GPS/metadatos innecesarios en artefactos procesados; sin registrar URI completa sensible, paths privados, binarios ni PII.
- **Moderación:** `MULTIMEDIA-MODERATION-BACKEND-BLOCKED`. Arquitectura/estados preparados (`seleccionando`, `procesando`, `listo`, `error validación`, `moderación pendiente`, `moderación rechazada`, `upload pendiente`) sin afirmar "contenido seguro/moderado" ni simular upload/moderación.
- **Texto:** validadores locales básicos reutilizables (vacío/longitud/whitespace/control chars) sin filtro IA falso.
- **Seguridad input:** tratamiento de URI/archivos como no confiables, uso de `ContentResolver`, sin `file://` inseguro ni path traversal.

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
