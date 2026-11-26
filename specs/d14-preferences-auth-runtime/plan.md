# PLAN D14 — Preferences Auth Runtime

| Campo | Valor |
|---|---|
| Spec | D14-Preferences-Auth-Runtime |
| Branch | `fix/preferences-auth-runtime` |
| PR | Separado, contra `main`. Sin merge automático. |

## Fases

### F1 — Auditoría y contrato (EJECUTADA en modo solo lectura)
Confirmar endpoint real, DTOs, status codes y campos requeridos por Smart Match contra el backend productivo. Resultado registrado en `spec.md` §3 y `evidence.md`.

### F2 — Instrumentación segura y mapeo de errores
- D14-T02: `PreferencesFlowLogger` DEBUG e instrumentación en `PreferencesViewModel`/`PreferencesScreen`.
- D14-T03: mapear `HttpException` a `DomainException` en `PreferenceRepositoryImpl`; caché solo para indisponibilidad; usar `Throwable.toSafeUiError()`.

**Gate humano:** revisar logs locales y tests antes de F3.

### F3 — Contrato de escritura y campos requeridos
- D14-T04: traducir UI ↔ códigos contractuales (`pets` bool, catálogos, budget entero MXN).
- D14-T05: mostrar/gestionar `missing` del 422; orientar `university` a Perfil/Quick Setup.
- D14-T06: guardado sin GET posterior, con actualización de estado y caché.

**Gate humano:** decisión ya dada en spec §4 (pets/catálogos/budget/university). No requiere nueva decisión salvo imprevisto.

### F4 — Tests, gates, PR y evidencia
- D14-T07: tests de VM, repository (MockWebServer), DTO/mapper y navegación.
- D14-T08: gates completos + evidencia + PR separado. Runtime en dispositivo como condición de merge.

## Orden y dependencias

```text
D14-T01 (documentación) → D14-T02 → D14-T03 → D14-T04 → D14-T05 → D14-T06 → D14-T07 → D14-T08
```

`D14-T04` depende de `T01` y `T03`; `D14-T05` depende de `T04`; `D14-T06` depende de `T03` y `T04`; `D14-T07` depende de `T03..T06`.

## Gates

```powershell
.\gradlew.bat clean compileDebugKotlin
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
git diff --check
```

CI remoto obligatorio verde. No reducir tests existentes.

## Condición de merge

Evidencia runtime (GET 200 / PUT 200 / recommendations 200 o `missing` documentado) y aprobación humana posterior. `BLOCKED` nunca equivale a `DONE`.
