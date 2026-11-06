---
name: accessibility
description: Valida accesibilidad en TheHouse Mobile (TalkBack, foco, contraste, tamaños y navegación accesible en Compose).
---

# Accessibility

## Nombre

accessibility

## Objetivo

Validar la accesibilidad de pantallas Compose de TheHouse Mobile: semántica, TalkBack, foco, contraste, tamaños y navegación accesible.

## Cuándo utilizarlo

- Al crear o modificar una pantalla Compose.
- Al revisar previews y comportamiento con TalkBack.
- Al revisar formularios, errores y avisos.
- Antes de una release.

## Entradas

- Pantallas Compose y sus previews.
- `AGENTS.md`, `constitution.md`, `memoria.md`.
- Spec/plan/task de accesibilidad autorizados, si existen.

## Procedimiento

1. Leer la jerarquía y confirmar spec/plan/task autorizados.
2. Revisar `contentDescription`, `Role`, estados y orden de foco.
3. Validar contraste, objetivo táctil mínimo y escalado de fuente.
4. Detectar acciones sin semántica y avisos no anunciados.
5. Revisar que errores de formulario sean perceptibles por TalkBack.
6. Emitir `PASS`, `PASS WITH WARNINGS` o `BLOCKED` con acción requerida.

## Reglas

- Ninguna skill sustituye spec ni autorización.
- Solo accesibilidad: no sustituye la revisión general de calidad Compose.
- No agregar permisos ni servicios externos para accesibilidad sin spec.
- No guardar secretos ni datos personales en el repositorio.

## Criterios de finalización

- Barreras por pantalla con severidad documentadas.
- Acciones de corrección requeridas documentadas.
- Dictamen emitido.

## Specs relacionadas

- NV-25 (Accessibility / UX Quality) — PLANNED.
- NV-01 (Foundation) — tema base y previews.
