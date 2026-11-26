# SPEC D14 — Preferences Auth Runtime

| Campo | Valor |
|---|---|
| Spec | D14-Preferences-Auth-Runtime |
| Versión | 1.0.0 |
| Estado | AUTORIZADA / EN EJECUCIÓN |
| Autorización | Humana explícita (instrucción individual 2026-11-26) |
| Branch | `fix/preferences-auth-runtime` |
| Base | `main` @ `2626e7cf7803c4c869d636fb5715c3d834d0f256` |
| Relación NV | D11 (Profile/Preferences/Quick Setup) y NV-23 (Smart Match AUTH). No crea spec NV nueva. |

## 1. Problema

La pantalla Preferencias ("Personaliza tu búsqueda") muestra:

- "No pudimos cargar tus preferencias"
- "Ocurrió un error inesperado. Intenta de nuevo."

Eso impide completar el perfil requerido por `GET /api/v1/smart-match/recommendations`, que responde 422 `smart_match_profile_incomplete` cuando faltan campos. La evidencia runtime aportada confirma que el 422 es contractual y que el bloqueo real son los datos incompletos, no Smart Match.

## 2. Root cause (auditoría D14, solo lectura)

1. **Data no mapea HTTP a Domain:** `PreferenceRepositoryImpl.loadPreferences()` relanza `HttpException`/`SerializationException` crudos (`data/repository/PreferenceRepository.kt:40-47`). Presentation ya usa correctamente `Throwable.toSafeUiError()` (`presentation/common/ThrowableToUiError.kt:14-19`); el bug `DomainException → Unknown` planteado NO existe en el código actual. El mensaje genérico nace en Data, no en el mapper central.
2. **Caché disfraza estados:** `isCacheFallbackEligible` acepta cualquier `HttpException` (`PreferenceRepository.kt:72-73`), incluyendo 401/400/404/422/429.
3. **Contrato de escritura roto:** `PreferencesPatchBody` envía `pets: String?`, catálogos en lenguaje natural y `budget.min/max: Double?`, mientras el backend exige `pets: bool?`, códigos cerrados e `int?`. El PUT responde 400 y `savePreferences()` encadena un GET (`PreferenceRepository.kt:69`) que propaga el fallo.

## 3. Contrato Backend confirmado

Fuente: código productivo de `thehouse-team/TheHouse-Backend` `origin/main` @ `3d7bd478d675f878735799aaa383cb0e97f83d6d` (`ProfileEndpoint.cs`, `ProfileContracts.cs`, `PreferencesService.cs`, `SmartMatchModel.cs`, `UserEndpointSupport.cs`). No se asumió ningún contrato.

- `GET /api/v1/users/me/preferences` → **200** con valores por omisión cuando no hay preferencias: `budget={min:null,max:null,currency:"MXN"}`, `maxDistanceKm=null`, listas vacías, `quickSetupCompleted=false`. **No devuelve 404 por ausencia**. Status: 200, 401, 429, 503, 500.
- `PUT /api/v1/users/me/preferences` → reemplazo; request `PreferencesRequest` con `JsonUnmappedMemberHandling.Disallow`: `Budget{Min:int?,Max:int?,Currency:string?}`, `MaxDistanceKm:decimal?`, `EssentialServices:list<string>?`, `LivingWith`, `PlaceType`, `Pets:bool?`, `Transport`, `PropertyTypes`. **No acepta** `quickSetupCompleted`, `version`, `updatedAt`. Responde **200 con `PreferencesResponse`**. Status: 200, 400, 401, 413, 415, 429, 503, 500. **No 404, no 422 en preferences**.
- Validación: solo `MXN`; budget `0..50000` y `min <= max`; `maxDistanceKm 0.5..30` (1 decimal). Catálogos cerrados:
  - `essentialServices`: `wifi, water, electricity, gas, hot_water, laundry, kitchen, furnished, parking, security, cleaning`
  - `livingWith`: `women, men, any`
  - `placeType`: `room, room_private_bath, studio, shared`
  - `transport`: `walk, bike, public, car`
  - `propertyTypes`: `house, apartment, student_residence`
- Smart Match requeridos (`SmartMatchRules.Resolve`): `university`, `budget.max`, `livingWith`, `placeType` → falta cualquiera = 422 `smart_match_profile_incomplete` con `missing=[...]`. Resto opcional con defaults.

## 4. Decisiones humanas vigentes

