# NV-28 — LANDING PAGE / EXTERNAL LINKS

| Campo | Valor |
|---|---|
| Spec ID | NV-28 |
| STATUS | PLANNED |
| AUTHORIZATION | DO NOT EXECUTE |
| Dependencias | NV-15 (URL_AVISO_PRIVACIDAD), NV-03 (store listing) |

## Problema
La app y Play Store necesitan enlaces externos reales (aviso de privacidad, soporte, landing) sin URLs inventadas ni enlaces rotos.

## Alcance
- Gestion de enlaces externos: apertura con Custom Tabs/intent seguro, validacion de URL y pantalla de fallback sin conexion.
- Inventario de enlaces (privacidad, soporte, landing) con URLs reales provistas por el humano.

## No-alcance
- Crear/hostear la landing page (fuera de la app movil) salvo que una spec futura la autorice.
- URLs placeholder o inventadas (BLOCKED-PRIVACY-URL hasta URL real).

## Requisitos
- NV28-REQ-001: Toda URL proviene de configuracion no secreta (BuildConfig/remoto si se justifica); sin hardcodeo en UI.
- NV28-REQ-002: Enlaces abren en navegador externo/Custom Tab; si fallan, mensaje accesible sin crash.
- NV28-REQ-003: BLOCKED-PRIVACY-URL se mantiene hasta que el humano provea la URL real del aviso de privacidad.

## Criterios de aceptacion
- Inventario de enlaces con URL real verificada (HTTP 200) o BLOCKED documentado.
- Sin enlaces a dominios placeholder (example.invalid, etc.) en UI.

## Riesgos
- Enlaces rotos o a dominios no controlados; URLs con tracking no declarado (Data Safety).
- Deep links externos que bypassean auth.

## Impacto seguridad
- Solo HTTPS; sin tokens/PII en parametros de URL; intents explicitos.

## COST-001 ($0)
- Free tier: hosting estatico gratuito o el ya existente (documentar limites antes de crear nada). Limites: ancho de banda/almacenamiento del tier. Tarjeta: documentar si el hosting la exige (entonces BLOCKED para crear hosting nuevo). Riesgo cobro: exceder cuota. Mitigacion: sin hosting nuevo en esta spec; reutilizar dominio existente. Alternativa: enlaces a paginas ya publicadas por el backend.

## Relacion con otras specs
- NV-15 (privacidad), NV-03 (store listing), NV-21 (intents/deep links), NV-23 (sin tracking oculto).

## Evidencia esperada
- Inventario de URLs verificadas. Hoy: sin evidencia.
