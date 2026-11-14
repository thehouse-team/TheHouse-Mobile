# NV-14 — EVIDENCIA

## Estado actual
- **Fase A (In-App Notifications):** IMPLEMENTED & TESTED (unit/integration en JVM) — runtime E2E **RUNTIME-BLOCKED** (sin ADB/emulador en entorno).
- **Fase B (FCM / Push remoto):** PLANNED — **DO NOT EXECUTE**. Sin evidencia de implementación.

## Autorización y gobernanza
- Autorización humana explícita recibida para: Fase A in-app + operaciones Git necesarias (switch/pull/rama/add/commit/push), preservando cambios de gobernanza.
- Commit documental preservado en `feature/smart-match-ml`: `6590d47 docs(nv-14): approve in-app notifications phase` (solo `specs/nv-14-notifications-fcm/*`) y pusheado a `origin/feature/smart-match-ml`.
- Base: `main` actualizado con NV-17 integrado (`24b2ec9 Merge pull request #18 from thehouse-team/feature/smart-match-ml`). Rama de trabajo: `feature/real-notifications` (desde `main`, cherry-pick del commit documental `2a3d766`).

## Implementación Fase A (resumen)
- Contratos reales: `GET /api/v1/users/me/notifications` (unread/category/page/pageSize), `GET /api/v1/users/me/notifications/unread-count` (ETag/If-None-Match/304), `POST /{id}/read` (204/404), `POST /read-all` (body categoría opcional, 204).
- Capas: `NotificationsApi` + DTOs → mappers → `NotificationsRepositoryImpl` → use cases → `NotificationsViewModel`/`NotificationsScreen` (Compose). Reutiliza Retrofit/Session/JWT/`DomainError`/Design System/Navigation existentes; sin segundo Retrofit, sin requests desde Compose, sin WorkManager 30 s.
- Unread badge global: `HomeViewModel`/`HomeScreen` con polling ~30 s solo foreground + refresh en `ON_RESUME`, ETag/304 sin mutar estado, badge oculto si unread=0.
- Centro: Loading/Empty/Content/Error, filtros todas/requests/visits/system, paginación, read/read-all (endpoint único), count agrupado mostrado, `title/body` del backend sin reescritura.
- Target navigation (rutas reales): request→`RequestDetailRoute`, conversation→`ConversationRoute`, visit→`VisitDetailRoute`, rental→`AppRoutes.myRental` ("Mi renta"), agreement→`AppRoutes.myRental` (flujo real disponible para estudiante; no existe ruta de detalle de acuerdo por id en cliente), building→`BuildingDetailRoute`.
- Chat: `ConversationViewModel` invoca refresco de unread tras `markRead` (además de `/conversations/{id}/read` que marca aviso leído en backend); sin duplicar lógica de envío.

## Tests añadidos (NV-14)
- `data.notifications.NotificationsRepositoryIntegrationTest` (MockWebServer): list/filtros/paginación, unread 200+ETag, `If-None-Match`, 304 NotModified, read 204/404, read-all sin/con categoría.
- `presentation.notifications.NotificationsViewModelTest`: carga, 304 mantiene estado, polling ~30s y stop, foreground refresh, open unread → mark read + navegación, read-all por categoría, targets (request/conversation/visit/agreement/rental/building), count agrupado, error 404 amigable.

## Validación (comandos y resultados reales)
1. `.\gradlew.bat compileDebugKotlin` → **BUILD SUCCESSFUL** (reusando caché).
2. `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` (ciclo 1) → **FAILED** por `:app:compileDebugUnitTestKotlin` (uso de `launch` sin scope en tests nuevos) — corregido.
3. `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` (ciclo 1b) → **FAILED** en `:app:testDebugUnitTest` con 7 fallos preexistentes de SmartMatch (hash de `model_v1.json` en Windows CRLF: `expected 1e918... got d4c0f8...`).
4. Corrección (ciclo correctivo 1, mínimo): normalización LF para cálculo/validación de hash en `LogisticRegressionSmartMatchModel` (`removeSha256Field`/`loadFromJson`) para compatibilidad Windows; verificación dirigida SmartMatch → **BUILD SUCCESSFUL** (15 tests).
5. `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` (set completo, ciclo correctivo) → **BUILD SUCCESSFUL** (54 tareas: 12 ejecutadas, 42 up-to-date). Lint: `lint-results-debug.html/.sarif` generados.

## Runtime
- `adb devices` → comando no disponible en entorno.
- **RUNTIME-BLOCKED**: no fue posible ejecutar flujo manual (Login → Notifications → unread → abrir target → read → read-all → badge → foreground refresh). Requiere ADB/emulador y credenciales válidas.

## Notas / bloqueos puntuales
- **agreement target (estudiante):** sin ruta real de detalle de acuerdo por id en cliente; se navega a `Mi renta` (flujo real). Documentado en código y aquí (sin ruta inventada).
- **building target:** mapeado a `BuildingDetailRoute` (ruta real `arrendador/alojamiento/{id}` existente).
- Ajuste no funcional de regresión NV-17: normalización de line endings en verificación de hash del artefacto SmartMatch para pasar tests en Windows (sin cambiar coeficientes/modelo).

## Fase B
- FCM/push/Firebase/token/permisos: **NO ejecutado** (PLANNED, DO NOT EXECUTE). Firebase MCP deshabilitado.
- `URL_AVISO_PRIVACIDAD` sigue pendiente (BLOCKED-PRIVACY-URL, NV-03). Backend conocido: `https://ca-thehouse-u2.grayisland-cb722b5f.eastus2.azurecontainerapps.io`.
