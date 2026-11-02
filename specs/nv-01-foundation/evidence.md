# NV-01 — EVIDENCIA DE EJECUCIÓN

| Campo | Valor |
|---|---|
| Spec | NV-01 Foundation |
| Plan | NV01-PLAN-1.0 |
| Fecha de ejecución | 2026-10-31 |
| Estado | ✅ EJECUTADA / VALIDADA |
| Módulo | `:app` |
| Tasks | 30/30 |

> Este documento registra evidencia real por task. No sustituye `task.md`; lo complementa.

---

## 1. Resumen de ejecución

Se implementó la foundation arquitectónica mínima de TheHouse Mobile dentro de `:app`, sin módulos Gradle adicionales, sin funcionalidades de negocio, sin endpoints, sin Room, sin workers funcionales y sin integraciones externas.

```text
Presentation → Domain
Data         → Domain
Domain       → independiente
```

La dirección de mapeo `Data ↔ Domain` se usó únicamente como transformación de modelos. No se creó dependencia `Domain → Data`.

---

## 2. Inventario y mapa de paquetes (NV01-T001)

### Estado inicial encontrado

```text
app/src/main/java/com/thehouse/app/
├── MainActivity.kt
└── ui/theme/
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

- `MainActivity.kt` contenía únicamente el saludo placeholder del proyecto.
- `ui/theme/` contenía el theme Compose existente.
- No existían paquetes `presentation`, `domain`, `data` ni `di`.
- No existían módulos Gradle adicionales.
- El baseline `assembleDebug` era exitoso antes de iniciar NV-01.

### Mapa final

```text
app/src/main/java/com/thehouse/app/
├── MainActivity.kt
├── TheHouseApplication.kt
├── di/
│   └── AppContainer.kt
├── domain/
│   ├── common/
│   │   └── DispatcherProvider.kt
│   └── error/
│       └── DomainError.kt
├── data/
│   ├── config/
│   │   ├── AppConfig.kt
│   │   └── AppConfigProvider.kt
│   ├── dispatcher/
│   │   └── DefaultDispatcherProvider.kt
│   ├── error/
│   │   ├── DataError.kt
│   │   └── DataErrorMapper.kt
│   ├── image/
│   │   └── ImageLoaderFactory.kt
│   ├── mapper/
│   │   └── Mapper.kt
│   ├── preferences/
│   │   ├── AppPreferencesDataStore.kt
│   │   ├── DataStorePreferencesStore.kt
│   │   └── PreferencesStore.kt
│   ├── remote/
│   │   ├── HttpClientFactory.kt
│   │   ├── NetworkTimeouts.kt
│   │   ├── RetrofitFactory.kt
│   │   └── SafeHttpLoggingInterceptor.kt
│   └── serialization/
│       └── JsonProvider.kt
└── presentation/
    ├── common/
    │   ├── BaseViewModel.kt
    │   ├── DomainErrorToUiError.kt
    │   ├── UiEffect.kt
    │   ├── UiError.kt
    │   ├── UiEvent.kt
    │   └── UiState.kt
    └── navigation/
        ├── AppNavHost.kt
        ├── AppNavigator.kt
        └── AppRoute.kt
```

No se movieron `MainActivity.kt` ni `ui/theme/` porque no era necesario para la foundation. `MainActivity` conserva el saludo placeholder existente y no se convirtió en pantalla de negocio.

---

## 3. Convención de paquetes y dependencias (NV01-T002)

### Boundaries

```text
presentation → domain
data         → domain
domain       → (sin dependencias de capas)
di           → presentation, domain, data
```

`di` es el composition root y no es una capa de negocio.

### Regla explícita

```text
Presentation puede depender de Domain.
Data puede depender de Domain.

