---
name: networking
description: Valida Retrofit, OkHttp, Kotlin Serialization, timeouts, errores HTTP, cancelación y logging seguro.
---

# Networking

## Nombre

networking

## Objetivo

Validar que la capa de red sea segura, tipada, cancelable, observable y alineada con los boundaries de TheHouse Mobile.

## Cuándo utilizarlo

- Diseño o revisión de clientes HTTP.
- Revisión de DTO, serialización o mapeo.
- Configuración de timeouts e interceptores.
- Revisión de errores y cancelación.
- Integración de una API externa.

## Entradas

- Spec, plan y task activos.
- Contrato de API.
- DTO y modelos de dominio.
- Configuración Retrofit/OkHttp.
- Requisitos de seguridad y COST-001.

## Procedimiento

1. Confirmar autorización.
2. Verificar que la API y los modelos tengan spec.
3. Revisar Retrofit y Kotlin Serialization.
4. Revisar timeouts de conexión, lectura y escritura.
5. Revisar semántica HTTP y mapeo de errores.
6. Revisar cancelación y propagación.
7. Revisar interceptores y logging.
8. Confirmar que no se registran tokens ni datos sensibles.
9. Revisar HTTPS y network security.
10. Revisar caching y reintentos.
11. Evaluar COST-001.
12. Emitir dictamen.

## Reglas

- La UI no consume Retrofit directamente.
- DTO en Data; no se filtran a Presentation.
- Timeouts explícitos.
- Errores de red mapeados a errores de dominio.
- Operaciones cancelables con Coroutines.
- No loggear Authorization, cookies, bodies sensibles ni PII.
- Logging HTTP deshabilitado o seguro en producción.
- No introducir endpoints funcionales sin spec.
- APIs externas requieren COST-001.

## Restricciones

- No crear modelos funcionales sin spec.
- No usar APIs sin HTTPS.
- No confiar en datos del cliente para autorización.
- No agregar dependencias de red no aprobadas.
- No registrar secretos.

## Criterios de finalización

- Configuración de red validada.
- Serialización y mapeo revisados.
- Timeouts, errores y cancelación cubiertos.
- Logging seguro confirmado.
- Dictamen y riesgos documentados.
