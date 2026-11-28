# D21 — Evidencia de plataforma, ML, GenAI y moderación

## FCM / dispositivos

| Ítem | Estado | Evidencia |
|---|---|---|
| `google-services.json` | PRESENTE y trackeado | `project_id=thehouse-ac943`, `package_name=com.thehouse.app` (API key de cliente NO impresa) |
| Firebase Messaging SDK | Sí | `libs.versions.toml`: BoM 35.2.0; `app/build.gradle.kts:140-141` |
| Google Services plugin | Sí (condicional) | `app/build.gradle.kts:10-13`; `FCM_CONFIGURED=true` |
| FirebaseMessagingService | Sí | `TheHouseFirebaseMessagingService.kt` (data-only, `onNewToken`) |
| onNewToken → coordinator | Sí (D21) | `pushTokenCoordinator.onTokenChanged` registrado en `observePushSession()` |
| Canales | 3: requests/visits/system | `NotificationChannelRegistry.kt`, VISIBILITY_PRIVATE |
| Token sin logging | Sí | Solo fingerprint SHA-256 truncado a 12 chars; logs solo códigos |
| Registro Backend | IMPLEMENTADO | `DeviceApi.kt`, `DeviceRepositoryImpl.kt`, `DeviceSyncCoordinator.kt` |
| installationId | UUID D persistente | `DataStoreInstallationIdStore.kt`, clave `device_installation_id`, Mutex anti-race |
| Guest sin registro | Sí | `RegisterDeviceUseCase` rechaza sesión no Active |
| Dedup | Sí | Clave (userId, installationId, token) + `syncedKeys` + single-flight Mutex |
| Logout desvinculación | Sí | `D21LogoutUseCase` → DELETE antes de revocar; outcomes 204/404/fallo real |
| Aislamiento A→B | Sí | `clearForAccountSwitch()` cancela jobs, limpia token y pending |
| Deep links | Allowlist de tipos + SAFE_ID + rutas tipadas | `PushPayload.kt`, `NotificationDestination.kt`, `MainActivity.kt:1320-1328` |
| FCM real delivery | **EXTERNAL_BLOCKER** | Backend no expone endpoint de envío; requiere `FIREBASE_SERVICE_ACCOUNT` (secreto no disponible; no autorizado a crear). Un PUT 200/201 NO demuestra recepción. |

## Moderación

| Operación | Moderada en Backend | Código Mobile D21 |
|---|---|---|
| Perfil / nombre | Sí (`profile.name`) | `ModerationErrorParser` centralizado |
| Edificios / landlord | Sí (`building.name`, `landlord.fullName`) | ídem |
| Visitas (mensaje/notas) | Sí | ídem |
| Solicitudes / chat | Sí (`request.message`, `chat.text`) | ídem |
| Reseñas / reportes | Sí | ídem |
| Fotos building/room | Sí (JPEG procesado antes de guardar, 5 MB) | `MediaServices.cs:218-247` (Backend) |
| Documentos INE | No moderados | Declarado, sin afirmar moderación |

- 422 `content_rejected` → `field` + categorías allowlist; conserva texto en UI para corregir.
- 503 `moderation_unavailable` → `isRetryable=true`, mensaje temporal.
- No se creó un segundo servicio de moderación en Mobile. No se envió contenido real a usuarios.

## Eliminación de cuenta

- Contrato verificado: GET preview 200; POST 204; 403 `password_mismatch`, 409 `deletion_blocked`, 429, 503 `deletion_incomplete`.
- Implementado en `AccountRepositoryImpl` + `DeleteAccountUseCase` + `LocalUserDataPurgerImpl`.
- Tras 204: purga local, sesión Anonymous, back stack limpio hacia login (`MainActivity.kt:1009-1029`).
- Sin cuenta desechable autorizada → **ACCOUNT_DELETION_RUNTIME: RUNTIME_PENDING**. No se ejecutó eliminación real.

## Machine Learning

| Artefacto | Mobile | Backend |
|---|---|---|
| `model_v1.json` | SÍ (`assets/smartmatch/`) | — |
| `model.onnx` | NO | SÍ (autoridad) |
| `golden_cases.json` | NO | SÍ |
| `feature_schema.json` | NO | SÍ |
| `SHA256SUMS` | NO | SÍ |
| Versión servidor | `logreg-1.0.0` | confirmada |
| Equivalencia local ↔ ONNX | **UNKNOWN** (sin pruebas de paridad) | — |

- Mobile **NO ejecuta ONNX**. El score lo calcula el Backend (autoridad); el fallback local es una estimación etiquetada «Estimación local».
- ONNX on-device queda **fuera de alcance** salvo ampliación explícita (instrucción FASE C §14).

## GenAI

- Endpoints reales: `GET /api/v1/smart-match/explanations/{roomId}` (auth), `POST /api/v1/smart-match/explanations/query` (guest).
- D21 los consume bajo demanda desde «¿Por qué es compatible contigo?» → `SmartMatchViewModel.loadExplanation()`.
- `generatedBy` se muestra honestamente: «Explicación generada con IA» (genai) vs «Explicación basada en tus preferencias» (template).
- El score NUNCA se recalcula mediante GenAI; factores numéricos y explicación textual permanecen separados.
- Mobile no llama a proveedores externos ni guarda API keys de IA.
