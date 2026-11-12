# D9 — Evidence

## Branch / base
- `git switch main; git pull origin main` → main en `6ca8639`.
- `git checkout -b feature/requests-real-chat` desde main.
- Base D11: `git merge feature/profile-preferences-ui-polish` → fast-forward a `23f7fc3` (36 archivos).

## Contratos implementados (solo confirmados por el orden D9.1)
- `POST /api/v1/rooms/{roomId}/requests`
- `GET/POST /api/v1/users/me/requests[...]` (lista, detalle, cancel)
- `GET /api/v1/landlord/requests[...]` (lista, detalle, accept, reject)
- `GET /api/v1/conversations`, `GET/POST /api/v1/conversations/{id}/messages`, `POST /api/v1/conversations/{id}/read`

## Archivos principales
- Data: `dto/requests/RequestDtos.kt`, `dto/chat/ChatDtos.kt`, `api/RequestsApi.kt`, `api/ChatApi.kt`, `requests/mapper|repository`, `chat/mapper|repository`.
- Domain: `model/Request.kt`, `model/Chat.kt`, `error/RequestError.kt`, `error/ChatError.kt`, `repository/RequestsRepository.kt`, `repository/ConversationRepository.kt`, `usecase/RequestsUseCases.kt`, `usecase/ChatUseCases.kt`.
- Presentation: `requests/` (create/list/detail + mensajes de error), `landlord/` (list/detail), `chat/` (conversations + conversation con polling/ETag).
- Wiring: `AppContainer.kt`, `U2ViewModelFactory.kt`, rutas (`ChatRoutes.kt`, `AppRoutes.kt`), `MainActivity.kt`, `HomeScreen.kt`, `PropertyDetail*` (Contact → Solicitar).

## Comandos y resultados reales
1. `.\gradlew.bat compileDebugKotlin` → BUILD SUCCESSFUL (tras correcciones de imports; 1 ciclo).
2. `.\gradlew.bat testDebugUnitTest` → BUILD SUCCESSFUL, **149 tests** completados, 0 fallos (correcciones: asserts de test; StopPolling al final de cada test de polling para no dejar el scheduler activo).
3. `.\gradlew.bat lintDebug assembleDebug` → BUILD SUCCESSFUL; lint: **0 errors, 20 warnings**.

## Regresión D3–D11
- Incluida en el set: auth/session (NV-04), catálogo, favorites, visits (D8), profile/preferences (D11), networking, config — todos verdes en los 149 tests.

## Runtime
- Backend desplegado: `GET /api/v1/universities` → **HTTP 200** real (Azure Container Apps, 2026-11-12).
- **RUNTIME-BACKEND-PENDING (parcial):** no hay emulador/adb ni credenciales autorizadas en esta sesión; no se inventaron credenciales. El flujo punta a punta (solicitud → conversación → mensajes → polling/read) queda pendiente de validación en dispositivo/emulador con cuentas reales estudiante + arrendador.

## Notas de implementación
- Polling: 1 fetch inicial + polls con `after=<último mensaje>` e `If-None-Match=<ETag>`; intervalo = `pollAfterSeconds` del backend (default 10 s); 304 → sin cambios de lista; polling vive en un Job del ViewModel, se detiene con `StopPolling` (onDispose) y `onCleared`; `CancellationException` rethrow.
- Idempotencia de envío: `pendingClientMessageId` persiste ante fallo (retry usa mismo UUID); se limpia al éxito (acción nueva → UUID nuevo); doble submit bloqueado con `isSending`.
- Landlord: la app no persiste rol; 403 `landlord_required` se mapea al mensaje amigable "Necesitas activar el modo arrendador." No se inventó endpoint de rol.
- `hasMore=true` se muestra como indicador ("Hay mensajes anteriores."); el contrato no define parámetro `before`, por lo que no se inventa paginación retroactiva.

## Blockers
- Ninguno de código. Runtime E2E pendiente (ver arriba).