Domain no depende de Presentation.
Domain no depende de Data.
```

### Dependencias prohibidas en Domain

```text
Android UI
Jetpack Compose
Navigation
Retrofit
OkHttp
Room
DataStore
WorkManager
Firebase
Google Maps
```

### Dependencias permitidas en Domain

- Kotlin stdlib.
- Kotlin Coroutines (`kotlinx.coroutines`).
- Contratos y modelos propios de Domain.

### Mapeo Data ↔ Domain

`Data ↔ Domain` solo describe transformación de modelos:

```text
DTO → Domain
Domain → DTO
Entity → Domain
Domain → Entity
```

No implica dependencia bidireccional. La dependencia arquitectónica permanece `Data → Domain`; `Domain → Data` está prohibido.

---

## 4. Checklist de imports y boundaries (NV01-T003)

Checklist verificado:

- ✅ Ningún archivo de `domain/` importa `android.*`, `androidx.compose.*`, `androidx.navigation.*`, `retrofit2.*`, `okhttp3.*`, `androidx.room.*`, `androidx.datastore.*`, `androidx.work.*`, `com.google.firebase.*` ni `com.google.android.gms.maps.*`.
- ✅ Ningún archivo de `domain/` importa `com.thehouse.app.data.*` ni `com.thehouse.app.presentation.*`.
- ✅ Ningún archivo de `data/` importa `com.thehouse.app.presentation.*`.
- ✅ Ningún archivo de `presentation/` importa `com.thehouse.app.data.*`.
- ✅ `di/` puede importar todas las capas porque es el composition root.
- ✅ La UI no consume Retrofit, Room, DataStore, WorkManager, Firebase ni Maps directamente.
- ✅ Los DTO y Room Entities no aparecen en `domain/` ni `presentation/`.
- ✅ No existe ningún import de `com.thehouse.app.data` en `domain/`.

Comando de verificación usado:

```powershell
Get-ChildItem app/src/main/java/com/thehouse/app/domain -Recurse -Filter *.kt |
  Select-String -Pattern '^import\s+(android\.|androidx\.|retrofit2|okhttp3|com\.thehouse\.app\.data|com\.thehouse\.app\.presentation)'
