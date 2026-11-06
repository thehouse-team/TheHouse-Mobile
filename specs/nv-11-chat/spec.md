# NV-11 — CHAT

| Campo | Valor |
|---|---|
| Spec ID | NV-11 |
| STATUS | PLANNED |
| AUTHORIZATION | DO NOT EXECUTE |
| Dependencias | NV-01, NV-04, NV-08, NV-27, NV-22 (moderacion) |

## Problema
El usuario necesita comunicarse con el anunciante/propietario desde la app sobre una propiedad concreta.

## Alcance
- Hilo de conversacion por propiedad/anunciante y envio de mensajes.
- Moderacion de contenido y reporte (NV-22) como requisito previo.

## No-alcance
- Tiempo real push (NV-14), llamadas de voz/video, adjuntos sin contrato.
- Endpoints de chat no confirmados: sin contrato, BLOCKED.

## Requisitos
- NV11-REQ-001: Solo endpoints/campos confirmados por NV-27.
- NV11-REQ-002: Mensajes con estado local (enviando/enviado/error) y reintento.
- NV11-REQ-003: Sin PII innecesaria en logs; bloqueo/reporte accesible.

## Criterios de aceptacion
- Mensaje enviado aparece en el hilo; error de red permite reintento sin duplicar.
- Usuario bloqueado/reportado no recibe contenido moderado pendiente.

## Riesgos
- Spam/abuso sin moderacion; polling con costo de bateria/datos.
- Tiempo real (WebSocket/FCM) sin contrato ni COST-001.

## Impacto seguridad
- Contenido de chat es dato sensible: cifrado en transito (HTTPS/WSS), minimizacion, sin logs de contenido.

## COST-001 ($0)
- Free tier: backend existente (polling) o FCM gratuito si NV-14 lo demuestra. Limites: cuotas de mensajes/polling. Tarjeta: no nueva (documentar si un proveedor la exige; entonces BLOCKED). Riesgo cobro: exceder cuota de mensajeria. Mitigacion: polling con backoff, sin servicios de pago. Alternativa: chat asincrono por backend existente.

## Relacion con otras specs
- NV-08 (contexto), NV-14 (push), NV-22 (moderacion), NV-27 (contrato).

## Evidencia esperada
- Tests de envio/reintento y moderacion. Hoy: sin evidencia.
