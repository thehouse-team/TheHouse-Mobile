# Plan D13

- Fase A (Contrato/Data): `ReportsApi`, DTOs exactos y mapper, `ReportsRepository` con verificación del dueño (inyectada en D12, sin duplicar landlord profile), `BuildingStatus` + `rejected|suspended`, `DomainError.AccountSuspended`; observación global en cliente existente, Session y login/refresh/restauración sin logout automático.
- Fase B (Domain/Presentation): `CreateReportUseCase`, `ReportViewModel` + `ReportScreen` (razones reales, contador, confirmación, anti doble submit); tarjeta de verificación en D12 building detail; mensajes de suspensión en Login/registro.
- Fase C (Navegación/UX): `ReportRoute(targetType, targetId)` validada (sin fallback de target); reportar cuarto/edificio desde Request Detail (student) usando los IDs reales. Pantalla global de suspensión con logout solo explícito. Reutiliza D11. Sin UI admin (contrato `/admin/*` no es de la app).
- Fase D (Validación): tests MockWebServer (reports/verification/suspensión/estados nuevos), regresión D3–D12, `compileDebugKotlin` → set completo, runtime si es posible.