```

Resultado real: `NONE`.

---

## 5. Convenciones de configuración y build (NV01-T004)

- No se versionan secretos.
- `BuildConfig` solo contiene valores no sensibles:
  - `API_BASE_URL = "https://example.invalid/"` (placeholder reservado, no productivo).
  - `ENABLE_HTTP_LOGGING = true` en debug y `false` en release.
- No se modifican AGP, Gradle, Kotlin, SDK, Java ni `applicationId`.
- No se agregan servicios externos ni claves.
- `local.properties` contiene únicamente `sdk.dir`; no contiene credenciales.

---

## 6. Naming y organización de archivos (NV01-T005)

- Paquetes en minúsculas: `presentation`, `domain`, `data`, `di`.
- Clases y objetos en PascalCase.
- Funciones y propiedades en camelCase.
- Un archivo por declaración pública principal.
- Sufijos aplicados:
  - `*ViewModel`
  - `*UiState`
  - `*UiEvent`
  - `*UiEffect`
  - `*Repository` para contratos futuros de Domain
  - `*RepositoryImpl` para implementaciones futuras de Data
  - `*DataSource` para fuentes de datos
  - `*Mapper` para transformaciones
  - `*Factory` para construcción técnica
  - `*Provider` para configuración/proveedores
  - `*Store` para boundaries de almacenamiento
- No se crearon archivos vacíos ni clases sin propósito.

---

## 7. Verificación de módulos Gradle (NV01-T006)

```text
Módulos Gradle: :app únicamente
Módulos domain/data/presentation/core: NO creados
```

`settings.gradle.kts` mantiene:

```kotlin
include(":app")
```

---

## 8. Fase F2 — Presentation, MVVM y UDF (NV01-T007 → NV01-T012)

### NV01-T007 — Convención de ViewModel y lifecycle

- Archivo: `presentation/common/BaseViewModel.kt`.
- `BaseViewModel` extiende `ViewModel`, expone `StateFlow` y usa `viewModelScope` mediante `launch`.
- No contiene referencias a `Context`, Composable, Activity ni vistas.
- `onCleared()` cierra el canal de efectos one-shot.

### NV01-T008 — Contrato de dependencias del ViewModel

- Los ViewModels futuros reciben dependencias por constructor.
- Pueden depender de contratos de Domain, `DispatcherProvider` y modelos de Presentation.
- No pueden depender de Retrofit, OkHttp, Room, DataStore, WorkManager, Firebase, Maps, Compose ni tipos de UI Android.
- El composition root `di/AppContainer.kt` construye las implementaciones.

### NV01-T009 — Contrato UDF de UI Event, estado y efectos one-shot

- Archivos: `UiEvent.kt`, `UiEffect.kt`, `BaseViewModel.kt`.
- Flujo definido:

```text
UI Event → ViewModel → Use Case / Repository → StateFlow → UI State → Compose
```

- Los eventos se atienden en `onEvent`.
- Los efectos one-shot se emiten por `Channel` y se exponen como `Flow`.

### NV01-T010 — Modelo de UI State

- Archivos: `UiState.kt`, `UiError.kt`.
- Estados: `Loading`, `Content<T>`, `Empty`, `Error`.
- `Error` transporta `UiError`, no excepciones crudas.

### NV01-T011 — Flow y lifecycle

- `BaseViewModel` expone `StateFlow<State>` inmutable mediante `asStateFlow()`.
- Convención para Compose: recolectar con `collectAsStateWithLifecycle`.
- No se creó ninguna pantalla que recolecte flows directamente.

### NV01-T012 — Navigation foundation

- Archivos: `AppRoute.kt`, `AppNavigator.kt`, `AppNavHost.kt`.
- `AppRoute` es un contrato de ruta tipada.
- `AppNavigator` encapsula `NavHostController`.
- `AppNavHost` es un wrapper reusable de `NavHost`.
- Destinos funcionales de negocio: `0`.
- Pantallas de negocio: `0`.

---

## 9. Fase F3 — Data boundaries (NV01-T013 → NV01-T016)

### NV01-T013 — Contrato de repositorio y límite de Room

- Convención definida:
  - Los contratos de repositorio futuros vivirán en `domain/repository/`.
  - Las implementaciones vivirán en `data/repository/`.
  - Room solo puede implementar contratos de Domain desde Data.
  - Domain no conoce `@Entity`, `@Dao`, `RoomDatabase` ni tipos de Room.
- No se crearon repositorios genéricos ni ficticios porque aún no existe una entidad de dominio real.
- Room runtime está instalado, pero no se crearon entidades, DAO ni base de datos.
- KSP permanece `NO CONFIGURADO`.

### NV01-T014 — Límite de DataStore

- Archivos: `AppPreferencesDataStore.kt`, `PreferencesStore.kt`, `DataStorePreferencesStore.kt`.
- `PreferencesStore` es la interfaz de boundary.
- `DataStorePreferencesStore` encapsula DataStore Preferences.
- Solo preferencias no sensibles; no tokens, credenciales ni PII.
- La UI no accede directamente a DataStore.

### NV01-T015 — Límite de WorkManager

- Convención definida:
  - Workers funcionales futuros vivirán en `data/work/`.
  - Solo podrán depender de contratos de Domain.
  - No se crean workers ni sincronización antes de que exista dominio real.
- Workers funcionales creados: `0`.
- Sincronización funcional creada: `0`.
- WorkManager runtime está instalado, pero no se usa todavía.

### NV01-T016 — Convención de mapeo Data ↔ Domain

- Fase: F3 / Requisito: NV01-REQ-012.
- Archivo: `data/mapper/Mapper.kt`.
- `Mapper<From, To>` es un contrato de transformación.
- `DataErrorMapper` transforma errores técnicos de Data a errores de Domain.
- El mapeo puede ser bidireccional como transformación de modelos, pero la dependencia arquitectónica permanece `Data → Domain`.

---

## 10. Fase F4 — Infraestructura base (NV01-T017 → NV01-T021)

### NV01-T017 — Retrofit/OkHttp sin endpoints

- Archivos: `HttpClientFactory.kt`, `RetrofitFactory.kt`, `NetworkTimeouts.kt`.
- `HttpClientFactory` configura timeouts y logging condicional.
- `RetrofitFactory` configura Retrofit con Kotlin Serialization.
- URL base: placeholder `https://example.invalid/`.
- Endpoints funcionales: `0`.
- Servicios Retrofit creados: `0`.
- Requests reales: `0`.

### NV01-T018 — Kotlin Serialization y DTO

- Archivo: `JsonProvider.kt`.
- Configuración: `ignoreUnknownKeys = true`, `encodeDefaults = true`, `isLenient = false`.
- No se crearon DTOs de negocio.
- El mapeo DTO → Domain queda como convención para specs funcionales.

