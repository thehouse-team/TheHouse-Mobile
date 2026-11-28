# D21 — Evidencia

## Baseline
- `git rev-parse HEAD` = `54ece77747d5f3416bdf189ee1ede597c6a40f8f`
- `gh pr view 36 --json state,mergeCommit` = `MERGED`, mergeCommit `54ece77...`
- Working tree limpio antes de crear la rama.

## Gates ejecutados (salida real)

| Comando | Resultado |
|---|---|
| `.\gradlew.bat clean compileDebugKotlin --no-daemon` | BUILD SUCCESSFUL |
| `.\gradlew.bat compileDebugAndroidTestKotlin --no-daemon` | BUILD SUCCESSFUL |
| `.\gradlew.bat testDebugUnitTest --no-daemon` | BUILD SUCCESSFUL |
| `.\gradlew.bat lintDebug assembleDebugAndroidTest --no-daemon` | BUILD SUCCESSFUL |
| `git diff --check` | exit 0 (verificado antes del commit) |

Conteo real desde `app/build/test-results/testDebugUnitTest/TEST-*.xml` (93 suites): **631 tests, 0 failures, 0 errors**.
Baseline 602 → +29 nuevos (27 D21 device/moderation/coordinator + 2 GenAI).

## Hallazgos corregidos con evidencia

1. `androidTest` no compilaba: 378 errores en 4 archivos. Reescritos con ActivityScenario/Compose reales → `compileDebugAndroidTestKotlin` SUCCESS.
2. `dexBuilderDebugAndroidTest` fallaba por nombre de método con espacios en `LoginScreenInstrumentedTest.kt` → renombrado → `assembleDebugAndroidTest` SUCCESS.
3. `DeviceSyncCoordinator` reintentaba inmediatamente si la red ya estaba online → se exige transición offline→online.
4. FCM token se obtenía pero nunca se enviaba al Backend (`FCM-TOKEN-BLOCKED`) → DeviceApi + coordinador implementados.

## Estado runtime

- `adb` no disponible en el entorno → sin dispositivo físico/emulador.
- `connectedDebugAndroidTest`: **RUNTIME_PENDING**.
- FCM real delivery: **EXTERNAL_BLOCKER** (Backend no expone endpoint de envío; requiere `FIREBASE_SERVICE_ACCOUNT`).
- Eliminación real: **RUNTIME_PENDING** (sin cuenta desechable autorizada).

## ADENDA NV-20 v1.3.0 — evidencia

- PR Backend #103: `MERGED`, head `d23bb3cd63ede2b515c01efd1e1317068ab8f51a`, título «docs(NV-20): 1.3.0 propuesta — 6 avisos nuevos». Documento leído en modo solo lectura (§6.1, §9.1).
- Contrato propuesto ≠ mergeado ≠ productivo: el documento marca 1.3.0 como PROPUESTA pendiente de T09–T12; Backend main expone `Type`/`Kind` como `string`; despliegue de los 6 tipos NO confirmado (`health/live` OK, sin credenciales para bandeja).
- Seis tipos literales extraídos del contrato (no de etiquetas): `agreement_proposed`, `agreement_cancelled`, `review_received`, `review_hidden`, `favorite_unavailable`, `report_resolved`.
- `targetKind` nuevos: `room` (targetId id cuarto → `GET /rooms/{id}`, `RoomDetailRoute`), `none` (bandeja, sin exigir targetId).
- Sin enums estrictos en DTO (`String`), por lo que no hay `SerializationException` por valores futuros; los tipos se mapean a destino tipado con fallback.
- Tests nuevos: 14 (10 compatibilidad NV-20 + 4 room target HTTP). Total 645 / 0 failures / 0 errors (631 + 14).
- Gates tras adenda: `compileDebugKotlin`, `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest` — todos SUCCESS.

---

## ADENDA VISUAL — evidencia (2026-11-27)

Gobernanza corregida por instrucción humana: trabajar sobre la rama existente
`feature/platform-security-automation` y el PR #37 abierto; sin rama nueva, sin PR #38,
sin merge; registrar como adenda visual de D21.

### Verificación previa del PR
- `gh pr view 37 --json number,title,state,baseRefName,headRefName,mergeable,url` =
  `37 | D21 — Platform, Security and Automation | OPEN | main | feature/platform-security-automation | MERGEABLE | https://github.com/thehouse-team/TheHouse-Mobile/pull/37`.
- Rama local sincronizada: `git pull --ff-only origin feature/platform-security-automation` → Already up to date.
- HEAD antes de la adenda: `735a697` (D21 NV-20 v1.3.0).
- Se eliminó el intento preliminar de rama/spec D22: `git branch -D feature/d22-visual-polish-ui` y
  `Remove-Item specs/d22-visual-polish-ui`. No quedan artefactos D22.

