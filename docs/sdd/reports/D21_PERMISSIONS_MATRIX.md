# D21 — Matriz de permisos Android

| Permiso | Declarado | Uso real | Clasificación | Solicitud contextual | Resultado |
|---|---|---|---|---|---|
| `INTERNET` | Sí | Retrofit/OkHttp a Backend HTTPS | Necesario | No requiere runtime | Necesario / OK |
| `ACCESS_NETWORK_STATE` | Sí (añadido en D21) | `AndroidConnectivityObserver` → `ConnectivityManager` para reintentos de sync y fallback local | Necesario | Normal, no requiere runtime | Necesario / OK (antes faltaba y se usaba la API) |
| `POST_NOTIFICATIONS` | Sí | `NotificationPermission.kt` tras entrada contextual al centro; verificaciones en `AndroidLocalPushNotifier.kt:92-100,113-119` | Opcional (Android 13+) | Sí, una sola vez; decisión persistida en DataStore | OK; denegar no bloquea registro/login/navegación. Causa raíz corregida: `askedOnce` era `rememberSaveable` y se perdía al morir el proceso |
| `CAMERA` | No | Photo Picker del sistema / Android Photo Picker | No utilizado | N/A | No añadir |
| `READ/WRITE_EXTERNAL_STORAGE` | No | Photo Picker del sistema / Coil | No utilizado | N/A | No añadir |
| `ACCESS_COARSE_LOCATION` | Sí (D21 adenda) | Solo botón «Usar mi ubicación actual» en Nuevo edificio; `AndroidDeviceLocationProvider` | Opcional | Sí, al pulsar el botón | OK; si se deniega, el formulario se completa manualmente |
| `ACCESS_FINE_LOCATION` | Sí (D21 adenda) | Ídem; `LocationManager` del sistema, sin proveedor comercial | Opcional | Sí, al pulsar el botón | OK; sin `ACCESS_BACKGROUND_LOCATION`, sin seguimiento continuo |

## Comportamiento de denegación

- `POST_NOTIFICATIONS`: la app continúa con notificaciones internas; FCM no muestra notificación del sistema pero el refresh en foreground y el Notification Center siguen funcionando.
- Denegación permanente: `NotificationPermissionStore` persiste la decisión; no se repite el diálogo y se ofrece `ACTION_APP_NOTIFICATION_SETTINGS`. El retorno desde Ajustes refresca el estado en `ON_RESUME`.
- Ubicación: denegado, GPS desactivado, timeout o ubicación no disponible → mensaje en español y campos manuales; permiso aproximado produce mensaje de confirmación. Sin coordenadas en logs.
- Sin permisos opcionales el registro, login, navegación, catálogo y pagos no se bloquean.

## Evidencia
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/thehouse/app/presentation/notifications/NotificationPermission.kt`
- `app/src/main/java/com/thehouse/app/data/connectivity/AndroidConnectivityObserver.kt`
- Tests: `app/src/test/java/com/thehouse/app/presentation/notifications/NotificationPermissionPolicyTest.kt`, `CameraPermissionPolicyTest.kt`
