# TASK D19 — Account + UX Core

| Campo | Valor |
|---|---|
| Spec | D19-Account-UX-Core |
| Plan | Fases F1..F7 (`plan.md`) |
| Autorización | Humana explícita 2026-11-26 |

## Tasks atómicas

- [x] **T01 · Sesión/Bearer/Refresh** (F2). Auditar flujo completo. Asegurar Bearer con token válido, refresh single-flight con retry único, 401 definitivo → `Anonymous` + login, sin loops, sin logs de token. Evidencia: tests unitarios 1-4.
- [ ] **T02 · Profile** (F3). GET 200; 401 → `RequireLogin`; PATCH `{name}`; PUT `{universityId}`. Sin campos inventados. Evidencia: tests 5-8.
- [ ] **T03 · Preferences** (F3). DTOs intactos; GET 200 → formulario; PUT 200 → estado/cache; 401 → `RequireLogin`; sin campos extra. Evidencia: tests 9-11.
- [ ] **T04 · Header/hamburger** (F4). `TheHouseRoot.topBar` con BrandMark + "TheHouse" + hamburger a la derecha, status bar, un solo `home_menu_button`, drawer global conservado. Evidencia: tests 12-14.
- [ ] **T05 · Drawer definitivo** (F4). Guest exacto; auth 7 + Arrendador 4 + Aviso de privacidad + logout; etiqueta corregida; sin rol inventado. Evidencia: tests 15-18.
- [ ] **T06 · Registro + link privacidad** (F5). Register body `{email,password}` intacto; texto "Aviso de privacidad" clicable → ruta `privacidad`. Evidencia: tests 19-20.
- [ ] **T07 · Legal API + aceptación** (F5). `GET /legal` con URLs null; `PUT /users/me/legal`; `legal/legalUpToDate` en `/users/me`; 409 → recarga sin loop; PrivacyScreen con versión/fecha/URL segura. Evidencia: tests 21-23.
- [ ] **T08 · Delete account** (F6). Preview + ejecución con password/"ELIMINAR"; mapeo 403/409/429/503; 204 → purga + Anonymous + back stack. Evidencia: tests 24-30.
- [ ] **T09 · Gates y cierre** (F7). Gates, memoria, evidencia, commit/push/PR sin merge. Evidencia: `evidence.md`.

## Reglas de ejecución

- Una task no puede ampliar el alcance de su plan.
- No tocar Rooms/Explore/RoomDetail/FCM/androidTest salvo regresión directa.
- No modificar Backend ni GenAI.
- `BLOCKED` nunca equivale a `DONE`.
