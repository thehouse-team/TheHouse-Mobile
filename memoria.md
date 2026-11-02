# MEMORIA TÉCNICA — THEHOUSE MOBILE

Memoria persistente de decisiones técnicas y arquitectónicas. No es un diario. Cada entrada registra una decisión vigente o una propuesta futura con estado explícito.

Estados permitidos: `APROBADO`, `PROPUESTO`, `PENDIENTE`, `DESCARTADO`.

---

## M-MOB-001 — Android nativo

- **Decisión:** Desarrollar TheHouse Mobile como aplicación Android nativa.
- **Estado:** APROBADO
- **Justificación:** Requisito académico y técnico; permite usar el toolchain Android, Kotlin y Compose sin capas multiplataforma.
- **Impacto:** Toda implementación futura debe respetar Android nativo y no introducir frameworks multiplataforma sin spec aprobada.

## M-MOB-002 — Kotlin 2.2.10

- **Decisión:** Mantener Kotlin `2.2.10` como versión del lenguaje y de los plugins Kotlin.
- **Estado:** APROBADO
- **Justificación:** Es la versión compatible con el proyecto Android existente y con AGP 9.5.0.
- **Impacto:** Compose y Serialization usan `version.ref = "kotlin"`; no se permite una segunda versión Kotlin ni una actualización arbitraria.

## M-MOB-003 — Jetpack Compose + Material 3

- **Decisión:** Usar Jetpack Compose con Material 3 y el Compose BOM `2026.09.00`.
- **Estado:** APROBADO
- **Justificación:** Stack UI definido por el proyecto y adecuado para UDF.
- **Impacto:** Las pantallas funcionales futuras deben usar composables stateless, state hoisting y UI State.

## M-MOB-004 — MVVM

- **Decisión:** Adoptar MVVM para toda pantalla funcional.
- **Estado:** APROBADO
- **Justificación:** Separa UI, estado y lógica de presentación; facilita testing y lifecycle.
- **Impacto:** Todo ViewModel futuro expone estado observable y atiende eventos; la UI no contiene lógica de negocio.

## M-MOB-005 — Clean Architecture por paquetes

- **Decisión:** Aplicar Clean Architecture inicialmente mediante paquetes y boundaries dentro de `:app`, sin módulos Gradle adicionales.
- **Estado:** APROBADO
- **Justificación:** Evita sobrearquitectura en la etapa inicial y permite evolucionar cuando exista justificación.
- **Impacto:** La dirección de dependencias es Presentation → Domain ← Data. La modularización requiere spec aprobada.

## M-MOB-006 — Unidirectional Data Flow

- **Decisión:** Adoptar UDF: UI Event → ViewModel → Use Case/Repository → StateFlow → UI State → Compose.
- **Estado:** APROBADO
- **Justificación:** Hace predecible el estado y evita mutaciones globales.
- **Impacto:** Los Composables no acceden a red, Room ni DataStore; el estado fluye en una sola dirección.

## M-MOB-007 — Coroutines y Flow

- **Decisión:** Usar Kotlin Coroutines `1.11.0` y Flow para concurrencia y reactividad.
- **Estado:** APROBADO
- **Justificación:** Stack estándar Android, compatible con lifecycle y ViewModel.
- **Impacto:** Se prohíbe `GlobalScope`; los ViewModels usan `viewModelScope` y los tests futuros usarán dispatchers controlados.

## M-MOB-008 — Navigation Compose

- **Decisión:** Usar Navigation Compose `2.10.2` como base de navegación.
- **Estado:** APROBADO
- **Justificación:** Integración nativa con Compose y lifecycle.
- **Impacto:** La foundation de navegación se definirá en NV-01; no se crean destinos funcionales sin spec.

## M-MOB-009 — Retrofit

- **Decisión:** Usar Retrofit `3.0.0` para networking.
- **Estado:** APROBADO
- **Justificación:** Cliente HTTP tipado, conocido y compatible con Kotlin Serialization.
- **Impacto:** La UI no consume Retrofit; los repositorios encapsulan la red y mapean DTO a modelos de dominio.

## M-MOB-010 — OkHttp

- **Decisión:** Usar OkHttp `5.5.1` como motor HTTP y para interceptores.
- **Estado:** APROBADO
- **Justificación:** Es la versión estable preferida y compila con el toolchain actual.
- **Impacto:** Timeouts, logging seguro y configuración de red se definirán en NV-01; nunca se registran tokens ni datos sensibles.

## M-MOB-011 — Kotlin Serialization

- **Decisión:** Usar Kotlin Serialization JSON `1.9.0` y el plugin `org.jetbrains.kotlin.plugin.serialization` con la misma versión Kotlin `2.2.10`.
- **Estado:** APROBADO
- **Justificación:** Serialización tipada sin reflexión y alineada con Retrofit.
- **Impacto:** No se crean modelos de red funcionales sin spec; el plugin se declara con `version.ref = "kotlin"`.

