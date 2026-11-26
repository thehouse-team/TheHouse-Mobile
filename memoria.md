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

## M-MOB-016 — Firebase Cloud Messaging

- **Decisión:** FCM implementado en NV-14 FASE B con Firebase Android BoM `35.2.0` (`firebase-messaging` 26.0.2) y plugin `com.google.gms.google-services` `4.6.0` aplicado condicionalmente solo si existe `app/google-services.json`.
- **Estado:** APROBADO (ejecutado 2026-11-17; configuración externa pendiente)
- **Justificación:** Requiere proyecto Firebase y `google-services.json` reales; el archivo no existe y no se generó uno falso. El build compila con `BuildConfig.FCM_CONFIGURED=false`.
- **Impacto:** Sin `google-services.json` no se inicializa Firebase ni se obtiene token real. El registro del token en Backend sigue bloqueado (`FCM-TOKEN-BACKEND-BLOCKED`) hasta contrato real de devices/token.

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

- **Decisión:** Documentar el roadmap en memoria y en specs reales cuando exista autorización. Actualizado para U2: el orden oficial de specs es `NV-01 Foundation (ejecutada)` → `NV-03 Release / Google Play` → `NV-04 Auth + Onboarding` → `NV-02 DevSecOps / CI-CD (documentar ahora, ejecutar después)`. NV-04 consolida registro, login, logout, persistencia de sesión y onboarding / first-time setup. La numeración NV Mobile es independiente de la numeración NV del backend, aunque ambos puedan tener un NV-04.
- **Estado:** APROBADO
- **Justificación:** Secuencia U2 acordada por el humano; evita specs vacías y definida antes de autorización.
- **Impacto:** Secuencia completa prevista: NV-04 Auth + Onboarding (U2), luego NV-02 DevSecOps, luego specs funcionales posteriores: NV-05 Home, NV-06 University Catalog, NV-07 Search and Map, NV-08 Property Detail, NV-09 Favorites, NV-10 Visit Scheduling, NV-11 Chat, NV-12 Rental Closing, NV-13 Reviews, NV-14 Notifications / FCM, NV-15 Privacy / Delete Account, NV-16 Offline Sync, NV-17 Smart Match / ML / Generative AI.

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

- **Decisión:** Durante el bootstrap no se ejecutó `git init`, `git add`, `git commit` ni `git push`.
- **Estado:** APROBADO
- **Justificación:** Evita efectos laterales no solicitados y preserva el estado del repositorio.
- **Impacto:** El repositorio ahora **es Git con remoto GitHub existente** y el trabajo se realiza mediante ramas feature. Las operaciones Git de escritura (commit, push) siguen requiriendo autorización explícita.

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

## M-MOB-041 — Matriz académica U2

- **Decisión:** Registrar la matriz académica por requisito, con spec responsable y estado real.

| Requisito | Spec | Estado |
|---|---|---|
| Descargable desde Google Play | NV-03 | Pendiente (requiere NV-03 ejecutada) |
| Fidelidad a maqueta | NV-04 | Pendiente (referencia visual) |
| Registro | NV-04 | Pendiente (backend `POST /api/v1/auth/register` confirmado) |
| Login | NV-04 | Pendiente (backend `POST /api/v1/auth/login` confirmado) |
| Primer ingreso / onboarding | NV-04 | Pendiente |
| Logout | NV-04 | Pendiente (backend `POST /api/v1/auth/logout` confirmado) |
| Sesión persistente | NV-04 | Pendiente (diseño M-MOB-044) |
| Permisos Android | NV-03 / NV-04 | Pendiente (`INTERNET` justificado) |
| Aviso de privacidad | NV-03 | Pendiente (URL humana pendiente) |
| Caché móvil | NV-04 | Pendiente (OkHttp HTTP disk cache) |
| Pruebas unitarias | NV-02 / NV-04 | Pendiente |
| Pruebas de integración | NV-02 / NV-04 | Pendiente (MockWebServer) |
| Pruebas de seguridad | NV-02 | Pendiente |
| CI/CD desde GitHub | NV-02 | Pendiente (documentar ahora, ejecutar después) |
| Versionamiento | NV-03 | Pendiente (definido en `VERSIONING.md`) |
| Eliminación de cuenta | Posterior | Obligatorio antes de producción |
| Push notifications | Posterior | Pendiente |
| Moderación | Posterior | Pendiente |
| SQLite / sincronización | Posterior | Pendiente |
| ML | Posterior | Pendiente |
| IA generativa | Posterior | Pendiente |

