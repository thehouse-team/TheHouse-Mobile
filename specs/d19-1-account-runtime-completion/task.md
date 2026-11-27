# TASK D19.1 — Account Runtime Completion

| ID | Tarea | Estado |
|---|---|---|
| D19.1-T01 | Crear rama desde `origin/main` @ `50febcd` y documentación SDD | DONE |
| D19.1-T02 | Diagnosticar Perfil (HTTP→DTO→mapper→repository→ViewModel→UI) | DONE — causa verificada: `updatedAt` no nullable |
| D19.1-T03 | Diagnosticar Preferencias (HTTP 200 con error visual) | DONE — causa verificada: campo `currency` raíz inexistente |
| D19.1-T04 | Diagnosticar Smart Match 422 `missing` y perfil completo | DONE — sin bug propio; dependía de Perfil/Preferencias |
| D19.1-T05 | Diagnosticar registro → aviso de privacidad → regreso | DONE — causa verificada: mensaje legal falso |
| D19.1-T06 | Verificar Arrendador (factory, 404/200/403, introducir código) | DONE — fix presente; sin bug nuevo |
| D19.1-T07 | Corregir causas verificadas + instrumentación DEBUG segura | DONE |
| D19.1-T08 | Tests de regresión por causa | DONE — 10 nuevos; ninguno eliminado |
| D19.1-T09 | Gates locales + evidencia + memoria | DONE — 524 tests, lint, assemble, diff-check |
| D19.1-T10 | Commit, push y PR nuevo; verificar CI remoto | IN PROGRESS |
| D19.1-T11 | Certificación runtime o `RUNTIME_PENDING` con pasos exactos | DONE — RUNTIME_PENDING sin credenciales |

Regla: ninguna tarea se marca DONE sin evidencia real. `BLOCKED` nunca equivale a `DONE`.
