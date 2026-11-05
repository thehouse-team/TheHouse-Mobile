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
.\gradlew.bat bundleRelease                                      # BUILD SUCCESSFUL (2m 53s)
# AAB: app/build/outputs/bundle/release/app-release.aab
# jarsigner -verify: "jar is unsigned" en la shell del agente
```

## 3. Estado del signing (pendiente humano)

Las variables `THEHOUSE_KEYSTORE_PATH | THEHOUSE_KEYSTORE_PASSWORD | THEHOUSE_KEY_ALIAS | THEHOUSE_KEY_PASSWORD` fueron cargadas por el autorizador en SU sesión, pero NO están visibles en la shell del agente (ni Machine ni User). Por decisión de seguridad (no terminar secretos vía prompt) el agente NO las establecó. Resultado:

```text
AAB release SIN FIRMAR en esta sesión. Tarea de cierre con autorización humana (sesión con variables):
  .\gradlew.bat bundleRelease
  jarsigner -verify app\build\outputs\bundle\release\app-release.aab   # debe decir "jar verified"
```

`BLOCKED-SIGNING-ENV` (no bloquea el resto de U2).

## 4. Bloqueo adicional

- `URL_AVISO_PRIVACIDAD` pendiente → store listing incompleto (`BLOCKED-PRIVACY-URL`).
- Contrato de `register` del backend no aceptó los payloads razonables (ver evidencia NV-04): no se sube nada a Play U2 prematuramente; esta evidencia no requiere subir.
