# NV-03 — RELEASE / GOOGLE PLAY

> ## ⛔ NO EJECUTAR
>
> Esta spec está en estado **NO AUTORIZADO**. No generar AAB, no configurar signing, no publicar y no marcar tasks sin autorización humana explícita.

| Campo | Valor |
|---|---|
| Spec ID | NV-03 |
| Nombre | Release / Google Play |
| Versión | 1.0.0 |
| Fecha | 2026-10-31 |
| Estado | ⛔ NO EJECUTAR |
| Autorización | PENDIENTE |
| Dependencia | NV-01 y NV-02 aprobadas; funcionalidad lista |

## 1. Contexto

TheHouse Mobile aún no tiene funcionalidades de negocio ni release configurado. Esta spec define el proceso futuro para versionar, firmar, empaquetar y preparar Google Play sin exponer secretos ni publicar automáticamente.

## 2. Objetivo

Establecer un release seguro, reproducible y conforme a Google Play, con signing protegido, AAB verificable, privacidad, permisos, Data Safety, evidencia y COST-001.

## 3. Alcance

- `versionCode`.
- `versionName`.
- Signing.
- Secretos de signing.
- Debug vs release.
- Build release y minificación.
- AAB.
- Google Play preparation.
- Privacy notice.
- Permissions disclosure.
- Data handling / Data Safety.
- COST-001.
- CI/CD release.
- Evidencia.
- Autorización y control de publicación.
- Rollback y estrategia de versiones.

## 4. Fuera de alcance

- Publicar la aplicación.
- Crear cuenta de Play Console.
- Comprar servicios.
- Subir AAB.
- Configurar Firebase, Maps, ML o IA.
- Implementar funcionalidades.
- Cambiar el toolchain.
- Firmar con credenciales reales sin autorización.

## 5. Requisitos

### NV03-REQ-001 — Política de versionCode

Definir un `versionCode` entero, monotónicamente creciente y único por release.

**Aceptación:** la política evita colisiones y está documentada.

### NV03-REQ-002 — Política de versionName

Definir un `versionName` legible y coherente con el release.

**Aceptación:** existe convención y trazabilidad con `versionCode`.

### NV03-REQ-003 — Separación de signing

Separar signing debug y release.

**Aceptación:** release no usa la clave de debug.

### NV03-REQ-004 — Secretos de signing

Definir almacenamiento seguro de keystore y passwords.

**Aceptación:** ningún secreto está en el repositorio; se usan secrets locales o CI.

### NV03-REQ-005 — Debug vs release

Definir diferencias de build, logging, endpoints y optimización.

**Aceptación:** release no habilita logging sensible ni configuración de debug.

### NV03-REQ-006 — Release build y minificación

Definir optimización, minificación y reglas de preservación.

**Aceptación:** el build release es reproducible y no rompe APIs necesarias.

### NV03-REQ-007 — AAB

Definir la generación y verificación de Android App Bundle.

**Aceptación:** el AAB se genera solo con autorización y se valida antes de cargar.

### NV03-REQ-008 — Preparación de Google Play

Definir checklist de ficha, clasificación, países, testing y requisitos de Play Console.

**Aceptación:** el checklist está completo y no se publica automáticamente.

### NV03-REQ-009 — Privacy notice

Definir enlace y contenido mínimo del aviso de privacidad.

**Aceptación:** la política es accesible y coherente con el manejo de datos.

### NV03-REQ-010 — Permissions disclosure

Documentar permisos y su justificación para Play.

**Aceptación:** cada permiso tiene finalidad y disclosure asociada.

### NV03-REQ-011 — Data handling y Data Safety

Documentar recolección, uso, compartición, retención y seguridad de datos.

**Aceptación:** Data Safety coincide con el comportamiento real.

### NV03-REQ-012 — COST-001 de servicios externos

Evaluar Firebase, Maps, APIs, analytics, ML e IA antes de release.

**Aceptación:** cada servicio demuestra costo $0, límites, riesgo y alternativa.

### NV03-REQ-013 — CI/CD release

Definir pipeline de release con permisos mínimos y secrets seguros.

**Aceptación:** no hay credenciales en el repositorio; la publicación requiere aprobación.

### NV03-REQ-014 — Evidencia de release

Registrar versión, artefacto, tests, lint, signing y aprobaciones.

**Aceptación:** cada release tiene evidencia verificable.

### NV03-REQ-015 — Autorización de publicación

Definir un gate humano obligatorio antes de cargar o publicar.

**Aceptación:** ningún agente o pipeline publica sin aprobación explícita.

### NV03-REQ-016 — Validación y rollback

Definir validaciones previas y estrategia de rollback/versionado.

**Aceptación:** existe plan de contingencia y criterio de aborto.

## 6. Trazabilidad

| Requisito | Fase | Tasks |
|---|---|---|
| NV03-REQ-001 | P1 | NV03-T001, NV03-T002 |
| NV03-REQ-002 | P1 | NV03-T002 |
| NV03-REQ-003 | P2 | NV03-T005, NV03-T008 |
| NV03-REQ-004 | P2 | NV03-T006, NV03-T007, NV03-T018 |
| NV03-REQ-005 | P1 | NV03-T003, NV03-T008 |
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

## 7. Criterios de aceptación globales

- `versionCode` válido.
- AAB validado.
- Signing seguro.
- Tests y lint exitosos.
- Privacy notice y Data Safety completos.
- Permisos justificados.
- COST-001 en $0.
- Ninguna publicación ejecutada sin autorización.

## 8. Riesgos

- Pérdida de keystore.
- Filtración de passwords.
- Publicación accidental.
- Data Safety inconsistente.
- Servicios con costo no evaluados.
- Cambios de toolchain durante release.

## 9. Estado

```text
⛔ NO EJECUTAR
```
