# NV-16 — OFFLINE + SYNC

| Campo | Valor |
|---|---|
| Spec ID | NV-16 |
| STATUS | APPROVED FOR IMPLEMENTATION |
| AUTHORIZATION | APPROVED FOR IMPLEMENTATION (autorización humana explícita en sesión, alcance acotado) |
| Dependencias | NV-01, NV-09, NV-27, NV-20 |

## Problema
El usuario necesita usar la app con conexion intermitente y que sus cambios (favoritos, visitas, borradores) se sincronicen sin perdidas ni duplicados.

## Alcance
- Estrategia cache-first + network refresh para lecturas cacheables seguras: propiedades (listado/detalle), universidades, favoritos (lectura) y perfil/preferencias (lectura segura).
- Reutilización de repositorios/mappers/DTO-domain existentes y de infraestructura local existente (DataStore/PreferencesStore) para cache JSON; no se crea segunda base Room (no existe BD Room actual con entidades).
- Estado offline/stale mínimo en UI cuando hay cache utilizable; error/retry existente cuando no hay cache.
- Invalidación simple: respuesta backend válida reemplaza cache; logout/delete account limpia cache privada user-specific.

## No-alcance
- Cola de escrituras offline, sync en background, WorkManager workers/polling/wakeups (sin operaciones syncables con contrato real aprobado en este alcance).
- Persistencia de JWT, refresh token, FCM token, agreement codes, qrToken u otros secretos/PII sensible innecesaria.
- Cache/sync de requests, chat, visitas, notificaciones o agreements sin contrato/estrategia segura aprobada.
- Modificación de Backend, Docker, merge, force push o reescritura de módulos completos.

## Requisitos
- NV16-REQ-001: Toda entidad sincronizable tiene contrato confirmado en NV-27; sin contrato, solo cache de lectura.
- NV16-REQ-002: Cola de escrituras persistente, idempotente y con reintentos acotados (WorkManager con backoff).
- NV16-REQ-003: Politica de conflictos documentada y testeada (ultimo-escritor o servidor-autoritativo, segun contrato).

## Criterios de aceptacion
- Sin conexion, favoritos/lecturas funcionan desde cache; al reconectar, la cola se procesa sin duplicados.
- Conflicto simulado produce el resultado documentado.

## Riesgos
- Perdida de escrituras, duplicados, bateria/datos por reintentos agresivos.
- Divergencia local/remoto sin indicador al usuario.

## Impacto seguridad
- Datos en reposo cifrados cuando incluyan sesion/PII (NV-20); cola sin tokens en texto plano ni en logs.

## COST-001 ($0)
- Free tier: Room/WorkManager locales + backend existente. Limites: almacenamiento/bateria del dispositivo. Tarjeta: no. Riesgo cobro: ninguno. Mitigacion: sin servicios de sync comerciales. Alternativa: N/A.

## Relacion con otras specs
- NV-09 (favoritos), NV-10/NV-11 (escrituras), NV-20 (seguridad en reposo), NV-27 (contrato).

## Evidencia esperada
- Tests de cola/conflictos y pruebas sin conexion. Hoy: sin evidencia.
