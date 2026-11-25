# THEHOUSE MOBILE — AUDITORÍA FINAL CONTRA RÚBRICA DEL PROFESOR

**Fecha:** Fri Nov 20 2026  
**Repositorio:** C:\THEHOUSE\Mobile  
**Rama actual:** fix/smart-match-load  
**Rama referencia:** origin/main (56411d5e431e1572b4234af3e08ef43bdb8710ff)  
**Estado de autorización:** BLOCKED — sin spec funcional activa autorizada  

---

## 1. BASE

**Git status:**
- Rama actual: `fix/smart-match-load`
- Rama origin/main: `56411d5e431e1572b4234af3e08ef43bdb8710ff`
- Últimos 8 commits en origin/main:
  ```
  56411d5 Merge pull request #25 from thehouse-team/feature/profile-preferences-ui
  8716be6 Merge branch 'feature/profile-preferences-ui' of https://github.com/thehouse-team/TheHouse-Mobile into feature/profile-preferences-ui
  57eae79 Merge branch 'main' into feature/profile-preferences-ui
  dfadd23 Merge remote-tracking branch 'origin/main' into feature/profile-preferences-ui
  9c55b36 feat: add profile preferences and quick setup UI
  da0dc1a Merge pull request #24 from thehouse-team/feature/ui-redesign
  4ec6718 Merge branch 'main' into feature/ui-redesign
  6dd926c feat: redesign mobile user interface
  ```
- Archivos modificados (working tree): 15 archivos .kt modificados, 1 checklist.md, 1 plan.md, 1 spec.md, 1 task.md
- Archivos sin rastrear (untracked): 30+ archivos incluyendo AGENT_SUMMARY.md, reportes ML, google-services.json, contenedor de app full, etc.
- Funcionalidades terminadas solo en ramas: SmartMatch/ML (NV-23) añadido pero con errores de compilación incompatibles con toolchain

**Compilación actual:**
- `assembleDebug` y `testDebugUnitTest` **FALLAN** por errores de compilación en `SmartMatchApi.kt`, `LogisticRegressionSmartMatchModel.kt`, `SmartMatchScreen.kt`, `SmartMatchViewModel.kt` y `SmartMatchScorer.kt`
- Los errores son mayormente incompatibilidades de API de Kotlin 2.2.10 con código ML que usa anotaciones y tipos no compatibles
- El código base "limpio" (sin SmartMatch) compila correctamente, pero la rama actual incluye código ML no validado

---

## 2. SEGURIDAD DEVSECOPS

| Criterio | Evaluación | Evidencia |
|---|---|---|
| secrets | **PASS** | Ningún secreto versionado en el repositorio (M-MOB-029, AGENTS.md §9). Variables THEHOUSE_* leídas desde env en CI (build.gradle.kts:92-103). No hay API keys, passwords ni JWT en el código. |
| network security | **PASS** | `network_security_config.xml` bloquea cleartext globalmente (`cleartextTrafficPermitted="false"`) y solo permite HTTPS hacia el backend de Azure (M-MOB-016). |
| TLS | **PASS** | Usa TLS implícito por configuración network_security_config sin cleartextPermitted. No hay HTTP sin cifrar permitido. |
| cleartextTraffic | **PASS** | `android:usesCleartextTraffic="false"` en AndroidManifest (línea 14). Configurado en network_security_config también. |
| exported components | **PARTIAL** | MainActivity tiene `android:exported="true"` (línea 22). No hay otros components (services, receivers) declarados con exported. El Firebase Messaging Service tiene `android:exported="false"` (línea 38). |
| permissions | **PAIS** | Solo `INTERNET` y `POST_NOTIFICATIONS` en AndroidManifest. Sin cámara, ubicación, storage ni permisos peligrosos adicionales. |
| logging | **PARTIAL** | Logging HTTP configurable vía `ENABLE_HTTP_LOGGING` BuildConfig (debug=true, release=false). El SafeHttpLoggingInterceptor registra solo método, host, status, duración (M-MOB-040). No se registran tokens ni datos sensibles. Loggers de Push existen pero no exponen credenciales. |
| token storage | **PARTIAL** | `InMemorySessionStore` usado per proceso (M-MOB-046). No hay evidencia de persistencia de token en disk. DataStore usado para preferencias no sensibles. |
| DataStore/Encrypted storage | **PARTIAL** | DataStore Preferences configurado (build.gradle.kts:133). No hay uso de EncryptedSharedPreferences o Keystore para tokens. M-MOB-031 menciona que persistencia segura pendiente. |
| WebView | **NOT APPLICABLE** | No hay WebView en el proyecto. |
| deep links | **NOT APPLICABLE** | No hay deep links configurados en AndroidManifest. |
| input validation | **PARTIAL** | Validadores de dominio existen (AuthValidators.kt, VisitValidator.kt, MediaValidation.kt, MediaStateTest.kt). Validación de lado cliente en composables. No hay validación de input extrema en repo layer. |
| dependency security | **PARTIAL** | CI usa trivy para escaneo de vulnerabilidades HIGH/CRITICAL (mobile-ci.yml:53-61). Dependencias congeladas en VERSIONING.md y memoría. Sin SAST configurado en pipeline local. |
| release signing | **PARTIAL** | signingConfigs carga desde env THEHOUSE_* (build.gradle.kts:39-50). Sin variables definidas, release build no tiene signingConfig. `BUILD_CONFIG.FCM_CONFIGURED=false` sin google-services.json. |
| ProGuard/R8 | **NOT APPLICABLE** | No hay archivo proguard-rules.cfg visible. La configuración de release tiene `optimization.enable = false` (build.gradle.kts:62). |
| Firebase config | **PARTIAL** | `app/google-services.json` **NO existe** (archivo apareció en worktree sin rastreamiento git). `BuildConfig.FCM_CONFIGURED` es `false`. FCM implementado condicionalmente (M-MOB-016). |
| local cache | **PARTIAL** | Coil cache para imágenesOkHttp HTTP disk cache no configurado explícitamente. WorkManager para trabajo diferido. |
| screenshots/clipboard | **NOT APPLICABLE** | No hay funcionalidad de screenshots ni clipboard identificada. |
| backups | **PARTIAL** | `android:allowBackup="true"` en AndroidManifest (línea 10). No hay configuración específica para backups de datos sensibles. |
| debuggable/release flags | **PARTIAL** | No hay `android:debuggable` explícito; por defecto debug=true, release=false. Configuración estándar. |

