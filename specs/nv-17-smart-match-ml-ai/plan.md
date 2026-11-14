# NV-17 — PLAN (SMART MATCH ML REAL)

| Campo | Valor |
|---|---|
| Spec | NV-17 Smart Match ML |
| Versión | 1.1.0 |
| Fecha | 2026-11-13 |
| STATUS | EXECUTED (RUNTIME-BLOCKED) |

## Resultado de ejecución por fases

### F0 — Cierre de contrato y viabilidad de features (COMPLETADO, GO)
- Contratos verificados: Preferences (`budget.min/max,currency,maxDistanceKm,essentialServices,livingWith,placeType,pets,transport,propertyTypes`) y Properties (`id,title,price,currency,city,universityId,bedrooms,bathrooms,areaM2,description,amenities,createdAt`). Sin endpoint ML backend.
- **Features finales reales/comparables:** `budget_fit`, `services_overlap` (essentialServices↔amenities), `price_affinity`, `currency_match`.
- **Excluidas (sin señal comparable real en Property):** `maxDistanceKm`, `pets`, `transport`, `placeType`, `propertyTypes`, `livingWith`; sin campos inventados (distancia/coordenadas/petFriendly/transport/propertyType/servicios). `bedrooms/bathrooms/areaM2` sin par de preferencia directo.
- **Decisión:** GO (vector defendible para regresión logística). Sin degradación a reglas.

### F1 — Modelo y artefacto (COMPLETADO)
- Algoritmo principal: **regresión logística** entrenada offline determinísticamente sobre dataset sintético/fixtures versionado (seed 42, 410 ejemplos, sin PII/prod).
- Artefacto: `app/src/main/assets/smartmatch/model_v1.json` (coefficients, bias, scaler mean/std, modelVersion/featureVersion 1.0.0, sha256 `1e918ea48458f62bd7101a4c9b3400974f39d0f6d26660a8823f5e6edbed1537`).
- Script/procedimiento: `tools/smartmatch/train_smartmatch_model.py` y copia en `specs/nv-17-smart-match-ml-ai/training/`; metadata en `training/training_metadata.json`.
- Score: `round(sigmoid(w·x_norm + b) * 100)` acotado 0–100; ranking descendente determinista.

### F2 — Dominio/datos de inferencia (COMPLETADO)
- Domain: `domain/smartmatch` (`FeatureVector`, `SmartMatchFeatures`, `SmartMatchFeatureExtractor`, `SmartMatchModel`, `SmartMatchResult`) y `domain/usecase/RunSmartMatchUseCase`.
- Data: `data/smartmatch` (`SmartMatchModelArtifact`, `LogisticRegressionSmartMatchModel` loader+inferencia con parámetros del artefacto). Sin segundo Retrofit.
- Integración: `AppContainer` (model/use case), `U2ViewModelFactory`/`SmartMatchViewModel` (session/preferences/properties + inferencia en `dispatchers.default`), UDF/MVVM.
- Política candidatos: hasta 3 páginas x 50, distinct por id.

### F3 — UI/UX Smart Match (COMPLETADO)
- `presentation/smartmatch/SmartMatchScreen` real: Loading / RequiresAuth / NoPreferences (CTA “Configurar preferencias”) / Empty / Content / Error.
- Cards con score ML, factores separados, `MatchBadge`, `PropertyCard` real, navegación a detalle y acciones favorito/contacto. Reutiliza componentes existentes. CTA preferencias → fallback home (ruta UI D11 no disponible).

### F4 — Calidad, pruebas y evidencia (COMPLETADO con runtime bloqueado)
- Tests NV-17: feature extraction, normalización (scaler), parsing/hash/version, inferencia/sigmoid, score 0–100, ranking, determinismo, cambio de preferencias, incompatibilidad moneda/presupuesto, amenities mapping, no-PII, factores, artefacto malformado, estados VM (NoPreferences/Empty/Error/Content/RequiresAuth), uso de coeficientes del artefacto (no hardcode).
- Validación: `compileDebugKotlin` BUILD SUCCESSFUL; `testDebugUnitTest lintDebug assembleDebug` → 213 tests, 0 failures, 0 errors, lint errores 0, APK debug generado.
- Runtime: **RUNTIME-BLOCKED** (sin adb/emulador). Evidence actualizada.

## Precondiciones y Git
- Main actualizado con D13.1 (merge #17) y D11 presente; rama `feature/smart-match-ml` creada desde main. Commit/push según autorización (sin merge/force push).