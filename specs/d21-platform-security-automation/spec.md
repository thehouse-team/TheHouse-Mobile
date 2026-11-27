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
