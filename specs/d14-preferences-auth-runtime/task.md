# TASKS D14 — Preferences Auth Runtime

| ID | Descripción | Dependencias | Estado |
|---|---|---|---|
| D14-T01 | Documentar contrato Backend confirmado, root cause y decisiones humanas en spec/evidence | Ninguna | [x] |
| D14-T02 | Agregar `PreferencesFlowLogger` DEBUG (sin PII/tokens/bodies) e instrumentar VM/Screen | T01 | [x] |
| D14-T03 | Mapear `HttpException` a `DomainException` en `PreferenceRepositoryImpl`; caché solo para IOException/timeout; `@Serializable` en DTOs | T02 | [x] |
| D14-T04 | Traducir UI ↔ códigos backend: `pets` bool, catálogos cerrados, budget entero MXN con validación 0..50000 y min<=max | T01, T03 | [x] |
| D14-T05 | Gestionar `missing` del 422 (`budget.max`, `livingWith`, `placeType` en Preferences; `university` → Perfil/Quick Setup) | T04 | [x] |
| D14-T06 | Guardado sin GET posterior: PUT 200 → estado local + caché + mensaje de éxito | T03, T04 | [x] |
| D14-T07 | Tests: GET 200→Content, sin preferencias→editable, 401→RequiresAuth, network/429/5xx→Error tipado, guardado exitoso, guardado inválido, required fields preservados, caché no oculta 4xx, mapper DTO | T03–T06 | [x] |
| D14-T08 | Gates completos, evidencia real, PR separado contra `main` (sin merge) | T07 | [x] |

## Notas

- `D14-T01` se considera ejecutada por la auditoría de solo lectura aprobada y registrada en `spec.md` §2/§3 y `evidence.md`.
- No se ejecuta ninguna task sin declaración de autorización vigente; la autorización humana individual está registrada en `memoria.md` (M-MOB-058).
