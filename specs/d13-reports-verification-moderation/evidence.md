# Evidencia D13 — Reportes, verificación y moderación

Fecha: 2026-11-12 · Rama: `feature/reports-verification-moderation`

## Contract evidence

- Fuente: `specs/nv-21-verification-moderation/spec.md` v1.1.0 (§6 y §6.1) — espejo Backend, solo lectura.
- Contratos implementados exactamente:
  - `POST /api/v1/reports` → body `{targetType,targetId,reason,details?}`; `targetType ∈ building|room|user`; `reason ∈ fake|fraud|inappropriate|spam|other`; `details` opcional ≤500; respuesta 201 `{id,status,createdAt}`; errores 400/401/404/409 `report_exists`/429 (+`Retry-After`)/503; 403 `code=account_suspended`.
  - `GET /api/v1/landlord/buildings/{id}/verification` → `{status,lastDecision:{decision,reason,decidedAt}|null}`; `decision ∈ approved|changes_requested|rejected`.
  - `BuildingStatus` extendido con `rejected` y `suspended` (nombres exactos NV-21).
- Sin userId en ningún request (identidad del JWT vía interceptor Bearer existente). Sin endpoints, DTOs ni enums inventados.
- Moderación automática de texto/imágenes/videos: **BLOCKED** (NV-13 es borrador no ejecutable; sin uploads, proveedores ni claves) — T10.

## Implementation

- Data: `ReportsApi.kt`, `data/remote/dto/reports/ReportDtos.kt`, `ReportsRepositoryImpl.kt`, `data/remote/ProblemDetails.kt` (lectura única del Problem Details).
- Domain: `domain/model/Report.kt` (`ReportTargetType`, `ReportReason`, `ReportCreated`, `VerificationInfo`, `VerificationDecision`), `domain/repository/ReportsRepository.kt`, `domain/usecase/ReportsUseCases.kt`, `domain/error/ReportError.kt`.
- Presentation: `ReportViewModel` + `ReportScreen/ReportDestination` (razones reales, contador 0–500, CTA deshabilitado si inválido, anti doble submit, confirmación previa, mensajes de error amigables), tarjeta de verificación en `BuildingDetailScreen` + `BuildingDetailViewModel.loadVerification`, estados `REJECTED/SUSPENDED` en el `when` de publicación, `AccountSuspendedScreen` + `RootViewModel` (pantalla global, logout solo explícito).
- Navegación: `ReportRoute(targetType,targetId)` registrada en `MainActivity` con validación de target (sin fallback de target). Entradas reales en `RequestDetailScreen`: «Reportar cuarto» (`request.room.id`) y «Reportar edificio» (`request.room.buildingId`) → `onReportRoom`/`onReportBuilding` → `ReportRoute(ROOM|BUILDING, id)`.
- Suspensión: `SessionState.Suspended`, `InMemorySessionStore` (ignora hidratación/token en vuelo), `BearerTokenInterceptor` (bloquea protegidas con `AccountSuspendedIOException`, observa 403 `account_suspended` con `peekBody`, permite logout explícito), `RefreshAuthenticator` (sin refresh tras suspensión), `AuthRepositoryImpl` (login/refresh/`/me`/restore → `DomainError.AccountSuspended` sin borrar refresh token).
- Correcciones de la interrupción (integración mínima): parámetro `onReportRoom` conectado, ruta `ReportRoute.PATTERN` registrada, import duplicado de `OutlinedButton` eliminado, `getVerification` inyectado en `U2ViewModelFactory.buildingDetail`, `when` de `BuildingStatus` completado, `ReportDestination` tipado con `U2ViewModelFactory`, `when` sin sujeto de `RequestsRepositoryImpl` corregido (`||`).

## Commands and results

- `git branch --show-current` → `feature/reports-verification-moderation`; `git status --short`/`git diff --stat` (working tree preservado; sin reset/restore/clean/stash).
- `.\gradlew.bat compileDebugKotlin` → **BUILD SUCCESSFUL** (1er ciclo correctivo; error original `No value passed for parameter 'onReportRoom'` resuelto).
- `.\gradlew.bat compileDebugUnitTestKotlin` → BUILD SUCCESSFUL.
- `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` → **BUILD SUCCESSFUL** (una sola pasada de validación final).
- Tests: **206 tests, 0 failures, 0 errors** (33 suites). Nuevos D13: `ReportsRepositoryIntegrationTest` **15/15**, `ReportViewModelTest` **8/8**, `AccountSuspensionIntegrationTest` **9/9**. Regresión D3–D12 verde; lint sin errores.
- `git diff --check` → sin conflictos ni espacios de sobra.

## Runtime (BLOCKED)

- `adb version` → comando no disponible; `emulator` → no disponible.
- Causa: entorno sin ADB ni emulador instalados → **RUNTIME-BLOCKED** (sin prueba punta a punta en dispositivo). No se inventan credenciales ni respuestas del Backend.

## Blockers / deuda

- T10 Moderación: **BLOCKED** por NV-13 (borrador, sin contrato ejecutable, COST-001/almacenamiento pendientes); video fuera de alcance.
- Runtime pendiente (requiere emulador/dispositivo + cuenta de prueba).
- Target `user` soportado por contrato/API sin entrada UI (no hay id de usuario expuesto en Chat/Solicitudes; no se inventa un ID).
