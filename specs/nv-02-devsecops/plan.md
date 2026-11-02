# NV-02 — PLAN DEVSECOPS

> ## ⛔ NO EJECUTAR
>
> Plan no autorizado. No instalar herramientas, no crear workflows y no modificar código. Requiere aprobación humana de NV-02.

| Campo | Valor |
|---|---|
| Spec | NV-02 DevSecOps |
| Plan ID | NV02-PLAN-1.0 |
| Versión | 1.0.0 |
| Estado | ⛔ NO EJECUTAR |
| Dependencia | NV-01 Foundation |
| Estado de dependencia | ✅ EJECUTADA / VALIDADA / CERRADA |
| Tasks | 28 (0 completadas) |

## 1. Estrategia

Construir calidad y seguridad de forma incremental:

1. Pruebas base.
2. Pruebas por capa.
3. Análisis estático.
4. Análisis de seguridad y dependencias.
5. CI y quality gates.
6. Evidencia, deferred y COST-001.

Principios de ejecución:

- No crear código de producción artificial únicamente para escribir una prueba.
- No crear Repository ficticio, UseCase ficticio, ViewModel de negocio ficticio, pantalla ficticia, DTO de negocio ficticio ni entidad ficticia.
- Probar solo código real existente; si no existe sujeto, aplicar `DEFERRED JUSTIFICADO`.
- Una herramienta no se integra solo porque aparezca en la spec.
- NV-02 implementa CI, quality y security automation.
- Release productiva, signing, AAB, Play Console y deployment móvil pertenecen a NV-03.
- GitHub Actions local puede prepararse; la ejecución remota requiere repositorio Git/remote real.
- No ejecutar `git init`, `git add`, `git commit` ni `git push` sin autorización humana independiente.

No se instala ninguna herramienta hasta que la spec y el plan estén aprobados y la tarea correspondiente autorizada.

## 2. Fases

### Fase P1 — Estrategia de pruebas

| Task | Entregable |
|---|---|
| NV02-T001 | Inventario de tests y cobertura actual. |
| NV02-T002 | Matriz requisito → prueba. |
| NV02-T003 | Convención de datos, fakes y aislamiento. |
| NV02-T004 | Definición de herramientas de test compatibles y gratuitas. |

**Requisitos:** NV02-REQ-001.

Reglas de realidad:

- T001-T004 son documentales; no crear tests nuevos ni instalar herramientas.
- La matriz debe cubrir specs autorizadas y foundation real de NV-01.
- Los fakes solo se permiten para contratos reales existentes.
- Toda limitación por falta de sujeto real se registra como `DEFERRED JUSTIFICADO`.

### Fase P2 — Pruebas por capa

| Task | Entregable |
|---|---|
| NV02-T005 | Plan y ejecución de ViewModel tests. |
| NV02-T006 | Plan y ejecución de Repository tests. |
| NV02-T007 | Plan y ejecución de Compose UI tests. |
| NV02-T008 | Plan y ejecución de instrumented tests. |
| NV02-T009 | Plan y ejecución de integration tests. |
| NV02-T010 | Plan y ejecución de security tests. |

**Requisitos:** NV02-REQ-002 a NV02-REQ-007.

Reglas de realidad por task:

- **T005:** usar `BaseViewModel` y foundation real de NV-01. No crear ViewModels funcionales ficticios. Si no existe sujeto, `DEFERRED JUSTIFICADO`.
- **T006:** no crear Repository ficticio. Convención: `<Repository>Test`, Given/When/Then, un comportamiento por prueba, fakes de contratos reales, casos de éxito/error/mapeo/cancelación, sin red/DB real. Ejecución diferible mediante `DEFERRED JUSTIFICADO` hasta que una spec funcional cree un repositorio real.
- **T007:** no crear pantallas funcionales. Convención: composables stateless, semantics, state rendering (loading/error/empty/success), events, accessibility y test tags solo cuando sean necesarios. Si no existe un componente Compose real significativo, `DEFERRED JUSTIFICADO`.
- **T008:** usar solo infraestructura Android real existente. Si no hay emulador/dispositivo, `DEFERRED POR ENTORNO` con evidencia. No marcar como exitoso un test que no se ejecutó.
- **T009:** integrar solo componentes reales. No usar red productiva, backend productivo, MongoDB, Firebase, Maps ni servicios externos. Si no hay integración significativa, `DEFERRED JUSTIFICADO`.
- **T010:** verificar controles reales de foundation: logging seguro, ausencia de secretos, configuración, requisito HTTPS, manejo de errores, preferencias no sensibles y separación debug/release. No inventar autenticación ni tokens.

### Fase P3 — Análisis estático

| Task | Entregable |
|---|---|
| NV02-T011 | Gate Android Lint. |
| NV02-T012 | Evaluación e integración Detekt. |
| NV02-T013 | Evaluación e integración ktlint. |
| NV02-T014 | Evaluación e integración Semgrep. |
| NV02-T015 | Evaluación COST-001 de SonarCloud. |

