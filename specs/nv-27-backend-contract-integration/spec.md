# NV-27 — BACKEND CONTRACT INTEGRATION

| Campo | Valor |
|---|---|
| Spec ID | NV-27 |
| STATUS | BLOCKED |
| AUTHORIZATION | BLOCKED por contrato (DO NOT EXECUTE) |
| Dependencias | NV-04 (auth), NV-06..NV-14 (consumidores) |

## Problema
La app solo puede integrar endpoints cuyo contrato este confirmado con evidencia real contra Azure; hoy faltan dos endpoints criticos.

## Alcance
- Integrar UNICAMENTE el contrato confirmado M-MOB-044: register/login/logout/properties/universities/health.
- Documentar el contrato exacto (metodo, ruta, body, respuesta, codigos) por endpoint.

## No-alcance
- NO inventar endpoints. En particular NO implementar `POST /api/v1/auth/refresh` ni `GET /api/v1/users/me` (sin contrato).
- NO ampliar el contrato con campos no verificados.

## Requisitos
- NV27-REQ-001: register `POST /api/v1/auth/register` body exacto `{"email","password"}` (sin name ni consentimientos; password 12-128) -> 201 Created.
- NV27-REQ-002: login `POST /api/v1/auth/login` -> 200 con `{"accessToken","tokenType":"Bearer","expiresIn":900}` (sin refreshToken ni user). Logout `POST /api/v1/auth/logout` con Bearer -> 204; sin sesion -> 401 (NV-04 observo 400 en una prueba temprana; el hecho confirmado posterior es 401 sin sesion segun evidencia NV-04 §7).
- NV27-REQ-003: properties/universities/health solo con sus campos reales verificados; hasta entonces, consumidores (NV-06..NV-08) en BLOCKED parcial.
- NV27-REQ-004: `POST /api/v1/auth/refresh` y `GET /api/v1/users/me` permanecen BLOCKED hasta contrato confirmado con prueba real.

## Criterios de aceptacion
- Cada endpoint integrado tiene transcripcion de request/response real (sin secretos).
- Ningun endpoint fuera de M-MOB-044 esta implementado en la app.

## Riesgos
- Integrar contra supuestos y romper auth/sesion; campos de properties/universities no confirmados.
- Deriva entre contrato documentado y backend real.

## Impacto seguridad
- Tokens nunca en logs; Problem Details sin filtrar detalle interno; HTTPS.

## COST-001 ($0)
- Free tier: backend Azure en su plan vigente. Limites: los del plan actual. Tarjeta: la ya contratada (no nueva). Riesgo cobro: sobreuso de llamadas. Mitigacion: cache, sin polling agresivo, sin servicios nuevos. Alternativa: N/A.

## Relacion con otras specs
- NV-04 (auth), NV-06/NV-07/NV-08 (universities/properties), NV-09..NV-14 (BLOCKED sin contrato), NV-16 (sync).

## Evidencia esperada
- Transcripciones reales por endpoint. Hoy: hechos de NV-04 preservados; properties/universities/health sin transcripcion en esta conversacion.