### NV01-T019 — Boundary de Coil

- Archivo: `ImageLoaderFactory.kt`.
- Construye un `ImageLoader` con `OkHttpNetworkFetcherFactory`.
- No crea pantallas ni componentes de negocio.
- La UI futura consumirá un `ImageLoader` controlado desde el composition root.

### NV01-T020 — Timeouts, errores HTTP y cancelación

- `NetworkTimeouts` define connect/read/write/call positivos.
- `HttpClientFactory` aplica los timeouts a OkHttp.
- `DataError.Http` representa errores HTTP técnicos.
- `DataErrorMapper` traduce códigos HTTP a errores semánticos de Domain.
- La cancelación se propaga mediante Coroutines; no se usa bloqueo de hilo principal.

### NV01-T021 — Logging seguro

- Archivo: `SafeHttpLoggingInterceptor.kt`.
- Registra únicamente método, host, status y duración.
- No registra headers, bodies, paths, query params, tokens, credenciales ni PII.
- Solo se agrega cuando `ENABLE_HTTP_LOGGING` es `true` (debug).
- Release: logging HTTP deshabilitado.

---

## 11. Fase F5 — Cross-cutting y DI (NV01-T022 → NV01-T025)

### NV01-T022 — Dispatchers y cancelación

- Archivos: `domain/common/DispatcherProvider.kt`, `data/dispatcher/DefaultDispatcherProvider.kt`.
- `DispatcherProvider` es un contrato de Domain para `io` y `default`.
- `DefaultDispatcherProvider` implementa con `Dispatchers.IO` y `Dispatchers.Default`.
- `BaseViewModel.launch` usa `viewModelScope`.
- `GlobalScope`: `0` ocurrencias.

### NV01-T023 — Decisión de DI

Estrategias evaluadas:

| Estrategia | Evaluación |
|---|---|
| Manual DI | Suficiente para la foundation actual; sin codegen, sin dependencias nuevas, testeable y explícita. |
| Hilt | Poderoso, pero introduce plugin, KSP/codegen y complejidad prematura sin features ni entidades reales. |
| Koin | DSL liviano, pero agrega dependencia y un contenedor global que no se necesita todavía. |

Decisión: **Manual DI** mediante `di/AppContainer.kt` y `TheHouseApplication`.

- Framework instalado: `NO`.
- `AppContainer` construye `Json`, `OkHttpClient`, `Retrofit`, `ImageLoader`, `PreferencesStore` y `DispatcherProvider`.
- Las dependencias se inyectan por constructor.
- Registro en `memoria.md`: `M-MOB-038`.

### NV01-T024 — Proveedor de configuración no secreta

- Archivos: `data/config/AppConfig.kt`, `data/config/AppConfigProvider.kt`.
- `AppConfig` valida HTTPS y trailing slash.
- `AppConfigProvider` lee `BuildConfig.API_BASE_URL` y `BuildConfig.ENABLE_HTTP_LOGGING`.
- No contiene secretos.

### NV01-T025 — Build types y entornos

- `debug`: `ENABLE_HTTP_LOGGING = true`.
- `release`: `ENABLE_HTTP_LOGGING = false`.
- `API_BASE_URL` es el mismo placeholder no productivo en ambos build types.
- `assembleRelease` validado correctamente.
- Artefacto release generado: `app/build/outputs/apk/release/app-release-unsigned.apk` (2,124,551 bytes).
- Toolchain sin cambios: AGP `9.5.0`, Gradle `9.6.0`, Kotlin `2.2.10`, compileSdk/targetSdk `37`, minSdk `26`, Java `17`, `applicationId = com.thehouse.app`.

---

## 12. Fase F6 — Errores, testing y validación (NV01-T026 → NV01-T030)

### NV01-T026 — Modelo de error de dominio y mapeo desde Data

Cadena implementada:

```text
DataError
   ↓
DomainError
   ↓
UiError
   ↓
UiState.Error
```

- `DataError`: Network, Timeout, Http(statusCode), Serialization, Local, Unknown.
- `DomainError`: Network, Timeout, Unauthorized, NotFound, Conflict, RateLimited, Server, Unknown.
- `UiError`: versión segura para Presentation.
- No se inventaron errores específicos de funcionalidades inexistentes.

