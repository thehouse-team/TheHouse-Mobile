# D21 — Hotfix de estabilidad y conectividad (emulador, 2026-11-28)

Mismo PR #37, rama `feature/platform-security-automation`, sin merge. Sin cambios visuales.

## Evidencia recuperada (antes de limpiar logs)

Crash buffer `adb -s emulator-5554 logcat -b crash -d`, incidente 17:28 del 27-Nov-2026, PID 3067:

```
FATAL EXCEPTION: main
java.net.SocketTimeoutException: timeout
  Http2Stream$StreamTimeout.newTimeoutException:731
  Http2Stream.takeHeaders:159
  SafeHttpLoggingInterceptor.intercept:32
  BearerTokenInterceptor.intercept:40
  RealCall$AsyncCall.run:582
Suppressed: DiagnosticCoroutineContextException:
  [StandaloneCoroutine{Cancelling}, Dispatchers.Main.immediate]
```

Los incidentes de 17:51 (PID 15910) y 18:02 (PID 16293) solo muestran `FATAL EXCEPTION: main` sin stack en el buffer; **no se asume** que tengan idéntica causa. El crash de `com.google.android.bluetooth` es del sistema, no de TheHouse.

## Causa raíz Mobile (demostrada por código)

1. No existía `CoroutineExceptionHandler` en `viewModelScope` (`BaseViewModel.launch`) ni en `applicationScope` (`AppContainer`). Cualquier excepción que escapara de un `launch` fire-and-forget llegaba al handler por defecto y terminaba en `FATAL EXCEPTION: main`. El suppressed `Dispatchers.Main.immediate` coincide exactamente con `viewModelScope`.
2. `AuthRepositoryImpl.login()`/`register()` evaluaban `api.login()/register()` **antes** de entrar al `try/catch` de `handleAuthResponse`, por lo que un `SocketTimeoutException`/`UnknownHostException` salía sin tipar hasta un `viewModelScope.launch` de Login/Register.
3. Las cargas de catálogo en `HomeViewModel`/`ExploreViewModel` y el reintento de `DeviceSyncCoordinator` usaban `launch` sin contención de `IOException`.

`SafeHttpLoggingInterceptor:32` y `BearerTokenInterceptor:40` aparecen en el stack porque son eslabones de la cadena OkHttp; **no son la causa**. No se modificó su comportamiento de logging ni de cabeceras.

## Corrección

| Archivo | Cambio |
|---|---|
| `presentation/common/SafeCoroutineLauncher.kt` (nuevo) | Handler que contiene `IOException`/`SerializationException`, ignora cancelación normal y registra `unexpected=` para defectos de programación; nunca escribe mensajes de excepción (evita URLs/PII) |
| `presentation/common/BaseViewModel.kt` | `launch()` usa el handler |
| `di/AppContainer.kt` | `applicationScope` usa el handler |
| `data/auth/repository/AuthRepositoryImpl.kt` | `safeAuthCall()` tipa timeout/IOException a `Result.failure` **sin limpiar una sesión válida**; `CancellationException` sigue propagando |
| `presentation/home/HomeViewModel.kt`, `presentation/explore/ExploreViewModel.kt` | cargas de universidades sin escape de `IOException`; cancelación tratada como normal |
| `domain/device/DeviceSyncCoordinator.kt` | reintento de registro con `Result` contenido y reprogramación ante fallo de red |

No se cambiaron timeouts (connect 15 s, read 30 s, write 30 s, call 60 s), URL, DNS, TLS, certificados ni interceptores de red.

## Causa raíz DNS del emulador (ambiente, no Mobile)

- Windows: DNS correcto, TCP 443 y `GET /api/v1/universities` → 200/10 universidades.
- Emulador `emulator-5554`: `ping google.com` y `ping ca-thehouse-u2...` → `unknown host`; `NetworkMonitor` de Android también falla para `connectivitycheck.gstatic.com`, `www.google.com` y `play.googleapis.com`.
- `dumpsys connectivity`: WiFi y móvil anuncian `DnsAddresses: /10.0.2.3` (NAT del emulador), pero la resolución falla también para dominios de Google → fallo de reenvío DNS del entorno host/emulador.
- **No se aplicó fix en la app**: prohibido hardcodear IP/DNS y no es un defecto Mobile. Sin cambios de red persistentes: WiFi se restauró (`wifi_on=1`).

Duración de la consulta de universidades: antes ~40 s (logs históricos `40240`/`40673` ms); en la reproducción con el hotfix, 20.5 s (00:28:00.754 → 00:28:21.278) y 10.1 s en el reintento (00:29:54.802 → 00:30:04.861). La diferencia obedece a condiciones DNS del emulador, no a cambios de timeout.

## Reproducción en emulador (evidencia real)

APK del hotfix instalado con `adb install -r` (datos preservados). Cráneo del Android actual:

| Prueba | Resultado |
|---|---|
| Arranque con DNS roto | App viva (PID 17614), `RoomSearchFlow error_type=UnknownHostException` → `state_transition to=Error`, sin crash en 75 s |
| Reintento manual desde Home | Nueva request, error tipado, app viva (PID 17614) |
| Navegación sin conectividad (Explorar y Smart Match) | App viva; Smart Match `exception=DomainException`, `fallback`, `state=Error`; sin crash |
| Arranque con WiFi desactivado | App viva (PID 18015), sin crash; WiFi restaurado después |
| Stack trace completo del incidente 17:28 | Recuperado del crash buffer; no se pudo reproducir ese mismo `SocketTimeoutException` porque el emulador falla en DNS (UnknownHost), no en read timeout. Por eso `CRASH_REPRODUCED` se reporta parcial |

## Gates y cobertura

- `clean compileDebugKotlin`, `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `compileDebugAndroidTestKotlin`, `assembleDebugAndroidTest`, `jacocoDebugTestReport` → BUILD SUCCESSFUL.
- `git diff --check` → exit 0.
- **716 tests / 0 failures / 0 errors** (702 anteriores + 14 nuevos de regresión offline).
- JaCoCo: líneas 7285/21941 = **33.20 %**; ramas 2817/8917 = **31.59 %**.
- Sin cambios en pantallas, navegación, colores, tipografías, espaciados ni galerías.
