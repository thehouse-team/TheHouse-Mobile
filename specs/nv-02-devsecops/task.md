# NV-02 — TASKS DEVSECOPS

> ## ⛔ NO EJECUTAR
>
> Tasks no autorizadas. Permanecen sin marcar. No instalar herramientas ni crear workflows hasta recibir autorización humana explícita para NV-02.

| Campo | Valor |
|---|---|
| Spec | NV-02 DevSecOps |
| Plan | NV02-PLAN-1.0 |
| Estado | INCREMENTAL D1 — unit tests + lint + secret/dependency scan + mínimo privilegio + build reproducible |
| Dependencia | NV-01 Foundation — ✅ EJECUTADA / VALIDADA / CERRADA |
| Tasks totales | 28 |
| Tasks completadas | 6 |
| Autorización | D1: controles gratuitos iniciales NV-02 (unit tests, lint, secret/dependency scan, mínimo privilegio). No marcar NV-02 completa. |

## Reglas

- No marcar tasks sin evidencia.
- No instalar herramientas todavía.
- No crear workflows ejecutables todavía; la preparación local de `.github/workflows/` solo se permite tras autorización de NV-02.
- No probar funcionalidades no autorizadas.
- No crear código de producción artificial únicamente para escribir una prueba.
- No crear Repository ficticio, UseCase ficticio, ViewModel de negocio ficticio, pantalla ficticia, DTO de negocio ficticio ni entidad ficticia.
- Las pruebas deben ejercitar únicamente código real existente.
- Los fakes de test solo se permiten para contratos reales existentes.
- No ocultar fallos.
- No ejecutar `git init`, `git add`, `git commit` ni `git push` sin autorización humana independiente.
- No ejecutar GitHub Actions remotamente sin repositorio Git y remote real.
- No publicar, firmar AAB de producción, subir a Play Console ni ejecutar deployment móvil; eso pertenece a NV-03.
- No imprimir secretos en logs ni evidencias.
- Si falta autorización: `BLOCKED`.

## Estados permitidos

| Estado | Significado | Marca |
|---|---|---|
| `COMPLETADA` | Ejecutada con evidencia real. | Solo tras evidencia |
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

## Principios de testing real

- NV-02 puede probar foundation real de NV-01: `BaseViewModel`, `DomainError`, `DataErrorMapper`, `DomainErrorToUiError`, `DispatcherProvider`, `AppConfig`, `JsonProvider`, `NetworkTimeouts`, `Mapper`, `SafeHttpLoggingInterceptor` y boundaries de configuración.
- No inventar features para tener algo que probar.
- Si no existe un sujeto real/autorizado, aplicar `DEFERRED JUSTIFICADO` sin crear código artificial.
- No crear pantallas ficticias, repositories ficticios, UseCases ficticios, ViewModels de negocio ficticios, DTOs ficticios ni entidades ficticias.

## Fase P1 — Estrategia de pruebas

- [ ] NV02-T001 — Inventariar los tests existentes y la cobertura actual.
- [ ] NV02-T002 — Crear la matriz requisito → prueba para las specs autorizadas.
- [ ] NV02-T003 — Definir convenciones de datos de prueba, fakes y aislamiento.
- [ ] NV02-T004 — Definir herramientas de test compatibles y de costo $0.

> T001-T004 son documentales. No crear tests nuevos ni instalar herramientas. La matriz debe incluir foundation real de NV-01.

## Fase P2 — Pruebas por capa

- [ ] NV02-T005 — Implementar y ejecutar ViewModel tests.
- [ ] NV02-T006 — Implementar y ejecutar Repository tests.
- [ ] NV02-T007 — Implementar y ejecutar Compose UI tests.
- [ ] NV02-T008 — Implementar y ejecutar instrumented tests.
- [ ] NV02-T009 — Implementar y ejecutar integration tests.
- [ ] NV02-T010 — Implementar y ejecutar security tests.

> Reglas de realidad por task:
>
> - **T005:** usar `BaseViewModel` y foundation real de NV-01. No crear ViewModels funcionales ficticios. Si no existe sujeto, `DEFERRED JUSTIFICADO`.
> - **T006:** no crear Repository ficticio. Convención: `<Repository>Test`, Given/When/Then, un comportamiento por prueba, fakes de contratos reales, casos de éxito/error/mapeo/cancelación, sin red/DB real. Ejecución diferible mediante `DEFERRED JUSTIFICADO` hasta que una spec funcional cree un repositorio real.
> - **T007:** no crear pantallas funcionales. Convención: composables stateless, semantics, state rendering (loading/error/empty/success), events, accessibility y test tags solo cuando sean necesarios. Si no existe un componente Compose real significativo, `DEFERRED JUSTIFICADO`.
> - **T008:** usar solo infraestructura Android real existente. Si no hay emulador/dispositivo, `DEFERRED POR ENTORNO` con evidencia. No marcar como exitoso un test que no se ejecutó.
> - **T009:** integrar solo componentes reales. No usar red productiva, backend productivo, MongoDB, Firebase, Maps ni servicios externos. Si no hay integración significativa, `DEFERRED JUSTIFICADO`.
> - **T010:** verificar controles reales de foundation: logging seguro, ausencia de secretos, configuración, requisito HTTPS, manejo de errores, preferencias no sensibles y separación debug/release. No inventar autenticación ni tokens.