### NV01-T027 — Costuras de testing, fakes y puntos de inyección

Costuras creadas:

- `PreferencesStore` como interfaz de boundary.
- `DispatcherProvider` como contrato inyectable.
- `AppContainer` como composition root con constructor injection.
- `BaseViewModel` como base testeable de estado/efectos.
- `Mapper` como contrato de transformación.

Fakes de repositorio no creados todavía porque no existen contratos de repositorio ni entidades reales. Se crearán con la primera spec funcional que los necesite.

Tests unitarios creados:

- `DataErrorMapperTest`
- `AppConfigTest`
- `NetworkTimeoutsTest`
- `NetworkingFoundationTest`
- `JsonProviderTest`
- `MapperTest`
- `BaseViewModelTest`
- `DomainErrorToUiErrorTest`
- `ExampleUnitTest` (preexistente)

Resultado real: 16 tests, 0 failures, 0 errors, 0 skipped.

### NV01-T028 — Build, tests y lint

Comandos ejecutados y resultados reales:

```powershell
.\gradlew.bat clean assembleDebug
# BUILD SUCCESSFUL in 12s
# 39 actionable tasks: 39 executed
```

```powershell
.\gradlew.bat testDebugUnitTest
# BUILD SUCCESSFUL in 6s
# 9 test suites, 16 tests, 0 failures, 0 errors, 0 skipped
```

```powershell
.\gradlew.bat lintDebug
# BUILD SUCCESSFUL in 15s
# 0 errors, 15 warnings
```

Validación adicional:

```powershell
.\gradlew.bat assembleRelease
# BUILD SUCCESSFUL in 1m 24s (primera ejecución)
# BUILD SUCCESSFUL in 1m 2s (regeneración posterior)
# Warning: unable to strip libandroidx.graphics.path.so y libdatastore_shared_counter.so; se empaquetan sin strip.
# Artefacto: app/build/outputs/apk/release/app-release-unsigned.apk
```

Las 15 warnings de lint son preexistentes y no provienen del código nuevo de NV-01:

- 1 `RedundantLabel` en el manifest existente.
- 2 `GradleDependency` por versiones de AndroidX Test.
- 5 `NewerVersionAvailable` por Kotlin, Serialization y Coil.
- 7 `UnusedResources` en `colors.xml`.

No se ocultaron warnings ni fallos.

### NV01-T029 — Trazabilidad, evidencia y memoria

- Este documento registra la evidencia por task.
- `task.md` marca las 30 tasks como completadas.
- `spec.md` y `plan.md` actualizan su estado a ejecutada/validada.
- `memoria.md` registra `M-MOB-038`, `M-MOB-039` y `M-MOB-040`.

### NV01-T030 — Auditoría final de alcance

```text
Funcionalidades de negocio:       0
Pantallas funcionales:            0
Endpoints funcionales:            0
Room entities:                    0
DAO:                              0
Room Database:                    0
Workers funcionales:              0
Firebase / FCM:                   0
Google Maps:                      0
ML / IA generativa:               0
Módulos Gradle adicionales:       0
Secretos:                         0
KSP:                              NO CONFIGURADO
```

---

## 13. Matriz de evidencia por task

