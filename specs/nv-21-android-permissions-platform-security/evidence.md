# NV-21 — EVIDENCIA

## Spec
- `NV-21 Permissions / Platform Security`
- Rama: `feature/permissions-multimedia` (creada desde `feature/offline-cache-sync` limpio en precheck)
- Autorización: humana explícita 2026-11-17 (actualizar specs NV-21/NV-22, implementar Mobile, tests/lint/build, commit+push). Sin merge, sin force push, sin Backend.

## Precheck (real)
- `git status --short` → vacío
- `git branch --show-current` → `feature/offline-cache-sync` (antes de crear rama de trabajo)
- `git log -5 --oneline` → `9afba91`, `4b599b0`, `4a02b6f`, `aa9fa1e`, `9b2540b`
- Cambios locales ajenos: no.

## Inventario y auditoría (real)
- `app/src/main/AndroidManifest.xml`: solo `INTERNET` y `POST_NOTIFICATIONS`. Sin `CAMERA`, sin `ACCESS_FINE_LOCATION/COARSE_LOCATION`, sin `READ_MEDIA_*`/`READ_EXTERNAL_STORAGE`, sin background location.
- `POST_NOTIFICATIONS`: preservado de NV-14 (solicitud contextual desde Notification Center, Android 13+ runtime, rechazo/rationale/permanente seguro, Android <13 sin runtime). No se solicita al startup.
- Cámara: no existe flujo real de captura en código actual → no declarada, no solicitada.
- Ubicación: no existe funcionalidad "cerca de mí"/ubicación del dispositivo → no declarada, no solicitada.
- Selección multimedia futura: preferencia por Photo Picker/Activity Result APIs (sin permisos amplios de almacenamiento).
- Superficie plataforma: `MainActivity` exported con launcher, `TheHouseFirebaseMessagingService` `exported=false`, `usesCleartextTraffic=false`, `networkSecurityConfig`, backup/data extraction excluyen `datastore/` y `sharedpref`; sin deep links.
- `PRIVACY_POLICY_URL` sigue vacío (`BLOCKED-PRIVACY-URL`, NV-03) → conciliación Data Safety completa permanece **PARTIAL**.

## Cambios realizados
- Actualización SDD NV-21/NV-22 (spec/plan/task/checklist/evidence).
- Utilidades/preparación móvil NV-22: validación local multimedia, estados honestos de moderación/upload (`MULTIMEDIA-MODERATION-BACKEND-BLOCKED`), inspección/sanitización best-effort por `ContentResolver` (recompresión con orientación para no propagar EXIF GPS/metadatos innecesarios), helpers Photo Picker y política pura de permiso cámara (solo uso futuro con flujo real).
- Sin cambios de Backend, sin endpoints/moderación inventados, sin secretos/URLs hardcodeadas.

## Gates ejecutados (reales)
- `.\gradlew.bat testDebugUnitTest` → **BUILD SUCCESSFUL** (27 actionable tasks: 7 executed, 20 up-to-date; configuration cache reused). Incluye tests nuevos de validación multimedia/estados/políticas.
- `.\gradlew.bat lintDebug` → **BUILD SUCCESSFUL** (29 actionable tasks: 4 executed, 25 up-to-date). Reportes: `app/build/reports/lint-results-debug.html`, `lint-results-debug.sarif`.
- `.\gradlew.bat assembleDebug` → **BUILD SUCCESSFUL** (38 actionable tasks: 3 executed, 35 up-to-date).

## Runtime / ADB
- `adb` no disponible en entorno (`adb` no reconocido). Validación runtime en dispositivo/emulador **NO EJECUTADA** (no inventada). Cobertura de comportamiento crítico preservado respaldada por tests/políticas existentes y nuevas.

## Riesgos / bloqueos
- `BLOCKED-PRIVACY-URL` (NV-03) fuera de alcance.
- Permisos transitivos del manifest fusionado requieren verificación en release/CI.
- Sin contrato backend ejecutable de upload/moderación multimedia (`MULTIMEDIA-MODERATION-BACKEND-BLOCKED`).