### Cambios visuales (solo presentación)
- Home (`HomeScreen.kt`): nuevo composable privado `PublishRentCta` (Surface de acento con
  `primaryContainer`, borde `primary` suave, icono `AddHome` y flecha) ubicado después de
  `SmartMatchPromo` y antes del header "Recomendados para ti"; el CTA secundario original al final
  se eliminó. "Privacidad" pasó de tarjeta `TheHouseCard` a acceso secundario compacto con
  `TheHouseTextAction` + icono, conservando `onOpenPrivacy`. Jerarquía de espaciado global
  `Arrangement.spacedBy(Spacing.lg)`; CTA sin FAB, sin nueva dependencia.
- Favoritos (`FavoritesScreen.kt`): `FavoriteCard` con título `titleLarge` y precio alineado a la
  derecha, ubicación con icono, metadata en `InfoChip`, etiqueta "Demo" como `StatusChip`,
  `HorizontalDivider` antes del CTA "Quitar de favoritos" (mismo `onRemove` / `removing`).
- Conversación (`ConversationScreen.kt`): `LazyColumn` con `spacedBy(Spacing.sm)` y
  `contentPadding`; estado vacío "Aún no hay mensajes" sin nuevo estado de ViewModel; burbujas con
  `padding(vertical = Spacing.xs)`, `widthIn(min = 96.dp, max = 320.dp)`, elevación `Elevations.card`
  del tema y etiqueta `labelMedium`; `HorizontalDivider` sobre el composer; contador
  "N / 1000 caracteres" (mismo valor `text.length`, sin cambiar el límite `take(1000)` del ViewModel);
  `FilledIconButton` con colores primarios explícitos.

### Confirmación de alcance cero funcional
- `git diff --name-only | Where-Object { $_ -match 'ViewModel|Dtos|Repository|UseCase|MainActivity|AndroidManifest|ui/theme|Domain|data/|notification|device/' }`
  → 0 archivos.
- Eventos preservados: `HomeEvent.PublishClicked`, `onOpenPrivacy`,
  `SmartMatchPromo(onClick = onNavigateToSmartMatch)`, `TheHouseSearchBar`,
  `Favorite.onRemove`, `ConversationEvent.ComposerChanged/Send`.
- "Nuevo edificio": NO modificado (pendiente del responsable de Backend).
- Sin Backend/GenAI/ML/ONNX/infra, contratos HTTP, secretos, permisos ni dependencias nuevas. COST-001 `$0`.

### Gates ejecutados (salida real, tras la adenda)
| Comando | Resultado |
|---|---|
| `.\gradlew.bat clean compileDebugKotlin --no-daemon` | BUILD SUCCESSFUL |
| `.\gradlew.bat testDebugUnitTest --no-daemon` | BUILD SUCCESSFUL |
| `.\gradlew.bat lintDebug --no-daemon` | BUILD SUCCESSFUL |
| `.\gradlew.bat assembleDebug --no-daemon` | BUILD SUCCESSFUL |
| `git diff --check` | exit 0 |

Conteo real desde `app/build/test-results/testDebugUnitTest/TEST-*.xml`:
**645 tests, 0 failures, 0 errors** (idéntico al baseline D21; ninguna prueba fue eliminada ni modificada).

### CI remoto del PR #37 (verificado tras push)
- `gh pr checks 37 --watch` → `Android quality gates` **pass** en ambos runs (commit final `2d7dd33`):
  - `https://github.com/thehouse-team/TheHouse-Mobile/actions/runs/46035190075/job/140628693461` (3m28s)
  - `https://github.com/thehouse-team/TheHouse-Mobile/actions/runs/46035185176/job/140628676266` (4m3s)
- Runs previos del commit `2d915e9` también pass: `46034528657` y `46034538488`.
- `gh pr view 37` tras el push: `OPEN`, `MERGEABLE`, head `feature/platform-security-automation`. Sin merge.

### Pendiente (registro anterior, supersedido por la corrección abajo)
- Validación visual y aprobación humana: PENDIENTE.
- Merge: NO AUTORIZADO / NO EJECUTADO.

---

## CORRECCIÓN POR EXCESO DE ALCANCE (2026-11-27)

El usuario rechazó el primer intento (`24a0686`). Se restauró la base visual aprobada
`735a697` y se reaplicaron SOLO las cuatro solicitudes autorizadas.

### Restauración
- `git checkout 735a697 -- HomeScreen.kt FavoritesScreen.kt ConversationScreen.kt`
  (sin `git reset --hard`, sin force push, sin reescribir historial; la implementación
  funcional D21 de `735a697` permanece intacta).
- Se eliminaron: tarjeta CTA nueva con icono/subtítulo/borde/flecha, cambio de `Spacing.xl`→`lg`,
  acceso de Privacidad rediseñado, `InfoChip`/`StatusChip`/`HorizontalDivider` en Favoritos,
  estado vacío/divisor/`contentPadding`/cambio de espaciado general en Conversación.

