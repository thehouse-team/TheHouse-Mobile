# D9 — Task

| Campo | Valor |
|---|---|
| Task | D9.1 Requests + Real Chat integración real |
| Estado | IN PROGRESS |
| Autorización | Orden humana explícita 2026-11-12 ("EJECUTA D9.1") |

## Subtasks
1. [x] Branch desde main + merge D11 base.
2. [x] DTOs/APIs/mappers/repositorios (requests + chat con ETag).
3. [x] Domain: modelos, errores, use cases.
4. [x] ViewModels + pantallas estudiante/arrendador/chat.
5. [x] Rutas, factory, MainActivity/Home/PropertyDetail.
6. [x] Tests repositorios + ViewModels.
7. [x] Validación Gradle (1 ciclo).
8. [x] Evidencia + memoria + commit + push.

## Reglas activas
- Sin userId; Bearer por interceptor existente; un solo Retrofit.
- Nada fake; errores siempre amigables; CancellationException rethrow.
