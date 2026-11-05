# NV-03 — RELEASE / GOOGLE PLAY

> ## ⛔ NO EJECUTAR
>
> Esta spec está en estado **NO AUTORIZADO**. No generar AAB, no configurar signing, no publicar y no marcar tasks sin autorización humana explícita.

| Campo | Valor |
|---|---|
| Spec ID | NV-03 |
| Nombre | Release / Google Play |
| Versión | 1.1.0 |
| Fecha | 2026-11-04 |
| Estado | ⛔ NO EJECUTAR |
| Autorización | PENDIENTE |
| Alcance | U2 |
| Dependencia | NV-01 ✅ ejecutada; se ejecuta antes de NV-04 en el orden U2; NV-02 documenta ahora y ejecuta después |

## 1. Contexto

TheHouse Mobile aún no tiene funcionalidades de negocio ni release configurado. Esta spec define el proceso futuro para versionar, firmar, empaquetar y preparar Google Play sin exponer secretos ni publicar automáticamente.

Nota U2: la cuenta de Play Console **ya existe y ya está pagada**; no hay task de creación de cuenta. Para U2 la entrega es AAB firmado disponible (verificación); la subida a pista interna requiere autorización humana.

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
- Crear cuenta de Play Console (ya existe y está pagada).
- Comprar servicios.
- Subir AAB (U2 entrega AAB firmado disponible; la subida requiere autorización humana).
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

Detalles U2:

- Play App Signing con upload key.
- Upload key en keystore **fuera del repositorio**.
- Nunca guardar contraseñas en Git; usar variables de entorno o `local.properties` (ignorado).
- El `signingConfig` de release obtiene credenciales desde esas fuentes.

**Aceptación:** ningún secreto está en el repositorio; se usan secrets locales ignorados o CI.

### NV03-REQ-005 — Debug vs release

Definir diferencias de build, logging, endpoints y optimización.

Detalles U2: logging de aplicación desactivado/reducido en release; `API_BASE_URL` configurable (`BuildConfig`), con valor U2 conocido `https://ca-thehouse-u2.grayisland-cb722b5f.eastus2.azurecontainerapps.io`; no hardcodear la URL dentro de clases Kotlin.

**Aceptación:** release no habilita logging sensible ni configuración de debug.

### NV03-REQ-006 — Release build y minificación

Definir optimización, minificación y reglas de preservación. U2: AAB firmado con **R8 activo** en release.

**Aceptación:** el build release es reproducible y no rompe APIs necesarias.

### NV03-REQ-017 — Identidad de la aplicación

- `applicationId` definitivo: `<APPLICATION_ID>` — **PENDIENTE HUMANO** hasta recibir el valor real.
- Es permanente después del primer AAB publicado.
- Alternativa de referencia actual documentada: `com.thehouse.app` (M-MOB-024) como placeholder hasta decisión humana.

**Aceptación:** el `applicationId` existe, es visto por el humano antes del primer AAB y no cambia después.

### NV03-REQ-007 — AAB

Definir la generación y verificación de Android App Bundle.

**Aceptación:** el AAB se genera solo con autorización y se valida antes de cargar.

### NV03-REQ-008 — Preparación de Google Play

Definir checklist de ficha, clasificación, países, testing y requisitos de Play Console.

Detalle U2 y de producción futura:

- **U2:** pista interna de Play Console.
- **Producción futura:** prueba cerrada solo si la política aplicable a la cuenta lo exige. Si es cuenta personal nueva sujeta a esa regla: mínimo 12 testers y 14 días consecutivos. **No afirmar que aplica hasta verificar el tipo y la fecha de creación de la cuenta** (`PENDIENTE HUMANO`).

**Aceptación:** el checklist está completo y no se publica automáticamente.

### NV03-REQ-009 — Privacy notice

Definir enlace y contenido mínimo del aviso de privacidad.

URL del aviso: `<URL_AVISO_PRIVACIDAD>` — **PENDIENTE HUMANO**.

**Aceptación:** la política es accesible y coherente con el manejo de datos.

### NV03-REQ-009b — Store listing

Documentar para Play: URL del aviso de privacidad (`<URL_AVISO_PRIVACIDAD>`), Data Safety, clasificación de contenido, público **18+** y capturas de pantalla.

**Aceptación:** el listing está completo sin datos falsos en Data Safety.

### NV03-REQ-018 — Manifest y seguridad de red

Tasks U2:

- `android.permission.INTERNET` como único permiso (justificado: comunicación HTTPS con el backend).
- Tráfico en claro deshabilitado (`usesCleartextTraffic=false`).
- `network_security_config` restringido al dominio del backend.
- Reglas de backup: `fullBackupContent` y `data_extraction_rules` que **excluyan cualquier almacenamiento de sesión/token**.

**Aceptación:** manifest verificado, cleartext deshabilitado y backups excluyen tokens.

### NV03-REQ-019 — Eliminación de cuenta

`Posterior — obligatorio antes de producción`. Documentado en store listing y Data Safety cuando aplique.

**Aceptación:** estado registrado y bloqueo de producción hasta cumplimiento.

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

| NV03-REQ-016 | P6 | NV03-T020, NV03-T022, NV03-T024 |
| NV03-REQ-017 | P1 | NV03-T001 |
| NV03-REQ-009b | P4 | NV03-T013, NV03-T015 |
| NV03-REQ-018 | P3/P4 | NV03-T025 |
| NV03-REQ-019 | P4 | NV03-T015 |

## 7. Criterios de aceptación globales

- `versionName`/`versionCode` coherentes con `VERSIONING.md`.
- `applicationId` definitivo autorizado por el humano (`<APPLICATION_ID>`).
- AAB release firmado con R8 activo y logging reducido.
- Signing seguro (Play App Signing, upload key fuera del repo, sin contraseñas en Git).
- Manifest/red: INTERNET justificado, cleartext deshabilitado, backups excluyen tokens.
- Tests y lint exitosos.
- Privacy notice (`<URL_AVISO_PRIVACIDAD>`) y Data Safety completos.
- Permisos justificados, público 18+.
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
