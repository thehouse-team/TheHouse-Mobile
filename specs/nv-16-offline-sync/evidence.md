# NV-16 — EVIDENCIA

Estado: IMPLEMENTED (cache-first reads) — autorización humana explícita en sesión.

## Alcance implementado
- Lecturas cacheables cache-first + network refresh: propiedades (listado p1 y detalle por id), universidades (p1), favoritos lectura (listado/ids, user-scoped), perfil y preferencias lectura segura (user-scoped).
- Cache local JSON sobre `PreferencesStore`/DataStore existente (`data/cache/OfflineCacheStore.kt`, snapshots) con key estable, `userScope` cuando aplica y `updatedAt` en envelope. No se creó segunda base Room (no existía BD Room con entidades).
- Fallback: en éxito de backend se reemplaza cache; en fallo de red/timeout/HTTP elegible se usa cache si existe (stale/offline); sin cache se retorna error/retry existente. Mutaciones (favoritos add/remove, update perfil/preferencias) siguen siendo online-only sin éxito simulado.
- UX mínima: `OfflineStatusBanner` ("Sin conexión" / "Sin conexión · Mostrando información guardada") en Explore, Detalle de propiedad y Favoritos vía `ConnectivityObserver` (señal UX, no garantía de Internet). Sin rediseño de pantallas.
- Aislamiento/limpieza: limpieza de cache privada por usuario en transición de sesión activa→no activa (`AppContainer.observePushSession`) y en `LocalUserDataPurgerImpl` (logout/delete account). Cache pública (catálogo) conservada.
- No persistido: JWT, refresh token, FCM token, agreement codes, qrToken ni secretos. Sin WorkManager workers/polling/colas de escritura (no hay operaciones syncables con contrato real aprobado).

## Validación
- `.\gradlew.bat testDebugUnitTest` — BUILD SUCCESSFUL (330 tests, 0 failed) tras 1 corrección en test nuevo de aislamiento de cache perfil/preferencias.
- `.\gradlew.bat lintDebug` — BUILD SUCCESSFUL.
- `.\gradlew.bat assembleDebug` — BUILD SUCCESSFUL.

## Tests agregados
- `app/src/test/java/com/thehouse/app/data/cache/OfflineCacheRepositoryTest.kt`: cache hit/miss y actualización en éxito, fallback con cache y error sin cache (propiedades/universidades), refresh reemplaza stale (detalle), favoritos lectura user-scoped, aislamiento de keys perfil/preferencias por usuario, purga de cache privada y verificación de ausencia de claves sensibles (jwt/fcm/qrToken/agreement).

## Runtime
- ADB no disponible en entorno (`adb` no reconocido). No se pudo ejecutar prueba manual online/offline en dispositivo/emulador; sin evidencia runtime inventada.

## Compatibilidad
- Sin cambios en Backend, contratos, Docker ni módulos no requeridos. Repositorios mantienen interfaces/firmas compatibles (parámetros opcionales para cache/connectividad).