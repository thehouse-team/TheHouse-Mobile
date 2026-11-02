# NV-01 — PLAN DE FOUNDATION

> ## ✅ EJECUTADA / VALIDADA
>
> Plan autorizado, ejecutado y validado el 2026-10-31. Las 6 fases y 30 tasks fueron completadas con evidencia real.

| Campo | Valor |
|---|---|
| Spec | NV-01 Foundation |
| Plan ID | NV01-PLAN-1.0 |
| Versión | 1.0.0 |
| Estado | ✅ EJECUTADA / VALIDADA |
| Dependencias | Bootstrap técnico y SDD |
| Módulo | `:app` |

## 1. Estrategia

Implementar la foundation en fases pequeñas y verificables, sin funcionalidades de negocio. Cada fase debe dejar evidencia y no puede iniciar hasta que la anterior esté validada.

El plan no crea módulos Gradle. Las boundaries son paquetes y contratos dentro de `:app`.

## 2. Fases

### Fase F1 — Gobernanza y boundaries

**Objetivo:** fijar mapa de paquetes, dirección de dependencias y convenciones de configuración.

| Task | Entregable |
|---|---|
| NV01-T001 | Inventario y mapa de paquetes actuales. |
| NV01-T002 | Convención de paquetes Presentation/Domain/Data y reglas de dependencia. |
| NV01-T003 | Checklist de imports y boundaries verificable. |
| NV01-T004 | Convenciones de configuración y build sin secretos. |
| NV01-T005 | Convenciones de naming y organización de archivos. |
| NV01-T006 | Revisión de que no se requieren módulos Gradle. |

**Regla de dependencia:** `Presentation → Domain` y `Data → Domain`. Domain no depende de Presentation ni de Data.

**Requisitos cubiertos:** NV01-REQ-001, NV01-REQ-002, NV01-REQ-017.

**Salida:** documento de boundaries y evidencia de revisión.

### Fase F2 — Presentation, MVVM y UDF

**Objetivo:** definir contratos de ViewModel, UI State, eventos, Flow y navegación sin pantallas funcionales.

| Task | Entregable |
|---|---|
| NV01-T007 | Convención de ViewModel y lifecycle. |
| NV01-T008 | Contrato de dependencias del ViewModel. |
| NV01-T009 | Convención de UI Event y UDF. |
| NV01-T010 | Modelo de UI State: loading, content, empty, error. |
| NV01-T011 | Convención de Flow y `collectAsStateWithLifecycle`. |
| NV01-T012 | Base de Navigation Compose sin destinos de negocio. |

**Requisitos cubiertos:** NV01-REQ-003, NV01-REQ-004, NV01-REQ-005, NV01-REQ-007, NV01-REQ-008.

**Salida:** convenciones y, si la task lo autoriza, componentes arquitectónicos sin funcionalidad.

### Fase F3 — Data boundaries

**Objetivo:** fijar contratos de repositorio y límites de persistencia sin crear modelos o schemas reales.

| Task | Entregable |
|---|---|
| NV01-T013 | Contrato de repositorio y límite de Room. |
| NV01-T014 | Límite de DataStore para preferencias no sensibles. |
| NV01-T015 | Límite de WorkManager para trabajo diferido. |
| NV01-T016 | Convención de mapeo Data ↔ Domain. |

**Regla de mapeo:** `Data ↔ Domain` en NV01-T016 describe únicamente transformación/mapeo de modelos (`DTO → Domain`, `Domain → DTO`, `Entity → Domain`, `Domain → Entity`). No implica dependencia arquitectónica bidireccional: la dependencia permanece `Data → Domain` y `Domain → Data` está prohibido.

**Requisitos cubiertos:** NV01-REQ-012, NV01-REQ-013, NV01-REQ-014.

**Salida:** contratos y reglas; cero entidades Room y cero workers funcionales.

### Fase F4 — Infraestructura base

**Objetivo:** definir networking, serialización, image loading y logging seguro sin endpoints ni pantallas.

