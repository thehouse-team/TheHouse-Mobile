# NV-04 — PLAN AUTH + ONBOARDING

> ## ⛔ NO EJECUTAR
>
> Plan no autorizado. Ninguna task se ejecuta sin autorización humana explícita.

| Campo | Valor |
|---|---|
| Spec | NV-04 Auth + Onboarding |
| Plan ID | NV04-PLAN-1.0 |
| Fecha | 2026-11-04 |
| Alcance | U2 |
| Estado | ⛔ NO EJECUTAR |
| Autorización | PENDIENTE |

## 1. Estrategia

Construcción bottom-up sobre la foundation de NV-01: primero contratos de Domain y Data (conectados al backend confirmado), después sesión segura, luego UI/UX con UDF y accesibilidad, y finalmente pruebas. Las tasks dependientes de endpoints no confirmados quedan `BLOCKED-BACKEND` y no se simulan con implementación ficticia.

## 2. Fases

### Fase F1 — Domain y Data de auth

- Modelos de dominio de sesión/auth.
- Casos de uso: `Register`, `Login`, `Logout`, `ObserveSession`; `GetMe` solo cuando el backend lo soporte (`BLOCKED-BACKEND`).
- DTO + API Retrofit para `register`, `login`, `logout`.
- `AuthRepository` (contrato Domain, implementación Data) con manejo de errores Problem Details.
- Caché HTTP OkHttp disk cache (`Cache-Control`, `ETag`); no aplicar caching a auth.
- `API_BASE_URL` configurable desde `BuildConfig` (URL backend conocida; no hardcodear en clases Kotlin).

### Fase F2 — Sesión y seguridad

- Access token solo en memoria.
- Token storage cifrado: AES-GCM + Android Keystore (implementación directa); compatibilidad Kotlin 2.2.10 verificada antes de agregar librerías.
- `ObserveSession` y persistencia de sesión entre aperturas.
- DataStore: `onboardingCompleted` por usuario (dato no sensible).
- Renovación automática (diseño OkHttp `Authenticator`, single-flight con `Mutex`): implementación `BLOCKED-BACKEND` hasta `/auth/refresh`.
- Logout seguro: backend + limpieza local siempre + borrado tokens + cancelación de trabajos; sin red → logout local inmediato.
- Manifest/red: `INTERNET`, cleartext deshabilitado, `network_security_config`, `data_extraction_rules` excluyendo almacenamiento sensibles de sesión/token.

### Fase F3 — UI/UX (Compose, MVVM + UDF)

- `AppRoute` y navegación: inicio → login/create account → onboarding → inicio provisional.
- Registro (RF-01): campos, validadores, consentimientos, enlace aviso privacidad `<URL_AVISO_PRIVACIDAD>`.
- Login (RF-02); Google Sign-In: `Posterior`.
- Onboarding: 3 pantallas + `Omitir` + bienvenida personalizada (identificación de usuario `BLOCKED-BACKEND` si falta `/users/me`).
- Inicio provisional: saludo + `Cerrar sesión`.
- Fidelidad visual a maqueta (solo textos, jerarquía, orden, colores, composición de los 5 archivos HTML de referencia).
- Accesibilidad: táctiles >= 44dp, semantics/contentDescription, contraste.

### Fase F4 — Pruebas y calidad

- Unitarias: ViewModels (dispatchers controlados), validadores, UseCases.
- Integración: MockWebServer compatible con OkHttp 5 — register, login, logout, errores Problem Details; refresh solo cuando el backend lo soporte (`BLOCKED-BACKEND`).
- Seguridad: tokens fuera de logs, cifrado activo, logout elimina sesión, backup excluye lo sensible, cleartext deshabilitado.
- UI: Compose Test Login; Register opcional.
- Validaciones base: `assembleDebug`, `testDebugUnitTest`, `lintDebug` verdes.

## 3. Matriz completa

| Task | Fase | Requisito | Dependencia | Estado |
|---|---|---|---|---|
| NV04-T001..T002 | F1 | NV04-REQ-007 | NV-01 | Pendiente |
| NV04-T003..T004 | F1 | NV04-REQ-002..003 | NV04-T001 | Pendiente |
| NV04-T005 | F1 | NV04-REQ-007 | NV04-T003 | Pendiente |
| NV04-T006..T007 | F1 | NV04-REQ-007 | NV04-T005 | Pendiente |
| NV04-T008, T010..T012 | F2 | NV04-REQ-006, 011 | F1 | Pendiente |
| NV04-T009 | F2 | NV04-REQ-006 | `/auth/refresh` | `BLOCKED-BACKEND` |
| NV04-T013..T015, T017..T018 | F3 | NV04-REQ-001..003, 005, 008..009 | F2 | Pendiente |
| NV04-T016 | F3 | NV04-REQ-004 | `/users/me` | `BLOCKED-BACKEND` (bienvenida personalizada) |
| NV04-T019..T022 | F4 | NV04-REQ-010 | F1..F3 | Pendiente |
| NV04-T023 | F4 | NV04-REQ-010 | All | Pendiente |

## 4. Dependencias

- Externas: backend real y acceso a red; contrato `/auth/refresh` y `/users/me` para las tasks marcadas.
- De specs: NV-01 (foundation); NV-03 para el AAB firmado que aparece en los criterios de aceptación U2.

## 5. Riesgos y mitigaciones

- Endpoints no confirmados → tasks `BLOCKED-BACKEND`, sin diseño ficticio.
- Incompatibilidad de dependencias con Kotlin 2.2.10 → verificación previa y `BLOCKED` si no hay alternativa.
- Alcance excesivo de UI tests → Register opcional.

## 6. Checklist de salida

- [ ] Tasks F1..F3 ejecutadas con autorización y evidencia.
- [ ] Tasks `BLOCKED-BACKEND` resueltas por contrato real o declaradas pendientes sin falsificar.
- [ ] Validaciones base verdes.
- [ ] AAB firmado disponible (con NV-03).
- [ ] `sdd-guardian` PASS.

## 7. Estado

```text
⛔ NO EJECUTAR — requiere autorización humana explícita
```
