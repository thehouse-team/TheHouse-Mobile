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

### Primer intento (2026-11-12)
- `.\gradlew.bat compileDebugKotlin` → **BUILD FAILED** (27s). Causas: los 23 archivos core borrados en el working tree (dependientes en `presentation/visits/*`, `AppContainer`, auth/favorites) + errores del código D11 nuevo.

### Recuperación y corrección D11 (2026-11-12, autorización humana FASE A)
- **Restauración selectiva** de los 23 archivos core con `git restore --source=HEAD -- <archivo>` uno por uno (nunca `git restore .`). Tipos verificados tras restaurar: `SessionState`, `UserSession`, `Property`, `University`, `Favorite`, `PagedResult`, `Visit`, `VisitStatus`, `VisitValidator`, `CancelVisitUseCase`, `GetVisitsUseCase` (grep, 10/11 coincidencias directas + SessionState en AuthRepository).
- **Correcciones D11 (solo archivos D11):**
  - `ProfileApi.kt` / `PreferencesApi.kt`: `profile interface` → `interface`; rutas con prefijo `api/v1/` alineadas al patrón existente (VisitsApi/FavoritesApi/AuthApi); PATCH/PUT con cuerpos tipados mínimos (`PatchProfileRequest{name}`, `PutUniversityRequest{universityId}`) — no se inventaron campos ni enums.
  - `AppContainer.kt`: eliminado import `SelectUniversityUseCase2` (no existe, no se creó); eliminada la declaración duplicada de `val selectUniversity` (se conserva la existente sobre `SelectedUniversityRepository`); la selección de universidad del perfil se expone como `selectUserUniversity` (clase `SelectUserUniversityUseCase`, renombrada para evitar redeclaración con `CatalogUseCases.kt`); imports añadidos (`ProfileApi`, `PreferencesApi`); repos wired con `ProfileRepositoryImpl`/`PreferenceRepositoryImpl`.
  - Eliminado el archivo duplicado `domain/usecase/SelectUniversityUseCase.kt` (redeclaraba la clase de `CatalogUseCases.kt` en el mismo paquete).
  - `domain/repository/ProfileRepository.kt` y `PreferenceRepository.kt` creados (contratos Domain; Data implementa, Clean Architecture).
  - `data/repository/ProfileRepository.kt`: clase renombrada a `ProfileRepositoryImpl`; `javax.inject.Inject` eliminado (DI manual, sin Hilt); `MutableFlow`/`asFlow` inexistentes → `MutableStateFlow`/`asStateFlow`; mapeo `dto.university?.toDomain()` resuelto con extensiones en el archivo de DTO.
  - `data/repository/PreferenceRepository.kt`: ídem (`PreferenceRepositoryImpl`); `PreferencesDto.toDomain(dto)` → extensión `dto.toDomain()`; imports de `PreferencesPatchBody`/`BudgetPatchBody` añadidos.
  - `ProfileDto.kt`/`PreferencesDto.kt`: eliminada herencia inválida (`: UserProfile?`, `: Preference?` — un data class no extiende un tipo nullable); eliminado `object PreferencesDto` (redeclaración con la data class); extensiones `toDomain()` top-level.
  - `domain/model/UserProfile.kt`: eliminado el hack `: Cloneable`/`clone()`.
  - `domain/model/Preference.kt` creado (`Preference`, `Budget`) — campos espejo del DTO existente, sin enums inventados.
  - Use cases D11 normalizados al patrón standalone existente (`suspend operator fun invoke`, sin base class), dependiendo de contratos Domain; `NoParams.kt` corregido (faltaban paréntesis del data class).
  - `GET /api/v1/universities/search`: no existe en el código ni es referenciado; NO se inventó (contrato no confirmado).
- **Validación final:**
  - `.\gradlew.bat compileDebugKotlin` → **BUILD SUCCESSFUL (44s)**.
  - `.\gradlew.bat testDebugUnitTest` → **BUILD SUCCESSFUL (22s): 99 tests, 0 fallos (23 suites)**. Sin borrar/relajar tests.
- **Commit:** `feat: add real profile preferences and quick setup` (código funcional D11). **D11-FUNCTIONAL-READY declarado.**

## D11-VISUAL-POLISH

- Estado: PLANNED / NO EJECUTADO. Sin commits de estilo. No se declara IMPLEMENTED.