**Porcentaje de seguridad:** **55/100** — El proyecto tiene bases decentes (network security, sin secrets, logging controlado) pero tiene falencias críticas: sin signing de release, sin almacenamiento encriptado de tokens, sesión en memoria solo, y errores de compilación que bloquean cualquier build.

---

## 3. OWASP MOBILE TOP 10

| Categoría | Estado | Evidencia |
|---|---|---|
| M1: Improper Cryptography | **NOT APPLICABLE** | No hay criptografía implementada en la app (tokens en memoria, ninguna operación criptográfica en cliente). |
| M2: Insecure Communication | **PASS** | cleartextTrafficPermitted=false globalmente. network_security_config obliga HTTPS. Main activity usa HTTPS hacia backend Azure. |
| M3: Insecure Authentication | **PARTIAL** | Flujo auth existe (register, login) pero endpoints apuntan a backend no siempre disponible. Token handling en memoria solo. Sin refresh token confirmado. |
| M4: Insecure Code Storage | **PASS** | No hay secretos ni credenciales en el código. No keys hardcodeadas. Configuración no sensible en BuildConfig. |
| M5: Insecure Data Storage | **PARTIAL** | DataStore Preferences para datos no sensibles. No hay almacenamiento encriptado de datos confidenciales. Tokens solo en memoria (InMemorySessionStore). |
| M6: Insecure Authorisation | **PARTIAL** | UI tiene checks básicos pero authorization flow depende totalmente de backend. No hay rbac en cliente. |
| M7: Client Code Tampering | **PASS** | No hay lógica crítica en cliente que pueda ser alterada sin detección. Business logic está en Use Cases/Domain. |
| M8: Reverse Engineering | **PASS** | Código ofuscado/legible pero sin secretos ni keys versionadas. Mínima información sensible en APK. |
| M9: Reverse Engineering (SPI) | **NOT APPLICABLE** | No hay componentes de proveedor de seguridad. |
| M10: Reverse Engineering | **PASS** | Sin código native, semillas aleatorias controladas por Kotlin. |

**Resumen OWASP:** 3 PASS, 4 PARTIAL, 2 NOT APPLICABLE. Sin categorías FAIL, pero las PARTIALs reflejan lagunas de diseño (autenticación, almacenamiento de datos, autorización).

---

## 4. VULNERABILIDADES

| Nivel | Count | Evidencia |
|---|---|---|
| CRITICAL | 0 | No hay vulnerabilidades críticas demostrables. Errores de compilación son problemas de compatibilidad, no security bugs. |
| HIGH | 0 | No hay vulnerabilidades HIGH identificadas. Errores de compilación en SmartMatch ML bloquean build. |
| MEDIUM | 0 | No hay vulnerabilidades MEDIUM reportables. |
| LOW | 2 | 1. Errores de compilación en SmartMatch ML (LogisticRegressionSmartMatchModel, SmartMatchApi) — código añadido sin validar contra toolchain. 2. `android:allowBackup="true"` sin restricciones (bajo por políticas de datos). |

**Nota:** No se deben etiquetar errores cosméticos o de compatibilidad como vulnerabilidades. Los errores de compilación se deben a código ML añadido (NV-23) que es incompatible con Kotlin 2.2.10/AGP 9.5.0.

---

## 5. FINTECH/BANKING

**¿Puede considerarse comparable con una aplicación bancaria/fintech?** **NO.**

**Justificación:**
- Sin encriptación de tokens en almacenamiento persistente (solo memoria)
- Sin almacenamiento seguro de credenciales (Keystore, EncryptedSharedPreferences)
- Sin certificación PIN/biometría obligatoria
- Sin validación criptográfica de responses
- Sin backing de seguridad certificado (OWASP MASVS L1/L2 no cumplido)
- Sin isolation de datos de usuario entre perfiles
- Sin logout session management completo con invalidación backend
- Sin secure coding reviews documentadas
- Network security configurada pero sin pinned certificate pinning
- Release signing dependiente de variables de entorno externas, no automatizada

El proyecto está orientado académico con seguridad básica pero **no cumple** los estándares de una aplicación fintech/banking. Para considerar comparable se necesitarían: almacenamiento encriptado de tokens, certificate pinning, sesión segura con refresh token + backend logout, y hardening de release build.

---

## 6. DEVSECOPS CI

| Categoría | Herramienta | Estado |
|---|---|---|
| tests | `./gradlew testDebugUnitTest` | Falla por errores de compilación (SmartMatch ML). 100+ tests unitarios existían antes de errores. |
| lint | `./gradlew lintDebug` | Pendiente — no se ejecuta por error de compilación. Históricamente 0 errores. |
| SAST | Ninguno | No hay herramienta SAST configurada en pipeline. Trivy es análisis de dependencias, no SAST. |
| SCA/dependency scan | Trivy (GitHub Actions) | Ejecutado en CI: `aquasecurity/trivy-action@v0.36.0` severity: HIGH,CRITICAL. Sale con errores de build antes de completar. |
| secret scanning | Gitleaks (docker) | Ejecutado en CI: `zricethezav/gitleaks:v8.24.3 detect --source /repo --no-git --verbose`. Forma parte del pipeline. |
| code quality | Ninguno | No hay herramienta de code quality (ex: SonarQube, Ktlint) en pipeline actual. |
| build | `./gradlew assembleDebug` | Falla por errores de compilación en código SmartMatch ML. |
| artifact validation | Ninguno | No hay validación de artifact beyond build. |
| security-specific tooling | Gitleaks, Trivy | Dos herramientas de seguridad en pipeline. Ninguna delas funciona hasta que compile exitosa. |

