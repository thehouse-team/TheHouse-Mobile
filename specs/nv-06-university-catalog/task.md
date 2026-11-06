# NV-06 - TASKS

| Campo | Valor |
|---|---|
| Spec | NV-06 University Catalog (+ alias roadmap "Persistent Session" = sesion NV-04) |
| STATUS | PLANNED (catalogo) / D3 ejecutada (sesion persistente) |
| AUTHORIZATION | University Catalog DO NOT EXECUTE; sesion D3 autorizada por orden humana 2026-11-06 |

- [ ] NV-06-T01 - Confirmar contrato universities (NV-27); si falta, marcar BLOCKED.
- [ ] NV-06-T02 - Modelo Domain + DTO + mapeo.
- [ ] NV-06-T03 - UniversityRepository (Domain/Data).
- [ ] NV-06-T04 - Lista de universidades (UDF, accesible).
- [ ] NV-06-T05 - Detalle basico de universidad.
- [ ] NV-06-T06 - Tests unitarios/integracion (MockWebServer).
- [ ] NV-06-T07 - Build/tests/lint y evidence.md.

## D3 - Persistent Session (BLOQUE 2, alias sesion NV-04)
- [x] NV-06-D3-01 - POST /auth/refresh + rotacion local del refreshToken.
- [x] NV-06-D3-02 - GET /users/me (solo id/email/createdAt; sin "Bienvenido" con /me OK).
- [x] NV-06-D3-03 - Refresh automatico ante 401 con single-flight (Mutex) + maximo 1 retry.
- [x] NV-06-D3-04 - Restauracion de sesion al reiniciar app (refresh 200 + /me; 401/sin refresh -> Anonymous).
- [x] NV-06-D3-05 - Cierre de sesion ante refresh 401 (borrar tokens, Anonymous -> Login).