### Verificación bloque por bloque contra `735a697`
| Archivo | Bloques modificados | Solicitud que lo justifica |
|---|---|---|
| `HomeScreen.kt` | 1) `TheHouseSecondaryButton("Publicar mi renta")` movido de la cola a antes del listado; 2) tarjeta `TheHouseCard(onClick = onOpenPrivacy)` eliminada | (1) Reubicar Publicar; (2) Retirar Privacidad redundante |
| `FavoritesScreen.kt` | Solo `FavoriteCard`: `titleLarge`, alineación `Top`, `spacedBy(xs)` interno, `spacedBy(md)` externo; precio/ubicación/CTA sin cambios funcionales | (3) Mejorar tarjetas de Favoritos |
| `ConversationScreen.kt` | 4 bloques: etiqueta `labelMedium`, `widthIn(min=96,max=320)` + padding vertical, hora con alpha 0.85, contador `N / 1000`, colores del botón enviar | (4) Mejorar estética de Conversación |

- Todo lo demás de esos archivos es byte-idéntico a `735a697`.
- `PrivacyTip`/`onOpenPrivacy` se conservan en la firma (test estructural `HomeAccountBlockRemovalTest`
  exige `onOpenPrivacy`). Privacidad verificada en `DrawerItemCatalog` (guest + auth),
  `DrawerNavigationCatalog`, `MainActivity` y `AppRoutes.privacy`; drawer NO modificado.
- Sin componentes compartidos, tema global, ViewModels, DTOs, repositorios, casos de uso,
  navegación, FCM, permisos, Backend, GenAI, ML, ONNX ni "Nuevo edificio".
- `git diff --name-only 735a697 -- app/src/main` filtrado por
  `ViewModel|Dtos|Repository|UseCase|MainActivity|AndroidManifest|ui/theme|Domain|data/|notification|device/|components/`
  → **0 archivos**.

### Gates tras la corrección
| Comando | Resultado |
|---|---|
| `.\gradlew.bat compileDebugKotlin --no-daemon` | BUILD SUCCESSFUL |
| `.\gradlew.bat testDebugUnitTest --no-daemon` | BUILD SUCCESSFUL |
| `.\gradlew.bat lintDebug --no-daemon` | BUILD SUCCESSFUL |
| `.\gradlew.bat assembleDebug --no-daemon` | BUILD SUCCESSFUL |
| `git diff --check` | exit 0 |

Conteo real: **645 tests, 0 failures, 0 errors** (baseline preservado).

### Estado
- Commit correctivo y CI remoto: ver registro final del reporte.
- Validación visual y aprobación humana: **PENDIENTE**.
- Merge: **NO AUTORIZADO / NO EJECUTADO**.

---

## HOTFIX D21 � Regresiones visuales, Publicar mi renta y carga real de fotograf�as (2026-11-27)

Autorizaci�n humana individual sobre el MISMO PR #37 (verificado OPEN / MERGEABLE / CI SUCCESS
antes de tocar c�digo), rama `feature/platform-security-automation`, HEAD inicial `a71ada3`.
Sin merge, sin rama/PR nuevo, sin Backend/GenAI/ML/ONNX/infra, sin Nuevo edificio.

### VISUAL_BASELINE_COMPARISON
- `git diff 54ece77..735a697` sobre `presentation/home|favorites|chat` = **0 l�neas**: la est�tica de `735a697` es BYTE-ID�NTICA a D20.
- `git diff 735a697..a71ada3` = cambios visuales NO aprobados �nicamente en `HomeScreen.kt`, `FavoritesScreen.kt`, `ConversationScreen.kt`.
- Cambios indirectos revisados: `NotificationsScreen.kt` (iconos NV-20) y `SmartMatchRecommendationCard.kt` (explicaci�n GenAI) son funcionales D21, NO est�ticos; se preservan.

### UNAUTHORIZED_VISUAL_CHANGES_FOUND
- Home: tarjeta CTA nueva y jerarqu�a alterada (24a0686), reubicaci�n del bot�n con nuevo estilo (a71ada3).
- Favoritos: `titleLarge`, `Alignment.Top`, `spacedBy(md/xs)` en `FavoriteCard` (24a0686 + a71ada3).
- Conversaci�n: `labelMedium`, `widthIn(min=96)`, padding vertical extra, alpha 0.85 en hora, `N / 1000`, bot�n enviar relleno (24a0686 + a71ada3).
- NO se declara aprobada la adenda visual.

### VISUAL_RESTORATION / FAVORITES / CONVERSATION
- `git checkout 735a697 -- <3 archivos>` (sin reset --hard, sin force push, sin reescribir historial).
- Favoritos y Conversaci�n: byte-id�nticos a `735a697` salvo etiquetas en espa�ol solicitadas en Favoritos.
- Home: solo 3 bloques frente a `735a697`: (1) tarjeta de Privacidad eliminada � PASS humano conservado; (2) bot�n original `TheHouseSecondaryButton("Publicar mi renta")` antes de recomendados � requisito expl�cito; (3) `HomeEffect.OpenLandlord -> onOpenLandlord()` � fix P0 funcional. Sin cambios de tipograf�a, espaciado, color ni componentes compartidos.