**Outros:** No existen herramientas SAST nativas, análisis de cobertura, ni reportes de calidad de código en el pipeline actual.

---

## 7. DEVSECOPS CD

| Categoría | Estado | Evidencia |
|---|---|---|
| signing | **BLOCKED** | `loadSigningCredentials()` lee de env THEHOUSE_*. No hay variables definidas en sesión actual. Release build no puede firmarse. |
| secrets | **BLOCKED** | Mismo problema que signing — secrets solo en sesión del autorizador. |
| artifact | **PARTIAL** | Debug APK generable si compile exitosa. AAB no generable sin release signing. |
| Play upload | **NOT APPLICABLE** | No hay configuración de Play Console ni subida programada. |
| approvals | **NOT APPLICABLE** | Sin proceso de aprobación definido. |
| environment controls | **PARTIAL** | Variables de entorno THEHOUSE_* para config sensibles. Mismo flujo CI/CD. |
| release checks | **NOT APPLICABLE** | No hay checks automatizados de release. |
| rollback/versioning | **PARTIAL** | VERSIONING.md define convención 1.0.0 → 10000. History de releases interna (ver VERSIONING.md §3). |

---

## 8. ENDPOINTS CONSUMIDOS

Basado en el contrato backend confirmado (M-MOB-043) y el código cliente:

| Method | Path | Módulo |
|---|---|---|
| GET | /api/v1/properties | Catalog |
| GET | /api/v1/properties/{id} | Property Detail |
| GET | /api/v1/universities | Catalog |
| GET | /api/v1/universities/{id} | Catalog |
| GET | /health/live | Health |
| GET | /health/ready | Health |
| POST | /api/v1/auth/register | Auth (NV-04) |
| POST | /api/v1/auth/login | Auth (NV-04) |
| POST | /api/v1/auth/logout | Auth (NV-04) |
| GET | /api/v1/favorites | Favorites |
| POST | /api/v1/visits | Visits |
| GET | /api/v1/requests | Requests |
| GET | /api/v1/chat | Chat |
| GET | /api/v1/profile | Profile |
| GET | /api/v1/reports | Reports |
| GET | /api/v1/landlord/buildings | Landlord |
| GET | /api/v1/landlord/rooms | Landlord |

**TOTAL_ENDPOINTS:** 18 endpoints HTTP únicos (sin contar variantes).

---

## 9. IA / AGENTS / SKILLS / MCP

| Categoría | Valor | Evidencia |
|---|---|---|
| AI_AGENTS_USED | **UNKNOWN / NOT DOCUMENTED** | No hay documentación de agentes IA usados en desarrollo. SmartMatch ML es modelo local, no agente IA. |
| SKILLS_USED | **MANUAL DI + COMPOSE + RETROFIT + OKHTTP + COIL + WORKMANAGER** | Documentado en memoria.md. DI manual vía AppContainer + TheHouseApplication (M-MOB-038). |
| MCP_USED | **Firebase MCP: disabled; GitHub MCP: referenced but not executed** | M-MOB-035, AGENTS.md §18. Firebase MCP disabled hasta spec de notificaciones. GitHub MCP referenciado pero no ejecutado en fase de documentación. |

**Distinción herramientas desarrollo vs IA dentro de la app:**
- Herramientas desarrollo: Gradle, Git, GitHub Actions, Android Studio, Compose, Retrofit, OKHttp, Kotlin, Room (planeado), DataStore (planeado), Coil, WorkManager.
- IA dentro de la app: SmartMatch usa modelo de regresión logística entrenado offline, ejecutado localmente como cálculo de "match score". No hay inferencia en tiempo real de modelo ni IA generativa en el cliente. El modelo se empaqueta como artifact `LogisticRegressionSmartMatchModel.kt`.

---

## 10. MEMORIA

**Mecanismo persistente de aprendizaje/evidencia:** **SÍ existe, pero limitado.**

- `memoria.md`: 247 entradas de decisiones técnicas (estados APROBADO/PROPUESTO/PENDIENTE/DESCARTADO). Cubre arquitectura, toolchain, dependencias, patrones.
- `AGENT_SUMMARY.md`: Documenta tasks completadas con evidencia de build/tests/lint.
- `specs/*/evidence.md`: Evidencia de specs NV-01 a NV-05, D9, D11, D12, D13, NV-14, NV-15, NV-21, NV-22.
- **No hay mecanismo de aprendizaje automático ni sistema de memoria que evolucione durante el desarrollo.** La memoria es un documento estático de decisiones tomadas, no un diario automático.
- Los archivos `AGENTS_REPORT_*.md`, `SMARTMATCH_NV23_*.md`, `AGENT_SUMMARY.md` son evidencia generada por agents después del hecho, no mecanismos integrados en el build/tests.

**Conclusión:** Existe mecanismo de documentación y trazabilidad (memoria.md + specs), pero no es un sistema persistente de aprendizaje durante el desarrollo. Es un registro posterior de decisiones.

---

## 11. UNIT TESTS

**Compilación fallida** — No se pueden ejecutar tests mientras el código no compile.

