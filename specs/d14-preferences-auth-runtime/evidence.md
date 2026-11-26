# EVIDENCIA D14 — Preferences Auth Runtime

| Campo | Valor |
|---|---|
| Branch | `fix/preferences-auth-runtime` |
| Base | `main` @ `2626e7cf7803c4c869d636fb5715c3d834d0f256` |
| Autorización | Humana individual (2026-11-26), registrada en `memoria.md` M-MOB-058 |
| Estado | IMPLEMENTADO / TESTED / EVIDENCIADO — **RUNTIME PENDIENTE** |

## 1. Hallazgo crítico añadido durante la implementación

**ROOT CAUSE REAL (confirmado por tests de integración):** `PreferencesDto`, `BudgetDto`, `PreferencesPatchBody` y `BudgetPatchBody` **no estaban anotados con `@Serializable`**. Retrofit + kotlinx.serialization lanzaban `SerializationException` en **toda** respuesta, que el repository relanzaba crudo y Presentation convertía en `UiError.Unknown` → "Ocurrió un error inesperado. Intenta de nuevo."

Esto explica por qué la pantalla fallaba **incluso con sesión válida y backend sano**. Matiza la auditoría previa: el error mapping central era correcto; el fallo estaba en la serialización del DTO (Data) y en la ausencia de conversión HTTP→Domain.

Evidencia: los 9 tests nuevos de `PreferencesRepositoryIntegrationTest` fallaban con `DomainException(Unknown)` antes de añadir `@Serializable`; tras añadirlo, los 9 pasan. Ver §4.

## 2. Cambios implementados

| Task | Cambio | Archivos |
|---|---|---|
| D14-T01 | Contrato backend confirmado documentado (spec §3) | `spec.md` |
| D14-T02 | Logger `PreferencesFlow` DEBUG, sin PII/tokens/bodies; activado solo con `BuildConfig.DEBUG` | `preferences/flow/PreferencesFlowLogger.kt`, `TheHouseApplication.kt`, `PreferencesScreen.kt`, `PreferencesViewModel.kt` |
| D14-T03 | `HttpException`→`DomainException` tipado; caché solo para `IOException`/timeout; `@Serializable` en DTOs | `data/repository/PreferenceRepository.kt`, `data/remote/dto/preferences/PreferencesDto.kt`, `data/remote/api/PreferencesApi.kt`, `data/error/DataErrorMapper.kt` (sin cambios finales) |
| D14-T04 | Contrato exacto: `pets` bool, catálogos código, budget `Int` MXN 0..50000, distancia 0.5..30, moneda MXN | `PreferencesContract.kt`, `PreferencesViewModel.kt`, `PreferencesScreen.kt` |
| D14-T05 | `missing` del 422: `university` se orienta a Perfil/Quick Setup; `budget.max`/`livingWith`/`placeType` se gestionan aquí | `PreferencesViewModel.kt`, `PreferencesScreen.kt` |
| D14-T06 | PUT sin GET posterior; estado y caché desde la respuesta 200 | `PreferenceRepository.kt` |
| D14-T07 | Tests VM + Repository (MockWebServer) | `PreferencesViewModelTest.kt`, `PreferencesRepositoryIntegrationTest.kt` |
| D14-T08 | Gates y PR separado | este documento |

## 3. Decisiones humanas aplicadas

- `pets`: UI "Sí"/"No" → `true`/`false`; sin tercer valor; `null` solo sin selección.
- Catálogos: etiquetas humanas visibles, códigos contractuales enviados (`Internet→wifi`, `Agua→water`, `Luz→electricity`, `Mujeres→women`, `Cuarto→room`, `Caminar→walk`, `Casa→house`, etc.). Códigos exactos del backend, ninguno inventado.
- Budget: `Int` en MXN; decimales rechazados con error antes del PUT (sin redondeo silencioso); rango 0..50000 y `min<=max`.
- University: no se edita en Preferences; si aparece en `missing`, se muestra orientación a Perfil/Configuración inicial.
- GET 200 con defaults → formulario editable; 401 → RequiresAuth; 404/429/5xx/400/422 → errores tipados; caché nunca oculta 4xx.
- PUT 200 actualiza estado y caché sin GET posterior.

## 4. Gates reales (ejecutados en esta sesión)

```text
.\gradlew.bat compileDebugKotlin   → BUILD SUCCESSFUL
.\gradlew.bat testDebugUnitTest    → BUILD SUCCESSFUL (417 tests, 0 failures)
.\gradlew.bat lintDebug            → BUILD SUCCESSFUL
.\gradlew.bat assembleDebug        → BUILD SUCCESSFUL
git diff --check                   → exit 0
```

Nota: `git diff --check` emite solo warnings de normalización LF→CRLF en 3 archivos, sin errores.

Cobertura nueva: 7 tests VM nuevos + 9 tests de integración Repository (200 defaults, decodificación de códigos, 401, 429, 503, 404, PUT tipos/códigos exactos, PUT 200 sin GET posterior, PUT 400). Total previo 402 → 417 (+15).

## 5. Riesgos / deuda

- **RUNTIME PENDIENTE**: no hay ADB/dispositivo en la sesión. No se afirma que el backend haya devuelto 200 en el dispositivo.
- El `missing` de Smart Match no existe en `main` (está en PR #29, no mergeado). D14 expone `MissingFieldsChanged` como evento y la UI muestra orientación basada en ese dato cuando se le suministra; la conexión automática desde Smart Match se hará cuando PR #29 se mergee y se autorice su cableado. No se ha tocado Smart Match.
- Las etiquetas de catálogo se traducen solo para los códigos confirmados; un valor escrito a mano que no coincida se omite del request (no se inventa código).

## 6. Evidencia runtime requerida para cerrar

```text
PreferencesFlow enter_screen
PreferencesFlow load_start
PreferencesFlow request_start
PreferencesFlow request_result=success
PreferencesFlow state=Content
TheHouseHttp GET /api/v1/users/me/preferences -> 200
TheHouseHttp PUT /api/v1/users/me/preferences -> 200
SmartMatchFlow auth_query_start
TheHouseHttp GET /api/v1/smart-match/recommendations -> 200
```
o, si sigue 422, documentar `missing=[...]` restante. Objetivo: `missing=[]` para datos gestionables por el usuario.

## 7. Rollback

Revert normal del PR (sin force push). Los DTO vuelven a su estado anterior; la caché se reconstruye desde el backend. No hay migración irreversible de datos.
