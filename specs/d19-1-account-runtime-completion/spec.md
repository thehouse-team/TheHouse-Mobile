# SPEC D19.1 — Account Runtime Completion

| Campo | Valor |
|---|---|
| Spec | D19.1-Account-Runtime-Completion |
| Versión | 1.0.0 |
| Estado | AUTORIZADA / EN EJECUCIÓN |
| Autorización | Humana explícita (instrucción individual 2026-11-27, opción (a)) |
| Branch | `fix/d19-runtime-completion` |
| Base | `origin/main` @ `50febcdbe47ff80b08f14323734e6883e4249858` |
| Relación | Complemento de D19 (Account + UX Core), D16 (Smart Match) y D18 (Room Detail). No crea spec NV nueva. |
| PR | Nuevo PR hacia `main`. NO mergear. NO modificar PR #34 (MERGED). |

## 1. Problema

D19 quedó integrado en `main` mediante PR #34 (`50febcd`) con 514 tests verdes y CI remoto exitoso, pero la certificación runtime humana dejó cinco pendientes:

1. **Mi perfil** — errores genéricos reportados por el usuario.
2. **Preferencias** — problemas visuales incluso con HTTP 200 histórico.
3. **Smart Match con perfil completo** — dependencia de Perfil + Preferencias y del campo `missing` del 422.
4. **Aviso de privacidad desde registro** — flujo Welcome → Crear cuenta → Aviso de privacidad.
5. **Sección Arrendador** — após del fix de `LandlordProfileViewModel`, verificar 404/200 y `landlord_required`.

Seis comportamientos ya aprobados no deben cambiar: arranque sin crash, hamburger superior derecho alineado con TheHouse, header sin duplicados, aviso de privacidad en drawer autenticado, preview/confirmación de eliminación y logout.

## 2. Principios

- No asumir que los cinco pendientes son cinco errores confirmados: reproducir y clasificar cada uno desde código, tests y evidencia disponible.
- No hacer cambios especulativos. Corregir solo causas verificadas.
- NO modificar Backend, GenAI ni infraestructura. NO inventar endpoints, campos, roles JWT ni textos legales.
- Instrumentación DEBUG segura: solo operación, status HTTP, tipo de excepción, resultado de deserialización, estado UI y request ID. Nunca tokens, contraseñas, bodies completos ni PII.
- Distinguir `CODE_READY`, `TESTED`, `RUNTIME_PASS`, `RUNTIME_FAILED`, `RUNTIME_PENDING` y `EXTERNAL_BLOCKER`.

## 3. Requisitos

### R1 — Perfil
Cadena completa HTTP → deserialización → DTO → mapper → repository → ViewModel → estado UI. Campos nullable, universidad como objeto nullable, fechas `DateTimeOffset`, `@Serializable`, campos desconocidos, caché, sesión, Bearer/refresh y excepciones. Aceptación: 200 con sesión válida, nombre y universidad editables, PATCH 200, PUT university 204, persistencia al regresar y 401 definitivo → autenticación.

### R2 — Preferencias
Diagnosticar error visual con HTTP 200. Revisar DTO/serializers, `budget` nullable, `pets` Boolean nullable, `maxDistanceKm`, opcionales, defaults, listas vacías, mapper dominio/UI, Loading/Content/Error, eventos de guardado, caché y restauración. PUT exacto sin campos extra. Aceptación: GET 200 → formulario editable; PUT 200 → confirmación; persistencia; sin error genérico tras 200.

### R3 — Smart Match
Usar `university`, `budget.max`, `livingWith`, `placeType`. Interpretar `missing` del 422 `smart_match_profile_incomplete` y mostrar qué falta, permitiendo completarlo con Perfil/Preferencias/Quick Setup. No inventar datos. Preservar D16 (autoridad Backend online, matchScore Int, breakdown Double, guest sin Authorization y sin GET de preferencias, fallback etiquetado, reconexión remota) y D18 (roomId → RoomDetail).

### R4 — Registro → privacidad
Welcome → Crear cuenta → Aviso de privacidad → regresar al registro. Sin sesión. `GET /api/v1/legal` con versiones y URLs vigentes. Si `privacyUrl` es null y no hay contenido legal aprobado, mostrar “El aviso de privacidad aún no ha sido publicado.” Registro envía exclusivamente email y password. Aceptación legal solo con consentimiento explícito vía `PUT /users/me/legal`.

### R5 — Arrendador
`U2ViewModelFactory.create()` debe seguir soportando `LandlordProfileViewModel`. `Mis alojamientos` sin crash. 404 → activación de perfil; 200 → dashboard real. `landlord_required` en rutas exclusivas → estado adecuado o alta de perfil. “Introducir código” no exige perfil landlord. Sin roles JWT inventados ni perfiles reales creados automáticamente.

### R6 — Regresiones prohibidas
Conservar los seis comportamientos aprobados y las pruebas existentes. No eliminar tests para conseguir gates verdes.

## 4. Fuera de alcance

Home/Explore, FCM, ONNX/ML local salvo el fallback D16 ya existente, androidTest general, Backend, GenAI, infraestructura, merge a `main`, PR B/C y modificación de PR #34.

## 5. Resultado esperado

`D19.1-ACCOUNT-RUNTIME-COMPLETION-CODE-READY` con evidencia real de gates y pruebas de regresión. `RUNTIME-CERTIFIED` solo si las pruebas runtime requeridas pasan realmente en dispositivo con credenciales autorizadas.
