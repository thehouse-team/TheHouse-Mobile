# TASKS D18 — Room Detail Mobile

Estado: `PENDING` salvo marcadas.

- [ ] **D18-T01** — Auditar `origin/main` Backend y documentar contrato real de `GET /api/v1/rooms/{id}` y `/rooms/search`.
- [ ] **D18-T02** — Crear `RoomApi` + DTOs `@Serializable` espejo del contrato.
- [ ] **D18-T03** — Crear `RoomRepository` con mapeo tipado de errores (400/401/404/429/503/network).
- [ ] **D18-T04** — Crear modelos Domain mínimos y `GetRoomDetailUseCase`.
- [ ] **D18-T05** — Crear `RoomDetailRoute`, `RoomDetailViewModel` y `RoomDetailScreen` con estados reales.
- [ ] **D18-T06** — Cablear ruta en `MainActivity` y factory; back y bottom nav.
- [ ] **D18-T07** — Cambiar click Smart Match a `roomId` → `RoomDetailRoute`; eliminar `/properties/{roomId}` desde Smart Match.
- [ ] **D18-T08** — Tests 1–12.
- [ ] **D18-T09** — Gates, PR separado, CI verde, evidencia y memoria.
- [x] **D18-T10** — Auditoría inicial de gaps de evaluación (solo lectura).
