---
name: mobile-devsecops
description: Planifica seguridad y calidad en CI/CD con herramientas gratuitas o tier $0 para TheHouse Mobile, sin instalar nada antes de NV-02.
---

# Mobile DevSecOps

## Nombre

mobile-devsecops

## Objetivo

Planificar e integrar controles de seguridad y calidad en el ciclo de desarrollo Mobile, priorizando herramientas gratuitas y evitando costos.

## Cuándo utilizarlo

- Diseño de CI/CD.
- Selección de análisis estático o de dependencias.
- Definición de quality gates.
- Revisión de seguridad de la cadena de suministro.
- Antes de instalar herramientas o crear pipelines.

## Entradas

- Spec, plan y task activos, normalmente NV-02.
- Stack y estructura del proyecto.
- Requisitos de seguridad.
- Restricciones COST-001.
- Secretos y variables de CI disponibles.

## Procedimiento

1. Confirmar autorización; NV-02 no está autorizada actualmente.
2. Inventariar herramientas candidatas.
3. Evaluar licencia, free tier, límites y requisitos de tarjeta.
4. Definir quality gates mínimos.
5. Definir cache y reproducibilidad del pipeline.
6. Definir manejo de secretos.
7. Definir evidencias y retención.
8. Planificar pruebas y análisis.
9. Documentar riesgos y alternativas gratuitas.
10. No instalar hasta autorización explícita.

## Herramientas previstas

- Android Lint.
- Detekt.
- ktlint.
- Semgrep.
- SonarCloud cuando sea viable.
- Dependency scanning.
- MobSF cuando sea viable.
- GitHub Actions.

## Reglas

- Costo objetivo $0.
- No instalar herramientas todavía.
- No almacenar secretos en el repositorio.
- No usar acciones de terceros sin revisión.
- No publicar artefactos automáticamente.
- Todo pipeline debe ser reproducible.
- Los gates no se relajan para ocultar fallos.
- Documentar free tier y riesgo de cobro.
- Si no hay viabilidad $0, reportar BLOCKED.

## Restricciones

- No crear workflows sin spec autorizada.
- No agregar plugins de análisis aún.
- No habilitar SonarCloud o MobSF sin evaluación.
- No usar servicios que requieran tarjeta sin decisión humana.

## Criterios de finalización

- Plan de herramientas documentado.
- Evaluación COST-001 completa.
- Quality gates definidos.
- Gestión de secretos definida.
- Evidencia esperada y riesgos documentados.
- Ninguna herramienta instalada sin autorización.
