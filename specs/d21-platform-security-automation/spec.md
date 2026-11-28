# D21 — Platform, Security and Automation

| Campo | Valor |
|---|---|
| ID | D21 |
| Estado | APROBADO / EN EJECUCIÓN |
| Autorización | Instrucción humana explícita (FASE C), 2026-11-27 |
| Rama | `feature/platform-security-automation` |
| Base | `origin/main @ 54ece77747d5f3416bdf189ee1ede597c6a40f8f` (PR #36 MERGED) |
| Constitución | 1.0.0 VIGENTE |
| COST-001 | $0 — FCM free tier, sin tarjeta, sin recursos cloud nuevos, sin servicios pagados |

## 1. Problema

La Fase A y B cerraron funcionalmente (602 tests locales, 23 integraciones HTTP, CI verde), pero persisten brechas de plataforma:

1. FCM está configurado y el token se obtiene, **pero nunca se registra en el Backend** (`FCM-TOKEN-BACKEND-BLOCKED` en `AppContainer.kt:683`), por lo que ningún push llega al dispositivo.
2. No existe `installationId` persistente; no hay forma de correlacionar instalación ↔ token ↔ cuenta.
3. El logout no desvincula el dispositivo del Backend y no hay aislamiento explícito cuenta A → cuenta B para FCM.
4. Las respuestas de moderación (422 `content_rejected`, 503 `moderation_unavailable`) no tienen manejo centralizado en Mobile.
5. `androidTest` no compila (4 archivos con imports inválidos, APIs inexistentes y firmas desactualizadas) y CI no lo detecta porque no compila instrumentación.
6. No existe automatización en dispositivo (scripts PowerShell) ni matriz final de requisitos del profesor actualizada.
7. La documentación afirma que Firebase NO está configurado cuando `app/google-services.json` existe y está versionado.

## 2. Alcance autorizado

Ver instrucción humana FASE C, secciones 2 y 3. Incluye: SDD, implementación Mobile, MainActivity/Manifest/AppContainer/Gradle cuando justificado, FCM, androidTest, scripts PowerShell, pruebas, CI preventivo justificado, commits/push y un único PR sin merge.

**Excluido:** merge, cambios en main, Backend/GenAI, modelos ML, recursos cloud, contratación de servicios, costos personales, Docker, secretos, eliminación real de cuentas, reducción de quality gates.

## 3. Contratos Backend verificados (solo lectura)

### Devices — `TheHouse.Http/PushDevicesEndpoint.cs`
| Método | Ruta | Respuesta | Notas |
|---|---|---|---|
| PUT | `/api/v1/users/me/devices/{installationId}` | 201 crear / 200 actualizar | `installationId` GUID formato **D**; inválido → 404 |
| GET | `/api/v1/users/me/devices` | 200 `{items:[...]}` | Sin tokens |
| DELETE | `/api/v1/users/me/devices/{installationId}` | 204 / 404 | Delete scope = userId + installationId (BOLA safe) |

Body `PushDeviceRequest`: `fcmToken` (20–4096, `[A-Za-z0-9:_-]`), `platform` = `"android"` exacto, `appVersion` opcional (1–32), `locale` opcional (1–16). Unknown field → 400. Conflictos: mismo installationId con token distinto en otra cuenta → 409 `installation_conflict`. Máximo 5 devices/usuario. Límites: 6144 bytes body, 415 si content-type incorrecto, 413 si excede.

### Moderación — `UserEndpointSupport.cs:107-119`
- 422 `content_rejected` + `field` + `categories` (`hate`, `sexual`, `violence`, `self_harm`, `blocklist`)
- 503 `moderation_unavailable` + `Retry-After: 30`
- Operaciones moderadas: perfil, edificios, landlord profile, visitas (mensaje/notas), solicitudes, chat, reseñas, reportes, roommate profile, fotos (building/room).
- Fotos: `PUT /landlord/buildings/{id}/photo`, `POST /landlord/rooms/{id}/photos`; multipart `file`, máx 5 MB, JPEG procesado, moderado **antes** de guardar. Documentos NO moderados.

### GenAI — `SmartMatchEndpoint.cs`
- `GET /api/v1/smart-match/explanations/{roomId}` (auth)
- `POST /api/v1/smart-match/explanations/query` (guest, body `roomId`, `universityId`, `preferences`)
- Respuesta: `roomId, score, summary, pros[], cons[], generatedBy ("genai"|"template"), modelVersion, explanationVersion`
- Backend llama a GenAI con service key; **Mobile nunca llama a GenAI directamente**.

### Eliminación — `AccountEndpoint.cs`
- GET preview 200; POST 204; 403 `password_mismatch`, 409 `deletion_blocked`, 429, 503 `deletion_incomplete`. Confirmación exacta `"ELIMINAR"`.

### Push real
No existe endpoint HTTP para enviar push. El push se dispara como efecto secundario al crear notificaciones; requiere `FIREBASE_SERVICE_ACCOUNT` en el Backend (secreto no disponible en Mobile y no autorizado a crear). → `EXTERNAL_BLOCKER` para FCM_REAL_DELIVERY.

## 4. Requisitos funcionales

| ID | Requisito | Criterio |
|---|---|---|
| D21-F01 | `installationId` UUID D persistente | DataStore, sobrevive restart/force-stop/update/rotación token; estable por instalación; no se regenera en login |
| D21-F02 | DeviceApi/DTO/Repository/UseCases | Retrofit real sin bodies default; DTO espejo estricto |
| D21-F03 | Registro de dispositivo | Solo sesión Active + token; dedup single-flight por (cuenta, installationId, token); 200/201 = éxito; otro código = fallo |
| D21-F04 | Sincronización en eventos | Login, restauración de sesión, `onNewToken`, recuperación de red con reintento pendiente acotado; guest nunca registra |
| D21-F05 | Desvinculación en logout | DELETE antes de revocar credenciales; timeout acotado; 204 = eliminado, 404 = ya no existe (tratar como no presente); otro fallo = estado seguro local, **no afirmar eliminación remota** |
| D21-F06 | Aislamiento de cuenta | Al cambiar de cuenta se cancela sync de la anterior, se limpia token/coordinador y se impide heredar notificaciones |
| D21-F07 | Errores de moderación centralizados | 422 → mensaje claro + conservar texto; 503 → error temporal + reintento; nunca mostrar éxito |
| D21-F08 | androidTest compila | `compileDebugAndroidTestKotlin` + `assembleDebugAndroidTest` SUCCESS sin deshabilitar tests |
| D21-F09 | Scripts PowerShell | check-device, build-and-install, run-unit-tests, run-instrumented-tests, collect-runtime-evidence; sin secretos, sin borrar datos, exit codes correctos |
| D21-F10 | CI preventivo | Añadir `compileDebugAndroidTestKotlin` + `assembleDebugAndroidTest` al job quality; sin bajar umbrales |
| D21-F11 | Matriz de requisitos | Actualizar `REQUIREMENTS_TRACEABILITY.md` con 21 items, porcentajes con numerador/denominador/criterio |
| D21-F12 | Matriz OWASP | Control / aplicabilidad / evidencia / resultado / hallazgo / severidad / corrección / riesgo residual |

## 5. Riesgos y mitigación

| Riesgo | Mitigación |
|---|---|
| Token FCM expuesto | Solo fingerprint SHA-256 truncado en estado público; logs solo códigos de evento; token nunca en DataStore |
| `google-services.json` versionado con API key | Firebase API keys son identificadores públicos restringidos por package/sha; documentar, no imprimir valor, no agregar service-account |
| Duplicar registros por recomposición | Coordinador con single-flight `Mutex` por clave (accountId, installationId, token) |
| Logout bloqueado por red | Timeout acotado (5 s) y `try/finally` que siempre purga local |
| Cuenta B hereda push de A | `DeviceSyncCoordinator.clearForAccountSwitch()` cancela jobs y limpia token antes de activar nueva sesión |
| Moderación 422 pierde texto | `ModerationFailure` conserva el texto original en el estado de UI para corrección |
| Falsos positivos de scanners | Clasificar con justificación, nunca suprimir para "verde" |

## 6. Evidencia esperada

- Gates: `clean compileDebugKotlin`, `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `compileDebugAndroidTestKotlin`, `assembleDebugAndroidTest`, `git diff --check`.
- Tests nuevos: installationId persistente, PUT/DELETE devices con MockWebServer, dedup, guest, logout/switch, red, deep links inválidos, moderación 422/503, eliminación 204, sesión concurrente, GenAI vs template.
- Matriz OWASP y de permisos, reporte final, PR OPEN/MERGEABLE sin merge.

---

## ADENDA — Compatibilidad NV-20 Notifications v1.3.0 (PR #103 Backend)

Autorizada dentro de la misma spec/rama/PR. Contrato leído en modo solo lectura de
`thehouse-team/TheHouse-Backend` PR #103 (`d23bb3c`) §6.1 y §9.1.

**Estado del contrato:** PR #103 MERGED en Backend; versión 1.3.0 marcada como
PROPUESTA pendiente de T09–T12 y aprobación humana en el documento. Backend main
expone `NotificationResponse.Type` y `NotificationTarget.Kind` como `string`
(no enums estrictos), por lo que no existe `SerializationException` por valores
desconocidos en Mobile. Despliegue productivo de los 6 tipos: NO CONFIRMADO.

### Seis tipos literales (no derivados de etiquetas españolas)

| Tipo | Categoría | targetKind | targetId | Prioridad | Título | Cuerpo |
|---|---|---|---|---|---|---|
| `agreement_proposed` | requests | rental | id convenio | Alta | Convenio por aceptar | «{Arrendador} preparó el convenio de {cuarto}. Pídele el código de cierre para aceptarlo.» |
| `agreement_cancelled` | requests | rental | id convenio | Normal | Convenio cancelado | «{Arrendador} canceló el convenio de {cuarto}.» |
| `review_received` | system | room | id cuarto | Normal | Nueva reseña | «{Estudiante} dejó una reseña de {cuarto}.» |
| `review_hidden` | system | room | id cuarto | Normal | Reseña ocultada | «Tu reseña de {cuarto} se ocultó porque no cumple las reglas de la comunidad.» |
| `favorite_unavailable` | system | room | id cuarto | Normal | Favorito no disponible | «{Cuarto} ya no está disponible.» |
| `report_resolved` | system | none | id reporte (informativo) | Normal | Reporte revisado | «Revisamos tu reporte. Gracias por ayudarnos a cuidar la comunidad.» |

`none` abre la bandeja; no exige targetId. `room` valida targetId y navega a
`RoomDetailRoute(targetId)` + `GET /rooms/{id}`. Tipo o targetKind desconocido,
o targetId inválido/ausente cuando es obligatorio → bandeja, sin petición HTTP.

---

## ADENDA VISUAL — Visual Polish UI (Home / Favoritos / Conversación)

Autorizada por corrección de gobernanza humana (2026-11-27) **dentro de la misma spec D21,
rama `feature/platform-security-automation` y PR #37**. No se crea spec nueva, no se abre PR #38,
no se crea rama nueva y **no se mergea**.

**Naturaleza:** exclusivamente estética / presentación. Sin cambios funcionales, de backend,
contratos HTTP, DTOs, repositorios, casos de uso, lógica de sesión, navegación base, Drawer ni
 MainActivity. No se modifica la ubicación de "Nuevo edificio" (pendiente de información del
 responsable de Backend). No se introducen dependencias, librerías ni servicios nuevos.
 COST-001: $0.

**Alcance visual autorizado:**

**Alcance visual CORREGIDO (estricto,tras rechazo del usuario):**

| ID | Requisito visual | Criterio |
|---|---|---|
| D21-V01 | Reubicar "Publicar mi renta" | MISMO botón `TheHouseSecondaryButton` original; solo cambia su posición (antes del listado de alojamientos). Sin tarjeta nueva, subtítulo, icono ni color nuevo. Conserva `HomeEvent.PublishClicked` |
| D21-V02 | Retirar "Privacidad" redundante de Inicio | Se elimina el acceso al final de Home; permanece en drawer y flujos legales existentes. Sin modificar drawer |
| D21-V03 | Mejorar tarjetas de Favoritos | Solo `FavoriteCard`: título, precio, ubicación, acción quitar y distribución interna. Sin componentes compartidos ni lógica |
| D21-V04 | Mejorar estética de Conversación | Solo burbujas, presentación de horas, campo de entrada y botón enviar. Sin tocar header, estructura, estados ni contratos |

**Prohibido expresamente:** rediseño general de Home, cambios de espaciados/tipografía/colores
generales, tarjetas nuevas, estados vacíos nuevos, divisores, chips compartidos, cambios en
Smart Match Promo, buscador, catálogo, recomendaciones, header, tema global, componentes
compartidos, Nuevo edificio, geolocalización, permisos, FCM, Backend, GenAI, ML, ONNX ni infra.

**Restricciones de regresión:** se preservan las 645 pruebas existentes y todas las
funcionalidades previamente certificadas (D19.1/D20/D21/NV-20). Los tests estructurales de
Home exigen: sin `BrandMark(` propio, con `SmartMatchPromo(onClick = onNavigateToSmartMatch)`,
`onOpenPrivacy`, `PublishClicked` y `TheHouseSearchBar`.

**Estado:** CORREGIDO AL ALCANCE ESTRICTO / IMPLEMENTADO / TESTED / EVIDENCIADO.
RUNTIME VISUAL PENDIENTE de validación y aprobación humana. SIN MERGE.

### Corrección de exceso de alcance (commit correctivo)

La revisión humana rechazó el primer intento (`24a0686`) por modificar elementos visuales que
estaban bien y no fueron solicitados: tarjeta CTA nueva con icono/subtítulo/borde, cambio de
espaciado global de Home, acceso de Privacidad rediseñado, chips/divisor en Favoritos, estado
vacío/divisor/cambios de espaciado general en Conversación. Se restauró cada archivo desde
`735a697` (`git checkout 735a697 -- <archivo>`, sin `reset --hard` ni force push) y se reaplicaron
únicamente los cuatro cambios autorizados. No se revirtió ninguna implementación funcional de D21.

### Hotfix de regresiones y carga real de imágenes (2026-11-27)

Autorización humana individual, MISMO PR #37 y rama `feature/platform-security-automation`, HEAD `a71ada3`. NO MERGEAR.

| ID | Requisito | Estado |
|---|---|---|
| D21-H01 | Restaurar estética aprobada (Favoritos/Conversación byte-idénticos a `735a697`; Home solo Privacidad eliminada + botón original antes de recomendados) | IMPLEMENTADO / TESTED |
| D21-H02 | Publicar mi renta: guest → login, autenticado → flujo arrendador existente, sin auto-activación ni edificios automáticos | IMPLEMENTADO / TESTED |
| D21-H03 | Mostrar thumbnails reales (URL relativas resueltas) y placeholder TheHouse en null/fallo, sin contenedores blancos | IMPLEMENTADO / TESTED |
| D21-H04 | Galería RoomDetail con fotos reales y espacio compacto cuando no hay fotos; Contactar/Agendar visita intactos | IMPLEMENTADO / TESTED |
| D21-H05 | Cliente de imágenes sin Bearer ni authenticator; TLS y Network Security Config intactos | IMPLEMENTADO / TESTED |
| D21-H06 | Etiquetas visibles en español mediante catálogo existente, sin cambiar valores serializados | IMPLEMENTADO / TESTED |
| D21-H07 | Backend FCM/GenAI/NV-20 #106/moderación/geolocalización documentados sin declarar producción no confirmada | EVIDENCIADO |

**Estado:** IMPLEMENTADO / TESTED / EVIDENCIADO. RUNTIME PENDING (sin dispositivo; sin aprobación visual). SIN MERGE.

---

## ADENDA FINAL — Cierre técnico sin dispositivo y geolocalización (2026-11-27)

Autorización humana explícita dentro del MISMO PR #37, rama `feature/platform-security-automation`, base visual `3bbd1db`. Sin merge, sin rama/PR nuevo, sin Backend/GenAI/ML/ONNX/infra, sin servicios nuevos (COST-001 $0).

| ID | Requisito | Estado |
|---|---|---|
| D21-C01 | Fotos: contrato auditado, `PhotoResponseDto` correcto, multipart/headers/rutas/errores con MockWebServer, sin mutaciones productivas | IMPLEMENTADO / LOCAL_TEST_PASS |
| D21-C02 | Moderación 422/503 conectada a formularios de arrendador con mensajes en español | IMPLEMENTADO / LOCAL_TEST_PASS |
| D21-C03 | POST_NOTIFICATIONS: decisión persistida, una sola solicitud contextual, denegación permanente → Ajustes | IMPLEMENTADO / LOCAL_TEST_PASS |
| D21-C04 | Coordenadas: parser puro con normalización y rangos contractuales 14..33 / -118.5..-86 | IMPLEMENTADO / LOCAL_TEST_PASS |
| D21-C05 | «Usar mi ubicación actual»: LocationManager del sistema, permiso contextual, sin background ni proveedor comercial | IMPLEMENTADO / LOCAL_TEST_PASS |
| D21-C06 | `GET /universities/search?lat&lng`: API real, DTO espejo, preselección sin pisar elección manual, cancelación | IMPLEMENTADO / LOCAL_TEST_PASS |
| D21-C07 | JaCoCo: cobertura real 33.16 % líneas / 31.49 % ramas, sin exclusiones artificiales | IMPLEMENTADO / LOCAL_TEST_PASS |
| D21-C08 | Cierre OWASP/DevSecOps, trazabilidad y memoria | EVIDENCIADO |

**Estado:** D21-OFFLINE-TECHNICAL-COMPLETION-CODE-READY + D21-BUILDING-GEOLOCATION-CODE-READY. RUNTIME_PENDING (sin dispositivo). SIN MERGE.

---

## ADENDA HOTFIX — Estabilidad y conectividad (2026-11-28)

Autorización humana sobre el MISMO PR #37, rama y commit base `4dc4e6e`, sin merge, sin cambios visuales.

| ID | Requisito | Estado |
|---|---|---|
| D21-S01 | Contener IOException de red en launch de Main.immediate sin ocultar CancellationException | IMPLEMENTADO / LOCAL_TEST_PASS |
| D21-S02 | Login/register: SocketTimeoutException y UnknownHostException tipados a Result sin borrar sesión válida | IMPLEMENTADO / LOCAL_TEST_PASS |
| D21-S03 | Catálogo, reintento de devices y navegación offline sin crash | IMPLEMENTADO / EMULATOR_PASS (parcial) |
| D21-S04 | DNS del emulador diagnosticado como fallo de entorno; sin IP/DNS/TLS/timeoutmodificados | EVIDENCIADO |

**Estado:** D21-NETWORK-STABILITY-HOTFIX-CODE-READY. CRASH_REPRODUCED: PARCIAL (stack real recuperado; offline reproducible, read-timeout no reproducible por DNS roto). SIN MERGE.

---

## ADENDA FINAL — Cierre integral del flujo del arrendador (2026-11-27)

Autorización humana individual sobre el MISMO PR #37 (OPEN/MERGEABLE, HEAD `6ab949e`),
rama `feature/platform-security-automation`. Sin merge, sin rama/PR nuevo, sin Backend,
GenAI, ML, ONNX ni infraestructura. Congelación visual total: no se tocan Home, Explore,
Smart Match, Favoritos, RoomDetail, Perfil, Preferencias, Header, Drawer ni tema.

Auditoría contractual dirigida: `docs/sdd/reports/D21_LANDLORD_END_TO_END_AUDIT.md`.
Contratos verificados en Backend (solo lectura): `MessagingEndpoint.cs`,
`MessagingServices.cs` (7 códigos, máx 5, mensaje ≤500, `conversationId` no nullable,
201), `MessagingContracts.cs` (`JsonUnmappedMemberHandling.Disallow`),
`UserEndpointSupport.cs` (422 `content_rejected`, 503 `moderation_unavailable`),
`LandlordEndpoint.cs`, `Listings.cs`, `AdminEndpoint.cs` (aprobación staff fuera de alcance).

| ID | Requisito | Estado |
|---|---|---|
| D21-L01 | Catálogo único de etiquetas españolas para las 7 preguntas rápidas; el detalle deja de mostrar códigos API | IMPLEMENTADO / TESTED |
| D21-L02 | Conversación: `RequestDetailEvent.OpenConversation` emite `conversationId` real del Backend y navega; antes era `Unit` | IMPLEMENTADO / TESTED |
| D21-L03 | Error de moderación 422/503 mapeado a `RequestErrorCode` y mensaje Spanish; formulario conservado para reintento | IMPLEMENTADO / TESTED |
| D21-L04 | Activación: recarga del dashboard tras `LandlordProfileEffect.Activated` (navegación `launchSingleTop` al mismo destino era no-op) | IMPLEMENTADO |
| D21-L05 | Actualización automática del listado de alojamientos al volver (ON_RESUME, sin refresh manual) | IMPLEMENTADO |
| D21-L06 | `availableFrom` opcional según contrato (`string?`); fecha inválida sí se rechaza | IMPLEMENTADO |
| D21-L07 | Matriz de endpoints, estados de dominio y brechas documentada | EVIDENCIADO |
| D21-L08 | 10 pruebas nuevas (labels, máx 5, payload HTTP real, moderación, conversationId, toggle) | LOCAL_TEST_PASS |

**Estado:** D21-LANDLORD-END-TO-END-CODE-READY. RUNTIME_PENDING. SIN MERGE.
