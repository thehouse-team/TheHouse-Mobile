# D21 â€” Evidencia

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
Baseline 602 â†’ +29 nuevos (27 D21 device/moderation/coordinator + 2 GenAI).

## Hallazgos corregidos con evidencia

1. `androidTest` no compilaba: 378 errores en 4 archivos. Reescritos con ActivityScenario/Compose reales â†’ `compileDebugAndroidTestKotlin` SUCCESS.
2. `dexBuilderDebugAndroidTest` fallaba por nombre de mÃ©todo con espacios en `LoginScreenInstrumentedTest.kt` â†’ renombrado â†’ `assembleDebugAndroidTest` SUCCESS.
3. `DeviceSyncCoordinator` reintentaba inmediatamente si la red ya estaba online â†’ se exige transiciÃ³n offlineâ†’online.
4. FCM token se obtenÃ­a pero nunca se enviaba al Backend (`FCM-TOKEN-BLOCKED`) â†’ DeviceApi + coordinador implementados.

## Estado runtime

- `adb` no disponible en el entorno â†’ sin dispositivo fÃ­sico/emulador.
- `connectedDebugAndroidTest`: **RUNTIME_PENDING**.
- FCM real delivery: **EXTERNAL_BLOCKER** (Backend no expone endpoint de envÃ­o; requiere `FIREBASE_SERVICE_ACCOUNT`).
- EliminaciÃ³n real: **RUNTIME_PENDING** (sin cuenta desechable autorizada).

## ADENDA NV-20 v1.3.0 â€” evidencia

- PR Backend #103: `MERGED`, head `d23bb3cd63ede2b515c01efd1e1317068ab8f51a`, tÃ­tulo Â«docs(NV-20): 1.3.0 propuesta â€” 6 avisos nuevosÂ». Documento leÃ­do en modo solo lectura (Â§6.1, Â§9.1).
- Contrato propuesto â‰  mergeado â‰  productivo: el documento marca 1.3.0 como PROPUESTA pendiente de T09â€“T12; Backend main expone `Type`/`Kind` como `string`; despliegue de los 6 tipos NO confirmado (`health/live` OK, sin credenciales para bandeja).
- Seis tipos literales extraÃ­dos del contrato (no de etiquetas): `agreement_proposed`, `agreement_cancelled`, `review_received`, `review_hidden`, `favorite_unavailable`, `report_resolved`.
- `targetKind` nuevos: `room` (targetId id cuarto â†’ `GET /rooms/{id}`, `RoomDetailRoute`), `none` (bandeja, sin exigir targetId).
- Sin enums estrictos en DTO (`String`), por lo que no hay `SerializationException` por valores futuros; los tipos se mapean a destino tipado con fallback.
- Tests nuevos: 14 (10 compatibilidad NV-20 + 4 room target HTTP). Total 645 / 0 failures / 0 errors (631 + 14).
- Gates tras adenda: `compileDebugKotlin`, `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest` â€” todos SUCCESS.

---

## ADENDA VISUAL â€” evidencia (2026-11-27)

Gobernanza corregida por instrucciÃ³n humana: trabajar sobre la rama existente
`feature/platform-security-automation` y el PR #37 abierto; sin rama nueva, sin PR #38,
sin merge; registrar como adenda visual de D21.

### VerificaciÃ³n previa del PR
- `gh pr view 37 --json number,title,state,baseRefName,headRefName,mergeable,url` =
  `37 | D21 â€” Platform, Security and Automation | OPEN | main | feature/platform-security-automation | MERGEABLE | https://github.com/thehouse-team/TheHouse-Mobile/pull/37`.
- Rama local sincronizada: `git pull --ff-only origin feature/platform-security-automation` â†’ Already up to date.
- HEAD antes de la adenda: `735a697` (D21 NV-20 v1.3.0).
- Se eliminÃ³ el intento preliminar de rama/spec D22: `git branch -D feature/d22-visual-polish-ui` y
  `Remove-Item specs/d22-visual-polish-ui`. No quedan artefactos D22.

### Cambios visuales (solo presentaciÃ³n)
- Home (`HomeScreen.kt`): nuevo composable privado `PublishRentCta` (Surface de acento con
  `primaryContainer`, borde `primary` suave, icono `AddHome` y flecha) ubicado despuÃ©s de
  `SmartMatchPromo` y antes del header "Recomendados para ti"; el CTA secundario original al final
  se eliminÃ³. "Privacidad" pasÃ³ de tarjeta `TheHouseCard` a acceso secundario compacto con
  `TheHouseTextAction` + icono, conservando `onOpenPrivacy`. JerarquÃ­a de espaciado global
  `Arrangement.spacedBy(Spacing.lg)`; CTA sin FAB, sin nueva dependencia.
