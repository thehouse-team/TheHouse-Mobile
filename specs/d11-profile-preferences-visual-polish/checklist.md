# D11 — CHECKLIST

## FASE A — Functional stabilization (histórica)
- [x] Rama/working tree preservados, spec registrada, trazabilidad/memoria actualizadas.
- [x] Restauración/correcciones D11 y validación: `compileDebugKotlin` + `testDebugUnitTest` en verde.

## FASE B — Visual Polish (histórica)
- [x] Design system, componentes, estados y pulido de pantallas previas validados (build/tests/lint/assemble).
- [ ] QA runtime con emulador: NO EJECUTADO en esa iteración.

## FASE C — Profile / Preferences / Quick Setup UI (2026-11-19)

- [x] Rama `feature/profile-preferences-ui` creada desde `feature/ui-redesign`; cambios locales preservados (`app/google-services.json`, `specs/nv-13-reviews/*` excluidos de commit).
- [x] Capas existentes revisadas: `ProfileRepository`/`PreferenceRepository` (domain+data), APIs/DTOs, use cases (`Get/UpdateProfile`, `SelectUserUniversity`, `Get/SavePreferences`, `GetQuickSetup`), `GetUniversitiesUseCase`, `SmartMatchViewModel`, `AppContainer`, `U2ViewModelFactory`, navegación y Design System.
- [x] Profile: pantalla real con datos soportados (nombre, correo de sesión, universidad, estado Quick Setup), edición de nombre, selección de universidad, Loading/Content/Error/Retry y mensajes seguros.
- [x] Preferences: pantalla real con carga/edición/guardado de campos soportados (`Preference`/`Budget`) usando use cases/repositorio existente; validación UI de números; estados y mensajes.
- [x] Quick Setup: pantalla con campos soportados (nombre, universidad) y acceso a preferencias; flag de completitud de solo lectura; sin forzar si ya completo.
- [x] Navegación: rutas `perfil`, `preferencias`, `configuracion_inicial`; accesos desde Home (cuenta), Perfil y Smart Match (NoPreferences → Preferencias); sin romper rutas existentes.
- [x] Tests añadidos: `ProfileViewModelTest`, `PreferencesViewModelTest`, `QuickSetupViewModelTest` (load/save/select/error/state).
- [x] Privacidad UI: sin JWT/tokens/IDs técnicos/datos sensibles innecesarios en pantalla o logs.
- [x] Gates ejecutados y registrados: `testDebugUnitTest`, `lintDebug`, `assembleDebug` en verde.
- [x] Runtime en `emulator-5554`: parcial (instalación/arranque OK, sin sesión autenticada/credenciales para flujos protegidos).
- [ ] `git diff --check`, commit `feat: add profile preferences and quick setup UI`, push `origin feature/profile-preferences-ui` (sin merge).
