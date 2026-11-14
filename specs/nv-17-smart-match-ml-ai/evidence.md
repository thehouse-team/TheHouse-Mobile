# NV-17 — EVIDENCE (SMART MATCH ML)

| Campo | Valor |
|---|---|
| Spec | NV-17 Smart Match ML |
| Versión doc | 1.1.0 |
| Fecha | 2026-11-13 |
| Branch | `feature/smart-match-ml` (desde `main` @ `3c1bd1c`, D13.1/D11 presentes) |
| STATUS | EXECUTED — **RUNTIME-BLOCKED** (sin adb/emulador) |

## 1. Resumen ejecutivo
Smart Match implementado con **ML real (regresión logística, inferencia on-device)** usando parámetros aprendidos de un artefacto versionado (entrenamiento offline determinista sobre dataset sintético, seed 42, 410 ejemplos, sin PII). Candidatos reales desde `GET /api/v1/properties`, preferencias reales D11. Sin endpoint ML backend, sin GenAI, costo $0. Validación: compile/tests/lint/assemble en verde; runtime bloqueado por ausencia de adb/emulador.

## 2. Gate F0 (cerrado, GO)
- **Candidatos (Property):** `id,title,price,currency,city,universityId,bedrooms,bathrooms,areaM2,createdAt,description?,amenities[]`.
- **Preferencias (D11):** `budget{min,max,currency}, maxDistanceKm, essentialServices, livingWith, placeType, pets, transport, propertyTypes, currency`.
- **Features finales (reales/comparables):** `budget_fit`, `services_overlap` (essentialServices∩amenities), `price_affinity`, `currency_match`.
- **Excluidas (sin señal comparable en Property / no inventadas):** `maxDistanceKm` (sin distancia/coordenadas), `pets`, `transport`, `placeType`, `propertyTypes`, `livingWith`. No se usan IDs como señal predictiva ni PII.
- **Decisión:** GO (vector defendible). Endpoint ML backend: **NO**.

## 3. Modelo y entrenamiento (ML real)

| Ítem | Valor |
|---|---|
| Algoritmo | Logistic Regression (clasificación/regresión de compatibilidad, sigmoide) |
| Por qué es ML | Modelo paramétrico **entrenado** (ajuste de coeficientes/bias por gradiente) que aplica parámetros aprendidos en inferencia; no reglas/ifs/ponderaciones manuales como motor de producción |
| Dataset | Sintético controlado, versionado, sin PII, sin datos productivos |
| Semilla | 42 |
| Ejemplos de entrenamiento | 410 (400 generados + 10 casos borde) |
| Épocas | 3000 |
| Learning rate | 0.5 |
| L2 | 0.01 |
| Features | `["budget_fit","services_overlap","price_affinity","currency_match"]` (featureVersion 1.0.0) |
| Normalización | Estandarización por `scaler.mean/std` del artefacto: `xn = (x-mean)/std` |
| Modelo | `z = bias + Σ(w_i * xn_i)`, `p = sigmoid(z)`, `score = round(p*100)` acotado [0,100] |
| Coefficients / bias | Persistidos en artefacto (aprendidos): ver `app/src/main/assets/smartmatch/model_v1.json` |
| modelVersion | 1.0.0 |
| SHA-256 (artefacto sin campo sha256) | `1e918ea48458f62bd7101a4c9b3400974f39d0f6d26660a8823f5e6edbed1537` |
| Hash method | sha256 de serialización JSON del artefacto sin `sha256` (indent=2, ensure_ascii=False, sort_keys=False, LF) — ver `training_metadata.json` |
| Script/procedimiento | `tools/smartmatch/train_smartmatch_model.py` (copia: `specs/nv-17-smart-match-ml-ai/training/train_smartmatch_model.py`) |
| Metadata | `specs/nv-17-smart-match-ml-ai/training/training_metadata.json` |

**Limitación explícita:** “El modelo inicial fue entrenado sobre datos sintéticos controlados; no representa aprendizaje histórico de comportamiento real de usuarios.” La app **no** entrena en runtime; solo carga artefacto e infiere.