### P0 PUBLISH_RENT
- Causa demostrada: `HomeEvent.PublishClicked -> guard(ProtectedAction.Publish)`; `guard()` solo emite `RequireLogin` para guest y **no emite ning�n efecto para autenticado**. `onOpenLandlord` estaba cableado en `MainActivity` a `AppRoutes.landlordDashboard.route` pero **nunca era invocado por HomeScreen** (callback desconectado). La ruta s� est� registrada. No exist�a condici�n de rol que bloqueara al autenticado.
- Fix: `publishRental()` privado en `HomeViewModel`: guest ? `HomeEffect.RequireLogin`; autenticado ? `HomeEffect.OpenLandlord`; HomeScreen observa el efecto y llama `onOpenLandlord`. El destino existente decide entre alta de perfil y dashboard seg�n estado real; NO se activa perfil ni se crea edificio. Sin rutas nuevas.

### P0 IM�GENES � diagn�stico con evidencia real
- `GET /api/v1/rooms/search?universityId=6aeecc11...&pageSize=25` productivo: HTTP **200**, `totalItems=15`; 13 habitaciones `[PRUEBA Mobile]` con `thumbnailUrl` presente; 2 DEMO con `thumbnailUrl=null` (placeholder correcto, conservado).
- Formato real del campo: **URL relativa** `/api/v1/media/{id}?size=thumb`, NO absoluta. Resuelta contra el origen: HTTP **200**, `Content-Type: image/webp`, 1666 bytes. No se imprimen URLs firmadas completas.
- Causa ra�z demostrada: Coil recib�a la URL relativa como `String` modelo (sin esquema ni host) ? no pod�a resolverla; adem�s `AppContainer.imageLoader` compart�a el `OkHttpClient` de Retrofit con `BearerTokenInterceptor` + authenticator ? riesgo de enviar Bearer a dominios externos y refreshes por 401 de imagen.
- Fix: `ImageUrlResolver` (puro), `ImageHttpClientFactory` (sin interceptor de autorizaci�n ni authenticator, timeouts acotados), `ImageLoaderFactory` con `OkHttpNetworkFetcherFactory` sobre ese cliente, `AppContainer.resolveImageUrl`. `TheHouseImage` reutiliza el placeholder TheHouse en loading/null/error con estados observables y mide solo el estado terminal. DTOs y mappers ya conservaban `thumbnailUrl` y `photos` (verificado, sin cambios).
- RoomDetail: galer�a compacta con foto principal + hasta 3 miniaturas; sin fotos ? placeholder de 96 dp (antes espacio blanco excesivo). Contactar/Agendar visita intactos.
- Coil 3.4.0 + `coil-network-okhttp` 3.4.0, WebP soportado. Network Security Config: cleartext deshabilitado, TLS intacto. Sin cach� obsoleta introducida.

### ETIQUETAS EN ESPA�OL
- `RoomSearchCatalog`: `single?Individual`, `double?Doble`, `shared?Compartido/a`, `house_rooms?Casa con habitaciones`, `rooms_building?Cuartos en edificio`, `full_apartment?Departamento completo`, `women?Solo mujeres`, `men?Solo hombres`, `any?Cualquiera`, `private?Privado`.
- Aplicado en RoomCard, RoomDetailScreen, FavoritesScreen y SmartMatchRecommendationCard mediante el cat�logo existente. Serializaci�n y valores enviados al Backend intactos.

### INFORMACI�N NUEVA DEL BACKEND (reportada por el responsable, NO verificada en producci�n por Mobile)
- Firebase Admin: `FIREBASE_SERVICE_ACCOUNT` configurado en producci�n; push real disponible para pruebas conjuntas con eventos de mensaje, visita y solicitud.
- GenAI: operativo, escala a cero; primera consulta en fr�o puede devolver template ? calentar y repetir ~30 s despu�s.
- NV-20 v1.3.0: PR Backend #106 implementado, 1,401 pruebas aprobadas seg�n reporte; **pendiente de merge y despliegue ? NO declarar disponible en producci�n**.
- Moderaci�n: Backend procesa fotograf�as al subirlas, m�ximo 4 por habitaci�n; `landlord1` autorizado para pruebas coordinadas. GAP Mobile: `LandlordApi` ya declara POST/DELETE de fotos, pero no existe flujo de carga/eliminaci�n en la UI; NO implementado en este hotfix.
- Geolocalizaci�n: Backend valida universidades cercanas por coordenadas; endpoint/payload/geocodificaci�n sin confirmar. **Nuevo edificio NO modificado**.

### GATES DEL HOTFIX (salida real)
| Comando | Resultado |
|---|---|
| `.\gradlew.bat clean compileDebugKotlin --no-daemon` | BUILD SUCCESSFUL |
| `.\gradlew.bat testDebugUnitTest --no-daemon` | BUILD SUCCESSFUL � **657 tests, 0 failures, 0 errors** (645 baseline + 12 nuevos) |
| `.\gradlew.bat lintDebug --no-daemon` | BUILD SUCCESSFUL, 0 errores SARIF |
| `.\gradlew.bat assembleDebug --no-daemon` | BUILD SUCCESSFUL |
| `.\gradlew.bat compileDebugAndroidTestKotlin --no-daemon` | BUILD SUCCESSFUL |
| `.\gradlew.bat assembleDebugAndroidTest --no-daemon` | BUILD SUCCESSFUL |
| `git diff --check` | exit 0 |

