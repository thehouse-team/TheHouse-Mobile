# Evidencia D12 — Modo arrendador, edificios y cuartos

Fecha: 2026-11-12 · Rama: `feature/landlord-buildings-rooms`

## Contract evidence
- Fuente: `TheHouse-Backend/specs/nv-12-landlord-buildings-rooms/spec.md` v1.1.0 (leída en `origin/main` vía `git show`; el checkout local del Backend está en `feature/u2-entrega` y no contiene el directorio).
- Contrato real extraído: 17 endpoints `/api/v1/landlord/*`, enums y validaciones exactas, `version` obligatoria en PATCH (cuerpo) y DELETE (query), 409 `version_conflict`, 403 `landlord_required`, publish → 202 `in_review`, `acceptedTermsVersion` = `2026-11` (`LandlordTerms:Version`).
- Sin endpoints/campos/estados inventados. Sin upload multimedia (NV-12 no lo incluye; D13).

## Implementation
- Api/DTO/mapper/repository/use cases: `LandlordApi.kt`, `LandlordDtos.kt`, `LandlordMappers.kt`, `LandlordRepositoryImpl.kt`, `LandlordUseCases.kt`, `domain/model/Landlord.kt`, `domain/error/LandlordError.kt`.
- ViewModels/pantallas: `LandlordDashboardViewModel/Screen` (incluye alta de arrendador), `BuildingFormViewModel/Screen`, `BuildingDetailViewModel/Screen` (publicación, pausar/reanudar, eliminar), `RoomFormViewModel/Screen` (generate/crear, editar, eliminar), `LandlordErrorMessages.kt`.
- Navegación: `LandlordRoutes.kt`, rutas en `MainActivity.kt`, entrada «Mis alojamientos (arrendador)» en `HomeScreen.kt`. DI en `AppContainer.kt` + `U2ViewModelFactory.kt`. Sin segundo Retrofit; reutiliza Bearer/authenticatore/ProblemDetails/Design System D11.
- Concurrencia: `version` guardada del Backend y enviada en cada PATCH/DELETE; tras éxito se refresca; 409 `version_conflict` → mensaje y «Recargar datos», sin retry automático.
- `duplicate`/`copy-from` implementados en Api/Repository/contrato (fuera de UI): deuda documentada en spec §5.

## Commands and results
- `.\gradlew.bat compileDebugKotlin` → OK (1er ciclo correctivo: imports/rutas/tipos).
- `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` → **BUILD SUCCESSFUL** (2ª pasada; 1ª falló por nullable `body` en MockWebServer, corregido).
- Tests D12: `LandlordRepositoryIntegrationTest` **25/25** (landlord_required, version_conflict, publication_incomplete, buildings_limit, room_state, room_number_taken, 400/401/404/429/503, profile 201/404).
- Regresión D3–D11: suite completa verde (0 failures; lint 0 errores, 20 warnings preexistentes).
- `git status` limpio tras commit; `git diff --check` sin conflictos de espacio.

## Runtime (BLOCKED)
- Backend desplegado disponible (`https://ca-thehouse-u2...`), pero **sin emulador/dispositivo (adb no instalado) y sin credenciales de prueba autorizadas** → no se ejecutó prueba HTTP real desde la app. Causa exacta: entorno sin ADB y sin datos de arrendador de prueba; no se inventan credenciales.

## Blockers / deuda
- Runtime pendiente (requiere emulador + cuenta de prueba).
- UI de `duplicate`/`copy-from` pendiente (contrato implementado).
- Base de la rama: `feature/requests-real-chat` (main aún no mergea D9.1/D11; al hacer merge, resolver trivial).
