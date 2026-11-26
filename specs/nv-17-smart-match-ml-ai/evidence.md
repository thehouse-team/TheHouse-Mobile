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

## 11. Runbook SMART-MATCH RUNTIME — NO HTTP REQUEST (2026-11-26)

Autorización humana explícita. Alcance: instrumentación y diagnóstico del flujo; sin Backend, contratos, GenAI, ML ni fallback artificial.

### Hallazgo (causa raíz)
- `SmartMatchScreen` crea el VM con `viewModel(factory = factory)` y dispara `LaunchedEffect(Unit) { vm.load() }` — **una** carga por entrada lógica; retry llama al mismo `vm.load()`.
- `U2ViewModelFactory.create` entrega dependencias reales (`sessionStore.state`, `preferencesRepository`, `smartMatchRepository`, `getProperties`, `runSmartMatch`, `dispatchers`); sin placeholders.
- En **Anonymous**, `loadPreferenceOrNull()` invoca `PreferenceRepositoryImpl.loadPreferences()` → `GET /api/v1/users/me/preferences`, que exige Bearer. Sin sesión hay 401; el `catch (ignored: Exception)` anterior lo tragaba y devolvía `preferenceState.first()` (StateFlow inicializado en `null`) → **NoPreferences sin HTTP de Smart Match**.
- En **Active**, el flujo pasa de preferencias a candidatos y ranking ML on-device; **no llama** al endpoint guest `queryRecommendations` (contrato NV-17 original).
- `fetchCandidates()` usa `GetPropertiesUseCase` → `GET /api/v1/properties` con caché; una lista vacía válida conduce a `Empty`, no a Error. Las excepciones se mapean a `DomainException` y ahora se registran en DEBUG.

### Cambios
- `SmartMatchFlowLogger.kt` (nuevo): tag `SmartMatchFlow`, solo DEBUG (`BuildConfig.DEBUG`), sink inyectable para tests. Eventos: `enter_screen`, `viewmodel_created`, `load_start`, `session=<Active|Anonymous|Suspended>`, `preferences_start`, `preferences_result=<present|missing|incomplete>`, `guest_query_start`, `auth_query_start`, `candidates_start`, `candidates_result count=<n>`, `ranking_start`, `state=<...>` y `exception=<SimpleName> operation=<...>`. Sin tokens, correo, nombre, bodies, preferencias completas ni stacktraces.
- `SmartMatchViewModel.kt`: logging en cada etapa; excepciones convertidas en `UiError` seguro pero con causa observable en DEBUG; `load()` es el único disparador; estados `NoPreferences`/`Empty` separados de `Error`.
- `SmartMatchScreen.kt`: emite `enter_screen` / `viewmodel_created` sin lógica de negocio.

### Pruebas nuevas
- `SmartMatchFlowLoggerTest`: eventos requeridos, sin PII, sin mensaje de excepción.
- `SmartMatchRuntimeFlowTest`: A) Anonymous con preferencias → 1 query; B) Anonymous sin preferencias → NoPreferences, 0 query; B2) incompletas → NoPreferences; C) excepción en candidates → Error controlado + log; C2) fallo de preferencias → NoPreferences + log; D) retry → exactamente una carga adicional; E) Active íntegro; F) Suspended → RequiresAuth; sesión leída en tiempo de carga como la factory real.

### Gates (resultados reales)
- `clean compileDebugKotlin` — BUILD SUCCESSFUL.
- `testDebugUnitTest` — **412 tests / 0 failures / 0 errors**.
- `lintDebug` — BUILD SUCCESSFUL (0 errores; warnings preexistentes).
- `assembleDebug` — BUILD SUCCESSFUL.
- `git diff --check` — exit 0 (solo advertencia LF→CRLF del archivo modificado).

### PENDIENTE (no afirmado)
- Sin dispositivo/ADB en la sesión: las líneas `SmartMatchFlow` y `TheHouseHttp` en runtime **no fueron capturadas**. La causa raíz está demostrada en código y tests; la confirmación en dispositivo queda para el humano con el APK `app-debug.apk` generado. `HTTP_CALL_REACHED` = NO VERIFICADO EN DISPOSITIVO.

