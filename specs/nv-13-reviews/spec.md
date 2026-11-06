# NV-13 — REVIEWS

| Campo | Valor |
|---|---|
| Spec ID | NV-13 |
| STATUS | PLANNED |
| AUTHORIZATION | DO NOT EXECUTE |
| Dependencias | NV-01, NV-12, NV-22 (moderacion), NV-27 |

## Problema
El usuario necesita calificar propiedades/anfitriones y leer opiniones de otros usuarios para decidir con confianza.

## Alcance
- Listado de reviews y envio de review (calificacion + texto) con moderacion previa (NV-22).
- Elegibilidad (solo tras cierre/visita) solo si NV-27 la confirma.

## No-alcance
- Reviews sin moderacion, ratings agregados no definidos por el contrato.
- Endpoints de reviews no confirmados: sin contrato, BLOCKED.

## Requisitos
- NV13-REQ-001: Solo endpoints/campos confirmados por NV-27.
- NV13-REQ-002: Validacion de calificacion/texto en Domain; anti-spam basico (un voto por usuario/propiedad si el contrato lo define).
- NV13-REQ-003: Contenido pendiente de moderacion no se publica como definitivo salvo regla confirmada.

## Criterios de aceptacion
- Review enviada queda en estado visible/pendiente segun el contrato; duplicada se maneja con 409.
- Texto con contenido prohibido se bloquea con mensaje accesible.

## Riesgos
- Abuso/falsas reviews sin elegibilidad ni moderacion.
- PII en textos libres.

## Impacto seguridad
- Textos libres tratados como no confiables (sin HTML/renderizado inseguro); sin PII en logs.

## COST-001 ($0)
- Free tier: backend existente + moderacion manual o regla $0 (NV-22). Limites: cuota de reviews. Tarjeta: no nueva. Riesgo cobro: moderacion comercial. Mitigacion: moderacion manual/reglas locales. Alternativa: reviews solo visibles tras moderacion manual.

## Relacion con otras specs
- NV-12 (elegibilidad), NV-22 (moderacion), NV-25 (accesibilidad de rating), NV-27 (contrato).

## Evidencia esperada
- Tests de validacion/duplicados y flujo de moderacion. Hoy: sin evidencia.
