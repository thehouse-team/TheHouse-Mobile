# SPEC D16 — Smart Match logreg-1.0.0 Migration

| Campo | Valor |
|---|---|
| ID | D16-Smart-Match-Logreg-1.0.0-Migration |
| Versión | 1.0.0 |
| Fecha | 2026-11-26 |
| Estado | APROBADA (autorización humana explícita 2026-11-26) |
| Rama | `fix/smart-match-logreg-1.0.0` desde `origin/main` @ `8d2f1a3` |
| Independencia | NO mezclar con D14 Preferences ni D15 Drawer |
| Jerarquía | constitution.md → AGENTS.md → memoria.md → esta spec → plan → task |

## 1. Problema

El contrato Smart Match productivo del Backend usa `modelVersion = "logreg-1.0.0"` y un `breakdown` decimal con `maxPoints` reales por factor:

| factor | maxPoints |
|---|---|
| budget | 9.6 |
| distance | 72.6 |
| essential_services | 6.7 |
| place_type | 4.9 |
| property_type | 1.7 |
| extras | 4.5 |

El Mobile actual (`origin/main` @ `8d2f1a3`) presenta desviaciones detectadas en auditoría:

1. `SmartMatchResult.score: Int` trunca `matchScore: Double` del Backend.
2. `SmartMatchResult.factors: List<String>` descarta `factor/label/points/maxPoints`.
3. La UI dibuja barras con máximo hardcodeado `100` y divide `maxPoints/100`.
4. El flujo AUTH (`GET /api/v1/smart-match/recommendations`) hace **re-score local** en vez de usar orden/score/breakdown del Backend.
5. El flujo GUEST (`POST /api/v1/smart-match/recommendations/query` 200) también hace **re-score local**.
6. El fallback local no está etiquetado ante el usuario y puede activarse en errores que no son de red/timeout/503.
7. No hay sustitución automática del resultado local por el remoto al recuperar conectividad.
8. androidTest de Smart Match usan firma vieja de `SmartMatchScreen` (sin callbacks obligatorios) y nodos inexistentes (`smart.match.loading`, `smart.match.local.fallback`, `action.complete.profile`, "LocalMlFallback imaginario").

## 2. Alcance

### Incluido

