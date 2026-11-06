# NV-26 — PLAN

| Campo | Valor |
|---|---|
| Spec | NV-26 Versioning / Environments |
| STATUS | PLANNED |
| AUTHORIZATION | DO NOT EXECUTE |

## Fases (solo dentro de NV-26)
- F1 — Matriz de entornos (URL, logging, build type) sin secretos. Verificable: tabla aprobada.
- F2 — Politica versionName/versionCode (VERSIONING.md). Verificable: regla de incremento sin colisiones.
- F3 — Verificacion: un build por entorno sin hardcodeo. Verificable: BuildConfig inspeccionado.
- F4 — Evidencia: build/tests/lint + evidence.md. Verificable: resultados reales.

## Fuera de plan
Cambios de toolchain/applicationId; URLs inventadas.
