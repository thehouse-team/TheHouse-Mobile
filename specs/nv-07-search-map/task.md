# NV-07 - TASKS

| Campo | Valor |
|---|---|
| Spec | NV-07 Search + Map |
| STATUS | D10 EXECUTED (busqueda real + mapa de referencia); SDK de mapas/permiso pendiente |
| AUTHORIZATION | D10 autorizada y ejecutada; resto DO NOT EXECUTE |

- [x] NV-07-T01 - Matriz filtros/coordenadas vs contrato NV-27 (D10: verificada contra fuente Backend en solo lectura).
- [ ] NV-07-T02 - Evaluacion COST-001 del SDK de mapas (o BLOCKED). D10 uso mapa de referencia $0, sin SDK ni tiles.
- [x] NV-07-T03 - SearchRepository + mapeo (D10: PropertyRepository extendido; sin segundo cliente).
- [x] NV-07-T04 - UI de busqueda con filtros confirmados (D10: /rooms/search real).
- [x] NV-07-T05 - UI de mapa + permiso ubicacion (NV-21) + fallback (D10 parcial: mapa de referencia real sin permiso; SDK/permiso siguen pendientes).
- [x] NV-07-T06 - Tests (busqueda, cancelacion, permiso denegado) (D10: 140/140 suite completa; permiso no aplica porque no se agrego permiso de ubicacion).
- [x] NV-07-T07 - Build/tests/lint y evidence.md (D10: BUILD SUCCESSFUL, evidencia actualizada).
- [x] NV-07-D4-01 - (Parcial D4) Tab Explorar sin mapa/SDK: lista fixture del prototipo, Guest/Auth.
- [x] NV-07-D5-01 - (Parcial D5) Catalogo real: PropertyRepository + ExploreViewModel (lista real, estados, busqueda local titulo/ciudad).
- [x] NV-07-D5-02 - (Parcial D5) Home con destacada real (primera propiedad, regla UI local) sin Match/verificacion/distancia inventados.
- [x] NV-07-D10-01 - Contrato real verificado en solo lectura: `/rooms/search`, `/rooms/map`, `/rooms/{id}`, `/places/search`; sin `q` en rooms ni campos inventados.
- [x] NV-07-D10-02 - Domain/DTO/mapper/PropertyRepository/use cases: busqueda, mapa por bbox, colonias y detalle de cuarto; validacion de filtros y viewport antes de llamar.
- [x] NV-07-D10-03 - ExploreViewModel/ExploreScreen reales: universidad obligatoria, filtros, sort, paginacion, lista/mapa, marcadores, preview, estados Loading/Empty/Content/Error, 400/401/429/503 amigables y cancelacion.
- [x] NV-07-D10-04 - Detalle real `/rooms/{id}` (`RoomDetailScreen` + ViewModel), guard Guest → Login; sin direccion, dueno ni contacto.
- [x] NV-07-D10-05 - Tests D10 (repositorio, use cases, Explore VM, RoomDetail VM) + regresion D3-D8; validacion runtime en emulador con Backend desplegado.
