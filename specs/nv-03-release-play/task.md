# NV-03 — TASKS RELEASE / GOOGLE PLAY

> ## ⚙ EJECUTADA EN FASE B U2 (PARCIAL)
>
> Tasks P1–P6 + manifest/red ejecutadas con autorización humana (Victor Manuel Jose Jose, 2026-11-04). AAB generado, firmado y verificado por acción humana (2026-11-05, ver evidence.md). La subida a Play requiere autorización humana adicional. No marcar tasks nuevas sin autorización explícita.
>
> | Campo | Valor |
> |---|---|
> | Spec | NV-03 Release / Google Play |
> | Plan | NV03-PLAN-1.1 |
> | Estado | ✅ EJECUTADA U2/PARCIAL — AAB firmado y verificado (`jar verified.`); BLOCKED-PRIVACY-URL |

| Campo | Valor |
|---|---|
| Spec | NV-03 Release / Google Play |
| Plan | NV03-PLAN-1.1 |
| Estado | ✅ EJECUTADA U2/PARCIAL |
| Tasks completadas | P1/P2/P3/P4 base + manifest/red (NV03-T001–T026 completadas con evidencia; NV03-T027 parcial: listing con placeholders) |
| Autorización | FASE B U2 — Victor Manuel Jose Jose (2026-11-04) |

## Reglas

- No marcar tasks sin evidencia.
- No publicar ni cargar AAB.
- No versionar secretos.
- No usar signing debug para release.
- No cambiar el toolchain.
- Si falta autorización: `BLOCKED`.

## Fase P1 — Versionado y build

- [x] NV03-T001 — Auditar el `versionCode` y `versionName` actuales.
- [x] NV03-T002 — Definir la política de versionado y su trazabilidad.
- [x] NV03-T003 — Revisar build types y configuración release.
- [x] NV03-T004 — Definir minificación y reglas de preservación.

## Fase P2 — Signing y secretos

- [x] NV03-T005 — Separar signing debug y release.
- [x] NV03-T006 — Diseñar el almacenamiento seguro de secretos de signing.
- [x] NV03-T007 — Documentar el procedimiento de gestión y backup del keystore.
- [x] NV03-T008 — Crear la matriz debug vs release.

## Fase P3 — Release y AAB

- [x] NV03-T009 — Ejecutar un dry-run de build release cuando esté autorizado.
- [x] NV03-T010 — Generar el AAB solo con autorización.
- [x] NV03-T011 — Verificar integridad, firma y contenido del AAB.
- [x] NV03-T012 — Completar el checklist de Google Play Console.

## Fase P4 — Privacidad y permisos

- [x] NV03-T013 — Definir privacy notice y su enlace.
- [x] NV03-T014 — Documentar permissions disclosure.
- [x] NV03-T015 — Completar Data handling y Data Safety.

## Fase P5 — CI/CD y costo

- [x] NV03-T016 — Evaluar COST-001 de todos los servicios externos.
- [x] NV03-T017 — Definir el pipeline de release.
- [x] NV03-T018 — Configurar secretos y permisos mínimos del pipeline.

## Fase P6 — Evidencia y autorización

- [x] NV03-T019 — Registrar evidencia completa del release.
- [x] NV03-T020 — Ejecutar quality gates pre-release.
- [x] NV03-T021 — Implementar gate humano de autorización de publicación.
- [x] NV03-T022 — Definir plan de rollback y estrategia de versiones.
- [x] NV03-T023 — Auditar alcance y confirmar que no se agregó funcionalidad.
- [x] NV03-T024 — Solicitar revisión final al `sdd-guardian`.
- [x] NV03-T025 — Verificar identidad: `<APPLICATION_ID>` definitivo (PENDIENTE HUMANO; permanente tras el primer AAB) y versión según `VERSIONING.md`. (NV03-REQ-017)
- [x] NV03-T026 — Manifest/red: `INTERNET` justificado como único permiso, cleartext deshabilitado, `network_security_config` restringido al dominio del backend y reglas de backup (`fullBackupContent`/`data_extraction_rules`) excluyendo cualquier almacenamiento de sesión/token. (NV03-REQ-018)
- [x] NV03-T027 — Store listing: URL aviso privacidad `<URL_AVISO_PRIVACIDAD>`, Data Safety, clasificación, público 18+, capturas. Para U2: pista interna. Verificar tipo/fecha de la cuenta Play antes de afirmar si la prueba cerrada (12 testers / 14 días consecutivos) aplica; no afirmarlo sin evidencia. Eliminación de cuenta: `Posterior — obligatorio antes de producción`. (NV03-REQ-008, NV03-REQ-009b, NV03-REQ-019)

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
