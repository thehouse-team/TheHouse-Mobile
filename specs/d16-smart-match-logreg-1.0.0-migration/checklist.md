# CHECKLIST D16

## Gobierno
- [x] constitution.md / AGENTS.md / memoria.md leídas
- [x] Declaración SPEC/PLAN/TASK/ESTADO emitida antes de tocar código
- [x] Rama `fix/smart-match-logreg-1.0.0` creada desde `origin/main` @ `8d2f1a3`
- [x] Sin mezcla con D14/D15
- [x] COST-001 = $0

## Implementación
- [x] matchScore Double en DTO / dominio / mappers / UI / tests
- [x] Breakdown estructurado (factor,label,points,maxPoints)
- [x] Barras points/maxPoints clamp 0..1, sin /100
- [x] AUTH 200 sin re-score
- [x] GUEST 200 sin re-score
- [x] Fallback solo offline/network/503, etiquetado
- [x] 400/401/404/422/429 sin fallback
- [x] Reconexión reemplaza estimación local
- [x] Fixtures online logreg-1.0.0 + maxPoints decimales
- [x] Artefacto local 1.0.0 intacto
- [x] androidTest Smart Match corregidos (deuda ajena documentada)

## Tests A–N — 14/14 PASS
- [x] A–N (ver `evidence.md` §4)

## Gates
- [x] `clean compileDebugKotlin` — BUILD SUCCESSFUL
- [x] `testDebugUnitTest` — 452 tests / 0 failures / 0 errors (base rebaseada `28cc3e0`)
- [x] `lintDebug` — 0 errores
- [x] `assembleDebug` — BUILD SUCCESSFUL
- [x] `git diff --check` — exit 0
- [x] CI remoto verde — `Android quality gates` run 45860702839, success (7m17s)

## Cierre
- [x] PR separado #32 abierto, MERGEABLE, sin merge
- [ ] RUNTIME-CERTIFIED en dispositivo — PENDIENTE (sin ADB)
- [x] memoria.md actualizada (M-MOB-060)
