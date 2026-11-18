# NV-15 — PLAN

| Campo | Valor |
|---|---|
| Spec | NV-15 Privacy + Delete Account |
| STATUS | EXECUTED (Mobile) / PARTIAL — contratos externos BLOCKED |
| AUTHORIZATION | AUTHORIZED (instruccion humana 2026-11-17, limitada a NV-15) |

## Fases (solo dentro de NV-15)
- F1 — Contrato de eliminacion (NV-27) + URL_AVISO_PRIVACIDAD real. Verificable: tabla o BLOCKED (mantener BLOCKED-PRIVACY-URL si la URL falta).
  - Resultado: `DELETE-ACCOUNT-BACKEND-BLOCKED` y `BLOCKED-PRIVACY-URL`; no se invento contrato ni URL.
- F2 — Domain/Data: casos de uso de borrado + limpieza local total. Verificable: tests de limpieza por usuario.
  - Resultado: `DeleteAccountUseCase`, `AccountRepository`, `LocalUserDataPurger`/`LocalUserDataPurgerImpl`, `LocalSessionCleaner`; tests verdes.
- F3 — UI privacidad/eliminar con doble confirmacion. Verificable: tras eliminar -> Onboarding sin residuales.
  - Resultado: ruta publica `privacidad`, entrada desde Home, dialogo de confirmacion, estado bloqueado sin contrato, navegacion a Login con `popUpTo(0)` tras exito.
- F4 — Calidad/evidencia: build/tests/lint + evidence.md. Verificable: resultados reales.
  - Resultado: `testDebugUnitTest`, `lintDebug` y `assembleDebug` en verde; evidencia en `evidence.md`.

## Fuera de plan
Exportacion de datos sin contrato, URL de privacidad inventada, endpoint de eliminacion inventado, cambios en Backend.
