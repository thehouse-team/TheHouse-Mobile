# NV-08 — PROPERTY DETAIL

| Campo | Valor |
|---|---|
| Spec ID | NV-08 |
| STATUS | PLANNED |
| AUTHORIZATION | DO NOT EXECUTE |
| Dependencias | NV-01, NV-06, NV-07, NV-27, NV-22 (multimedia) |

## Problema
El usuario necesita ver el detalle completo de una propiedad para decidir si contactar, agendar visita o guardar en favoritos.

## Alcance
- Detalle de propiedad con datos que el contrato properties (NV-27) confirme.
- Galeria de imagenes via Coil (NV-22); acciones hacia NV-09/NV-10/NV-11 como navegacion.

## No-alcance
- Implementar favoritos (NV-09), visitas (NV-10) o chat (NV-11) en esta spec.
- Campos no confirmados por el contrato.

## Requisitos
- NV08-REQ-001: Solo campos confirmados por NV-27; campos ausentes se omiten, no se inventan.
- NV08-REQ-002: Imagenes con placeholder/error, cache y contentDescription.
- NV08-REQ-003: Detalle con UDF, reintento y manejo de 404.

## Criterios de aceptacion
- Propiedad inexistente -> error 404 mapeado a UiError, sin crash.
- Acciones primarias visibles y accesibles.

## Riesgos
- Imagenes pesadas sin optimizacion; datos incompletos del backend.
- Acciones que parecen funcionales pero no tienen spec autorizada.

## Impacto seguridad
- URLs de imagenes sin tokens en query; sin datos personales del anunciante mas alla del contrato.

## COST-001 ($0)
- Free tier: imagenes servidas por backend/CDN existente. Limites: ancho de banda del tier vigente. Tarjeta: no nueva. Riesgo cobro: trafico de imagenes. Mitigacion: thumbs + cache Coil (NV-22). Alternativa: placeholders locales.

## Relacion con otras specs
- NV-07 (origen), NV-09/NV-10/NV-11 (acciones), NV-22 (media), NV-27 (contrato).

## Evidencia esperada
- Tests de detalle/404, captura de estados. Hoy: sin evidencia.
