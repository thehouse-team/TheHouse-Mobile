# Plan D13.1 — Acuerdos de renta + Mi renta

| Campo | Valor |
| --- | --- |
| Spec | `specs/d13.1-agreements-my-rental/spec.md` v1.0.0 |
| Estado | PLANNED — **NO EJECUTAR** |
| Branch | `feature/agreements-my-rental` (NO creada: precondición `main` con D13 pendiente) |
| Base prevista | `main` **después** de fusionar PR #16 (D13) |
| Ciclo correctivo máximo | 1 |
| Validación | `compileDebugKotlin` primero; si verde → `testDebugUnitTest lintDebug assembleDebug` (set completo una vez) |

## Precondiciones (bloqueantes)

1. `git switch main && git pull origin main` y **verificar que `main` contenga D13**.
   - *Estado actual: FALLA. `main = 6ce90c1`; D13 = `443603e` solo en
     `feature/reports-verification-moderation`; PR #16 OPEN / `BLOCKED` / CI SUCCESS / `MERGEABLE`.*
   - **Acción humana requerida:** fusionar PR #16 (o indicar otra base). El agente NO fusiona ni empuja.
2. Autorización humana explícita de ejecución de D13.1 (`EJECUTA D13.1`).
3. Declaración de bloqueo de autorización antes de tocar código:

```text
SPEC ACTIVA: D13.1
PLAN ACTIVO: D13.1 F0..F6
TASK ACTIVO: <id>
ESTADO AUTORIZADO: NO AUTORIZADO  →  BLOCKED
```

## Fases

### F0 — Gobernanza y base (T01–T04)

| Task | Actividad | Salida |
| --- | --- | --- |
| T01 | Verificar `main` con D13; si no, escalar a decisión humana | Evidencia de `git log` |
| T02 | Crear `feature/agreements-my-rental` desde la base aprobada | Rama (sin tocar `main`) |
| T03 | Aprobación humana de spec + plan + task | Spec/plan/task APROBADOS |
| T04 | Declaración de autorización antes de implementar | Bloqueo declarado |

### F1 — Contrato y dominio (T05–T10)

| Task | Actividad | Archivos |
| --- | --- | --- |
| T05 | DTOs de las 9 rutas (solo campos reales de `RentalContracts.cs`) | `data/remote/dto/agreements/AgreementDtos.kt` |
| T06 | `AgreementsApi` con las 9 rutas exactas | `data/remote/api/AgreementsApi.kt` |
| T07 | Modelo de dominio `Agreement`/`Rental` + estados exactos | `domain/model/Agreement.kt` |
| T08 | `AgreementErrorCode` + `AgreementException` (estilo `RequestError`) | `domain/error/AgreementError.kt` |
| Mappers | DTO → dominio (fechas `YYYY-MM-DD`, `currency`, `endDate` derivado) | `data/agreements/mapper/AgreementMappers.kt` |
| T09 | Repository con `safeCall`, Problem Details `code`, `Retry-After`, 410 | `data/agreements/repository/AgreementsRepositoryImpl.kt`, `domain/repository/AgreementsRepository.kt` |
| T10 | Use cases (create/list/detail/code/cancel/end/preview/accept/rental) | `domain/usecase/AgreementUseCases.kt` |

**Reglas de F1:** sin segundo Retrofit; sin campos desconocidos en requests; `closingCode` solo
se modela como nullable; sin persistencia; sin logs.

### F2 — UX arrendador (T11–T14)

| Task | Actividad | Pantalla |
| --- | --- | --- |
| T11 | Formulario «Crear acuerdo» desde solicitud `ACCEPTED` (validación local §1.5, anti doble submit, muestra `backupCode`+`expiresAt` al éxito) | `CreateAgreementScreen` + VM |
| T12 | Lista con filtro `status`, paginación (pageSize ≤ 50) y estados Loading/Empty/Content/Error | `LandlordAgreementsScreen` + VM |
| T13 | Detalle (`closingCode = null`), regenerar código (muestra `backupCode`+`expiresAt`; nunca `qrToken`) | `LandlordAgreementDetailScreen` + VM |
| T14 | Cancelar (`pending`) / Finalizar (`active`) con `reason`+`note` reales y confirmación previa | `LandlordAgreementDetailScreen` + VM |

