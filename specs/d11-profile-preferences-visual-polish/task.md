# D11 - TASKS

| Campo | Valor |
|---|---|
| Spec | D11 Profile + Preferences + Quick Setup + Visual Polish |
| Estado | FASE A DONE (compile + tests verdes); FASE B NO EJECUTAR |

## FASE A — Functional stabilization (TASK ACTIVA)

- [x] D11-T01 - Gobernanza: rama feature con working tree preservado, sin pérdida (sin reset/restore/clean/stash).
- [x] D11-T02 - Registro de spec/plan/task/checklist/evidence D11 y actualización de trazabilidad.
- [x] D11-T03 - Commit de gobernanza solo documental (`docs: register d11 profile preferences and visual polish`).
- [x] D11-T04 - Clasificación del working tree (D11 legítimo / cambio ajeno / dudoso) sin descartar nada.
- [x] D11-T05 - Ejecutar `.\gradlew.bat compileDebugKotlin` y registrar resultado real. **[SEGUNDA EJECUCIÓN: BUILD SUCCESSFUL]**
- [x] D11-T06 - Restaurar los 23 archivos core eliminados (git restore --source=HEAD selectivo, archivo por archivo, autorizado).
- [x] D11-T07 - Corregir errores de compilación del código D11 nuevo (ver evidence.md FASE A).
- [x] D11-T08 - `testDebugUnitTest` (99 tests, 0 fallos) y commit funcional `feat: add real profile preferences and quick setup`.

## FASE B — Visual Polish (NO EJECUTAR)

- [ ] D11-T09 - Design system centralizado.
- [ ] D11-T10 - Pulido visual por pantalla.
- [ ] D11-T11 - Componentes unificados y patrones Loading/Empty/Error.
- [ ] D11-T12 - Accesibilidad / responsive / QA runtime.
- [ ] D11-T13 - Validación completa y commit `style: polish mobile ui and unify design system`.