- **Estado:** PENDIENTE
- **Justificación:** Correspondencia trazable entre requisitos académicos y specs.
- **Impacto:** No se marca como implementado nada sin evidencia real registrada.

## M-MOB-042 — Sesión y seguridad de tokens (diseño NV-04)

- **Decisión:** Diseño de sesión persistente: access token solo en memoria; refresh token cifrado con AES-GCM usando Android Keystore, con implementación directa de Keystore (no `EncryptedSharedPreferences`). Renovación automática con OkHttp `Authenticator`, refresh single-flight con `Mutex` y máximo una renovación concurrente; fallo de refresh → logout local. Logout: intento backend (`POST /api/v1/auth/logout`), limpieza siempre del estado local, borrado de tokens y cancelación de trabajos asociados; sin red → logout local inmediato.
- **Estado:** PROPUESTO
- **Justificación:** Requisito académico de sesión persistente con mínimo riesgo de filtración; cualquier librería adicional solo se usará si demuestra compatibilidad con Kotlin `2.2.10`.
- **Impacto:** La implementación de renovación automática depende de `POST /api/v1/auth/refresh`, **no confirmado**: tasks dependientes quedan `BLOCKED-BACKEND`. `FLAG_SECURE` queda PROPUESTO y solo será requisito si el humano lo aprueba. Nunca: tokens o contraseñas en logs, credenciales en backups.

## M-MOB-043 — MCP deshabilitado (Firebase) y GitHub MCP oficial

- **Decisión:** Registrar en `opencode.json` el servidor Firebase MCP (`npx -y firebase-tools@latest mcp`) **disabled** hasta la futura spec de notificaciones, y el servidor oficial GitHub MCP (`github/github-mcp-server`) referenciando variable de entorno para autenticación, sin tokens versionados y con preferencia read-only para análisis.
- **Estado:** APROBADO
- **Justificación:** Preparar las herramientas sin activarlas ni introducir secretos; FCM aún sin spec.
- **Impacto:** No se instala ni ejecuta ningún MCP durante fases de documentación. Decisiones también reflejadas en `AGENTS.md` sección 18.

## M-MOB-044 — Backend U2 y contrato confirmado

- **Decisión:** Backend .NET 10 en Azure Container Apps con URL pública estable `https://ca-thehouse-u2.grayisland-cb722b5f.eastus2.azurecontainerapps.io`. Contrato confirmado: `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`, `GET /api/v1/properties`, `GET /api/v1/properties/{id}`, `GET /api/v1/universities`, `GET /api/v1/universities/{id}`, `GET /health/live`, `GET /health/ready`. **No confirmados:** `POST /api/v1/auth/refresh` y `GET /api/v1/users/me`.
- **Estado:** APROBADO
- **Justificación:** Fuente única de verdad del contrato móvil ↔ backend; no se inventan endpoints.
- **Impacto:** Los flujos móvil que requieran refresh o identificación de usuario (bienvenida personalizada, renovación automática) quedan `BLOCKED-BACKEND` hasta evidencia del contrato real. `API_BASE_URL` configurable, no hardcodeada en clases Kotlin.

## M-MOB-045 — Resultado de ejecución U2 (NV-03 + NV-04)

- **Decisión:** Registrar los hallazgos reales de la FASE B U2: NV-04 ejecutada (auth + onboarding) con `BUILD SUCCESSFUL` en `clean assembleDebug testDebugUnitTest lintDebug`; 401 del login funcional; `POST /api/v1/auth/register` rechaza los payloads razonables ({name,email,password,acceptedTerms,acceptedPrivacy} y variantes) con 400 Problem Details "Entrada inválida". AAB release generado PERO sin firmar en la sesión del agente (variables THEHOUSE_* no visibles fuera de la sesión del autorizador).
- **Estado:** APROBADO
- **Justificación:** Evidencia real registrada en `specs/nv-04-auth-onboarding/evidence.md` y `specs/nv-03-release-play/evidence.md`.
- **Impacto:** `BLOCKED-REGISTER-CONTRACT` (se necesita contrato exacto del body de register) y `BLOCKED-SIGNING-ENV` (re-firmar AAB en sesión con variables). Login/logout punta a punta quedan parcialmente validados (endpoint reactivo; flujo completo requiere register). No se sube nada a Play.