- Pruebas nuevas: `D21PublishNavigationTest` (4) y `D21ImagePipelineTest` (8), incluyendo Retrofit/MockWebServer real para verificar que el cliente de im�genes **no env�a Authorization** a un dominio externo.
- HTTP productivo verificado en solo lectura con `Invoke-WebRequest`; sin credenciales, sin tokens, sin exponer contrase�as.
- RUNTIME PENDING: renderizado real en dispositivo y validaci�n visual humana siguen pendientes. Una prueba de deserializaci�n NO equivale a una fotograf�a renderizada.

---

## D21 � Hotfix exclusivo de etiquetas en espa�ol (2026-11-27)

Autorizaci�n humana individual sobre el MISMO PR #37, rama `feature/platform-security-automation`,
base `d11c3f18fe28ade4510a95bdb3bcc2cbbab8c6f2`. Congelaci�n visual total; sin merge.

### Validaci�n humana
- **10/11 validaciones runtime APROBADAS.** El usuario confirma que dise�o anterior, navegaci�n,
  fotograf�as, galer�as, placeholders, Favoritos y Conversaciones funcionan correctamente.
- �nico FAIL: las caracter�sticas de los alojamientos todav�a mostraban c�digos en ingl�s.
- La adenda visual NO se declara aprobada m�s all� de lo ya confirmado por el usuario.

### TRANSLATION_ROOT_CAUSE
- El hotfix anterior (`d11c3f1`) tradujo solo `roomType`, `buildingType`, `bathroom` y
  `allowedTenants` mediante `RoomSearchCatalog`. Quedaron rutas de presentaci�n que renderizaban
  c�digos del contrato directamente:
  - `RoomCard` (Home/Explore): `room.services` sin mapear.
  - `PropertyCard` (Home/Explore legacy): `property.services` sin mapear.
  - `ExploreScreen.ServicesChips`: `code.replaceFirstChar` en lugar del cat�logo.
  - `SmartMatchRecommendationCard`: `readableCode()` solo capitalizaba (`hot_water` ? �Hot Water�).
  - `RoomDetailScreen`: `building.services`, `furniture` y `minContract` sin mapear.
- Cat�logo Backend verificado en solo lectura (`ListingRules.cs`): 11 servicios, 8 muebles,
  3 duraciones de contrato. Sin cambios en DTOs, peticiones, filtros ni l�gica.

### Correcci�n (solo textos visibles)
- `RoomSearchCatalog`: mapas `SERVICE`, `FURNITURE`, `MIN_CONTRACT`; `shared` ? �Compartido o Compartida�.
- `label()` con fallback seguro: c�digo desconocido se devuelve tal cual, sin traducci�n inventada.
- Aplicado en Home, Explorar, Smart Match, Detalle del cuarto y Favoritos (Favoritos ya usaba el cat�logo).
- T�tulos, descripciones y contenido escrito por usuarios NO traducidos.
- Sin cambios en DTOs/c�digos serializados, HTTP, filtros, VIEWModels, navegaci�n ni contratos.

### Verificaci�n de congelaci�n
- `git diff d11c3f1` revisado bloque por bloque: **solo expresiones de texto**. Cero cambios en
  modificadores, colores, tipograf�as, tama�os, m�rgenes, espaciados, tarjetas, iconos, fotos,
  galer�as, placeholders, header, drawer, navegaci�n, conversaciones ni posici�n de botones.

### Gates
| Comando | Resultado |
|---|---|
| `.\gradlew.bat compileDebugKotlin --no-daemon` | BUILD SUCCESSFUL |
| `.\gradlew.bat testDebugUnitTest --no-daemon` | BUILD SUCCESSFUL � **669 tests, 0 failures, 0 errors** (657 + 12 nuevos) |
| `.\gradlew.bat lintDebug --no-daemon` | BUILD SUCCESSFUL |
| `.\gradlew.bat assembleDebug --no-daemon` | BUILD SUCCESSFUL |
| `git diff --check` | exit 0 |

- Nuevos tests: `D21SpanishLabelsTest` (6) y `D21LabelsWiringTest` (6): 21 combinaciones de
  servicios, 8 de mobiliario, 3 de contrato, c�digos base y fallback desconocido.

---

## ADENDA FINAL — cierre técnico sin dispositivo y geolocalización

- `git rev-parse HEAD` antes de los cambios = `3bbd1dbe223ee2da7e11e05767dc842a7094552d`.
- `gh pr view 37` = OPEN / MERGEABLE / `feature/platform-security-automation` → `main`.
- Sin Backend/GenAI/ML/ONNX/infra, sin servicios nuevos (COST-001 $0), sin secretos.

