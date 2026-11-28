# D21 — Matriz OWASP MASVS (auditoría dirigida)

Metodología: análisis estático dirigido + revisión de contratos + pruebas locales reales. No se afirma cumplimiento completo de MASVS solo por análisis estático.

| Control | Aplicabilidad | Evidencia | Resultado | Hallazgo | Severidad | Corrección D21 | Riesgo residual |
|---|---|---|---|---|---|---|---|
| MASVS-STORAGE-1 (tokens) | Sí | `EncryptedRefreshTokenStorage.kt`: AES-256/GCM, Keystore no exportable, IV 12B, tag 128b, DataStore Base64 | PASS | Ninguno | — | — | Bajo: depende de Keystore del dispositivo |
| MASVS-STORAGE-2 (backup) | Sí | `backup_rules.xml`/`data_extraction_rules.xml` excluyen `datastore/` y `sharedpref` | PASS | Ninguno | — | — | Bajo |
| MASVS-STORAGE-3 (installationId) | Sí | `DataStoreInstallationIdStore`: UUID D en DataStore no sensible; sin IMEI/MAC/AndroidId | PASS | No existía | Medio | Implementado | Bajo; se pierde al reinstalar (documentado) |
| MASVS-CRYPTO-1 (TLS) | Sí | `network_security_config.xml`: cleartext=false global y por dominio; Retrofactory exige `https://` | PASS | Ninguno | — | — | Sin pinning: rotación no requiere release |
| MASVS-NETWORK-1 (HTTPS) | Sí | BuildConfig `API_BASE_URL` HTTPS productivo | PASS | Ninguno | — | — | — |
| MASVS-NETWORK-2 (logging) | Sí | `SafeHttpLoggingInterceptor`: método + path saneado + status + requestId; sin headers/bodies/query | PASS | Ninguno | — | — | — |
| MASVS-AUTH-1 (sesión persistente) | Sí | `TokenRefresher` single-flight (monitor JVM), 401/403 tipados, refresh rotado cifrado | PASS | Ninguno | — | — | Medio: refresh revocado depende del Backend |
| MASVS-AUTH-2 (logout) | Sí | `D21LogoutUseCase`: DELETE devices antes de revocar; `finally` purga y aísla cuenta | PASS | Sin desvinculación remota | Alto | Implementado con outcome real | Si DELETE falla queda dispositivo remoto; **no se afirma eliminado** |
| MASVS-AUTH-3 (separación de cuentas) | Sí | `DeviceSyncCoordinator.clearForAccountSwitch()`: cancel jobs, limpia token y pending | PASS | Riesgo de herencia A→B | Alto | Implementado | Bajo |
| MASVS-AUTHZ-1 (BOLA) | Sí | DELETE Backend con scope userId+installationId (verificado); Mobile envía installationId propio | PASS | Ninguno en Mobile | — | — | Backend 404 para ajeno |
| MASVS-PLATFORM-1 (exported) | Sí | Solo MainActivity exported con MAIN/LAUNCHER; FCM service exported=false | PASS | Ninguno | — | — | — |
| MASVS-PLATFORM-2 (deep links) | Sí | Sin ACTION_VIEW/BROWSABLE; allowlist de `NotificationTargetKind` + regex SAFE_ID + rutas tipadas | PASS | Ninguno | — | — | — |
| MASVS-PLATFORM-3 (permisos) | Sí | Matriz `D21_PERMISSIONS_MATRIX.md`; `ACCESS_NETWORK_STATE` añadido por uso real | PASS | Permiso usado sin declarar | Medio | Declarado | — |
| MASVS-CODE-1 (secretos) | Sí | Sin service-account en repo; `google-services.json` contiene API key **pública de cliente**. La restricción por package/SHA **no es verificable desde Mobile**. | WARN | API key versionada | Medio | Documentado; allowlist Gitleaks limitada a ese archivo; no imprimir valor; confirmar restricción con responsable Firebase | Medio: configuración Firebase estándar; depende de control externo no verificable aquí |
| MASVS-CODE-2 (integridad release) | Parcial | Signing desde env `THEHOUSE_*`; `optimization.enable=false` | PASS | Sin Play App Signing (fuera de alcance) | Bajo | — | Medio: release no publicado |
| MASVS-RESILIENCE-1 (ofuscación) | No evaluado | Sin R8 en release (optimization=false) | NOT_APPLICABLE | Alcance académico; sin secretos en cliente | — | — | Medio |
| MASVS-CODE-3 (dependencias) | Sí | Trivy HIGH/CRITICAL en CI (informativo); Firebase BoM 35.2.0 | WARN | Trivy `exit-code=0` | Medio | Sin cambiar umbral (no autorizado) | Medio: hallazgos se reportan, no bloquean |

## Detalle de las dos advertencias (WARN)

| Control | Causa | Riesgo | Corrección viable en Mobile | Prueba | Riesgo residual |
|---|---|---|---|---|---|
| MASVS-CODE-1 — API key pública versionada en `app/google-services.json` | El plugin `google-services` requiere el archivo; es una clave de cliente, no service-account ni credencial Backend. La restricción por package/SHA no se puede comprobar desde el repositorio Mobile. | Medio: posible abuso de cuota del proyecto Firebase si la restricción externa no existe. | Mantener allowlist de Gitleaks **solo** para ese archivo, no imprimir el valor, no añadir service account, y reportar como pendiente de confirmación externa. No eliminar el archivo sin autorización (deshabilitaría FCM). | `gitleaks detect --config .gitleaks.toml` sin hallazgos fuera de allowlist; grep `AIza|private_key|service_account` → solo la entrada conocida. | Medio, aceptado y documentado. No es un secreto Backend. |
| MASVS-CODE-3 — Trivy `exit-code=0` | Decisión deliberada: NV-02/NV-18 no autorizan cambiar umbrales ni crear allowlists nuevas. El escaneo HIGH/CRITICAL se ejecuta y publica tabla, pero no bloquea. | Medio: ventana entre detección y corrección de dependencias. | Ninguna corrección segura autorizada ahora. Proponer gate por severidad con excepción revisada en futura NV-02; NO subir `exit-code` por iniciativa propia. | CI del PR #37 con paso Trivy ejecutado; 0 CRITICAL/0 HIGH confirmados en alcance Mobile. | Medio, aceptado y documentado. |

## Hallazgos abiertos

- **CRITICAL_OPEN: 0** (evidencia estática + scanners disponibles).
- **HIGH_OPEN: 0 confirmados en Mobile**.
- **MEDIUM_OPEN: 3** — (1) API key pública de cliente versionada, (2) Trivy informativo, (3) sin ofuscación/Play signing.
- **LOW_OPEN: 0**.
- **SCANNER_COVERAGE**: Gitleaks (repo), Trivy fs (HIGH/CRITICAL), Android Lint, unit tests, compile androidTest.
- **UNSCANNED_AREAS**: runtime en dispositivo físico (sin adb), Backend/infra (fuera de alcance), firma release real (sin keystore).
