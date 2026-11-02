---
name: local-persistence-sync
description: Valida Room, DataStore, WorkManager, caché offline, sincronización, reintentos, conflictos y lifecycle de datos.
---

# Local Persistence & Sync

## Nombre

local-persistence-sync

## Objetivo

Validar que la persistencia local y la sincronización sean coherentes, seguras, idempotentes y trazables cuando exista un dominio aprobado.

## Cuándo utilizarlo

- Diseño de Room o DataStore.
- Definición de caché offline.
- Diseño de workers o sync.
- Revisión de conflictos y reintentos.
- Cambios en lifecycle de datos locales.

## Entradas

- Spec, plan y task activos.
- Modelo de dominio y entidades.
- Estrategia de persistencia.
- Requisitos de sync y conectividad.
- Política de retención y seguridad.

## Procedimiento

1. Confirmar autorización.
2. Verificar que entidades y casos de uso existan en spec.
3. Definir fuente de verdad.
4. Revisar esquema Room y migraciones.
5. Revisar DataStore y datos sensibles.
6. Revisar caché y expiración.
7. Revisar WorkManager: triggers, constraints, retry y backoff.
8. Revisar idempotencia y conflictos.
9. Revisar recuperación de conectividad.
10. Revisar observabilidad sin PII.
11. Revisar pruebas de fallo y reintento.
12. Emitir dictamen.

## Reglas

- No crear entidades sin spec.
- No configurar KSP hasta que Room real lo requiera.
- DataStore no almacena secretos sin protección.
- La UI no accede a Room ni DataStore.
- Sync debe evitar duplicados y pérdida de datos.
- Conflictos deben tener política explícita.
- Retries con backoff y límites.
- No sincronizar datos sensibles sin protección.
- No inventar sync antes del dominio.
- Las migraciones deben ser seguras y probadas.

## Restricciones

- No implementar workers funcionales sin spec.
- No introducir una segunda fuente de verdad sin justificación.
- No borrar datos sin política de retención.
- No asumir conectividad permanente.
- No usar `allowMainThreadQueries`.

## Criterios de finalización

- Fuente de verdad y esquema validados.
- Retry, conflictos y conectividad definidos.
- Seguridad y lifecycle revisados.
- Pruebas y evidencia planificadas.
- Dictamen y riesgos documentados.