## M-MOB-046 — Estado real de tokens de sesión tras U2

- **Decisión:** El access token vive SOLO en el proceso (InMemorySessionStore); el refresh token se cifra con AES-GCM + Android Keystore y se persiste en DataStore excluido de backup. Sin `/auth/refresh` confirmado NO hay renovación automática ni sesión "siempre viva": ante 401 o proceso nuevo sin sesión válida, la app limpia el estado y regresa a Login.
- **Estado:** APROBADO
- **Justificación:** Mitigación mínima de MASVS-STORAGE/AUTH según lo exigido por NV-04 v1.0.0.
- **Impacto:** Implementación de renovación (OkHttp Authenticator + Mutex) queda documentada y `BLOCKED-BACKEND` hasta contrato real.

## M-MOB-047 — Contrato register resuelto (corrección 2026-11-05)

- **Decisión:** Backend confirmó POST /api/v1/auth/register con body exacto {"email","password"} (password 12–128, campos extra NO permitidos, Content-Type: application/json, sin 
ame). Prueba real Azure: 201 Created. BLOCKED-REGISTER-CONTRACT resuelto y eliminado de evidencia NV-04.
- **Estado:** APROBADO
- **Justificación:** Verificación real contra backend más cambios unitarios verdes: 40/40 tests.
- **Impacto:** RegisterRequestDto solo email+password; AuthRepositoryImpl no envía 
ame/consentimientos (quedan solo en la UI); AuthValidators registra mínimo 12 y máximo 128 en registro; login limitado sin cambio de contrato (validadores login intactos). No se cambiaron otros endpoints.

## M-MOB-049 — D9 Requests + Real Chat (2026-11-12)

- **Decisión:** Ejecutar D9.1 (autorización humana explícita): integración real de solicitudes (estudiante/arrendador), conversaciones, mensajes, unread/read y polling con ETag en `feature/requests-real-chat` (base = main + merge D11). Reemplaza el estado CHAT-BACKEND-BLOCKED. Contratos implementados exactamente según el orden; sin userId (Bearer por interceptor existente), un solo Retrofit. Polling respeta `pollAfterSeconds` (default 10 s), `after` + `If-None-Match`, 304 no modifica lista, se cancela al salir (StopPolling/onCleared, CancellationException rethrow). Envío idempotente con clientMessageId UUID estable en retry. Entradas de UI: Home ("Mis solicitudes", "Conversaciones", "Solicitudes recibidas (arrendador)") y Room Detail ("Solicitar este cuarto" → composer; "Mis solicitudes" en top bar).
- **Estado:** APROBADO por instrucción humana explícita ("EJECUTA D9.1").
- **Justificación:** Eliminar el bloqueo CHAT-BACKEND-BLOCKED con contratos reales del backend desplegado.
- **Impacto:** Validación real: `compileDebugKotlin` verde, `testDebugUnitTest` 149 tests verdes, `lintDebug assembleDebug` verde (0 errores lint). Backend desplegado (universities 200). Runtime punta a punta pendiente (sin emulador/adb ni credenciales autorizadas en la sesión) → RUNTIME-BACKEND-PENDING parcial. Spec en `specs/d9-requests-real-chat/`.

## M-MOB-048 - Gobernanza D11 Profile/Preferences/Visual Polish (2026-11-12)

