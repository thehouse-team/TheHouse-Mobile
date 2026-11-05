# AGENTS — THEHOUSE MOBILE

## 1. Propósito

Este documento define cómo deben actuar los agentes de IA dentro de la raíz actual del repositorio TheHouse Mobile. El repositorio es un repositorio Git con remoto GitHub existente; el trabajo se realiza mediante ramas feature.

Su función es impedir implementaciones sin autorización, preservar la trazabilidad SDD y asegurar que cada cambio respete la Constitución Mobile.

## 2. Jerarquía obligatoria

```text
constitution.md
      ↓
AGENTS.md
      ↓
memoria.md
      ↓
spec activa
      ↓
plan activo
      ↓
task activo
```

Si existe conflicto, prevalece el documento de mayor jerarquía.

Una spec no puede ejecutar tareas de otra spec. Una task no puede ampliar el alcance de su plan. Un agente no puede inventar aprobaciones.

Reglas de gobernanza obligatorias:

- **Spec primero:** ninguna funcionalidad, pantalla, endpoint, entidad ni integración se implementa sin una spec aprobada previamente.
- **Autorización humana explícita:** cada ejecución de spec, plan o task requiere autorización humana confirmada antes de implementar.
- **Iteración acotada:** cada iteración de trabajo debe comprobar al menos:
  1. `constitution.md`;
  2. `AGENTS.md`;
  3. `memoria.md`;
  4. spec activa (y su plan/task);
  5. brechas OWASP MASVS aplicables al alcance.
- Corregir y repetir como máximo **2 ciclos** de coherencia/corrección; si persiste la inconsistencia, detenerse y escalar a decisión humana.

## 3. Declaración obligatoria antes de implementar

Antes de leer o modificar código con intención de implementar, y antes de ejecutar cualquier tarea de una spec, el agente debe declarar:

```text
SPEC ACTIVA: <id o NINGUNA>
PLAN ACTIVO: <id/fase o NINGUNO>
TASK ACTIVO: <id o NINGUNA>
ESTADO AUTORIZADO: <APROBADO | NO AUTORIZADO | BLOCKED>
```

Si cualquiera de estos elementos falta o no está aprobado:

```text
BLOCKED
```

El agente debe detenerse y solicitar autorización humana.

## 4. Estado actual del repositorio

- El proyecto Android existente compila.
- Las dependencias técnicas base fueron agregadas y validadas.
- La estructura SDD fue creada.
- `NV-01 Foundation`: **✅ EJECUTADA / VALIDADA** (30/30 tasks).
- `NV-03 Release / Play`: documentada para U2, **⛔ NO EJECUTAR**.
- `NV-04 Auth + Onboarding`: creada, **⛔ NO EJECUTAR**, Alcance U2.
- `NV-02 DevSecOps / CI-CD`: documentada ahora, ejecución después de NV-04, **⛔ NO EJECUTAR**.

Orden oficial de specs para U2:

| Orden | Spec | Estado |
|---|---|---|
| 1 | NV-01 Foundation | Ejecutada / validada |
| 2 | NV-03 Release / Google Play | U2 — Ejecutar tras autorización humana |
| 3 | NV-04 Auth + Onboarding | U2 — Nueva, ⛔ NO EJECUTAR |
| 4 | NV-02 DevSecOps / CI-CD | Documentar ahora, ejecutar después |
- No existe una spec funcional autorizada.
- No hay funcionalidades de negocio implementadas.
- Firebase, Maps, ML, IA generativa y Google Play no están configurados.

Por lo tanto, el estado de autorización actual para implementación funcional es:

```text
BLOCKED — sin spec funcional activa autorizada
```

La única excepción es una instrucción humana explícita limitada a gobernanza/documentación o a una task previamente aprobada.

## 5. Flujo de trabajo obligatorio

1. Leer `constitution.md`.
2. Leer `AGENTS.md`.
3. Leer `memoria.md`.
4. Identificar la spec activa.
5. Verificar plan y task aprobados.
6. Declarar el bloque de autorización.
7. Confirmar alcance permitido.
8. Implementar solo las tasks autorizadas.
9. Ejecutar las validaciones definidas.
10. Registrar evidencia y reportar resultado.
11. Si falta cualquier condición, detenerse con `BLOCKED`.

## 6. Reglas de alcance

- No crear funcionalidades de TheHouse sin spec funcional aprobada.
- No crear pantallas funcionales, APIs, entidades Room, endpoints, integraciones ni configuraciones externas sin spec.
- No crear módulos Gradle adicionales sin spec de arquitectura aprobada.
- No recrear el proyecto Android.
- No modificar `MainActivity.kt`, `ui/theme/`, `AndroidManifest.xml`, recursos, pantallas o tests existentes sin autorización explícita.
- No implementar trabajo perteneciente a otra spec.
- No agregar dependencias ajenas a la task activa.
- No ejecutar `git init`, `git add`, `git commit` ni `git push` salvo autorización explícita.
- No publicar en Google Play.
- No introducir secretos.

## 7. Reglas técnicas permanentes

