# NV-01 — FOUNDATION

> ## ✅ EJECUTADA / VALIDADA
>
> Esta spec fue autorizada, ejecutada y validada el 2026-10-31. La evidencia real está registrada en `evidence.md`.

| Campo | Valor |
|---|---|
| Spec ID | NV-01 |
| Nombre | Foundation |
| Versión | 1.0.0 |
| Fecha | 2026-10-31 |
| Estado | ✅ EJECUTADA / VALIDADA |
| Autorización | APROBADA |
| Dependencia | Bootstrap técnico y SDD aprobados |
| Módulo | `:app` |

## 1. Contexto

El proyecto Android existente ya compila. Las dependencias técnicas base están instaladas y validadas. La estructura SDD está creada.

Esta spec define la foundation arquitectónica futura de TheHouse Mobile. No implementa funcionalidades de negocio ni crea pantallas, APIs, entidades o integraciones.

## 2. Precondiciones reconocidas

Las siguientes condiciones ya existen y **no deben recrearse como tareas**:

- Proyecto Android nativo existente.
- Kotlin configurado.
- Jetpack Compose configurado.
- `minSdk = 26`.
- `compileSdk = 37`, `targetSdk = 37`.
- Java 17.
- Build exitoso.
- Dependencias técnicas base instaladas.
- AGP `9.5.0`, Gradle `9.6.0`, Kotlin `2.2.10`.
- Un único módulo `:app`.

## 3. Objetivo

Definir y, solo tras autorización, implementar las convenciones y boundaries mínimas para que las futuras specs funcionales puedan construir MVVM, Clean Architecture y UDF de forma consistente.

## 4. Alcance autorizable

- MVVM foundation.
- Clean Architecture boundaries por paquetes.
- UDF y contratos de estado/evento.
- Convenciones de ViewModel, UI State, Coroutines y Flow.
- Navigation foundation.
- Networking foundation sin endpoints funcionales.
- Serialization y mapeo.
- Persistence boundaries sin entidades Room.
- DataStore boundaries.
- WorkManager boundaries.
- Image loading boundary con Coil.
- Error handling.
- Decisión de Dependency Injection.
- Configuration/build conventions.
- Costuras de test para NV-02.

## 5. Fuera de alcance

- Registro, login, logout funcional.
- Usuarios, perfiles, propiedades, edificios, habitaciones.
- Universidades, catálogo, búsqueda o mapas funcionales.
- Favoritos, visitas, chat, rentas, reseñas.
- Notificaciones funcionales, FCM y push.
- Smart Match, ML, IA generativa.
- Entidades Room, DAOs, bases de datos y KSP.
- Endpoints o APIs funcionales.
- Pantallas funcionales.
- Firebase, Maps o Google Play.
- Módulos Gradle adicionales.
- Publicación.

## 6. Requisitos

### NV01-REQ-001 — Boundaries por paquetes

Definir la estructura de paquetes de Presentation, Domain y Data dentro de `:app`, junto con las reglas de dependencia.

**Aceptación:** existe una convención documentada y verificable; no se crean módulos Gradle.

### NV01-REQ-002 — Dirección de dependencias

Garantizar la dirección de dependencias:

```text
Presentation → Domain
Data         → Domain
```

Regla explícita:

```text
Domain no depende de Presentation.
Domain no depende de Data.

Presentation puede depender de Domain.
Data puede depender de Domain.
```

Domain tampoco puede depender de:

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

**Aceptación:** la única dirección de dependencia es Presentation → Domain y Data → Domain; Domain no depende de Android, Jetpack Compose, Navigation, Retrofit, OkHttp, Room, DataStore, WorkManager, Firebase ni Google Maps.

### NV01-REQ-003 — Convenciones MVVM

Definir cómo se declara un ViewModel, sus dependencias, estado y eventos.

**Aceptación:** la UI no contiene lógica de negocio y el ViewModel no depende de tipos de UI Android.

### NV01-REQ-004 — Contrato UDF

Definir el flujo UI Event → ViewModel → Use Case/Repository → StateFlow → UI State → Compose.

**Aceptación:** estados y eventos son explícitos y no existe estado mutable global.

### NV01-REQ-005 — Convenciones de UI State

Definir estados de loading, content, empty y error, y la representación de eventos one-shot.

**Aceptación:** la UI puede representar todos los estados sin lógica ad hoc.

### NV01-REQ-006 — Coroutines y dispatchers

Definir `viewModelScope`, dispatchers, cancelación y manejo de errores.

**Aceptación:** no se usa `GlobalScope`; I/O no bloquea el hilo principal.

### NV01-REQ-007 — Flow y lifecycle

Definir `StateFlow`, `collectAsStateWithLifecycle` y recolección segura.

**Aceptación:** la UI no recolecta flows de forma que ignore lifecycle.

### NV01-REQ-008 — Navigation foundation

Definir la base de Navigation Compose, rutas, argumentos y back stack sin pantallas funcionales.

**Aceptación:** la navegación no contiene lógica de negocio ni destinos de negocio.

### NV01-REQ-009 — Error handling

Definir el modelo de error de dominio, mapeo desde Data y presentación segura.

**Aceptación:** los errores de infraestructura no se filtran como excepciones crudas a la UI.

### NV01-REQ-010 — Networking foundation

