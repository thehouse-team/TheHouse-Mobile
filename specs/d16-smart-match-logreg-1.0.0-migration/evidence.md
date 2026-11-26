# EVIDENCIA D16 — Smart Match logreg-1.0.0 Migration

| Campo | Valor |
|---|---|
| Fecha | 2026-11-26 |
| Rama | `fix/smart-match-logreg-1.0.0` |
| Base | `origin/main` @ `8d2f1a3` al crear la rama; rebase final sobre `origin/main` @ `28cc3e0` (D14 ya en main) |
| Autorización | Instrucción humana explícita (spec independiente, no mezclar D14/D15) |
| Resultado | **SMART-MATCH-LOGREG-MIGRATION-CODE-READY** |
| Runtime | **PENDING** (sin dispositivo/ADB en la sesión) |

## 1. Declaración de gobernanza (antes de implementar)

```text
SPEC ACTIVA: D16-Smart-Match-Logreg-1.0.0-Migration
PLAN ACTIVO: D16 FASE A/B/C/D
TASK ACTIVO: D16-T01..T15
ESTADO AUTORIZADO: APROBADO
```

Gobernanza: **PASS WITH WARNINGS**. Advertencia: la sesión inició en
`fix/home-account-drawer-navigation` (D15, `d4226ea`). Se creó la rama D16
desde `origin/main` actualizado sin reset/restore/stash; working tree limpio.
`main` avanzó a `28cc3e0` (PR #30, D14) tras la creación; se hizo rebase a esa
base. No se implementó trabajo funcional D14: solo se ajustaron dos `when`
exhaustivos y una aserción de test (`503 → ServiceUnavailable`) como
consecuencia mecánica de los nuevos tipos de error de D16. D16 y D14 siguen en
PR separados (D14 = #30, ya mergeado; D16 = #32, sin merge).

## 2. Cambios implementados

| Requisito | Archivo | Cambio |
|---|---|---|
| matchScore Double | `SmartMatchScoreDto.kt`, `SmartMatchModels.kt`, `SmartMatchViewModel.kt` | `matchScore: Double` en DTO AUTH y dominio; sin `.toInt()` |
| Breakdown estructurado | `SmartMatchModels.kt`, mapper en ViewModel | `SmartMatchFactor(factor,label,points,maxPoints)`; NO `List<String>` |
| UI decimal | `SmartMatchScreen.kt`, `MatchBadge.kt`, `PropertyCard.kt` | `73.5%`, entero `73%`; barras `points/maxPoints` clamp 0..1, sin /100 |
| Etiqueta fallback | `SmartMatchScreen.kt` | "Estimación local" con `contentDescription = smart.match.local.fallback` |
| AUTH authority | `SmartMatchViewModel.kt` | GET recommendations 1:1, sin re-score ni re-order |
| GUEST authority | `SmartMatchViewModel.kt` | POST query 200 1:1; eliminado `fetchCandidates` + ranking local |
| Fallback acotado | `SmartMatchRepository.kt`, `DomainError.kt`, `UiError.kt` | 503/red/timeout → `ServiceUnavailable`/`Network`/`Timeout`; 400→`ValidationError`, 401→`Unauthorized`, 404→`NotFound`, 422→`NoPreferences`, 429→`RateLimited` |
| Reconexión | `ConnectivityObserver` existente + `SmartMatchViewModel.observeConnectivity()` | Al volver online con estimación local, `load()` reemplaza por remoto; sin polling |
| Versiones | `SmartMatchModelVersions` | SERVER `logreg-1.0.0`, LOCAL `1.0.0`, EQUIVALENCE `UNKNOWN` |
| Fixtures online | `SmartMatchBackendContractTest.kt` | `logreg-1.0.0`, maxPoints 9.6/72.6/6.7/4.9/1.7/4.5 |
| Artefacto local | `model_v1.json` | **INTACTO** (`1.0.0`); tests `ModelArtifactTest`/`SmartMatchMlTest` siguen validando `1.0.0` |
| androidTest | `DeviceSmartMatchTest.kt`, `DeviceUITestSuite.kt`, `DeviceAuthFlowTest.kt` | Firma real con callbacks; nodos reales; eliminado bloque duplicado y "LocalMlFallback" |

## 3. Comandos y resultados reales

```text
.\gradlew.bat clean compileDebugKotlin   → BUILD SUCCESSFUL (41s)
.\gradlew.bat testDebugUnitTest            → BUILD SUCCESSFUL
                                             454 tests / 0 failures / 0 errors (70 clases; incluye 2 tests críticos fix Guest)
                                             D16: 14 tests nuevos (A–N)
.\gradlew.bat lintDebug                    → BUILD SUCCESSFUL (1m57s)
                                             0 errores lint
.\gradlew.bat assembleDebug                → BUILD SUCCESSFUL (31s)
git diff --check                           → exit 0
```

CI remoto: **VERDE** — GitHub Actions `Android quality gates`, run `45860702839`, conclusión `success`, 7m17s (verificado con `gh pr checks 32`).

## 4. Cobertura de tests A–N

| Test | Resultado |
|---|---|
| A — `matchScore=73.5` deserializa sin truncar | PASS |
| B — seis maxPoints decimales (9.6/72.6/6.7/4.9/1.7/4.5) | PASS |
| C — UI `73.5%` (y `73%` para entero) | PASS |
| D — `progress = points/maxPoints`, clamp 0..1 | PASS |
| E — `maxPoints=72.6` sin romper layout | PASS |
| F — `maxPoints=1.7` visible | PASS |
| G — AUTH 200 sin re-score, orden y score del Backend | PASS |
| H — GUEST 200 sin re-score, orden y score del Backend | PASS |
| I — network → fallback local + etiqueta | PASS |
| J — 503 → fallback local + etiqueta | PASS |
| K — 400/401/404/422/429 sin fallback | PASS |
| L — retry/reconnect reemplaza estimación local | PASS |
| M — modelVersion remoto `logreg-1.0.0` | PASS |
| N — artefacto local sigue `1.0.0` | PASS |

Archivo: `app/src/test/java/com/thehouse/app/presentation/smartmatch/D16SmartMatchLogregMigrationTest.kt`.
Tests preexistentes de regresión (`SmartMatchRuntimeFlowTest`, `SmartMatchViewModelTest`,
`SmartMatchMlTest`, `SmartMatchBackendContractTest`) actualizados al nuevo contrato decimal.

## 5. Deuda y límites honestos

- **`compileDebugAndroidTestKotlin` NO compila:** `DeviceAuthFlowTest.kt` y
  `DeviceNavigationTest.kt` contienen corrupción sintáctica **preexistente en
  `origin/main`** (`import androidx.test.uiautomator by`, nombres de test con `:`,
  llamadas a Composables sin callbacks, imports faltantes). Se corrigió
  únicamente el bloque Smart Match dentro de D16; el resto queda fuera de alcance
  (AGENTS.md §6: no ampliar la spec). No se afirma que androidTest esté verde.
- **CI remoto:** verde, verificado con `gh pr checks 32` y `gh run view 45860702839` (conclusión `success`).
- **Runtime en dispositivo:** sin ADB/dispositivo en la sesión; la certificación
  `SMART-MATCH-LOGREG-RUNTIME-CERTIFIED` queda **PENDING**.
- **EQUIVALENCE = UNKNOWN:** no se afirma que `model_v1.json` sea el modelo
  productivo `logreg-1.0.0`; no se sustituye el artefacto sin artefacto oficial.
- **COST-001:** $0. Sin servicios nuevos; Backend ya contratado y artefacto local.

## 6. Definition of Done

```text
SPECIFIED ✅  IMPLEMENTED ✅  TESTED ✅  DEPLOYED (PR #32 abierto) ✅  EVIDENCED ✅
```

Estado: `SMART-MATCH-LOGREG-MIGRATION-CODE-READY`.
`SMART-MATCH-LOGREG-RUNTIME-CERTIFIED` requiere verificación en dispositivo y CI verde.

## 7. Fix de regresión Guest (D16-T16)

- **Root cause:** `loadGuest()` invocaba `loadPreferenceOrNull()` → `preferenceRepository.loadPreferences()` → `GET /api/v1/users/me/preferences` sin token (401), por lo que `preferenceState` quedaba vacío y se emitía `NoPreferences` aunque existieran preferencias locales.
- **GUEST_STORAGE_SOURCE:** `OfflineCacheStore` sobre el DataStore existente (`DataStorePreferencesStore`), clave pública `cache_guest_preferences` con `PreferencesSnapshot`; más el `preferenceState` en memoria de `PreferenceRepositoryImpl`. Sin almacenamiento nuevo.
- **Cambio:** `loadGuest()` usa `loadGuestPreferencesOrNull()` → `restoreLocalOrNull()` (0 HTTP). `PreferenceRepositoryImpl.restoreLocalOrNull()` lee la clave guest del DataStore; `writeCache()` escribe también esa clave al guardar preferencias.
- **Tests críticos:** Anonymous con preferencias faltantes → `loadPreferences()=0`, `POST query=0`, `NoPreferences`; Anonymous con preferencias completas → `loadPreferences()=0`, `restoreLocal=1`, `POST query=1`, score `73.5`.
- **Preservado:** matchScore Double, breakdown decimal, `logreg-1.0.0`, autoridad Backend, etiqueta offline, reconexión, flujo AUTH y `model_v1.json` intacto.
- **Gates del fix:** `clean compileDebugKotlin`, `testDebugUnitTest` 454/0/0, `lintDebug` 0 errores, `assembleDebug`, `git diff --check` exit 0.
## 8. Realineación con contrato productivo confirmado (2026-11-26)

Backend/ML confirmó: `matchScore` SIEMPRE entero 0..100; breakdown `points`/`maxPoints` Double; códigos cerrados en el request GUEST; 422 `smart_match_profile_incomplete`; GUEST sin GET de preferencias; detalle debe usar `GET /api/v1/rooms/{roomId}`.

- **MATCH_SCORE_TYPE:** `Int` en `SmartMatchRecommendationItemDto`, `SmartMatchRecommendationDto` y `SmartMatchResult`. UI `"73%"` (nunca `"73.0%"`). `MatchBadge` restaurado a `Int`.
- **BREAKDOWN_TYPES:** `points: Double`, `maxPoints: Double` intactos; `fraction = if (maxPoints > 0) (points/maxPoints).coerceIn(0..1) else 0.0`. Acepta `points=0`, `maxPoints=72.6`, `1.7`, sin NaN.
- **GUEST_CODES_VALIDATED:** `women`, `room`, `walk`, `apartment`, `wifi` — mapper reutiliza `PreferencesContract` (D14); etiquetas visuales descartadas. `@Transient` en `pets`/`currency` de `SmartMatchPreferencesDto` para evitar campos extra (400). Tests de serialización incluidos.
- **EXTRA_FIELDS_REMOVED:** `pets`, `currency` del objeto `preferences`; `currency` solo dentro de `budget` (contractual).
- **GUEST:** 0 `GET /users/me/preferences`, 1 `POST /recommendations/query` (tests 11/12).
- **ROOM_DETAIL:** BLOQUEADO. No existe `RoomDetailScreen`, `RoomDetailViewModel`, ruta de room ni API `GET /api/v1/rooms/{id}` en este repo. NO se inventó pantalla ni endpoint. El click de Smart Match sigue navegando a `PropertyDetailRoute` con `result.property.id` (que proviene de `room.id` del DTO) y el ViewModel de detalle consulta `/properties/{id}`. Requiere spec/autorización nueva.
- **ONNX:** `model.onnx`, `golden_cases.json` NO están en el repo; no se agregó ONNX Runtime. Se mantiene `model_v1.json` con etiqueta "Estimación local".
- **Gates:** `clean compileDebugKotlin`, `testDebugUnitTest` 482/0/0, `lintDebug` 0 errores, `assembleDebug`, `git diff --check` exit 0.

## 9. Auditoría inicial evaluación profesor (solo lectura)

| Ítem | ENDPOINT | MOBILE_IMPLEMENTED | SCREEN | VIEWMODEL | API | TESTS | RUNTIME | BLOCKER |
|---|---|---|---|---|---|---|---|---|
| Búsqueda real cuartos | `GET /api/v1/rooms/search` | NO | NO (`ExploreScreen` usa `/properties`) | SÍ (Explore) | NO | Parciales de explore | PENDING | Sin spec ni contrato móvil de rooms/search |
| Push registration | Sin contrato confirmado de devices/token | NO | NO | NO | NO | NO | PENDING | `FCM-TOKEN-BACKEND-BLOCKED` (AppContainer:602) |
| Eliminar cuenta | Sin `DELETE /users/me` confirmado | PARCIAL (UI + purga local) | SÍ (PrivacyScreen) | SÍ (PrivacyViewModel) | NO | SÍ | PENDING | `DELETE-ACCOUNT-BACKEND-BLOCKED` + `BLOCKED-PRIVACY-URL` |
| Avisos legales | URL pública no publicada | PARCIAL (ruta `privacidad`) | SÍ | SÍ | N/A | SÍ | PENDING | `BLOCKED-PRIVACY-URL` (`BuildConfig.PRIVACY_POLICY_URL` vacío) |