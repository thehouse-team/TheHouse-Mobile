# NV-02 — DEVSECOPS

> ## ⛔ NO EJECUTAR
>
> Esta spec está en estado **NO AUTORIZADO**. No instalar herramientas, no crear workflows, no modificar código y no marcar tasks sin autorización humana explícita.

| Campo | Valor |
|---|---|
| Spec ID | NV-02 |
| Nombre | DevSecOps |
| Versión | 1.0.0 |
| Fecha | 2026-10-31 |
| Estado | ⛔ NO EJECUTAR |
| Autorización | PENDIENTE |
| Dependencia | NV-01 Foundation |
| Estado de dependencia | ✅ EJECUTADA / VALIDADA / CERRADA |

## 1. Contexto

TheHouse Mobile ya cuenta con NV-01 Foundation ejecutada, validada y cerrada:

```text
NV-01 Foundation:
✅ EJECUTADA
✅ VALIDADA
✅ CERRADA
30/30 tasks
```

La foundation existente incluye boundaries `Presentation → Domain ← Data`, Manual DI con `AppContainer` + `TheHouseApplication`, modelo de errores, configuración no secreta, red base, DataStore para preferencias no sensibles, Coil y tests unitarios de foundation.

No existe todavía una spec funcional autorizada ni entidades, repositories, pantallas o ViewModels de negocio reales. Por eso, NV-02 no debe crear código de producción artificial para poder escribir pruebas.

Esta spec define el sistema de calidad y DevSecOps futuro. No instala herramientas, no crea workflows ejecutables, no implementa tests nuevos y no modifica código.

## 2. Objetivo

Establecer pruebas, análisis estático, análisis de seguridad, dependency scanning, integración continua, quality gates y evidencia para que las specs funcionales futuras se integren con controles verificables y costo $0.

`CI/CD` puede usarse como término metodológico, pero NV-02 implementa únicamente CI, quality y security automation. La entrega y publicación Android pertenece a NV-03 Release / Play.

## 3. Alcance

- Unit tests.
- ViewModel tests sobre foundation real existente.
- Repository tests con estrategia documentada y ejecución diferible.
- Compose UI tests cuando exista un sujeto real significativo.
- Instrumented tests con infraestructura Android real.
- Integration tests entre componentes reales existentes.
- Security tests sobre controles reales de foundation.
- Android Lint.
- Detekt.
- ktlint.
- Semgrep, solo si aporta valor real para Android/Kotlin.
- SonarCloud cuando sea viable y COST-001 se demuestre.
- Dependency scanning.
- Secret scanning.
- MobSF cuando sea viable y aporte valor suficiente.
- GitHub Actions: preparación local y validación de workflow.
- Quality gates.
- Evidencia y estados deferred.
- COST-001 y manejo de secretos.

### Límite CI vs release

```text
NV-02:
CI
Quality
Security
Automated validation
Gates DevSecOps

NV-03:
Release productiva
Signing
AAB
Play Console
Deployment móvil
```

Ninguna task de NV-02 puede publicar, firmar un AAB de producción, subir a Play Console ni ejecutar deployment móvil.

### GitHub Actions: local vs remoto

La preparación local de `.github/workflows/` y la validación de estructura YAML, comandos, permisos, referencias a secrets, cache y gates puede formar parte de NV-02 cuando sea autorizada.

La afirmación `GitHub Actions ejecutado correctamente` solo puede emitirse cuando existan repositorio Git, remote GitHub y una ejecución real del workflow. Si no existen: `Validación remota: DEFERRED POR INFRAESTRUCTURA`.

No se ejecuta `git init`, `git add`, `git commit` ni `git push` sin autorización humana independiente.

## 4. Fuera de alcance

