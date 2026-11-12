# D11 - TASKS

| Campo | Valor |
|---|---|
| Spec | D11 Profile + Preferences + Quick Setup + Visual Polish |
| Estado | FASE A activa; FASE B NO EJECUTAR |

## FASE A — Functional stabilization (TASK ACTIVA)

- [x] D11-T01 - Gobernanza: rama feature con working tree preservado, sin pérdida (sin reset/restore/clean/stash).
- [x] D11-T02 - Registro de spec/plan/task/checklist/evidence D11 y actualización de trazabilidad.
- [x] D11-T03 - Commit de gobernanza solo documental (`docs: register d11 profile preferences and visual polish`).
- [x] D11-T04 - Clasificación del working tree (D11 legítimo / cambio ajeno / dudoso) sin descartar nada.
- [ ] D11-T05 - Ejecutar `.\gradlew.bat compileDebugKotlin` y registrar resultado real. **[EJECUTADO: FALLO — ver evidence.md]**
- [ ] D11-T06 - Resolver eliminaciones de 23 archivos requeridos por código sobreviviente (decisión humana pendiente).
- [ ] D11-T07 - Corregir errores de compilación del código D11 nuevo.
- [ ] D11-T08 - `testDebugUnitTest` y commit funcional `feat: add real profile preferences and quick setup`.

## FASE B — Visual Polish (NO EJECUTAR)

- [ ] D11-T09 - Design system centralizado.
- [ ] D11-T10 - Pulido visual por pantalla.
- [ ] D11-T11 - Componentes unificados y patrones Loading/Empty/Error.
- [ ] D11-T12 - Accesibilidad / responsive / QA runtime.
- [ ] D11-T13 - Validación completa y commit `style: polish mobile ui and unify design system`.
