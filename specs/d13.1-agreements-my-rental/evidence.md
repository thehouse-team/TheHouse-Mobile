# Evidencia D13.1 — Acuerdos de renta + Mi renta

> **Estado: DOCUMENTACIÓN ÚNICAMENTE. Ninguna task de implementación ejecutada.**
> Regla: `BLOCKED` ≠ `DONE`. Este archivo solo registra hechos reales observados.

---

## E1 — Precondición de rama (T01) → **BLOCKED**

Comandos ejecutados (sesión 2026-11-13, repo `C:\THEHOUSE\Mobile`):

```powershell
git status
# On branch feature/reports-verification-moderation ; nothing to commit, working tree clean

git switch main
# Switched to branch 'main'

git pull origin main
# Already up to date.

git log --oneline -5
# 6ce90c1 Merge pull request #15 from thehouse-team/feature/landlord-buildings-rooms
# f458e8b Merge branch 'main' into feature/landlord-buildings-rooms
# 3859b82 Merge pull request #14 from thehouse-team/feature/requests-real-chat
# aac595e Merge branch 'main' into feature/requests-real-chat
# 1f20788 Merge pull request #13 from thehouse-team/feature/profile-preferences-ui-polish
```

```powershell
gh pr view 16 --json mergeable,mergeStateStatus,state,baseRefName,headRefName,title
# {"base":"main","head":"feature/reports-verification-moderation",
#  "mergeable":"MERGEABLE","mergeStateStatus":"BLOCKED","state":"OPEN",
#  "title":"D13: reportes, verificación del dueño y suspensión de cuenta (NV-21)"}

gh pr checks 16
# Android quality gates   pass   6m39s
# Android quality gates   pass   4m40s
```

**Resultado: FALLA.**

| Verificación | Esperado | Real |
| --- | --- | --- |
| `main` contiene D13 (`443603e`) | sí | **no** (`main = 6ce90c1`) |
| PR #16 fusionado | sí | **OPEN / BLOCKED** (CI SUCCESS, `MERGEABLE`) |
| `specs/d13-reports-verification-moderation/` en `main` | presente | **ausente** (se eliminó al hacer checkout a `main`) |

**Acción:** no se fusiona, no se empuja, no se crea la rama → escalado a decisión humana.
Decisión humana recibida: *«Esperar — no fusionar nada»* + *«Crear specs/d13.1 completa»*.

---

## E2 — Verificación de contrato (no inventado)

Fuente: `gh api` sobre `thehouse-team/TheHouse-Backend@main` (público).

| Archivo leído | Qué confirma |
| --- | --- |
| `TheHouse.Application/Contracts/Rentals/RentalContracts.cs` | Bodies y respuestas exactos de las 9 rutas; `AgreementTermsResponse` incluye `endDate` y `currency`; `RentalAddress` = `street,number,neighborhood,city,state,postalCode` |
| `TheHouse/Http/RentalsEndpoint.cs` | Las 9 rutas, códigos 201/200/400/401/403/404/409/410/413/415/429/503 y rate limits (60/min, 10/min, 30/min IP, 5 intentos/15 min) |
| `TheHouse.Application/UseCases/RentalServices.cs` | Validaciones (rent 500–100000, deposit ≤2×rent, fecha −30/+90 d, 1–24 meses, día 1–28, note ≤300), `NormalizeCode` (8 chars, acepta `-`/espacio/minúsculas), `NormalizeQrToken` (43 chars) |
| `TheHouse.Domain/Entities/Rental.cs` | Estados `Pending/Active/Ended/Cancelled`; `ClosingCode.Lifetime = 10 min`; `MaximumFailedAttempts = 5`; `EnsureCodeUsable` → 404 `code_invalid` / 410 `code_used` / 410 `code_expired` |
| `TheHouse/Http/UserEndpointSupport.cs` | Mapeo excepción→HTTP: `NotFoundException`→404+`code`, `GoneException`→410+`reason`, `ConflictException`→409+`reason`, `RateLimitExceededException`→429+`Retry-After` |
| `TheHouse/Http/UserResourceProblems.cs` | Textos reales de `code`: `agreement_open`, `agreement_state`, `code_invalid`, `code_expired`, `code_used`, `too_many_attempts`, `no_active_rental`, `request_state`, `room_unavailable`, `landlord_required` |
| `TheHouse.Application/Common/UserRateLimitOptions.cs` | `RentalsPerWindow=60`, `ClosingCodePerWindow=10`, `ClosingCodeIpPerWindow=30`, `ClosingCodeAttempts=5`, ventana 15 min, `RetryAfter=900` |
| `TheHouse.Application/Common/PropertyQueryValidation.cs` | `page ≥ 1`, `pageSize 1–50`, default `1/20` |