- Funcionalidades de negocio.
- Pantallas funcionales.
- APIs o entidades de dominio.
- Firebase, Maps, FCM, ML o IA.
- Publicación en Google Play.
- Configuración de signing.
- Release productiva, AAB de producción o deployment móvil.
- Herramientas que requieran tarjeta o costo.
- Cambios de toolchain.
- Crear código de producción artificial únicamente para escribir pruebas.
- Crear Repository ficticio, UseCase ficticio, ViewModel de negocio ficticio, pantalla ficticia, DTO de negocio ficticio o entidad ficticia para completar NV-02.
- Ejecutar operaciones Git sin autorización humana independiente.

## 5. Principios transversales

### 5.1 Principio de testing real

```text
NO crear código de producción artificial únicamente para poder escribir una prueba.
```

Tampoco se debe crear:

- Repository ficticio.
- UseCase ficticio.
- ViewModel de negocio ficticio.
- Pantalla ficticia.
- DTO de negocio ficticio.
- Entidad ficticia.

solo para completar NV-02.

Las pruebas deben ejercitar únicamente código real existente. Los fakes de test solo se permiten para contratos reales ya existentes; no se inventan contratos para justificar un fake.

NV-02 puede probar el código real creado por NV-01 cuando sea pertinente:

```text
BaseViewModel
DomainError
DataErrorMapper
DomainErrorToUiError
DispatcherProvider
AppConfig
JsonProvider
NetworkTimeouts
Mapper
SafeHttpLoggingInterceptor
configuration boundaries
```

No se inventan features para tener algo que probar. Si no existe un sujeto real/autorizado, se aplica `DEFERRED JUSTIFICADO`.

### 5.2 Estados de ejecución permitidos

| Estado | Significado | Checkbox |
|---|---|---|
| `COMPLETADA` | Ejecutada con evidencia real. | `[x]` |
| `DEFERRED JUSTIFICADO` | No existe todavía un sujeto real/autorizado que probar. No es fallo ni completada. | `[ ]` |
| `DEFERRED POR ENTORNO` | Requiere emulador/dispositivo no disponible. Debe existir evidencia. | `[ ]` |
| `DEFERRED POR INFRAESTRUCTURA` | Requiere repositorio Git/remote/ejecución remota no disponible. Debe existir evidencia. | `[ ]` |
| `BLOCKED` | Falta autorización, herramienta, secreto seguro o se detecta riesgo no aceptado. | `[ ]` |
| `FAILED` | Se ejecutó y falló. No se oculta. | `[ ]` |

Plantilla obligatoria para deferred:

```text
Task:
Estado:
DEFERRED JUSTIFICADO

Motivo:
No existe todavía un sujeto real/autorizado que probar.

Spec futura que lo habilitará:
[spec correspondiente]

Código artificial creado:
NO
```

Para `DEFERRED POR ENTORNO` o `DEFERRED POR INFRAESTRUCTURA` debe registrarse además la evidencia concreta de la limitación.

### 5.3 Principio de herramientas

```text
Una herramienta no se integra únicamente porque aparezca en la spec.
```

Cada herramienta debe justificar:

- Objetivo.
- Valor agregado.
- Solapamiento con otros controles.
- Mantenimiento.
- Compatibilidad.
- Costo.
- Ejecución local/CI.
- Política de fallo.

Controles principales considerados:

```text
Android Lint
Tests
Detekt
ktlint
Dependency scanning
Secret scanning
GitHub Actions
```

Semgrep debe evaluarse por valor real para seguridad Android/Kotlin antes de convertirlo en gate obligatorio: reglas útiles, ejecución local gratuita, CI, falsos positivos y solapamiento. SonarCloud y MobSF permanecen como evaluación y no se convierten automáticamente en dependencias obligatorias.

### 5.4 Principio de cobertura

No se elige un porcentaje arbitrario antes de obtener un baseline. El proceso es:

1. Seleccionar un mecanismo de coverage compatible.
2. Medir el baseline real.
3. Identificar código realmente testeable.
4. Proponer un umbral razonable.
5. Documentar exclusiones justificadas.
6. Prohibir disminuciones artificiales del gate.

