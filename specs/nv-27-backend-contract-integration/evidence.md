# NV-27 — EVIDENCIA

Sin evidencia nueva todavia — BLOCKED, DO NOT EXECUTE.

## Hechos reales preservados (NV-04, 2026-11-05)
- register 201 Created con body exacto {"email","password"} (usuario de prueba qa.register.u2.1105@ejemplo.com; contrasena no registrada por seguridad).
- login 200: {"accessToken":"<JWT>","tokenType":"Bearer","expiresIn":900} (sin refreshToken ni user).
- logout con Bearer 204; sin sesion 401 (Problem Details).
- health/live 200 {"status":"Healthy"} (NV-04 §5).
- Contrato confirmado M-MOB-044: register/login/logout/properties/universities/health.
- Faltan: POST /api/v1/auth/refresh y GET /api/v1/users/me -> BLOCKED. No se inventaron endpoints.

## Evidencia futura esperada
Transcripciones de properties/universities/health y, cuando el backend los confirme, refresh/me.
