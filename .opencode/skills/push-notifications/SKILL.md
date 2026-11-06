---
name: push-notifications
description: Planifica y valida notificaciones push (FCM) en TheHouse Mobile, solo diseño y gobernanza, sin implementar Firebase.
---

# Push Notifications

## Nombre

push-notifications

## Objetivo

Planificar el diseño y la gobernanza de notificaciones push en TheHouse Mobile (casos de uso, permiso, canales, preferencias) sin implementar FCM.

## Cuándo utilizarlo

- Al diseñar una spec de notificaciones.
- Al revisar permiso `POST_NOTIFICATIONS` y canales.
- Al revisar opt-in/opt-out y persistencia de preferencias.
- Al revisar deep links asociados a notificaciones.

## Entradas

- Spec/plan/task de notificaciones (inexistente hasta su spec).
- `AGENTS.md` (§18 — Firebase MCP deshabilitado), `constitution.md`, `memoria.md`.

## Procedimiento

1. Leer la jerarquía; sin spec autorizada de notificaciones, detener con `BLOCKED`.
2. Definir casos de uso y categorías de mensajes.
3. Revisar permiso, canales y agrupación.
4. Validar opt-in/opt-out y persistencia de preferencias.
5. Confirmar que ningún payload contenga datos personales sensibles.
6. Emitir `PASS`, `PASS WITH WARNINGS` o `BLOCKED` con acción requerida.

## Reglas

- Ninguna skill sustituye spec ni autorización.
- Firebase MCP permanece deshabilitado/no usado hasta la spec de notificaciones (FCM) autorizada.
- No inventar endpoints ni topics de backend.
- No guardar claves FCM ni service accounts en el repositorio.

## Criterios de finalización

- Matriz caso de uso → permiso/canal/preferencia documentada.
- Riesgos y acción requerida documentados.
- Dictamen emitido.

## Specs relacionadas

- NV-14 (Notifications / FCM) � PLANNED.
- NV-04 (Auth + Onboarding) — permisos post-registro.
