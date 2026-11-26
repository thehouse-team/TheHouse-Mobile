# SPEC D19 — Account + UX Core

| Campo | Valor |
|---|---|
| Spec | D19-Account-UX-Core |
| Versión | 1.0.0 |
| Estado | AUTORIZADA / EN EJECUCIÓN |
| Autorización | Humana explícita (instrucción individual 2026-11-26) |
| Branch | `fix/account-ux-core` |
| Base | `origin/main` @ `1ab64eb7dbf7d3ed4fa4920038b2a7e04dcfb881` |
| Backend source of truth | `origin/main` @ `780b7be88258f44a94bf966057eff552a82f35af` |
| Relación NV | NV-04 (Auth/Onboarding), NV-10 (Account), NV-11 (Profile/Preferences), D15 (Drawer/Home). No crea spec NV nueva. |

## 1. Problema

La auditoría maestra (solo lectura, 2026-11-26) confirmó que Profile y Preferences tienen DTOs correctos contra el Backend, pero la app muestra en runtime:

- "No pudimos cargar tu perfil"
- "No pudimos cargar tus preferencias"

Además:

- El hamburger aparece arriba a la izquierda, cerca de la status bar, en lugar de `[logo] TheHouse … ☰`.
- El drawer NO ofrece "Aviso de privacidad" al usuario autenticado.
- Registro muestra checkboxes de términos/privacidad sin enlace navegable al aviso.
- No se consume `GET /api/v1/legal` ni `PUT /api/v1/users/me/legal`.
- La eliminación de cuenta está declarada `DELETE-ACCOUNT-BACKEND-BLOCKED` aunque el Backend ya expone y validó `GET/POST /api/v1/users/me/deletion`.

## 2. Root cause (auditoría D19, alcance restringido)

1. **Sesión:** `InMemorySessionStore.accessToken` es `null` cuando la sesión persistida aún no se hidrató o el access token expiró. `BearerTokenInterceptor.intercept()` envía la request protegida **sin** header `Authorization` (`BearerTokenInterceptor.kt:45-48`). El Backend responde 401. `RefreshAuthenticator` y `RestoreSessionUseCase` ya existen y son correctos (single-flight, máximo 1 retry, sin loops), pero el interceptor no distingue "sin token" de "token no disponible temporalmente" y el error 401 llega a la UI como `Unknown`.
2. **Profile/Preferences DTO:** correctos. No se rediseñan contratos.
3. **Header:** `Scaffold.topBar` en `TheHouseRoot` contiene **solo** el `IconButton` del menú, sin `BrandMark`, sin texto `TheHouse`, sin `Arrangement.End` y sin insets de status bar → Compose alinea al inicio (esquina superior izquierda).
4. **Drawer:** `DrawerItemCatalog.authItems()` omite la entrada de privacidad; `guestItems()` usa la etiqueta "Privacidad" en vez de "Aviso de privacidad".
5. **Legal/Deletion:** las implementaciones están deliberadamente bloqueadas con contrato falso; el Backend confirmado ya las provee.

## 3. Contrato Backend confirmado (source of truth 780b7be)

- `GET /api/v1/users/me` → `id, email, createdAt, legal, legalUpToDate`.
- `GET/PATCH /api/v1/users/me/profile` → `name, university, quickSetupCompleted, updatedAt`; PATCH **solo** `{name}`.
- `PUT /api/v1/users/me/university` → `{universityId}`.
- `GET/PUT /api/v1/users/me/preferences` → `budget{min,max,currency:MXN}, maxDistanceKm(0.5..30, 1 decimal), essentialServices, livingWith, placeType, pets(bool?), transport, propertyTypes`. **No enviar** `version`, `quickSetupCompleted`, `updatedAt`, `userId`.
- `GET /api/v1/legal` (público) → `termsVersion, privacyVersion, landlordTermsVersion, termsUrl?, privacyUrl?, accountDeletionUrl?, updatedAt`. URLs pueden ser `null`.
- `PUT /api/v1/users/me/legal` → body `{termsVersion, privacyVersion}` → `200 {termsVersion, privacyVersion, acceptedAt}`; `409 legal_version_outdated`.
- `GET /api/v1/users/me/deletion` → `{canDelete, blockers[{code,count}], effects{...}}`.
- `POST /api/v1/users/me/deletion` → body `{password, confirmation:"ELIMINAR"}` (case-sensitive) → **204**; `403 password_mismatch`, `409 deletion_blocked`, `429`, `503 deletion_incomplete`.
- `POST /api/v1/auth/register` → **solo** `{email, password}`. Campos legales NO se envían.

Fuente: `docs/mobile/contrato-pendiente-mobile.md` del Backend (§2 y §3) y specs `nv-10-account-session`, `nv-11-profile-preferences`.

