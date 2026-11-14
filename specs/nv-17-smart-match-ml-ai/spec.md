# NV-17 — SMART MATCH ML (REAL, ON-DEVICE)

| Campo | Valor |
|---|---|
| Spec ID | NV-17 |
| Nombre | Smart Match ML real |
| Versión | 1.1.0 (governance + ejecución) |
| Fecha | 2026-11-13 |
| STATUS | EXECUTED (RUNTIME-BLOCKED: sin adb/emulador) |
| IMPLEMENTATION | EJECUTADA en `feature/smart-match-ml` (autorización humana explícita) |
| AUTHORIZATION | Implementación y operaciones Git necesarias autorizadas explícitamente (sin merge, sin force push) |
| Dependencias | NV-09 (favoritos UI/detalle), D11 Preferences, D10/Search+catálogo propiedades, NV-27 (contratos backend), NV-08 (detalle propiedad) |

## 1. Propósito
Evolucionar Smart Match desde placeholder/demo a una solución con **componente de Machine Learning/inferencia real** (on-device), defendible académicamente, integrada a Mobile, con **costo operativo $0 adicional** y sin tratamiento de PII sensible. Reemplaza el enfoque de baseline por reglas como solución final.

## 2. Descubrimiento acotado y Gate F0 (cerrado)

### 2.1 Contratos reales usados
- **Preferences** `GET/PUT api/v1/users/me/preferences` (`Preference`/DTO): `budget{min,max,currency}`, `maxDistanceKm`, `essentialServices[]`, `livingWith`, `placeType`, `pets`, `transport`, `propertyTypes[]`, `currency`.
- **Candidatos** `GET api/v1/properties` (paginado `page/pageSize`, máx. 50) y detalle `GET /api/v1/properties/{id}` (`Property`): `id,title,price,currency,city,universityId,bedrooms,bathrooms,areaM2,createdAt,description?,amenities[]`.
- **Backend:** sin endpoint Smart Match/recomendación/ML (búsqueda acotada). **Endpoint ML existente: NO.**

### 2.2 Gate F0 — decisión (GO)
Features finales (solo señales reales comparables Preference↔Property, sin PII, sin IDs como señal predictiva, sin datos inventados):
1. `budget_fit` (0..1): ajuste de `price` al rango `budget.min/max` (1 en rango; decaimiento proporcional fuera).
2. `services_overlap` (0..1): intersección normalizada `essentialServices ∩ amenities` / |essentialServices| (comparación case-insensitive de strings reales disponibles).
3. `price_affinity` (0..1): afinidad del precio respecto a presupuesto (preferencia por no exceder `budget.max` / cercanía a referencia).
4. `currency_match` (0/1): coincidencia de moneda preferida vs `property.currency`.

**Excluidas por ausencia de señal comparable real en `Property`:** `maxDistanceKm` (sin distancia/coordenadas), `pets`, `transport`, `placeType`, `propertyTypes`, `livingWith` (sin campos equivalentes). `bedrooms/bathrooms/areaM2` existen en Property pero no en Preference como requisito comparable directo, por lo que no se usan como match pairwise inventado. **No se inventan distancia, coordenadas, petFriendly, transport, propertyType ni servicios.**

**Conclusión:** vector suficiente y defendible para un modelo paramétrico entrenado (regresión logística). No se degrada a scoring manual.

## 3. Solución implementada (ML real)

### 3.1 Algoritmo y pipeline
- **Algoritmo:** Regresión logística (binaria de compatibilidad) — modelo **paramétrico realmente entrenado**; inferencia con coeficientes aprendidos (no ponderaciones hardcodeadas en producción).
- **Pipeline:** `Preference + Property` → `SmartMatchFeatureExtractor` → `FeatureVector` → `Normalizer/Scaler (mean,std del artefacto)` → `LogisticRegressionSmartMatchModel (w·x + b)` → `sigmoid` → `probability` → `score = round(probability*100)` → `ranking descendente (probability, id)`.

