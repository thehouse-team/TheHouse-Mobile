# NV-04 — CHECKLIST

Fuente: `evidence.md` y `task.md` NV-04 (U2; register 201 {email,password}, login 200, logout 204; tests 41/41 BUILD SUCCESSFUL según encargo).

- [x] NV04-T001 — Modelos de sesion / ObserveSession
- [x] NV04-T002 — Use cases Register/Login/Logout
- [x] NV04-T003 — DTO/API register/login/logout
- [x] NV04-T004 — Errores Problem Details y mapeo
- [x] NV04-T005 — AuthRepository (Domain/Data)
- [x] NV04-T006 — Cache HTTP (sin caching en auth)
- [x] NV04-T007 — API_BASE_URL en BuildConfig
- [x] NV04-T008 — Token en memoria + refresh cifrado Keystore (AES-GCM)
- [ ] NV04-T009 — BLOCKED-BACKEND: renovacion con `/auth/refresh` (sin contrato, no implementar)
- [x] NV04-T010 — Logout seguro con limpieza local siempre
- [x] NV04-T011 — DataStore onboardingCompleted por usuario
- [x] NV04-T012 — Manifest/red y backup rules
- [x] NV04-T013 — Navegacion inicial segun sesion
- [x] NV04-T014 — Pantalla Login (RF-02)
- [x] NV04-T015 — Pantalla Registro (RF-01)
- [ ] NV04-T016 — BLOCKED-BACKEND: `GET /users/me` (sin contrato); onboarding/bienvenida con datos de login/register ya hechos
- [x] NV04-T017 — Inicio provisional + Cerrar sesion
- [x] NV04-T018 — Accesibilidad transversal (>=44dp, semantics)
- [x] NV04-T019 — Unitarias (ViewModels/validadores/UseCases)
- [x] NV04-T020 — Integracion MockWebServer3
- [x] NV04-T021 — Seguridad (logs, cifrado, backups, cleartext)
- [x] NV04-T022 — UI test LoginScreen (ejecucion requiere emulador)
- [x] NV04-T023 — Validaciones base verdes y evidencia (incl. re-firma AAB en NV-03)
- [ ] BLOCKED-PRIVACY-URL — URL_AVISO_PRIVACIDAD pendiente humano (sin enlace inventado)
