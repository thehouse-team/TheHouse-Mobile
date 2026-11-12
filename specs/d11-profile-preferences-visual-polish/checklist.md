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

## FASE B — Visual Polish

- [x] Design system centralizado: ColorScheme + Typography + Shapes extraídos del prototipo autoritativo (sin paleta nueva externa), tokens `Spacing`/`Heights`/`Elevations` en `ui/theme/Dimens.kt`; `dynamicColor` desactivado por fidelidad.
- [x] Pantallas pulidas: Onboarding (dots, CTA 56dp, jerarquía prototipo), Login/Register (botones 56dp, loading en CTA, error accesible), Explore (títulos/estados), Favorites (estados), PropertyCard (jerarquía de precio).
- [x] Componentes unificados: `StateViews.kt` (LoadingView / EmptyStateView / ErrorStateView) reutilizables y stateless.
- [x] States: Loading/Empty/Error con mensajes humanos, sin códigos HTTP/DomainError/stack traces; CTA en Empty, retry en Error.
- [x] Accesibilidad: semantics/contentDescription en estados y errores, targets ≥48dp, contraste por tokens; responsive con `fillMaxWidth`/`weight` (sin tamaños rígidos).
- [x] Validación: `compileDebugKotlin` verde → `testDebugUnitTest lintDebug assembleDebug` **BUILD SUCCESSFUL** (1m 46s).
- [ ] QA runtime con emulador: NO EJECUTADO (sin emulador/adb en el entorno). Pendiente si se requiere cierre total.
- [x] Commit `style: polish mobile ui and unify design system` + push a origin.
