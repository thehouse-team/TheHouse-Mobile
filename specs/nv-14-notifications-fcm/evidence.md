# NV-14 — EVIDENCIA

## Estado actual
- **Fase A (In-App Notifications):** IMPLEMENTED & TESTED (unit/integration en JVM) — runtime E2E **RUNTIME-BLOCKED** (sin ADB/emulador en entorno).
- **Fase B (FCM / Push remoto):** IMPLEMENTED & TESTED (unit/build) — runtime **FCM-RUNTIME-PARTIAL** (sin `google-services.json`, sin sender Backend, sin ADB/emulador).

## Autorización y gobernanza
- Autorización humana explícita para: Fase A in-app + operaciones Git necesarias (switch/pull/rama/add/commit/push), preservando cambios de gobernanza.
- Commit documental preservado en `feature/smart-match-ml`: `6590d47 docs(nv-14): approve in-app notifications phase` (solo `specs/nv-14-notifications-fcm/*`) y pusheado a `origin/feature/smart-match-ml`.
- Base: `main` actualizado con NV-17 integrado (`24b2ec9 Merge pull request #18 from thehouse-team/feature/smart-match-ml`). Rama de trabajo: `feature/real-notifications` (desde `main`, cherry-pick del commit documental `2a3d766`).
- **Fase B:** autorización humana explícita 2026-11-17 para implementar Mobile, actualizar spec/plan/tasks/checklist/evidence, tests/build/lint, commit y push. Sin merge, force push, Docker, secretos ni endpoints inventados.

## Implementación Fase A (resumen)
- Contratos reales: `GET /api/v1/users/me/notifications` (unread/category/page/pageSize), `GET /api/v1/users/me/notifications/unread-count` (ETag/If-None-Match/304), `POST /{id}/read` (204/404), `POST /read-all` (body categoría opcional, 204).
- Capas: `NotificationsApi` + DTOs → mappers → `NotificationsRepositoryImpl` → use cases → `NotificationsViewModel`/`NotificationsScreen` (Compose). Reutiliza Retrofit/Session/JWT/`DomainError`/Design System/Navigation existentes; sin segundo Retrofit, sin requests desde Compose, sin WorkManager 30 s.
- Unread badge global: `HomeViewModel`/`HomeScreen` con polling ~30 s solo foreground + refresh en `ON_RESUME`, ETag/304 sin mutar estado, badge oculto si unread=0.
- Centro: Loading/Empty/Content/Error, filtros todas/requests/visits/system, paginación, read/read-all (endpoint único), count agrupado mostrado, `title/body` del backend sin reescritura.
- Target navigation (rutas reales): request→`RequestDetailRoute`, conversation→`ConversationRoute`, visit→`VisitDetailRoute`, rental→`AppRoutes.myRental` ("Mi renta"), agreement→`AppRoutes.myRental` (flujo real disponible para estudiante; no existe ruta de detalle de acuerdo por id en cliente), building→`BuildingDetailRoute`.
- Chat: `ConversationViewModel` invoca refresco de unread tras `markRead` (además de `/conversations/{id}/read` que marca aviso leído en backend); sin duplicar lógica de envío.

