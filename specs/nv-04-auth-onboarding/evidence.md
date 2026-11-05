# NV-04 — EVIDENCIA DE EJECUCIÓN U2

| Campo | Valor |
|---|---|
| Spec | NV-04 Auth + Onboarding v1.0.0 |
| Fase | U2 (ramas: `feature/u2-auth-onboarding`) |
| Fecha | 2026-11-04 |
| Autorización humana | Victor Manuel Jose Jose (FASE B) |

## 1. Comandos ejecutados

```powershell
.\gradlew.bat clean assembleDebug testDebugUnitTest lintDebug   # BUILD SUCCESSFUL (55/55 tasks, 2 ejecuciones)
# y de forma incremental: assembleDebug (BUILD SUCCESSFUL), testDebugUnitTest + lintDebug (BUILD SUCCESSFUL, 36 tests, 0 fallos)
```

Validación final limpia: `BUILD SUCCESSFUL in 1m 23s`; `assembleDebug`, `testDebugUnitTest` y `lintDebug` verdes.

## 2. Implementación

- Manifest: `INTERNET` (único permiso), `usesCleartextTraffic=false`, `network_security_config` restringido al dominio del backend, `backup_rules.xml`/`data_extraction_rules.xml` excluyen `datastore/` y sharedprefs (sesión/tokens fuera de backup).
- Configuración: `API_BASE_URL` real en `defaultConfig` (única fuente BuildConfig; sin duplicar en Kotlin).
- Domain: `UserSession`/`SessionState`, `AuthRepository` (register/login/logout/ObserveSession), `OnboardingRepository`, use cases `Register/Login/Logout/ObserveSession`, `AuthValidators` (mensajes en español), `DomainException`.
- Data: `AuthApi` (solo endpoints confirmados; `Cache-Control: no-store`), DTOs tolerantes + ProblemDetails, `AuthRepositoryImpl` (access token en memoria, refresh cifrado AES-GCM + Keystore en `EncryptedRefreshTokenStorage`, no `EncryptedSharedPreferences`), `OnboardingPreferencesRepository`, caché HTTP OkHttp (10 MiB, respeta `Cache-Control`/`ETag`).
- Presentation: `AppRoutes`; pantallas Login (RF-02), Crear cuenta (RF-01), Onboarding 3 pantallas + `Omitir`, Bienvenida, Inicio provisional con `Cerrar sesión`; RootViewModel decide inicio por sesión + `onboardingCompleted` (DataStore por usuario); accesibilidad: targets ≥44dp, contentDescription, errores accesibles, toggle mostrar/ocultar contraseña.
- Problemas: logout SIEMPRE limpia local; 401 sin `/auth/refresh` → sesión limpia → Login (sin sesión "siempre viva").

## 3. Bloqueos vigentes

```text
NV04-T009  BLOCKED-BACKEND  (/auth/refresh NO confirmado → Authenticator/Mutex sin implementar)
NV04-T016  BLOCKED-BACKEND  (/users/me NO confirmado → bienvenida usa nombre real de login/register)
BLOCKED-PRIVACY-URL         (URL_AVISO_PRIVACIDAD pendiente humano; UI sin enlace inventado)
BLOCKED-REGISTER-CONTRACT   (ver prueba Azure: 400 en register para payload razonable)
```

## 4. Pruebas

- Unitarias: validadores, Login/Register/Home-Logout ViewModels con dispatcher controlado (`TestMainDispatcherRule`).
- Integración (MockWebServer3/OkHttp 5): register/login/logout, Problem Details 409→Conflict, 401 limpia sesión, logout limpia sin backend, `Cache-Control no-store` en auth.
- Seguridad: tokens nunca en logs (`SafeHttpLoggingInterceptor` solo método/host/status/duración), refresh cifrado Keystore, backups excluyen tokens, cleartext deshabilitado, sin secretos versionados.
- UI: `LoginScreenInstrumentedTest` creado (requiere emulador; fuera de la JVM U2).

## 5. Prueba real contra Azure (2026-11-04)

```text
GET  /health/live                → 200 {"status":"Healthy"}
POST /api/v1/auth/login (creds inválidas) → 401 (endpoint reactivo; contrato funcional)
POST /api/v1/auth/logout (sin sesión)     → 400 (requiere sesión; endpoint existente)
POST /api/v1/auth/register       → 400 {"title":"Validation failed","detail":"The registration request could not be completed.","errors":{"request":["Entrada inválida."]}}
                                   para los payloads {name,email,password,acceptedTerms,acceptedPrivacy}, camelCase+confirmPassword y PascalCase.
```

Conclusión: `register` no acepta el cuerpo de registro que la app envía. **No se inventa contrato**: se requieren los nombres/validaciones exactos del DTO de registro (dependencia backend.). Login/logout con sesión no se pueden validar punta a punta hasta desbloquear register.

## 6. Pendientes humanos

- Contrato exacto del body de `POST /api/v1/auth/register` (BLOCKED-REGISTER-CONTRACT).
- `URL_AVISO_PRIVACIDAD`.
- Fidelidad visual: los HTML de la maqueta NO estuvieron accesibles en la sesión (`C:\Users\vicko\...` inexistente en esta máquina); el diseño U2 es Material 3 neutro. Requiere revisión humana contra la maqueta.
- Re-firmar AAB (ver evidencia NV-03).
