# NV-17 — SMART MATCH / ML / AI

| Campo | Valor |
|---|---|
| Spec ID | NV-17 |
| STATUS | PLANNED |
| AUTHORIZATION | DO NOT EXECUTE |
| Dependencias | NV-09, NV-27, NV-15 (privacidad), NV-23 (analytics) |

## Problema
El usuario necesita recomendaciones de propiedades ajustadas a sus preferencias sin entregar datos innecesarios ni pagar por IA.

## Alcance
- Reglas deterministicas de matching (preferencias declaradas: universidad, precio, distancia) como primera etapa.
- Evaluacion de ML/IA solo si se demuestra mejora y COST-001 $0; sin modelo sin evaluacion.

## No-alcance
- IA generativa, embeddings o servicios ML de pago sin COST-001 aprobado.
- Recomendaciones basadas en datos no consentidos.

## Requisitos
- NV17-REQ-001: Preferencias minimas y consentidas; sin perfil implicito sin consentimiento (NV-15).
- NV17-REQ-002: Ranking explainable y testeable; resultados deterministicos ante mismos inputs.
- NV17-REQ-003: Endpoint de recomendaciones solo si NV-27 lo confirma; si no, calculo local con datos ya disponibles.

## Criterios de aceptacion
- Con mismas preferencias, el ranking es reproducible en tests.
- Sin consentimiento, no se recogen datos de comportamiento para el match.

## Riesgos
- Costo/latencia de ML en dispositivo, sesgos del ranking, privacidad de datos de comportamiento.
- Dependencia de servicios externos no evaluados.

## Impacto seguridad
- Datos de preferencias tratados como PII; sin envio a terceros sin consentimiento y contrato.

## COST-001 ($0)
- Free tier: reglas locales (costo $0) como baseline. Limites: N/A local. Tarjeta: no. Riesgo cobro: servicios ML/IA de pago. Mitigacion: prohibido cualquier servicio con costo o tarjeta; si una IA exige pago, BLOCKED. Alternativa: matching por reglas locales, permanente si no hay IA $0 demostrada.

## Relacion con otras specs
- NV-09 (favoritos como senal), NV-15 (consentimiento), NV-23 (medicion sin PII), NV-27 (contrato).

## Evidencia esperada
- Dataset de prueba, ranking reproducible y evaluacion de costo. Hoy: sin evidencia.
