# NV-10 — EVIDENCIA

## D8.1 — Conexión real de visitas (2026-11-08)

- Branch: `feature/real-visits-api`.
- Contrato real: `POST /api/v1/visits`, `GET /api/v1/users/me/visits`, `GET /api/v1/visits/{id}`, `POST /api/v1/visits/{id}/cancel`.
- Implementación: `VisitsApi` → DTO → mapper → `VisitsRepositoryImpl` → use cases → ViewModels/UI.
- Idempotencia: misma intención conserva `Idempotency-Key`; nueva intención genera UUID nuevo; doble submit bloqueado.
- Validación: America/Mexico_City, 08:00–20:00, intervalos de 15 min, mínimo 2 h, máximo 60 días, mensaje ≤300.
- Errores: 409/422 mapeados a códigos de negocio y textos amigables; 429/503 recuperables.
- Tests: `VisitsRepositoryIntegrationTest`, `VisitValidatorTest`, `ScheduleVisitViewModelTest`, `VisitsViewModelTest`.
- Comando: `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug`.
- Resultado real: `BUILD SUCCESSFUL` (99 tests).
- Runtime: no se ejecutó emulador en esta sesión; validación estática y de tests únicamente.