## Fase P3 — Análisis estático

- [x] NV02-T011 - Configurar Android Lint como quality gate. [D1 — lintDebug local PASS + gate en mobile-ci.yml]
- [ ] NV02-T012 — Evaluar e integrar Detekt.
- [ ] NV02-T013 — Evaluar e integrar ktlint.
- [ ] NV02-T014 — Evaluar e integrar Semgrep.
- [ ] NV02-T015 — Evaluar SonarCloud contra COST-001 y documentar límites.

> - **T011:** Android Lint es gate core; el gate exige 0 errores; warnings existentes se documentan.
> - **T012/T013:** Detekt y ktlint son core candidates; justificar valor, solapamiento, mantenimiento y costo antes de instalar.
> - **T014:** Semgrep es evaluación; solo se convierte en gate si aporta valor real para Android/Kotlin, con reglas útiles, ejecución local gratuita, uso en CI, revisión de falsos positivos y solapamiento. No usar reglas indiscriminadas.
> - **T015:** SonarCloud es evaluación, no dependencia obligatoria. Si COST-001 no se demuestra, `NO INTEGRAR`.

## Fase P4 — Seguridad de dependencias

- [x] NV02-T016 - Configurar dependency scanning. [D1 — trivy fs HIGH/CRITICAL (exit 0), run 42173546971 success]
- [x] NV02-T017 - Configurar secret scanning. [D1 — gitleaks CLI free, 0 leaks, run 42173546971 success]
- [ ] NV02-T018 — Evaluar MobSF contra COST-001 y documentar alcance.

> - **T016:** definir herramienta gratuita, severidad y excepciones; no bloquear automáticamente por cualquier hallazgo.
> - **T017:** un secreto confirmado bloquea; el valor nunca se imprime en logs ni evidencias.
> - **T018:** MobSF es evaluación, no gate obligatorio. Si no aporta valor suficiente, `DEFERRED`.
> - Clasificar hallazgos: Critical, High, Medium, Low, False Positive, Accepted Risk.

## Fase P5 — CI y gates DevSecOps

- [x] NV02-T019 - Crear el workflow de GitHub Actions. [D1 — .github/workflows/mobile-ci.yml]
- [x] NV02-T020 - Configurar cache, artefactos y permisos mínimos del workflow. [D1 — setup-gradle cache, upload-artifact, contents:read]
- [x] NV02-T021 - Definir y aplicar quality gates. [D1 — testDebugUnitTest + lintDebug + assembleDebug bloqueantes]
- [ ] NV02-T022 — Definir cobertura mínima y política de excepciones.

> - **T019:** **Localmente:** preparación de `.github/workflows/`; validar YAML, comandos, permisos, secrets references, cache y gates. **Remotamente:** requiere repositorio Git/remote real y workflow ejecutado; si no existe, `Validación remota: DEFERRED POR INFRAESTRUCTURA`.
> - **T020:** **Localmente:** cache, artefactos y permisos mínimos; validación local. **Remotamente:** no afirmar ejecución sin workflow real; si no existe repositorio, `Validación remota: DEFERRED POR INFRAESTRUCTURA`.
> - **T021:** gates mínimos: build, unit tests, Android Lint sin errores, secret scan sin secretos confirmados y sin vulnerabilidades críticas confirmadas.
> - **T022:** primero seleccionar mecanismo de coverage, medir baseline, identificar código testeable, proponer umbral, documentar exclusiones y prohibir disminuciones artificiales. No adoptar 80%, 90% o 100% sin evidencia.
> - NV-02 no publica, no firma AAB de producción, no sube a Play Console ni ejecuta deployment móvil.
> - No ejecutar `git init`, `git add`, `git commit` ni `git push` sin autorización humana independiente.

## Fase P6 — Evidencia y cierre / Validación global NV-02

- [ ] NV02-T023 — Definir reportes y evidencia de cada gate.
- [ ] NV02-T024 — Documentar COST-001 y manejo de secretos.
- [ ] NV02-T025 — Auditar que no se agregó funcionalidad de negocio.
- [ ] NV02-T026 — Ejecutar validación integral y registrar resultados reales.
- [ ] NV02-T027 — Actualizar trazabilidad y `memoria.md` si corresponde.
- [ ] NV02-T028 — Solicitar revisión final al `sdd-guardian`.

> T025–T028 constituyen la **VALIDACIÓN GLOBAL NV-02** y se trazan a `NV02-REQ-018`; T026 y T028 también se asocian a `NV02-REQ-017`. No son tasks huérfanas.

## Evidencia por task

```text
Task:
Requisito:
Herramienta:
Archivos:
Comandos:
Resultado:
Costo:
Riesgos:
```

## Evidencia de deferred

```text
Task:
Estado:
DEFERRED JUSTIFICADO | DEFERRED POR ENTORNO | DEFERRED POR INFRAESTRUCTURA

Motivo:
No existe todavía un sujeto real/autorizado que probar.

Spec futura que lo habilitará:
[spec correspondiente]

Código artificial creado:
NO

Evidencia de la limitación:
[entorno/infraestructura, cuando aplique]
```

## Estado final

```text
⛔ NO EJECUTAR
Tasks totales: 28
Tasks completadas: 0
Tasks huérfanas: 0
```