| Task | Requirement | Archivos | Cambios | Comandos | Resultado | Riesgos |
|---|---|---|---|---|---|---|
| NV01-T001 | NV01-REQ-001 | `evidence.md` | Inventario y mapa de paquetes | `Get-ChildItem app/src/main/java/com/thehouse/app -Recurse -File` | Mapa documentado | Ninguno |
| NV01-T002 | NV01-REQ-001 | `evidence.md`, paquetes nuevos | Convención de paquetes y dependencias | `Get-ChildItem ...` | Convención documentada | Ninguno |
| NV01-T003 | NV01-REQ-002 | `evidence.md` | Checklist de imports | `Select-String -Pattern '^import...'` | `NONE` en violaciones | Ninguno |
| NV01-T004 | NV01-REQ-017 | `app/build.gradle.kts`, `AppConfig.kt`, `AppConfigProvider.kt` | Configuración no secreta | `.\gradlew.bat assembleDebug` | BUILD SUCCESSFUL | Sin secretos |
| NV01-T005 | NV01-REQ-001 | `evidence.md` | Convención de naming | `Get-ChildItem ...` | Documentada | Ninguno |
| NV01-T006 | NV01-REQ-001 | `settings.gradle.kts` | Verificación de módulos | `Select-String 'include\('` | Solo `include(":app")` | Ninguno |
| NV01-T007 | NV01-REQ-003 | `BaseViewModel.kt` | Base de ViewModel/lifecycle | `.\gradlew.bat testDebugUnitTest` | 16 tests OK | Ninguno |
| NV01-T008 | NV01-REQ-003 | `BaseViewModel.kt`, `AppContainer.kt` | Contrato de dependencias | `Select-String '^import'` | Sin infraestructura en Presentation | Ninguno |
| NV01-T009 | NV01-REQ-004 | `UiEvent.kt`, `UiEffect.kt`, `BaseViewModel.kt` | Contrato UDF | `BaseViewModelTest` | OK | Ninguno |
| NV01-T010 | NV01-REQ-005 | `UiState.kt`, `UiError.kt` | Estados loading/content/empty/error | `.\gradlew.bat testDebugUnitTest` | OK | Ninguno |
| NV01-T011 | NV01-REQ-007 | `BaseViewModel.kt` | StateFlow y lifecycle | `Select-String 'StateFlow'` | Convención aplicada | Solo convención, sin pantalla |
| NV01-T012 | NV01-REQ-008 | `AppRoute.kt`, `AppNavigator.kt`, `AppNavHost.kt` | Base Navigation Compose | `.\gradlew.bat assembleDebug` | 0 destinos funcionales | Ninguno |
| NV01-T013 | NV01-REQ-012 | `evidence.md` | Convención repository/Room | `Select-String '@Entity\|@Dao\|@Database\|RoomDatabase'` | `NONE` | Contratos diferidos a spec funcional |
| NV01-T014 | NV01-REQ-013 | `PreferencesStore.kt`, `DataStorePreferencesStore.kt`, `AppPreferencesDataStore.kt` | Boundary DataStore | `.\gradlew.bat assembleDebug` | Compila | Solo preferencias no sensibles |
| NV01-T015 | NV01-REQ-014 | `evidence.md` | Convención WorkManager | `Select-String 'androidx.work'` | `NONE` | Workers diferidos |
| NV01-T016 | NV01-REQ-012 | `Mapper.kt`, `DataErrorMapper.kt` | Mapeo Data ↔ Domain | `MapperTest`, `DataErrorMapperTest` | OK | Mapeo no implica dependencia |
| NV01-T017 | NV01-REQ-010 | `HttpClientFactory.kt`, `RetrofitFactory.kt`, `NetworkTimeouts.kt` | Base Retrofit/OkHttp | `NetworkingFoundationTest` | OK, sin requests | URL placeholder |
| NV01-T018 | NV01-REQ-011 | `JsonProvider.kt` | Configuración Kotlin Serialization | `JsonProviderTest` | OK | Sin DTOs de negocio |
| NV01-T019 | NV01-REQ-015 | `ImageLoaderFactory.kt` | Boundary Coil | `.\gradlew.bat assembleDebug` | Compila | Sin pantallas |
| NV01-T020 | NV01-REQ-010 | `NetworkTimeouts.kt`, `HttpClientFactory.kt`, `DataError.kt` | Timeouts y errores HTTP | `NetworkTimeoutsTest`, `DataErrorMapperTest` | OK | Ninguno |
| NV01-T021 | NV01-REQ-010 | `SafeHttpLoggingInterceptor.kt` | Logging seguro | `Select-String 'logger\('` | Sin headers/bodies/paths | Solo debug |
| NV01-T022 | NV01-REQ-006 | `DispatcherProvider.kt`, `DefaultDispatcherProvider.kt`, `BaseViewModel.kt` | Dispatchers y cancelación | `Select-String 'GlobalScope'` | `NONE` | Ninguno |
| NV01-T023 | NV01-REQ-016 | `AppContainer.kt`, `TheHouseApplication.kt`, `memoria.md` | DI manual | `.\gradlew.bat assembleDebug` | Compila; sin Hilt/Koin | Reevaluar con features |
| NV01-T024 | NV01-REQ-017 | `AppConfig.kt`, `AppConfigProvider.kt`, `app/build.gradle.kts` | Configuración no secreta | `AppConfigTest` | OK | Sin secretos |
| NV01-T025 | NV01-REQ-017 | `app/build.gradle.kts` | Build types debug/release | `.\gradlew.bat assembleRelease` | BUILD SUCCESSFUL | APK unsigned, no publicado |
| NV01-T026 | NV01-REQ-009 | `DomainError.kt`, `DataError.kt`, `DataErrorMapper.kt`, `UiError.kt`, `DomainErrorToUiError.kt` | Cadena de errores | `DataErrorMapperTest`, `DomainErrorToUiErrorTest` | OK | Ninguno |
| NV01-T027 | NV01-REQ-018 | `PreferencesStore.kt`, `DispatcherProvider.kt`, `AppContainer.kt`, tests | Costuras e inyección | `.\gradlew.bat testDebugUnitTest` | 16 tests OK | Fakes diferidos |
| NV01-T028 | Validación global | Código y configuración | Build/tests/lint | `clean assembleDebug`, `testDebugUnitTest`, `lintDebug` | Build OK; 16 tests; 0 lint errors | 15 warnings preexistentes |
| NV01-T029 | Trazabilidad | `evidence.md`, `task.md`, `spec.md`, `plan.md`, `memoria.md` | Evidencia y estados | `Select-String` de conteos | 18 req / 6 fases / 30 tasks | `AGENTS.md` pendiente |
| NV01-T030 | Auditoría global | `evidence.md`, código | Auditoría de alcance | `Select-String` de marcadores prohibidos | 0 funcionalidades/integraciones | Ninguno |

