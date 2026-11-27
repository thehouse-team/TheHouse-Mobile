# D21 — Matriz de permisos Android

| Permiso | Declarado | Uso real | Clasificación | Solicitud contextual | Resultado |
|---|---|---|---|---|---|
| `INTERNET` | Sí | Retrofit/OkHttp a Backend HTTPS | Necesario | No requiere runtime | Necesario / OK |
| `ACCESS_NETWORK_STATE` | Sí (añadido en D21) | `AndroidConnectivityObserver` → `ConnectivityManager` para reintentos de sync y fallback local | Necesario | Normal, no requiere runtime | Necesario / OK (antes faltaba y se usaba la API) |
| `POST_NOTIFICATIONS` | Sí | `NotificationPermission.kt:107` tras acción del usuario; verificaciones en `AndroidLocalPushNotifier.kt:92-100,113-119` | Opcional (Android 13+) | Sí, solo ante acción del usuario | OK; denegar no bloquea registro/login/navegación |
| `CAMERA` | No | Ningún flujo real | No utilizado | N/A | No añadir |
| `READ/WRITE_EXTERNAL_STORAGE` | No | Photo Picker del sistema / Coil | No utilizado | N/A | No añadir |
| `ACCESS_*_LOCATION` | No | Sin funcionalidad de geolocalización en Mobile | No utilizado | N/A | No añadir |

## Comportamiento de denegación

- `POST_NOTIFICATIONS`: la app continúa con notificaciones internas; FCM no muestra notificación del sistema pero el refresh en foreground y el Notification Center siguen funcionando.
- Denegación permanente: el usuario puede revocar desde Settings; `NotificationPermission` comprueba el estado antes de mostrar.
- Sin permisos opcionales el registro, login, navegación, catálogo y pagos no se bloquean.

## Evidencia
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/thehouse/app/presentation/notifications/NotificationPermission.kt`
- `app/src/main/java/com/thehouse/app/data/connectivity/AndroidConnectivityObserver.kt`
- Tests: `app/src/test/java/com/thehouse/app/presentation/notifications/NotificationPermissionPolicyTest.kt`, `CameraPermissionPolicyTest.kt`