**Requisitos:** NV02-REQ-008 a NV02-REQ-012.

Reglas de herramientas:

- **T011:** Android Lint es gate core; el gate exige 0 errores; warnings existentes se documentan.
- **T012/T013:** Detekt y ktlint son core candidates; justificar valor, solapamiento, mantenimiento y costo antes de instalar.
- **T014:** Semgrep es evaluación; solo se convierte en gate si aporta valor real para Android/Kotlin, con reglas útiles, ejecución local gratuita, uso en CI, revisión de falsos positivos y solapamiento. No usar reglas indiscriminadas.
- **T015:** SonarCloud es evaluación, no dependencia obligatoria. Si COST-001 no se demuestra, `NO INTEGRAR`.
- Ninguna herramienta se instala durante la corrección documental.

### Fase P4 — Seguridad de dependencias

| Task | Entregable |
|---|---|
| NV02-T016 | Dependency scanning. |
| NV02-T017 | Secret scanning. |
| NV02-T018 | Evaluación COST-001 de MobSF. |

**Requisitos:** NV02-REQ-013 a NV02-REQ-015.

Reglas de seguridad:

- **T016:** definir herramienta gratuita, severidad y excepciones; no bloquear automáticamente por cualquier hallazgo.
- **T017:** un secreto confirmado bloquea; el valor nunca se imprime en logs ni evidencias.
- **T018:** MobSF es evaluación, no gate obligatorio. Si no aporta valor suficiente, `DEFERRED`.
- Clasificar hallazgos: Critical, High, Medium, Low, False Positive, Accepted Risk.

### Fase P5 — CI y gates DevSecOps

| Task | Entregable |
|---|---|
| NV02-T019 | Workflow GitHub Actions. |
| NV02-T020 | Cache, artefactos y permisos mínimos. |
| NV02-T021 | Quality gates. |
| NV02-T022 | Umbral de cobertura y política de excepciones. |

**Requisitos:** NV02-REQ-016, NV02-REQ-017.

Reglas de CI:

- **T019:** **Localmente:** preparación de `.github/workflows/`; validar YAML, comandos, permisos, secrets references, cache y gates. **Remotamente:** requiere repositorio Git/remote real y workflow ejecutado; si no existe, `Validación remota: DEFERRED POR INFRAESTRUCTURA`.
- **T020:** **Localmente:** cache, artefactos y permisos mínimos; validación local. **Remotamente:** no afirmar ejecución sin workflow real; si no existe repositorio, `Validación remota: DEFERRED POR INFRAESTRUCTURA`.
- **T021:** gates mínimos: build, unit tests, Android Lint sin errores, secret scan sin secretos confirmados y sin vulnerabilidades críticas confirmadas.
- **T022:** primero seleccionar mecanismo de coverage, medir baseline, identificar código testeable, proponer umbral, documentar exclusiones y prohibir disminuciones artificiales. No adoptar 80%, 90% o 100% sin evidencia.
- NV-02 no publica, no firma AAB de producción, no sube a Play Console ni ejecuta deployment móvil.

### Fase P6 — Evidencia y cierre / Validación global NV-02

| Task | Entregable |
|---|---|
| NV02-T023 | Evidencia y reportes. |
| NV02-T024 | COST-001 y manejo de secretos. |
| NV02-T025 | Auditoría de cero funcionalidad. |
| NV02-T026 | Validación integral. |
| NV02-T027 | Actualización de trazabilidad. |
| NV02-T028 | Revisión final con `sdd-guardian`. |

**Requisitos:** NV02-REQ-018; T026/T028 apoyan NV02-REQ-017.

Reglas de cierre:

- T025–T028 constituyen la **VALIDACIÓN GLOBAL NV-02**.
- T023–T028 se trazan a `NV02-REQ-018`; T026 y T028 también se asocian a `NV02-REQ-017`.
- La validación integral registra resultados reales; no se marcan tests no ejecutados.
- La trazabilidad final debe demostrar 18 requisitos, 6 fases, 28 tasks y 0 tasks huérfanas.
- `sdd-guardian` debe emitir PASS o PASS WITH WARNINGS.

## 3. Matriz completa

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

La matriz cubre las 28 tasks. T025–T028 son la VALIDACIÓN GLOBAL NV-02 y se trazan a `NV02-REQ-018`; T026/T028 también apoyan `NV02-REQ-017`. Tasks huérfanas = 0.

## 4. Dependencias

```text
P1 → P2 → P3 → P4 → P5 → P6
```

P3 y P4 pueden solaparse si P1 y P2 están estables. P5 requiere que las herramientas elegidas estén validadas. P6 requiere todas las fases.