- Android nativo, Kotlin, Jetpack Compose.
- MVVM, Clean Architecture y UDF.
- Coroutines y Flow.
- Navigation Compose.
- Retrofit, OkHttp y Kotlin Serialization.
- Room y DataStore cuando la spec lo requiera.
- Coil para imágenes.
- WorkManager para trabajo diferido.
- Un único módulo `:app` en la etapa inicial.
- Toolchain congelado: AGP `9.5.0`, Gradle `9.6.0`, Kotlin `2.2.10`, compileSdk `37`, targetSdk `37`, minSdk `26`, Java `17`.
- DI: Manual Dependency Injection.
  - Estado: **APROBADO**.
  - Composition Root: `AppContainer` + `TheHouseApplication`.
  - Framework externo: **NO** (sin Hilt/Koin).
- KSP pendiente hasta que exista persistencia Room real.

## 8. Reglas de arquitectura

- La UI no consume Retrofit, Room, DataStore, Firebase ni Maps directamente.
- Los Composables no contienen lógica de negocio.
- Los ViewModels no dependen de Android UI.
- Domain no depende de frameworks de infraestructura.
- Data implementa contratos de Domain.
- Las boundaries iniciales son paquetes dentro de `:app`, no módulos Gradle.
- Toda pantalla funcional futura seguirá el flujo UDF definido en la Constitución.

## 9. Reglas de seguridad

- Ningún secreto se versiona.
- Ningún token se registra en logs.
- No se agregan permisos sin justificación.
- No se agregan keys de Maps, Firebase o IA.
- No se almacenan credenciales en texto plano.
- No se configura Google Play ni signing de release sin NV-03 autorizada.
- Toda integración externa debe evaluar COST-001.

## 10. COST-001

Costo objetivo obligatorio:

```text
$0
```

Toda propuesta que use un servicio externo debe documentar:

- Free tier.
- Límites.
- Requiere tarjeta.
- Riesgo de cobro.
- Mecanismo para impedir sobrecostos.
- Alternativa gratuita.

Si no se demuestra viabilidad de costo $0, el agente debe reportar `BLOCKED` y no implementar.

## 11. Roles y coordinación

Los agentes especializados están en `.opencode/agents/`:

- `sdd-guardian`: gobernanza, autorización, alcance y trazabilidad.
- `mobile-architect`: arquitectura Android, MVVM, Clean Architecture y UDF.
- `compose-ui-agent`: calidad de Compose, UI State, accesibilidad y previews.
- `networking-data-agent`: red, serialización, repositorios, Room y DataStore.
- `offline-sync-agent`: offline-first, WorkManager, sync y conflictos.
- `security-devsecops-agent`: seguridad, permisos, secretos y cadena de suministro.
- `testing-quality-agent`: pruebas, cobertura y quality gates.
- `release-play-agent`: versionado, AAB, signing y Google Play.

Un agente especializado puede analizar y proponer, pero no autoriza su propio trabajo ni amplía alcance.

## 12. Skills

Las skills están en `.opencode/skills/` y representan procedimientos reutilizables:

- `sdd-compliance`
- `android-architecture`
- `compose-quality`
- `networking`
- `local-persistence-sync`
- `mobile-security`
- `mobile-devsecops`
- `testing`
- `release-play`

Las skills no sustituyen specs ni autorizaciones.

## 13. Validación

Antes de declarar terminada una task autorizada, el agente debe ejecutar las validaciones que correspondan y reportar resultados reales.

Base actual:

```powershell
.\gradlew.bat clean assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

No se deben ocultar fallos, ni crear pruebas o código artificial para forzar un resultado.

## 14. Evidencia y trazabilidad

Cada implementación autorizada debe dejar:

- Spec y requisitos cubiertos.
- Plan y fase.
- Tasks ejecutadas.
- Archivos modificados.
- Comandos ejecutados.
- Resultados reales.
- Riesgos o deuda pendiente.
- Actualización de `memoria.md` cuando exista una decisión relevante.

## 15. Criterios de bloqueo

El agente debe responder `BLOCKED` si:

- No existe spec aprobada.
- No existe plan aprobado.
- No existe task aprobada.
- El pedido excede el alcance.
- Se requiere un secreto no disponible de forma segura.
- Se requiere una versión incompatible del toolchain.
- Se requiere un servicio que no cumple COST-001.
- Se intenta ejecutar NV-02, NV-03 o NV-04+ sin autorización.
- Se intenta implementar funcionalidad de negocio sin spec.
- Se detecta riesgo de seguridad no aceptado.
- Se intenta publicar o firmar un release sin autorización.

## 16. Resultado esperado de gobernanza

El `sdd-guardian` debe emitir uno de estos resultados:

```text
PASS
PASS WITH WARNINGS
BLOCKED
```

Cualquier resultado distinto de `PASS` debe explicar la causa y la acción requerida.

## 17. Principio final

La velocidad no justifica romper la Constitución. Si una tarea no puede ejecutarse de forma segura, trazable y a costo $0, debe detenerse y escalarse para decisión humana.

## 18. MCP (Model Context Protocol)

Decisiones vigentes (registradas también en `memoria.md`):

- **Firebase MCP:** comando soportado `npx -y firebase-tools@latest mcp`. Debe permanecer **deshabilitado / no usado** hasta la futura spec de notificaciones (FCM). No instalar ni ejecutar sin spec autorizada.
- **GitHub MCP:** se referencia el servidor oficial `github/github-mcp-server`. No se inventan tokens; si requiere autenticación, se referencia una variable de entorno. Se prefiere modo read-only para agentes de análisis.
- Durante fases de documentación no se instalan ni ejecutan servidores MCP.
- La configuración corresponde a `opencode.json` en la raíz del repositorio; no incluye secretos.
