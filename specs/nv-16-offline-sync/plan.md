# NV-16 — PLAN

| Campo | Valor |
|---|---|
| Spec | NV-16 Offline + Sync |
| STATUS | PLANNED |
| AUTHORIZATION | DO NOT EXECUTE |

## Fases (solo dentro de NV-16)
- F1 — Inventario de entidades sincronizables vs contrato NV-27 + politica de conflictos. Verificable: tabla o alcance solo-cache.
- F2 — Persistencia local + cola de escrituras + Workers con backoff. Verificable: tests de cola sin duplicados.
- F3 — Indicadores offline/pendientes en UI (UDF). Verificable: flujo sin conexion probado.
- F4 — Calidad/evidencia: build/tests/lint + evidence.md. Verificable: resultados reales.

## Fuera de plan
Sync de entidades sin contrato; KSP sin autorizacion.