Un `DEFERRED JUSTIFICADO` no bloquea la fase si la estrategia está documentada, no existe sujeto real y no se creó código artificial. Un `DEFERRED POR INFRAESTRUCTURA` de GitHub Actions no bloquea la preparación local del workflow.

## 5. Herramientas y COST-001

| Herramienta | Rol | Costo objetivo | Estado |
|---|---|---|---|
| Android Lint | Core gate | $0 | Incluido con Android |
| Tests | Core gate | $0 | Existentes en foundation |
| Detekt | Core candidate | $0 | No instalado |
| ktlint | Core candidate | $0 | No instalado |
| Semgrep | Evaluación | Free tier | No instalado; gate solo si aporta valor |
| SonarCloud | Evaluación | Free tier sujeto a límites | No evaluado; no obligatorio |
| Dependency scanning | Core | $0 | No seleccionado |
| Secret scanning | Core | $0 | No configurado |
| MobSF | Evaluación | $0/self-hosted | No evaluado; no obligatorio |
| GitHub Actions | Core CI | Free tier | No configurado; preparación local |

Cada herramienta debe justificar objetivo, valor agregado, solapamiento, mantenimiento, compatibilidad, costo, ejecución local/CI y política de fallo.

```text
Una herramienta no se integra únicamente porque aparezca en la spec.
```

Toda herramienta con free tier debe documentar free tier, límites, si requiere tarjeta, riesgo de cobro y alternativa gratuita antes de usarse. Si COST-001 no se demuestra, no se integra.

## 6. Quality gates y políticas

### 6.1 Gates mínimos

```text
Build debug exitoso
testDebugUnitTest exitoso
lintDebug sin errores
Secret scan sin secretos confirmados
Sin vulnerabilidades críticas confirmadas
Cobertura mínima acordada después de baseline
```

Ningún gate puede desactivarse para ocultar un fallo. Los gates adicionales se activan solo después de validar la herramienta correspondiente.

### 6.2 Política de vulnerabilidades

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

### 6.3 Política de secretos

```text
Secreto confirmado:
BLOCK

Valor del secreto:
Nunca se imprime en logs ni evidencias
```

### 6.4 Cobertura

Proceso obligatorio:

1. Seleccionar mecanismo de coverage compatible.
2. Medir baseline real.
3. Identificar código realmente testeable.
4. Proponer umbral razonable.
5. Documentar exclusiones justificadas.
6. Prohibir disminuciones artificiales del gate.

No adoptar automáticamente 80%, 90% o 100% sin evidencia. Priorizar lógica de dominio, mappers, ViewModels, errores, configuración y repositories cuando existan, sobre código generado o UI trivial.

### 6.5 CI vs release

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

### 6.6 GitHub Actions local vs remoto

Localmente, tras autorización de NV-02, se podrá crear `.github/workflows/` y validar:

```text
Estructura YAML
Comandos
Permisos
Secrets references
Cache
Gates
```

Remotamente, no se podrá afirmar `GitHub Actions ejecutado correctamente` hasta que existan:

```text
Repositorio Git
Remote GitHub
Workflow ejecutado realmente
```

Si no existen: `Validación remota: DEFERRED POR INFRAESTRUCTURA`.

No ejecutar `git init`, `git add`, `git commit` ni `git push` sin autorización humana independiente.

## 7. Validaciones

```powershell
.\gradlew.bat clean assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

Cuando aplique:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Si no hay emulador/dispositivo, se registra `DEFERRED POR ENTORNO` con evidencia. No se marca un test no ejecutado como exitoso.

## 8. Riesgos

| Riesgo | Mitigación |
|---|---|
| Herramientas con costo | Evaluación COST-001 previa. |
| Tool sprawl | Justificar valor, solapamiento y mantenimiento. |
| Exceso de gates | Empezar por los mínimos y medir. |
| Tests frágiles o artificiales | Código real, fakes solo de contratos reales, datos deterministas. |
| Uso indebido de deferred | Motivo, spec futura y confirmación de código artificial = NO. |
| Secretos en CI | GitHub Secrets, permisos mínimos y redacción de evidencias. |
| Ruido de análisis | Baseline y revisión de reglas. |
| Scope creep de CI a release | Release/Play reservado a NV-03. |
| GitHub Actions remoto no disponible | `DEFERRED POR INFRAESTRUCTURA` con evidencia. |
| Cobertura arbitraria | Baseline real y umbral justificado. |

## 9. Criterio de salida

- Requisitos cubiertos por pruebas y gates, o deferred justificado.
- Herramientas aprobadas y gratuitas.
- Evidencia disponible, incluida la de deferred.
- Sin funcionalidad de negocio añadida.
- Sin código de producción artificial.
- 18 requisitos, 6 fases, 28 tasks, 0 tasks huérfanas.
- Sin release, signing, AAB, Play Console ni deployment móvil.
- `sdd-guardian` emite PASS o PASS WITH WARNINGS.

## 10. Estado

```text
⛔ NO EJECUTAR
```
