# D11 — PLAN

| Campo | Valor |
|---|---|
| Spec | D11 |
| Estado | FASE A: EN VALIDACIÓN / FASE B: PLANNED |
| Autorización | APROBADA por instrucción humana (gobernanza D11, 2026-11-12) |

## FASE A — Functional stabilization (TASK ACTIVA)

Formalizar y validar el trabajo funcional ya existente. NO ejecutar Visual Polish aún.

- A1 — Recuperación de gobernanza: rama `feature/profile-preferences-ui-polish` desde main con working tree preservado; specs/plan/task/checklist/evidence registrados; commit de gobernanza solo documental. **[COMPLETADO]**
- A2 — Clasificación del working tree: D11 legítimo / cambio ajeno / dudoso (ver `checklist.md`), sin descartar nada automáticamente.
- A3 — Resolución de archivos borrados que el código sobreviviente aún requiere (23 archivos) — requiere decisión humana si se confirma que las eliminaciones no son parte de D11.
- A4 — Corrección de errores de compilación del código D11 nuevo (`profile interface`, `SelectUniversityUseCase2`, `selectUniversity` duplicado).
- A5 — Validación: `.\gradlew.bat compileDebugKotlin`, luego `.\gradlew.bat testDebugUnitTest`.
- A6 — Commit separado del trabajo funcional: `feat: add real profile preferences and quick setup`.
- A7 — Estado D11-FUNCTIONAL-READY solo si A5 está en verde; `BLOCKED` nunca equivale a `DONE`.

## FASE B — Visual Polish (NO EJECUTAR sin autorización posterior)

- B1 — Design system: colores, typography, shapes, spacing, elevations, card/button/input styles reutilizables.
- B2 — Pantallas: Onboarding, Login/Register, Home, Explore/Search, Map (sin SDK nuevo), Property Detail, Favorites, Visits, Profile, Preferences, Quick Setup.
- B3 — Componentes unificados: cards, botones, inputs, TopBar/BottomNav.
- B4 — Patrones Loading/Empty/Error reutilizables (sin códigos técnicos).
- B5 — Accesibilidad (targets ≥48dp, contentDescription, contraste), responsive, animaciones sutiles.
- B6 — Validación: `compileDebugKotlin`, luego `testDebugUnitTest lintDebug assembleDebug` (máx. 1 ciclo correctivo).
- B7 — Runtime visual QA con emulador si está disponible.
- B8 — Commit: `style: polish mobile ui and unify design system`; push a `origin/feature/profile-preferences-ui-polish`.