Definir clientes Retrofit/OkHttp, timeouts, interceptores, logging seguro y contratos base sin endpoints funcionales.

**Aceptación:** no hay API funcional; no se registran tokens.

### NV01-REQ-011 — Serialization

Definir convenciones de Kotlin Serialization y mapeo DTO → dominio.

**Aceptación:** DTO no se expone a Presentation.

### NV01-REQ-012 — Persistence boundary Room

Definir contratos de repositorio y límites de persistencia sin crear entidades, DAOs ni bases de datos.

**Aceptación:** Room no se consume desde UI ni Domain; KSP sigue pendiente.

### NV01-REQ-013 — DataStore boundary

Definir el uso de DataStore para preferencias no sensibles y su encapsulamiento.

**Aceptación:** no se almacenan secretos; la UI no accede directamente.

### NV01-REQ-014 — WorkManager boundary

Definir cómo se encapsulará el trabajo diferido sin implementar workers funcionales.

**Aceptación:** no se crean workers ni sync antes del dominio.

### NV01-REQ-015 — Image loading

Definir el boundary de Coil `3.4.0` y su configuración mínima.

**Aceptación:** la UI usa un componente o capa controlada; no se crean pantallas.

### NV01-REQ-016 — Decisión de DI

Evaluar DI manual vs Hilt/Koin y seleccionar una estrategia para esta foundation.

**Aceptación:** decisión documentada en `memoria.md`; no se instala un framework sin aprobación.

### NV01-REQ-017 — Configuration y build conventions

Definir configuración por build type, manejo seguro de valores y límites para entornos.

**Aceptación:** no hay secretos en el repositorio ni cambios de toolchain.

### NV01-REQ-018 — Costuras de testing

Definir interfaces, fakes y puntos de inyección que permitan las pruebas de NV-02.

**Aceptación:** la foundation es testeable sin acceso real a red o base de datos.

#### Regla transversal de mapeo

La expresión `Data ↔ Domain` solo describe transformación/mapeo de modelos:

```text
DTO → Domain
Domain → DTO
Entity → Domain
Domain → Entity
```

Un mapeo puede ser bidireccional, pero esto **NO** implica dependencia arquitectónica bidireccional. La regla permanece:

```text
Data → Domain
```

Queda prohibido:

```text
Domain → Data
```

Domain no debe conocer DTO, Room Entity, Retrofit, DataStore ni implementaciones de Repository.

## 7. Trazabilidad resumida

| Requisito | Fase de plan | Task(s) |
|---|---|---|
| NV01-REQ-001 | F1 | NV01-T001, NV01-T002, NV01-T005, NV01-T006 |
| NV01-REQ-002 | F1 | NV01-T003 |
| NV01-REQ-003 | F2 | NV01-T007, NV01-T008 |
| NV01-REQ-004 | F2 | NV01-T009 |
| NV01-REQ-005 | F2 | NV01-T010 |
| NV01-REQ-006 | F5 | NV01-T022 |
| NV01-REQ-007 | F2 | NV01-T011 |
| NV01-REQ-008 | F2 | NV01-T012 |
| NV01-REQ-009 | F6 | NV01-T026 |
| NV01-REQ-010 | F4 | NV01-T017, NV01-T020, NV01-T021 |
| NV01-REQ-011 | F4 | NV01-T018 |
| NV01-REQ-012 | F3 | NV01-T013, NV01-T016 |
| NV01-REQ-013 | F3 | NV01-T014 |
| NV01-REQ-014 | F3 | NV01-T015 |
| NV01-REQ-015 | F4 | NV01-T019 |
| NV01-REQ-016 | F5 | NV01-T023 |
| NV01-REQ-017 | F1/F5 | NV01-T004, NV01-T024, NV01-T025 |
| NV01-REQ-018 | F6 | NV01-T027 |
| Cierre global | F6 | NV01-T028, NV01-T029, NV01-T030 |

La matriz completa y el orden de fases están en `plan.md`. Las tasks están en `task.md`. Las 30 tasks quedan cubiertas por esta matriz y por la matriz de `plan.md`.

## 8. Criterios de aceptación globales

- Build exitoso.
- Tests unitarios existentes exitosos.
- Lint sin errores nuevos.
- No hay funcionalidades de negocio.
- No hay pantallas funcionales nuevas.
- No hay entidades Room.
- No hay Firebase, Maps, ML ni IA.
- No hay secretos.
- No se cambiaron AGP, Gradle, Kotlin, SDK, Java ni applicationId.
- La arquitectura es verificable y está documentada.
- COST-001 sigue en costo $0.

## 9. Evidencia esperada

```powershell
.\gradlew.bat clean assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

Además:

- Archivos modificados.
- Mapa de paquetes y dependencias.
- Decisión de DI.
- Matriz de trazabilidad actualizada.
- Resultados reales sin ocultar fallos.

## 10. Riesgos

- Sobrearquitectura prematura.
- Introducción innecesaria de DI.
- Configurar Room/KSP sin entidades reales.
- Mezclar foundation con funcionalidades.
- Cambios de toolchain no autorizados.

## 11. Estado

```text
✅ EJECUTADA
✅ VALIDADA
```

La ejecución fue autorizada y validada el 2026-10-31. La evidencia por task, los resultados de build/test/lint y la auditoría de alcance están en `evidence.md`.
