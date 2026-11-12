# Plan D12 — Modo arrendador, edificios y cuartos

Base: `feature/requests-real-chat` (contiene D3–D11; `main` aún no tiene el merge de D9.1/D11 — desviación documentada en `evidence.md`).

## Fase A — Contrato y Data
- A1. `LandlordApi` + `LandlordDtos` (DTOs exactos del contrato §2 de la spec).
- A2. Modelos de dominio, enums exactos y mappers.
- A3. `LandlordRepository` + `LandlordRepositoryImpl` (ProblemDetails → `LandlordException` con `LandlordErrorCode`; `Retry-After`).

## Fase B — Casos de uso y ViewModels
- B1. Use cases (perfil, edificios CRUD, rooms generate/CRUD, pause/resume, publication/publish).
- B2. ViewModels con estados Loading/Empty/Content/Saving/Success/Error, doble-submit bloqueado, manejo 409 `version_conflict` y 403 `landlord_required`.

## Fase C — UI
- C1. Pantallas: alta de arrendador, dashboard/listado, crear/editar edificio, detalle edificio + cuartos, crear/editar cuarto, publicación (pasos + enviar a revisión).
- C2. Navegación: entrada en Home; rutas landlord; no romper D3–D11.
- C3. Design System D11 (StateViews, Spacing, Heights, semántica, accesibilidad).

## Fase D — Validación
- D1. Tests de repository (MockWebServer) y regresión D3–D11.
- D2. `compileDebugKotlin` → `testDebugUnitTest lintDebug assembleDebug`.
- D3. Runtime contra Backend desplegado (si hay credenciales/emulador) y evidencia.