## Implementación Fase B (resumen)
- **Firebase/FCM:** BoM `35.2.0` + `firebase-messaging` (26.0.2 resuelto); plugin `com.google.gms.google-services` `4.6.0` declarado en catálogo y aplicado condicionalmente solo si existe `app/google-services.json`. El archivo **no existe** y no se generó uno falso. `BuildConfig.FCM_CONFIGURED=false` verificado en `app/build/generated/.../BuildConfig.java`.
- **Servicio:** `TheHouseFirebaseMessagingService` (`android:exported=false`, action `com.google.firebase.MESSAGING_EVENT`) procesa data-only; `onMessageReceived` valida y delega a `PushMessageHandler`; `onNewToken` delega a `PushTokenCoordinator`.
- **Token:** `FirebasePushTokenProvider` consulta token solo si `FCM_CONFIGURED` y Firebase está inicializado; `PushTokenCoordinator` valida longitud/espacios/controles y mantiene el token **solo en memoria**. Estado público `PushTokenState.Available(fingerprint)` con SHA-256 truncado; logs solo event codes.
- **Backend token:** búsqueda en `C:\THEHOUSE\TheHouse-backend` (controllers/specs) y specs Mobile sin endpoint de devices/token → **FCM-TOKEN-BACKEND-BLOCKED**. No se creó `POST /devices`, `POST /fcm-token` ni equivalente.
- **Permiso Android 13+:** `POST_NOTIFICATIONS` en manifest; `NotificationPermissionPolicy` puro; `rememberNotificationPermissionUi` usa `RequestPermission` y solo se dispara desde tarjeta contextual del Notification Center. Rechazo → tarjeta con rationale; rechazo definitivo → `ACTION_APP_NOTIFICATION_SETTINGS`; Android <13 devuelve `GRANTED` sin runtime.
- **Canales:** `thehouse_requests` (Solicitudes), `thehouse_visits` (Visitas), `thehouse_system` (Sistema); importancia `DEFAULT`, descripción visible, sin sonidos personalizados; `PushChannels.idFor(category)` cae a Sistema.
- **Foreground/background:** `AppVisibilityTracker` (activity callbacks) distingue foreground. Foreground → `pushRefreshSignals` (Home y Notifications recargan estado real del Backend), sin notificación de sistema; background → `AndroidLocalPushNotifier` con `VISIBILITY_PRIVATE` + versión pública genérica. Sin sesión activa (incluye fallo de restauración persistente) → push suprimido.
- **Navegación:** `NotificationDestinationResolver` es el único mapper target→destino (Fase A y Fase B). Destinos: request/conversation/visit/building con id; agreement/rental→Mi renta; desconocido/ausente→Notification Center. `MainActivity` con `launchMode=singleTop` procesa cold start y `onNewIntent`; `PendingNotificationNavigation` conserva el target durante Login y se consume solo tras navegar.
- **Sesión/logout:** `observePushSession` detecta Active→no Active, cancela notificaciones locales y limpia target pendiente; no revoca token ni rompe Firebase. No existe flujo de delete account en Mobile; sin inventar revocación Backend.
- **Seguridad:** payload tratado como no confiable (kind/id/category/texto); sin URLs/intents/comandos; logs sin token/título/body/ids; PendingIntent inmutable y extracts validados; sin secretos versionados.

## Tests añadidos (NV-14)
- Fase A: `data.notifications.NotificationsRepositoryIntegrationTest` (MockWebServer) y `presentation.notifications.NotificationsViewModelTest`.
- Fase B (47 tests nuevos):
  - `domain.push.PushPayloadParserTest` (11): payload válido, target desconocido, id vacío/inválido, target ausente, categoría desconocida, title/body inválidos, controles, texto no ejecutable.
  - `domain.push.NotificationDestinationTest` (10) + `PushNavigationIntentParserTest` (6): request/conversation/visit/agreement/rental/building, target desconocido, ids inseguros, intent sin extras, fallback.
  - `domain.push.PushTokenCoordinatorTest` (5): refresh, fingerprint, onNewToken, token inválido, provider null/error, sin token completo en logs.
  - `data.notifications.push.PushMessageHandlerTest` (7): background notifica, foreground solo refresca, payload inválido, sin sesión, target desconocido, fallo de notify, datos sensibles fuera de logs.
  - `presentation.notifications.NotificationPermissionPolicyTest` (7): SDK 32/33, granted/denied/permanente, app operativa sin permiso.
  - `presentation.notifications.NotificationsViewModelTest`: señal push refresca unread y recarga estado real.

