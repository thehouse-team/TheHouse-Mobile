# Checklist D13.1 — Acuerdos de renta + Mi renta

Uso: marcar solo con evidencia real. Un ítem sin evidencia queda **NO**.
`BLOCKED` nunca equivale a `DONE`.

## 0. Gobernanza

- [ ] `main` contiene D13 (PR #16 fusionado) — **NO** (evidencia §E1)
- [ ] `feature/agreements-my-rental` creada desde la base aprobada — NO
- [ ] Spec/plan/task aprobados por instrucción humana (`EJECUTA D13.1`) — NO
- [ ] Declaración de autorización emitida antes de implementar — NO
- [ ] `constitution.md` + `AGENTS.md` + `memoria.md` + spec activa + OWASP revisados — NO

## 1. Contrato

- [ ] Exactamente las 9 rutas de la spec §1.1, sin rutas extra — NO
- [ ] Cero cambios en contratos existentes (D3–D13) — NO
- [ ] Bodies exactos: `CreateAgreementRequest`, `EndAgreementRequest`, `ClosingCodeRequest`, `AcceptAgreementRequest` — NO
- [ ] Sin campos desconocidos en requests (`Disallow` en Backend) — NO
- [ ] Estados exactos: `pending|active|ended|cancelled` — NO
- [ ] `reason` exacto: `contract_finished|early_termination|other` — NO
- [ ] Paginación `page ≥ 1`, `pageSize 1–50` — NO

## 2. Arrendador

- [ ] «Crear acuerdo» solo desde solicitud `ACCEPTED` — NO
- [ ] Body con `requestId, monthlyRent, deposit, startDate(YYYY-MM-DD), durationMonths, paymentDay` — NO
- [ ] Validación local: rent 500–100000 · deposit 0–2×rent · fecha −30/+90 d · 1–24 meses · día 1–28 — NO
- [ ] Anti doble submit en create/cancel/end/code — NO
- [ ] 409 `request_state` / `room_unavailable` / `agreement_open` con mensaje amigable — NO
- [ ] Lista con filtro `status`, `roomId` y paginación — NO
- [ ] Estados Loading / Empty / Content / Error en lista — NO
- [ ] Detalle con **`closingCode = null`** (no se asume código disponible) — NO
- [ ] «Generar nuevo código» (`POST /code`) muestra `backupCode` + `expiresAt` (10 min) — NO
- [ ] 409 `agreement_state` en `/code` — NO
- [ ] Cancelar solo `pending`; finalizar solo `active` con `reason`+`note` reales — NO
- [ ] Confirmación previa antes de cancelar/finalizar — NO

## 3. Estudiante

- [ ] Ingreso manual de código con formato `k7pm-2qxa` (guiones/espacios/minúsculas aceptados) — NO
- [ ] `POST /agreements/preview` con **exactamente uno** de `code`/`qrToken` — NO
- [ ] Preview muestra `room`, `building{name,rules}`, `landlord{name}`, `terms`, `codeExpiresAt` — NO
- [ ] **Sin aceptación automática**; CTA deshabilitado hasta marcar el checkbox — NO
- [ ] `POST /agreements/accept` envía `acceptedTerms: true` solo con aceptación explícita — NO
- [ ] Tras 200 → navega a Mi renta con el `RentalResponse` devuelto — NO
- [ ] 400 si `acceptedTerms != true` o ambos/ningún código — NO

## 4. Errores (spec §7)

- [ ] 404 `code_invalid` → «El código no es válido.» — NO
- [ ] 410 `code_expired` → «El código expiró. Solicita uno nuevo al arrendador.» — NO
- [ ] 410 `code_used` → «Este código ya fue utilizado.» — NO
- [ ] 429 `too_many_attempts` → «Realizaste demasiados intentos. Inténtalo más tarde.» — NO
- [ ] `Retry-After` respetado (900 s / 60 s) y **sin retry automático** — NO
- [ ] 404 `no_active_rental`, 409 `agreement_state`, 409 `agreement_open`, 409 `request_state`, 409 `room_unavailable` — NO
- [ ] 400 validación · 401 sesión · 403 `landlord_required` · 503 temporal · Network/Timeout recuperables — NO
- [ ] Nunca se muestra status HTTP, ProblemDetails crudo ni stack trace — NO
- [ ] Máximo 5 intentos/15 min respetado (Backend autoridad; contador local solo UX) — NO

## 5. Mi renta

- [ ] `GET /api/v1/users/me/rental` implementado — NO
- [ ] Muestra habitación, edificio, términos, dirección, arrendador, `activatedAt` — NO
- [ ] `nextPayment` **solo informativo** (fecha + monto, sin acción) — NO
- [ ] **Cero** UI de pago (sin «Pagar», checkout, tarjeta, transferencia, simulación) — NO
- [ ] Si aparece «Pagar»: oculto o deshabilitado con «Pago no disponible desde la aplicación.» — NO
- [ ] `no_active_rental` → estado Empty — NO

## 6. Privacidad y contacto

- [ ] Sin teléfono del arrendador (ni mostrado ni solicitado) — NO
- [ ] Contacto solo vía `conversationId` → `ConversationRoute` (Chat D9.1) — NO
- [ ] Chat no duplicado; sin segundo flujo de mensajes — NO
- [ ] Sin `userId`/`studentId`/`ownerId` enviados por el cliente — NO

## 7. QR

- [ ] Sin SDK de cámara/QR, sin permisos nuevos, sin dependencias nuevas — NO
- [ ] `qrToken` no se muestra en UI ni se persiste — NO
- [ ] `backupCode` en memoria de ViewModel, limpiado al salir, nunca loggeado — NO
- [ ] `QR-SCAN-PENDING` documentado como pendiente — NO

## 8. Navegación

- [ ] Entradas Home: «Mi renta» e «Introducir código de renta» — NO
- [ ] Entrada arrendador desde solicitud aceptada → «Crear acuerdo» — NO
- [ ] Guest → Login; autenticado → flujo normal — NO
- [ ] `bottomTabs` sin cambios; sin romper navegación existente — NO
- [ ] Rutas nuevas en `AgreementRoutes.kt`; registro puntual en `MainActivity` — NO

## 9. Tests (spec §11)

- [ ] LANDLORD: create 201 · `request_state` · `room_unavailable` · `agreement_open` — NO
- [ ] LANDLORD: list/filter/pagination · detail · `closingCode == null` — NO
- [ ] LANDLORD: regenerate code · `agreement_state` · cancel · end — NO
- [ ] STUDENT: preview `code` · preview `qrToken` · accept `acceptedTerms=true` — NO
- [ ] STUDENT: `code_invalid` · `code_expired` · `code_used` · `too_many_attempts` + `Retry-After` — NO
- [ ] STUDENT: get rental · `no_active_rental` · `conversationId` → chat — NO
- [ ] PAYMENT: `nextPayment` informativo · ninguna acción de pago — NO
- [ ] REGRESIÓN D3–D13 en verde — NO

## 10. Validación

- [ ] `.\gradlew.bat compileDebugKotlin` verde — NO
- [ ] `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` verdes (set completo, 1 sola vez) — NO
- [ ] Máximo 1 ciclo correctivo usado — NO
- [ ] Sin pruebas/artificios para forzar un resultado — NO

## 11. Runtime y evidencia

- [ ] Flujo punta a punta ejecutado (emulador/ADB + credenciales + solicitud aceptada) — NO
- [ ] O causa exacta documentada sin inventar credenciales — NO
- [ ] `evidence.md` con comandos y salidas reales — NO
- [ ] `memoria.md` actualizado (M-MOB-051) — NO
- [ ] Deuda y pendientes (`QR-SCAN-PENDING`, `MAP-PENDING`) registrados — NO