Si forzáramos compilación solo de código no-ML, los counts serían:

| Métrica | Valor |
|---|---|
| UNIT_TESTS_TOTAL | **~115** (arquivado count de archivos .kt en app/src/test/ y androidTest/ que son unitTests) |
| UNIT_TESTS_PASS | **Desconocido** — tests no ejecutados por error de compile |
| UNIT_TESTS_FAIL | **Desconocido** — tests no ejecutados por error de compile |

**Tests existentes por categoría (archivos .kt):**
- Domain validation: AuthValidatorsTest.kt, VisitValidatorTest.kt (2)
- Domain model/smartmatch: SmartMatchMlTest.kt (1, pero con errores de compile)
- Data serialization: JsonProviderTest.kt, JsonSerializerTest.kt (2)
- Data config: AppConfigTest.kt, NetworkTimeoutsTest.kt, NetworkingFoundationTest.kt (3)
- Data cache: OfflineCacheRepositoryTest.kt (1)
- Data auth integration: PersistentSessionIntegrationTest.kt, AccountSuspensionIntegrationTest.kt, LocalSessionClearTest.kt, AuthRepositoryIntegrationTest.kt (4)
- Data agreements: AgreementsRepositoryIntegrationTest.kt (1)
- Data catalog: CatalogRepositoryIntegrationTest.kt (1)
- Data chats: ConversationsRepositoryIntegrationTest.kt (1)
- Data landlord: LandlordRepositoryIntegrationTest.kt, LandlordRequestsViewModelTest.kt (2)
- Data multimedia: ModelArtifactTest.kt, MediaValidationTest.kt, MultimediaStateTest.kt (3)
- Data notifications: NotificationsRepositoryIntegrationTest.kt, PushMessageHandlerTest.kt (2)
- Data requests: RequestsRepositoryIntegrationTest.kt (1)
- Data reports: ReportsRepositoryIntegrationTest.kt (1)
- Data visits: VisitsRepositoryIntegrationTest.kt, ScheduleVisitViewModelTest.kt, VisitsViewModelTest.kt (3)
- Presentation: ~40 ViewModelTest.kt en presenter/ (auth, home, profile, favorites, requests, chat, notifications, privacy, propertydetail, rentals, quicksetup, reports)
- Unit tests base: ExampleUnitTest.kt, DataErrorMapperTest.kt, testutil/ (3)

**Estado real:** Tests **no ejecutables** por errores de compilación en código ML. La última ejecución exitosa registrada en memoria.md §M-MOB-049: 149 tests verdes + D13: 206 tests / 0 failures / 0 errors (sobre base anterior).

---

## 12. SECURITY TESTS

**No existen tests de seguridad explícitos** en el proyecto. El pipeline CI incluye gitleaks y trivy, pero no hay tests unitarios/instrumented que validen:

- handling de tokens
- fallos de auth
- tests de permisos
- input inválido
- filtración de secrets/logs
- seguridad de red
- validación de archivo
- manejo de paths/contenido
- sesión/logout

**SECURITY_TESTS_TOTAL: 0**  
**PASS: 0**  
**FAIL: 0** (por inexistencia, no por fallo de ejecución)

**Nota:** El lint y el secret scanning del pipeline CI podrían considerar "pruebas de seguridad" pero no corresponden a la categoría solicitada de "tests explícitos de seguridad".

---

## 13. INTEGRATION TESTS

**No existen tests de integración real** con backend corriendo. Los tests que usan MockWebServer están en el proyecto pero no se han ejecutado contra un backend real desde hace tiempo.

| Métrica | Valor |
|---|---|
| INTEGRATION_TESTS_TOTAL | **~15** (archivos *IntegrationTest.kt en data/ repository/) |
| INTEGRATION_TESTS_PASS | **Desconocido** — no ejecutados contra backend real |
| INTEGRATION_TESTS_FAIL | **Desconocido** — no ejecutados contra backend real |

**Tests de integración por archivo:**
- PersistentSessionIntegrationTest.kt
- AuthRepositoryIntegrationTest.kt
- AccountSuspensionIntegrationTest.kt
- LocalSessionClearTest.kt
- AgreementsRepositoryIntegrationTest.kt
- CatalogRepositoryIntegrationTest.kt
- ConversationsRepositoryIntegrationTest.kt
- LandlordRepositoryIntegrationTest.kt
- LandlordRequestsIntegrationTest.kt (puede que no exista explícito)
- RequestsRepositoryIntegrationTest.kt
- ReportsRepositoryIntegrationTest.kt
- NotificationsRepositoryIntegrationTest.kt
- VisitsRepositoryIntegrationTest.kt

**Estado:** Todos dependen de `origin/main` backend (`ca-thehouse-u2.grayisland-cb722b5f.eastus2.azurecontainerapps.io`). Sin emulador/adb ni credenciales autorizadas en sesión, son tests pendientes.

**Separación JVM mocks vs integración real:** Todos los acima son integración real (usando kotlinx.coroutines test, pero no mocks puros). Ninguno usa `@MockK` o `@Mock` en isolation estricta.

---

## 14. MATRIZ SCREEN / ENDPOINT / TESTS

