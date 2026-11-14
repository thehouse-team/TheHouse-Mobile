# NV-14 — PLAN

| Campo | Valor |
|---|---|
| Spec | NV-14 Notifications |
| Spec oficial | `specs/nv-14-notifications-fcm/` |
| Fase A | In-App Notifications — **APPROVED / IMPLEMENTED & TESTED (unit)** — runtime pendiente por entorno |
| Fase B | FCM / Push remoto — **PLANNED** — **DO NOT EXECUTE** |

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

## Fase B — FCM / Push remoto (PLANNED, DO NOT EXECUTE)

| Etapa (documental, no autorizada) | Alcance |
|---|---|
| B1 | Contrato de eventos/token + evaluación COST-001 Firebase/FCM (o BLOCKED) |
| B2 | Configuración Firebase/FCM + registro/desregistro de token (sesión/logout) |
| B3 | Manejo foreground/background/deep links; sin PII en payloads/bloqueo |
| B4 | Calidad/evidencia: build/tests/lint + prueba real |

- Firebase MCP permanece deshabilitado hasta autorización explícita de Fase B (AGENTS.md §18).
- No agregar Firebase, no pedir permisos push, no crear token FCM ahora.