### Causas demostradas
- Coordenadas: `BuildingFormUiState` usaba `toDoubleOrNull()` directo y rangos genéricos (-90..90/-180..180) en lugar de los rangos contractuales (14..33/-118.5..-86); una coma decimal localizada o un signo Unicode producía "Latitud inválida". El envío usaba `toDouble()` sin normalizar. Corregido con `CoordinateParser` puro.
- Notificaciones: `askedOnce` era `rememberSaveable`, por lo que una denegación permanente se reclasificaba como primera negación tras morir el proceso; Android no vuelve a mostrar el diálogo y el usuario debía ir a Ajustes. Corregido con `NotificationPermissionStore` (DataStore).
- Fotos: `LandlordApi` declaraba `Response<BuildingDto>`/`Response<RoomDto>` para cargas que el Backend responde con `PhotoResponse`; el ID real se descartaba. Corregido.
- Ya existía UI de fotos en `RoomFormScreen` (galería, límite 4, progress, borrado); NO se duplicó.

### Gates ejecutados (salida real)
| Comando | Resultado |
|---|---|
| `.\gradlew.bat clean compileDebugKotlin` | BUILD SUCCESSFUL |
| `.\gradlew.bat testDebugUnitTest` | BUILD SUCCESSFUL — 702 tests / 0 failures / 0 errors |
| `.\gradlew.bat lintDebug` | BUILD SUCCESSFUL |
| `.\gradlew.bat assembleDebug` | BUILD SUCCESSFUL |
| `.\gradlew.bat compileDebugAndroidTestKotlin` | BUILD SUCCESSFUL |
| `.\gradlew.bat assembleDebugAndroidTest` | BUILD SUCCESSFUL |
| `.\gradlew.bat jacocoDebugTestReport` | BUILD SUCCESSFUL |
| `git diff --check` | exit 0 |

### Cobertura real (JaCoCo)
- Líneas: 7262/21902 = 33.16 %. Ramas: 2810/8923 = 31.49 %.
- `domain/location` 43/45, `data/catalog/repository` 103/147, `data/landlord/repository` 224/280.
- Sin exclusiones artificiales. No se contabilizan pruebas no ejecutadas.

### Evidencia humana incorporada
- `ACCOUNT_DELETION_RUNTIME=PASS`: 200/403→204, requestId `9d1dc271075f46e4b7539a196b619ae3`, login 401 posterior.
- GenAI: 200 en 8237 ms (`9a170e8db113424692b5b64342e4dcb4`) y 200 en 2220 ms (`c45db7b43beb401ab5ab707f95dacf93`); `generatedBy=genai` pendiente de confirmación Backend.

### Pendiente
- Sin dispositivo: GPS físico, UI de la nueva sección y `connectedDebugAndroidTest` → PENDING.
- Push real → EXTERNAL_BLOCKER. Merge → NO AUTORIZADO.

---

## ADENDA HOTFIX RED — 2026-11-28

- Crash buffer recuperado: SocketTimeoutException + StandaloneCoroutine{Cancelling}/Dispatchers.Main.immediate, PID 3067 (17:28). Incidentes posteriores sin stack completo; causalidad idéntica NO asumida.
- Causa: sin CoroutineExceptionHandler + llamada de red de login/register fuera del try/catch. SafeHttpLoggingInterceptor/BearerTokenInterceptor son eslabanes del stack, no la causa.
- Fix: SafeCoroutineLauncher en BaseViewModel y applicationScope; safeAuthCall tipa timeout/IOException sin borrar sesión válida; contención en Home/Explore/DeviceSyncCoordinator.
- DNS emulador: 10.0.2.3 en NetworkAgent pero resolución fallida también para dominios Google → entorno, no Mobile. Sin IP/DNS/TLS/timeouts modificados. WiFi restaurado.
- Gates: 716/0/0, lint, assemble, compile/assemble androidTest, jacoco, diff-check — SUCCESS.
- Emulador: app viva con DNS roto, reintento manual, navegación offline y arranque sin WiFi; sin crash. connectedDebugAndroidTest sigue PENDING.

---

## ADENDA D21-CIERRE-ARRENDADOR — 2026-11-27

- Base: `6ab949e75018aa269b426ce9dc119e126af71e5c`; PR #37 verificado OPEN/MERGEABLE; SIN MERGE.
- Auditoría contractual dirigida (solo lectura Backend): `docs/sdd/reports/D21_LANDLORD_END_TO_END_AUDIT.md`.
- Contratos confirmados: 7 códigos de preguntas rápidas, máx 5, mensaje ≤500, `conversationId` no nullable, POST 201, 422 `content_rejected`, 503 `moderation_unavailable`, aprobación staff solo en `/admin` (fuera de alcance Mobile), visita `rejected` inexistente en v1.3.
- Causas raíz corregidas (evidencia en código):
  1. `RequestDetailEffect.OpenConversation` era `Unit` → ahora emite el `conversationId` real del Backend y MainActivity navega a `ConversationRoute`.
  2. `RequestDetailScreen` mostraba `question.api` (`available_now`) → catálogo único `QuickQuestionLabels` compartido con el composer.
  3. `RequestErrorCode.fromApi` no reconocía `content_rejected`/`moderation_unavailable` → mensaje genérico; ahora Spanish y con `DomainError.ValidationError`/`ServiceUnavailable`, formulario conservado.
  4. `LandlordProfileEffect.Activated` hacía `launchSingleTop` al mismo destino (no-op) → recarga explícita del dashboard.
  5. Dashboard sin refresco al volver de mutaciones → `LifecycleEventEffect(ON_RESUME)` con salto del primer evento.
  6. `availableFrom` se exigía en UI aunque el contrato lo declara `string?` → ahora opcional con fecha inválida rechazada.