- Favoritos (`FavoritesScreen.kt`): `FavoriteCard` con tÃ­tulo `titleLarge` y precio alineado a la
  derecha, ubicaciÃ³n con icono, metadata en `InfoChip`, etiqueta "Demo" como `StatusChip`,
  `HorizontalDivider` antes del CTA "Quitar de favoritos" (mismo `onRemove` / `removing`).
- ConversaciÃ³n (`ConversationScreen.kt`): `LazyColumn` con `spacedBy(Spacing.sm)` y
  `contentPadding`; estado vacÃ­o "AÃºn no hay mensajes" sin nuevo estado de ViewModel; burbujas con
  `padding(vertical = Spacing.xs)`, `widthIn(min = 96.dp, max = 320.dp)`, elevaciÃ³n `Elevations.card`
  del tema y etiqueta `labelMedium`; `HorizontalDivider` sobre el composer; contador
  "N / 1000 caracteres" (mismo valor `text.length`, sin cambiar el lÃ­mite `take(1000)` del ViewModel);
  `FilledIconButton` con colores primarios explÃ­citos.

### ConfirmaciÃ³n de alcance cero funcional
- `git diff --name-only | Where-Object { $_ -match 'ViewModel|Dtos|Repository|UseCase|MainActivity|AndroidManifest|ui/theme|Domain|data/|notification|device/' }`
  â†’ 0 archivos.
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
**645 tests, 0 failures, 0 errors** (idÃ©ntico al baseline D21; ninguna prueba fue eliminada ni modificada).

### CI remoto del PR #37 (verificado tras push)
- `gh pr checks 37 --watch` â†’ `Android quality gates` **pass** en ambos runs (commit final `2d7dd33`):
  - `https://github.com/thehouse-team/TheHouse-Mobile/actions/runs/46035190075/job/140628693461` (3m28s)
  - `https://github.com/thehouse-team/TheHouse-Mobile/actions/runs/46035185176/job/140628676266` (4m3s)
- Runs previos del commit `2d915e9` tambiÃ©n pass: `46034528657` y `46034538488`.
- `gh pr view 37` tras el push: `OPEN`, `MERGEABLE`, head `feature/platform-security-automation`. Sin merge.

### Pendiente (registro anterior, supersedido por la correcciÃ³n abajo)
- ValidaciÃ³n visual y aprobaciÃ³n humana: PENDIENTE.
- Merge: NO AUTORIZADO / NO EJECUTADO.

---

## CORRECCIÃ“N POR EXCESO DE ALCANCE (2026-11-27)

El usuario rechazÃ³ el primer intento (`24a0686`). Se restaurÃ³ la base visual aprobada
`735a697` y se reaplicaron SOLO las cuatro solicitudes autorizadas.

### RestauraciÃ³n
- `git checkout 735a697 -- HomeScreen.kt FavoritesScreen.kt ConversationScreen.kt`
  (sin `git reset --hard`, sin force push, sin reescribir historial; la implementaciÃ³n
  funcional D21 de `735a697` permanece intacta).
- Se eliminaron: tarjeta CTA nueva con icono/subtÃ­tulo/borde/flecha, cambio de `Spacing.xl`â†’`lg`,
  acceso de Privacidad rediseÃ±ado, `InfoChip`/`StatusChip`/`HorizontalDivider` en Favoritos,
  estado vacÃ­o/divisor/`contentPadding`/cambio de espaciado general en ConversaciÃ³n.

### VerificaciÃ³n bloque por bloque contra `735a697`
| Archivo | Bloques modificados | Solicitud que lo justifica |
|---|---|---|
| `HomeScreen.kt` | 1) `TheHouseSecondaryButton("Publicar mi renta")` movido de la cola a antes del listado; 2) tarjeta `TheHouseCard(onClick = onOpenPrivacy)` eliminada | (1) Reubicar Publicar; (2) Retirar Privacidad redundante |
| `FavoritesScreen.kt` | Solo `FavoriteCard`: `titleLarge`, alineaciÃ³n `Top`, `spacedBy(xs)` interno, `spacedBy(md)` externo; precio/ubicaciÃ³n/CTA sin cambios funcionales | (3) Mejorar tarjetas de Favoritos |
| `ConversationScreen.kt` | 4 bloques: etiqueta `labelMedium`, `widthIn(min=96,max=320)` + padding vertical, hora con alpha 0.85, contador `N / 1000`, colores del botÃ³n enviar | (4) Mejorar estÃ©tica de ConversaciÃ³n |