### 3.2 Entrenamiento offline (reproducible, $0)
- Dataset **sintético controlado**, versionado, **sin PII**, sin datos productivos. **Semilla fija 42.**
- **410 ejemplos** (400 generados + 10 casos borde), 3000 épocas, `lr=0.5`, `l2=0.01`, gradiente batch sobre features estandarizadas.
- **Limitación explícita:** “El modelo inicial fue entrenado sobre datos sintéticos controlados; no representa aprendizaje histórico de comportamiento real de usuarios.”
- Script: `tools/smartmatch/train_smartmatch_model.py` (copia en `specs/nv-17-smart-match-ml-ai/training/train_smartmatch_model.py`). App **no** entrena en runtime.

### 3.3 Artefacto (cargado por la app)
- `app/src/main/assets/smartmatch/model_v1.json`: `modelVersion=1.0.0`, `featureVersion=1.0.0`, `algorithm=logistic_regression`, `features`, `coefficients`, `bias`, `scaler{mean,std}`, `training`, `sha256`.
- **SHA-256 (declarado/verificado):** `1e918ea48458f62bd7101a4c9b3400974f39d0f6d26660a8823f5e6edbed1537` (hash del JSON del artefacto sin campo `sha256`, según `training_metadata.json`).
- Carga/verificación: `data/smartmatch/LogisticRegressionSmartMatchModel.loadFromAssets/loadFromJson` (valida algoritmo, tamaños, y hash).

### 3.4 Candidatos reales y política
- Fuente: `GET /api/v1/properties` vía `GetPropertiesUseCase` existente. Política implementada: hasta **3 páginas** de `pageSize=50` (máx. ~150 candidatos), detiene si alcanza `totalPages` o página vacía, `distinctBy(id)`. Sin descarga infinita, sin datos fake.

### 3.5 UI/UX
- `SmartMatchScreen` reemplaza placeholder: estados **Loading / RequiresAuth / NoPreferences (CTA “Configurar preferencias”) / Empty / Content / Error**.
- Content: ranking descendente por score ML; por resultado muestra compatibilidad %, `MatchBadge`, factores estructurados, y `PropertyCard` real con navegación a detalle y acciones favorito/contacto. Reutiliza componentes existentes (sin requests desde Compose).
- Nota: no existe ruta UI de Preferences D11 en presentation; CTA navega a fallback seguro (home) documentado.

### 3.6 Score vs explicación
- **ML SCORE:** salida matemática del modelo (`probabilidad*100`, acotado 0–100). **FACTORES:** comparaciones estructuradas reales (presupuesto, servicios coincidentes, moneda), separados del score. Sin GenAI, sin afirmar causalidad interna no demostrada.

## 4. Privacidad y costo
- Features/modelo sin nombre, email, teléfono, JWT, refresh token, chat, solicitudes, reportes, acuerdos, códigos de renta, PII innecesaria. Sin telemetría sensible, sin red externa para ML.
- **COST-001:** $0 adicional (inferencia on-device, entrenamiento offline local, sin servicios/endpoints externos, sin API keys, sin tarjeta).

## 5. Limitaciones (documentadas)
- Entrenamiento sobre **datos sintéticos** (no comportamiento histórico real de usuarios).
- Vector reducido (4 features) por contrato Property disponible; preferencias de distancia/mascotas/transporte/tipo/convivencia no mapeables hoy → poder predictivo limitado y honestamente acotado.
- `services_overlap` depende de coincidencia textual `essentialServices`↔`amenities` (case-insensitive); sin taxonomía cerrada garantizada.
- Sin runtime verificado en dispositivo/emulador en esta sesión (`RUNTIME-BLOCKED`).

## 6. Validación ejecutada
- `compileDebugKotlin`: **BUILD SUCCESSFUL**.
- `testDebugUnitTest lintDebug assembleDebug`: **tests=213, failures=0, errors=0, skipped=0** (incluye tests NV-17: feature extraction, normalización/scaler, parsing/hash/version, inferencia/sigmoid, score 0–100, ranking, determinismo, cambio de preferencias, moneda/presupuesto incompatibles, amenities mapping, no-PII, factores, artefacto malformado, estados VM NoPreferences/Empty/Error/Content/Auth, y prueba de uso de coeficientes del artefacto), **lint errores=0**, **APK debug generado**.
- Runtime: **RUNTIME-BLOCKED** (adb/emulador no disponibles).

## 7. No-alcance respetado
Sin GenAI, sin servicios ML externos/pagados, sin endpoints/contratos inventados, sin datos fake en producción, sin merge/force push. Baseline por reglas no usado como motor de producción.