## 4. Pipeline de inferencia (producción)
`INPUT (Preference + Property candidatos reales)` → `FeatureExtractor` → `Normalization (scaler)` → `LogisticRegressionModel (coeficientes del artefacto)` → `sigmoid` → `probability` → `score 0–100` → `ranking descendente`.

- Motor usa coeficientes/bias/scaler del artefacto (`data/smartmatch/LogisticRegressionSmartMatchModel`, verificación de hash/tamaños/algoritmo). Baseline por reglas no usado en producción.
- Inferencia fuera de UI thread (`dispatchers.default` en ViewModel). Sin recálculo en recomposición por flujo de carga explícito. Sin requests desde Compose.

## 5. Datos, candidatos y UI
- **Preferencias:** repositorio D11 (`loadPreferences` / `GET /users/me/preferences`).
- **Candidatos:** `GET /api/v1/properties` (hasta 3 páginas x 50, distinct por id), sin descarga infinita, sin datos fake en producción.
- **UI:** `SmartMatchScreen` + `SmartMatchViewModel` — estados Loading, RequiresAuth, NoPreferences (CTA “Configurar preferencias”), Empty, Content, Error. Content: score ML + `MatchBadge`, factores estructurados separados, `PropertyCard` real, favorito, abrir detalle. Reutiliza componentes existentes. CTA preferencias → fallback home (ruta UI D11 no expuesta).

## 6. Privacidad y costo
- **No usados en features/modelo/logs:** nombre, email, teléfono, JWT, refresh token, conversaciones/solicitudes, reportes, acuerdos, códigos de renta, PII innecesaria. Sin telemetría sensible, sin terceros.
- **COST-001:** $0 adicional (on-device, entrenamiento offline local, sin servicios/endpoints externos, sin API keys, sin tarjeta).

## 7. Pruebas (NV-17 añadidas)
- `domain/smartmatch/SmartMatchMlTest`: extracción de features (budget_fit, services_overlap, currency_match), inferencia/sigmoid en rango, parsing/version/hash de artefacto, **motor usa coeficientes del artefacto** (no hardcode; salida cambia con artefacto mutado), score [0,100], ranking descendente, determinismo, cambio de preferencias altera resultado, moneda/presupuesto incompatibles no artificialmente altos, amenities mapping, no-PII en vector, factores estructurados separados.
- `data/smartmatch/ModelArtifactTest`: artefacto malformado/algoritmo inválido, normalización aplicada (scaler), hash declarado coincide.
- `presentation/smartmatch/SmartMatchViewModelTest`: estados RequiresAuth, NoPreferences, Empty, Error (red/DomainError), Content con resultados rankeados e IDs reales (integrables con detalle/favoritos).

## 8. Validación (resultados reales)

| Comando | Resultado |
|---|---|
| `compileDebugKotlin` | **BUILD SUCCESSFUL** (43s, cache) |
| `testDebugUnitTest` | **213 tests, 0 failures, 0 errors, 0 skipped** |
| `lintDebug` | **0 errores** (issues no-error presentes en reporte) |
| `assembleDebug` | **BUILD SUCCESSFUL**, `app/build/outputs/apk/debug/app-debug.apk` generado |

Ejecución del set `testDebugUnitTest lintDebug assembleDebug` realizada una vez (compile previo verde). Sin fallos que requirieran ciclo correctivo adicional más allá del fix de compilación menor (`DomainError.Unknown`).

## 9. Runtime
- **RUNTIME-BLOCKED:** `adb`/`emulador` no disponibles en el entorno. No se pudo ejecutar flujo Login→Preferences→Smart Match→ranking→Detalle→Favoritos en dispositivo. Sin validación de runtime afirmada.

## 10. Limitaciones y notas
- Entrenamiento sobre dataset sintético (no aprendizaje histórico real de usuarios); poder predictivo acotado por 4 features y contrato Property.
- `maxDistanceKm`, mascotas, transporte, tipo/convivencia no mapeables a Property actual (excluidos). Overlap de servicios depende de matching textual de strings.
- Ruta UI de Preferences D11 no expuesta; CTA “Configurar preferencias” usa fallback a home.
- Sin verificación runtime en dispositivo (bloqueado por entorno).