- Todo lo demÃ¡s de esos archivos es byte-idÃ©ntico a `735a697`.
- `PrivacyTip`/`onOpenPrivacy` se conservan en la firma (test estructural `HomeAccountBlockRemovalTest`
  exige `onOpenPrivacy`). Privacidad verificada en `DrawerItemCatalog` (guest + auth),
  `DrawerNavigationCatalog`, `MainActivity` y `AppRoutes.privacy`; drawer NO modificado.
- Sin componentes compartidos, tema global, ViewModels, DTOs, repositorios, casos de uso,
  navegaciÃ³n, FCM, permisos, Backend, GenAI, ML, ONNX ni "Nuevo edificio".
- `git diff --name-only 735a697 -- app/src/main` filtrado por
  `ViewModel|Dtos|Repository|UseCase|MainActivity|AndroidManifest|ui/theme|Domain|data/|notification|device/|components/`
  â†’ **0 archivos**.

### Gates tras la correcciÃ³n
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
- ValidaciÃ³n visual y aprobaciÃ³n humana: **PENDIENTE**.
- Merge: **NO AUTORIZADO / NO EJECUTADO**.

---

## HOTFIX D21 — Regresiones visuales, Publicar mi renta y carga real de fotografías (2026-11-27)

Autorización humana individual sobre el MISMO PR #37 (verificado OPEN / MERGEABLE / CI SUCCESS
antes de tocar código), rama `feature/platform-security-automation`, HEAD inicial `a71ada3`.
Sin merge, sin rama/PR nuevo, sin Backend/GenAI/ML/ONNX/infra, sin Nuevo edificio.

### VISUAL_BASELINE_COMPARISON
- `git diff 54ece77..735a697` sobre `presentation/home|favorites|chat` = **0 líneas**: la estética de `735a697` es BYTE-IDÉNTICA a D20.
- `git diff 735a697..a71ada3` = cambios visuales NO aprobados únicamente en `HomeScreen.kt`, `FavoritesScreen.kt`, `ConversationScreen.kt`.
- Cambios indirectos revisados: `NotificationsScreen.kt` (iconos NV-20) y `SmartMatchRecommendationCard.kt` (explicación GenAI) son funcionales D21, NO estéticos; se preservan.

### UNAUTHORIZED_VISUAL_CHANGES_FOUND
- Home: tarjeta CTA nueva y jerarquía alterada (24a0686), reubicación del botón con nuevo estilo (a71ada3).
- Favoritos: `titleLarge`, `Alignment.Top`, `spacedBy(md/xs)` en `FavoriteCard` (24a0686 + a71ada3).
- Conversación: `labelMedium`, `widthIn(min=96)`, padding vertical extra, alpha 0.85 en hora, `N / 1000`, botón enviar relleno (24a0686 + a71ada3).
- NO se declara aprobada la adenda visual.

### VISUAL_RESTORATION / FAVORITES / CONVERSATION
- `git checkout 735a697 -- <3 archivos>` (sin reset --hard, sin force push, sin reescribir historial).
- Favoritos y Conversación: byte-idénticos a `735a697` salvo etiquetas en español solicitadas en Favoritos.
- Home: solo 3 bloques frente a `735a697`: (1) tarjeta de Privacidad eliminada — PASS humano conservado; (2) botón original `TheHouseSecondaryButton("Publicar mi renta")` antes de recomendados — requisito explícito; (3) `HomeEffect.OpenLandlord -> onOpenLandlord()` — fix P0 funcional. Sin cambios de tipografía, espaciado, color ni componentes compartidos.

### P0 PUBLISH_RENT
- Causa demostrada: `HomeEvent.PublishClicked -> guard(ProtectedAction.Publish)`; `guard()` solo emite `RequireLogin` para guest y **no emite ningún efecto para autenticado**. `onOpenLandlord` estaba cableado en `MainActivity` a `AppRoutes.landlordDashboard.route` pero **nunca era invocado por HomeScreen** (callback desconectado). La ruta sí está registrada. No existía condición de rol que bloqueara al autenticado.
- Fix: `publishRental()` privado en `HomeViewModel`: guest ? `HomeEffect.RequireLogin`; autenticado ? `HomeEffect.OpenLandlord`; HomeScreen observa el efecto y llama `onOpenLandlord`. El destino existente decide entre alta de perfil y dashboard según estado real; NO se activa perfil ni se crea edificio. Sin rutas nuevas.

