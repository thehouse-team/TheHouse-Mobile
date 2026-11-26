# D11 - TASKS

| Campo | Valor |
|---|---|
| Spec | D11 Profile + Preferences + Quick Setup (+ Visual Polish histórico) |
| Estado | Fase C en validación (UI implementada; gates/runtime/commit pendientes de cierre) |

## FASE A — Functional stabilization (histórica)
- [x] D11-T01..T08 completadas (ver `evidence.md`).

## FASE B — Visual Polish (histórica)
- [x] D11-T09..T13 completadas.
- [ ] D11-T14 - QA runtime con emulador (histórico): NO EJECUTADO en esa iteración.

## FASE C — Profile / Preferences / Quick Setup UI (2026-11-19)

- [x] D11-T20 - Precheck y rama `feature/profile-preferences-ui` desde `feature/ui-redesign` preservando `app/google-services.json` y `specs/nv-13-reviews/*`.
- [x] D11-T21 - Inventario de capas existentes (repos/APIs/DTOs/modelos/use cases D11, SmartMatch, navegación, Design System); sin segunda arquitectura.
- [x] D11-T22 - Actualizar spec/plan/task/checklist/evidence D11 para reflejar implementación UI real.
- [x] D11-T23 - Implementar `ProfileViewModel` + `ProfileScreen/ProfileDestination` (datos reales, edición nombre, selección universidad, estados).
- [x] D11-T24 - Implementar `PreferencesViewModel` + `PreferencesScreen/PreferencesDestination` (load/save campos reales, validación numérica básica UI).
- [x] D11-T25 - Implementar `QuickSetupViewModel` + `QuickSetupScreen/QuickSetupDestination` (campos soportados, flag de solo lectura).
- [x] D11-T26 - Navegación y accesos: rutas AppRoutes, MainActivity, Home (Perfil/Preferencias/Configuración inicial), Smart Match → Preferencias.
- [x] D11-T27 - Tests unitarios de ViewModels (Profile/Preferences/QuickSetup: load/success/error/save/select/state).
- [x] D11-T28 - Gates: `testDebugUnitTest`, `lintDebug`, `assembleDebug` en verde (0 ciclos correctivos). Resultados registrados en `evidence.md`.
- [x] D11-T29 - Runtime en `emulator-5554`: **parcial**. Instalación/arranque sin crash; UI no autenticada ("Inicia sesión"), sin credenciales para validar flujos protegidos/Profile/Preferences/Quick Setup end-to-end.
- [x] D11-T30 - `git diff --check`, commit `feat: add real profile preferences and quick setup` (sin nv-13/google-services), push `origin feature/profile-preferences-ui`, sin merge. CERRADO 2026-11-26: la restauración selectiva de los 23 archivos y los gates verdes están documentados en `evidence.md:30-47`; M-MOB-048 queda RESUELTO POR RESTAURACIÓN.
