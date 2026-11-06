---
name: ml-generative-ai
description: Planifica y valida ML e IA generativa en TheHouse Mobile, solo viabilidad, costo y gobernanza, sin integrar modelos.
---

# ML Generative AI

## Nombre

ml-generative-ai

## Objetivo

Planificar y validar casos de uso de ML e IA generativa en TheHouse Mobile: viabilidad, costo $0, moderación y gobernanza, sin integrar modelos ni SDKs.

## Cuándo utilizarlo

- Al evaluar un caso de uso de ML o IA generativa.
- Al revisar qué datos personales se enviarían a un proveedor de IA.
- Al revisar límites de uso y riesgo de cobro.
- Al revisar moderación de contenido generado por IA.

## Entradas

- Caso de uso propuesto y datos involucrados.
- `AGENTS.md` (sin ML/IA configurados), `constitution.md`, `memoria.md`.
- Evaluaciones COST-001 existentes.

## Procedimiento

1. Leer la jerarquía y confirmar spec/plan/task autorizados.
2. Definir el caso de uso y su valor.
3. Identificar datos personales involucrados y su base legal.
4. Evaluar costo: free tier, límites, tarjeta requerida, riesgo de cobro, mecanismo anti-sobrecosto y alternativa $0.
5. Revisar moderación de contenido generado.
6. Emitir `PASS`, `PASS WITH WARNINGS` o `BLOCKED` con acción requerida.

## Reglas

- Ninguna skill sustituye spec ni autorización.
- Sin spec, no se integra ningún modelo ni SDK.
- No inventar modelos, proveedores ni endpoints.
- No guardar API keys ni secretos en el repositorio.
- Sin viabilidad de costo $0, reportar `BLOCKED`.

## Criterios de finalización

- Caso de uso y datos documentados.
- Evaluación COST-001 completa.
- Dictamen y acción requerida documentados.

## Specs relacionadas

- NV-17 (Smart Match / ML / IA) — PLANNED.
- NV-03 (Release / Play) — políticas de IA de Google Play.
