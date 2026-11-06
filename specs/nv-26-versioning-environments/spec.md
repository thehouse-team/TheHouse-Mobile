# NV-26 — VERSIONING / ENVIRONMENTS

| Campo | Valor |
|---|---|
| Spec ID | NV-26 |
| STATUS | PLANNED |
| AUTHORIZATION | DO NOT EXECUTE |
| Dependencias | NV-03 (VERSIONING.md), NV-01 (build types), NV-27 |

## Problema
La app necesita versionado monotono y entornos (dev/staging/prod) sin secretos ni configuracion duplicada en codigo.

## Alcance
- Politica versionName/versionCode (VERSIONING.md) y estrategia de entornos con BuildConfig/flavors si se justifica.
- Configuracion por entorno sin secretos versionados.

## No-alcance
- Cambiar toolchain/applicationId sin autorizacion NV-03.
- URLs de entornos inventadas: solo la base URL real ya configurada o las que NV-27 confirme.

## Requisitos
- NV26-REQ-001: versionCode monotono creciente y versionName segun VERSIONING.md; sin reutilizacion.
- NV26-REQ-002: Unica fuente de API_BASE_URL por entorno (BuildConfig); sin hardcodeo en Kotlin (NV-04).
- NV26-REQ-003: Logging HTTP solo en debug; release sin logging (NV-01/NV-03).

## Criterios de aceptacion
- Cada release identifica versionCode/versionName/entorno sin ambiguedad.
- Ningun secreto en configuracion versionada.

## Riesgos
- Colision de versionCode en Play; entornos que apuntan a prod por error.
- Deriva entre entornos sin matriz de configuracion.

## Impacto seguridad
- Entornos no productivos sin datos reales; HTTPS en todos; sin credenciales en BuildConfig.

## COST-001 ($0)
- Free tier: configuracion local de Gradle/BuildConfig. Limites: N/A. Tarjeta: no. Riesgo cobro: ninguno. Mitigacion: sin servicios de config remota de pago. Alternativa: N/A.

## Relacion con otras specs
- NV-03 (release/versionado), NV-18 (pipeline por entorno), NV-27 (URLs por entorno).

## Evidencia esperada
- Matriz de entornos y politica de versionado. Hoy: sin evidencia.
