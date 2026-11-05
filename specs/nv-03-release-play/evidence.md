# NV-03 — EVIDENCIA DE EJECUCIÓN U2

| Campo | Valor |
|---|---|
| Spec | NV-03 Release / Google Play v1.1.0 |
| Fase | U2 |
| Fecha | 2026-11-04 |
| Autorización | Victor Manuel Jose Jose (FASE B) |

## 1. Configuración aplicada

- `applicationId`/`namespace` = `com.thehouse.app` (confirmado por autorizador, permanente tras 1er AAB).
- `versionName = 1.0.0`, `versionCode = 10000` (fórmula `VERSIONING.md`).
- Kotlin `2.2.10`, Java `17`, compileSdk/targetSdk `37`, minSdk `26` (toolchain intacto).
- `API_BASE_URL` = `https://ca-thehouse-u2.grayisland-cb722b5f.eastus2.azurecontainerapps.io/` (BuildConfig único; placeholder `https://example.invalid/` reemplazado).
- HTTP logging: true en debug, false en release (ningún token en logs).
- Signing release: `signingConfig` se arma SOLO cuando existen las 4 variables `THEHOUSE_*`; keystore `C:\THEHOUSE-KEYS\thehouse-upload.jks` fuera del repo; nada de secretos versionado.
- R8 optimización activa en release (`optimization { enable = true }`).
- Manifest/red: `INTERNET`, cleartext deshabilitado, `network_security_config` restringido al backend, `backup_rules.xml` y `data_extraction_rules.xml` excluyendo DataStore/sharedprefs (sesión/tokens).
- Play: cuenta existente (ya pagada); U2 entrega en pista interna; prueba cerrada (12 testers/14 días) NO afirmada hasta verificar tipo/fecha de cuenta. Store listing preparado con placeholders (`URL_AVISO_PRIVACIDAD`); público 18+; Data Safety pendiente de esos valores.

## 2. Comandos y resultados

```powershell
.\gradlew.bat clean assembleDebug testDebugUnitTest lintDebug   # BUILD SUCCESSFUL (1m 23s)
.\gradlew.bat bundleRelease                                      # BUILD SUCCESSFUL (2m 53s) — re-ejecutado con éxito por el autorizador con variables THEHOUSE_* (2026-11-05)
# AAB: app/build/outputs/bundle/release/app-release.aab
# jarsigner -verify -verbose -certs: "jar verified." (2026-11-05, sesión del autorizador)
```

## 3. Estado del signing (resuelto por acción humana 2026-11-05)

Las variables `THEHOUSE_*` fueron cargadas por el autorizador en SU sesión PowerShell y `bundleRelease` se ejecutó desde ahí. Resultado real (hechos comprobados):

```text
AAB: app\build\outputs\bundle\release\app-release.aab   (BUILD SUCCESSFUL)
jarsigner -verify -verbose -certs                     → "jar verified."
Firmante: CN=TheHouse Team, OU=Development, O=TheHouse, ST=Hidalgo, C=MX
Clave: RSA 2048 bits | versionName=1.0.0 | versionCode=10000
```

- Los warnings de certificado autofirmado/PKIX NO se registran como fallo: `jarsigner` terminó en `jar verified.`.
- Secretos del keystore permanecen fuera del repo; nada versionado.
- NV03-T010 (generación) y NV03-T011 (verificación de firma/integridad) quedan COMPLETADAS con esta evidencia. No se tocó código ni se ejecutó Gradle en esta actualización.

BLOQUEO RESUELTO: ~~`BLOCKED-SIGNING-ENV`~~ eliminado (2026-11-05).

## 4. Bloqueo adicional

- `URL_AVISO_PRIVACIDAD` pendiente → store listing incompleto (`BLOCKED-PRIVACY-URL`).
- Contrato de `register` del backend no aceptó los payloads razonables (ver evidencia NV-04): no se sube nada a Play U2 prematuramente; esta evidencia no requiere subir.