## 4. Decisiones humanas vigentes

1. Registro: los checkboxes de términos/privacidad pueden permanecer como requisito UX previo, pero **NO** se envían al Backend. El texto "Aviso de privacidad" debe ser clicable y abrir la pantalla de privacidad/legal.
2. Legal: si `privacyUrl == null`, NO mostrar error absurdo ni URL inventada; mostrar estado seguro ("El aviso de privacidad está disponible dentro de la aplicación").
3. Aceptación legal: si el producto no inicia sesión automáticamente tras register, **no inventarlo**; en el login, si `legalUpToDate == false`, solicitar aceptación de las versiones vigentes.
4. `409 legal_version_outdated`: recargar `GET /legal`, mostrar la nueva versión, sin loop infinito.
5. Drawer: la sección "Arrendador" sigue visible para todo usuario autenticado (decisión temporal D15). **NO** inventar claim `isLandlord`.
6. Delete: la UI debe pedir contraseña y confirmación explícita "ELIMINAR"; NO un AlertDialog sí/no. Tras 204: purga local, sesión `Anonymous`, back stack limpio, sin requests autenticadas posteriores.

## 5. Alcance

1. **Sesión:** auditar/fijar el flujo `SessionStore → AuthRepository → BearerTokenInterceptor → RefreshAuthenticator → RetrofitFactory → RootViewModel`; access token válido o renovado antes/al recibir 401; retry una sola vez; sin refresh loops; refresh fallido → `Anonymous` + login requerido, nunca "error inesperado". Sin logs de tokens.
2. **Profile:** GET 200; 401 definitivo → `RequireLogin`; PATCH name y PUT university según contrato.
3. **Preferences:** mantener DTOs/contrato; corregir solo flujo auth/session y UX de errores (401 → `RequireLogin`; 200 → formulario; PUT 200 → estado/cache).
4. **Header:** `Scaffold.topBar` de `TheHouseRoot` con `[BrandMark] TheHouse … [☰]`, insets de status bar, un solo `home_menu_button`, conservar `ModalNavigationDrawer`.
5. **Drawer:** items exactos (guest 2, auth 7 + sección Arrendador 4 + Aviso de privacidad + Cerrar sesión), etiqueta "Aviso de privacidad".
6. **Legal API:** `LegalApi`, `LegalRepository`, caso de uso; `CurrentUserDto` con `legal/legalUpToDate`; `PrivacyScreen` con estado real.
7. **Delete account:** `AccountApi`, preview, ejecución con password + "ELIMINAR", mapeo de 403/409/429/503, purga y navegación.
8. Tests unitarios y documentación SDD.

## 6. Fuera de alcance (explícitamente NO tocar en este PR)

- `/rooms/search`, RoomDetail, PR #33, Favorites heart, Visits roomId, Smart Match feedback, FCM Devices, androidTest general, ONNX.
- Backend y GenAI.
- Merge automático del PR.

## 7. Criterios de aceptación

Ver `task.md` T01..T08 y los gates de `checklist.md`. Resumen:

1. Bearer presente con token válido; 401 con refresh válido → refresh + retry 1 vez; refresh inválido → Anonymous; 0 loops.
2. Profile y Preferences cargan 200; 401 → `RequireLogin`; sin "error inesperado".
3. Header con logo + "TheHouse" + hamburger a la derecha; 1 solo `home_menu_button`; status bar respetada.
4. Drawer guest/auth exactos; auth incluye "Aviso de privacidad".
5. Register body `{email,password}`; link de privacidad navegable; URLs null seguras.
6. `GET /legal` y `PUT /users/me/legal` con 409 manejado.
7. Delete preview + password + "ELIMINAR"; 403/409/429/503 mapeados; 204 → purga + Anonymous + back stack limpio.
8. Gates: `clean compileDebugKotlin`, `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `git diff --check` en verde.

## 8. Riesgos

- El 401 puede provenir de refresh no completado al arranque: requiere verificar que la hidratación del access token termine antes de las pantallas protegidas.
- `legal_version_outdated` puede generar loop si no se controla el reintento.
- URLs null del legal: no abrir navegador con valores inválidos (`PrivacyPolicyUrl.normalize` ya existe).
- Sin dispositivo/emulador en la sesión: runtime queda `PENDING`; `BLOCKED` nunca equivale a `DONE`.

## 9. Impacto de seguridad y COST-001

- Sin secretos nuevos; sin logs de tokens; sin permisos nuevos.
- Endpoints ya existentes en el Backend: costo $0, sin servicios externos nuevos, sin tarjeta.

## 10. Evidencia esperada

`evidence.md` con comandos reales, resultados, archivos modificados, tests (30 requeridos), gates, y estado `D19-ACCOUNT-UX-CORE-CODE-READY`. Runtime pendiente documentado.