Entradas: Home → «Mis acuerdos (arrendador)»; `LandlordRequestDetailScreen` → «Crear acuerdo»
solo si `status == ACCEPTED`.

### F3 — UX estudiante (T15–T17)

| Task | Actividad | Pantalla |
| --- | --- | --- |
| T15 | Ingreso de código manual (`k7pm-2qxa`) → `POST /agreements/preview` | `RentalCodeScreen` + VM |
| T16 | Preview con datos reales + **checkbox explícito** + `POST /agreements/accept` (`acceptedTerms=true`) → navega a Mi renta | `AgreementPreviewScreen` + VM |
| T17 | Manejo de errores de código (404/410/429), `Retry-After`, intentos, sin auto-retry | ambas VMs + `AgreementErrorMessages` |

`qrToken` se acepta por contrato si llega por medio legítimo; en UI solo se ingresa `backupCode`.

### F4 — Mi renta + contacto (T18)

| Task | Actividad |
| --- | --- |
| T18 | `GET /users/me/rental`: habitación, edificio, términos, dirección, arrendador, `activatedAt`, `nextPayment` **solo informativo**, botón «Hablar con arrendador» → `ConversationRoute(conversationId)`; `no_active_rental` → Empty; sin teléfono; sin UI de pago; `location` parseado, sin render de mapa |

### F5 — Tests (T19)

Ver spec §11 (repository MockWebServer + ViewModels + regresión D3–D13).
Archivos previstos:

```
app/src/test/java/com/thehouse/app/data/agreements/AgreementsRepositoryIntegrationTest.kt
app/src/test/java/com/thehouse/app/presentation/rental/MyRentalViewModelTest.kt
app/src/test/java/com/thehouse/app/presentation/rental/AgreementPreviewViewModelTest.kt
app/src/test/java/com/thehouse/app/presentation/landlord/CreateAgreementViewModelTest.kt
app/src/test/java/com/thehouse/app/presentation/landlord/LandlordAgreementsViewModelTest.kt
```

### F6 — Validación y evidencia (T20)

1. `.\gradlew.bat compileDebugKotlin`
2. Si verde: `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` (una sola vez)
3. Máximo **1** ciclo correctivo; si persiste → detener y escalar.
4. Runtime (T21, condicional): solo con emulador/ADB + credenciales autorizadas + solicitud
   aceptada de prueba; si no, documentar la causa exacta (nunca inventar credenciales).
5. Registrar evidencia en `evidence.md` y decisión en `memoria.md` (M-MOB-051).

## Orden de ejecución

```
F0 (bloqueante) → F1 → F2 → F3 → F4 → F5 → F6
```

F1 es prerrequisito de F2/F3/F4. F5 puede paralelizarse con F2–F4 por capa.
Ninguna fase se inicia sin la declaración de autorización del bloque activo.

## Criterios de salida (Definition of Done)

- [ ] Las 9 rutas implementadas exactamente; cero contratos existentes modificados.
- [ ] Sin pagos, sin teléfono, sin permisos, sin dependencias nuevas, sin segundo Retrofit.
- [ ] Tabla de errores §7 completa y sin fugas de HTTP/ProblemDetails/stack trace.
- [ ] `closingCode` nunca persistido ni loggeado; `qrToken` no se muestra en UI.
- [ ] Set de validación verde con máx. 1 ciclo correctivo.
- [ ] Evidencia real (comandos + salidas) en `evidence.md`; `memoria.md` actualizado.
- [ ] Runtime documentado como ejecutado o con causa exacta de no ejecución.
