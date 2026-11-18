# NV-21 — PLAN

| Campo | Valor |
|---|---|
| Spec | NV-21 Permissions / Platform Security |
| STATUS | EXECUTED (alcance móvil autorizado) |
| AUTHORIZATION | AUTORIZADO 2026-11-17 |

## Fases
- F1 — Inventario de permisos declarados y auditoría de necesidad real. **DONE** (INTERNET, POST_NOTIFICATIONS; sin cámara/ubicación/storage).
- F2 — Runtime permissions: preservar notificaciones contextuales; evitar cámara/ubicación sin flujo real. **DONE**.
- F3 — Superficie de plataforma: exported, intents, deep links, backup/data extraction. **DONE**.
- F4 — Conciliación con Data Safety NV-03 y evidencia. **PARTIAL** (inventario listo; URL privacidad NV-03 sigue `BLOCKED-PRIVACY-URL`).

## Fuera de plan
Permisos de specs futuras no implementadas, background location, cambios de manifest sin justificación, publicación Play.
