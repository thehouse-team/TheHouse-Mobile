# NV-15 — EVIDENCIA

## FASE D3 — Implementación Mobile (2026-11-17, branch `feature/privacy-delete-account`)

### Resultado

`PRIVACY-DELETE-PARTIAL: DELETE-ACCOUNT-BACKEND-BLOCKED + BLOCKED-PRIVACY-URL`

Mobile queda implementado y validado en build/tests/lint. La eliminación remota y el enlace real al aviso no se pueden cerrar porque los contratos externos no existen.

### Contratos externos verificados (sin inventar)

**1. Eliminación de cuenta — BLOCKED**

```text
git -C C:\THEHOUSE\TheHouse-backend log -1 --oneline origin/main
9dfb497 Merge pull request #37 from thehouse-team/docs/nv-16-close

git -C C:\THEHOUSE\TheHouse-backend grep -n "HttpDelete\|DELETE /api" origin/main -- "*.cs" "*.md"
(sin coincidencias, exit 1)
```

- `origin/main` no define `DELETE /api/v1/users/me` ni una ruta equivalente.
- `specs/nv-10-account-session/spec.md:23` difiere explícitamente la eliminación de cuenta a una versión posterior.
- Decisión: `DELETE-ACCOUNT-BACKEND-BLOCKED`. No se creó ruta, DTO, body, status ni query.
- `AccountRepositoryImpl.isDeletionAvailable = false`; la UI muestra el bloqueo y no abre una confirmación falsa.

**2. URL del aviso de privacidad — BLOCKED**

```text
Invoke-WebRequest https://lively-llama-5185d1.netlify.app
status=200 bytes=74995
has_id_privacidad=False
has_id_terminos=False
href_privacidad=True
```

- La landing responde 200, pero solo contiene anclas `href="#privacidad"` sin secciones reales (`id="privacidad"` / `id="terminos"` no existen).
- Decisión: `BLOCKED-PRIVACY-URL`. No se inventó URL ni contenido legal.
- `BuildConfig.PRIVACY_POLICY_URL = ""`; `PrivacyPolicyUrl.normalize` devuelve null; la app informa el bloqueo sin crash.

### Implementación

| Área | Archivos |
|---|---|
| Dominio | `domain/privacy/PrivacyPolicyUrl.kt`, `domain/repository/AccountRepository.kt`, `domain/repository/LocalUserDataPurger.kt`, `domain/auth/LocalSessionCleaner.kt`, `domain/usecase/DeleteAccountUseCase.kt` |
| Datos | `data/account/repository/AccountRepositoryImpl.kt`, `data/account/repository/LocalUserDataPurgerImpl.kt`, `AuthRepositoryImpl.clearLocalSession`, `OnboardingPreferencesRepository.clear`, `FavoriteIdsStore.clear` |
| Presentación | `presentation/privacy/PrivacyViewModel.kt`, `presentation/privacy/PrivacyScreen.kt`, `AppRoutes.privacy`, entrada "Privacidad" en Home, navegación post-eliminación a Login con `popUpTo(0)` |
| Wiring | `AppContainer`, `U2ViewModelFactory`, `MainActivity`, `RootViewModel.onAccountDeleted`, `AppConfig`, `AppConfigProvider`, `app/build.gradle.kts` |

Flujo UDF: UI → `PrivacyEvent` → `PrivacyViewModel` → `DeleteAccountUseCase` → `AccountRepository` + `LocalUserDataPurger` → `PrivacyEffect`.

Reglas aplicadas:
- Confirmación obligatoria; cancelar no llama al Backend.
- `isDeleting` + guard bloquean doble submit.
- Solo un `Deleted` real emite `AccountDeleted`; `BackendUnavailable` y `Failed` conservan la sesión.
- Errores mapeados a mensajes seguros; sin body crudo, stack traces ni secretos en logs.

### Limpieza local posterior al éxito

Orden: sesión primero, luego datos acotados al usuario.

| Dato | Acción |
|---|---|
| Access token en memoria / refresh token cifrado / SessionState | Limpiados (`AuthRepositoryImpl.clearLocalSession`) |
| Onboarding del usuario eliminado | Borrada solo su clave (`onboardingRepository.clear`) |
| Favoritos en memoria | `FavoriteIdsStore.clear` |
| Target pendiente de push | `PendingNotificationNavigation.clear` |
| Notificaciones locales | `LocalPushNotifier.cancelAll` |
| Caché HTTP privada | `Cache.evictAll` |

