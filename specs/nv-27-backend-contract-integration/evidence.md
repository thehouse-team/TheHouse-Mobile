# NV-27 — EVIDENCIA

Sin evidencia nueva todavia — BLOCKED, DO NOT EXECUTE.

## Hechos reales preservados (NV-04, 2026-11-05)
- register 201 Created con body exacto {"email","password"} (usuario de prueba qa.register.u2.1105@ejemplo.com; contrasena no registrada por seguridad).
- login 200: {"accessToken":"<JWT>","tokenType":"Bearer","expiresIn":900} (sin refreshToken ni user).
- logout con Bearer 204; sin sesion 401 (Problem Details).
- health/live 200 {"status":"Healthy"} (NV-04 §5).
- Contrato confirmado M-MOB-044: register/login/logout/properties/universities/health.
- Faltan: POST /api/v1/auth/refresh y GET /api/v1/users/me -> BLOCKED. No se inventaron endpoints.

## Evidencia D3 (2026-11-06, branch feature/persistent-session-contracts)
Contratos confirmados por orden humana D3 (backend Azure):
- POST /api/v1/auth/refresh body {"refreshToken"} -> 200 {accessToken, refreshToken (rotacion obligatoria), tokenType, expiresIn}; 401 limpia sesion.
- GET /api/v1/users/me Bearer -> 200 {id, email, createdAt}; 401 dispara refresh single-flight + maximo 1 retry.
- POST /api/v1/auth/logout 204 (revoca access + familia refresh).
Implementacion verificada con MockWebServer (sin secretos):
- `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` BUILD SUCCESSFUL (54/54 unit tests verdes, 0 failures; PersistentSessionIntegrationTest 13/13).
- AuthApi.refresh + AuthApi.getCurrentUser + RefreshRequestDto + CurrentUserDto implementados.
- properties/universities/health siguen BLOCKED parcial (sin transcripcion en D3; no se implementan consumidores).

## Evidencia futura esperada
Transcripciones de properties/universities/health y, cuando el backend los confirme, refresh/me.
