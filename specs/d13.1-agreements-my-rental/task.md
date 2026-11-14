# Tasks D13.1 — Acuerdos de renta + Mi renta

| Campo | Valor |
| --- | --- |
| Spec | `specs/d13.1-agreements-my-rental/spec.md` v1.0.0 |
| Plan | `specs/d13.1-agreements-my-rental/plan.md` |
| Estado global | **BLOCKED — NO AUTORIZADO** |
| Rama | `feature/agreements-my-rental` (NO creada) |

Leyenda de estado: `PENDING` · `IN_PROGRESS` · `DONE` · `BLOCKED` · `N/A`.
**`BLOCKED` nunca equivale a `DONE`.**

## F0 — Gobernanza

| ID | Task | Estado | Bloqueo |
| --- | --- | --- | --- |
| T01 | Verificar `main` contenga D13 (`git switch main && git pull origin main`) | **BLOCKED** | `main = 6ce90c1` sin D13; PR #16 OPEN/BLOCKED. Requiere decisión humana |
| T02 | Crear `feature/agreements-my-rental` desde la base aprobada | PENDING | depende de T01 |
| T03 | Aprobación humana de spec/plan/task | PENDING | requiere «EJECUTA D13.1» |
| T04 | Declaración de autorización antes de implementar | PENDING | depende de T03 |

## F1 — Contrato y dominio

| ID | Task | Estado |
| --- | --- | --- |
| T05 | `AgreementDtos.kt`: `CreateAgreementRequest`, `EndAgreementRequest`, `ClosingCodeRequest`, `AcceptAgreementRequest`, `AgreementRoomSummary`, `AgreementTermsResponse`, `AgreementStudentSummary`, `ClosingCodeResponse`, `LandlordAgreementResponse`, `LandlordAgreementListResponse`, `AgreementBuildingSummary`, `AgreementLandlordSummary`, `AgreementPreviewResponse`, `RentalNextPayment`, `RentalAddress`, `RentalLocation`, `RentalResponse` | PENDING |
| T06 | `AgreementsApi.kt`: las 9 rutas exactas (`@POST/@GET` con `@Path`/`@Query`) | PENDING |
| T07 | `domain/model/Agreement.kt`: `AgreementStatus{pending,active,ended,cancelled}`, `EndReason{contract_finished,early_termination,other}`, `Agreement`, `AgreementTerms`, `ClosingCode`, `MyRental` | PENDING |
| T08 | `domain/error/AgreementError.kt`: `AgreementErrorCode` (`REQUEST_STATE`, `ROOM_UNAVAILABLE`, `AGREEMENT_OPEN`, `AGREEMENT_STATE`, `VERSION_CONFLICT`, `LANDLORD_REQUIRED`, `CODE_INVALID`, `CODE_EXPIRED`, `CODE_USED`, `TOO_MANY_ATTEMPTS`, `NO_ACTIVE_RENTAL`, `UNKNOWN`) + `AgreementException(domainError, code, retryAfterSeconds)` | PENDING |
| T09 | `AgreementMappers.kt` + `AgreementsRepositoryImpl.kt` + `AgreementsRepository.kt`: `safeCall`, parseo de `code` desde Problem Details, header `Retry-After`, 410 → `code_expired`/`code_used` | PENDING |
| T10 | `AgreementUseCases.kt`: `CreateAgreement`, `GetLandlordAgreements`, `GetLandlordAgreement`, `IssueAgreementCode`, `CancelAgreement`, `EndAgreement`, `PreviewAgreement`, `AcceptAgreement`, `GetMyRental` | PENDING |

## F2 — UX arrendador