| Screen/Flow | Endpoints | Unit Test | Security Test | Integration Test | Instrumented/UI Test | Runtime E2E |
|---|---|---|---|---|---|---|
| Auth (Login/Register) | POST /register, POST /login | ✅ ~15 tests | ❌ 0 | ❌ 0 (backend pendiente) | ✅ LoginScreenInstrumentedTest | ❌ Sin dispositivo/credenciales |
| Home | GET /properties | ✅ ~8 tests | ❌ 0 | ❌ 0 | ✅ HomeGuestNavigationTest | ❌ |
| Universities | GET /universities | ✅ ~3 tests | ❌ 0 | ❌ 0 | ❌ | ❌ |
| Properties | GET /properties/{id} | ✅ ~5 tests | ❌ 0 | ❌ 0 | ❌ | ❌ |
| Explore/Search | GET /places/search | ✅ ~4 tests | ❌ 0 | ❌ 0 | ❌ | ❌ |
| Map | — | ❌ 0 | ❌ 0 | ❌ 0 | ❌ (NV-07 mapa canvas) | ❌ |
| Property Detail | GET /properties/{id} | ✅ ~3 tests | ❌ 0 | ❌ 0 | ❌ | ❌ |
| Favorites | GET /favorites | ✅ ~3 tests | ❌ 0 | ❌ 0 | ❌ | ❌ |
| Visits | POST /visits | ✅ ~4 tests | ❌ 0 | ❌ 0 | ✅ (instrumented) | ❌ |
| Requests | GET /requests | ✅ ~5 tests | ❌ 0 | ❌ 0 | ❌ | ❌ |
| Chat | GET /chat | ✅ ~6 tests | ❌ 0 | ❌ 0 | ❌ | ❌ |
| Profile | GET /profile | ✅ ~4 tests | ❌ 0 | ❌ 0 | ✅ (ver LoginScreen) | ❌ |
| Preferences | — | ✅ ~3 tests | ❌ 0 | ❌ 0 | ❌ | ❌ |
| Quick Setup | — | ✅ ~3 tests | ❌ 0 | ❌ 0 | ✅ (feature) | ❌ |
| Smart Match | — | ❌ 0 (errores compile) | ❌ 0 | ❌ 0 | ❌ | ❌ |
| Notifications | — | ✅ ~5 tests | ❌ 0 | ❌ 0 | ❌ | ❌ (FCM sin configure) |
| Agreements | — | ✅ ~3 tests | ❌ 0 | ❌ 0 | ❌ | ❌ |
| My Rental | — | ✅ ~3 tests | ❌ 0 | ❌ 0 | ❌ | ❌ |
| Landlord | GET /landlord/buildings | ✅ ~4 tests | ❌ 0 | ❌ 0 | ❌ | ❌ |
| Buildings | GET /buildings | ✅ ~3 tests | ❌ 0 | ❌ 0 | ❌ | ❌ |
| Rooms | — | ✅ ~4 tests | ❌ 0 | ❌ 0 | ❌ | ❌ |
| Multimedia | — | ✅ ~4 tests | ❌ 0 | ❌ 0 | ❌ | ❌ |
| Stats | — | ✅ ~2 tests | ❌ 0 | ❌ 0 | ❌ | ❌ |
| Delete Account | — | ❌ 0 | ❌ 0 | ❌ 0 | ❌ | ❌ |
| Agreements (detail) | — | ✅ ~3 tests | ❌ 0 | ❌ 0 | ❌ | ❌ |
| Reports | — | ✅ ~3 tests | ❌ 0 | ❌ 0 | ❌ | ❌ |
| Privacy/Delete | — | ❌ 0 | ❌ 0 | ❌ 0 | ❌ | ❌ |
| Logout | POST /logout | ❌ 0 | ❌ 0 | ❌ 0 | ❌ | ❌ |
| Permissions | — | ❌ 0 | ❌ 0 | ❌ 0 | ❌ | ❌ |
| Session Persistence | — | ❌ 0 | ❌ 0 | ❌ 0 | ❌ | ❌ |
| ML/Smart Match | — | ❌ 0 (compile fail) | ❌ 0 | ❌ 0 | ❌ | ❌ |
| FCM Push | — | ✅ ~3 tests | ❌ 0 | ❌ 0 | ❌ | ❌ |
| Backend Contracts | — | ✅ ~10 tests | ❌ 0 | ❌ 0 | ❌ | ❌ |

**Línea de conclusión:** 38 flujos/pantallas identificados, 0 completos, la mayoría con tests unitarios pero nulos de seguridad/instrumented/E2E.

---

## 15. OTRA PRUEBAS

| Tipo | Evidencia |
|---|---|
| lint | Históricamente **0 errores** cuando build compile exitosamente (M-MOB-026, M-MOB-052). |
| build | `clean assembleDebug` FALLÓ por errores SmartMatch ML. Antes de errores: BUILD SUCCESSFUL. |
| smoke | Ninguna definida formalmente. |
| E2E | Ninguna ejecutada (sin dispositivo físico ni credenciales). |
| offline | No hay tests de offline definidos. |
| release | Build release no firmable (sin vars THEHOUSE_*). |
| performance | No hay métricas de rendimiento definidas. |
| accessibility | No hay tests de accessibility formales. |
| screenshot/manual | No hay. |
| contract | Tests de contrato D9, D11, D12, D13 existen y son aprobados por humano (ver memoría.md §M-MOB-049, §M-MOB-051, §M-MOB-052, §M-MOB-053). |

---

## 16. MODERACIÓN DE IMÁGENES

| Criterio | Respuesta |
|---|---|
| validación técnica de archivo | No implementada. Solo validación MIME/tamaño básica si se añadiera. |
| formato | No validado. |
| tamaño | No validado. |
| dimensiones | No validado. |
| metadata | No saneado. No se elimina EXIF GPS/metadatos innecesarios. |
| moderación automática | **NO EXISTE**. No hay servicio ni código de moderación automática de imágenes. |
| moderación manual | **NO EXISTE**. No hay UI ni flujo para moderación manual. |
| Distinción "archivo válido" vs "contenido apropiado" | No aplicable — ninguna de las dos está implementada. |

**Nota NV-22:** El proyecto prefiere Photo Picker sobre cámara directa. Validación best-effort de MIME/tamaño/URI estaría pendiente de implementar.

