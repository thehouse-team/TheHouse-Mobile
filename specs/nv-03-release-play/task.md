# NV-03 — TASKS RELEASE / GOOGLE PLAY

> ## ⛔ NO EJECUTAR
>
> Tasks no autorizadas. Permanecen sin marcar. No generar AAB, no configurar signing, no publicar ni modificar código sin autorización humana explícita.

| Campo | Valor |
|---|---|
| Spec | NV-03 Release / Google Play |
| Plan | NV03-PLAN-1.0 |
| Estado | ⛔ NO EJECUTAR |
| Tasks completadas | 0 |
| Autorización | PENDIENTE |

## Reglas

- No marcar tasks sin evidencia.
- No publicar ni cargar AAB.
- No versionar secretos.
- No usar signing debug para release.
- No cambiar el toolchain.
- Si falta autorización: `BLOCKED`.

## Fase P1 — Versionado y build

- [ ] NV03-T001 — Auditar el `versionCode` y `versionName` actuales.
- [ ] NV03-T002 — Definir la política de versionado y su trazabilidad.
- [ ] NV03-T003 — Revisar build types y configuración release.
- [ ] NV03-T004 — Definir minificación y reglas de preservación.

## Fase P2 — Signing y secretos

- [ ] NV03-T005 — Separar signing debug y release.
- [ ] NV03-T006 — Diseñar el almacenamiento seguro de secretos de signing.
- [ ] NV03-T007 — Documentar el procedimiento de gestión y backup del keystore.
- [ ] NV03-T008 — Crear la matriz debug vs release.

## Fase P3 — Release y AAB

- [ ] NV03-T009 — Ejecutar un dry-run de build release cuando esté autorizado.
- [ ] NV03-T010 — Generar el AAB solo con autorización.
- [ ] NV03-T011 — Verificar integridad, firma y contenido del AAB.
- [ ] NV03-T012 — Completar el checklist de Google Play Console.

## Fase P4 — Privacidad y permisos

- [ ] NV03-T013 — Definir privacy notice y su enlace.
- [ ] NV03-T014 — Documentar permissions disclosure.
- [ ] NV03-T015 — Completar Data handling y Data Safety.

## Fase P5 — CI/CD y costo

- [ ] NV03-T016 — Evaluar COST-001 de todos los servicios externos.
- [ ] NV03-T017 — Definir el pipeline de release.
- [ ] NV03-T018 — Configurar secretos y permisos mínimos del pipeline.

## Fase P6 — Evidencia y autorización

- [ ] NV03-T019 — Registrar evidencia completa del release.
- [ ] NV03-T020 — Ejecutar quality gates pre-release.
- [ ] NV03-T021 — Implementar gate humano de autorización de publicación.
- [ ] NV03-T022 — Definir plan de rollback y estrategia de versiones.
- [ ] NV03-T023 — Auditar alcance y confirmar que no se agregó funcionalidad.
- [ ] NV03-T024 — Solicitar revisión final al `sdd-guardian`.

## Evidencia por task

```text
Task:
Requisito:
Versión:
Artefacto:
Signing:
Privacidad:
Permisos:
COST-001:
Publicación ejecutada: NO
```

## Estado final

```text
⛔ NO EJECUTAR
```
