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

## FASE B — Visual Polish (2026-11-12)

### Design system (fidelidad al prototipo)
- `ui/theme/Color.kt`: ColorScheme M3 completo extraído del prototipo autoritativo (light + dark derivado del mismo esquema; tokens: primary #00288E, primaryContainer #1E40AF, background/surface #F7F9FB, surfaceVariant #E0E3E5, outline #757684, etc.). Sin hex dispersos en pantallas (verificado: 0 coincidencias `Color(0x` en `presentation/`).
- `ui/theme/Type.kt`: escala del prototipo (headline 32/40 w800, 24/32 w700, 20/28 w600; body 16/24 w500, 14/20, 13/18; labels 14/20 w600, 12/16 w600, 11/14 w700). Fuente del sistema (sin assets externos).
- `ui/theme/Theme.kt`: light/dark schemes, `Shapes` (8/12/16/20/28dp), `dynamicColor` desactivado por fidelidad.
- `ui/theme/Dimens.kt` (nuevo): `Spacing` (4/8/16/24/32, gutter 16), `Heights` (button 48, buttonLarge 56, input 56), `Elevations` (card 2, raised 4).

### Componentes unificados
- `presentation/components/StateViews.kt` (nuevo): `LoadingView`, `EmptyStateView` (título/mensaje/CTA), `ErrorStateView` (título/mensaje/retry), stateless, con semantics y mensajes humanos (sin códigos HTTP/DomainError/stack traces).

### Pantallas pulidas (presentación, sin tocar lógica/contratos)
- `OnboardingScreen`: dots tipo prototipo (activo alargado 24x8dp, primario), CTA full-width 56dp shape medium, jerarquía headlineLarge/bodyLarge + onSurfaceVariant, Omitir con labelLarge, alturas 48/56 tokens.
- `LoginScreen` / `RegisterScreen`: CTA 56dp con loading integrado (CircularProgress en onPrimary), error con `semantics contentDescription`, TextButtons 48dp con labelLarge.
- `ExploreScreen`: estados reemplazados por StateViews (Loading "Buscando rentas…", Empty con CTA Reintentar, Error con retry); texto interno "Mapa completo pendiente de NV-07" sustituido por mensaje de usuario neutral; sin cambios de eventos/VM.
- `FavoritesScreen`: Loading/Empty (con CTA "Explorar propiedades")/Error/Login-wall via StateViews; sin cambios de callbacks.
- `PropertyCard`: precio con jerarquía `headlineSmall` + primary (ambas overloads real/prototipo); resto intacto.
- Map/PropertyDetail/Visits/Navigation: se mantienen sobre tokens M3 (BottomNav ya usa NavigationBar accesible); sin SDK de pago ni API keys (COST-001/M-MOB-017 intactos).

### Accesibilidad / responsive / performance
- Targets ≥48dp en botones/estados; `contentDescription` en estados, errores y CTA de onboarding; contraste vía tokens M3; layouts con `fillMaxWidth`/`weight` (sin tamaños rígidos); listas con `key` (Explore ya usaba `key = { it.id }`); sin requests desde Composables; sin dependencias nuevas.

### Validación real
- `.\gradlew.bat compileDebugKotlin` → **BUILD SUCCESSFUL** (19s; tras 1 ciclo correctivo: imports faltantes `semantics` en RegisterScreen y `Button` en FavoritesScreen).
- `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` → **BUILD SUCCESSFUL (1m 46s)**, 99 tests 0 fallos.
- QA runtime: **NO EJECUTADO** — sin emulador/adb disponible en el entorno (requisito condicional "si hay emulador").
- Commit: `style: polish mobile ui and unify design system`; push a `origin/feature/profile-preferences-ui-polish`.

## FASE C — UI real Profile / Preferences / Quick Setup (2026-11-19)

### Gobernanza y precheck
- Rama base: `feature/ui-redesign` (`6dd926c`). Rama de trabajo creada: `feature/profile-preferences-ui`.
- Working tree preservado: `specs/nv-13-reviews/*` modificados y `app/google-services.json` untracked (no incluidos en esta tarea).
- Alcance respetado: sin Backend, sin contratos inventados, sin Map/Roommate/Reviews, sin cambios en lógica de negocio innecesaria.

### Implementación (capas existentes + presentación nueva)
- `presentation/common/ThrowableToUiError.kt`: mapeo seguro de errores a mensajes UI (sin exponer detalles crudos).
- `presentation/profile/`: `ProfileViewModel` + `ProfileScreen`/`ProfileDestination`.
  - Datos reales: nombre y universidad (`GetProfileUseCase`/`ProfileRepository`), correo de sesión activa, estado Quick Setup.
  - Edición de nombre: `UpdateProfileUseCase` con loading/success/error.
  - Selección de universidad: `GetUniversitiesUseCase` + `SelectUserUniversityUseCase`.
  - Estados Loading/Content/Error/Retry; sin tokens/IDs técnicos.
- `presentation/preferences/`: `PreferencesViewModel` + `PreferencesScreen`/`PreferencesDestination`.
  - `GetPreferencesUseCase`/`SavePreferencesUseCase`/`PreferenceRepository` para load-save real.
  - Campos reales: presupuesto (min/max/moneda), distancia, transporte, convivencia, tipo de lugar, mascotas, servicios esenciales, tipos de propiedad.
  - Validación UI de números inválidos; Smart Match sigue consumiendo las mismas preferencias (sin duplicación).
- `presentation/quicksetup/`: `QuickSetupViewModel` + `QuickSetupScreen`/`QuickSetupDestination`.
  - Flujo breve con nombre/universidad soportados y acceso a Preferencias.
  - `GetQuickSetupUseCase`/perfil: `quickSetupCompleted` tratado como flag de solo lectura (no se fuerza ni se inventa completitud).
- Navegación/DI:
  - `AppRoutes`: `profile`, `preferences`, `quickSetup`.
  - `U2ViewModelFactory`: creación de Profile/Preferences/QuickSetup ViewModels con dependencias existentes.
  - `MainActivity`: destinos autenticados y rutas reales; Smart Match `NoPreferences` navega a Preferencias.
  - `HomeScreen`: accesos "Mi perfil", "Preferencias", "Configuración inicial" en cuenta autenticada.

### Tests añadidos
- `presentation/profile/ProfileViewModelTest.kt`: load success/error, actualización de nombre, selección de universidad.
- `presentation/preferences/PreferencesViewModelTest.kt`: load, save, validación numérica, error.
- `presentation/quicksetup/QuickSetupViewModelTest.kt`: completed/not completed, save, error/state.

### Gates (ejecutados)
- `.\gradlew.bat testDebugUnitTest` → **BUILD SUCCESSFUL** (47s, 28 tareas: 7 ejecutadas, 21 up-to-date, configuration cache reutilizada).
- `.\gradlew.bat lintDebug` → **BUILD SUCCESSFUL** (2m 3s, reportes generados en `app/build/reports/lint-results-debug.html` y `.sarif`).
- `.\gradlew.bat assembleDebug` → **BUILD SUCCESSFUL** (20s).

### Runtime (parcial)
- `emulator-5554` detectado (`adb devices`: device). APK debug instalado y app iniciada (`com.thehouse.app/.MainActivity` resumida) sin crash observado.
- Estado de UI detectado: pantalla no autenticada ("¿Ya tienes cuenta? Inicia sesión"). Sin credenciales/sesión autenticada disponible, no fue posible ejercer end-to-end los flujos protegidos de Profile, Preferences y Quick Setup (rutas autenticadas) ni validar carga/guardado contra backend.
- Resultado: **runtime parcial** (arranque/instalación OK; validación funcional autenticada pendiente de credenciales/sesión).

### Git (pendiente de commit en este paso)
- `git diff --check` sin errores en archivos de esta tarea. Se excluirán `specs/nv-13-reviews/*` y `app/google-services.json` del commit. Push previsto a `origin feature/profile-preferences-ui`, sin merge.
