# SPEC D15 — Home Account Drawer

| Campo | Valor |
|---|---|
| Spec | D15-Home-Account-Drawer |
| Versión | 1.0.0 |
| Estado | AUTORIZADA / EN EJECUCIÓN |
| Autorización | Humana explícita (instrucción individual 2026-11-26) |
| Branch | `fix/home-account-drawer-navigation` |
| Base | `main` @ `2626e7cf7803c4c869d636fb5715c3d834d0f256` (NO copia cambios de D14) |

## 1. Objetivo

Eliminar del body/scroll de Home el bloque "Tu cuenta" con sus 12 acciones y concentrarlas en un drawer hamburguesa **único y global** en el shell (`MainActivity.TheHouseRoot`), preservando contenido Home, propiedades, CTA "Publicar mi renta" y bottom navigation.

## 2. Inventario de rutas reales (auditadas)

| Entrada | Ruta | Pantalla | ViewModel | Auth | Rol |
|---|---|---|---|---|---|
| Mi perfil | `AppRoutes.profile` `"perfil"` | `ProfileScreen` | `ProfileViewModel` | SÍ | No |
| Preferencias | `AppRoutes.preferences` `"preferencias"` | `PreferencesScreen` | `PreferencesViewModel` | SÍ | No |
| Configuración inicial | `AppRoutes.quickSetup` `"configuracion_inicial"` | `QuickSetupScreen` | `QuickSetupViewModel` | SÍ | No |
| Notificaciones | `AppRoutes.notifications` `"notificaciones"` | `NotificationsScreen` | `NotificationsViewModel` | SÍ | No |
| Mis solicitudes | `AppRoutes.requests` `"solicitudes"` | `RequestsScreen` | `RequestsViewModel` | SÍ | No |
| Conversaciones | `AppRoutes.conversations` `"conversaciones"` | `ConversationsScreen` | `ConversationsViewModel` | SÍ | No |
| Mi renta | `AppRoutes.myRental` `"mi_renta"` | `MyRentalScreen` | `MyRentalViewModel` | SÍ | No |
| Introducir código | `AppRoutes.rentalCode` `"codigo_renta"` | `RentalCodeScreen` | `RentalCodeViewModel` | SÍ | Arrendador (sección visual temporal) |
| Mis alojamientos | `AppRoutes.landlordDashboard` `"arrendador"` | `LandlordDashboardScreen` | `LandlordDashboardViewModel` | SÍ | Arrendador (sección visual temporal) |
| Solicitudes recibidas | `AppRoutes.landlordRequests` `"solicitudes_arrendador"` | `LandlordRequestsScreen` | `LandlordRequestsViewModel` | SÍ | Arrendador (sección visual temporal) |
| Mis acuerdos | `AppRoutes.landlordAgreements` `"acuerdos_arrendador"` | `LandlordAgreementsScreen` | `LandlordAgreementsViewModel` | SÍ | Arrendador (sección visual temporal) |
| Cerrar sesión | No es ruta: `HomeEvent.LogoutClicked` → `LogoutUseCase` | N/A | `HomeViewModel` + `RootViewModel` | Implícito | No |

11/11 rutas navegables tienen pantalla y ViewModel reales. **Ninguna pantalla dummy.** Defecto a corregir: `onOpenNotifications` está sin cablear (`HomeScreen.kt:109` vs ausencia en `MainActivity.kt:230-286`).

## 3. Decisiones humanas vigentes

### Guest
NO ve: Mi perfil, Preferencias, Configuración inicial, Notificaciones, Mis solicitudes, Conversaciones, Mi renta ni acciones de arrendador (todas requieren auth). Ve **"Iniciar sesión o registrarse"** y solo acciones públicas existentes (privacidad). **NO se cambian los guards** de Preferencias/Quick Setup dentro de D15.

### Auth
Mi perfil, Preferencias, Configuración inicial, Notificaciones, Mis solicitudes, Conversaciones, Mi renta, Cerrar sesión.

### Arrendador
**No existe señal confiable de rol** en la sesión (`AuthResponseDto.kt:46` prohíbe inventar `landlordEnabled`/`isLandlord`). Por compatibilidad, las cuatro acciones de arrendador se muestran en una sección visual **"Arrendador"** a usuarios autenticados. El backend/destino sigue siendo autoridad. Decisión **temporal** hasta que exista una señal contractual de rol/capacidad. **No se implementa un rol nuevo.**

## 4. Drawer global y UX

- Elevar `ModalNavigationDrawer` al shell real (`TheHouseRoot`, `MainActivity.kt:138-1075`) usando el `navController` existente.
- **Un solo hamburger**: eliminar el drawer/hamburger local de `HomeScreen.kt` (`drawerState`, `ModalNavigationDrawer`, `HomeMenuDrawer`, botón `home_menu_button`, líneas 119-120, 141-156, 175-186, 330-397).
- Eliminar `AccountActionsCard` + `AccountActionRow` (`HomeScreen.kt:265-301,507-627`) y el título "Tu cuenta" del scroll de Home.
- Patrón obligatorio por opción: **cerrar drawer → navegar → pantalla real → back funcional** (`launchSingleTop`). Sin TODO, callbacks vacíos ni rutas dummy.
- Home conserva: contenido principal, propiedades, CTA "Publicar mi renta", bottom navigation.
- `TheHouseTopBar` no tiene slot de menú; no se modifica en D15 salvo necesidad de compilación.

## 5. Navegación y logout

Cablear rutas reales existentes: `profile`, `preferences`, `quickSetup`, `notifications`, `requests`, `conversations`, `myRental`, `rentalCode`, `landlordDashboard`, `landlordRequests`, `landlordAgreements`. Corregir `onOpenNotifications`.

Logout: reutilizar **exclusivamente** `LogoutUseCase` → `AuthRepository.logout()` → limpieza local primero + `POST /api/v1/auth/logout` best-effort → `SessionState.Anonymous` → `RootViewModel` → `targetRoute` con `popUpTo(0){inclusive=true}`.

## 6. Fuera de alcance

- No crear pantallas, rutas ni endpoints nuevos.
- No modificar contratos Backend ni D14/Preferences.
- No inventar roles ni capacidades.
- No cambiar guards de auth.
- No mergear el PR de D15 sin evidencia runtime y aprobación humana.
- No mergear PR #29 ni PR #30 desde este branch.

## 7. Criterios de aceptación

1. Home sin "Tu cuenta" ni sus 12 filas en el scroll; con propiedades, CTA y bottom nav.
2. Un solo hamburger, que abre el drawer global.
3. Ítems correctos por sesión/rol (guest / auth / sección Arrendador).
4. Cada ítem cierra el drawer, navega a pantalla real y el back funciona.
5. `onOpenNotifications` cableado.
6. Logout real, sin back stack autenticado.
7. Tests nuevos sin reducir los existentes.

## 8. Riesgos

- `MainActivity.kt` concentra ~880 líneas de NavHost con guards y efectos de root/push: riesgo de regresión.
- Evitar hamburger duplicado al retirar el de Home.
- Rol arrendador sin señal: decisión temporal documentada.
- Sin ADB/dispositivo: runtime pendiente.

## 9. Rollback

Revert normal del PR (sin force push): se recupera `AccountActionsCard` y el drawer local de Home. Ningún dato de sesión se migra; `RootViewModel`/`LogoutUseCase` intactos.
