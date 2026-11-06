# NV-24 — PERFORMANCE

| Campo | Valor |
|---|---|
| Spec ID | NV-24 |
| STATUS | PLANNED |
| AUTHORIZATION | DO NOT EXECUTE |
| Dependencias | NV-01, NV-06, NV-07, NV-08, NV-19 (medicion) |

## Problema
Listas, busqueda, mapa e imagenes deben ser fluidas en dispositivos de gama baja sin degradar la experiencia.

## Alcance
- Presupuesto de performance: startup, scroll de listas, carga de imagenes, tamanos de APK/AAB.
- Tecnicas: paginacion probada, LazyColumn, thumbs (NV-22), cache HTTP, baseline profiles solo si se justifica.

## No-alcance
- Optimizaciones que rompan UDF/accesibilidad; mediciones sin baseline real.

## Requisitos
- NV24-REQ-001: Baseline medido (startup, jank en listas, peso de imagenes) antes de optimizar.
- NV24-REQ-002: Listas con LazyColumn + keys estables; imagenes con tamano acorde al viewport.
- NV24-REQ-003: Sin regresiones de accesibilidad (NV-25) ni de seguridad (NV-20) por optimizar.

## Criterios de aceptacion
- Mejora medible contra el baseline documentada; sin numeros inventados.
- AAB/APK dentro del presupuesto definido tras medir el baseline.

## Riesgos
- Optimizacion prematura que oculta problemas; mediciones en dispositivos no representativos.
- Cache agresiva que sirve datos obsoletos/sensibles.

## Impacto seguridad
- Cache HTTP respeta Cache-Control/ETag y no-cache en auth (NV-04); sin datos sensibles en cache.

## COST-001 ($0)
- Free tier: herramientas locales (Macrobenchmark/Profiler de Android Studio, OSS). Limites: tiempo de medicion. Tarjeta: no. Riesgo cobro: ninguno. Mitigacion: sin servicios de performance de pago. Alternativa: N/A.

## Relacion con otras specs
- NV-07 (mapa), NV-22 (imagenes), NV-25 (a11y), NV-19 (medicion en tests).

## Evidencia esperada
- Baseline y comparativa post-optimizacion. Hoy: sin evidencia.
