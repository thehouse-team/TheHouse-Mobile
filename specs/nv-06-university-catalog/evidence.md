# NV-06 — EVIDENCIA

Sin evidencia todavia — PLANNED, DO NOT EXECUTE.

## Hechos reales preservados
- Contrato confirmado M-MOB-044: register/login/logout/properties/universities/health. NV-27 sigue BLOCKED por /auth/refresh y /users/me. No se inventan endpoints.

## Evidencia D3 - Persistent Session (2026-11-06, branch feature/persistent-session-contracts)
Traduccion roadmap: "NV-06 Persistent Session" = sesion persistente NV-04 (sin crear spec nueva). University Catalog sigue PLANNED/DO NOT EXECUTE.
- Implementado: refresh, /me, single-flight (Mutex + authenticator), restore al arrancar (RootViewModel), rotacion, refresh 401 -> Anonymous.
- Tests MockWebServer: login guarda refresh, refresh 200 rota/reemplaza, refresh 401 limpia, /me 200/401, restart restaura/sin refresh/401, multiples 401 -> 1 refresh, max 1 retry, logout limpia, register body solo email+password, authenticator single-flight (PersistentSessionIntegrationTest 13/13 + AuthRepositoryIntegrationTest 8/8).
- `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` (JAVA_HOME jbr Android Studio) BUILD SUCCESSFUL, 54/54 verdes, 0 failures.

## Evidencia futura esperada
Transcripcion del contrato usado, comandos y resultados reales tras autorizacion.
