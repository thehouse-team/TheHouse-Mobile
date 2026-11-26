# PLAN D16 — Smart Match logreg-1.0.0 Migration

| Fase | Objetivo | Verificación |
|---|---|---|
| A | Contrato decimal y breakdown estructurado | Tests A–F |
| B | Backend authority AUTH/GUEST + error mapping | Tests G, K, M |
| C | Fallback etiquetado + reconexión | Tests I, J, L |
| D | Fixtures online + androidTest reales + docs | Tests H, N; gates; CI |

Nota: el orden de ejecución es A → B → C → D. Ninguna fase se marca DONE sin evidencia real.

## FASE A — Tipos y breakdown

- A1 `SmartMatchModels.kt`: `SmartMatchResult.matchScore: Double`, `factors: List<SmartMatchFactor>`, `source: SmartMatchSource`, `modelVersion: String`.
- A2 DTO: `SmartMatchRecommendationItemDto`/`SmartMatchRecommendationDto` ya tienen `matchScore: Double?`; añadir `breakdown: List<SmartMatchBreakdownItem>` al item de recomendación (compatibilidad con contrato NV-23).
- A3 Mappers: DTO → dominio preservando Double y breakdown; `SmartMatchScoreItem` → dominio.
- A4 UI: `SmartMatchScreen` muestra `%.1f%%`; `SmartMatchFactorBar` con `progress = points/maxPoints`, clamp, sin máximo 100; etiqueta "Estimación local".
- A5 Tests A–F.

## FASE B — Backend authority

- B1 AUTH (`getRecommendations`): mapear items del Backend en orden, con score y breakdown, sin `runSmartMatch`.
- B2 GUEST (`queryRecommendations` 200): mapear items del Backend, sin `fetchCandidates()` ni re-score.
- B3 Error mapping: 400/401/404/422/429 → errores tipados; 503/red/timeout → señal de fallback.
- B4 Tests G, K, M.

## FASE C — Fallback y reconexión

- C1 `SmartMatchUiState.Content` incluye `source: SmartMatchSource`.
- C2 `load()` captura `fallbackReason` y calcula estimación local solo en casos permitidos.
- C3 `LaunchedEffect` observa `connectivityObserver.isOnline` (inyectado en ViewModel y factory); al pasar a online con estimación local activa → `load()`.
- C4 Retry manual desde `ErrorStateView` ya existente; al reintentar con éxito, reemplaza estado.
- C5 Tests I, J, L.

## FASE D — Fixtures, androidTest, documentación y gates

- D1 Fixtures contractuales online: `modelVersion=logreg-1.0.0`, maxPoints 9.6/72.6/6.7/4.9/1.7/4.5. NO tocar tests que validan artefacto local `1.0.0`.
- D2 androidTest reales: firma correcta de `SmartMatchScreen`, callbacks, nodos existentes.
- D3 `memoria.md`: nueva entrada M-MOB-060 con SERVER_MODEL/LOCAL_MODEL/EQUIVALENCE.
- D4 Gates: `clean compileDebugKotlin`, `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `git diff --check`, CI remoto.
- D5 PR separado, sin merge; runtime pendiente si no hay dispositivo.

## Matriz de trazabilidad

| Requisito | Fase | Test |
|---|---|---|
| matchScore decimal | A | A, C |
| breakdown decimal | A | B, D, E, F |
| UI 73.5% | A | C |
| barra points/maxPoints | A | D |
| maxPoints 72.6 / 1.7 | A | E, F |
| AUTH sin re-score | B | G |
| GUEST sin re-score | B | H |
| network/503 etiquetado | C | I, J |
| 400/401/404/422/429 sin fallback | B | K |
| retry/reconnect | C | L |
| modelVersion remoto | B/D | M |
| artefacto local intacto | D | N |
| androidTest reales | D | compileDebugKotlin + revisión |