No se borran: selección global de universidad, imágenes públicas de Coil, preferencias de dispositivo ni datos de otros usuarios.

### Tests

| Suite | Tests | Resultado |
|---|---:|---|
| `PrivacyPolicyUrlTest` | 4 | PASS |
| `DeleteAccountUseCaseTest` | 6 | PASS |
| `LocalUserDataPurgerTest` | 3 | PASS |
| `LocalSessionClearTest` | 2 | PASS |
| `PrivacyViewModelTest` | 11 | PASS |
| **Total del proyecto** | **323** | **0 fallos, 0 omitidos** |

Cobertura de criterios: URL válida/ inválida/ ausente, confirmación obligatoria, cancelar sin request, un solo request con loading, éxito limpia sesión y navega, reinicio sin restore, fallo de red conserva sesión/datos, contrato no disponible no borra nada, errores seguros.

### Comandos y resultados reales

```text
.\gradlew.bat testDebugUnitTest
BUILD SUCCESSFUL in 18s
51 suites, 323 tests, 0 failures, 0 skipped

.\gradlew.bat lintDebug
BUILD SUCCESSFUL in 1m 59s
0 errors, 20 warnings (ninguno en presentation/privacy)

.\gradlew.bat assembleDebug
BUILD SUCCESSFUL in 8s
```

### Pendiente / bloqueado

- Eliminación remota real: `DELETE-ACCOUNT-BACKEND-BLOCKED` (requiere contrato NV-27).
- URL publicada del aviso: `BLOCKED-PRIVACY-URL` (requiere URL real; configurar `PRIVACY_POLICY_URL`).
- Validación runtime en dispositivo/emulador: **NO EJECUTADA** (sin evidencia ADB en esta sesión).
- Al existir contrato/URL, reemplazar `AccountRepositoryImpl` bloqueado y añadir tests de integración HTTP del endpoint real.

---

## FASE D2 — Auditoría privacidad (2026-11-06, branch feature/privacy-landing-integration)

> Registro histórico de D2. El estado vigente es el de la Fase D3 anterior.

### Verificación URL_AVISO_PRIVACIDAD: NO VÁLIDA → BLOCKED-PRIVACY-URL se mantiene
- Landing auditada: https://lively-llama-5185d1.netlify.app (HTTP 200).
- Footer contiene solo anclas `#privacidad` y `#terminos` SIN secciones/contenido real (no existen `id="privacidad"`/`id="terminos"` en el HTML).
- No es HTTPS pública estable con contenido real apto para Google Play; es redirección interna rota. NO se inventó URL.
- Mobile: NO se enlazó “Aviso de privacidad” (sin placeholder, sin cambio funcional falso, sin WebView).

### ACTUAL vs PLANNED (sin inventar tratamientos)
| Área | ACTUAL (evidencia real) | PLANNED / BLOCKED |
|---|---|---|
| Auth | register 201, login 200, logout 204 (NV-04) | refresh/users/me BLOCKED (NV-27) |
| Perfil | Sin perfil funcional (BLOCKED) | NV-27 pendiente |
| Favoritos/visitas/chat | No implementados | NV-09/NV-10/NV-11 PLANNED |
| Almacenamiento local | DataStore prefs (onboarding) + refresh token cifrado AES-GCM/Keystore excluido de backup (NV-04) | Sin base de datos de negocio |
| Tokens | Access solo memoria; refresh cifrado | Sin renovación automática (BLOCKED) |
| Analytics | No implementado | NV-23 PLANNED |
| Push | No implementado (FCM sin configurar) | NV-14 PLANNED |
| ML/IA | No implementado | NV-17 PLANNED |
| Delete account | NO implementado | NV-15-T03/T04 PLANNED; contrato BLOCKED |
| Retención/eliminación | Solo limpieza por logout (204) | Sin política de retención/TAFLIGHT |
| Contacto | No publicado | Sin contacto real en landing |
| Data Safety Play | Sin declaraciones completas | NV-03 BLOCKED-PRIVACY-URL; sin URL no se declara |

### Hechos reales preservados
- register 201 `{email,password}`, login 200, logout 204, tests 41/41, AAB signed `jar verified.`, versionName 1.0.0 / versionCode 10000.
- BLOCKED: `/auth/refresh`, `/users/me`, BLOCKED-PRIVACY-URL (se mantiene).
