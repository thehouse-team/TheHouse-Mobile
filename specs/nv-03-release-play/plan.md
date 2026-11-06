# NV-03 — PLAN RELEASE / GOOGLE PLAY

> ## ⛔ NO EJECUTAR
>
> Plan no autorizado. No generar AAB, no configurar signing, no publicar y no modificar código sin aprobación humana.

| Campo | Valor |
|---|---|
| Spec | NV-03 Release / Google Play |
| Plan ID | NV03-PLAN-1.1 |
| Versión | 1.1.0 |
| Fecha | 2026-11-04 |
| Alcance | U2 |
| Estado | ⛔ NO EJECUTAR |
| Dependencia | NV-01 ✅ ejecutada; se ejecuta antes de NV-04 en el orden U2; NV-02 documenta ahora y ejecuta después |

## 1. Estrategia

Preparar el release en seis fases verificables. La publicación es siempre un acto humano explícito y no forma parte de la automatización.

Detalle U2: la cuenta de Play Console ya existe y está pagada. La entrega U2 es **AAB release firmado disponible** con R8 activo, logging reducido, `API_BASE_URL` configurable (`https://ca-thehouse-u2.grayisland-cb722b5f.eastus2.azurecontainerapps.io`) y manifest/red endurecido. La subida a pista interna requiere autorización humana.

## 2. Fases

### Fase P1 — Versionado y build

| Task | Entregable |
|---|---|
| NV03-T001 | Auditoría del `versionCode` actual. |
| NV03-T002 | Política de `versionCode`/`versionName`. |
| NV03-T003 | Revisión de build types y configuración release. |
| NV03-T004 | Política de minificación y reglas de preservación. |

**Requisitos:** NV03-REQ-001, NV03-REQ-002, NV03-REQ-005, NV03-REQ-006.

### Fase P2 — Signing y secretos

| Task | Entregable |
|---|---|
| NV03-T005 | Separación de signing debug/release. |
| NV03-T006 | Diseño de almacenamiento seguro de secretos. |
| NV03-T007 | Procedimiento de gestión de keystore. |
| NV03-T008 | Matriz debug vs release. |

**Requisitos:** NV03-REQ-003, NV03-REQ-004, NV03-REQ-005.

### Fase P3 — Release y AAB

| Task | Entregable |
|---|---|
| NV03-T009 | Dry-run de build release cuando esté autorizado. |
| NV03-T010 | Generación de AAB autorizada. |
| NV03-T011 | Verificación del AAB. |
| NV03-T012 | Checklist de Play Console. |

**Requisitos:** NV03-REQ-007, NV03-REQ-008.

### Fase P4 — Privacidad y permisos

| Task | Entregable |
|---|---|
| NV03-T013 | Privacy notice y enlace. |
| NV03-T014 | Permissions disclosure. |
| NV03-T015 | Data handling y Data Safety. |

**Requisitos:** NV03-REQ-009, NV03-REQ-010, NV03-REQ-011.

### Fase P5 — CI/CD y costo

| Task | Entregable |
|---|---|
| NV03-T016 | Evaluación COST-001 de servicios externos. |
| NV03-T017 | Pipeline de release. |
| NV03-T018 | Secretos del pipeline y permisos mínimos. |

**Requisitos:** NV03-REQ-012, NV03-REQ-013.

### Fase P6 — Evidencia y autorización

| Task | Entregable |
|---|---|
| NV03-T019 | Evidencia de release. |
| NV03-T020 | Quality gates pre-release. |
| NV03-T021 | Gate humano de publicación. |
| NV03-T022 | Plan de rollback y versionado. |
| NV03-T023 | Auditoría final de alcance. |
| NV03-T024 | Revisión final con `sdd-guardian`. |

**Requisitos:** NV03-REQ-014, NV03-REQ-015, NV03-REQ-016.

## 3. Matriz completa