- Tipos: `matchScore: Double` en DTO, dominio, mappers, UI y tests (sin truncamiento ni conversión silenciosa a `Int`).
- Modelo de dominio de breakdown equivalente (`factor`, `label`, `points`, `maxPoints`), NO `List<String>`.
- UI: score formateado (ej. `73.5%`; entero del Backend se muestra sin regresión visual, ej. `73%` o `73.0%` legible); barras con `progress = points / maxPoints`, clamp `0..1`, sin divisor 100 ni máximo hardcodeado.
- Backend como autoridad ONLINE: AUTH y GUEST usan orden, score y breakdown del Backend; **NO re-score, NO re-order** en respuestas 200.
- Fallback local SOLO para `offline / network / timeout / 503`. NUNCA para `400 / 401 / 404 / 422 / 429`.
- Etiqueta visible "Estimación local" / "Estimado sin conexión" cuando se use fallback local; no presentarlo como score oficial.
- Reconexión: resultado local se sustituye por resultado remoto al recuperar conectividad o retry; **un solo score a la vez**; sin polling agresivo. Se reutiliza `ConnectivityObserver` existente (`domain/connectivity/ConnectivityObserver.kt`, `data/connectivity/AndroidConnectivityObserver.kt`, ya en `AppContainer`).
- Error mapping tipado: `400 → ValidationError`, `401 → RequiresAuth`, `404 → NotFound`, `422 smart_match_profile_incomplete → NoPreferences/profileMissing`, `429 → RateLimited`, `503/network/timeout → fallback local`.
- Fixtures contractuales online: `modelVersion = logreg-1.0.0` y maxPoints decimales anteriores. **NO** cambiar tests del artefacto local (`version=1.0.0`).
- Corrección de androidTest de Smart Match para la UI real (firma con callbacks, nodos reales, sin estados inventados).
- Documentación: spec/plan/task/evidence + `memoria.md` con `SERVER_MODEL = logreg-1.0.0`, `LOCAL_MODEL = 1.0.0`, `EQUIVALENCE = UNKNOWN`.
- Gates: `clean compileDebugKotlin`, `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `git diff --check`, CI remoto verde.
- Git: rama independiente, `git add`/`commit`/`push`/PR separado; **sin merge automático**.

### Excluido

- Sustituir `model_v1.json` ni afirmar equivalencia con `logreg-1.0.0`.
- Cambiar tests del artefacto local que validan `version=1.0.0`.
- D14 Preferences, D15 Drawer, NV-03/Play, secretos, Firebase, Maps ni dependencias nuevas.
- Publicar en Google Play ni mergear el PR sin autorización y certificación runtime.

## 3. Requisitos funcionales

### RF-01 — Tipos decimales
`SmartMatchRecommendationItemDto.matchScore`, `SmartMatchResult.matchScore`, mappers y UI usan `Double`. Se prohíbe `.toInt()`, `roundToInt()` o formateo que pierna el decimal. Presentación: `String.format(Locale.US, "%.1f%%", score)` → `73.5%`. Si Backend devuelve `73.0`, se muestra `73.0%` sin regresión visual (aceptable también `73%` si el test lo especifica; se prioriza `%.1f`).

### RF-02 — Breakdown estructurado
`SmartMatchResult.factors: List<SmartMatchFactor>` con `factor: String`, `label: String`, `points: Double`, `maxPoints: Double`. La UI renderiza cada factor real con `label`, `points` legibles y barra `progress = (points / maxPoints).coerceIn(0.0, 1.0)`. `maxPoints = 72.6` y `1.7` deben renderizar sin overflow ni desaparición (barra mínima visible cuando `points > 0`).

### RF-03 — Backend authority (AUTH)
`SessionState.Active` → `GET /api/v1/smart-match/recommendations`. Se consumen `items` en el orden del Backend, con su `matchScore` y breakdown (vía `GET /scores` o el campo incluido según contrato NV-23). **Sin `runSmartMatch.execute`, sin `sortedWith`.**

### RF-04 — Backend authority (GUEST)
`SessionState.Anonymous` → `POST /api/v1/smart-match/recommendations/query`. En respuesta 200: orden, score y breakdown del Backend. **Eliminar el bloque de re-score local** (`fetchCandidates()` + `runSmartMatch.execute`) de la ruta 200.

### RF-05 — Fallback local acotado
Enum `SmartMatchSource { BACKEND, LOCAL_ESTIMATE }` en el estado de UI. Fallback local permitido únicamente cuando la causa es `offline`, `IOException`/timeout o `HttpException.code() == 503`. Prohibido en `400`, `401`, `404`, `422`, `429` → esos errores se mapean a `UiError` tipado y estado `Error`. En fallback, la UI muestra texto "Estimación local" o "Estimado sin conexión" junto al score.

### RF-06 — Recuperación
`SmartMatchViewModel` observa `connectivityObserver.isOnline`. Cuando una estimación local está activa y `isOnline` pasa a `true` (o el usuario pulsa Reintentar), se ejecuta `load()` de nuevo y el estado se sustituye por el remoto. No se conservan dos scores. Sin polling; solo eventos de conectividad y acción explícita.

### RF-07 — Error mapping tipado
`DomainError` existente se extiende/usa con: `ValidationError` (400), `RequiresAuth` (401), `NotFound` (404), `NoPreferences/profileMissing` (422 con `code = smart_match_profile_incomplete`), `RateLimited` (429). Ninguno de esos degrada a `Unknown`.

### RF-08 — Model version
Constantes documentadas en código y docs:
```
LOCAL_MODEL_VERSION = 1.0.0
SERVER_MODEL_VERSION = logreg-1.0.0
LOCAL_EQUIVALENCE = UNKNOWN
```
`model_v1.json` queda intacto. Tests del artefacto local siguen validando `1.0.0`. Fixtures contractuales online cambian `baseline-1.0.0` → `logreg-1.0.0`.

### RF-09 — androidTest
Corregir `DeviceSmartMatchTest.kt`, `DeviceUITestSuite.kt`, `DeviceAuthFlowTest.kt` (bloque `DeviceSmartMatchTest`) y `DeviceNavigationTest.kt` para usar la firma real de `SmartMatchScreen(factory, onRequireLogin, onConfigurePreferences, onPropertyClick, onFavoriteClick, onContactClick, modifier)` y nodos reales del Screen. No inventar estados ni nodos.

## 4. Criterios de aceptación

A–N según pedido: deserialización decimal de `73.5` y de los seis maxPoints; UI `73.5%`; barra `points/maxPoints`; `72.6` sin romper layout; `1.7` no desaparece; AUTH 200 y GUEST 200 sin re-score; network y 503 con etiqueta; 400/401/404/422/429 sin fallback; retry/reconnect reemplaza local; modelVersion remoto `logreg-1.0.0`; artefacto local sigue `1.0.0`.

Gates ejecutados y verdes + CI remoto verde antes de declarar `CODE-READY`.

`RUNTIME-CERTIFIED` requiere verificación en dispositivo (AUTH 200 decimal visible, GUEST 200 sin re-score, OFFLINE etiquetado, RECOVERY remoto reemplaza local). Si no hay ADB/dispositivo, se reporta parcialmente: `CODE-READY` con `RUNTIME-PENDING`.

## 5. Riesgos y seguridad

- **MASVS-NETWORK:** se consume HTTPS existente; no se loguean tokens ni PII. El logging DEBUG actual solo ruta relativa, status y duración (M-MOB-055).
- **MASVS-AUTH:** 401 → RequiresAuth sin fallback (evita mostrar datos de otra cuenta como "estimación").
- **Privacidad:** preferencias del usuario se envían al backend ya contratado; sin datos nuevos.
- **COST-001:** $0. Backend ya contratado y artefacto local ya versionado; sin proveedores nuevos, sin tarjeta, sin riesgo de cobro, alternativa gratuita = fallback local.
- **Riesgo de deuda:** equivalencia UNKNOWN Documentada; no se afirma que el modelo local sea el productivo.

## 6. Evidencia esperada

`specs/d16-smart-match-logreg-1.0.0-migration/evidence.md` con comandos reales, resultados, lista de archivos, tests A–N, gates, CI y estado runtime.