- **Decisi�n:** Se registra la spec D11 en `specs/d11-profile-preferences-visual-polish/` (spec/plan/task/checklist/evidence), separada de nv-11-chat y de toda otra spec. FASE A (functional stabilization) activa; FASE B (Visual Polish) PLANNED, NO EJECUTADA. Branch `feature/profile-preferences-ui-polish` creada desde el estado real de main con working tree preservado (sin reset/restore/clean/stash/force push).
- **Estado:** APROBADO por instrucci�n humana expl�cita.
- **Justificaci�n:** Trabajo funcional Profile/Preferences/Quick Setup existente sin spec; la trazabilidad mapea "Perfil / cuenta" a NV-27 (BLOCKED). `compileDebugKotlin` real: BUILD FAILED (23 archivos borrados en working tree requeridos por ~30 archivos sobrevivientes; adem�s errores en c�digo D11 nuevo: `profile interface`, `SelectUniversityUseCase2`, `selectUniversity` duplicado). Eliminaciones clasificadas cambio ajeno/dudoso, pendientes de decisi�n humana. D11-FUNCTIONAL-READY NO declarado.
- **Impacto:** Ning�n commit de c�digo funcional hasta validaci�n verde; Visual Polish bloqueado hasta FASE A lista.
- **CIERRE DOCUMENTAL (2026-11-26, autorización humana individual):** RESUELTO POR RESTAURACIÓN. La restauración selectiva de los 23 archivos ya se ejecutó en su día (`specs/d11-profile-preferences-visual-polish/evidence.md:30-47`, con `compileDebugKotlin` BUILD SUCCESSFUL y commit `feat: add real profile preferences and quick setup`). Los paths están presentes en `main`/`origin/main` (`2626e7cf`); la rama D11 se integró por fast-forward. NO se restauró ningún archivo nuevamente. Quedan actualizadas las trazaciones obsoletas: esta entrada, `specs/d11-profile-preferences-visual-polish/task.md` (D11-T30), `checklist.md` y `docs/sdd/REQUIREMENTS_TRACEABILITY.md` (que afirmaban que `compileDebugKotlin` fallaba por 23 archivos borrados). Deuda técnica cerrada; deuda documental cerrada.

## M-MOB-050 - D12 Landlord Buildings/Rooms (2026-11-12)

- **Decisión:** Ejecutar D12 (autorización humana explícita «EJECUTA D12»): flujo real de arrendador contra el contrato NV-12 Backend §6.1 — alta de perfil (POST /landlord/profile, acceptedTermsVersion 2026-11), edificios (list+summary/detail/crear/editar/eliminar), cuartos (list/generate/editar/eliminar, pause/resume), publicación (publication pasos + publish 202 → in_review) y concurrencia optimista con ersion (PATCH cuerpo, DELETE query; 409 version_conflict → mensaje + recarga, sin retry). Branch eature/landlord-buildings-rooms baseada en eature/requests-real-chat (main aún sin merge D9.1/D11). Sin endpoints/campos/estatos inventados; sin upload multimedia (NV-13). Reutiliza Retrofit/auth/DomainError/Design System D11; un solo Retrofit.
- **Estado:** APROBADO por instrucción humana explícita.
- **Justificación:** Contrato real extraído de TheHouse-Backend origin/main (specs/nv-12-landlord-buildings-rooms/spec.md v1.1.0); sin desviaciones.
- **Impacto:** compileDebugKotlin, 	estDebugUnitTest lintDebug assembleDebug verdes; 25/25 tests D12; regresión D3–D11 verde; lint 0 errores. Runtime punta a punta BLOCKED (sin emulador/adb ni credenciales de prueba). Spec en specs/d12-landlord-buildings-rooms/. Deuda: UI duplicate/copy-from.

## M-MOB-051 - D13 Reportes, verificación y moderación (2026-11-12)