| Requisito | Fase | Tasks |
|---|---|---|
| NV03-REQ-001 | P1 | NV03-T001, NV03-T002 |
| NV03-REQ-002 | P1 | NV03-T002 |
| NV03-REQ-003 | P2 | NV03-T005, NV03-T008 |
| NV03-REQ-004 | P2 | NV03-T006, NV03-T007, NV03-T018 |
| NV03-REQ-005 | P1/P2 | NV03-T003, NV03-T008 |
| NV03-REQ-006 | P1 | NV03-T003, NV03-T004 |
| NV03-REQ-007 | P3 | NV03-T010, NV03-T011 |
| NV03-REQ-008 | P3 | NV03-T012 |
| NV03-REQ-009 | P4 | NV03-T013 |
| NV03-REQ-010 | P4 | NV03-T014 |
| NV03-REQ-011 | P4 | NV03-T015 |
| NV03-REQ-012 | P5 | NV03-T016 |
| NV03-REQ-013 | P5 | NV03-T017, NV03-T018 |
| NV03-REQ-014 | P6 | NV03-T019 |
| NV03-REQ-015 | P6 | NV03-T021, NV03-T023 |
| NV03-REQ-016 | P6 | NV03-T020, NV03-T022, NV03-T024 |

## 4. Dependencias

```text
P1 → P2 → P3 → P4 → P5 → P6
```

P3 requiere signing aprobado. P6 requiere todas las fases y autorización humana.

## 5. Checklist Play

- Identidad de la aplicación.
- Ficha y assets.
- Clasificación de contenido.
- Privacy policy URL.
- Data Safety.
- Permissions declaration.
- Testing tracks.
- AAB firmado.
- Países y precios cuando aplique.
- Revisión de COST-001.

Notas U2 (NV03-REQ-008, 009b, 019):

- U2: pista interna.
- Producción futura: prueba cerrada solo si la política de la cuenta lo exige (cuenta personal nueva: 12 testers / 14 días consecutivos). No afirmar que aplica hasta verificar tipo y fecha de la cuenta.
- Público 18+; URL privacidad `<URL_AVISO_PRIVACIDAD>` (pendiente humano).
- Eliminación de cuenta: `Posterior — obligatorio antes de producción`.
- Play App Signing; upload key fuera del repositorio; sin contraseñas en Git.

## 6. Quality gates

```text
build release exitoso
testDebugUnitTest exitoso
lintDebug exitoso
AAB validado
sin secretos en repositorio
privacy/Data Safety revisados
COST-001 aprobado
autorización humana registrada
```

## 7. Riesgos

| Riesgo | Mitigación |
|---|---|
| Pérdida de keystore | Backup seguro y procedimiento documentado. |
| Secreto filtrado | Secrets fuera del repo y scanning. |
| Publicación accidental | Gate humano y workflow sin deploy automático. |
| Data Safety incorrecto | Revisión cruzada con código y permisos. |
| Costo inesperado | COST-001 previo. |

## 8. Criterio de salida

- Release preparado y validado.
- AAB verificable cuando se autorice.
- Privacidad y permisos completos.
- COST-001 aprobado.
- Publicación no ejecutada o autorizada explícitamente.
- `sdd-guardian` emite PASS o PASS WITH WARNINGS.

## 9. Estado

```text
⛔ NO EJECUTAR
```

## Cobertura Play (C1) — aditivo

- Fases P1–P6 ya ejecutadas según `evidence.md` (AAB firmado `jar verified.`, `versionName 1.0.0`, `versionCode 10000`, `applicationId com.thehouse.app`). Sin publicación en Play.
- Pendiente humano: `BLOCKED-PRIVACY-URL` (URL real del aviso de privacidad), Data Safety con valores reales, store listing final (icono/screenshots/feature graphic, content rating, App Access, tester/reviewer credentials por canal seguro, release notes) y human release approval antes de cualquier subida a Internal/Closed/Production.
- **NO publicar.** Ninguna fase de este plan autoriza subida, promoción ni publicación.
