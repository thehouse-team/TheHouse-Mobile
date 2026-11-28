# D21 — Checklist

- [x] Gobernanza leída (constitution, AGENTS, memoria) y declaración de autorización
- [x] PR #36 verificado MERGED, base `54ece77`, rama creada desde origin/main
- [x] Spec/plan/task creados antes de implementar
- [x] Contratos Backend verificados en solo lectura (devices, moderación, GenAI, delete)
- [x] installationId UUID D persistente (DataStore, Mutex, estable)
- [x] DeviceApi/DTO/Repository/UseCases sin bodies default
- [x] DeviceSyncCoordinator: dedup, guest, rotación, red, logout, cambio de cuenta
- [x] onNewToken → sincronización; DI en AppContainer
- [x] D21LogoutUseCase: DELETE antes de revocar, outcome real, purga always
- [x] Moderación centralizada 422/503 con conservación de texto
- [x] GenAI: explicación bajo demanda con generatedBy honesto, score intacto
- [x] Permiso ACCESS_NETWORK_STATE añadido por uso real
- [x] androidTest reparado (4 archivos reescritos con APIs reales)
- [x] Scripts PowerShell de automatización (5 + common)
- [x] CI: compile/assemble androidTest añadidos sin bajar umbrales
- [x] Matrices OWASP / permisos / plataforma / trazabilidad
- [x] 631 tests / 0 failures / 0 errors
- [x] Gates: compile, unit, lint, assembleDebug, compile androidTest, assemble androidTest
- [ ] `connectedDebugAndroidTest` — sin dispositivo (adb no disponible)
- [ ] FCM real delivery — EXTERNAL_BLOCKER (Backend sin envío / secreto)
- [ ] Eliminación en cuenta desechable — sin autorización de cuenta
- [ ] CI remoto del PR — pendiente de push/PR
- [x] NV-20 §9.1 leído desde PR #103 (solo lectura); 6 tipos literales extraídos
- [x] Contrato propuesto vs mergeado vs productivo distinguido
- [x] DTOs/NotificationType tolerantes (String, sin enum estricto)
- [x] targetKind room + none + desconocido + targetId inválido → bandeja
- [x] Navegación RoomDetailRoute real; sin PropertyDetail ni propertyId
- [x] Push e in-app comparten el mismo resolver centralizado
- [x] Pruebas de 6 tipos, bandeja mixta, 200/404, guest, push y regresiones
- [ ] Despliegue productivo de los 6 tipos - NO CONFIRMADO (runtime pendiente)

## ADENDA VISUAL — checklist (ALCANCE ESTRICTO CORREGIDO)

- [x] Misma rama `feature/platform-security-automation` y PR #37 (sin rama/PR nuevos)
- [x] Cambios no autorizados restaurados desde `735a697` con `git checkout` (sin reset/force push)
- [x] Home: SOLO botón original "Publicar mi renta" movido antes del listado (mismo diseño/callback)
- [x] Home: SOLO tarjeta redundante de Privacidad retirada; disponible en drawer y flujos legales
- [x] Home: espaciados, tipografía, colores, tarjetas, SmartMatchPromo, buscador y header = `735a697`
- [x] Favoritos: SOLO `FavoriteCard` (título, precio, ubicación, distribución, acción quitar)
- [x] Conversación: SOLO burbujas, hora, campo de entrada y botón enviar
- [x] Sin estado vacío nuevo, divisor, chip compartido ni cambio de espaciado general
- [x] Sin tocar Backend/GenAI/ML/ONNX/infra/contratos/DTOs/sesión/navegación/Drawer/MainActivity/theme/componentes compartidos
- [x] Sin mover "Nuevo edificio" (pendiente del responsable de Backend)
- [x] 645 pruebas existentes preservadas
- [x] `git diff 735a697` revisado bloque por bloque: cada cambio justificado por una de las 4 solicitudes
- [x] Gates locales ejecutados (ver evidence.md)
- [ ] CI remoto del PR #37 verificado tras el commit correctivo
- [ ] Validación visual y aprobación humana - PENDIENTE
- [ ] Merge - NO AUTORIZADO (no ejecutar)

## Hotfix regresiones/imágenes (2026-11-27)
- [x] PR #37 verificado OPEN / MERGEABLE / CI SUCCESS antes de implementar.
- [x] Comparación visual 54ece77 vs 735a697 vs 24a0686 vs a71ada3 documentada.
- [x] Favoritos y Conversación restaurados a 735a697; Home con 3 bloques autorizados.
- [x] Causa P0 Publicar demostrada (callback desconectado) y corregida con efecto de navegación.
- [x] thumbnailUrl productivo verificado: relativo, HTTP 200, image/webp.
- [x] URLs relativas resueltas; cliente de imágenes sin Bearer.
- [x] Placeholder TheHouse en null/loading/error; galería compacta sin fotos.
- [x] Etiquetas en español aplicadas sin tocar serialización.
- [x] 657 tests / 0 failures; lint, assemble y androidTest SUCCESS; diff-check exit 0.
- [ ] Validación visual humana y renderizado en dispositivo (RUNTIME_PENDING).
- [ ] Merge de PR #37 (NO AUTORIZADO).

## ADENDA FINAL — cierre técnico y geolocalización (2026-11-27)

- [x] PR #37 verificado OPEN / MERGEABLE; base visual `3bbd1db` preservada.
- [x] Contratos de fotos y `universities/search` auditados en Backend (solo lectura).
- [x] Photo Picker gestionado por la UI existente de habitaciones (sin duplicar ni rediseñar).
- [x] `PhotoResponseDto` corregido; multipart/DELETE/422/503/413/415/409 probados con MockWebServer.
- [x] Moderación 422/503 conectada a formularios del arrendador con textos en español.
- [x] POST_NOTIFICATIONS: `askedOnce` persistido, una sola solicitud contextual, Ajustes para denegación permanente.
- [x] `CoordinateParser` con coma/Unicode/invisibles/límites/parcial; UI conserva texto original.
- [x] «Usar mi ubicación actual» solo en Ubicación; permiso contextual; sin GPS en background.
- [x] Universidades cercanas: Backend ordena y calcula distancia; sin Haversine local ni IDs inventados.
- [x] JaCoCo sin exclusiones: 33.16 % líneas / 31.49 % ramas.
- [x] 702 tests / 0 failures / 0 errors (669 + 33); 8 gates + cobertura SUCCESS.
- [ ] GPS físico, renderizado y `connectedDebugAndroidTest` — PENDING (sin dispositivo).
- [ ] Merge de PR #37 — NO AUTORIZADO.