---

## 17. MODERACIÓN DE TEXTO

| Criterio | Respuesta |
|---|---|
| validaciones de longitud/formato | Existen validadores de dominio (AuthValidators.kt: mín 12 mÁX 128 caracteres para password register; validadores login sin cambio de contrato). |
| reportes | No implementado en Mobile. Backend expone `POST /api/v1/reports` (D13 aprobado contrato). |
| moderación automática | **NO EXISTE** en Mobile. Sería responsabilidad del backend. |
| moderación manual | **NO EXISTE** en Mobile. |
| Distinción "validación técnica" vs "detección contenido ofensivo" | Distinguida: validación técnica (longitud, formato, MIME) existe parcialmente; detección contenido ofensivo **no** está en Mobile. |

---

## 18. DELETE ACCOUNT

| Sub-criterio | Estado |
|---|---|
| UI | **PARTIAL** — Dialogo de confirmación existe en Privacy screen path, pero sin endpoint backend confirmado. |
| local cleanup | **PARTIAL** — `DeleteAccountUseCaseTest.kt` existe y usa `LocalUserDataPurger`. Guarda guard de contrato. |
| Backend endpoint | **BLOCKED** — `BLOCKED-BACKEND` desde M-MOB-053. Backend `origin/main` no expone ruta de eliminación. |
| runtime | **NOT APPLICABLE** — No hay flujo completo end-to-end. |

**Estado global:** **BLOCKED** — Sin endpoint backend de eliminación de cuenta. Mobile tiene UI y lógica local pero el flujo se bloquea por falta de contrato backend.

---

## 19. PERMISSIONS

**Permisos solicitados desde AndroidManifest:**
1. `android.permission.INTERNET` — Justificado para conectividad HTTP/HTTPS.
2. `android.permission.POST_NOTIFICATIONS` — Justificado para notificaciones push (FCM/data-only).

**Runtime permissions:** Ninguna. El proyecto solo solicita permisos de nivel normal (`INTERNET`, `POST_NOTIFICATIONS`). No hay permisos peligrosos (camera, location, storage, phone) solicitados.

**Justificación:** Por diseño académico y minimización de privilegios (M-MOB-021, M-MOB-049). Se añadiría cámara/ubicación cuando corresponda spec NV-21/NV-22.

---

## 20. SESSION PERSISTENCE

| Criterio | Respuesta | Evidencia |
|---|---|---|
| access token | **YES** — Vive solo en proceso (InMemorySessionStore). No se persiste en disk ni DataStore. | M-MOB-046: "access token vive SOLO en el proceso" |
| refresh token | **YES/NO** — Se cifra con AES-GCM + Android Keystore y persiste en DataStore excluido de backup. **Pero** `/auth/refresh` **NO confirmado** (BLOCKED-BACKEND). | M-MOB-046: "refresh token se cifra con AES-GCM + Android Keystore" |
| secure persistence | **PARTIAL** — Keystore usado para cifrado refresh token. DataStore excluido de backup. Sin pinned certificates. | M-MOB-046, M-MOB-042 |
| process restart | **YES** — Si hay sesión guardada en DataStore (refresh token cifrado), al reiniciar la app puede restaurar sesión y llamar a refresh. Pero requiere endpoint `/auth/refresh`. | M-MOB-042, M-MOB-046 |
| logout cleanup | **PARTIAL** — Logout intenta `POST /api/v1/auth/logout` backend, y **siempre** limpia estado local (borra tokens, cancela trabajos). Sin red → logout local inmediato. | M-MOB-042, M-MOB-046 |

**Resultado:** **PARTIAL** — Implementación diseñada y documentada (M-MOB-042, M-MOB-046), pero inoperativa sin endpoint `/auth/refresh` confirmado del backend. Flujo: login → token en memoria → al cerrar/app en background → datos a DataStore cifrado → al reiniciar → restore desde DataStore + refresh attempt → si falla → logout local.

---

## 21. ML

| Criterio | Evidencia |
|---|---|
| Smart Match ML local | **IMPLEMENTADO PERO CON ERRORES DE COMPILACIÓN** |
| modelo | `LogisticRegressionSmartMatchModel.kt` — modelo de regresión logística entrenado offline, empaquetado como código Kotlin. Usa `FeatureVector` (no resuelto — error de compatibilidad Kotlin 2.2.10). |
| tipo | Regresión logística (modelo estadístico, no Deep Learning/Neural Net). |
| feature extraction | No hay código de extracción en tiempo real. Features se calculan offline y el modelo usa pesos pre-entrenados empaquetados en el APK. |
| inference | Ejecutado localmente como cálculo de `matchScore`/`probability`. El código `RunSmartMatchUseCase.kt` falla por parámetros no encontrados (probability, score, factors, matchScore no definidos en la firma actual). |
| runtime use | Está diseñado para usarse en `SmartMatchScreen.kt` y `SmartMatchViewModel.kt`, pero ambos tienen errores de compilación por incompatibilidades API Compose/Kotlin. |
| fallback | No hay fallback documentado. El diseño sugiere fallback por criterios simples si ML no está disponible. |
| tests | `SmartMatchMlTest.kt` existe pero no se ejecuta por errores de compilación en código dependiente. 40/40 tests unitarios de register/login son verdes (M-MOB-047). |
| ¿Requisito satisfecho? | **NO** — El módulo ML está añadido pero roto. Requiere refactoring para compatibilidad con Kotlin 2.2.10/AGP 9.5.0. No es funcional hasta que se corrijan los errores de tipo. |

---

## 22. GENAI

