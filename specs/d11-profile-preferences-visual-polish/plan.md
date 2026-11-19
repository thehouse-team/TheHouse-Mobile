# D11 — PLAN

| Campo | Valor |
|---|---|
| Spec | D11 (Profile / Preferences / Quick Setup) |
| Estado | Fases históricas A/B documentadas; Fase C (UI real) en ejecución/validación en `feature/profile-preferences-ui` |
| Autorización | APROBADA por instrucción humana explícita (2026-11-19) |

## FASE A — Functional stabilization (histórica, completada)
- A1–A7: gobernanza, restauración, correcciones y validación funcional de capas data/domain/use cases. Ver `evidence.md`.

## FASE B — Visual Polish (histórica, completada parcialmente)
- Design system y pulido de pantallas previas ejecutado en branch anterior; QA runtime quedó pendiente por entorno. No reabierta en esta iteración.

## FASE C — UI real Profile / Preferences / Quick Setup (TASK ACTIVA)

1. **Precheck y rama**: preservar working tree; crear `feature/profile-preferences-ui` desde `feature/ui-redesign`.
2. **Inventario de contrato existente**: revisar repos/APIs/DTOs/modelos/use cases D11, `SmartMatchViewModel`, navegación y Design System. Sin segunda arquitectura ni contratos inventados.
3. **Implementación UI (MVVM + UDF)**:
   - Profile: vista de datos reales (nombre, identidad básica de sesión, universidad, estado Quick Setup), edición de nombre y selección de universidad solo con operaciones existentes.
   - Preferences: carga/edición/guardado de campos reales del modelo `Preference`.
   - Quick Setup: flujo breve con campos soportados; flag de completitud de solo lectura.
4. **Navegación**: rutas `perfil`, `preferencias`, `configuracion_inicial`; accesos desde Home/Profile y CTA de Smart Match (NoPreferences) hacia Preferencias.
5. **Estados y privacidad**: Loading/Content/Error/Retry; mensajes seguros; sin tokens/IDs técnicos/PII innecesaria.
6. **Tests**: unit tests de ViewModels (load/success/error/save/select/quick setup state) según necesidad.
7. **Gates**: `testDebugUnitTest`, `lintDebug`, `assembleDebug` (máx. 1 ciclo correctivo).
8. **Runtime**: validación en `emulator-5554` si operativo (navegación y estados básicos, sin inventar PASS).
9. **Spec/evidencia y commit**: actualizar D11, `git diff --check`, commit `feat: add profile preferences and quick setup UI`, push `origin feature/profile-preferences-ui` (sin merge).
