---
name: android-architecture
description: Valida MVVM, Clean Architecture, UDF, lifecycle, Coroutines, Flow, Navigation y dirección de dependencias en TheHouse Mobile.
---

# Android Architecture

## Nombre

android-architecture

## Objetivo

Validar que la arquitectura Android respete MVVM, Clean Architecture, Unidirectional Data Flow, lifecycle, Coroutines/Flow y Navigation sin sobrearquitectura.

## Cuándo utilizarlo

- Diseño o revisión de una capa, contrato, ViewModel o flujo de datos.
- Revisión de boundaries de Presentation, Domain y Data.
- Decisiones de navegación, DI o modularidad.
- Antes de introducir nuevas dependencias arquitectónicas.

## Entradas

- Spec, plan y task activos.
- Diagrama o descripción del flujo.
- Contratos, ViewModels, repositorios y navegación propuestos.
- Dependencias involucradas.

## Procedimiento

1. Confirmar autorización.
2. Identificar capas y dependencias.
3. Verificar dirección Presentation → Domain ← Data.
4. Verificar que Domain no dependa de frameworks.
5. Verificar que UI no acceda a infraestructura.
6. Verificar MVVM y UDF.
7. Verificar `viewModelScope`, Flow y cancelación.
8. Verificar lifecycle y manejo de configuración.
9. Verificar Navigation sin lógica de negocio.
10. Verificar que no se creen módulos Gradle no aprobados.
11. Revisar performance y recomposición.
12. Emitir dictamen.

## Reglas

- Un solo módulo `:app` inicialmente.
- Clean Architecture por paquetes y boundaries.
- UI Event → ViewModel → Use Case/Repository → StateFlow → UI State → Compose.
- Los ViewModels no contienen referencias a UI Android.
- Los Composables no hacen I/O ni contienen reglas de negocio.
- Domain define contratos; Data los implementa.
- Retrofit, Room, DataStore y Firebase permanecen en Data.
- DI y KSP requieren spec aprobada.
- No cambiar el toolchain.

## Restricciones

- No crear pantallas, entidades ni endpoints.
- No introducir Hilt/Koin por conveniencia.
- No modularizar sin decisión.
- No mover lógica de negocio a la UI.
- No aprobar atajos que rompan boundaries.

## Criterios de finalización

- Boundaries y dependencias validados.
- Flujo UDF verificable.
- Lifecycle y concurrencia revisados.
- Navegación y performance evaluadas.
- Dictamen y riesgos documentados.
