# NV-12 — RENTAL CLOSING

| Campo | Valor |
|---|---|
| Spec ID | NV-12 |
| STATUS | PLANNED |
| AUTHORIZATION | DO NOT EXECUTE |
| Dependencias | NV-01, NV-10, NV-11, NV-27 |

## Problema
El usuario necesita cerrar el alquiler de una propiedad (pasos finales, documentos y estado del cierre) dentro de la app.

## Alcance
- Flujo de cierre con pasos/estados que NV-27 confirme (solicitud, aprobacion, documentos si el contrato los define).
- Resumen de estado del cierre por propiedad.

## No-alcance
- Pagos en app, firma electronica legal, contratos formales: fuera de alcance salvo contrato backend confirmado y revision legal.
- Endpoints de cierre no confirmados: sin contrato, BLOCKED.

## Requisitos
- NV12-REQ-001: Solo pasos/estados/documentos confirmados por NV-27.
- NV12-REQ-002: Confirmacion explicita antes de cualquier accion irreversible.
- NV12-REQ-003: Documentos (si los hubiere) con permisos y almacenamiento seguro (NV-21/NV-22).

## Criterios de aceptacion
- Cada paso refleja el estado real del backend; accion irreversible exige confirmacion.
- Error de red no deja el cierre en estado ambiguo (reconsulta de estado).

## Riesgos
- Actos juridicos/economicos sin cobertura legal; documentos sensibles.
- Pasos no definidos por el backend.

## Impacto seguridad
- Documentos de identidad/contrato: cifrado, minimizacion, sin backup inseguro, sin logs.

## COST-001 ($0)
- Free tier: backend existente. Limites: los del plan vigente. Tarjeta: no nueva. Riesgo cobro: ninguno adicional. Mitigacion: sin pasarelas de pago ni firma comercial. Alternativa: cierre asistido fuera de la app si no hay contrato.

## Relacion con otras specs
- NV-10 (visita previa), NV-11 (chat), NV-15 (privacidad/documentos), NV-27 (contrato).

## Evidencia esperada
- Maquina de estados del cierre y tests. Hoy: sin evidencia.