---

## 14. Archivos modificados y creados

### Configuración y manifest

- `app/build.gradle.kts` (modificado: `buildConfig`, `API_BASE_URL`, `ENABLE_HTTP_LOGGING`).
- `app/src/main/AndroidManifest.xml` (modificado: `android:name=".TheHouseApplication"`).

### Código de producción

- `app/src/main/java/com/thehouse/app/TheHouseApplication.kt`
- `app/src/main/java/com/thehouse/app/di/AppContainer.kt`
- `app/src/main/java/com/thehouse/app/domain/common/DispatcherProvider.kt`
- `app/src/main/java/com/thehouse/app/domain/error/DomainError.kt`
- `app/src/main/java/com/thehouse/app/data/config/AppConfig.kt`
- `app/src/main/java/com/thehouse/app/data/config/AppConfigProvider.kt`
- `app/src/main/java/com/thehouse/app/data/dispatcher/DefaultDispatcherProvider.kt`
- `app/src/main/java/com/thehouse/app/data/error/DataError.kt`
- `app/src/main/java/com/thehouse/app/data/error/DataErrorMapper.kt`
- `app/src/main/java/com/thehouse/app/data/image/ImageLoaderFactory.kt`
- `app/src/main/java/com/thehouse/app/data/mapper/Mapper.kt`
- `app/src/main/java/com/thehouse/app/data/preferences/AppPreferencesDataStore.kt`
- `app/src/main/java/com/thehouse/app/data/preferences/DataStorePreferencesStore.kt`
- `app/src/main/java/com/thehouse/app/data/preferences/PreferencesStore.kt`
- `app/src/main/java/com/thehouse/app/data/remote/HttpClientFactory.kt`
- `app/src/main/java/com/thehouse/app/data/remote/NetworkTimeouts.kt`
- `app/src/main/java/com/thehouse/app/data/remote/RetrofitFactory.kt`
- `app/src/main/java/com/thehouse/app/data/remote/SafeHttpLoggingInterceptor.kt`
- `app/src/main/java/com/thehouse/app/data/serialization/JsonProvider.kt`
- `app/src/main/java/com/thehouse/app/presentation/common/BaseViewModel.kt`
- `app/src/main/java/com/thehouse/app/presentation/common/DomainErrorToUiError.kt`
- `app/src/main/java/com/thehouse/app/presentation/common/UiEffect.kt`
- `app/src/main/java/com/thehouse/app/presentation/common/UiError.kt`
- `app/src/main/java/com/thehouse/app/presentation/common/UiEvent.kt`
- `app/src/main/java/com/thehouse/app/presentation/common/UiState.kt`
- `app/src/main/java/com/thehouse/app/presentation/navigation/AppNavHost.kt`
- `app/src/main/java/com/thehouse/app/presentation/navigation/AppNavigator.kt`
- `app/src/main/java/com/thehouse/app/presentation/navigation/AppRoute.kt`

