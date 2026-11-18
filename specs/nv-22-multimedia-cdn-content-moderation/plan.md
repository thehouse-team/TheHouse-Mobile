# NV-22 — PLAN

| Campo | Valor |
|---|---|
| Spec | NV-22 Multimedia / CDN / Moderation |
| STATUS | PARTIAL |
| AUTHORIZATION | AUTORIZADO 2026-11-17 |

## Fases
- F1 — Inventario real vs contrato NV-27: sin flujos upload/cámara/picker implementados, sin contrato moderación ejecutable. **DONE (BLOCKED contractual documentado)**.
- F2 — Selección/captura segura preparada: Photo Picker preferido, sin permisos storage; cámara solo contextual si existe flujo real. **DONE (preparación/políticas/utilidades)**.
- F3 — Validación local, compresión/sanitización metadata best-effort, estados y manejo seguro de URI/archivos. **DONE**.
- F4 — Moderación backend: contrato ausente → `MULTIMEDIA-MODERATION-BACKEND-BLOCKED`; sin simulación. **DONE (bloqueo explícito)**.
- F5 — Tests/gates/evidencia. **DONE**.

## Fuera de plan
CDN comercial, moderación IA/pago, uploads reales sin contrato, endpoints/moderación inventados, video sin requisito contractual.
