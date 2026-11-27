# EVIDENCIA D19.1 — Account Runtime Completion

| Campo | Valor |
|---|---|
| Spec | D19.1-Account-Runtime-Completion |
| Branch | `fix/d19-runtime-completion` |
| Base | `origin/main` @ `50febcdbe47ff80b08f14323734e6883e4249858` |
| Fecha | 2026-11-27 |
| PR | #35 (OPEN, MERGEABLE) — NO mergear |

## RESULTADOS REALES (2026-11-27, cierre funcional y visual integral)

### Causas verificadas por lectura de código y tests

- **PREFERENCIAS — guardado (P0):** la cadena HTTP/DTO/UseCase/repository ya era contractual tras el D19.1 inicial (`PUT /users/me/preferences` con `budget{min,max,currency}`, códigos cerrados, sin `currency` raíz ni `quickSetupCompleted/version/updatedAt`; un solo PUT, sin trailing GET). El defecto demostrado era de UI: CTA `Guardar preferencias` dentro de la misma Columna con `verticalScroll` (fuera del viewport), campos de catálogo como texto libre que permitían values no reconocidos y `mapNotNull(::xxxCode)` los descartaba silenciosamente, y `budgetCurrency` editable aunque el contrato solo acepta MXN. Fix: CTA fijo en `bottomBar` del Scaffold con `imePadding`, selectores Material 3 (`ExposedDropdownMenuBox`) alimentados por los catálogos contractuales y moneda fija MXN. Causa descartada: `applyPreference` ya hidrataba el formulario desde GET 200; no se revirtió.
- **SMART MATCH (P0):** backend 422 `smart_match_profile_incomplete` transporta `missing` y el repositorio lo deserializa (`SmartMatchProfileIncompleteException.missing`), pero `SmartMatchScreen` NUNCA coleccionaba `profileMissing`: mostraba un mensaje genérico y el NavHost no pasaba los campos a Preferences. Fix: state real en pantalla con etiquetas legibles, CTA a Perfil si falta `university` y ruta `preferencias?missing=...` hacia Preferencias; `PreferencesDestination` recibe `missingFields` y ofrece accesos a Perfil/Configuración inicial. Sin fallback local por 422 (D16 preservado).
- **PRIVACIDAD DESDE REGISTRO (P1):** enlace, ruta pública y `GET /legal` ya estaban cableados. Defecto real: `PrivacyViewModel` conservaba una URL local previa cuando el backend devolvía `privacyUrl=null` o fallaba `GET /legal`, pudiendo abrir una URL obsoleta. Fix: URL se limpia en ambos casos y se muestra «El aviso de privacidad aún no ha sido publicado.». Runtime: backend publicó `privacyUrl` válido; enlace abre Chrome y regreso al registro conserva la pantalla. NO se inventó contenido legal ni consentimiento automático.
- **MIS ALOJAMIENTOS (P1):** el fix del factory (`LandlordProfileViewModel`) permanece verificado por `D19CrashRegressionTest` y por el nuevo test de regresión. Sin credenciales de arrendador en el dispositivo: **RUNTIME_PENDING** (no se inventaron perfiles ni roles JWT).
- **HEADER DUPLICADO (P1):** `HomeScreen` renderizaba su propio `BrandMark()` además del header global del shell. Fix: eliminado; capture real muestra un solo TheHouse.
- **DRAWER (P1):** sin iconos, jerarquía ni selección. Fix: catálogo puro con `IconKey` por entrada, encabezado de marca, secciones "Mi cuenta / Arrendador / Sesión y legal", separadores, área táctil 48dp, scroll, insets del `ModalDrawerSheet` sin duplicar y estado seleccionado por ruta. Todas las rutas guest/auth permanecen.
- **UNIVERSIDAD SYNC (P1):** Home solo leía `SelectedUniversityRepository` local; Perfil guardaba la universidad en backend sin actualizar esa copia. Fix: `HomeViewModel` recarga perfil con sesión activa y observa `profileRepository.universityState`, priorizando la universidad remota guardada.
- **HOME → SMART MATCH (P1):** la tarjeta promocional no era navegable. Fix: `SmartMatchPromo` es clickable y navega a la pestaña Smart Match.
- **INSTRUMENTACIÓN:** `ProfileFlow`, `PreferencesFlow` y `SmartMatchFlow` activos en DEBUG con logger seguro (sin PII/tokens/bodies); `TheHouseHttp` registra status/requestId; `TheHouseHttp`/`AndroidRuntime` sin FATAL EXCEPTION en las capturas.

### Gates locales

| Comando | Resultado |
|---|---|
| `clean compileDebugKotlin --offline` | BUILD SUCCESSFUL |
| `testDebugUnitTest --offline` | BUILD SUCCESSFUL — **534 tests / 0 failures / 0 errors** (baseline 524 + 10 nuevos) |
| `lintDebug --offline` | BUILD SUCCESSFUL (0 errores) |
| `assembleDebug --offline` | BUILD SUCCESSFUL |
| `git diff --check` | exit 0 |

### Tests nuevos (10, ninguno eliminado)

`D191FunctionalVisualCompletionTest`: payload PUT exacto (sin currency raíz ni campos de servidor), budget sin campo extra, ruta `missing` del 422, etiquetas reales de campos faltantes, iconos del drawer, logo único en Home, CTA fijo + catálogos en Preferencias, URL legal null/fallo sin URL obsoleta (2).

### Certificación runtime (emulador-5554, APK del HEAD final)

| Prueba | Clasificación |
|---|---|
| Inicio con un solo logo TheHouse + hamburger | **RUNTIME_PASS** (uiautomator: un solo `TheHouse`; screenshot `d191-home2.png`) |
| Drawer guest con marca, iconos, sección legal | **RUNTIME_PASS** (screenshot `d191-drawer.png`) |
| Registro → Aviso de privacidad → abrir navegador → regresar | **RUNTIME_PASS** (Chrome abierto; regreso a "Crea tu cuenta") |
| Smart Match guest (NoPreferences, logs DEBUG) | **RUNTIME_PASS** (screenshot `d191-smartmatch.png`, `SmartMatchFlow` con `state=NoPreferences`) |
| Sin FATAL EXCEPTION en arranque/navegación | **RUNTIME_PASS** (`AndroidRuntime:E` vacío) |
| Preferencias autenticadas (GET/PUT/persistencia/CTA con teclado) | **RUNTIME_PENDING** (sin credenciales autorizadas) |
| Smart Match 422 con `missing` real + detalle de cuarto | **RUNTIME_PENDING** (sin perfil completo autenticado) |
| Mis alojamientos 404/200 + landlord_required | **RUNTIME_PENDING** (sin credenciales de arrendador) |

`EXTERNAL_BLOCKER`: no aplica en este HEAD — `GET /legal` devolvió `privacyUrl` HTTPS válido y se abrió en Chrome; el contenido legal continúa siendo responsabilidad del Backend.

### Resultado

`D19.1-FUNCTIONAL-AND-VISUAL-COMPLETION-CODE-READY`. No se declara `RUNTIME-CERTIFIED`: faltan pruebas autenticadas. NO se mergeó PR #35.
