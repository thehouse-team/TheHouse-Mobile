---
name: compose-quality
description: Valida calidad de Jetpack Compose, state hoisting, UI State, eventos, recomposición, accesibilidad, semántica, previews y reutilización.
---

# Compose Quality

## Nombre

compose-quality

## Objetivo

Asegurar que los componentes Compose de TheHouse Mobile sean stateless cuando corresponda, accesibles, reutilizables y correctos frente a recomposición y lifecycle.

## Cuándo utilizarlo

- Revisión de un Composable.
- Diseño de UI State o eventos.
- Validación de accesibilidad.
- Revisión de previews.
- Detección de lógica de negocio en UI.

## Entradas

- Spec, plan y task activos.
- Composable y ViewModel involucrados.
- UI State y eventos.
- Previews.
- Requisitos de accesibilidad.

## Procedimiento

1. Confirmar autorización y que exista spec para la pantalla.
2. Identificar UI State, eventos y fuentes de verdad.
3. Verificar state hoisting.
4. Verificar que el Composable no haga I/O ni lógica de negocio.
5. Revisar estabilidad y recomposición.
6. Revisar `remember` y `rememberSaveable`.
7. Revisar accesibilidad: roles, labels, contraste y tamaños.
8. Revisar semántica y testing tags.
9. Revisar previews de estados.
10. Revisar reutilización y Material 3.
11. Revisar adaptabilidad.
12. Emitir dictamen.

## Reglas

- UI State explícito.
- Eventos como callbacks.
- Composables stateless por defecto.
- Sin acceso a Retrofit, Room, DataStore ni Firebase.
- `collectAsStateWithLifecycle` para Flow.
- Evitar `remember` para estado que debe sobrevivir.
- Accesibilidad obligatoria.
- Previews útiles y sin dependencias frágiles.
- Material 3 y componentes consistentes.
- No crear pantallas sin spec.

## Restricciones

- No implementar funcionalidad de negocio.
- No modificar `MainActivity.kt` ni `ui/theme/` sin autorización.
- No agregar librerías UI no aprobadas.
- No ocultar estados de loading/error/empty.

## Criterios de finalización

- State hoisting validado.
- UI State y eventos claros.
- Accesibilidad y semántica revisadas.
- Previews y reutilización evaluadas.
- Hallazgos y dictamen documentados.