## M-MOB-012 — Room

- **Decisión:** Usar Room `2.8.5` para persistencia estructurada futura.
- **Estado:** APROBADO
- **Justificación:** Solución Android estándar para SQLite con soporte de Flow y Coroutines.
- **Impacto:** No se crean entidades, DAOs ni bases de datos sin spec. Room Compiler y KSP no se configuran todavía.

## M-MOB-013 — DataStore

- **Decisión:** Usar DataStore Preferences `1.3.0` para preferencias y estado simple futuro.
- **Estado:** APROBADO
- **Justificación:** Reemplazo moderno de SharedPreferences con API asíncrona.
- **Impacto:** No almacenar secretos en DataStore sin estrategia segura aprobada; el uso funcional requiere spec.

## M-MOB-014 — Coil

- **Decisión:** Usar Coil `3.4.0` para carga de imágenes, en lugar de `3.6.3`.
- **Estado:** APROBADO
- **Justificación:** La versión preferida `3.6.3` exige Kotlin stdlib `2.4.10` y provoca incompatibilidad real de metadata con Kotlin `2.2.10`. `3.4.0` es la versión estable compatible más cercana; el build fue validado.
- **Impacto:** Se documenta el ajuste de versión. No se actualiza Kotlin/AGP/Gradle. Revisar esta decisión solo con una spec de toolchain aprobada.

## M-MOB-015 — WorkManager

- **Decisión:** Usar WorkManager `2.12.0` para trabajo diferido y sincronización futura.
- **Estado:** APROBADO
- **Justificación:** API Android recomendada para trabajo persistente y con restricciones.
- **Impacto:** No se implementan workers funcionales sin spec; la estrategia offline/sync se definirá con entidades reales.

## M-MOB-016 — Firebase Cloud Messaging futuro

- **Decisión:** FCM se implementará en una spec funcional futura, no ahora.
- **Estado:** PENDIENTE
- **Justificación:** Requiere proyecto Firebase, `google-services.json`, plugin y configuración externa; no debe improvisarse.
- **Impacto:** No hay Firebase configurado. Referencia no instalada: Firebase Android BoM `35.1.0`; deberá verificarse la versión vigente cuando se autorice la spec.

## M-MOB-017 — Google Maps futuro

- **Decisión:** Google Maps se implementará mediante spec independiente/funcional futura, no ahora.
- **Estado:** PENDIENTE
- **Justificación:** Requiere API key, configuración de billing y manejo seguro de secretos.
- **Impacto:** No hay Maps, Maps Compose ni API keys configurados. Referencia no instalada: Maps Compose `8.4.0`. Debe cumplir COST-001.

## M-MOB-018 — COST-001

- **Decisión:** Todo servicio externo debe demostrar viabilidad de costo objetivo $0 antes de ser aprobado.
- **Estado:** APROBADO
- **Justificación:** El proyecto es académico y no debe generar riesgo de cobro.
- **Impacto:** Firebase, Maps, APIs, cloud, ML, IA, moderación, analytics y servicios equivalentes requieren free tier, límites, riesgo de tarjeta/cobro, mitigación y alternativa gratuita. Si no se demuestra, queda BLOCKED.

## M-MOB-019 — minSdk 26

- **Decisión:** Mantener `minSdk = 26`.
- **Estado:** APROBADO
- **Justificación:** Cobertura definida por el proyecto y compatibilidad con APIs modernas.
- **Impacto:** No cambiar sin spec de compatibilidad y autorización humana.

## M-MOB-020 — compileSdk/targetSdk 37

- **Decisión:** Mantener `compileSdk = 37` y `targetSdk = 37`, con Java 17.
- **Estado:** APROBADO
- **Justificación:** Configuración existente validada; requisito del toolchain actual.
- **Impacto:** No actualizar arbitrariamente. Cambios de SDK requieren spec y validación.

## M-MOB-021 — Un solo módulo `:app`

- **Decisión:** Mantener un único módulo Gradle `:app` en la etapa inicial.
- **Estado:** APROBADO
- **Justificación:** Evita sobrearquitectura prematura y mantiene el build simple.
- **Impacto:** No crear módulos `domain`, `data`, `presentation`, `core` ni otros. La modularización requiere spec aprobada.

## M-MOB-022 — Dependency Injection pendiente

- **Decisión:** No introducir Hilt, Koin ni otro framework de DI todavía.
- **Estado:** PENDIENTE
- **Justificación:** La estrategia debe evaluarse en NV-01 Foundation con las boundaries y necesidades reales.
- **Impacto:** No agregar frameworks de DI sin spec aprobada. Se permite DI manual si una task autorizada lo define.

