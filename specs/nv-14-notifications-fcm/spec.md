# NV-14 — NOTIFICATIONS / FCM

| Campo | Valor |
|---|---|
| Spec ID | NV-14 |
| STATUS | PLANNED |
| AUTHORIZATION | DO NOT EXECUTE |
| Dependencias | NV-01, NV-04, NV-27, NV-03 (Data Safety/permisos) |

## Problema
El usuario necesita recibir avisos (visitas, chat, cierre) aunque no tenga la app abierta.

## Alcance
- Permiso de notificaciones justificado, registro de token FCM y manejo de notificaciones (foreground/background/terminada).
- Firebase MCP permanece deshabilitado hasta esta spec (AGENTS.md §18).

## No-alcance
- Remitente/backend de notificaciones sin contrato NV-27; campanas de marketing.
- Activar Firebase/FCM sin autorizacion de esta spec.

## Requisitos
- NV14-REQ-001: Configuracion Firebase/FCM solo tras autorizacion y COST-001 aprobado.
- NV14-REQ-002: El token FCM se registra solo con sesion valida y se desregistra en logout.
- NV14-REQ-003: Datos sensibles nunca en payloads de notificacion; deep links validados.

## Criterios de aceptacion
- Sin permiso, la app funciona sin notificaciones.
- Logout desregistra el token; notificaciones no muestran contenido sensible en pantalla de bloqueo.

## Riesgos
- Costo/limites de FCM, spam de notificaciones, filtrado de PII en payloads.
- Permiso de notificaciones denegado por el usuario.

## Impacto seguridad
- Token FCM tratado como identificador sensible; HTTPS; sin secretos versionados; Data Safety actualizada (NV-03).

## COST-001 ($0)
- Free tier: FCM sin costo en su tier gratuito documentado. Limites: cuota de mensajes/dispositivos del proveedor. Tarjeta: documentar si Firebase la exige (si la exige para este uso y no hay alternativa $0, BLOCKED). Riesgo cobro: exceder cuota con envios masivos. Mitigacion: envios solo transaccionales, sin campanas. Alternativa: polling local o bandeja in-app sin push.

## Relacion con otras specs
- NV-10/NV-11/NV-12 (eventos), NV-03 (Data Safety), NV-15 (privacidad), NV-27 (contrato).

## Evidencia esperada
- Flujo de permiso/token/logout y prueba de notificacion real. Hoy: sin evidencia.
