# NV-06 — CHECKLIST

- [ ] Contrato universities confirmado (NV-27)
- [ ] Repository y mapeo testeados
- [ ] Lista/detalle con estados UDF
- [ ] Accesibilidad verificada
- [ ] Build/tests/lint verdes con evidencia real (catalogo: pendiente)
- [ ] COST-001 $0 documentado

## D3 - Persistent Session (BLOQUE 2, alias sesion NV-04)
- [x] Refresh 200 rota y reemplaza refresh anterior (tests MockWebServer)
- [x] Refresh 401 limpia sesion/storage -> Anonymous (tests)
- [x] /me 200 hidrata identidad real; /me 401 -> refresh + 1 retry (tests)
- [x] Restart restaura con refresh+/me; sin refresh/401 -> Anonymous (tests)
- [x] Multiples 401 concurrentes -> exactamente 1 refresh (Mutex + authenticator) (tests)
- [x] Logout limpia SIEMPRE local; register sin regresion (tests)
- [x] `testDebugUnitTest lintDebug assembleDebug` BUILD SUCCESSFUL (54/54)
