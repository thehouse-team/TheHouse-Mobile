# NV-06 — EVIDENCIA

Sin evidencia todavia — PLANNED, DO NOT EXECUTE.

## Hechos reales preservados
- Contrato confirmado M-MOB-044: register/login/logout/properties/universities/health. NV-27 sigue BLOCKED por /auth/refresh y /users/me. No se inventan endpoints.

## Evidencia D3 - Persistent Session (2026-11-06, branch feature/persistent-session-contracts)
Traduccion roadmap: "NV-06 Persistent Session" = sesion persistente NV-04 (sin crear spec nueva). University Catalog sigue PLANNED/DO NOT EXECUTE.
- Implementado: refresh, /me, single-flight (Mutex + authenticator), restore al arrancar (RootViewModel), rotacion, refresh 401 -> Anonymous.
- Tests MockWebServer: login guarda refresh, refresh 200 rota/reemplaza, refresh 401 limpia, /me 200/401, restart restaura/sin refresh/401, multiples 401 -> 1 refresh, max 1 retry, logout limpia, register body solo email+password, authenticator single-flight (PersistentSessionIntegrationTest 13/13 + AuthRepositoryIntegrationTest 8/8).
- `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` (JAVA_HOME jbr Android Studio) BUILD SUCCESSFUL, 54/54 verdes, 0 failures.

## Evidencia D5 - Catalogo Universitario real (2026-11-06, branch feature/universities-properties-explore)
Contrato confirmado solo en C:\THEHOUSE\TheHouse-Backend (read-only, sin modificar), AllowAnonymous:
- GET /api/v1/universities?page&pageSize -> 200 {items[{id,code,name,city,country,createdAt}],page,pageSize,totalItems,totalPages}; 400/429/503/500.
- GET /api/v1/universities/{id} (ObjectId 24hex) -> 200 {id,code,name,city,country,createdAt}; 400/404/429/503/500.
- Default page=1 pageSize=20 max 50. Sin filtros/search. JSON camelCase.
Implementacion: UniversityApi/Dto/mapper/UniversityRepositoryImpl + SelectedUniversityRepository (DataStore selected_university_id) + Home selector real con loading/success/empty/error/retry.
Tests: CatalogRepositoryIntegrationTest 9/9 (200/empty/500/429/detail/persistencia) + CatalogViewModelTest 4/4; suite 75/75 verde, BUILD SUCCESSFUL.
Nota: live Azure timeout (red), contrato por codigo (fuente primaria); no se inventa ningun campo.

## Evidencia futura esperada
Transcripcion del contrato usado, comandos y resultados reales tras autorizacion.
