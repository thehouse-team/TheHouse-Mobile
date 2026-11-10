# NV-07 — CHECKLIST

- [x] Matriz filtro->contrato aprobada (D10: verificada contra fuente Backend; solo parametros reales)
- [x] COST-001 mapas $0 demostrado (D10: mapa de referencia sin SDK/tiles/API key; M-MOB-017 y SDK siguen pendientes)
- [x] Busqueda con estados UDF (D5: Explore real loading/success/empty/error/retry + busqueda local; D10: Loading/Empty/Content/Error reales)
- [x] Mapa con fallback sin ubicacion (D10: bbox derivado de resultados/colonia; sin permiso de ubicacion)
- [x] Tests de busqueda/cancelacion (D10: repositorio, use cases, ViewModels; cancelacion rethrow verificada)
- [x] Build/tests/lint verdes con evidencia real (D10: 140/140 tests, lint sin errores, BUILD SUCCESSFUL)
- [x] (D4 parcial) Tab Explorar fixture sin mapa (ExploreScreen)
- [x] (D5 parcial) Explore real sin Match/verificacion/distancia inventados; mapa placeholder
- [x] (D10) Busqueda real `/rooms/search`, filtros, sort, paginacion, colonias `/places/search`
- [x] (D10) Mapa real `/rooms/map` con bbox 0.3°, marcadores aproximados, pan/zoom, preview y detalle
- [x] (D10) Detalle real `/rooms/{id}` con guard Guest → Login; sin direccion/dueno/contacto
- [x] (D10) Errores 400/401/429/503 amigables; sin crash; cancelacion rethrow
- [x] (D10) Validacion runtime en emulador con Backend desplegado (busqueda, mapa, colonias, guest guard)