## Validación (comandos y resultados reales)
1. `.\gradlew.bat compileDebugKotlin` (ciclo 1) → **FAILED**: `goAsync` no existe en `FirebaseMessagingService` — corregido eliminando `goAsync` y usando `applicationScope`.
2. `.\gradlew.bat compileDebugKotlin` (ciclo correctivo 1) → **BUILD SUCCESSFUL** (7 tareas; 1 ejecutada).
3. `.\gradlew.bat compileDebugUnitTestKotlin` → **BUILD SUCCESSFUL**.
4. `.\gradlew.bat testDebugUnitTest --tests "com.thehouse.app.domain.push.*" --tests "com.thehouse.app.data.notifications.push.*" --tests "com.thehouse.app.presentation.notifications.*"` (ciclo 1) → **FAILED** 1/56 (expectativa de cola unread en el test nuevo) — corregido el test.
5. Mismo comando dirigido (ciclo correctivo 1) → **BUILD SUCCESSFUL**, 56/56 tests.
6. Set final (ciclo 1): `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` → **BUILD FAILED in 14m 28s**. Tests 297/297 verdes y `:app:assembleDebug` completado, pero `:app:lintDebug` encontró **1 error** `MissingPermission` en `AndroidLocalPushNotifier.kt:90` (`notify` sin comprobación directa reconocible por lint) + 25 warnings.
7. Corrección del ciclo 1: comprobación directa de `POST_NOTIFICATIONS` inmediatamente antes de `notify` + captura de `SecurityException` (permiso revocado entre check y publicación); warnings nuevos corregidos (`InlinedApi` suprimido con justificación SDK 33, `ObsoleteSdkInt` eliminado en canales por minSdk 26, orden de `modifier` en `NotificationsScreen`).
8. Set final (ciclo correctivo 1): `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` → **BUILD SUCCESSFUL in 3m 6s** (54 tareas: 16 ejecutadas, 38 up-to-date). Resultados: **297 tests, 0 failures, 0 errors, 0 skipped** (46 suites); **lint 0 errores / 20 warnings**; APK `app/build/outputs/apk/debug/app-debug.apk` (24,031,238 bytes).

## Runtime
- `google-services.json` → **no existe**; `FCM_CONFIGURED=false`; Firebase no se inicializa.
- No hay proyecto Firebase, sender Backend ni endpoint de registro (`FCM-TOKEN-BACKEND-BLOCKED`).
- `adb devices` → `emulator-5554 device` (Android SDK 37). Validación real de instalación: `adb install -r app/build/outputs/apk/debug/app-debug.apk` → **FAILED**: `cmd: Can't find service: package`; `service check package` → `not found`; `system_server` no está corriendo. El emulador no tiene framework Android operativo, por lo que no permite validar instalación, permiso, canales ni navegación.
- **FCM-RUNTIME-PARTIAL**: no fue posible validar token real, recepción FCM, permiso en dispositivo, canal en shade ni navegación desde push real. Lo validado es compilación, unit tests y wiring de runtime; no se inventa evidencia E2E.
- Fase A runtime sigue **RUNTIME-BLOCKED** (emulador detectado por ADB sin framework Android operativo; sin credenciales de prueba).

## Notas / bloqueos puntuales
- **FCM-TOKEN-BACKEND-BLOCKED:** no existe contrato real de registro de token/device; la app obtiene/valida el token pero no lo envía a Backend.
- **FCM-RUNTIME-PARTIAL:** falta `app/google-services.json` real (proyecto Firebase), sender data-only y dispositivo/emulador con Google Play Services.
- **Token FCM clásico deprecado por Firebase 26/FID:** se mantiene el flujo token + `onNewToken` solicitado, con `@Suppress("DEPRECATION")`; la migración a `register()`/`onRegistered` (FID) queda fuera de alcance y requerirá contrato Backend nuevo.
- **agreement target (estudiante):** sin ruta real de detalle de acuerdo por id en cliente; se navega a `Mi renta` (flujo real). Documentado en código y aquí (sin ruta inventada).
- **building target:** mapeado a `BuildingDetailRoute` (ruta real `arrendador/alojamiento/{id}` existente).
- Ajuste no funcional de regresión NV-17: normalización de line endings en verificación de hash del artefacto SmartMatch para pasar tests en Windows (sin cambiar coeficientes/modelo).
- `URL_AVISO_PRIVACIDAD` sigue pendiente (BLOCKED-PRIVACY-URL, NV-03). Backend conocido: `https://ca-thehouse-u2.grayisland-cb722b5f.eastus2.azurecontainerapps.io`.