### P0 IMÁGENES — diagnóstico con evidencia real
- `GET /api/v1/rooms/search?universityId=6aeecc11...&pageSize=25` productivo: HTTP **200**, `totalItems=15`; 13 habitaciones `[PRUEBA Mobile]` con `thumbnailUrl` presente; 2 DEMO con `thumbnailUrl=null` (placeholder correcto, conservado).
- Formato real del campo: **URL relativa** `/api/v1/media/{id}?size=thumb`, NO absoluta. Resuelta contra el origen: HTTP **200**, `Content-Type: image/webp`, 1666 bytes. No se imprimen URLs firmadas completas.
- Causa raíz demostrada: Coil recibía la URL relativa como `String` modelo (sin esquema ni host) ? no podía resolverla; además `AppContainer.imageLoader` compartía el `OkHttpClient` de Retrofit con `BearerTokenInterceptor` + authenticator ? riesgo de enviar Bearer a dominios externos y refreshes por 401 de imagen.
- Fix: `ImageUrlResolver` (puro), `ImageHttpClientFactory` (sin interceptor de autorización ni authenticator, timeouts acotados), `ImageLoaderFactory` con `OkHttpNetworkFetcherFactory` sobre ese cliente, `AppContainer.resolveImageUrl`. `TheHouseImage` reutiliza el placeholder TheHouse en loading/null/error con estados observables y mide solo el estado terminal. DTOs y mappers ya conservaban `thumbnailUrl` y `photos` (verificado, sin cambios).
- RoomDetail: galería compacta con foto principal + hasta 3 miniaturas; sin fotos ? placeholder de 96 dp (antes espacio blanco excesivo). Contactar/Agendar visita intactos.
- Coil 3.4.0 + `coil-network-okhttp` 3.4.0, WebP soportado. Network Security Config: cleartext deshabilitado, TLS intacto. Sin caché obsoleta introducida.

### ETIQUETAS EN ESPAÑOL
- `RoomSearchCatalog`: `single?Individual`, `double?Doble`, `shared?Compartido/a`, `house_rooms?Casa con habitaciones`, `rooms_building?Cuartos en edificio`, `full_apartment?Departamento completo`, `women?Solo mujeres`, `men?Solo hombres`, `any?Cualquiera`, `private?Privado`.
- Aplicado en RoomCard, RoomDetailScreen, FavoritesScreen y SmartMatchRecommendationCard mediante el catálogo existente. Serialización y valores enviados al Backend intactos.

### INFORMACIÓN NUEVA DEL BACKEND (reportada por el responsable, NO verificada en producción por Mobile)
- Firebase Admin: `FIREBASE_SERVICE_ACCOUNT` configurado en producción; push real disponible para pruebas conjuntas con eventos de mensaje, visita y solicitud.
- GenAI: operativo, escala a cero; primera consulta en frío puede devolver template ? calentar y repetir ~30 s después.
- NV-20 v1.3.0: PR Backend #106 implementado, 1,401 pruebas aprobadas según reporte; **pendiente de merge y despliegue ? NO declarar disponible en producción**.
- Moderación: Backend procesa fotografías al subirlas, máximo 4 por habitación; `landlord1` autorizado para pruebas coordinadas. GAP Mobile: `LandlordApi` ya declara POST/DELETE de fotos, pero no existe flujo de carga/eliminación en la UI; NO implementado en este hotfix.
- Geolocalización: Backend valida universidades cercanas por coordenadas; endpoint/payload/geocodificación sin confirmar. **Nuevo edificio NO modificado**.

### GATES DEL HOTFIX (salida real)
| Comando | Resultado |
|---|---|
| `.\gradlew.bat clean compileDebugKotlin --no-daemon` | BUILD SUCCESSFUL |
| `.\gradlew.bat testDebugUnitTest --no-daemon` | BUILD SUCCESSFUL — **657 tests, 0 failures, 0 errors** (645 baseline + 12 nuevos) |
| `.\gradlew.bat lintDebug --no-daemon` | BUILD SUCCESSFUL, 0 errores SARIF |
| `.\gradlew.bat assembleDebug --no-daemon` | BUILD SUCCESSFUL |
| `.\gradlew.bat compileDebugAndroidTestKotlin --no-daemon` | BUILD SUCCESSFUL |
| `.\gradlew.bat assembleDebugAndroidTest --no-daemon` | BUILD SUCCESSFUL |
| `git diff --check` | exit 0 |

