# NV-16 — PLAN

| Campo | Valor |
|---|---|
| Spec | NV-16 Offline + Sync (cache reads) |
| STATUS | APPROVED FOR IMPLEMENTATION |
| AUTHORIZATION | APPROVED FOR IMPLEMENTATION (autorización humana explícita en sesión) |

## Fases
- F1 — Inventario/clasificación: lecturas cacheables (propiedades, universidades, favoritos lectura, perfil/preferencias lectura segura). Escrituras y flujos sin contrato/soporte: online-only o requieren conexión; sin cola/sync en background.
- F2 — Cache local reutilizando infraestructura existente (DataStore/JSON, sin segunda base Room) con key estable, `userScope` cuando aplique y `updatedAt/cache timestamp`.
- F3 — Repositorios cache-first + network refresh: emitir cache si existe, consultar backend, actualizar cache en éxito, emitir fresco; en fallo de red usar cache stale/offline y si no hay cache retornar error/retry existente.
- F4 — UX mínima offline/stale y limpieza de cache privada en logout/delete account para aislamiento entre usuarios.
- F5 — Tests necesarios + `testDebugUnitTest`, `lintDebug`, `assembleDebug` + evidence.

## Fuera de plan
Sync de escrituras, WorkManager workers/polling/colas ficticias, contratos inventados, datos sensibles persistidos.