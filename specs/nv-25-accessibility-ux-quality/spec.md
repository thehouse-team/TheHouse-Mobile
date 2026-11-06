# NV-25 — ACCESSIBILITY / UX QUALITY

| Campo | Valor |
|---|---|
| Spec ID | NV-25 |
| STATUS | PLANNED |
| AUTHORIZATION | DO NOT EXECUTE |
| Dependencias | NV-01 (Compose), NV-04 (baseline a11y), NV-19 |

## Problema
Todas las pantallas futuras deben ser usables con lector de pantalla, tacto amplio y contraste suficiente desde el primer diseno.

## Alcance
- Estandar de accesibilidad: targets >=44dp, contentDescription, semantics, orden de foco, contraste y tamaños de fuente escalables.
- Revision UX: jerarquia, estados de error en espanol y feedback consistente.

## No-alcance
- Rediseno visual completo; cambios de marca sin autorizacion.
- Cumplimiento legal formal sin revision experta (no se afirma WCAG sin auditoria).

## Requisitos
- NV25-REQ-001: Todo control interactivo >=44dp con semantics/contentDescription (baseline ya aplicado en NV-04).
- NV25-REQ-002: Errores/estados anunciables (live regions/semantics) y navegables por teclado/lector.
- NV25-REQ-003: Rating, toggle y formularios operables sin punteria fina (NV-13/NV-09).

## Criterios de aceptacion
- Recorrido completo de cada pantalla con TalkBack sin callejones sin salida.
- Lint/accesibilidad sin errores nuevos; revision manual documentada.

## Riesgos
- Accesibilidad como parche final; contraste de temas personalizados futuros.
- Textos hardcodeados sin escalado de fuente.

## Impacto seguridad
- Mensajes de error accesibles sin filtrar detalle tecnico sensible (NV-20).

## COST-001 ($0)
- Free tier: herramientas locales (Accessibility Scanner, TalkBack, lint). Limites: N/A. Tarjeta: no. Riesgo cobro: ninguno. Mitigacion: sin auditorias de pago. Alternativa: N/A.

## Relacion con otras specs
- NV-05..NV-14 (pantallas), NV-19 (UI tests de semantics), NV-24 (performance sin regresion a11y).

## Evidencia esperada
- Checklist por pantalla con TalkBack y capturas. Hoy: sin evidencia.