- **Decisión:** Ejecutar D13 (autorización humana explícita) en `feature/reports-verification-moderation` reanudando el working tree interrumpido (sin reset/restore/clean/stash): reportes reales `POST /api/v1/reports` (body exacto sin userId, motivos y `details` ≤500 del contrato NV-21 §6.1), verificación del dueño `GET /landlord/buildings/{id}/verification` integrada en el detalle de edificio D12, estados `rejected|suspended` en `BuildingStatus`, suspensión global de cuenta por 403 `account_suspended` (login/refresh/restore/interceptor, sin logout automático ni borrado de refresh token) y pantalla global con logout solo explícito. Entradas reales en Request Detail: «Reportar cuarto» (`request.room.id`) y «Reportar edificio» (`request.room.buildingId`) → `ReportRoute(ROOM|BUILDING, id)` registrada en `MainActivity` con validación de target.
- **Estado:** APROBADO por instrucción humana explícita.
- **Justificación:** Contrato espejo `specs/nv-21-verification-moderation/spec.md` v1.1.0 §6/§6.1; sin endpoints/DTOs/enums inventados; COST-001 $0 (sin proveedores nuevos).
- **Impacto:** `compileDebugKotlin` verde tras corregir la integración interrumpida (`onReportRoom` sin conectar, ruta sin registrar, `getVerification` sin inyectar, `when` incompleto, import duplicado); `testDebugUnitTest lintDebug assembleDebug` BUILD SUCCESSFUL con 206 tests / 0 failures / 0 errors (D13: 15 + 8 + 9). RUNTIME-BLOCKED (sin adb/emulador). T10 Moderación permanece BLOCKED (NV-13 borrador). Spec en `specs/d13-reports-verification-moderation/`.

## M-MOB-052 — NV-14 FASE B Push FCM real (2026-11-17)

- **Decisión:** Implementar FCM real en `feature/real-notifications` preservando FASE A: data-only keys `notificationId/category/title/body/targetKind/targetId`, `NotificationDestinationResolver` compartido como único mapper target→navegación, foreground solo emite refresh signal (sin duplicar notificación), background notifica localmente, sin sesión se suprime, canales `thehouse_requests/thehouse_visits/thehouse_system`, permiso `POST_NOTIFICATIONS` contextual Android 13+.
- **Estado:** APROBADO por instrucción humana explícita para FASE B Mobile.
- **Justificación:** Contratos reales FASE A (`/users/me/notifications`) sin inventar endpoints; no existe contrato Backend de token/device (`FCM-TOKEN-BACKEND-BLOCKED`), token FCM solo en memoria y nunca enviado. Sin `google-services.json` no se crea archivo falso.
- **Impacto:** Gate final `testDebugUnitTest lintDebug assembleDebug` BUILD SUCCESSFUL: 297 tests / 0 failures, lint 0 errores / 20 warnings, APK debug generado. Runtime FCM parcial por falta de `google-services.json`, sender Backend y emulador ADB sin `system_server`/package service. Migración futura de token clásico a FID `register()` fuera de alcance.

## M-MOB-053 - NV-15 Privacy + Delete Account Mobile (2026-11-17)

- **Decisión:** Ejecutar NV-15 Mobile (autorización humana explícita) en `feature/privacy-delete-account`: ruta pública `privacidad` con entrada desde Home, URL centralizada `BuildConfig.PRIVACY_POLICY_URL` (vacía), validación HTTPS pura, diálogo de confirmación irreversible con cancelar/loading/doble-submit, `DeleteAccountUseCase` con guard de contrato y purga local posterior al éxito (sesión/refresh token, onboarding del usuario, favoritos, target push, notificaciones locales y caché HTTP). Sin endpoint, DTO ni URL inventados.
- **Estado:** PARTIAL APROBADO en Mobile: `DELETE-ACCOUNT-BACKEND-BLOCKED` + `BLOCKED-PRIVACY-URL`.
- **Justificación:** `origin/main` Backend `9dfb497` no expone ruta de eliminación y `specs/nv-10-account-session/spec.md:23` la difiere; la landing solo tiene anclas `#privacidad` sin contenido real. `AccountRepository.isDeletionAvailable=false` muestra el bloqueo sin simular borrado.
- **Impacto:** Gate final `testDebugUnitTest lintDebug assembleDebug` BUILD SUCCESSFUL: 323 tests / 0 failures, lint 0 errores / 20 warnings (ninguno en privacy), APK debug generado. Runtime en dispositivo NO EJECUTADO (sin evidencia ADB). Spec actualizada en `specs/nv-15-privacy-delete-account/`.

## M-MOB-054 - NV-21/NV-22 Permisos y hardening multimedia Mobile (2026-11-17)