Se prioriza lógica de dominio, mappers, ViewModels, errores, configuración y repositories cuando existan, por encima de código generado o UI trivial.

### 5.5 Política de hallazgos

```text
Critical confirmado:
BLOCK

High confirmado:
BLOCK salvo excepción humana documentada

Medium / Low:
Evaluación y remediación planificada

False Positive / Accepted Risk:
Documentado, nunca ignorado silenciosamente
```

Un secreto confirmado bloquea. El valor del secreto nunca se imprime en logs ni evidencias.

### 5.6 Estrategia de Repository testing

No existe un repository funcional concreto. No se crea un Repository ficticio. Cuando una spec funcional cree un repository real, la convención será:

- Nombre: `<Repository>Test`.
- Estructura Given/When/Then o nombres descriptivos; un comportamiento por prueba.
- Casos mínimos: éxito, error de fuente, mapeo dominio/data y cancelación.
- Fakes solo de contratos reales existentes; datos deterministas.
- Sin red productiva, base de datos real ni servicios externos.
- Dispatchers controlados.
- La ejecución permanece `DEFERRED JUSTIFICADO` hasta que exista el repository real.

### 5.7 Estrategia de Compose UI testing

No se crean pantallas funcionales para probar. Cuando exista un componente Compose real significativo:

- Probar composables stateless con state hoisting.
- Verificar semantics, state rendering (loading/error/empty/success), events y accessibility.
- Usar test tags solo cuando aporten estabilidad o no exista un selector semántico adecuado.
- No probar lógica de negocio en la UI ni detalles de implementación.
- Si no existe sujeto significativo, `DEFERRED JUSTIFICADO`.

## 6. Requisitos

### NV02-REQ-001 — Infraestructura de pruebas base

Definir JUnit, Coroutines test, fakes y convenciones de datos de prueba. Los fakes solo se crean para contratos reales existentes; no se crean contratos ficticios.

**Aceptación:** existe una estrategia documentada y ejecutable para pruebas unitarias deterministas, sin crear código de producción artificial.

### NV02-REQ-002 — ViewModel tests

Definir pruebas de estado, eventos, errores y cancelación de ViewModels reales. Puede usarse `BaseViewModel` y la foundation de NV-01. No crear ViewModels funcionales ficticios.

**Aceptación:** se cubren éxito, error, loading y cancelación con dispatchers controlados; si no existe un sujeto real, se registra `DEFERRED JUSTIFICADO`.

### NV02-REQ-003 — Repository tests

Definir la estrategia de pruebas de repositorios con fuentes falsas y mapeo de datos. Actualmente no existen repositories funcionales concretos; no crear un Repository ficticio.

**Aceptación:** estrategia y convención documentadas; la ejecución es diferible mediante `DEFERRED JUSTIFICADO` hasta que una spec funcional cree un repositorio real. Cuando exista el sujeto, se cubren éxito, error, mapeo y cancelación sin red/base real.

### NV02-REQ-004 — Compose UI tests

Definir pruebas de UI, semántica y accesibilidad con Compose. No crear pantallas funcionales para ejecutar Compose tests.

**Aceptación:** si existe un componente Compose genérico real susceptible de prueba, se prueba; si no existe un sujeto significativo, se registra `DEFERRED JUSTIFICADO`. Debe documentarse la estrategia futura de semantics, state rendering, events, accessibility y test tags solo cuando sean necesarios, sin lógica de negocio en la UI.

### NV02-REQ-005 — Instrumented tests

Definir pruebas instrumentadas estables y aisladas usando únicamente infraestructura Android real existente. No crear negocio para justificar una prueba instrumentada.

**Aceptación:** existen criterios de dispositivo, datos y limpieza. Si se requiere emulador/dispositivo no disponible, se registra `DEFERRED POR ENTORNO` con evidencia. No se marca como exitoso un test que no se ejecutó.

