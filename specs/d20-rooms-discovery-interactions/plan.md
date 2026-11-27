# PLAN D20 — Rooms, Discovery and Interactions

| Fase | Objetivo | Verificación |
|---|---|---|
| F1 Catálogo | DTO `RoomSearchItem`/`RoomSearchResponse`, `RoomApi.searchRooms`, `RoomSearchRepository`, filtros/catálogos, paginación, estados, Home y Explore migrados, imágenes, búsqueda local | Tests de contrato + ViewModels Home/Explore |
| F2 Detalle | Unificación de rutas a `RoomDetailRoute(roomId)`, fotos reales, acciones (favorito/contactar/visita), guest | Tests de navegación y D19.1 regressions |
| F3 Favoritos | DTOs reales, `roomIds` como identidad, estado compartido, PUT/DELETE, rollback, guest, pantalla con roomId | Tests de repository/VM + sincronía |
| F4 Solicitudes | Validación de preguntas, roomId, conversationId, dedupe, errores | Tests de VM + repository |
| F5 Visitas | `roomId`, Idempotency-Key, `scheduledAt` offset, response nullable, dedupe | Tests de validator + repository |
| F6 Legacy/CI/Git | Documentar callers, gates, SDD/evidencia, commit/push, PR único | Gates remotos y locales |
| F1/F5 (cierre) | Filtros: borrador como texto + validación al aplicar + panel ModalBottomSheet con acciones fijas. Visitas: DatePicker + cuadrícula de horarios permitidos + POST /visits con roomId/Idempotency-Key y errores visibles | Tests de borrador (sin consulta, una sola al aplicar, limpiar), VisitScheduleOptions y POST HTTP real con MockWebServer; gates locales |

Orden de ejecución: F1 → F2 → F3 → F4 → F5 → F6. Cada fase conserva compilación
y tests verdes antes de avanzar. Sin auditorías maestras repetidas.
