# PLAN D19 — Account + UX Core

| Campo | Valor |
|---|---|
| Spec | D19-Account-UX-Core (`specs/d19-account-ux-core/spec.md`) |
| Versión | 1.0.0 |
| Branch | `fix/account-ux-core` desde `origin/main` @ `1ab64eb` |

## Fases verificables

### F1 — Gobernanza y base
- Crear spec/plan/task/checklist/evidence.
- Crear branch desde `origin/main`.
- Verificar `constitution.md`, `AGENTS.md`, `memoria.md` y contrato Backend 780b7be.
- **Salida:** documentos SDD presentes; branch correcta.

### F2 — Sesión / Bearer / Refresh (T01)
- Auditar `SessionStore`, `AuthRepository`, `BearerTokenInterceptor`, `RefreshAuthenticator`, `RetrofitFactory`, `RootViewModel`.
- Asegurar: token válido → Bearer; 401 → refresh single-flight + retry 1 vez; refresh inválido → `Anonymous` + login; sin loops; sin logs de token.
- Tipar 401 definitivo como `Unauthorized` → `RequireLogin` en Profile/Preferences.
- **Salida:** tests de sesión 1-4; sin regresión de auth existente.

### F3 — Profile y Preferences (T02, T03)
- Sin cambios de DTO. Ajustar mapeo de error 401 a `RequireLogin` en ViewModels/pantallas.
- Verificar PATCH `{name}` y PUT `{universityId}` intactos; PUT preferences sin campos extra.
- **Salida:** tests 5-11.

### F4 — Header y Drawer (T04, T05)
- `Scaffold.topBar` de `TheHouseRoot`: `[BrandMark] TheHouse` + hamburger a la derecha, insets status bar, 1 solo tag.
- `DrawerItemCatalog`: guest 2 items con etiqueta "Aviso de privacidad"; auth con privacidad añadida antes de logout.
- **Salida:** tests 12-18; sin hamburger en HomeScreen.

### F5 — Registro, Legal y Aceptación (T06, T07)
- Link clicable en `RegisterScreen` hacia `AppRoutes.privacy` sin enviar campos legales.
- `LegalApi` + `LegalRepository` + caso de uso; `CurrentUserDto` con `legal/legalUpToDate`.
- `PrivacyScreen/ViewModel`: estado real, versión, updatedAt, URL null segura.
- Aceptación `PUT /users/me/legal` con 409 → recarga de versión sin loop.
- **Salida:** tests 19-23.

### F6 — Eliminar cuenta (T08)
- `AccountApi` con preview y delete; body exacto con "ELIMINAR".
- UI con password + confirmación explícita; preview de blockers/effects.
- Mapeo 403/409/429/503; 204 → purga + sesión Anonymous + back stack limpio; sin requests posteriores.
- **Salida:** tests 24-30.

### F7 — Gates y cierre
- Ejecutar gates obligatorios y `git diff --check`.
- Actualizar `memoria.md` y `evidence.md`.
- Commit + push + apertura de PR. **NO merge.**
- **Salida:** `D19-ACCOUNT-UX-CORE-CODE-READY`, runtime pendiente.

## Dependencias entre fases

F1 → F2 → F3 → F4 → F5 → F6 → F7. F4 y F5/F6 pueden compartir `MainActivity.kt`/`PrivacyScreen` pero se ejecutan secuencialmente para evitar conflictos.

## Criterio de detención

Si un gate falla dos ciclos seguidos, detener y escalar a decisión humana (AGENTS.md §2).
