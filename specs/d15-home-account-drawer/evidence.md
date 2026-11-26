# EVIDENCIA D15 — Home Account Drawer

| Campo | Valor |
|---|---|
| Branch | `fix/home-account-drawer-navigation` |
| Base | `main` @ `2626e7cf...`; sincronizada mediante **merge normal** de `origin/main` @ `28cc3e0` (D14 ya integrado en main) |
| Autorización | Humana individual (2026-11-26) |
| Estado | IMPLEMENTADO / TESTED / EVIDENCIADO — **RUNTIME PENDIENTE** |

## 1. Cambios implementados

| Task | Cambio | Archivos |
|---|---|---|
| D15-T01 | Inventario de 11 rutas reales + decisiones guest/auth/arrendador | `spec.md` |
| D15-T02 | `DrawerItemCatalog.kt` (catálogo puro, sin Compose/Android) y `AppDrawer.kt` (render + dispatch) | `presentation/navigation/` |
| D15-T03 | Drawer global en `TheHouseRoot` (`MainActivity.kt`): `ModalNavigationDrawer`, hamburger único con `testTag("home_menu_button")`, 11 navegaciones reales + privacidad + logout | `MainActivity.kt` |
| D15-T04 | Eliminados `AccountActionsCard`, `AccountActionRow`, `HomeMenuDrawer`, `ModalNavigationDrawer` local y hamburger duplicado de `HomeScreen.kt` | `HomeScreen.kt` |
| D15-T05 | `onOpenNotifications` cableado en `MainActivity.kt` (antes callback vacío) | `MainActivity.kt` |
| D15-T06 | Guest solo acciones públicas; auth 7 ítems; sección visual "Arrendador" sin rol inventado; logout reutiliza `container.logout` (`LogoutUseCase`) | `DrawerItemCatalog.kt`, `AppDrawer.kt`, `MainActivity.kt` |
| D15-T07 | Back (`launchSingleTop`) y bottom nav preservados; `showHamburger = showBottomNav` para no duplicar back arrow | `MainActivity.kt` |
| D15-T08 | Tests + gates + PR separado | ver §3 |

## 2. Decisiones aplicadas

- Guest: `drawer_login` + `drawer_privacy` únicamente. Sin rutas protegidas. No se cambiaron guards de Preferencias/Quick Setup.
- Auth: Mi perfil, Preferencias, Configuración inicial, Notificaciones, Mis solicitudes, Conversaciones, Mi renta + Cerrar sesión.
- Arrendador: sección visual temporal "Arrendador" con 4 acciones; sin `isLandlord`/`landlordEnabled`/rol nuevo; backend/destino sigue siendo autoridad. Documentado como temporal.
- Un solo hamburger: el de `HomeScreen` fue eliminado; el global vive en el shell.
- Logout: `container.logout()` (`LogoutUseCase` → `AuthRepositoryImpl`, limpieza local primero + POST best-effort) y `RootViewModel` resuelve guest/onboarding con `popUpTo(0)`. No se creó logout alternativo.

## 3. Gates reales

```text
.\gradlew.bat clean compileDebugKotlin   → BUILD SUCCESSFUL
.\gradlew.bat testDebugUnitTest          → BUILD SUCCESSFUL (413 tests, 0 failures)
.\gradlew.bat lintDebug                  → BUILD SUCCESSFUL
.\gradlew.bat assembleDebug              → BUILD SUCCESSFUL
git diff --check                         → exit 0
```

Nuevos tests: `DrawerItemCatalogTest` (7) + `HomeAccountBlockRemovalTest` (4). Los existentes no se redujeron.

## 4. Riesgos / deuda

- **RUNTIME PENDIENTE**: sin ADB/dispositivo. No se afirma que el drawer se haya abierto en pantalla.
- El rol arrendador sigue sin señal contractual: decisión temporal documentada en `spec.md` §3.
- `MainActivity.kt` concentra el NavHost; el cambio fue quirúrgico (wrapper + lambdas + un callback), sin mover el grafo.

## 5. Evidencia runtime requerida para cerrar

```text
Home sin panel "Tu cuenta"
Un solo hamburger → abre drawer
Guest: "Iniciar sesión o registrarse" + Privacidad (sin 401)
Auth: 7 ítems + sección Arrendador + Cerrar sesión
Cada opción: drawer se cierra → pantalla real → back funcional
Cerrar sesión → sesión limpia → guest → sin pantallas autenticadas en back stack
```

## 6. Rollback

Revert normal del PR (sin force push): se recupera `AccountActionsCard` y el drawer local de Home. Sin migración de datos ni cambios de sesión.

## 4. Certificación D15 (2026-11-26, segundo pase)

- **BASE_SYNCED:** `git merge origin/main --no-edit` sobre `28cc3e0`, sin rebase destructivo. Conflicto único en `memoria.md`, resuelto conservando las entradas D15 y D14/main. Sin código D16 (#32) en esta rama.
- **HOME_ACCOUNT_BLOCK_REMOVED:** `HomeScreen.kt` no contiene `AccountActionsCard`/`AccountActionRow` ni "Tu cuenta" en el body; conserva propiedades, "Publicar mi renta" y tarjeta pública de privacidad.
- **GLOBAL_HAMBURGER:** un único `testTag("home_menu_button")` en `MainActivity.TheHouseRoot`; `showHamburger = showBottomNav` para no duplicar el back arrow en detalle.
- **GUEST_ITEMS:** `Iniciar sesión o registrarse`, `Privacidad` (0 rutas protegidas).
- **AUTH_ITEMS:** Mi perfil, Preferencias, Configuración inicial, Notificaciones, Mis solicitudes, Conversaciones, Mi renta, Cerrar sesión.
- **LANDLORD_ITEMS:** sección visual temporal "Arrendador": Introducir código, Mis alojamientos, Solicitudes recibidas, Mis acuerdos. Sin rol inventado.
- **NOTIFICATIONS_FIXED:** `onOpenNotifications` → `AppRoutes.notifications.route` en el shell.
- **ROUTES_WORKING:** 12 destinos registrados en `MainActivity`; `AppDrawerContent` cierra el drawer antes de navegar con `launchSingleTop`. Nuevo `DrawerNavigationCatalog.kt` (puro, sin Compose) usado por el dispatch: 14 tags → acciones reales, 13 rutas reales; ningún callback vacío ni Toast.
- **LOGOUT:** `container.logout()` = `LogoutUseCase(AuthRepository)`; limpieza local + POST best-effort; `RootViewModel` limpia back stack con `popUpTo(0)`.
- **TESTS:** `DrawerItemCatalogTest` (11) + `DrawerNavigationCatalogTest` (20 nuevos) verifican catálogo guest/auth/arrendador, 14 despachos reales y las 13 rutas. Gates: `clean compileDebugKotlin`, `testDebugUnitTest` **469 tests / 0 failures / 0 errors**, `lintDebug` 0 errores, `assembleDebug`, `git diff --check` exit 0.
- **RUNTIME_PENDIENTE:** sin dispositivo/ADB. Requiere recorrido AUTH (hamburger → Perfil → volver → Preferencias → Notificaciones → Solicitudes → Conversaciones → Mi renta → Arrendador → logout), GUEST (solo Login/Registro + Privacidad) y_HOME sin "Tu cuenta".
- **CI:** GitHub Actions `Android quality gates` run `45869173568`, conclusión `success` (verificado con `gh pr checks 31`). PR #31 MERGEABLE, abierto, sin merge.\n- **Resultado:** D15-DRAWER-CODE-CERTIFIED.