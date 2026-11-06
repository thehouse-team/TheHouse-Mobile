# NV-14 — PLAN

| Campo | Valor |
|---|---|
| Spec | NV-14 Notifications/FCM |
| STATUS | PLANNED |
| AUTHORIZATION | DO NOT EXECUTE |

## Fases (solo dentro de NV-14)
- F1 — Contrato de eventos/token (NV-27) + evaluacion COST-001 de Firebase/FCM. Verificable: tabla o BLOCKED; MCP Firebase sigue deshabilitado hasta autorizacion.
- F2 — Configuracion Firebase/FCM + registro/desregistro de token. Verificable: token solo con sesion; logout lo desregistra.
- F3 — Manejo foreground/background/deep links. Verificable: sin PII en payloads ni en bloqueo.
- F4 — Calidad/evidencia: build/tests/lint + prueba real + evidence.md. Verificable: resultados reales.

## Fuera de plan
Remitente backend, campanas de marketing, activar MCP Firebase sin autorizacion.
