# D11 — PROFILE + PREFERENCES + QUICK SETUP + VISUAL POLISH

| Campo | Valor |
|---|---|
| Spec ID | D11 |
| Nombre | Profile + Preferences + Quick Setup + Visual Polish |
| Versión | 1.1.0 |
| Fecha | 2026-11-19 |
| Estado | IMPLEMENTACIÓN UI EN CURSO (branch `feature/profile-preferences-ui`) — pendiente gates/runtime finales |
| Autorización | APROBADA por instrucción humana explícita (esta sesión, 2026-11-19) |
| Branch actual | `feature/profile-preferences-ui` (desde `feature/ui-redesign`) |
| Módulo | `:app` |
| Relación NV | No existe spec NV previa para Profile/Preferences/Quick Setup. Trazabilidad académica «Perfil / cuenta»: NV-27 sigue BLOCKED para contrato `/users/me`; Mobile consume exclusivamente capas data/domain/use cases D11 ya existentes sin inventar endpoints. |

## Problema

El estudiante no tenía pantallas reales de Perfil, Preferencias y Configuración inicial (Quick Setup), a pesar de existir capas data/domain/use cases previas. Esto impedía ver/editar datos de cuenta soportados y configurar preferencias reales consumidas por Smart Match.

## Alcance (implementado en esta iteración)

### D11-FUNCTIONAL (data/domain existente, sin cambios de contrato)
- Reutiliza APIs/DTOs/repositorios/use cases existentes: `ProfileApi`, `PreferencesApi`, `GetProfileUseCase`, `UpdateProfileUseCase`, `SelectUserUniversityUseCase`, `GetPreferencesUseCase`, `SavePreferencesUseCase`, `GetQuickSetupUseCase`, `GetUniversitiesUseCase`.
- Sin nuevos endpoints, sin cambios de lógica de negocio innecesarios.

### D11-UI (nuevo)
- `presentation/profile`: `ProfileViewModel`, `ProfileScreen`/`ProfileDestination`.
  - Muestra datos reales disponibles: nombre, correo de sesión activa, universidad y estado de Quick Setup.
  - Edición de nombre vía `UpdateProfileUseCase` (loading/success/error).
  - Selección de universidad vía `SelectUserUniversityUseCase` con catálogo real (`GetUniversitiesUseCase`).
- `presentation/preferences`: `PreferencesViewModel`, `PreferencesScreen`/`PreferencesDestination`.
  - Carga/edición/guardado vía `GetPreferencesUseCase` y `SavePreferencesUseCase`.
  - Campos reales del modelo: presupuesto (min/max/moneda), distancia máxima, transporte, convivencia, tipo de lugar, mascotas, servicios esenciales y tipos de propiedad.
- `presentation/quicksetup`: `QuickSetupViewModel`, `QuickSetupScreen`/`QuickSetupDestination`.
  - Flujo breve con campos soportados (nombre, universidad) y acceso a Preferencias.
  - Usa `GetQuickSetupUseCase`/perfil; el flag `quickSetupCompleted` es de solo lectura (no se inventa operación de marcado).
- Navegación: rutas `perfil`, `preferencias`, `configuracion_inicial` en `AppRoutes`; destinos en `MainActivity`.
- Accesos: Home (cuenta autenticada) → Perfil/Preferencias/Configuración inicial; Perfil → Preferencias/Quick Setup; Smart Match (NoPreferences) → Preferencias.
- Design System existente (`TheHouseScreen`, TopBar, Cards, Avatar, SectionHeader, Buttons, Fields, StateViews, InlineMessage, tokens) sin hardcodeo repetido de colores/spacing.
- Estados: Loading / Content / Error / Retry en cada pantalla; mensajes seguros (sin JWT/tokens/IDs técnicos/PII sensible en UI o logs).

## No-alcance

- Backend, contratos, DTOs, repositorios o reglas de negocio; Map; Roommate; Reviews (NV-13 bloqueado).
- `app/google-services.json`, secretos, signing, AAB, merge, force push, `reset --hard`.
- Nuevas operaciones no soportadas (p. ej. forzar/completar Quick Setup sin contrato).

## Requisitos

- D11-REQ-001: Solo datos/campos/endpoints ya soportados por las capas existentes.
- D11-REQ-002: MVVM + UDF + Clean Architecture; UI consume ViewModels/use cases/repositorios de dominio existentes, no Retrofit.
- D11-REQ-003: Sin datos inventados/fake, sin secretos, sin PII sensible en logs/UI.
- D11-REQ-004: Consistencia visual con el Design System actual.

## Criterios de aceptación

- Pantallas reales de Profile, Preferences y Quick Setup integradas y navegables.
- Smart Match sigue consumiendo las mismas preferencias reales (sin duplicar lógica).
- `testDebugUnitTest`, `lintDebug`, `assembleDebug` en verde (máx. 1 ciclo correctivo).
- Runtime en emulador operativo: navegación y estados sin crashes (o parcial documentado si faltan credenciales/backend).

## Impacto seguridad

- Sin tokens/JWT/IDs técnicos expuestos. Errores mostrados mediante mensajes seguros. Sin permisos nuevos.

## COST-001 ($0)

- Sin servicios externos nuevos ni riesgo de cobro.

## Evidencia

- Ver `evidence.md` (implementación, tests/gates/runtime y commit/push de esta iteración).
