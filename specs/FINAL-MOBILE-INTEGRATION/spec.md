# FINAL-MOBILE-INTEGRATION

> ## ⚠️ INTEGRACIÓN COMPLETADA - BLOQUEO DE BUILD

> Esta spec ha sido autorizada por el prompt humano. Objetivo: Consolidar ramas Mobile ya implementadas, resolver únicamente conflictos/errores de integración y recuperar main verde para evaluación final.

| Campo | Valor |
|---|---|
| Spec ID | FINAL-MOBILE-INTEGRATION |
| Nombre | Consolidación Final Mobile |
| Versión | 1.0.0 |
| Fecha | 2026-11-20 |
| Estado | ⛔ PENDIENTE DE EJECUCIÓN |
| Autorización | APPROVED FOR IMPLEMENTATION (por prompt humano) |
| Módulo | `:app` |

## 1. Contexto

El repositorio TheHouse Mobile tiene el branch `main` por debajo de `origin/main`. El trabajo de múltiples features ya ha sido fusionado en `origin/main`, pero `main` no ha sido actualizado para incluir esos cambios. Esta spec tiene como objetivo actualizar `main` para incluir todo el trabajo móvil terminado y preparar el proyecto para la auditoría final del profesor.

## 2. Precondiciones

Las siguientes condiciones ya existen y **no deben recrearse**:

- Proyecto Android nativo existente compilado con AGP `9.5.0`, Gradle `9.6.0`, Kotlin `2.2.10`
- `compileSdk = 37`, `targetSdk = 37`, `minSdk = 26`
- Kotlin `2.2.10`
- Estructura SDD creada
- Todas las specs NV-01 a NV-29 documentadas
- `app/google-services.json` presente en el worktree
- Branches de features ya implementadas en `origin/main`

## 3. Objetivo

Consolidar ramas Mobile ya implementadas, resolver únicamente conflictos/errores de integración y recuperar `main` verde para evaluación final. **No implementar nuevas funcionalidades.** Solo integración y corrección de errores derivados de merges.

## 4. Orden de Integración

Integrar en el siguiente orden, ajustándose a la historia real:

1. **UI redesign** (ya en origin/main via PR #24)
2. **Profile / Preferences / Quick Setup** (ya en origin/main via PR #25)
3. **Smart Match 304 hardening** (hardening de cache 304, retry, offline)
4. **Navigation + real map** (feature/navigation-map-ui, MapLibre GL Native)
5. **Smart Match NV-23 v1.2** (contrato v1.2, matchScore autoridad, breakdown, excludedBy, profile incomplete 422, dismiss/restore, pagination, 304 cache handling, cache miss single retry, local ML fallback offline/503, recuperación a resultado remoto)
6. **Local ML fallback/certification** (Modelo LR, certificación on-device)

> **Nota:** Si alguna rama ya está contenida en `origin/main`, NO volver a mergearla. Preservar historia y evitar duplicar commits. Preferir merge normal, NO rebase destructivo.

## 5. Arquitectura y Consecuentes

- La UI no consume Retrofit, Room, DataStore, Firebase ni Maps directamente
- Los Composables no contienen lógica de negocio
- Los ViewModels no dependen de Android UI
- DI: Manual Dependency Injection (aprobado)
- Composition Root: `AppContainer` + `TheHouseApplication`
- Ningún secreto se versiona
- COST-001: costo $0

## 6. Branches a Considerar

Las siguientes branches tienen trabajo terminado y deben ser consideradas para integración hacia main:

| Branch | Estado | Comentario |
|---|---|---|
| feature/ui-redesign | ✅ EN origin/main | Mergeado via PR #24 |
| feature/profile-preferences-ui | ✅ EN origin/main | Mergeado via PR #25 |
| fix/smart-match-load | ✅ EN fix/smart-match-load | Branch actual (HEAD) |
| feature/navigation-map-ui | ✅ EN origin/main | origin/HEAD |
| feature/smart-match-ml | ✅ EN origin/main | Algunos commits |
| feature/real-multimedia | ✅ EN origin/main | |
| hotfix/release-crash-1.1.1 | ✅ EN origin/main | |
| feature/u2-auth-onboarding | ✅ EN origin/main | |
| feature/sdd-mobile-complete | ✅ EN origin/main | |

## 7. Trazabilidad

- Spec creada: `specs/FINAL-MOBILE-INTEGRATION/spec.md`
- Plan en: `specs/FINAL-MOBILE-INTEGRATION/plan.md` (por crear si es necesario)
- Tasks: completar según branches integradas
- Archivos modificados: registrarse durante la integración
- Gates: `compileDebugKotlin`, `testDebugUnitTest`, `lintDebug`, `assembleDebug`
- Evidencia: reportes de build, test, lint y conflictos resueltos

## 8. Criterios de aceptación globales

- Build exitoso (`compileDebugKotlin`)
- `assembleDebug` sin errores
- Lint sin errores nuevos
- No se implementan funcionalidades nuevas
- No se tocan archivos de Backend
- No se borra `app/google-services.json`
- No force push, no reset --hard, no git clean
- Google Services presente y trackear
- Smart Match functional runtime verificado
- Navigation/Map runtime verificado
- Profile/Preferences runtime verificado

## 9. Evidencia esperada

```powershell
.\gradlew.bat clean assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

Además:
- Archivos modificados durante integración
- Mapa de paquetes y dependencias después de merge
- Resultados reales de unit tests (total/pass/fail)
- Resultados de lint
- Conflictos resueltos y línea de resolución
- Estado de `google-services.json`
- Reportes runtime (Smart Match, Navigation, Profile)

## 10. Riesgos

- Merge conflictos no resueltos que rompan el build
- Incompatibilidad de imports o APIs entre branches
- Dependencias conflictivas en build.gradle.kts
- Perder historial relevante al fast-forward
- Romper credenciales o configuración sensible

## 11. Estado Actual

```text
SPEC ACTIVA: FINAL-MOBILE-INTEGRATION
PLAN ACTIVO: F1 - Integración completada (main -> origin/main)
TASK ACTIVO: Integración final-mobile completada
ESTADO AUTORIZADO: APPROVED FOR IMPLEMENTATION - Integración completada
```