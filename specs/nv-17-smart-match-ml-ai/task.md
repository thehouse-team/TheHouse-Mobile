# NV-17 — TASKS (SMART MATCH ML REAL)

| Campo | Valor |
|---|---|
| Spec | NV-17 Smart Match ML |
| Versión | 1.1.0 |
| Fecha | 2026-11-13 |
| STATUS | EXECUTED (RUNTIME-BLOCKED) |

## F0 — Contrato y viabilidad (GO)
- [x] NV17-T00 — Contratos verificados (preferences/properties reales) y ausencia de endpoint ML backend documentada; gaps de features identificados.
- [x] NV17-T01 — Feature vector final cerrado: `budget_fit, services_overlap, price_affinity, currency_match`; exclusiones documentadas (distancia/mascotas/transporte/tipo/convivencia sin señal comparable; sin campos inventados). Decisión: **GO**.

## F1 — Modelo (ML real)
- [x] NV17-T02 — Dataset sintético/fixtures versionado (sin PII/prod), seed 42, 410 ejemplos, entrenamiento determinista regresión logística documentado (`tools/smartmatch/train_smartmatch_model.py`, `specs/nv-17-smart-match-ml-ai/training/`).
- [x] NV17-T03 — Artefacto versionado generado (`app/src/main/assets/smartmatch/model_v1.json`): coefficients/bias/scaler, modelVersion/featureVersion 1.0.0, sha256 `1e918ea4...`, score `probability*100`.

## F2 — Dominio/datos e inferencia
- [x] NV17-T04 — Extracción y normalización de features desde `Preference`+`Property` reales (`SmartMatchFeatureExtractor`, scaler del artefacto).
- [x] NV17-T05 — Cargador de modelo y motor de inferencia on-device (`LogisticRegressionSmartMatchModel`, `RunSmartMatchUseCase`), ranking determinista; integrado con preferencias/candidatos reales (MVVM+Clean+UDF).
- [x] NV17-T06 — Inferencia en `dispatchers.default`, sin recálculo en recomposición por diseño de flujo VM, candidatos acotados (3 páginas x 50, distinct).

## F3 — UI/UX
- [x] NV17-T07 — `SmartMatchScreen` real: flujo auth→preferencias→candidatos→inferencia→ranking; estados Loading/NoPreferences(CTA)/Empty/Content/Error.
- [x] NV17-T08 — Cards reales con score ML derivado, factores separados, favorito/contacto y navegación a Property Detail; reutiliza componentes existentes.

## F4 — Pruebas, validación y evidencia
- [x] NV17-T09 — Tests: feature extraction, normalización, inferencia/sigmoid, determinismo, rango [0,100], ranking, cambio de preferencias, incompatibilidades/moneda, amenities mapping, no-preferences, empty, errores, artefacto malformado/hash/version, privacidad (no PII), coeficientes del artefacto.
- [x] NV17-T10 — Tests de integración lógica: resultados con propertyId real (detalle/favoritos); VM estados; performance básica (inferencia fuera de main vía dispatcher default). UI instrumentada no disponible.
- [x] NV17-T11 — Validación: `compileDebugKotlin` OK; `testDebugUnitTest lintDebug assembleDebug` OK (213 tests, 0 failures, 0 errors; lint errores 0). **Runtime: RUNTIME-BLOCKED** (sin adb/emulador).
- [x] NV17-T12 — `evidence.md` completada con algoritmo, entrenamiento, artefacto/hash, pipeline, costo $0, privacidad, limitaciones y resultados reales. (Trazabilidad/memoria: actualización recomendada en seguimiento documental.)

## Notas
- CTA “Configurar preferencias” implementada; ruta UI de Preferences D11 no existe en presentation → fallback seguro (home). No se inventaron pantallas/contratos.