**Corrección de supuesto de la orden:** el Backend **no** exige `note` cuando
`reason=other` en `end` (`EndAgreementRequest` ambas opcionales, `EndAsync` solo valida
longitud ≤300). Adoptada la regla real; ver `spec.md` §1.4 y `task.md` `END-NOTE-RULE`.

**Forma de `terms` confirmada** (no inferida): `monthlyRent, deposit, startDate,
durationMonths, endDate, paymentDay, currency`.

---

## E3 — Verificación de infraestructura QR (T/§5)

```powershell
Get-Content app\build.gradle.kts   # bloque dependencies
```

Ausentes: `zxing`, `camera`, `camera2`, `mlkit`, cualquier librería de escaneo.
Existentes: Retrofit, OkHttp, kotlinx.serialization, Room, DataStore, Coil, WorkManager,
Compose BOM, Navigation Compose. **Sin dependencias nuevas aprobadas** → ingreso manual
del `backupCode`; escaneo QR queda como pendiente `QR-SCAN-PENDING`.

---

## E4 — Verificación de puntos de integración

| Punto | Evidencia |
| --- | --- |
| Patrón Api→DTO→mapper→repo→use case/VM→Compose | `RequestsApi.kt`, `RequestDtos.kt`, `RequestMappers.kt`, `RequestsRepositoryImpl.kt`, `RequestsUseCases.kt`, `RequestDetailScreen.kt` |
| Un único Retrofit / DI manual | `AppContainer.kt:123-129` (`retrofit` lazy único) + `U2ViewModelFactory.kt` |
| Error pattern | `RequestError.kt` (`RequestErrorCode` + `RequestException`), `RequestErrorMessages.kt`, `ProblemDetailsDto.code` (`VisitDtos.kt:48-54`) |
| Estados Loading/Empty/Error | `StateViews.kt` (D11) |
| Chat reutilizable | `ChatRoutes.kt` → `ConversationRoute(conversationId)` |
| Entrada arrendador desde solicitud aceptada | `LandlordRequestDetailScreen.kt:149` (solo `RequestStatus.PENDING` hoy) |
| Entradas Home existentes | `HomeScreen.kt:246-271` («Mis solicitudes», «Conversaciones», «Solicitudes recibidas (arrendador)») |
| `bottomTabs` | `AppRoutes.kt:74` → `home, explore, smartMatch, favorites` (no se modifica) |

---

## E5 — Implementación

**NO EJECUTADA.** Cero archivos de código modificados o creados.
`git status` tras la sesión de documentación: sin cambios en `app/`.

| Comando | Resultado |
| --- | --- |
| `.\gradlew.bat compileDebugKotlin` | NO EJECUTADO (sin implementación) |
| `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` | NO EJECUTADO |
| Runtime (emulador/ADB + credenciales) | NO EJECUTADO |

---

## E6 — Artefactos de documentación creados

| Archivo | Estado |
| --- | --- |
| `specs/d13.1-agreements-my-rental/spec.md` | CREADO (v1.0.0) |
| `specs/d13.1-agreements-my-rental/plan.md` | CREADO |
| `specs/d13.1-agreements-my-rental/task.md` | CREADO (27 tasks: 1 BLOCKED, 26 PENDING, 0 DONE) |
| `specs/d13.1-agreements-my-rental/checklist.md` | CREADO (0 ítems marcados) |
| `specs/d13.1-agreements-my-rental/evidence.md` | ESTE ARCHIVO |

> Nota verificada: `.gitignore` termina en `*.log`; **`specs/` no está ignorado**
> (`git check-ignore` → exit 1; `git status --porcelain` muestra `?? specs/d13.1-agreements-my-rental/`).
> Los archivos son trackeables con `git add` normal. No se ha hecho ningún `add`/`commit`.

---

## E7 — Bloqueos abiertos

| ID | Bloqueo | Requiere |
| --- | --- | --- |
| `BLOCKED-PR-16` | `main` sin D13 → sin rama `feature/agreements-my-rental` | Decisión humana: fusionar PR #16 o indicar otra base |
| `BLOCKED-AUTH` | Sin autorización de ejecución (`EJECUTA D13.1`) | Autorización humana explícita |
| `QR-SCAN-PENDING` | Sin infraestructura de cámara/QR aprobada | Spec futura + aprobación de dependencia/permiso |
| `MAP-PENDING` | `location{lat,lng}` sin UI de mapa existente | Spec de mapa (NV-07) |
| `RUNTIME-PENDING` | Sin emulador/ADB ni credenciales/solicitud de prueba | Entorno de runtime autorizado |