## M-MOB-023 — KSP pendiente hasta Room real

- **Decisión:** No configurar KSP ni Room Compiler en el bootstrap.
- **Estado:** PENDIENTE
- **Justificación:** La app no tiene `@Entity`, `@Dao` ni `@Database`; agregar procesamiento de anotaciones ahora sería deuda innecesaria.
- **Impacto:** KSP se agregará cuando una spec autorizada implemente persistencia Room real.

## M-MOB-024 — Toolchain congelado

- **Decisión:** Mantener AGP `9.5.0`, Gradle `9.6.0`, Kotlin `2.2.10`, compileSdk/targetSdk `37`, minSdk `26`, Java `17` y applicationId `com.thehouse.app`.
- **Estado:** APROBADO
- **Justificación:** El proyecto existente compila y no se deben introducir cambios arbitrarios.
- **Impacto:** Cualquier actualización requiere spec de toolchain, análisis de compatibilidad, build y autorización humana.

## M-MOB-025 — Dependencias técnicas base

- **Decisión:** Instalar Lifecycle `2.11.0`, Navigation `2.10.2`, Coroutines `1.11.0`, Serialization JSON `1.9.0`, Retrofit `3.0.0`, OkHttp `5.5.1`, Room `2.8.5`, DataStore `1.3.0`, Coil `3.4.0` y WorkManager `2.12.0`.
- **Estado:** APROBADO
- **Justificación:** Habilitan la arquitectura Mobile base sin implementar funcionalidades.
- **Impacto:** No se eliminan dependencias Compose existentes. Se validó compilación, tests unitarios y lint.

## M-MOB-026 — Validación del bootstrap

- **Decisión:** El bootstrap técnico se considera validado únicamente con evidencia real de build.
- **Estado:** APROBADO
- **Justificación:** La compilación exitosa demuestra compatibilidad de dependencias y toolchain.
- **Impacto:** Resultados registrados: `clean assembleDebug` BUILD SUCCESSFUL, `testDebugUnitTest` BUILD SUCCESSFUL, `lintDebug` BUILD SUCCESSFUL.

## M-MOB-027 — Compose BOM existente

- **Decisión:** Conservar Compose BOM `2026.09.00`.
- **Estado:** APROBADO
- **Justificación:** Es parte de la configuración existente y compila con las dependencias agregadas.
- **Impacto:** No cambiar el BOM sin spec y validación.

## M-MOB-028 — Sesión persistente y logout

- **Decisión:** La sesión podrá persistir entre aperturas hasta logout explícito o invalidación de seguridad.
- **Estado:** PROPUESTO
- **Justificación:** Requisito futuro de experiencia y seguridad.
- **Impacto:** La estrategia de tokens, almacenamiento seguro y logout se definirá en la spec funcional de Login/Logout. No se implementa ahora.

## M-MOB-029 — Seguridad de secretos

- **Decisión:** Ningún secreto se versiona en el repositorio.
- **Estado:** APROBADO
- **Justificación:** Evita filtración de claves y credenciales.
- **Impacto:** API keys, passwords, JWT secrets, signing passwords, keystore privado y service accounts quedan fuera del repositorio; se usarán secrets del CI o archivos locales ignorados.

## M-MOB-030 — Offline/sync no especificado

- **Decisión:** No diseñar ni implementar sincronización offline antes de conocer entidades y casos de uso reales.
- **Estado:** PENDIENTE
- **Justificación:** Una sync inventada genera conflictos y deuda de datos.
- **Impacto:** Room y WorkManager quedan disponibles como herramientas, pero su uso funcional y políticas de conflicto se definirán en specs futuras.

## M-MOB-031 — Requisitos académicos móviles

- **Decisión:** Mantener como restricciones futuras: registro, login, pantallas posteriores al primer ingreso, Google Play, Push Notifications, Privacy Notice, eliminar cuenta, permisos Android, sesión persistente hasta logout, almacenamiento/sync offline cuando sea necesario, ML, IA generativa y moderación de contenido.
- **Estado:** PENDIENTE
- **Justificación:** Forman parte del alcance académico, pero no del bootstrap.
- **Impacto:** No se implementan en esta etapa. Cada capacidad requiere spec independiente y evaluación COST-001 cuando aplique.

## M-MOB-032 — Roadmap funcional futuro

