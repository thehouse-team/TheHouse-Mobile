# NV-21 — ANDROID PERMISSIONS / PLATFORM SECURITY

| Campo | Valor |
|---|---|
| Spec ID | NV-21 |
| STATUS | EXECUTED (MOBILE hardening) / PARTIAL (Data Safety URL NV-03 sigue bloqueada por política de privacidad) |
| AUTHORIZATION | AUTORIZADO por instrucción humana explícita 2026-11-17 (sin merge, sin force push, sin Backend, sin inventar contratos) |
| Dependencias | NV-03 (manifest/permisos), NV-14 (notificaciones), NV-15, NV-16, NV-20 |

## Problema
Cada permiso Android es superficie de ataque y de declaración en Play; la app necesita inventario justificado, mínimo privilegio y flujos runtime seguros.

## Alcance (ejecutado)
- Auditoría de permisos declarados y superficie de plataforma (exported, intents, backup/data extraction).
- Preservación de `POST_NOTIFICATIONS` contextual (NV-14), sin solicitud al startup.
- Verificación de que no se requieren ni declaran `CAMERA`, ubicación, ni permisos de lectura de almacenamiento/media para el estado actual del producto.
- Preparación de política runtime reutilizable para `CAMERA` (solo si existe flujo real futuro; no invocada).

## No-alcance
- Agregar permisos sin justificación/uso real.
- Ubicación en background.
- Cambios de Backend, endpoints, merge, publicación Play.
- Completar Data Safety pública de Play (depende de URL de privacidad NV-03 `BLOCKED-PRIVACY-URL`).

## Inventario de permisos

| Permiso | Declarado | Justificación | Spec/uso | Runtime | Estado |
|---|---:|---|---|---|---|
| `android.permission.INTERNET` | Sí | Comunicación HTTPS con API/catalog/chat/notificaciones según contratos existentes | NV-01/NV-04+ red | No (normal) | MANTENER |
| `android.permission.POST_NOTIFICATIONS` | Sí | Notificaciones locales/FCM data-only para usuario autenticado | NV-14 | Sí, Android 13+ contextual desde Notification Center; Android <13 no aplica | MANTENER |
| `android.permission.CAMERA` | No | No existe flujo real de captura en la app actual | NV-22 futuro | Debe pedirse solo al usar captura si se implementa | NO DECLARAR / NO PEDIR |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | No | No existe funcionalidad "cerca de mí"/ubicación real del dispositivo | NV-07 sin uso real | Solo si se activa función real | NO DECLARAR / NO PEDIR |
| `READ_MEDIA_*` / `READ_EXTERNAL_STORAGE` | No | Selección prevista por Photo Picker/Activity Result sin permisos amplios de almacenamiento | NV-22 | N/A con Photo Picker | NO DECLARAR / NO PEDIR |

## Superficie de plataforma auditada
- `MainActivity`: `exported=true` con `MAIN/LAUNCHER` (requerido).
- `TheHouseFirebaseMessagingService`: `exported=false`, intent filter `MESSAGING_EVENT`.
- `allowBackup=true` con `dataExtractionRules` y `fullBackupContent` excluyendo `datastore/` y `sharedpref` (sesión/tokens/datos locales sensibles fuera de backup).
- `usesCleartextTraffic=false`, `networkSecurityConfig` presente.
- Sin deep links/app links declarados; sin componentes exportados adicionales.

## Requisitos
- NV21-REQ-001: Inventario y mínima superficie — **PASS**.
- NV21-REQ-002: Runtime permissions contextuales y fallback seguro — **PASS** para notificaciones; cámara/location no solicitadas innecesariamente.
- NV21-REQ-003: Componentes exportados/intents/backups auditados — **PASS**.

## Criterios de aceptación
- Manifest sin permisos innecesarios: **PASS**.
- Flujo sin permiso de notificación no rompe app: **PASS (NV-14 preservado + cobertura existente)**.
- Cámara/ubicación no solicitadas sin función real: **PASS**.
- Conciliación Data Safety NV-03 completa: **PARTIAL** (`BLOCKED-PRIVACY-URL` previo, fuera de alcance de este cambio).

## Riesgos
- Permisos transitivos del manifest fusionado deben revisarse en CI/release (O-01 histórico).
- Revisión Play/Data Safety depende de URL de privacidad y datos finales.

## Impacto seguridad
- Mínimo privilegio, sin exposición de almacenamiento/ubicación/cámara.
- `POST_NOTIFICATIONS` solo contextual; rechazo seguro.

## COST-001 ($0)
- Sin servicios nuevos. Revisión/implementación local $0.

## Relación con otras specs
- NV-03 (manifest/Data Safety), NV-14 (push), NV-15/NV-16 (no regresión), NV-22 (multimedia), NV-20.

## Evidencia
- Ver `evidence.md` de esta spec (comandos, resultados, límites de runtime/ADB/contratos).
