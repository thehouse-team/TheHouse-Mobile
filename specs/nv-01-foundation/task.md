# NV-01 — TASKS DE FOUNDATION

> ## ✅ EJECUTADA / VALIDADA
>
> Las 30 tasks fueron ejecutadas y validadas con evidencia real. La autorización humana fue otorgada y el resultado final está registrado en `evidence.md`.

| Campo | Valor |
|---|---|
| Spec | NV-01 Foundation |
| Plan | NV01-PLAN-1.0 |
| Estado | ✅ EJECUTADA / VALIDADA |
| Tasks completadas | 30 |
| Autorización | APROBADA |
| Fecha de ejecución | 2026-10-31 |

## Reglas de ejecución

- No marcar ninguna task sin evidencia.
- No ejecutar tasks de otras specs.
- No crear funcionalidades de negocio.
- No crear pantallas, APIs, entidades Room ni integraciones externas.
- No cambiar el toolchain.
- Si falta autorización: `BLOCKED`.

## Fase F1 — Gobernanza y boundaries

- [x] NV01-T001 — Inventariar paquetes existentes y documentar el mapa actual de `:app`.
- [x] NV01-T002 — Definir la convención de paquetes Presentation/Domain/Data y las reglas de dependencia.
- [x] NV01-T003 — Crear un checklist verificable de imports y límites de capas.
- [x] NV01-T004 — Definir convenciones de configuración/build sin secretos ni cambios de toolchain.
- [x] NV01-T005 — Definir convenciones de naming y organización de archivos.
- [x] NV01-T006 — Verificar que la foundation no requiere módulos Gradle adicionales.

## Fase F2 — Presentation, MVVM y UDF

- [x] NV01-T007 — Documentar la convención de ViewModel, lifecycle y responsabilidades.
- [x] NV01-T008 — Definir el contrato de dependencias permitidas en un ViewModel.
- [x] NV01-T009 — Definir el contrato UDF de UI Event, estado y efectos one-shot.
- [x] NV01-T010 — Definir el modelo de UI State con loading, content, empty y error.
- [x] NV01-T011 — Definir la convención de Flow y `collectAsStateWithLifecycle`.
- [x] NV01-T012 — Definir la base de Navigation Compose sin destinos funcionales.

## Fase F3 — Data boundaries

- [x] NV01-T013 — Definir contratos de repositorio y el límite de Room sin crear entidades ni DAO.
- [x] NV01-T014 — Definir el límite de DataStore para preferencias no sensibles.
- [x] NV01-T015 — Definir el límite de WorkManager sin workers funcionales.
- [x] NV01-T016 — Definir la convención de mapeo de modelos Data ↔ Domain, sin dependencia arquitectónica Domain → Data.

> **Nota de mapeo:** `Data ↔ Domain` en NV01-T016 describe únicamente transformación/mapeo (`DTO → Domain`, `Domain → DTO`, `Entity → Domain`, `Domain → Entity`). No implica dependencia bidireccional; la regla permanece `Data → Domain` y `Domain → Data` está prohibido.

## Fase F4 — Infraestructura base

- [x] NV01-T017 — Definir la configuración base de Retrofit/OkHttp sin endpoints funcionales.
- [x] NV01-T018 — Definir la convención de Kotlin Serialization y DTO.
- [x] NV01-T019 — Definir el boundary de carga de imágenes con Coil `3.4.0`.
- [x] NV01-T020 — Definir timeouts, semántica de errores HTTP y cancelación.
- [x] NV01-T021 — Definir logging seguro sin tokens, credenciales ni PII.

## Fase F5 — Cross-cutting y DI

- [x] NV01-T022 — Definir dispatchers, `viewModelScope` y reglas de cancelación.
- [x] NV01-T023 — Evaluar y documentar la decisión de Dependency Injection.
- [x] NV01-T024 — Definir un proveedor de configuración no secreta.
- [x] NV01-T025 — Definir convenciones de build types y entornos.

## Fase F6 — Errores, testing y validación

- [x] NV01-T026 — Definir el modelo de error de dominio y su mapeo desde Data.
- [x] NV01-T027 — Definir costuras de testing, fakes y puntos de inyección para NV-02.
- [x] NV01-T028 — Ejecutar `clean assembleDebug`, `testDebugUnitTest` y `lintDebug`; registrar resultados.
- [x] NV01-T029 — Actualizar trazabilidad, evidencia y decisiones en `memoria.md` si corresponde.
- [x] NV01-T030 — Auditar alcance final y confirmar cero funcionalidades de negocio.

## Evidencia por task

Cada task completada deberá registrar:

```text
Task:
Requisito:
Archivos:
Comandos:
Resultado:
Riesgos:
```

## Criterio de cierre

- Todas las tasks autorizadas ejecutadas y verificadas.
- Build, tests y lint exitosos.
- Matriz requisito → fase → task completa.
- Cero funcionalidades, pantallas, APIs, entidades o integraciones no autorizadas.
- `sdd-guardian` emite PASS o PASS WITH WARNINGS.

## Estado final de este documento

```text
✅ EJECUTADA
✅ VALIDADA
30/30 tasks completadas
```
