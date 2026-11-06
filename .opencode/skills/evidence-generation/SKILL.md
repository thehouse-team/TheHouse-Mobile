---
name: evidence-generation
description: Genera evidencia auditable de tasks en TheHouse Mobile (comandos, salidas y artefactos); BLOCKED nunca equivale a DONE.
---

# Evidence Generation

## Nombre

evidence-generation

## Objetivo

Generar y verificar la evidencia auditable de cada task cerrada en TheHouse Mobile: comando, salida real y artefacto, con trazabilidad completa.

## Cuándo utilizarlo

- Antes de cerrar cualquier task.
- Al revisar evidencia de validaciones (build, test, lint).
- Al auditar tareas declaradas como terminadas.
- Al documentar decisiones en `memoria.md`.

## Entradas

- Task/plan/spec cerrados o propuestos.
- Reportes y salidas reales de validaciones.
- `AGENTS.md`, `constitution.md`, `memoria.md`.

## Procedimiento

1. Identificar la task, su plan y su spec.
2. Listar cada validación requerida y su comando.
3. Registrar salida real y artefacto; nunca afirmaciones sin ejecución.
4. Contrastar evidencia con requisito → fase → task.
5. Verificar que `BLOCKED` no se cierre como `DONE`.
6. Emitir `PASS`, `PASS WITH WARNINGS` o `BLOCKED` con acción requerida.

## Reglas

- Ninguna skill sustituye spec ni autorización.
- No afirmar pruebas no ejecutadas.
- No crear código o tests artificiales para forzar un resultado.
- No ocultar fallos; reportar resultados reales.

## Criterios de finalización

- Evidencia por requisito/plan/task documentada.
- Trazabilidad verificada.
- Dictamen emitido.

## Specs relacionadas

- Todas las specs NV-01..NV-29.
