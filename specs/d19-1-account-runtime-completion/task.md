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
| D19.1-T12 | Cierre funcional/visual integral (autorización humana 2026-11-27, PR #35 OPEN) | DONE |
| D19.1-T13 | CTA de Preferencias fijo + selectores contractuales (P0) | DONE |
| D19.1-T14 | Smart Match: mostrar `missing` real y ruta a Perfil/Preferencias (P0) | DONE |
| D19.1-T15 | Detalle de cuarto con roomId real (D18 preservado) | DONE — sin cambios; ruta/factory/DTOs verificados |
| D19.1-T16 | Privacidad desde registro sin URL obsoleta (P1) | DONE |
| D19.1-T17 | Mis alojamientos: factory y estados 404/200/403 (P1) | DONE — código verificado; runtime PENDING |
| D19.1-T18 | Logo único en Inicio + tarjeta Smart Match navegable (P1) | DONE |
| D19.1-T19 | Drawer Material 3 con iconos, secciones y selección (P1) | DONE |
| D19.1-T20 | Sincronización de universidad guardada en Inicio (P1) | DONE |
| D19.1-T21 | 10 regresiones nuevas + gates + evidencia + memoria | DONE — 534 tests, lint/assemble/diff-check verdes |
| D19.1-T22 | Reinstalar APK y capturas en dispositivo | DONE — RUNTIME_PASS parcial; auth PENDING |
| D19.1-T23 | Hotfix scroll de Preferencias: aplicar modifier del caller, insets y spacer final | DONE — causa verificada: modifier descartado |
| D19.1-T24 | Diagnóstico solo lectura de recomendaciones/count/search + logging DEBUG existente | DONE — EMPTY plausible, sin defecto Mobile demostrado |
| D19.1-T25 | Texto Smart Match sin claim de ML on-device | DONE — RUNTIME_PASS en dispositivo |
| D19.1-T26 | Favoritos diferido a PR B; 2 regresiones nuevas y gates | DONE — 536 tests, lint/assemble/diff-check verdes |

Regla: ninguna tarea se marca DONE sin evidencia real. `BLOCKED` nunca equivale a `DONE`.