- **Decisión:** Documentar el roadmap únicamente en memoria, sin crear carpetas ni código.
- **Estado:** PENDIENTE
- **Justificación:** Evita specs vacías y sobrearquitectura antes de autorización.
- **Impacto:** Secuencia prevista: NV-04 Registration, NV-05 Login, NV-06 First-Time Setup / Profile, NV-07 Home, NV-08 University Catalog, NV-09 Search and Map, NV-10 Property Detail, NV-11 Favorites, NV-12 Visit Scheduling, NV-13 Chat, NV-14 Rental Closing, NV-15 Reviews, NV-16 Notifications / FCM, NV-17 Privacy / Delete Account, NV-18 Offline Sync, NV-19 Smart Match / ML / Generative AI. No se crean esas carpetas.

## M-MOB-033 — Separación ML / IA generativa

- **Decisión:** Mantener Machine Learning para calcular resultados y Generative AI para explicarlos.
- **Estado:** PROPUESTO
- **Justificación:** Separación de responsabilidades y reducción de dependencia de proveedores.
- **Impacto:** El Mobile no contendrá secretos de IA; llamadas sensibles se gestionarán mediante backend cuando corresponda. Requiere spec y COST-001.

## M-MOB-034 — Estructura SDD Mobile

- **Decisión:** Crear `constitution.md`, `AGENTS.md`, `memoria.md`, `.opencode/agents/`, `.opencode/skills/` y `specs/` en la raíz Mobile.
- **Estado:** APROBADO
- **Justificación:** Establece gobernanza, roles y trazabilidad antes de implementar.
- **Impacto:** No se introducen archivos SDD dentro de `app/src/`.

## M-MOB-035 — Sin funcionalidad en el bootstrap

- **Decisión:** No implementar funcionalidades de negocio, pantallas, APIs, entidades Room ni integraciones externas durante el bootstrap.
- **Estado:** APROBADO
- **Justificación:** El bootstrap solo prepara toolchain, dependencias y gobernanza.
- **Impacto:** Registro, login, propiedades, chat, mapas funcionales, FCM, ML, IA y demás capacidades quedan para specs futuras.

## M-MOB-036 — Git en bootstrap

- **Decisión:** No ejecutar `git init`, `git add`, `git commit` ni `git push` en la tarea de bootstrap.
- **Estado:** APROBADO
- **Justificación:** Evita efectos laterales no solicitados y preserva el estado del repositorio.
- **Impacto:** Las operaciones Git requerirán autorización explícita.

## M-MOB-037 — Estado de specs NV-01, NV-02 y NV-03

- **Decisión:** Crear NV-01 Foundation, NV-02 DevSecOps y NV-03 Release/Play como specs independientes.
- **Estado:** APROBADO
- **Justificación:** Permite revisión humana y autorización por separado.
- **Impacto:** NV-01 fue autorizada, ejecutada y validada el 2026-10-31. NV-02 y NV-03 permanecen **⛔ NO EJECUTAR**; sus tasks no están marcadas.

## M-MOB-038 — Dependency Injection manual en NV-01

- **Decisión:** Usar DI manual mediante `AppContainer` y `TheHouseApplication`; no instalar Hilt ni Koin.
- **Estado:** APROBADO
- **Justificación:** La foundation aún no tiene features ni entidades reales. Manual DI es suficiente, explícito, testeable y evita plugin, codegen y complejidad prematura.
- **Impacto:** `AppContainer` es el composition root; las dependencias se inyectan por constructor. Hilt/Koin solo se reevaluarán cuando exista un grafo de dependencias funcional que lo justifique.

## M-MOB-039 — Convenciones arquitectónicas de NV-01

- **Decisión:** Mantener boundaries por paquetes `presentation`, `domain` y `data` dentro de `:app`, con `di` como composition root. Dirección `Presentation → Domain` y `Data → Domain`; Domain no depende de Presentation ni Data.
- **Estado:** APROBADO
- **Justificación:** Foundation mínima, verificable y extensible sin módulos Gradle adicionales ni arquitectura ficticia.
- **Impacto:** Domain solo usa Kotlin y Coroutines. Presentation define `UiState`, `UiEvent`, `UiEffect`, `UiError` y `BaseViewModel`. Data implementa errores, mapeo, DataStore, red, serialización e imagen. El mapeo `Data ↔ Domain` es solo transformación de modelos y no crea dependencia `Domain → Data`.

## M-MOB-040 — Configuración no secreta y logging HTTP seguro

- **Decisión:** Exponer solo configuración no sensible mediante `BuildConfig`: `API_BASE_URL` con placeholder `https://example.invalid/` y `ENABLE_HTTP_LOGGING` true en debug / false en release. El logging HTTP registra únicamente método, host, status y duración.
- **Estado:** APROBADO
- **Justificación:** Evita secretos versionados y previene filtración de headers, bodies, paths, query params, tokens, credenciales o PII.
- **Impacto:** DataStore queda limitado a preferencias no sensibles. URLs reales y entornos futuros deberán inyectarse de forma segura sin versionar secretos ni cambiar el toolchain.