| Task | Entregable |
|---|---|
| NV01-T017 | Fábrica/configuración base Retrofit + OkHttp sin endpoints. |
| NV01-T018 | Convención Kotlin Serialization y DTO. |
| NV01-T019 | Boundary de Coil `3.4.0`. |
| NV01-T020 | Política de timeouts, errores HTTP y cancelación. |
| NV01-T021 | Política de logging seguro sin tokens ni PII. |

**Requisitos cubiertos:** NV01-REQ-010, NV01-REQ-011, NV01-REQ-015.

**Salida:** foundation de infraestructura sin APIs funcionales.

### Fase F5 — Cross-cutting y DI

**Objetivo:** definir concurrencia, inyección de dependencias y configuración de entornos.

| Task | Entregable |
|---|---|
| NV01-T022 | Convención de dispatchers y cancelación. |
| NV01-T023 | Decisión documentada de DI. |
| NV01-T024 | Proveedor de configuración no secreta. |
| NV01-T025 | Convención de build types y entornos. |

**Requisitos cubiertos:** NV01-REQ-006, NV01-REQ-016, NV01-REQ-017.

**Salida:** decisión de DI y reglas de configuración aprobadas.

### Fase F6 — Errores, testing y validación

**Objetivo:** cerrar el modelo de errores, las costuras de prueba y validar la foundation completa.

| Task | Entregable |
|---|---|
| NV01-T026 | Modelo de error de dominio y mapeo desde Data. |
| NV01-T027 | Costuras de testing, fakes y puntos de inyección. |
| NV01-T028 | Ejecución de build, tests y lint. |
| NV01-T029 | Actualización de trazabilidad y evidencia. |
| NV01-T030 | Auditoría final de alcance y cero funcionalidades. |

**Requisitos cubiertos:** NV01-REQ-009, NV01-REQ-018, validación global.

**Salida:** evidencia completa y dictamen.

## 3. Matriz completa requisito → fase → task

| Requisito | Fase | Tasks |
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

La matriz cubre las 30 tasks. La misma matriz se refleja en `spec.md` y en la matriz de evidencia de `evidence.md`.

## 4. Dependencias entre fases

```text
F1 → F2 → F3 → F4 → F5 → F6
```

F3 y F4 pueden solaparse solo si F1 y F2 están validadas. F5 requiere F2 y F4. F6 requiere todas las anteriores.

## 5. Validaciones por fase

### F1

- Revisión de imports y boundaries.
- Confirmación de que no se crearon módulos Gradle.

### F2

- Revisión de contratos UDF.
- Confirmación de que no se crearon pantallas funcionales.

### F3

- Confirmación de cero entidades Room, cero DAO y cero base de datos.
- Confirmación de que KSP sigue sin configurar.

### F4

- Confirmación de cero endpoints funcionales.
- Revisión de logging seguro.
- Confirmación de que no hay secretos.

### F5

- Decisión de DI registrada.
- Confirmación de que no se instaló Hilt/Koin sin aprobación.

### F6

```powershell
.\gradlew.bat clean assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

- Auditoría de alcance.
- Actualización de `memoria.md` si hay decisiones nuevas.

## 6. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Sobrearquitectura | Limitar foundation a contratos y convenciones; no crear capas vacías innecesarias. |
| DI prematura | Documentar decisión y no instalar framework sin justificación. |
| Room/KSP prematuro | Prohibir entidades y procesador hasta persistencia real. |
| Mezcla con negocio | Cada task debe declarar requisito; auditar alcance al final. |
| Toolchain roto | No cambiar versiones; validar build. |
| Secretos | Revisión obligatoria de archivos y logs. |

## 7. Criterio de salida

La spec NV-01 solo puede considerarse completada cuando:

- Todas las tasks autorizadas estén cerradas con evidencia.
- La matriz de trazabilidad esté completa.
- El build, tests y lint pasen.
- No exista funcionalidad de negocio.
- COST-001 siga en $0.
- El `sdd-guardian` emita `PASS` o `PASS WITH WARNINGS`.

## 8. Estado del plan

```text
✅ EJECUTADA
✅ VALIDADA
```

Las 6 fases y las 30 tasks fueron ejecutadas y validadas. La evidencia real está en `evidence.md`.
