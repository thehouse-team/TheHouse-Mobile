# NV-06 - TASKS

| Campo | Valor |
|---|---|
| Spec | NV-06 University Catalog (+ alias roadmap "Persistent Session" = sesion NV-04) |
| STATUS | PARTIAL D5 (catalogo real ejecutado; detalle basico via API) |
| AUTHORIZATION | D5 autorizada por orden humana explicita 2026-11-06 |

- [x] NV-06-T01 - Confirmar contrato universities (NV-27); si falta, marcar BLOCKED.
- [x] NV-06-T02 - Modelo Domain + DTO + mapeo.
- [x] NV-06-T03 - UniversityRepository (Domain/Data).
- [x] NV-06-T04 - Lista de universidades (UDF, accesible).
- [x] NV-06-T05 - Detalle basico de universidad.
- [x] NV-06-T06 - Tests unitarios/integracion (MockWebServer).
- [x] NV-06-T07 - Build/tests/lint y evidence.md.

## D3 - Persistent Session (BLOQUE 2, alias sesion NV-04)
- [x] NV-06-D3-01 - POST /auth/refresh + rotacion local del refreshToken.
- [x] NV-06-D3-02 - GET /users/me (solo id/email/createdAt; sin "Bienvenido" con /me OK).
- [x] NV-06-D3-03 - Refresh automatico ante 401 con single-flight (Mutex) + maximo 1 retry.
- [x] NV-06-D3-04 - Restauracion de sesion al reiniciar app (refresh 200 + /me; 401/sin refresh -> Anonymous).
- [x] NV-06-D3-05 - Cierre de sesion ante refresh 401 (borrar tokens, Anonymous -> Login).

## D5 - Catalogo Universitario real (2026-11-06)
- [x] NV-06-D5-01 - GET /universities + /universities/{id} (AllowAnonymous, page/pageSize) con DTO/mapper/repository.
- [x] NV-06-D5-02 - Selector Home con lista real y seleccion persistida (DataStore, sin UTTT productiva).
- [x] NV-06-D5-03 - Estados loading/success/empty/error/retry (tests MockWebServer + ViewModel).
