# NV-23 — OBSERVABILITY / ANALYTICS

| Campo | Valor |
|---|---|
| Spec ID | NV-23 |
| STATUS | PLANNED |
| AUTHORIZATION | DO NOT EXECUTE |
| Dependencias | NV-15 (privacidad), NV-20, NV-03 (Data Safety) |

## Problema
El equipo necesita saber como se usa la app y donde fallan los flujos, sin espiar al usuario ni pagar por analitica.

## Alcance
- Eventos de analitica anonimizados/agregados y logs de diagnostico sin PII.
- Crash reporting solo si una alternativa $0 se demuestra; si no, logs locales documentados.

## No-alcance
- Analytics con identificadores publicitarios, fingerprinting o datos de terceros sin consentimiento.
- Servicios de observabilidad de pago sin COST-001.

## Requisitos
- NV23-REQ-001: Catalogo de eventos con finalidad, sin PII ni tokens; consentimiento/opt-out segun NV-15.
- NV23-REQ-002: Logs de diagnostico sin tokens, passwords, contenido de chat ni PII (NV-01 SafeHttpLoggingInterceptor como baseline).
- NV23-REQ-003: Crash/errores reportados solo con proveedor $0 demostrado; si no, solo registro local.

## Criterios de aceptacion
- Auditoria de eventos/logs: 0 PII/secretos.
- Data Safety (NV-03) refleja los datos realmente recogidos.

## Riesgos
- Filtracion de PII en eventos; costo de servicios de analitica; sesgo de metricas sin consentimiento.
- Exceso de telemetria que degrada bateria/datos.

## Impacto seguridad
- Minimizacion y agregacion; sin identificadores persistentes salvo los del backend ya autorizados.

## COST-001 ($0)
- Free tier: analitica agregada propia (backend existente) o proveedor con tier gratuito y limites documentados. Limites: eventos/mes y retencion. Tarjeta: documentar si el proveedor la exige (entonces BLOCKED). Riesgo cobro: exceder eventos. Mitigacion: muestreo, sin SDKs de pago, opt-out sin friccion. Alternativa: solo logs locales + metricas del backend.

## Relacion con otras specs
- NV-15 (consentimiento), NV-17 (medicion del match), NV-20 (logs seguros), NV-03 (Data Safety).

## Evidencia esperada
- Catalogo de eventos y auditoria sin PII. Hoy: sin evidencia.
