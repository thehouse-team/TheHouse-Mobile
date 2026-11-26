# EVIDENCIA D19 — Account + UX Core

| Campo | Valor |
|---|---|
| Spec | D19-Account-UX-Core |
| Branch | `fix/account-ux-core` desde `origin/main` @ `1ab64eb7dbf7d3ed4fa4920038b2a7e04dcfb881` |
| Backend source of truth | `origin/main` @ `780b7be88258f44a94bf966057eff552a82f35af` |
| Fecha | 2026-11-26 |

## Gates ejecutados (resultados reales)

| Comando | Resultado |
|---|---|
| `.\gradlew.bat clean compileDebugKotlin --offline` | **BUILD SUCCESSFUL** |
| `.\gradlew.bat testDebugUnitTest --offline` | **BUILD SUCCESSFUL — 501 tests / 0 failures / 0 errors** (75 clases) |
| `.\gradlew.bat lintDebug --offline` | **BUILD SUCCESSFUL** (0 errores; reporte `app/build/reports/lint-results-debug.html`) |
| `.\gradlew.bat assembleDebug --offline` | **BUILD SUCCESSFUL** (APK generado) |
| `git diff --check` | **exit 0** (solo avisos CRLF, sin errores de whitespace) |

El build final se ejecutó como `clean compileDebugKotlin testDebugUnitTest assembleDebug`: **BUILD SUCCESSFUL, 47/47 tareas ejecutadas**.

## Cobertura de los 30 requisitos (checklist.md)

- **Sesión (1-4):** `D19SessionBearerTest` — 2 tests: (a) sesión activa sin access token → 1 refresh + retry con Bearer → 200; (b) refresh inválido → 401 + `SessionState.Anonymous`. El refresher es single-flight (monitor JVM) y el authenticator conserva `priorResponse != null` y `noRetryPaths`: **0 loops**.
- **Profile (5-8):** `DomainError.Unauthorized` → `ProfileEffect.RequireLogin` (nuevo effect cableado en `ProfileDestination`/`MainActivity`); DTOs验证ados por serialización exacta `{name}` y `{universityId}`.
- **Preferences (9-11):** contrato intacto (8 campos, sin `version`/`quickSetupCompleted`/`userId`); `PreferencesViewModel` ya mapea 401 → `requiresAuth` (verificado).
- **Header (12-14):** `TheHouseRoot.topBar` con `BrandMark()` + `Arrangement.SpaceBetween` + `windowInsetsPadding(WindowInsets.statusBars)` + un único `home_menu_button`; drawer sin hamburger local.
- **Drawer (15-18):** guest 2 items ("Aviso de privacidad"); auth 13 items (7 cuenta + 4 arrendador + privacidad + logout); todos los tags con ruta real.
- **Registro/Legal (19-23):** `RegisterRequestDto` serializa exactamente `{email,password}`; link "Aviso de privacidad" → `AppRoutes.privacy`; `GET /legal` con URLs null → estado seguro; `PUT /users/me/legal` con versiones string vigentes; 409 `legal_version_outdated` → 1 sola recarga (sin loop).
- **Delete (24-30):** preview con blockers/effects; password + confirmación "ELIMINAR" case-sensitive; 403/409/429/503 mapeados; 204 → purga local + estado limpio; navegación con `popUpTo(0)` en `MainActivity`.

Tests nuevos: `D19SessionBearerTest.kt` (2), `D19AccountUxCoreTest.kt` (15).

## Archivos modificados principales

- `data/remote/auth/TokenRefresher.kt` (nuevo), `BearerTokenInterceptor.kt`, `RefreshAuthenticator.kt`, `di/AppContainer.kt`.
- `data/remote/api/LegalApi.kt`, `AccountApi.kt` (nuevos), DTOs `legal/`, `account/`.
- `domain/repository/LegalRepository.kt`, `AccountRepository.kt`, `domain/usecase/DeleteAccountUseCase.kt`, modelos/error de dominio.
- `presentation/privacy/PrivacyScreen.kt`, `PrivacyViewModel.kt`, `presentation/profile/ProfileViewModel.kt`/`Screen.kt`, `RegisterScreen.kt`, `DrawerItemCatalog.kt`, `MainActivity.kt`.
- Tests: 2 nuevos + ajustes en `DrawerNavigationCatalogTest` y `PrivacyViewModelTest` por el nuevo flujo D19.

## Riesgos / deuda

- **RUNTIME PENDIENTE:** no hay emulador/dispositivo en esta sesión. Los ítems A-G de la sección 15 del pedido quedan pendientes de certificación en dispositivo. `BLOCKED` nunca equivale a `DONE`.
- `legalUpToDate` se hidrata desde `GET /users/me` vía callback en `AuthRepositoryImpl`; si el login no devuelve `/me` inmediatamente, el valor por defecto es `true` (no bloquea).
- NO se implementaron Rooms/Explore/RoomDetail/FCM/androidTest/ONNX (pertenecen a PR B/C). Sin merge del PR.

## Resultado

**D19-ACCOUNT-UX-CORE-CODE-READY** — SIN MERGE.
