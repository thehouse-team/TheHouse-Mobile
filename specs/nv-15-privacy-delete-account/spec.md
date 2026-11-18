# NV-15 — PRIVACY + DELETE ACCOUNT

| Campo | Valor |
|---|---|
| Spec ID | NV-15 |
| STATUS | PARTIAL — Mobile implementado; contratos externos BLOCKED |
| AUTHORIZATION | AUTHORIZED (instruccion humana 2026-11-17, limitada a NV-15) |
| Dependencias | NV-04 (sesion), NV-03 (Privacy Policy/Data Safety), NV-27 |

## Problema
El usuario necesita controlar sus datos personales y eliminar su cuenta de forma definitiva y verificable, cumpliendo la politica de privacidad y los requisitos de Play.

## Alcance
- Pantalla de privacidad, acceso a datos del usuario y eliminacion de cuenta con confirmacion.
- Borrado local completo (tokens, caches, DataStore por usuario) tras confirmacion del backend o, si el contrato lo define, solo local.

## No-alcance
- Endpoint de eliminacion no confirmado: sin contrato NV-27, BLOCKED (no simular borrado remoto).
- URL_AVISO_PRIVACIDAD inventada: se mantiene BLOCKED-PRIVACY-URL hasta que el humano la provea.

## Requisitos
- NV15-REQ-001: Solo endpoints confirmados por NV-27 para borrado remoto; sin contrato, BLOCKED documentado.
- NV15-REQ-002: Eliminacion local total tras exito (o tras confirmacion local si el contrato es solo-local): tokens, DataStore, caches, favoritos locales.
- NV15-REQ-003: Confirmacion en dos pasos + re-autenticacion si el contrato la exige; sin accion irreversible en un solo toque.

## Resultado de ejecucion (2026-11-17)

| Requisito | Estado | Evidencia |
|---|---|---|
| NV15-REQ-001 | BLOCKED | `DELETE-ACCOUNT-BACKEND-BLOCKED`: `origin/main` del Backend (`9dfb497`) no expone ruta de eliminacion; `nv-10-account-session/spec.md` la difiere. No se invento endpoint/DTO. |
| NV15-REQ-002 | IMPLEMENTADO (Mobile) | Purga local posterior a exito confirmado: sesion/refresh token, onboarding del usuario, favoritos en memoria, target push, notificaciones locales y cache HTTP. Tests verdes. |
| NV15-REQ-003 | IMPLEMENTADO (Mobile) | Accion "Eliminar cuenta" + dialogo de confirmacion con explicacion de irreversibilidad, cancelar, loading y bloqueo de doble submit. Re-autenticacion no aplica: no existe contrato que la exija. |
| URL aviso | BLOCKED | `BLOCKED-PRIVACY-URL`: la landing solo contiene anclas rotas `#privacidad`; no hay aviso publicado. La app no abre ni inventa URL. |

Estado de la accion remota: `AccountRepository.isDeletionAvailable = false`; la UI muestra el bloqueo y no abre una confirmacion que simule una eliminacion.

## Criterios de aceptacion
- Tras eliminar, la app vuelve a Login/Onboarding sin datos residuales del usuario. (Logica y tests locales; ejecucion remota BLOCKED.)
- La politica de privacidad enlazada es la URL real provista por el humano (BLOCKED-PRIVACY-URL hasta entonces). (Estado bloqueado implementado sin crash.)

## Riesgos
- Borrado incompleto (backups, caches) o borrado remoto simulado sin contrato.
- Perdida de datos sin confirmacion suficiente.
- Deuda: cuando exista contrato Backend y URL real, reemplazar `AccountRepositoryImpl` bloqueado y configurar `PRIVACY_POLICY_URL`.

## Impacto seguridad
- Datos personales minimizados; borrado verificado; sin PII en logs; backups excluyen sesion/tokens (NV-03/NV-04).
- La limpieza local es best-effort y limpia primero la sesion para que un reinicio no restaure la cuenta.

## COST-001 ($0)
- Free tier: backend existente + almacenamiento local. Limites: ninguno adicional. Tarjeta: no. Riesgo cobro: ninguno. Mitigacion: sin servicios de cumplimiento de pago. Alternativa: borrado solo-local si el backend no lo soporta (documentado).

## Relacion con otras specs
- NV-03 (Privacy Policy/Data Safety, BLOCKED-PRIVACY-URL), NV-04 (sesion), NV-14 (notificaciones locales), NV-20 (MASVS), NV-27 (contrato).

## Evidencia esperada
- Prueba de borrado local/remoto y checklist de datos residuales. Ver `evidence.md` (Fase D3).
