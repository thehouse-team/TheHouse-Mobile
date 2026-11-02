---
name: release-play
description: Valida versionCode, versionName, AAB, signing, secretos, release, Play Console, privacidad, permisos y COST-001 sin publicar.
---

# Release & Play

## Nombre

release-play

## Objetivo

Preparar y validar un release Android seguro y trazable para Google Play, sin publicar ni exponer secretos.

## Cuándo utilizarlo

- Preparación de versión.
- Generación de AAB.
- Configuración de signing.
- Revisión de privacidad, permisos o Data Safety.
- Preparación de Play Console.
- Diseño de pipeline de release.

## Entradas

- Spec, plan y task activos, normalmente NV-03.
- `versionCode` y `versionName`.
- Configuración de build y signing.
- Política de privacidad.
- Permisos y manejo de datos.
- Evaluación COST-001.

## Procedimiento

1. Confirmar autorización de release.
2. Verificar `versionCode` y `versionName`.
3. Verificar signing fuera del repositorio.
4. Verificar build release y minificación.
5. Ejecutar tests, lint y quality gates.
6. Construir AAB solo si está autorizado.
7. Revisar permisos y disclosure.
8. Revisar privacy notice y Data Safety.
9. Revisar servicios externos contra COST-001.
10. Preparar checklist de Play Console.
11. Reportar evidencia sin publicar.

## Reglas

- `versionCode` entero y creciente.
- `versionName` coherente.
- AAB obligatorio para Play.
- Signing secrets fuera del repositorio.
- Debug y release separados.
- No publicar sin autorización humana.
- No cambiar el toolchain en el release.
- No relajar permisos o privacidad.
- No omitir tests o lint.
- No asumir costo $0 sin evidencia.

## Restricciones

- No ejecutar publicación automática.
- No cargar AAB a Play Console sin autorización.
- No versionar keystore ni passwords.
- No usar signing de debug para release.
- No ocultar warnings críticos.

## Criterios de finalización

- Versión y artefacto validados.
- Signing y secretos verificados.
- Calidad y pruebas aprobadas.
- Privacidad, permisos y Data Safety revisados.
- COST-001 evaluado.
- Estado reportado; publicación no ejecutada.