- Pruebas nuevas: `D21PublishNavigationTest` (4) y `D21ImagePipelineTest` (8), incluyendo Retrofit/MockWebServer real para verificar que el cliente de imágenes **no envía Authorization** a un dominio externo.
- HTTP productivo verificado en solo lectura con `Invoke-WebRequest`; sin credenciales, sin tokens, sin exponer contraseñas.
- RUNTIME PENDING: renderizado real en dispositivo y validación visual humana siguen pendientes. Una prueba de deserialización NO equivale a una fotografía renderizada.

---

## D21 — Hotfix exclusivo de etiquetas en español (2026-11-27)

Autorización humana individual sobre el MISMO PR #37, rama `feature/platform-security-automation`,
base `d11c3f18fe28ade4510a95bdb3bcc2cbbab8c6f2`. Congelación visual total; sin merge.

### Validación humana
- **10/11 validaciones runtime APROBADAS.** El usuario confirma que diseño anterior, navegación,
  fotografías, galerías, placeholders, Favoritos y Conversaciones funcionan correctamente.
- Único FAIL: las características de los alojamientos todavía mostraban códigos en inglés.
- La adenda visual NO se declara aprobada más allá de lo ya confirmado por el usuario.

### TRANSLATION_ROOT_CAUSE
- El hotfix anterior (`d11c3f1`) tradujo solo `roomType`, `buildingType`, `bathroom` y
  `allowedTenants` mediante `RoomSearchCatalog`. Quedaron rutas de presentación que renderizaban
  códigos del contrato directamente:
  - `RoomCard` (Home/Explore): `room.services` sin mapear.
  - `PropertyCard` (Home/Explore legacy): `property.services` sin mapear.
  - `ExploreScreen.ServicesChips`: `code.replaceFirstChar` en lugar del catálogo.
  - `SmartMatchRecommendationCard`: `readableCode()` solo capitalizaba (`hot_water` ? «Hot Water»).
  - `RoomDetailScreen`: `building.services`, `furniture` y `minContract` sin mapear.
- Catálogo Backend verificado en solo lectura (`ListingRules.cs`): 11 servicios, 8 muebles,
  3 duraciones de contrato. Sin cambios en DTOs, peticiones, filtros ni lógica.

### Corrección (solo textos visibles)
- `RoomSearchCatalog`: mapas `SERVICE`, `FURNITURE`, `MIN_CONTRACT`; `shared` ? «Compartido o Compartida».
- `label()` con fallback seguro: código desconocido se devuelve tal cual, sin traducción inventada.
- Aplicado en Home, Explorar, Smart Match, Detalle del cuarto y Favoritos (Favoritos ya usaba el catálogo).
- Títulos, descripciones y contenido escrito por usuarios NO traducidos.
- Sin cambios en DTOs/códigos serializados, HTTP, filtros, VIEWModels, navegación ni contratos.

### Verificación de congelación
- `git diff d11c3f1` revisado bloque por bloque: **solo expresiones de texto**. Cero cambios en
  modificadores, colores, tipografías, tamaños, márgenes, espaciados, tarjetas, iconos, fotos,
  galerías, placeholders, header, drawer, navegación, conversaciones ni posición de botones.

### Gates
| Comando | Resultado |
|---|---|
| `.\gradlew.bat compileDebugKotlin --no-daemon` | BUILD SUCCESSFUL |
| `.\gradlew.bat testDebugUnitTest --no-daemon` | BUILD SUCCESSFUL — **669 tests, 0 failures, 0 errors** (657 + 12 nuevos) |
| `.\gradlew.bat lintDebug --no-daemon` | BUILD SUCCESSFUL |
| `.\gradlew.bat assembleDebug --no-daemon` | BUILD SUCCESSFUL |
| `git diff --check` | exit 0 |

- Nuevos tests: `D21SpanishLabelsTest` (6) y `D21LabelsWiringTest` (6): 21 combinaciones de
  servicios, 8 de mobiliario, 3 de contrato, códigos base y fallback desconocido.