1. **Pets:** backend `bool?`. UI muestra "Sí"/"No" y mapea `Sí→true`, `No→false`, sin tercer valor; `null` solo si no hay selección.
2. **Catálogos:** la UI mantiene etiquetas humanas pero envía códigos contractuales (`Internet→wifi`, `Agua→water`, `Luz→electricity`, etc.). No inventar códigos.
3. **Budget:** enteros MXN. No redondear decimales silenciosamente: si el usuario introdujo decimal, validar y pedir entero antes del PUT. Rangos 0..50000, `min <= max`.
4. **University:** NO se edita en Preferences. Fuente = Perfil / Configuración inicial. Si `missing=["university"]`, orientar/navegar a Perfil o Quick Setup. Preferences se responsabiliza de `budget.max`, `livingWith`, `placeType` y el resto de preferencias.
5. **GET:** 200 con defaults → formulario editable, nunca error. 401 → RequiresAuth. 429/503/5xx/timeout → error tipado. Caché **nunca** esconde 401/403/400/404/422/429; fallback solo para indisponibilidad (IOException/timeout) compatible con la arquitectura existente.
6. **PUT:** contrato exacto. El guardado NO depende de un GET posterior. Tras 200: actualizar estado local + caché + mensaje de éxito; posible regreso a Smart Match.
7. **Runtime cierre:** evidencia de `GET -> 200`, `PUT -> 200`, y después `GET recommendations -> 200` o 422 con `missing` restante documentado. Objetivo `missing=[]` para datos gestionables.

## 5. Alcance

- Logging DEBUG seguro `PreferencesFlow` (`enter_screen`, `load_start`, `request_start`, `request_result=<success|not_found|unauthorized|error>`, `state=<Loading|Content|Empty|RequiresAuth|Error>`, `exception=<SimpleName> operation=<operation>`), sin PII/tokens/bodies/stacktraces. `TheHouseHttp` ya registra endpoint/status.
- Mapeo HTTP→Domain en Data y uso exclusivo de `Throwable.toSafeUiError()` en Presentation.
- Traducción UI↔códigos contractuales y validación de budget entero.
- Manejo de `missing` del 422 con orientación a Perfil/Quick Setup para `university`.
- Guardado sin GET posterior, con actualización de estado y caché.
- Tests unitarios/instrumentados y documentación SDD.

## 6. Fuera de alcance

- No modificar Smart Match, ML/IA, Backend, NV-15/NV-21.
- No modificar `MainActivity.kt` ni navegación global (pertenece a D15).
- No inventar roles, secretos, endpoints ni campos.
- No mergear el PR de D14 sin evidencia runtime y aprobación humana posterior.
- No mergear PR #29 (requiere review humano APPROVED).

## 7. Estados UI

`Loading | Content | Empty(editable) | RequiresAuth | Error(tipado)`, más `isSaving`, `saveError`, `message` en `PreferencesUiState`. Ningún estado contractual conocido debe caer en `UiError.Unknown`.

## 8. Criterios de aceptación

1. GET 200 con defaults → `Content`/formulario editable; ausencia de preferencias nunca es error.
2. 401 → `RequiresAuth` sin usar caché.
3. 400/404/422/429/503/5xx/timeout → errores tipados, nunca genérico.
4. `pets`, catálogos y budget se envían con tipos/códigos exactos del backend.
5. Decimales en budget → validación antes del PUT, sin redondeo silencioso.
6. PUT 200 → estado y caché actualizados, mensaje de guardado, sin GET posterior innecesario.
7. `missing=["university"]` → orientación a Perfil/Configuración inicial, sin duplicar university en Preferences.
8. Logs `PreferencesFlow` presentes en DEBUG, sin datos sensibles.
9. Tests nuevos verdes sin reducir los existentes.

## 9. Riesgos

- Migración de `PreferencesSnapshot.pets: String?` a bool en caché (reconstruible desde backend).
- Catálogos: las etiquetas actuales pueden no mapear 1:1; se mapean solo códigos confirmados.
- Sin ADB/dispositivo en la sesión: runtime queda pendiente; `BLOCKED` nunca equivale a `DONE`.

## 10. Evidencia esperada

Spec/plan/task/checklist/evidence actualizados; gates locales; CI remoto verde; PR separado; captura runtime (`PreferencesFlow`, `TheHouseHttp`, `SmartMatchFlow`).

## 11. Rollback

Revert normal del PR (sin force push). Tipos y snapshots vuelven al estado anterior; la caché se reconstruye desde el backend; ninguna migración irreversible de datos de usuario.
