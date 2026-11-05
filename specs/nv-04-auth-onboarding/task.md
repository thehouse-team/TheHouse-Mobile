# NV-04 — TASKS AUTH + ONBOARDING

> ## ⛔ NO EJECUTAR
>
> Tasks no autorizadas. Permanecen sin marcar. No implementar pantallas, endpoints, almacenamiento de tokens ni dependencias sin autorización humana explícita.

| Campo | Valor |
|---|---|
| Spec | NV-04 Auth + Onboarding |
| Plan | NV04-PLAN-1.0 |
| Estado | ⛔ NO EJECUTAR |
| Tasks completadas | 0 |
| Autorización | PENDIENTE |

## Reglas

- No marcar tasks sin evidencia real.
- No implementar alrededor de endpoints no confirmados: `/auth/refresh` y `/users/me` → tasks dependientes `BLOCKED-BACKEND`.
- Toda dependencia nueva debe ser compatible con Kotlin `2.2.10`; si no: `BLOCKED`.
- No versionar secretos ni registrar tokens/contraseñas en logs.
- No usar `EncryptedSharedPreferences`.
- Referencia visual limitada a los 5 archivos HTML autorizados: solo textos, jerarquía, orden, colores y composición.
- Si falta autorización: `BLOCKED`.

## Fase F1 — Domain y Data

- [x] NV04-T001 — Modelos de dominio de sesión/auth y contrato `ObserveSession`.
- [x] NV04-T002 — Casos de uso `Register`, `Login`, `Logout` (y `GetMe` solo cuando backend lo soporte).
- [x] NV04-T003 — DTO + API Retrofit para `/auth/register`, `/auth/login`, `/auth/logout`.
- [x] NV04-T004 — Errores Problem Details y mapeo DTO ↔ Domain.
- [x] NV04-T005 — `AuthRepository` (contrato Domain, implementación Data).
- [x] NV04-T006 — Caché HTTP OkHttp disk cache con `Cache-Control` y `ETag` (sin caching para auth).
- [x] NV04-T007 — configuración `API_BASE_URL` inyectable por BuildConfig; verificación de no hardcodeo.

## Fase F2 — Sesión y seguridad

- [x] NV04-T008 — Access token solo en memoria; token storage cifrado AES-GCM + Android Keystore (sin librerías nuevas; no `EncryptedSharedPreferences`).
- [ ] NV04-T009 — `BLOCKED-BACKEND`: diseño de renovación con OkHttp `Authenticator`, single-flight con `Mutex`, máximo una renovación concurrente, fallo → logout local. Implementar solo cuando exista `/auth/refresh` confirmado.
- [x] NV04-T010 — Logout seguro: backend + limpieza local siempre + borrado tokens + cancelación de trabajos; sin red → logout local inmediato.
- [x] NV04-T011 — DataStore `onboardingCompleted` por usuario (no secreto).
- [x] NV04-T012 — Manifest/red: `INTERNET`, cleartext deshabilitado, `network_security_config`, `data_extraction_rules`/backup rules excluyendo almacenamiento sensibles de sesión/token.

## Fase F3 — UI/UX

- [x] NV04-T013 — Navegación inicial según sesión (`Inicio` o `Login → Crear cuenta`) vía RootViewModel + Navigation Compose.
- [x] NV04-T014 — Pantalla Login (RF-02).
- [x] NV04-T015 — Pantalla Registro (RF-01); consentimiento de términos y aviso privacidad (enlace pendiente `BLOCKED-PRIVACY-URL`, sin URL inventada).
- [ ] NV04-T016 — `BLOCKED-BACKEND`: bienvenida personalizada vía `GET /api/v1/users/me`. Onboarding (3 pantallas + `Omitir`) y bienvenida con nombre real de login/register: implementados.
- [x] NV04-T017 — Inicio provisional: saludo + `Cerrar sesión`.
- [x] NV04-T018 — Accesibilidad transversal: táctiles >= 44dp, semantics/contentDescription, contraste.

## Fase F4 — Pruebas y calidad

- [x] NV04-T019 — Unitarias: ViewModels, validadores, UseCases con dispatchers controlados.
- [x] NV04-T020 — Integración MockWebServer3 (OkHttp 5): register, login, logout, errores Problem Details. Refresh: solo cuando backend lo soporte.
- [x] NV04-T021 — Seguridad: tokens ausentes de logs, cifrado activo, logout elimina sesión, backup excluye lo sensible, cleartext deshabilitado, sin secretos versionados.
- [x] NV04-T022 — UI Compose Test: `LoginScreenInstrumentedTest` creado (ejecución requiere emulador, fuera de JVM U2).
- [x] NV04-T023 — Validaciones base: `clean assembleDebug testDebugUnitTest lintDebug` verdes; primer intento `bundleRelease` (AAB sin firmar: `BLOCKED-SIGNING-ENV`); evidencia registrada; revisión final `sdd-guardian` solicitada.

> Adición U2 (hallazgo real): prueba Azure evidencia que el backend rechaza el payload de registro razonable (400 "Entrada inválida") → **`BLOCKED-REGISTER-CONTRACT`**: aceptación punta-a-punta de register pendiente del contrato exacto del body. No se inventaron contratos.

## Estado final

```text
PARCIALMENTE COMPLETA — BLOCKED-BACKEND en NV04-T009 y NV04-T016; BLOCKED-PRIVACY-URL; BLOCKED-REGISTER-CONTRACT; firma del AAB BLOCKED-SIGNING-ENV
```
