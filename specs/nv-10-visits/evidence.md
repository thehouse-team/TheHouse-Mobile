# NV-10 — EVIDENCIA

Estado: VISITS-BACKEND-BLOCKED.

## Backend read-only
- Revisado C:\THEHOUSE\TheHouse-Backend.
- No existen endpoints de visitas/appointments/booking.
- PropertyEndpoint solo expone list/detail públicos.

## Mobile
- PropertyDetail incluye acción "Agendar visita".
- Guest → Login.
- Auth → mensaje de indisponibilidad (sin simular agendado).
- No se crean visitas fake ni éxito optimista.

## Validación
- compileDebugKotlin: BUILD SUCCESSFUL
- testDebugUnitTest: BUILD SUCCESSFUL
- lintDebug: BUILD SUCCESSFUL
- assembleDebug: BUILD SUCCESSFUL
- git diff --check: sin errores