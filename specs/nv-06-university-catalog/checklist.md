# NV-06 — CHECKLIST

- [x] Contrato universities confirmado (NV-27)
- [x] Repository y mapeo testeados
- [x] Lista/detalle con estados UDF
- [x] Accesibilidad verificada
- [x] Build/tests/lint verdes con evidencia real (75/75)
- [x] COST-001 $0 documentado

## D3 - Persistent Session (BLOQUE 2, alias sesion NV-04)
- [x] Refresh 200 rota y reemplaza refresh anterior (tests MockWebServer)
- [x] Refresh 401 limpia sesion/storage -> Anonymous (tests)
- [x] /me 200 hidrata identidad real; /me 401 -> refresh + 1 retry (tests)
- [x] Restart restaura con refresh+/me; sin refresh/401 -> Anonymous (tests)
- [x] Multiples 401 concurrentes -> exactamente 1 refresh (Mutex + authenticator) (tests)
- [x] Logout limpia SIEMPRE local; register sin regresion (tests)
- [x] `testDebugUnitTest lintDebug assembleDebug` BUILD SUCCESSFUL (54/54)

## D5 - Catalogo Universitario real
- [x] GET /universities 200/empty/error mapeados (AllowAnonymous, page/pageSize)
- [x] Selector real con seleccion persistida; sin UTTT hardcodeada productiva
- [x] PropertyCard real sin Match/verificacion/distancia inventados
