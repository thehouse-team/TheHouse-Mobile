# D21 — Reporte adicional NV-20 v1.3.0 (PR Backend #103)

| Campo | Valor |
|---|---|
| NV20_PR103_STATUS | MERGED (head `d23bb3cd63ede2b515c01efd1e1317068ab8f51a`); documento marca 1.3.0 como PROPUESTA pendiente de T09–T12 |
| NV20_CONTRACT_VERSION | 1.3.0 (propuesta documental; §6.1 y §9.1) |
| NEW_NOTIFICATION_TYPES | `agreement_proposed`, `agreement_cancelled`, `review_received`, `review_hidden`, `favorite_unavailable`, `report_resolved` (6/6 implementados) |
| KNOWN_TYPES_SUPPORTED | SÍ — todos los tipos previos (requests, visitas, mensajes, convenios, sistema) conservan su destino; bandeja mixta verificada |
| ROOM_TARGET_SUPPORTED | SÍ — `targetKind=room` + targetId válido → `RoomDetailRoute(targetId)` → `GET /api/v1/rooms/{targetId}`; sin `PropertyDetailRoute`, sin propertyId, sin marcador `{roomId}` literal |
| NONE_TARGET_SUPPORTED | SÍ — `targetKind=none` abre bandeja y no exige targetId |
| UNKNOWN_TYPE_FALLBACK | Bandeja; notificación genérica con título/body del Backend e icono genérico |
| UNKNOWN_TARGET_FALLBACK | Bandeja, sin petición HTTP |
| INVALID_TARGET_ID_FALLBACK | Bandeja; nunca se navega ni se ejecuta GET con id inválido |
| FCM_NAVIGATION_COMPATIBILITY | SÍ — `PushPayloadParser` + `PushNavigationIntentParser` usan el resolver centralizado |
| IN_APP_NAVIGATION_COMPATIBILITY | SÍ — `NotificationsScreen.handleTarget` usa el mismo resolver |
| NOTIFICATION_DESERIALIZATION_TESTS | 10 pruebas D21Nv20CompatibilityTest + mapper real de DTO; bandeja mixta; sin SerializationException por valores futuros |
| NOTIFICATION_SECURITY_TESTS | SAFE_ID centralizado, sin rutas arbitrarias, sin URLs externas, sesión requerida en root, sin logs de contenido/tokens |
| BACKEND_DEPLOYMENT_STATUS | NO CONFIRMADO — `health/live` OK; sin credenciales para consultar bandeja productiva y verificar los 6 tipos |
| RUNTIME_PENDING | SÍ — sin dispositivo/adb y sin despliegue confirmado de los 6 tipos |

## Detalle de destinos §9.1

| Tipo | Categoría | targetKind | targetId | Prioridad Android |
|---|---|---|---|---|
| `agreement_proposed` | requests | rental | id convenio | Alta |
| `agreement_cancelled` | requests | rental | id convenio | Normal |
| `review_received` | system | room | id cuarto | Normal |
| `review_hidden` | system | room | id cuarto | Normal |
| `favorite_unavailable` | system | room | id cuarto | Normal |
| `report_resolved` | system | none | id reporte (informativo) | Normal |

## Favorito no disponible

`targetKind=room` → se abre RoomDetail; si el Backend responde 404 o el cuarto ya no está disponible, la pantalla existente muestra su estado NotFound sin crash. No se elimina el favorito automáticamente: el contrato §9.1 no establece esa eliminación, solo el aviso. La reconciliación queda gobernada por el flujo existente de favoritos por `roomId` (D20), sin cambios automáticos.

## Preservación verificada

Estado leído/no leído, conteo y HTTP 304 (ETag), marcar leída / todas, paginación, autenticación y aislamiento entre cuentas permanecen en `NotificationsViewModel`/`NotificationsRepositoryImpl` sin modificación de contrato.