## 12. Migración del flujo AUTH a NV-23 v1.2.0 (2026-11-26, PR #29)

Autorización humana explícita; mismo branch/PR (`fix/smart-match-guest-runtime`), sin merge. Fuente de verdad: código productivo del Backend `origin/main` (`SmartMatchEndpoint.cs`, `SmartMatchService.cs`, `SmartMatchContracts.cs`, `SearchContracts.cs`, `UserEndpointSupport.cs`), no suposiciones.

### Contrato confirmado
- AUTH `GET /api/v1/smart-match/recommendations?page&pageSize`: **autenticado**, 200 `{items[{room:RoomSearchItem, matchScore, breakdown, dismissed, modelVersion, featureSchemaVersion}], page, pageSize, totalItems, totalPages}`; 422 `smart_match_profile_incomplete` con `missing=[university,budget.max,livingWith,placeType]`; 401; 429; 503.
- El Backend **resuelve perfil, preferencias, universidad, scoring, ranking, breakdown y descartes**; Mobile NO reordena ni re-scorea una respuesta 200. El ranking on-device de NV-17 queda **solo** como fallback offline/network/503.

### Cambios
- DTOs: `SmartMatchRoomSearchDto`, `SmartMatchApproxLocationDto`, `SmartMatchFactorDto`, `SmartMatchRecommendationItemDto` y `SmartMatchListResponse` reescritos al contrato real (room anidado + breakdown + versiones). Sin campos inventados.
- `SmartMatchRepository.getRecommendations()` usa `Result` y mapea errores a dominio: 422→`SmartMatchProfileIncompleteException(missing)`, 401/403→`Unauthorized`, 429→`RateLimited`, 404→`NotFound`, 5xx→`Server`, IOException→`Network`, timeout→`Timeout`.
- `SmartMatchViewModel.loadAuthenticated()`: una sola llamada a `getRecommendations(page=1,pageSize=20)`; 200 items→Content (mapping 1:1, orden y score del Backend intactos), 200 vacío→Empty, 422→NoPreferences (con `profileMissing` expuesto), 401→RequiresAuth, 429/400/404→Error tipado, network/timeout/503→`localMlFallback()` con `preferences_start`/`fetchCandidates`/`ranking`. Online AUTH **no** carga preferencias ni catálogo.
- `Throwable.toSafeUiError()` reemplaza al mapper incorrecto (`DomainException` → `Unknown`).
- `SmartMatchFlowLogger`: `auth_query_result count=<n>`, `auth_query_result=profile_incomplete`, `fallback_start reason=<network|503>`, `candidates_start` solo en fallback.

### Pruebas nuevas/actualizadas
- `SmartMatchRuntimeFlowTest`: E reescrita a autoridad Backend; A–H (200/empty/422/401/429/503/network/400-404); regresión Anonymous intacta.
- `SmartMatchViewModelTest`: 4 tests Active reescritos al contrato real (422, Empty, 429 tipado, orden/score del Backend).
- `SmartMatchBackendContractTest` (nuevo): parseo del JSON real del Backend y del Problem Details 422.

### Gates (resultados reales)
- `clean compileDebugKotlin` — BUILD SUCCESSFUL.
- `testDebugUnitTest` — **423 tests / 0 failures / 0 errors** (baseline 413, +10).
- `lintDebug` — BUILD SUCCESSFUL (0 errores).
- `assembleDebug` — BUILD SUCCESSFUL.
- `git diff --check` — exit 0 (solo advertencia LF→CRLF).

### PENDIENTE
- Sin dispositivo/ADB: captura de `SmartMatchFlow`/`TheHouseHttp` en runtime con usuario autenticado queda para verificación humana. Resultado de código: **SMART-MATCH-ACTIVE-CODE-READY-FOR-RUNTIME**.