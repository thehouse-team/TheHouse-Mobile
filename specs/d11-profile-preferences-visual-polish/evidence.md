# D11 — EVIDENCIA

## Gobernanza / recuperación D11 (2026-11-12)

- Branch: `feature/profile-preferences-ui-polish` (creada localmente ya existente en el mismo commit que main, `6ca8639`; switch preservó el working tree completo — 36 cambios: 23 borrados, 1 modificado, 12 no trackeados).
- Sin uso de `git reset/restore/clean/stash/checkout -- .` ni force push.
- Estado de entrada verificado: rama real `main`, working tree sucio, rama feature inexistente en remoto, spec D11 inexistente.

## Clasificación del working tree (sin reescribir)

### D11 legítimo (nuevo, no trackeado)
- `data/remote/api/ProfileApi.kt`, `data/remote/api/PreferencesApi.kt`
- `data/remote/dto/profile/`, `data/remote/dto/preferences/`
- `data/repository/` (`ProfileRepository`, `PreferenceRepository`)
- `domain/model/UserProfile.kt`, `domain/model/NoParams.kt`
- `domain/usecase/GetProfileUseCase.kt`, `UpdateProfileUseCase.kt`, `SelectUniversityUseCase.kt`, `GetPreferencesUseCase.kt`, `SavePreferencesUseCase.kt`, `GetQuickSetupUseCase.kt`

### D11 legítimo (modificado)
- `di/AppContainer.kt` (bloque "D11 Profile + Preferences + Quick Setup") — PERO contiene errores: import `SelectUniversityUseCase2` inexistente y `val selectUniversity` duplicado (líneas ~194 y ~227 con constructors distintos).

### Cambio ajeno / dudoso (borrados en working tree, NO descartados automáticamente)
23 archivos borrados que el código sobreviviente aún referencia (~30 dependientes): `AuthApi`, `CatalogApis`, `FavoritesApi`, `VisitsApi`, `AuthResponseDto`, `CatalogDtos`, `FavoritesDtos`, `VisitDtos`, `domain/model/Catalog`, `Favorite`, `UserSession`, `Visit`, use cases `CatalogUseCases`, `FavoritesUseCases`, `GetPropertyByIdUseCase`, `LoginUseCase`, `LogoutUseCase`, `ObserveSessionUseCase`, `RegisterUseCase`, `RestoreSessionUseCase`, `VisitValidator`, `VisitsUseCases`. Dependientes verificados por grep: `AppContainer`, `AuthRepositoryImpl`, `AuthMappers`, `InMemorySessionStore`, `AuthRepository`, `FavoritesRepository(Impl)`, `FavoritesMappers`, `VisitsRepository(Impl)`, `VisitMappers`, `VisitsRepository`, `Login/RegisterViewModel`, `Home/Explore/PropertyDetail/Favorites/Visits/ScheduleVisit/VisitDetail` (ViewModels y Screens), `U2ViewModelFactory`.
→ **Conclusión:** estas eliminaciones rompen la compilación y exceden el alcance declarado de D11 (Profile/Preferences/Quick Setup). Requieren decisión humana: restaurarlas (no se usó `git restore` por regla de seguridad) o confirmar que son intencionales con un plan de reemplazo.

## Validación real (FASE A)

- Comando: `.\gradlew.bat compileDebugKotlin` → **BUILD FAILED** (27s, 2026-11-12). Errores reales: `Unresolved reference 'Visit'/'VisitStatus'/'VisitValidator'/'CancelVisitUseCase'/'GetVisitsUseCase'` en `presentation/visits/*` (dependientes de los 23 archivos borrados en el working tree), más errores pendientes de corrección en el código D11 nuevo (`profile interface` en `ProfileApi.kt`/`PreferencesApi.kt`, `SelectUniversityUseCase2` inexistente, `val selectUniversity` duplicado en `AppContainer`).
- `testDebugUnitTest`: NO EJECUTADO (depende de compilación).
- **D11-FUNCTIONAL-READY: NO declarado. `BLOCKED` nunca equivale a `DONE`.**

## D11-VISUAL-POLISH

- Estado: PLANNED / NO EJECUTADO. Sin commits de estilo. No se declara IMPLEMENTED.
