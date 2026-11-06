# NV-20 — SECURITY / OWASP MASVS

| Campo | Valor |
|---|---|
| Spec ID | NV-20 |
| STATUS | PLANNED |
| AUTHORIZATION | DO NOT EXECUTE |
| Dependencias | NV-01, NV-04, NV-21, NV-02 |

## Problema
La app maneja sesiones, tokens, PII y contenido de usuarios; necesita controles verificables alineados con OWASP MASVS antes de cualquier release.

## Alcance
- Evaluacion MASVS por dominio (almacenamiento, cripto, auth, red, plataforma, codigo, resiliencia, privacidad).
- Controles: Keystore/AES-GCM, HTTPS/cleartext off, backup rules, logging seguro, sin secretos.

## No-alcance
- Pentest externo de pago; MobSF/SonarCloud como gate obligatorio (NV-02: evaluacion).
- Cambios de arquitectura fuera de esta spec.

## Requisitos
- NV20-REQ-001: Matriz MASVS con nivel objetivo y estado por control (PASS/FAIL/NA justificado).
- NV20-REQ-002: Tokens solo en memoria + refresh cifrado (NV-04); nada de tokens en logs/backups.
- NV20-REQ-003: Hallazgos Critical/High bloquean release salvo excepcion humana documentada (NV-02 §5.5).

## Criterios de aceptacion
- Matriz MASVS aprobada por sdd-guardian/security-devsecops-agent.
- 0 secretos en repo/logs/artefactos (secret scan).

## Riesgos
- Falsa sensacion de seguridad; controles no verificados; dependencias vulnerables.
- Secretos en pipelines futuros (NV-18).

## Impacto seguridad
- Esta spec ES el control de seguridad transversal; sus hallazgos pueden BLOCKED cualquier release.

## COST-001 ($0)
- Free tier: revision manual + herramientas OSS gratuitas (secret/dependency scan). Limites: N/A. Tarjeta: no. Riesgo cobro: pentest/servicios de pago. Mitigacion: prohibido contratar servicios de pago; MobSF solo si es viable $0. Alternativa: revision interna documentada.

## Relacion con otras specs
- NV-02 (secret/dependency scan), NV-21 (permisos/plataforma), NV-18 (gates), NV-15 (privacidad).

## Evidencia esperada
- Matriz MASVS, scans y remediation. Hoy: sin evidencia.