- **Decisión:** Ejecutar NV-21/NV-22 Mobile (autorización humana explícita) en `feature/permissions-multimedia`: auditar permisos/plataforma (solo `INTERNET` y `POST_NOTIFICATIONS` justificados; sin cámara/ubicación/storage; sin deep links; backups/data extraction seguros), preservar NV-14 contextual sin startup request, y preparar hardening multimedia local sin contratos/Backend inventados (Photo Picker preferido, validación MIME/tamaño/URI/archivo vacío, saneamiento best-effort de imagen por recompresión/orientación en IO para no propagar EXIF GPS/metadatos innecesarios, estados honestos sin afirmar moderación/upload, texto validado sin filtro IA).
- **Estado:** NV-21 **PARTIAL** (inventario/runtime/plataforma PASS; conciliación Data Safety completa bloqueada por `BLOCKED-PRIVACY-URL` NV-03). NV-22 **PARTIAL** con **`MULTIMEDIA-MODERATION-BACKEND-BLOCKED`** (sin contrato ejecutable de moderación/upload; sin flujos UI reales de cámara/picker/upload).
- **Justificación:** No existen flujos actuales de cámara/galería/upload ni contrato backend de moderación ejecutable (NV-13 media/docs borrador, D13 automático bloqueado). COST-001 $0, mínimo privilegio, sin simulaciones.
- **Impacto:** Nuevas utilidades/tests (`domain/multimedia`, `data/multimedia`, `presentation/multimedia`). Gates finales reales: `testDebugUnitTest` BUILD SUCCESSFUL, `lintDebug` BUILD SUCCESSFUL, `assembleDebug` BUILD SUCCESSFUL. Runtime ADB no disponible (no ejecutado). Specs actualizadas en `specs/nv-21-android-permissions-platform-security/` y `specs/nv-22-multimedia-cdn-content-moderation/`.

## M-MOB-056 — Smart Match runtime: diagnóstico y logging DEBUG (2026-11-26)

- **Decisión:** Ejecutar el runbook humano «SMART MATCH RUNTIME: NO HTTP REQUEST» sin tocar Backend, contratos, GenAI ni ML. Se agrega `SmartMatchFlowLogger` (tag `SmartMatchFlow`, solo DEBUG, sin PII/tokens/bodies/stacktraces) y se instrumenta `SmartMatchViewModel`/`SmartMatchScreen`; `load()` sigue siendo el único disparador (LaunchedEffect(Unit) + retry explícito, sin loops ni key inestable).
- **Estado:** APROBADO por instrucción humana explícita; gates locales PASS.
- **Justificación:** En guest, `loadPreferenceOrNull()` llamaba a `GET /users/me/preferences` sin Bearer y tragaba el 401, cayendo en un StateFlow `null` → NoPreferences sin query HTTP de Smart Match. En Active el flujo usa candidatos + ranking on-device y no llama al endpoint guest. Las excepciones ahora son observables en DEBUG pero la UI sigue recibiendo un `UiError` seguro; lista vacía → Empty, preferencias ausentes → NoPreferences (nunca Error genérico).
- **Impacto:** Factory verificada por inspección (dependencias reales, sin placeholders). Tests nuevos A–F + factory/sesiónmutable. Gates reales: `clean compileDebugKotlin` OK, `testDebugUnitTest` 412 tests / 0 failures, `lintDebug` 0 errores, `assembleDebug` OK, `git diff --check` exit 0. Sin dispositivo/ADB en la sesión: captura de `SmartMatchFlow`/`TheHouseHttp` en runtime queda pendiente; la causa raíz está demostrada en código/tests, no afirmada en dispositivo. Evidencia en `specs/nv-17-smart-match-ml-ai/evidence.md` §11.

## M-MOB-057 — Smart Match AUTH migrado a autoridad Backend NV-23 v1.2.0 (2026-11-26)

