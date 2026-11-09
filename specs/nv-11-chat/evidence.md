# NV-11 — EVIDENCIA

Estado: CHAT-BACKEND-BLOCKED.

## Backend read-only
- Revisado C:\THEHOUSE\TheHouse-Backend.
- No existen endpoints de chat/conversations/messages.

## Mobile
- Property Detail "Contactar" mantiene guard Guest -> Login.
- Auth -> aviso de indisponibilidad (sin simular envío).
- No se crean mensajes fake.

## Validación
- compileDebugKotlin: BUILD SUCCESSFUL
- testDebugUnitTest: BUILD SUCCESSFUL
- lintDebug: BUILD SUCCESSFUL
- assembleDebug: BUILD SUCCESSFUL
- git diff --check: sin errores