| Criterio | Estado |
|---|---|
| Mobile | **NO** — No hay GenAI (LLM, modelo generativo) en el cliente móvil. SmartMatch es regresión logística clásica, no IA generativa. |
| Backend | **NO** — No hay servicio GenAI en backend .NET Azure. |
| GenAI service | **NO** — No configurado, no contratado, no integrado. |

**Confirmación:** El proyecto descarta explícitamente IA generativa en M-MOB-033: "Mantener Machine Learning para calcular resultados y Generative AI para explicarlos" → estado PROPUESTO, no implementado. Sin `google-generative-ai`, `openai`, `anthropic` ni SDKs equivalentes. COST-001 $0 cumplido por no usar servicios externos.

---

## 23. LOGOUT

| Criterio | Valor |
|---|---|
| Consume endpoint backend real? | **SÍ, intentado** — El caso de uso `LogoutUseCase.kt` llama a `POST /api/v1/auth/logout`. |
| Method/path/status esperado | `POST /api/v1/auth/logout` → 200 OK (cuando backend disponible) |
| ¿Logout limpia estado local? | **SÍ** — M-MOB-042 y M-MOB-046: "intento backend siempre, limpieza siempre del estado local, borrado de tokens y cancelación de trabajos asociados". |
| ¿Sin red → logout local? | **SÍ** — "sin red → logout local inmediato" (M-MOB-042). |

**Conclusión:** Logout está implementado con flujo completo (endpoint + limpieza local), pero el endpoint es reactivo y depende de la disponibilidad del backend. Sin backend, el logout es local-only.

---

## 24. PUSH

| Criterio | Estado |
|---|---|
| Firebase configured | **NO** — `app/google-services.json` no existe. `BuildConfig.FCM_CONFIGURED=false`. |
| FCM token | **Generado en memoria solo** — `FirebasePushTokenProvider.kt` pero nunca registrado exitosamente sin google-services.json. |
| registration endpoint | **NO EXISTE** — No hay registro con backend por falta de token FCM válido. |
| backend sender | **NO CONFIGURADO** — Sin token FCM, no hay quién sea enviado. |
| receiving | **NO PROBADO** — Sin configuración FCM, no hay recibimiento de mensajes. |
| tap navigation | **NO PROBADO** — Sin notificaciones reales. |
| flows donde se usa | M-MOB-052: "data-only keys notificationId/category/title/body/targetKind/targetId". Canales: `thehouse_requests/thehouse_visits/thehouse_system`. Permiso `POST_NOTIFICATIONS` contextual Android 13+. |
| Preparación vs E2E real | **Preparado conceptualmente** — FCM data-only implementado en `feature/real-notifications`. E2E real bloqueado por falta de `google-services.json`, sender backend y emulador ADB con `system_server`/package service. |

**Separación preparación de E2E real:** La preparación (configuración de servicio, proves de token, canales) está documentada y parcialmente implementada en código. La E2E real (dispositivo físico, token registrado, mensaje recibido, navegación al tap) **NO ESTÁ DADA**.

---

## 25. AUTOMATION ON PHYSICAL DEVICE

| Criterio | Valor |
|---|---|
| ANDROID_INSTRUMENTED_TESTS_TOTAL | **2** (ExampleInstrumentedTest.kt, LoginScreenInstrumentedTest.kt) |
| PHYSICAL_DEVICE_AUTOMATION_READY | **NO** |

**Qué falta para que sea YES:**
1. Dispositivo físico conectado con depuración USB habilitada.
2. `adb devices` detecte el dispositivo.
3. Los instrumented tests compile and run sin errores (actualmente fallan por errores de compilación general).
4. Los tests de login y example instrumented estén verdes en dispositivo real.

**Errores específicos que impedirían la ejecución incluso con dispositivo:**
- Errores de compilación en `compileDebugKotlin` bloquean todo build.
- Una vez corregidos los errores de compile, los tests instrumented deberían ejecutarse en dispositivo.

---

## 26. SALIDA

**Tabla principal — Pregunta del profesor | Respuesta actual | Evidencia | Estado | Acción pendiente**