- Gates reales: `clean compileDebugKotlin` SUCCESS; `testDebugUnitTest` **726/0/0** (716 baseline + 10 nuevos `D21LandlordFlowClosureTest`); `lintDebug` 0 errores; `assembleDebug` SUCCESS; `compileDebugAndroidTestKotlin` + `assembleDebugAndroidTest` SUCCESS; `jacocoDebugTestReport` SUCCESS; `git diff --check` exit 0.
- JaCoCo real: 33.41 % líneas (7339/21969), 31.75 % ramas (2838/8939), sin exclusiones artificiales.
- Sin secretos, sin servicios nuevos (COST-001 $0), sin datos productivos mutados, sin Backend/GenAI/ML/ONNX/infra, sin cambios visuales fuera del flujo arrendador.
- RUNTIME_PENDING (sin dispositivo/emulador con conectividad). NO MERGEAR.

### Runtime controlado (emulador, 2026-11-28)

- `adb devices`: `emulator-5554 device` (SDK en `C:\Users\skinn\AppData\Local\Android\Sdk`).
- Backend real `ca-thehouse-u2.grayisland-cb722b5f.eastus2.azurecontainerapps.io` (9.169.177.51) responde ping; DNS del host productivo resuelve correctamente en el emulador (a diferencia del host genérico `api.thehouse.mx` usado en el hotfix anterior).
- `adb install -r app-debug.apk` SUCCESS; arranque tras `am force-stop` + monkey launcher: sin entradas en `logcat -b crash` y sin `FATAL EXCEPTION`.
- `uiautomator dump`: Home invitado renderiza «Hola, invitado», «Publicar mi renta» antes de «Recomendados para ti», catálogo real y bottom bar — baseline visual preservado.
- Tap controlado sobre «Publicar mi renta» (bounds `[53,1477][1027,1603]`) → pantalla «Inicia sesión en TheHouse». Guard de invitado confirmado, sin mutaciones productivas.
- NO se ejecutó flujo autenticado de arrendador ni se crearon edificios/cuartos/solicitudes: sin credenciales y prohibido crear datos productivos sin autorización. `RUNTIME_AUTHENTICATED=PENDING`.

### Cierre de commit / PR (2026-11-28)

- Commit: `ffeb61e57f833579641e2471fbca51abcc03beff` — 19 archivos, +577/-26.
- Push: `6ab949e..ffeb61e feature/platform-security-automation -> feature/platform-security-automation` SUCCESS.
- PR #37: OPEN / MERGEABLE (verificado con `gh pr view 37` tras el push). NO MERGEADO.
- CI remoto: dos runs para `ffeb61e` (`46110222161`, `46110225627`) en estado `in_progress` al momento del reporte; run anterior `46089420527` sobre `6ab949e` = SUCCESS. `REMOTE_CI=PENDING` (no se afirma verde).

---

## ADENDA HOTFIX — Formulario Nuevo edificio y validaciones (2026-11-28)