### NV02-REQ-006 — Integration tests

Definir pruebas de integración entre componentes reales existentes.

**Aceptación:** se validan contratos y boundaries reales sin red productiva, backend productivo, MongoDB, Firebase, Maps ni servicios externos. Si no existe una integración significativa todavía, se registra `DEFERRED JUSTIFICADO`.

### NV02-REQ-007 — Security tests

Definir pruebas de permisos, secretos, logging y almacenamiento seguro sobre controles reales de foundation.

**Aceptación:** se verifican logging seguro, ausencia de secretos, configuración, requisito HTTPS, manejo de errores, preferencias no sensibles y separación debug/release. No se inventan autenticación ni tokens para probar seguridad.

### NV02-REQ-008 — Android Lint gate

Integrar Android Lint como gate obligatorio.

**Aceptación:** `lintDebug` se ejecuta; el gate exige 0 errores; los warnings existentes se documentan y no se ocultan.

### NV02-REQ-009 — Detekt

Evaluar e integrar Detekt con costo $0 como control core candidate.

**Aceptación:** objetivo, valor, solapamiento, mantenimiento, compatibilidad, costo, configuración, reglas, umbrales y política de fallo documentados; no instalado antes de autorización.

### NV02-REQ-010 — ktlint

Evaluar e integrar ktlint con costo $0 como control core candidate.

**Aceptación:** formato verificable, política de fallo y justificación documentados; no instalado antes de autorización.

### NV02-REQ-011 — Semgrep

Evaluar Semgrep y, solo si aporta valor real para Android/Kotlin, integrarlo.

**Aceptación:** reglas Android/Kotlin útiles, ejecución local gratuita, uso en CI, falsos positivos, solapamiento y política de gate documentados; no se usan reglas indiscriminadas; sin secretos.

### NV02-REQ-012 — SonarCloud

Evaluar viabilidad de SonarCloud bajo COST-001.

**Aceptación:** free tier, límites, privacidad, tipo de repositorio soportado, cuenta requerida, riesgo de costo y solapamiento documentados. No es dependencia obligatoria; si COST-001 no se demuestra, `NO INTEGRAR`.

### NV02-REQ-013 — Dependency scanning

Definir escaneo de dependencias y alertas de vulnerabilidades.

**Aceptación:** herramienta gratuita, política de severidad y excepciones documentadas; no se bloquea automáticamente por cualquier hallazgo.

### NV02-REQ-014 — Secret scanning

Definir detección de secretos en repositorio y CI.

**Aceptación:** no se exponen credenciales; un secreto confirmado bloquea el pipeline; el valor del secreto nunca se imprime en logs ni evidencias.

### NV02-REQ-015 — MobSF

Evaluar viabilidad de MobSF bajo COST-001.

**Aceptación:** modalidad self-hosted, recursos necesarios, análisis APK, automatización posible, mantenimiento, costo real y limitaciones documentados. No es gate obligatorio; si no aporta valor suficiente en esta etapa, se registra `DEFERRED`.

### NV02-REQ-016 — GitHub Actions

Definir CI reproducible con permisos mínimos y cache.

**Aceptación:** workflow propuesto, secretos por GitHub Secrets y artefactos controlados. La preparación local de `.github/workflows/` y la validación de estructura YAML, comandos, permisos, referencias a secrets, cache y gates forma parte de NV-02. La ejecución remota requiere repositorio Git, remote GitHub y workflow ejecutado realmente; si no existe, `Validación remota: DEFERRED POR INFRAESTRUCTURA`. No se ejecuta `git init`, `git add`, `git commit` ni `git push` sin autorización humana independiente. NV-02 no publica ni entrega release.

### NV02-REQ-017 — Quality gates

Definir gates de build, tests, cobertura, lint y seguridad.

