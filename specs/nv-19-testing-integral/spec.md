# NV-19 — TESTING INTEGRAL

| Campo | Valor |
|---|---|
| Spec ID | NV-19 |
| STATUS | PLANNED |
| AUTHORIZATION | DO NOT EXECUTE |
| Dependencias | NV-02 (estrategia), NV-01, NV-04, NV-27 |

## Problema
Las specs funcionales necesitan una piramide de pruebas coherente (unit/integracion/UI/instrumented/security) con quality gates medibles.

## Alcance
- Estrategia de testing integral: unitarias, ViewModel, repository (MockWebServer), Compose UI, instrumented y security tests sobre codigo real.
- Baseline de cobertura medido antes de fijar umbrales (NV-02 §5.4).

## No-alcance
- Crear codigo de produccion artificial para probar (NV-02 §5.1).
- Tests de specs no autorizadas ni endpoints no confirmados.

## Requisitos
- NV19-REQ-001: Cada spec funcional define su matriz requisito->prueba antes de implementarse.
- NV19-REQ-002: Fakes solo para contratos reales; dispatchers controlados; sin red/base productiva.
- NV19-REQ-003: Instrumented/UI solo con sujeto real significativo; si falta dispositivo, DEFERRED POR ENTORNO con evidencia.

## Criterios de aceptacion
- Baseline de cobertura publicado; umbral propuesto sin porcentaje arbitrario previo.
- Suite verde reproducible en local y (futura) CI de NV-18.

## Riesgos
- Tests fragiles/flaky, cobertura inflada con tests sin valor, falsos verdes.
- Espera de dispositivos para instrumented.

## Impacto seguridad
- Security tests (NV-20) integrados como capa, no como parche final.

## COST-001 ($0)
- Free tier: JUnit/Coroutines test/MockWebServer/Compose test (OSS). Limites: tiempo/emuladores locales. Tarjeta: no. Riesgo cobro: ninguno. Mitigacion: sin servicios de testing de pago. Alternativa: N/A.

## Relacion con otras specs
- NV-02 (gobernanza de testing), NV-18 (gates en CI), NV-20 (security tests), NV-25 (UI/a11y tests).

## Evidencia esperada
- Matriz requisito->prueba, baseline de cobertura y resultados reales. Hoy: sin evidencia.
