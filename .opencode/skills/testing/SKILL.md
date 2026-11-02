---
name: testing
description: Planifica y valida unit, ViewModel, repository, Compose UI, instrumented, integration y security tests, cobertura y quality gates.
---

# Testing

## Nombre

testing

## Objetivo

Definir y validar una estrategia de pruebas trazable a requisitos, con evidencia real y sin pruebas artificiales.

## Cuándo utilizarlo

- Planificación de pruebas de una spec.
- Revisión de cobertura de requisitos.
- Cambios en ViewModels, repositorios o UI.
- Configuración de quality gates.
- Investigación de fallos.

## Entradas

- Spec, plan y task activos.
- Requisitos y criterios de aceptación.
- Código y contratos involucrados.
- Herramientas disponibles.
- Resultados de build y lint.

## Procedimiento

1. Confirmar autorización.
2. Mapear requisitos a casos de prueba.
3. Seleccionar niveles adecuados.
4. Definir datos, dobles y dispatchers.
5. Cubrir éxito, error, borde y cancelación.
6. Cubrir accesibilidad y semántica en Compose.
7. Cubrir seguridad cuando aplique.
8. Ejecutar las validaciones reales.
9. Reportar resultados y fallos.
10. Actualizar trazabilidad.
11. Emitir quality gate.

## Reglas

- JUnit como base.
- Tests deterministas.
- `MainDispatcherRule` y dispatchers controlados para Coroutines.
- Repositorios con fakes controlados.
- Compose UI tests con semántica y tags estables.
- Instrumented tests sin dependencias frágiles.
- No tests siempre verdaderos.
- No modificar código para forzar un test.
- Reportar resultados reales.
- No instalar frameworks sin autorización.

## Restricciones

- No probar funcionalidad no aprobada.
- No modificar tests existentes sin autorización.
- No ocultar fallos.
- No reducir quality gates.
- No crear datos reales sensibles en tests.

## Criterios de finalización

- Requisitos cubiertos por pruebas.
- Resultados reales registrados.
- Fallos y riesgos documentados.
- Trazabilidad actualizada.
- Quality gate emitido.
