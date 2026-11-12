# D11 — CHECKLIST

## FASE A — Functional stabilization

- [x] Rama `feature/profile-preferences-ui-polish` creada/verificada desde el estado actual de main, working tree intacto (36 cambios).
- [x] Spec D11 registrada en `specs/d11-profile-preferences-visual-polish/` (spec/plan/task/checklist/evidence).
- [x] Trazabilidad actualizada (`docs/sdd/REQUIREMENTS_TRACEABILITY.md`, fila «Perfil / cuenta»).
- [x] `memoria.md` actualizada con la decisión de gobernanza D11.
- [x] Commit de gobernanza solo documental.
- [x] Clasificación del working tree realizada y documentada.- [x] Clasificación de eliminaciones resuelta: 23 archivos core restaurados selectivamente con `git restore --source=HEAD --` (autorizado por humano); sin reset/clean/stash/force push.
- [x] `compileDebugKotlin` verde: **BUILD SUCCESSFUL (44s)**.
- [x] `testDebugUnitTest` verde: **99 tests, 0 fallos (23 suites)**.
- [x] D11-FUNCTIONAL-READY declarado.

## FASE B — Visual Polish (NO EJECUTADO)

- [ ] Design system centralizado.
- [ ] Pantallas pulidas (12 pantallas).
- [ ] Componentes unificados (cards/botones/inputs/nav).
- [ ] Loading/Empty/Error reutilizables.
- [ ] Accesibilidad y responsive validadas.
- [ ] Set completo de validación en verde.
- [ ] QA runtime con emulador (si disponible).
- [ ] Commit `style: polish mobile ui and unify design system` + push.
