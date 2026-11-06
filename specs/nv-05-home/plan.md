# NV-05 — PLAN

| Campo | Valor |
|---|---|
| Spec | NV-05 Home |
| STATUS | PLANNED |
| AUTHORIZATION | DO NOT EXECUTE |

## Fases (solo dentro de NV-05)
- F1 — Diseno de estados y rutas: UiState/UiEvent/UiEffect, ruta Home, gate de sesion. Verificable: contrato documentado + revision de boundaries.
- F2 — ViewModel + UI: implementacion MVVM/UDF y accesibilidad. Verificable: tests unitarios ViewModel en verde.
- F3 — Navegacion y migracion del Inicio provisional de NV-04. Verificable: sin sesion -> Login; con sesion -> Home.
- F4 — Calidad y evidencia: lint/tests/build y registro en evidence.md. Verificable: comandos ejecutados con resultado real.

## Fuera de plan
Cualquier fase de NV-06..NV-29. Ninguna tarea de otra spec.
