# NV-07 — SEARCH + MAP

| Campo | Valor |
|---|---|
| Spec ID | NV-07 |
| STATUS | PLANNED |
| AUTHORIZATION | DO NOT EXECUTE |
| Dependencias | NV-01, NV-06, NV-27, NV-21 (permisos de ubicacion) |

## Problema
El usuario necesita encontrar propiedades por criterios y visualizarlas geograficamente cerca de su universidad.

## Alcance
- Busqueda con filtros que el contrato NV-27 confirme (texto, universidad, precio, etc., solo si existen).
- Mapa con marcadores de propiedades si el contrato provee coordenadas.
- Permiso de ubicacion justificado y alternativo sin ubicacion.

## No-alcance
- Detalle completo de propiedad (NV-08), favoritos (NV-09).
- SDK de mapas no evaluado bajo COST-001; no integrar sin spec de costo aprobada.

## Requisitos
- NV07-REQ-001: Solo filtros/coordenadas confirmados por NV-27; si no existen, BLOCKED parcial documentado.
- NV07-REQ-002: Mapa compliant con permisos (NV-21) y sin exponer ubicacion precisa en logs.
- NV07-REQ-003: Busqueda con debounce/cancelacion; estados UDF completos.

## Criterios de aceptacion
- Buscar sin resultados muestra empty state; sin permiso de ubicacion la app sigue usable.
- Filtros no confirmados no se envian al backend.

## Riesgos
- Costo de mapas/tiles fuera de free tier; tiles sin clave segura.
- Precision de ubicacion y privacidad del usuario.

## Impacto seguridad
- Ubicacion es dato sensible: minimizacion, sin logs, sin backup, retencion minima.

## COST-001 ($0)
- Free tier: evaluar SDK de mapas con tier gratuito y limites documentados antes de integrar. Limites: cuotas de tiles/requests. Tarjeta: posible requisito del proveedor (documentar; si lo exige y no hay alternativa $0, BLOCKED). Riesgo cobro: exceder cuota. Mitigacion: cache de mapa, viewport limitado, sin polling. Alternativa: lista sin mapa o mapa estatico de costo $0 si se demuestra.

## Relacion con otras specs
- NV-06 (catalogo), NV-08 (detalle), NV-21 (permisos), NV-24 (performance de mapa), NV-27 (contrato).

## Evidencia esperada
- Matriz filtro->contrato, tests de busqueda, evidencia de permiso denegado. Hoy: sin evidencia.
