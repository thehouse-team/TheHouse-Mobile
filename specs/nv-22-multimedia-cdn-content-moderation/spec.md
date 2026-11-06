# NV-22 — MULTIMEDIA / CDN / CONTENT MODERATION

| Campo | Valor |
|---|---|
| Spec ID | NV-22 |
| STATUS | PLANNED |
| AUTHORIZATION | DO NOT EXECUTE |
| Dependencias | NV-01 (Coil), NV-08, NV-11, NV-13, NV-27 |

## Problema
Las fotos de propiedades y el contenido de usuarios (chat/reviews) necesitan carga eficiente y moderacion a costo $0.

## Alcance
- Pipeline de imagenes: thumbs, cache Coil, placeholders/errores y URLs firmadas solo si el contrato las define.
- Moderacion de contenido: reglas + revision manual; reporte/bloqueo.

## No-alcance
- CDN comercial o moderacion con IA de pago sin COST-001.
- Subida de imagenes sin contrato NV-27.

## Requisitos
- NV22-REQ-001: Imagenes con tamaño controlado (thumbs) y cache disk/memoria; sin descargar originales completos en listas.
- NV22-REQ-002: Todo contenido de usuario pasa por moderacion (reglas y/o manual) antes de publicarse como definitivo (NV-11/NV-13).
- NV22-REQ-003: CDN solo si el backend ya lo provee o si una alternativa $0 se demuestra; si no, BLOCKED para CDN nuevo.

## Criterios de aceptacion
- Lista de propiedades carga thumbs sin jank; error de imagen con placeholder.
- Contenido reportado/bloqueado deja de mostrarse segun la regla definida.

## Riesgos
- Costo de ancho de banda; imagenes maliciosas/sobredimensionadas; moderacion insuficiente.
- URLs con tokens filtrados en logs.

## Impacto seguridad
- Validacion de tipo/tamaño en subida (si el contrato la define); URLs sin secretos; contenido no confiable sin renderizado inseguro.

## COST-001 ($0)
- Free tier: Coil local + storage/CDN ya provisto por el backend (tier vigente). Limites: ancho de banda/almacenamiento del plan. Tarjeta: no nueva. Riesgo cobro: CDN/moderacion comercial. Mitigacion: thumbs + cache, moderacion manual/reglas locales, sin servicios de pago. Alternativa: servir imagenes desde el backend existente sin CDN adicional.

## Relacion con otras specs
- NV-08 (galeria), NV-11/NV-13 (contenido), NV-24 (performance), NV-27 (contrato).

## Evidencia esperada
- Metricas de carga de imagenes y flujo de moderacion. Hoy: sin evidencia.