- **Decisión:** El flujo `SessionState.Active` de Smart Match usa `GET /api/v1/smart-match/recommendations` como única llamada online. El Backend resuelve perfil/preferencias/universidad/scoring/ranking/breakdown/dismissed; Mobile NO reordena ni re-scorea respuestas 200. El ML on-device de NV-17 queda exclusivamente como fallback offline/network/503, con recuperación a resultado remoto. 422 `smart_match_profile_incomplete` (`missing`) → NoPreferences; 401 → RequiresAuth; 400/404/429 → Error tipado; sin preferencias locales en el camino online. Se elimina el mapper que convertía `DomainException` en `Unknown` y se usa `Throwable.toSafeUiError()`.
- **Estado:** APROBADO por instrucción humana explícita; gates locales PASS; **RUNTIME PENDIENTE** (sin ADB/dispositivo).
- **Justificación:** Contrato real auditado en código productivo del Backend `origin/main` (`SmartMatchEndpoint.cs`, `SmartMatchService.cs`, `SmartMatchContracts.cs`, `SearchContracts.cs`, `UserEndpointSupport.cs`). El DTO plano histórico de Mobile no coincidía con la respuesta anidada del Backend. NV-23 v1.2.0 fue aceptado por Mobile (`FINAL-MOBILE-INTEGRATION/spec.md:45`).
- **Impacto:** Mismo PR #29 / rama `fix/smart-match-guest-runtime`, sin merge. DTOs alineados, repository con mapeo tipado, ViewModel Active reescrito, logger con `auth_query_result`/`fallback_start`. Gates: `clean compileDebugKotlin`, `testDebugUnitTest` 423 tests / 0 failures, `lintDebug` 0 errores, `assembleDebug`, `git diff --check` exit 0. Evidencia en `specs/nv-17-smart-match-ml-ai/evidence.md` §12. Matiza M-MOB-056: lo que antes se describía como "Active usa ML on-device" ya no aplica online.

## M-MOB-055 - Hardening de runtime / performance Mobile (2026-11-25)

- **Decisión:** Ejecutar el hardening de runtime pedido (autorización humana explícita, runbook secciones 1-13) en `fix/mobile-runtime-performance` desde `main` actualizado, sin mezclar PR #27 (abierto): (a) logging HTTP DEBUG con **ruta relativa** (`HttpUrl.encodedPath`), status, duración y `requestId` si el Backend lo envía, enmascarando segmentos de ruta con posible PII y **sin** query, headers, bodies ni tokens; (b) instrumentación `PERF <operation> durationMs=<n>` (nuevo paquete `perf/Perf`) habilitada solo con `BuildConfig.DEBUG`, con spans `network.*`, `mapping.*`, `state.update`, `smartmatch.ranking` e `image.load.*`; (c) **single-flight** del unread count en `HomeViewModel` para que ON_START + ON_RESUME + tick de polling en la misma tanda no dupliquen `GET /users/me/notifications/unread-count`; (d) claves estables de `LaunchedEffect` en `PropertyDetail` vía `rememberUpdatedState` y `collectAsStateWithLifecycle` en Home. Sin cambios de timeouts, contratos, Backend, ML, Firebase ni dependencias.
- **Estado:** APROBADO por instrucción humana explícita; gates locales PASS.
- **Justificación:** El log anterior solo registraba el host, por lo que el `GET ... -> 200 (23020ms)` era **indeterminado**. Medición externa de solo-lectura (curl contra los GET públicos del Backend) dio 0.23-0.90 s en caliento (15 sondas) y **1 estancamiento >120 s** → el caso observado es un stall intermitente (red/servidor/emulador), no un endpoint crónicamente lento; se requiere el nuevo log (o `PERF network.*`) en dispositivo para atribuirlo. Payloads medidos muy pequeños (properties 3 208 B, universities 2 145 B, detail 389 B) y artefacto ML de 970 B, por lo que no se optimizó a ciegas el mapeo/carga de modelo.
- **Impacto:** Matiza **M-MOB-040**: el logging HTTP ya no se limita a host; conserva la prohibición de query, headers, bodies, tokens y PII. Nuevas pruebas (`PerfTest`, `PerfCatalogSpansTest`, `SafeHttpLoggingInterceptorTest`, `HomeRequestDeduplicationTest`) sin modificar tests existentes. Gates reales: `clean compileDebugKotlin` OK, `testDebugUnitTest` 399 tests / 0 failures, `lintDebug` 0 errores (24 warnings preexistentes, ninguno en archivos tocados), `assembleDebug` OK, `git diff --check` exit 0. Sin dispositivo/ADB: valores runtime `NETWORK/MAPPING/STATE_DURATION_MS` pendientes de captura; `SLOW_ENDPOINT` pendiente del primer log con ruta.

