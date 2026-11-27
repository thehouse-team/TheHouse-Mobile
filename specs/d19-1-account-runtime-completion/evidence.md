# EVIDENCIA D19.1 — Account Runtime Completion

| Campo | Valor |
|---|---|
| Spec | D19.1-Account-Runtime-Completion |
| Branch | `fix/d19-runtime-completion` |
| Base | `origin/main` @ `50febcdbe47ff80b08f14323734e6883e4249858` |
| Fecha | 2026-11-27 |

## Gates ejecutados

(Pendiente de ejecutar; se registrarán resultados reales, no estimados.)

## Hallazgos de diagnóstico

(Pendiente; cada pendiente se clasificará con causa verificada o `EXTERNAL_BLOCKER`/`RUNTIME_PENDING`.)

## Pruebas de regresión

(Pendiente; se listarán clases y causas cubiertas.)

## Resultado

`D19.1-ACCOUNT-RUNTIME-COMPLETION-CODE-READY` — pendiente de verificación.

## RESULTADOS REALES (2026-11-27)

### Causas verificadas (no especulativas)

- **PERFIL (root cause):** `ProfileResponse.UpdatedAt` del Backend es `DateTimeOffset?` (null o ausente), pero `ProfileDto.updatedAt` era `String` no nullable sin `@Serializable`. Toda respuesta 200 con `updatedAt: null` lanzaba `SerializationException` ANTES del mapper → UI con error genérico. Evidencia: `D191ContractDeserializationTest` falló 5/5 antes del fix (`SerializationException` / `MissingFieldException`), pasa 5/5 después. Fix: DTO espejo con `updatedAt: String? = null`, `quickSetupCompleted = false` y `@Serializable`; `UniversityDto` con defaults. Instrumentación `ProfileFlow` DEBUG segura conectada al ViewModel.
- **PREFERENCIAS (root cause):** `PreferencesDto` declaraba un campo `currency` a nivel raíz que NO existe en `PreferencesResponse` del Backend (la moneda vive en `budget.currency`). Los tests D14 anteriores inventaban `"currency":"MXN"` y enmascaraban el fallo. Toda respuesta 200 real lanzaba `MissingFieldException` → error visual pese al HTTP 200. Fix: eliminar el campo raíz, defaults en todos los opcionales y `currency = budget?.currency ?: "MXN"`. Evidence: mismo test de contrato real.
- **REGISTRO → PRIVACIDAD (root cause):** `LegalInfo.privacyStatus` afirmaba falsamente "El aviso de privacidad está disponible dentro de la aplicación" cuando `privacyUrl` era null. Fix: mensaje honesto "El aviso de privacidad aún no ha sido publicado." La navegación ya estaba cableada (Welcome → registro → ruta pública `AppRoutes.privacy`), sin sesión, con `launchSingleTop`. Registro envía exclusivamente email/password (verificado). Sin contenido legal inventado.
- **SMART MATCH:** sin bug propio encontrado. El flujo 422 ya conserva `missing` y la UI ya expone `missingFields`/`missingUniversity`; la causa del pendiente era la deserialización de Preferencias/Perfil. D16 y D18 preservados sin cambios.
- **ARRENDADOR:** sin bug nuevo. `U2ViewModelFactory.create()` sigue soportando `LandlordProfileViewModel` tras el merge; 404 → `notLandlord=true` → activación; `landlord_required` con mensaje propio. "Introducir código" es solo agrupación visual (`Section.Landlord`), sin gating de rol: no exige perfil landlord. Sin cambios especulativos.

### Gates locales

| Comando | Resultado |
|---|---|
| `clean compileDebugKotlin --offline` | BUILD SUCCESSFUL |
| `testDebugUnitTest --offline` | BUILD SUCCESSFUL — **524 tests / 0 failures / 0 errors** (baseline 514 + 10 nuevos) |
| `lintDebug --offline` | BUILD SUCCESSFUL (0 errores) |
| `assembleDebug --offline` | BUILD SUCCESSFUL |
| `git diff --check` | exit 0 |

### Tests nuevos

- `D191ContractDeserializationTest` (5): contrato real Preferencias sin `currency` raíz; defaults; Perfil `updatedAt` null/ausente; sin `SerializationException`.
- `D191RuntimeCompletionTest` (5): Legal null/HTTPS, drawer privacy guest+auth, sin rutas protegidas en guest, factory LandlordProfileViewModel.
- `D19AccountUxCoreTest` test 21 ajustado al mensaje legal honesto (no eliminado).

### Estado runtime

- `RUNTIME_PENDING` para Perfil/Preferencias/Smart Match/Arrendador: no hay credenciales de prueba autorizadas para verificar HTTP real en dispositivo. Los seis comportamientos aprobados no se modificaron.
- `EXTERNAL_BLOCKER`: `privacyUrl` depende de `GET /api/v1/legal` del Backend; sin URL publicada se muestra el mensaje honesto. No se inventó contenido ni URL.
