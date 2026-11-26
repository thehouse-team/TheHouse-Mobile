# TASKS D16 — Smart Match logreg-1.0.0 Migration

Estado general: **EJECUTADAS** (2026-11-26). Todas dentro de la spec D16 y el plan D16.
Evidencia: `evidence.md`.

## FASE A

- [x] **D16-T01** — `SmartMatchFactor`, `SmartMatchSource`, `SmartMatchResult.matchScore: Double` y `SmartMatchModelVersions`. `RunSmartMatchUseCase` produce score decimal y factores sin inventar breakdown.
- [x] **D16-T02** — DTO AUTH con `matchScore: Double`; DTO GUEST extendido con `breakdown` y `modelVersion`; mappers preservan Double y factores estructurados.
- [x] **D16-T03** — UI: `73.5%`, barras `points/maxPoints` clamp 0..1 sin /100; etiqueta "Estimación local".
- [x] **D16-T04** — Tests A–F.

## FASE B

- [x] **D16-T05** — AUTH: `getRecommendations` mapeado 1:1 en orden del Backend, sin re-score.
- [x] **D16-T06** — GUEST: respuesta 200 mapeada 1:1; eliminado re-score local.
- [x] **D16-T07** — Error mapping tipado: `ValidationError`(400), `Unauthorized`(401), `NotFound`(404), `NoPreferences`(422), `RateLimited`(429), `ServiceUnavailable`(503).
- [x] **D16-T08** — Tests G, K, M.

## FASE C

- [x] **D16-T09** — Fallback local etiquetado solo offline/red/timeout/503.
- [x] **D16-T10** — Reconexión con `ConnectivityObserver` existente; un solo score; sin polling.
- [x] **D16-T11** — Tests I, J, L.

## FASE D

- [x] **D16-T12** — Fixtures online `logreg-1.0.0` + maxPoints decimales; artefacto local `1.0.0` intacto; test N.
- [x] **D16-T13** — androidTest Smart Match corregidos a firma real y nodos existentes (deuda preexistente fuera de alcance documentada).
- [x] **D16-T14** — Documentación: spec/plan/task/checklist/evidence + `memoria.md` M-MOB-060.
- [x] **D16-T15** — Gates locales verdes; PR separado (CI remoto y runtime pendientes).