<<<<<<< HEAD
## M-MOB-059 — D15 Home Account Drawer (2026-11-26)

- **Decisión:** Ejecutar D15 (autorización humana individual) en `fix/home-account-drawer-navigation` desde `main` @ `2626e7cf`, sin copiar cambios de D14: eliminar "Tu cuenta" y sus 12 filas del scroll de Home y elevar un drawer hamburguesa **único** al shell (`MainActivity.TheHouseRoot`). 11 rutas reales cableadas; `onOpenNotifications` corregido. Guest solo acciones públicas; auth 7 ítems; arrendador en sección visual temporal sin `isLandlord`/`landlordEnabled` ni rol nuevo (el backend sigue siendo autoridad). Logout reutiliza `container.logout()` (`LogoutUseCase`).
- **Estado:** APROBADO / IMPLEMENTADO / TESTED / EVIDENCIADO. **RUNTIME PENDIENTE** (sin ADB/dispositivo).
- **Justificación:** Inventario auditado: 11/11 rutas con pantalla y ViewModel reales, sin pantallas dummy; COST-001 $0; sin endpoints ni secretos nuevos.
- **Impacto:** `DrawerItemCatalog.kt` es catálogo puro (sin Compose/Android) que hace testeables las reglas de sesión/rol; `AppDrawer.kt` renderiza y despacha. Gates reales: `clean compileDebugKotlin`, `testDebugUnitTest` 413 tests / 0 failures (+11), `lintDebug`, `assembleDebug`, `git diff --check` exit 0. Spec/plan/task/checklist/evidence en `specs/d15-home-account-drawer/`. PR separado, sin merge sin runtime y aprobación.
=======
## M-MOB-058 — D14 Preferences Auth Runtime (2026-11-26)

- **Decisión:** Ejecutar D14 (autorización humana individual) en `fix/preferences-auth-runtime` desde `main` @ `2626e7cf`: contrato exacto `GET|PUT /api/v1/users/me/preferences` contra el backend productivo confirmado; logger DEBUG `PreferencesFlow` sin PII; mapeo `HttpException`→`DomainException` con caché solo para indisponibilidad; `pets` bool, catálogos cerrados en códigos, budget `Int` MXN 0..50000 sin redondeo silencioso; `university` NO se edita en Preferences (se orienta a Perfil/Quick Setup); PUT 200 actualiza estado y caché sin GET posterior. PR separado, sin merge sin runtime y aprobación.
- **ROOT CAUSE REAL (añadido durante implementación):** `PreferencesDto`/`BudgetDto`/`PreferencesPatchBody`/`BudgetPatchBody` **no tenían `@Serializable`** → `SerializationException` en cada respuesta → `<Unknown>` → "Ocurrió un error inesperado". Matiza la auditoría: el mapper central `ThrowableToUiError` era correcto; no existía el bug `DomainException→Unknown` planteado. Los 9 tests de integración fallaban antes del fix y pasan después.
- **Estado:** APROBADO / IMPLEMENTADO / TESTED / EVIDENCIADO. **RUNTIME PENDIENTE** (sin ADB/dispositivo); `BLOCKED` nunca equivale a `DONE`.
- **Justificación:** Contrato auditado en código productivo del Backend `origin/main` @ `3d7bd478`; sin endpoints/códigos inventados; COST-001 $0 (sin servicios nuevos).
- **Impacto:** Gates reales: `compileDebugKotlin`, `testDebugUnitTest` 417 tests / 0 failures (+15 nuevos), `lintDebug`, `assembleDebug`, `git diff --check` exit 0. Spec/plan/task/checklist/evidence en `specs/d14-preferences-auth-runtime/`. No se tocó Smart Match ni `MainActivity.kt`. Requiere captura `PreferencesFlow`/`TheHouseHttp` (GET 200, PUT 200) y `GET recommendations -> 200` o `missing` documentado antes de merge.
>>>>>>> origin/main
