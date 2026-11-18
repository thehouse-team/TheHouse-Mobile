# NV-15 — CHECKLIST

- [x] Contrato de eliminación verificado: NO existe → `DELETE-ACCOUNT-BACKEND-BLOCKED` (sin endpoint/DTO inventado)
- [x] URL_AVISO_PRIVACIDAD auditada: NO válida → `BLOCKED-PRIVACY-URL` se mantiene
- [x] Aviso de privacidad sin crash cuando falta o es inválida la URL (estado bloqueado, sin abrir nada)
- [x] URL HTTPS validada (absoluta, host obligatorio) y abierta con intent seguro
- [x] Doble confirmación implementada (acción + diálogo irreversible)
- [x] Cancelar no ejecuta ninguna request
- [x] Loading + guard bloquean doble submit (un solo request)
- [x] Éxito limpia sesión/tokens, onboarding del usuario, favoritos, target push, notificaciones y caché HTTP
- [x] Reinicio sin restore tras limpieza local (test `LocalSessionClearTest`)
- [x] Fallo de red/servidor conserva sesión y datos locales
- [x] Errores seguros sin body crudo ni stack traces
- [x] Tests unitarios ejecutados: 323 tests, 0 fallos
- [x] `lintDebug`: 0 errores (20 warnings, ninguno en `presentation/privacy`)
- [x] `assembleDebug`: BUILD SUCCESSFUL
- [x] Evidencia registrada en `evidence.md`
- [ ] Eliminación remota real: BLOQUEADA por contrato Backend NV-27
- [ ] Aviso de privacidad real: BLOQUEADO hasta URL publicada
- [ ] Validación runtime en dispositivo/emulador: PENDIENTE (sin evidencia ADB)
