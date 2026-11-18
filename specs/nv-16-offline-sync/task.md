# NV-16 - TASKS

| Campo | Valor |
|---|---|
| Spec | NV-16 Offline + Sync (cache reads) |
| STATUS | COMPLETED |
| AUTHORIZATION | APPROVED FOR IMPLEMENTATION (autorización humana explícita en sesión) |

- [x] NV-16-T01 - Inventario/clasificación de flujos y política: cache-first reads; sin cola/sync de escrituras no soportadas.
- [x] NV-16-T02 - Cache local (DataStore/JSON, sin segunda base Room) con keys/timestamps/userScope.
- [x] NV-16-T03 - Repositorios cache-first + network refresh (propiedades, universidades, favoritos lectura, perfil/preferencias lectura segura).
- [x] NV-16-T04 - UX offline/stale mínima y limpieza/aislamiento de cache privada (logout/delete account).
- [x] NV-16-T05 - Tests necesarios (cache hit/miss, refresh, offline/stale fallback, aislamiento, no sensibles).
- [x] NV-16-T06 - testDebugUnitTest, lintDebug, assembleDebug y evidence.md (runtime no disponible por falta de ADB).