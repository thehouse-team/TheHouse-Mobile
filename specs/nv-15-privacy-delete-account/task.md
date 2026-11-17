# NV-15 - TASKS (FASE D2/D3)

| Campo | Valor |
|---|---|
| Spec | NV-15 Privacy + Delete Account |
| STATUS | PARTIAL — Mobile implementado; delete remoto y URL real BLOCKED |
| AUTHORIZATION | AUTHORIZED (instruccion humana 2026-11-17, limitada a NV-15) |

- [x] NV-15-T01 - Confirmar contrato de eliminacion (NV-27) o BLOCKED. [Verificado 2026-11-17: `DELETE-ACCOUNT-BACKEND-BLOCKED`; sin ruta/DTO inventados]
- [x] NV-15-T02 - Confirmar URL_AVISO_PRIVACIDAD real o mantener BLOCKED-PRIVACY-URL. [Verificado: landing sin aviso real → `BLOCKED-PRIVACY-URL`]
- [x] NV-15-T03 - Caso de uso DeleteAccount + limpieza local total. [Implementado con guard de contrato y purga solo tras exito]
- [x] NV-15-T04 - UI privacidad y eliminacion con doble confirmacion. [Implementada; accion deshabilitada sin contrato, sin URL falsa]
- [x] NV-15-T05 - Tests de limpieza sin residuales. [5 clases nuevas; sesion, purga, URL y ViewModel]
- [x] NV-15-T06 - Build/tests/lint y evidence.md. [Ejecutado: 323 tests, lint 0 errores, assembleDebug OK]
- [x] NV-15-T07-D2 - Auditar URLs (privacy/terms/play) y documentar ACTUAL vs PLANNED sin inventar tratamientos.
- [ ] NV-15-T08 - Integracion remota real de eliminacion. [BLOQUEADA: requiere contrato Backend NV-27]
- [ ] NV-15-T09 - Enlace al aviso de privacidad publicado. [BLOQUEADO: requiere URL real; configurar `PRIVACY_POLICY_URL`]
- [ ] NV-15-T10 - Validacion runtime en dispositivo/emulador. [PENDIENTE: sin evidencia ADB en esta sesion]