### Tests

- `app/src/test/java/com/thehouse/app/data/config/AppConfigTest.kt`
- `app/src/test/java/com/thehouse/app/data/error/DataErrorMapperTest.kt`
- `app/src/test/java/com/thehouse/app/data/mapper/MapperTest.kt`
- `app/src/test/java/com/thehouse/app/data/remote/NetworkingFoundationTest.kt`
- `app/src/test/java/com/thehouse/app/data/remote/NetworkTimeoutsTest.kt`
- `app/src/test/java/com/thehouse/app/data/serialization/JsonProviderTest.kt`
- `app/src/test/java/com/thehouse/app/presentation/common/BaseViewModelTest.kt`
- `app/src/test/java/com/thehouse/app/presentation/common/DomainErrorToUiErrorTest.kt`

### Documentación SDD

- `specs/nv-01-foundation/evidence.md` (nuevo).
- `specs/nv-01-foundation/spec.md` (estado actualizado).
- `specs/nv-01-foundation/plan.md` (estado actualizado).
- `specs/nv-01-foundation/task.md` (30 tasks completadas).
- `memoria.md` (decisiones `M-MOB-038`, `M-MOB-039`, `M-MOB-040`).

### Archivos no modificados

- `MainActivity.kt`
- `app/src/main/java/com/thehouse/app/ui/theme/*`
- `constitution.md`
- `AGENTS.md`
- `settings.gradle.kts`
- `gradle/libs.versions.toml`
- `app/src/main/res/*`
- `app/src/androidTest/*`

---

## 15. Riesgos y deuda pendiente

- No existen contratos de repositorio ni workers porque aún no hay entidades de dominio reales. Se crearán en specs funcionales.
- `AppContainer` y `AppNavHost` son foundation; todavía no hay una pantalla funcional que los consuma.
- `MainActivity` conserva el saludo placeholder existente. No se convirtió en pantalla de negocio.
- Las 15 warnings de lint son preexistentes y quedan documentadas.
- `assembleRelease` reporta un warning de strip de librerías nativas; el build finaliza correctamente.
- No se ejecutaron tests instrumentados; NV-02 definirá la estrategia de testing/CI.
- `AGENTS.md` conserva referencias de gobernanza anteriores a la ejecución de NV-01 (`NV-01: NO EJECUTAR`, `DI pendiente`). Su actualización requiere autorización humana explícita; no se modificó en esta task.
- El manifest fusionado incorpora permisos transitivos de dependencias base (`INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`). Deben revisarse antes de release en NV-02/NV-03.
- El bloque `release.optimization` de `app/build.gradle.kts` es preexistente y no fue modificado por NV-01.

---

## 16. Revisión sdd-guardian

Resultado: **PASS WITH WARNINGS**.

Hallazgos registrados:

- `W-01`: `AGENTS.md` desactualizado respecto de NV-01 ejecutada/validada y DI manual. Requiere autorización humana para modificarse.
- `W-02`: matrices de trazabilidad desincronizadas. Corregidas en `spec.md` y `plan.md`; ahora cubren las 30 tasks.
- `W-03`: artefacto release ausente tras `clean`. Regenerado en `app/build/outputs/apk/release/app-release-unsigned.apk`.
- `W-04`: `T013` y `T027` se cerraron con convención documentada, no con repositorios/fakes ejecutables, porque no existe entidad de dominio real. Aceptado y diferido a la primera spec funcional.
- `O-01`: revisar permisos transitivos del manifest fusionado antes de release.
- `O-02`: el repositorio no tiene historial Git; la auditoría se apoyó en contenido, timestamps y reportes.

---

## 17. Dictamen

```text
NV-01 Foundation
✅ EJECUTADA / VALIDADA
30/30 tasks
0 funcionalidades de negocio
0 integraciones externas
COST-001 = $0
sdd-guardian: PASS WITH WARNINGS
```