**Aceptación:** gates mínimos documentados; ningún gate se relaja para ocultar fallos; la cobertura se define después de medir un baseline real, sin porcentajes arbitrarios; vulnerabilidades y secretos se clasifican según la política de NV-02.

### NV02-REQ-018 — Evidencia y COST-001

Definir reportes, retención, responsables, evidencia de deferred y evaluación de costo $0.

**Aceptación:** cada gate deja evidencia; cada deferred registra motivo, spec futura que lo habilitará y confirmación de que no se creó código artificial; todo servicio externo documenta free tier, límites, requisito de tarjeta, riesgo de cobro, alternativa gratuita y viabilidad $0.

## 7. Trazabilidad

| Requisito | Fase | Tasks |
|---|---|---|
| NV02-REQ-001 | P1 | NV02-T001, NV02-T002, NV02-T003, NV02-T004 |
| NV02-REQ-002 | P2 | NV02-T005 |
| NV02-REQ-003 | P2 | NV02-T006 |
| NV02-REQ-004 | P2 | NV02-T007 |
| NV02-REQ-005 | P2 | NV02-T008 |
| NV02-REQ-006 | P2 | NV02-T009 |
| NV02-REQ-007 | P2 | NV02-T010 |
| NV02-REQ-008 | P3 | NV02-T011 |
| NV02-REQ-009 | P3 | NV02-T012 |
| NV02-REQ-010 | P3 | NV02-T013 |
| NV02-REQ-011 | P3 | NV02-T014 |
| NV02-REQ-012 | P3 | NV02-T015 |
| NV02-REQ-013 | P4 | NV02-T016 |
| NV02-REQ-014 | P4 | NV02-T017 |
| NV02-REQ-015 | P4 | NV02-T018 |
| NV02-REQ-016 | P5 | NV02-T019, NV02-T020 |
| NV02-REQ-017 | P5 | NV02-T021, NV02-T022 |
| NV02-REQ-018 | P6 | NV02-T023, NV02-T024, NV02-T025, NV02-T026, NV02-T027, NV02-T028 |

T025–T028 constituyen la **VALIDACIÓN GLOBAL NV-02** y se trazan a `NV02-REQ-018`; T026 y T028 también se asocian a `NV02-REQ-017` por su naturaleza de quality gate y cierre. La matriz cubre las 28 tasks; tasks huérfanas = 0.

## 8. Criterios de aceptación globales

- `testDebugUnitTest` y `lintDebug` exitosos.
- Pruebas deterministas y trazables.
- Sin herramientas instaladas antes de autorización.
- Sin secretos en logs, código o reportes.
- Sin funcionalidades nuevas.
- Sin código de producción artificial para pruebas.
- Sin componentes ficticios de Repository, UseCase, ViewModel de negocio, pantalla, DTO de negocio o entidad.
- Sin release productiva, signing, AAB, Play Console ni deployment móvil.
- Deferred registrados con motivo, spec futura y confirmación de código artificial = NO.
- Cobertura definida después de baseline real, sin porcentajes arbitrarios.
- Vulnerabilidades clasificadas; secretos confirmados bloquean.
- Herramientas justificadas; SonarCloud y MobSF en evaluación; Semgrep solo si aporta valor.
- Matriz con 18 requisitos, 6 fases, 28 tasks y 0 tasks huérfanas.
- COST-001 en $0.

## 9. Riesgos

- Complejidad excesiva de herramientas y tool sprawl.
- SonarCloud/MobSF con límites o costo.
- Tests frágiles o artificiales.
- Pipelines que filtran secretos.
- Gates que bloquean sin criterio de remediación.
- Uso indebido de `DEFERRED JUSTIFICADO` para evitar pruebas posibles.
- Scope creep de CI hacia release/publicación.
- Ejecución remota de GitHub Actions sin repositorio/remote real.
- Umbrales de cobertura arbitrarios sin baseline.

## 10. Estado

```text
⛔ NO EJECUTAR
```
