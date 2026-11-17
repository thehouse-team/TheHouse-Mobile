# NV-14 — PLAN

| Campo | Valor |
|---|---|
| Spec | NV-14 Notifications |
| Spec oficial | `specs/nv-14-notifications-fcm/` |
| Fase A | In-App Notifications — **APPROVED / IMPLEMENTED & TESTED (unit)** — runtime pendiente (emulador ADB sin framework operativo; sin credenciales) |
| Fase B | FCM / Push remoto — **APPROVED / IMPLEMENTED & TESTED (unit/build)** — runtime **FCM-RUNTIME-PARTIAL** |

## Precondiciones de gobernanza (cumplidas en esta preparación)
- Preservar estado actual (`feature/smart-match-ml`, sin reset/restore/clean/stash/force push).
- Actualizar únicamente documentación NV-14 existente (sin duplicar spec, sin switch/pull/rama nueva/commit/push).

## Fase A — Plan de implementación (trazable, PENDING hasta autorización explícita)

> Reutilizar Retrofit, Session/JWT, `DomainError`, Design System y Navigation existentes. Patrón: `Api → DTO → mapper → repository → ViewModel/use case → Compose`. **NO** segundo Retrofit, **NO** refactor global, **NO** requests desde Compose, **NO** WorkManager cada 30 s.

| Task | Objetivo | Verificable (evidencia esperada) |
|---|---|---|
| **T01** Contratos/DTOs | Modelar contratos reales (list, unread-count, read, read-all) y DTOs/mappers | DTOs y mapeos alineados a payloads reales; sin campos inventados |
| **T02** Repository/use cases | Api + repository + casos de uso (list/filtros/paginación, unread-count, read, read-all) | Lógica de dominio/datos aislada, errores mapeados a `DomainError` |
| **T03** Notification center UI | Centro: Loading/Empty/Content/Error, filtros todas/requests/visits/system, unread, paginación, items (title/body/count/read/fecha/indicador accesible), badge global | UI consume estado del ViewModel; `title/body` del backend sin reescritura; badge oculto si unread=0 |
| **T04** Unread count + ETag/304 | Guardar `ETag`, enviar `If-None-Match`, tratar `304` sin modificar estado | 200 actualiza; 304 preserva estado previo |
| **T05** Polling/foreground | Unread count ~30 s solo foreground, refresh al volver, detener cuando no aplique, `CancellationException` rethrow | Sin polling en background; sin recomposición como trigger; timing ~30 s |
| **T06** Target navigation | Mapear targets a rutas reales existentes (request/conversation/visit/agreement/rental/building) | Navegación correcta o bloqueo documentado si falta ruta; sin rutas inventadas |
| **T07** Read/read-all + chat integration | `POST /{id}/read` al abrir no leída (tras confirmación); `read-all` único (+ categoría opcional); `/conversations/{id}/read` marca aviso leído; evitar duplicados; refrescar unread | Sin POST individual masivo en read-all; chat read integrado sin duplicación |
| **T08** Tests/regresión | Unit tests: list/filtros/paginación, unread 200, ETag/If-None-Match/304 mantiene estado, read 204/404, read-all (+ categoría), polling ~30s/start-stop, foreground refresh, targets, count agrupado, integración chat read. Regresión D3–NV17 | Resultados reales; sin ocultar fallos |
| **T09** Runtime/evidence | Validación build/tests/lint/assemble (según autorización), runtime ADB si disponible o RUNTIME-BLOCKED, `evidence.md` con comandos/salidas/artefactos | Evidencia auditable; BLOCKED nunca equivale a DONE |

### Validación prevista (solo cuando se autorice implementación)
1. `.\gradlew.bat compileDebugKotlin`
2. Si verde: `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` (set completo una vez, máximo 1 ciclo correctivo)

### Fuera de Fase A
- FCM/push remoto, Firebase, permisos push, token FCM.
- Rutas/targets inexistentes inventados, refactors globales, segundo Retrofit.
- Polling 1 s, WorkManager cada 30 s, requests desde recomposición.

## Fase B — Plan de implementación FCM / Push (EJECUTADO, runtime parcial)

> Precondición: Fase A sin regresiones. FCM es evento/señal; el estado real sigue viniendo del Backend. Sin endpoints/tokens inventados.

| Task | Objetivo | Verificable (evidencia) |
|---|---|---|
| **B-T01** Contrato token/device | Buscar contrato real de registro de token/device en Backend/specs | **FCM-TOKEN-BACKEND-BLOCKED**: no existe contrato; sin endpoint inventado |
| **B-T02** COST-001 | Demostrar FCM $0 (free tier, límites, tarjeta, mitigación, alternativa) | Documentado en spec/evidence; sin billing ni servicios extra |
| **B-T03** Firebase/FCM | BoM `35.2.0`, `firebase-messaging`, plugin `google-services` condicional a `google-services.json` | Build sin archivo real; `FCM_CONFIGURED=false`; sin secreto falso |
| **B-T04** Token/sesión/logout | `onNewToken` + consulta al activar sesión; validación; solo memoria; fingerprint; logout local | Tests token; sin token en logs; sin registro Backend |
| **B-T05** Recepción foreground/background | Parser data-only; foreground solo señal; background notificación local; sesión inválida suprime | Tests handler foreground/background/invalid/session |
| **B-T06** Canales | `thehouse_requests`, `thehouse_visits`, `thehouse_system` con nombre/descripción/importancia | `NotificationChannelRegistry`; sin sonidos propios |
| **B-T07** Permiso Android 13+ | `POST_NOTIFICATIONS` contextual en Notification Center, rationale, rechazo seguro, <13 sin runtime | Policy tests 32/33, granted/denied/permanent |
| **B-T08** Navegación segura | Mapper único target→ruta; cold start/onNewIntent; pending target sobrevive Login; fallback Notification Center | Navigation/intent tests; MainActivity singleTop |
| **B-T09** Tests/regresión | Parsing, target desconocido, 6 navegaciones, onNewToken, permiso, foreground/background, sesión, no token en logs | 47 tests nuevos + regresión completa |
| **B-T10** Build/evidencia | `testDebugUnitTest lintDebug assembleDebug` una vez; runtime ADB/Firebase si existe | Evidencia real; **FCM-RUNTIME-PARTIAL** si falta configuración/sender/ADB |

### Validación ejecutada (Fase B)
1. `.\gradlew.bat compileDebugKotlin` → verde (tras corregir `goAsync` inexistente en Service).
2. `.\gradlew.bat compileDebugUnitTestKotlin` → verde.
3. Tests dirigidos NV-14 → 56/56 verdes.
4. Set final: `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` → ver `evidence.md`.

### Fuera de Fase B
- Registro/desregistro de token en Backend (sin contrato → `FCM-TOKEN-BACKEND-BLOCKED`).
- Envío real desde consola/Backend (sin proyecto Firebase/sender → runtime parcial).
- FID/installation-id migration, topics, campañas, Analytics, Cloud Functions.
- Cambios en Fase A, refactors globales, segundo Retrofit.