| ID | Task | Estado |
| --- | --- | --- |
| T11 | `CreateAgreementScreen` + `CreateAgreementViewModel`: form con validación local (rent 500–100000, deposit 0–2×rent, startDate −30/+90 d, duration 1–24, paymentDay 1–28), anti doble submit, muestra `backupCode`+`expiresAt` al 201 | PENDING |
| T12 | `LandlordAgreementsScreen` + VM: filtro `status`, paginación (pageSize ≤ 50), Loading/Empty/Content/Error | PENDING |
| T13 | `LandlordAgreementDetailScreen` + VM: detalle con `closingCode = null`, «Generar nuevo código» (`POST /code`) mostrando solo `backupCode`+`expiresAt` | PENDING |
| T14 | «Cancelar» (solo `pending`) y «Finalizar» (solo `active`) con selector `reason` + `note` ≤300 y confirmación previa | PENDING |
| T14b | Entradas: Home → «Mis acuerdos (arrendador)»; `LandlordRequestDetailScreen` → «Crear acuerdo» **solo** si `status == ACCEPTED` | PENDING |

## F3 — UX estudiante

| ID | Task | Estado |
| --- | --- | --- |
| T15 | `RentalCodeScreen` + VM: input de código manual (formato `k7pm-2qxa`), envío a `POST /agreements/preview` | PENDING |
| T16 | `AgreementPreviewScreen` + VM: `room`/`building{name,rules}`/`landlord{name}`/`terms`/`codeExpiresAt`, **checkbox explícito** con CTA deshabilitado hasta marcar, `POST /agreements/accept` (`acceptedTerms=true`) → Mi renta | PENDING |
| T17 | `AgreementErrorMessages.kt` + manejo 404/410/429: mensajes exactos de la spec §7, `Retry-After` (900 s / 60 s) bloquea el CTA sin retry automático, contador local de intentos (UX, no autoridad), 400 → validación | PENDING |

## F4 — Mi renta

| ID | Task | Estado |
| --- | --- | --- |
| T18 | `MyRentalScreen` + VM: habitación, edificio, términos, dirección, arrendador, `activatedAt`, `nextPayment` **informativo** («Próximo pago (informativo)»), **sin botón de pago**, «Hablar con arrendador» → `ConversationRoute(conversationId)`, `no_active_rental` → Empty, sin teléfono, `location` parseado sin mapa | PENDING |

## F5 — Tests

| ID | Task | Estado |
| --- | --- | --- |
| T19a | `AgreementsRepositoryIntegrationTest` — LANDLORD: create 201 (body exacto), `request_state`, `room_unavailable`, `agreement_open`, list/filter/pagination, detail, **`closingCode == null`**, regenerate code, `agreement_state`, cancel, end, `landlord_required` | PENDING |
| T19b | `AgreementsRepositoryIntegrationTest` — STUDENT: preview `code`, preview `qrToken`, 400 ambos/ninguno, accept `acceptedTerms=true`, 400 si ≠ true, `code_invalid`, `code_expired`, `code_used`, `too_many_attempts` + `Retry-After: 900`, rental 200, `no_active_rental`, `conversationId` → Chat | PENDING |
| T19c | PAYMENT: `nextPayment` informativo, **ninguna** acción de pago disponible en la UI | PENDING |
| T19d | ViewModels: anti doble submit, CTA de aceptación, bloqueo por `Retry-After`, Loading/Empty/Content/Error, mensajes exactos | PENDING |
| T19e | REGRESIÓN D3–D13 en verde | PENDING (requiere `main` con D13) |

## F6 — Validación y evidencia

| ID | Task | Estado |
| --- | --- | --- |
| T20 | `.\gradlew.bat compileDebugKotlin`; si verde → `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` (set completo una vez, máx. 1 ciclo correctivo) | PENDING |
| T21 | Runtime condicional (emulador/ADB + credenciales + solicitud aceptada) o documentación exacta de la causa | PENDING |
| T22 | `evidence.md` real + `memoria.md` (M-MOB-051) | PENDING |

## Resumen

| Estado | Cantidad |
| --- | --- |
| `BLOCKED` | 1 (T01) |
| `PENDING` | 26 |
| `DONE` | 0 |
| Total | 27 |
