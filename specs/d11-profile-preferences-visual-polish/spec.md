# D11 — PROFILE + PREFERENCES + QUICK SETUP + VISUAL POLISH

| Campo | Valor |
|---|---|
| Spec ID | D11 |
| Nombre | Profile + Preferences + Quick Setup + Visual Polish |
| Versión | 1.0.0 |
| Fecha | 2026-11-12 |
| Estado | PARCIAL — Fase A (funcional) PENDIENTE DE FORMALIZACIÓN Y VALIDACIÓN; Fase B (Visual Polish) PLANNED / NO EJECUTADO |
| Autorización | APROBADA por instrucción humana explícita (gobernanza D11, 2026-11-12) |
| Branch | `feature/profile-preferences-ui-polish` |
| Módulo | `:app` |
| Relación NV | No existe spec NV previa para Profile/Preferences/Quick Setup. La trazabilidad académica mapea «Perfil / cuenta» a NV-27 (BLOCKED: `/users/me` sin confirmar). D11 NO reutiliza `nv-11-chat` ni ninguna otra spec; permanece separada. |

## Problema

El flujo de cuenta del estudiante (ver/editar perfil, seleccionar universidad, preferencias de búsqueda y Quick Setup configurativo) no tiene spec registrada; el trabajo funcional correspondiente existe en el working tree sin formalizar ni validar. Adicionalmente la app carece de una pasada de consistencia visual global.

## Alcance

### D11-FUNCTIONAL (estado: IMPLEMENTACIÓN EXISTENTE / PENDIENTE DE FORMALIZACIÓN Y VALIDACIÓN)
- APIs reales de perfil y preferencias (`ProfileApi`, `PreferencesApi`), DTOs, repositorios y use cases (`GetProfile`, `UpdateProfile`, `SelectUniversity`, `GetPreferences`, `SavePreferences`, `GetQuickSetup`).
- Registro DI en `AppContainer`.
- Sin pantallas de presentación nuevas de Profile/Preferences/Quick Setup en este commit (no existen aún en `presentation/`).

### D11-VISUAL-POLISH (estado: PLANNED / NO EJECUTADO)
- Design system centralizado (colores, typography, shapes, spacing, cards, buttons, inputs).
- Pulido visual de Onboarding, Login, Register, Home, Explore, Map, Property Detail, Favorites, Visits, Profile, Preferences, Quick Setup y Navigation.
- Patrones reutilizables Loading / Empty / Error, accesibilidad, responsive, runtime visual QA.

## No-alcance

- Backend, contratos de API, Chat, Landlord, ML, IA, SDK de mapas de pago, nueva paleta/logo/navegación, datos fake, refactor arquitectónico.

## Requisitos

- D11-REQ-001: Solo endpoints confirmados por el backend. Contrato registrado por NV-27 confirmaba `auth/*`, `properties`, `universities`, `health`; `GET/PUT /users/me/preferences` y `users/me/profile` NO estaban confirmados en la matriz de trazabilidad. Deben validarse contra el backend real antes de declarar EXECUTED.
- D11-REQ-002: Arquitectura MVVM + Clean Architecture + UDF; UI no consume Retrofit directamente; Manual DI vía `AppContainer`.
- D11-REQ-003: Ningún dato inventado ni fake; sin secretos; sin PII en logs.
- D11-REQ-004: Visual Polish no rehace lógica funcional ni cambia contratos; fidelidad al prototipo autoritativo (https://thehouse-prototipo.netlify.app/pantallas/00-onboarding.html).

## Criterios de aceptación

- D11-FUNCTIONAL: `compileDebugKotlin` y `testDebugUnitTest` en verde con el working tree íntegro (sin archivos borrados que el código sobreviviente requiera).
- D11-VISUAL-POLISH: `compileDebugKotlin`, luego `testDebugUnitTest lintDebug assembleDebug`; runtime visual QA si hay emulador; commit `style: polish mobile ui and unify design system`.

## Riesgos

- Working tree actual borra 23 archivos (`AuthApi`, `CatalogApis`, `FavoritesApi`, `VisitsApi`, modelos y use cases de auth/visits/favorites/catalog) que ~30 archivos sobrevivientes aún importan → el árbol actual NO compila.
- `AppContainer` tiene `val selectUniversity` duplicado (líneas ~194 y ~227) e importa `SelectUniversityUseCase2`, que no existe.
- `ProfileApi.kt` y `PreferencesApi.kt` declaran `profile interface` (error de sintaxis Kotlin).
- Endpoints de perfil/preferencias sin confirmación registrada de contrato (riesgo BLOCKED-BACKEND).

## Impacto seguridad

- Sin secretos versionados; tokens fuera de logs; sin permisos nuevos; datos de perfil minimizados.

## COST-001 ($0)

- Sin servicios externos nuevos. Free tier: backend existente. Tarjeta: no. Riesgo de cobro: ninguno. Mitigación: sin integraciones de pago/mapa/IA. Alternativa: N/A.

## Evidencia esperada

- Registro de validación en `evidence.md` de esta spec; actualización de `docs/sdd/REQUIREMENTS_TRACEABILITY.md`; entrada en `memoria.md`.