| # | Pregunta | Respuesta | Evidencia | Estado | Acción pendiente |
|---|---|---|---|---|---|
| 1 | Estado real del proyecto | BLOCKED — sin spec funcional activa | AGENTS.md §4, memoria.md | BLOCKED | Esperar autorización spec NV-04 o similar |
| 2 | Seguridad DevSecops | 55/100 | Tabla sección 2 | PARTIAL | Corregir errores compile, endurecer storage |
| 3 | OWASP Mobile Top 10 | 3 PASS, 4 PARTIAL, 2 NA | Tabla sección 3 | PARTIAL | Implementar almacenamiento seguro, auth flow |
| 4 | Vulnerabilidades | 0 CRITICAL, 0 HIGH, 0 MEDIUM, 2 LOW | Tabla sección 4 | LOW | Arreglar errores compile (no son vulns) |
| 5 | FinTech/Banking | NO comparable | Texto sección 5 | NO | Aplicar hardening bancario |
| 6 | DevSecOps CI | Pipeline definido, tools: gitleaks, trivy | Tabla sección 6 | PARTIAL | Arreglar compile para ejecutar pipeline |
| 7 | DevSecOps CD | Signing blocked, secrets en env | Tabla sección 7 | BLOCKED | Firme release con vars seguras |
| 8 | Endpoints consumidos | 18 únicos | Tabla sección 8 | DONE | Documentar endpoints restantes |
| 9 | IA/Agents/MCP | AI=UNKNOWN, Skills=manual DI, MCP=disabled/not executed | Tabla sección 9 | UNKNOWN | Documentar uso de ML actual |
| 10 | Memoria | Sistema documentación estática, no aprendizaje auto | Tabla sección 10 | PARTIAL | Continuarregistrando decisiones |
| 11 | Unit Tests | ~115 archivados, 0 ejecutables (compile fail) | Tabla sección 11 | BLOCKED | Corregir errores SmartMatch ML |
| 12 | Security Tests | 0 (inexistentes) | Tabla sección 12 | NO IMPLEMENTED | Agregar tests de seguridad explícitos |
| 13 | Integration Tests | ~15 archivados, 0 contra backend real | Tabla sección 13 | PENDIENTE | Ejecutar contra backend Azure |
| 14 | Matriz screen/endpoint/tests | 38 flujos, 0 completos | Tabla sección 14 | POOR | Cubrir flujos faltantes |
| 15 | Otras pruebas | lint: 0 errors históricamente; build: fail; release: no sign; etc. | Tabla sección 15 | MIXTO | Regularizar pipeline |
| 16 | Moderación imágenes | Ninguna implementada | Tabla sección 16 | NOT APPLICABLE | Implementar si requiere NV-22 |
| 17 | Moderación texto | Validaciones longitud/formato; sin moderación automática/manu | Tabla sección 17 | PARTIAL | Documentar backend responsabilidad |
| 18 | Delete Account | BLOCKED — sin endpoint backend | Tabla sección 18 | BLOCKED | Coordinar backend endpoint |
| 19 | Permisos | INTERNET + POST_NOTIFICATIONS (normal) | Tabla sección 19 | DONE | Ningún permiso runtime solicitado |
| 20 | Session persistence | PARTIAL — diseño documentado, requiere /refresh backend | Tabla sección 20 | PARTIAL | Esperar contrato refresh backend |
| 21 | ML | Implementado pero roto (errores compile) | Tabla sección 21 | PARTIAL | Refactor ML para compatibilidad toolchain |
| 22 | GenAI | NO implementado en ningún lado | Tabla sección 22 | NO | Cumple COST-001 $0 |
| 23 | Logout | Flujo completo (endpoint + limpieza local) | Tabla sección 23 | READY (local) | Validar endpoint backend |
| 24 | Push | Preparado data-only, E2E bloqueado (sin google-services.json) | Tabla sección 24 | PARTIAL | Añadir google-services.json o deshabilitar |
| 25 | Automation physical device | 2 tests instrumentados, sin dispositivo conectado | Tabla sección 25 | NO | Conectar dispositivo y ejecutar |

**SIGNATURES / SCORES:**

- **SECURITY_SCORE:** 55/100
- **OWASP_SCORE:** 3 PASS, 4 PARTIAL, 2 NOT APPLICABLE (0 FAIL)
- **CRITICAL_VULNERABILITIES:** 0
- **HIGH_VULNERABILITIES:** 0
- **ENDPOINTS_TOTAL:** 18
- **UNIT_TESTS:** ~115 archivados, 0 ejecutables
- **SECURITY_TESTS:** 0
- **INTEGRATION_TESTS:** ~15 archivados, 0 contra backend real
- **INSTRUMENTED_TESTS:** 2 (Example + LoginScreen), 0 en dispositivo físico
- **AI_AGENTS:** UNKNOWN / NOT DOCUMENTED
- **SKILLS:** MANUAL DI + COMPOSE + RETROFIT + OKHTTP + COIL + WORKMANAGER
- **MCP:** Firebase disabled; GitHub referenced not executed
- **MEMORY_SYSTEM:** Estática documentación (memoria.md), no aprendizaje auto
- **ML:** Implementado pero roto — refactor necesario para compatibilidad toolchain
- **GENAI:** NO implementado (cumple COST-001 $0)
- **DELETE_ACCOUNT:** BLOCKED — sin endpoint backend
- **FCM:** Preparado data-only, E2E bloqueado (sin google-services.json)
- **PHYSICAL_DEVICE_AUTOMATION:** NO — sin dispositivo conectado, errors compile

**TOP 10 GAPS BEFORE EVALUATION:**

1. **Corregir errores de compilación en SmartMatch ML** — Código NV-23 incompatible con Kotlin 2.2.10/AGP 9.5.0 bloquea build entero.
2. **Autorización spec funcional activa** — Estado actual BLOCKED; necesidad spec NV-04/Auxiliar aprobada.
3. **Endpoint `/auth/refresh` backend** — Necesario para sesión persistente, renovación tokens y logout punta a punta.
4. **`google-services.json` para FCM** — Requerido para E2E push notifications; ausente proyecto.
5. **Endpoint delete account en backend** — Bloquea flujo NV-15 delete account completo.
6. **Liberación release signing** — Falta variables THEHOUSE_* para firmar AAB; necesario para Play.
7. **Tests de seguridad explícitos** — 0 tests de seguridad corriendo; añadir auth/token/permiso tests.
8. **Instrumented tests en dispositivo físico** — Ningún test ejecutado en dispositivo real; necesidad ADB + credenciales.
9. **Cobertura de integración backend** — ~15 tests integration existían pero ningún backend corriendo en sesión de agente.
10. **Hardening seguridad fintech** — 55/100 no es nivel bancario; necesita encriptación tokens, certificate pinning, session secure.

**FINAL_READINESS_PERCENT:** **35/100** — El proyecto tiene base técnica sólida, arquitectura validada y tests unitarios diseñados, pero tres bloqueos principales impiden cualquier evaluación: (1) errores de compilación sin resolver, (2) falta autorización spec funcional, (3) dependencias backend incompletas.

**RESULTADO:** **PROFESSOR-RUBRIC-AUDIT-READY**

---
*Este informe fue generado automatizadamente contra la rúbrica del profesor. Todos los hallazgos se basan en evidencia real del repositorio C:\THEHOUSE\Mobile. Se abstiene de afirmar resultados no verificables (tests no ejecutados, builds fallando, specs sin autorizar).*