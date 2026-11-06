# NV-10 — VISITS

| Campo | Valor |
|---|---|
| Spec ID | NV-10 |
| STATUS | PLANNED |
| AUTHORIZATION | DO NOT EXECUTE |
| Dependencias | NV-01, NV-04, NV-08, NV-27 |

## Problema
El usuario necesita agendar y gestionar visitas a propiedades (solicitar, ver estado, cancelar) de forma fiable.

## Alcance
- Flujo de agendamiento de visita y listado de visitas del usuario.
- Estados/conflictos de horario solo si NV-27 los confirma.

## No-alcance
- Calendario del anunciante, pagos, contratos de alquiler (NV-12).
- Endpoints de visitas no confirmados: sin contrato, BLOCKED.

## Requisitos
- NV10-REQ-001: Solo endpoints/campos confirmados por NV-27; sin contrato, BLOCKED total.
- NV10-REQ-002: Validacion de fecha/hora en Domain (mensajes en espanol, como NV-04).
- NV10-REQ-003: Cancelacion idempotente y manejo de 409/conflicto.

## Criterios de aceptacion
- Visita creada aparece en el listado; cancelada deja de estar activa.
- Doble envio no duplica la visita (idempotencia o 409 mapeado).

## Riesgos
- Zonas horarias y solapamientos no definidos por el backend.
- Notificaciones de visita (NV-14) fuera de alcance aqui.

## Impacto seguridad
- Datos de contacto/visita minimizados; sin PII en logs.

## COST-001 ($0)
- Free tier: backend existente. Limites: los del plan vigente. Tarjeta: no nueva. Riesgo cobro: ninguno adicional. Mitigacion: sin servicios de agenda externos. Alternativa: N/A.

## Relacion con otras specs
- NV-08 (origen), NV-12 (cierre), NV-14 (notificaciones), NV-27 (contrato).

## Evidencia esperada
- Tests de validacion y flujo crear/cancelar. Hoy: sin evidencia.
