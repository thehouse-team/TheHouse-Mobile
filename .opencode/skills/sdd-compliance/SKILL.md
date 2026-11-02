---
name: sdd-compliance
description: Valida gobernanza SDD, jerarquía documental, autorización, alcance y trazabilidad en TheHouse Mobile.
---

# SDD Compliance

## Nombre

sdd-compliance

## Objetivo

Verificar que todo trabajo en TheHouse Mobile cumpla la Constitución, AGENTS, memoria y la spec activa, con autorización y trazabilidad completas.

## Cuándo utilizarlo

- Antes de implementar cualquier task.
- Antes de aceptar una spec o plan.
- Antes de revisar un cambio de alcance.
- Antes de declarar terminada una tarea.
- Cuando exista duda sobre autorización o jerarquía documental.

## Entradas

- `constitution.md`.
- `AGENTS.md`.
- `memoria.md`.
- Spec, plan y task activos.
- Solicitud humana de trabajo.
- Archivos o cambios propuestos.
- Evidencia de validación.

## Procedimiento

1. Leer la jerarquía completa.
2. Identificar spec, plan y task activos.
3. Exigir la declaración:
   ```text
   SPEC ACTIVA:
   PLAN ACTIVO:
   TASK ACTIVO:
   ESTADO AUTORIZADO:
   ```
4. Confirmar aprobación humana y estado autorizado.
5. Revisar que la task pertenezca a la spec activa.
6. Confirmar que la spec no ejecuta tareas de otra spec.
7. Revisar que cada requisito tenga fase de plan y task.
8. Verificar que el cambio no amplía alcance silenciosamente.
9. Revisar prohibiciones constitucionales y COST-001.
10. Emitir `PASS`, `PASS WITH WARNINGS` o `BLOCKED`.
11. Documentar evidencia y acción requerida.

## Reglas

- La Constitución prevalece sobre todo.
- Sin spec, plan y task aprobados no se implementa.
- Una spec no puede ejecutar tareas de otra.
- Un agente no puede autorizar su propio trabajo.
- Todo requisito debe ser trazable a plan y task.
- Todo cambio de alcance requiere actualización aprobada.
- Los secretos y servicios externos se revisan siempre.
- Las tareas no se marcan completadas sin evidencia.

## Restricciones

- No implementar código.
- No modificar specs ni Constitución para desbloquear trabajo.
- No aprobar NV-01, NV-02 o NV-03.
- No aceptar autorizaciones verbales implícitas.
- No ignorar riesgos de seguridad o costo.

## Criterios de finalización

- Dictamen emitido.
- Jerarquía documental verificada.
- Autorización y alcance confirmados o bloqueados.
- Trazabilidad requisito → plan → task verificada.
- Hallazgos y acción requerida documentados.