- Base `6a092b3`, MISMO PR #37 (OPEN/MERGEABLE), sin merge, sin Backend/GenAI/ML/ONNX/infra.
- Causa raíz demostrada por código: `BuildingFormScreen.kt:342` deshabilitaba el botón con `enabled = !isLoading && isValid`, mientras `submit()` era el único lugar que activaba `submitAttempted`. Resultado: formulario inválido ⇒ botón no pulsable ⇒ nunca se mostraban todas las validaciones. No se afirma qué campo concreto bloqueó al usuario (sin evidencia de su estado real).
- Contrato Backend verificado (`BuildingService.CreateAsync`, `Address`, `Location`, `ListingRules.Text`): name 2..120, street 1..120, number 1..20, neighborhood 1..120, city 1..80, state 1..60, postalCode 5 dígitos, lat 14..33, lng -118.5..-86, nearestUniversityId presente, rules <=2000.
- Fix: fuente única `BuildingFormValidation.errors()`; `isValid` derivado de esa fuente; `submit()` siempre activa `submitAttempted`, calcula `pendingFields`/`firstInvalidField`, registra DEBUG seguro y NO llama HTTP si hay errores; botón habilitado cuando `!isLoading && !isSaving`; resumen compacto y scroll al primer campo; number/neighborhood ahora visibles con error.
- Universidad: `load()` no sobrescribe selección válida/manual; `performNearbySearch()` ya no borra `universityId` con resultados vacíos; `isNearbyResult` solo con items no vacíos.
- 409 `POST /rooms/{id}/requests`: códigos reales `active_request_exists`, `request_in_progress`, `active_requests_limit` (el Backend NO devuelve conversationId en el 409), más `active_requests_limit`. Se agregó `REQUEST_IN_PROGRESS` con mensaje Spanish; `active_request_exists` recupera el conversationId REAL vía `GET /users/me/requests` y ofrece «Abrir conversación existente». No se inventa ningún ID.
- Gates: `compileDebugKotlin`, `testDebugUnitTest` **738/0/0** (726 baseline + 12), `lintDebug` 0 errores, `assembleDebug`, `compileDebugAndroidTestKotlin`, `assembleDebugAndroidTest`, `jacocoDebugTestReport`, `git diff --check` exit 0.
- JaCoCo: 33.79 % líneas (7476/22123), 31.77 % ramas (2865/9017), sin exclusiones.
- Runtime emulador `emulator-5554` (sesión autenticada preexistente, SIN mutaciones productivas): APK instalado, arranque sin crash; Home → «Publicar mi renta» → «Mis alojamientos» → «Crear edificio» → formulario. Pulsar Submit con campos vacíos mostró TODOS los errores (incluidos Número y Colonia), scroll al inicio del formulario y **ningún POST** a `/landlord/buildings`. Log DEBUG real: `BuildingFormFlow: validation_failed fields=name,street,number,neighborhood,city,state,postalCode,lat,lng` y `university_preselected source=catalog`. Sin datos personales en logs.
- Nota de prueba: `BuildingFormViewModelTest.submit sends numeric lat and lng...` se reforzó (no se debilitó) añadiendo Número y Colonia para representar un formulario válido según el contrato Backend; la aserción de conversión numérica original se mantiene intacta.

---

## ADENDA — Mejora del flujo de publicación del arrendador (2026-11-28)

Base `d821049`, MISMO PR #37 (verificado OPEN / MERGEABLE / CI SUCCESS antes de tocar código).
Sin merge, sin rama/PR nuevo, sin Backend/GenAI/ML/ONNX/infra, sin mutaciones productivas.

- `BUILDING_CREATION_RUNTIME=PASS` registrado por evidencia humana: `POST /api/v1/landlord/buildings` → 201, requestId `248c7019430449468e4570afd09644f6`, edificio UD3 `6b0a6e113f6d5ad952b41873`; detalle/rooms/verification/documents/publication → 200. No se repitió ni se modificó UD3.
- Contratos Backend auditados solo lectura e informe: `docs/sdd/reports/D21_PUBLICATION_FLOW_IMPROVEMENT_AUDIT.md`.
- Guía de publicación: `PublicationGuide.kt` deriva pasos/progreso del endpoint real; `PublicationGuideCard` y `PublicationCard` muestran completos, pendientes, estado, motivo de bloqueo y acción. Sin requisitos ni porcentajes inventados; sin simulación de aprobación administrativa.
- Reordenación exclusiva de `BuildingDetailScreen`: guía → resumen (con advertencia de distancia) → foto → habitaciones → documentos → verificación → enviar a revisión → acciones existentes. Todas las funciones previas preservadas (edición, habitaciones, documentos, verificación, eliminación, solicitudes).
- Coordenadas: `LandlordFlowPolicies.COORDINATE_HINT` explica 14..33 y -118.5..-86 en el formulario. Distancia UTTT 1957.9 km: calculada por Backend (Haversine) a partir de coordenadas y universidad seleccionada; se muestra advertencia sin bloqueo geográfico nuevo. GPS y `universities/search` intactos.
- Documentos: `DocumentUploadPolicy` valida 10 MB, firma de bytes PDF/JPEG/PNG y mensajes específicos antes del envío (413/415 evitables); se muestran formatos y tamaño máximo antes de seleccionar. Sin conversión automática de PDF, sin contenido de identificación en logs, caché privado, fixtures sintéticos.
- `GET /landlord/stats/rooms` 400: el Backend valida `to` contra la fecha de Ciudad de México; el dashboard usaba `LocalDate.now()` del dispositivo (`to` podía ser futuro). Fix con `America/Mexico_City` y rango de 30 días; sin estadísticas ficticias.
- Refresco sin reiniciar: `BuildingDetailViewModel` recalcula la guía tras publicar y recarga `publication` tras pausar/reanudar cuartos y subir foto/documentos.
- Gates: `clean`, `compileDebugKotlin`, `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `compileDebugAndroidTestKotlin`, `assembleDebugAndroidTest`, `jacocoDebugTestReport`, `git diff --check` — BUILD SUCCESSFUL / exit 0.
- Conteo real: **761 tests, 0 failures, 0 errors** (738 baseline + 23 nuevos: 18 D21PublicationFlowTest + 5 LandlordRoomStatsHttpTest).
- JaCoCo: 34.89 % líneas (7771/22272), 32.46 % ramas (2954/9101), sin exclusiones.
