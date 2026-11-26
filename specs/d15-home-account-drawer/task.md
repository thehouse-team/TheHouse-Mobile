# TASKS D15 — Home Account Drawer

| ID | Descripción | Dependencias | Estado |
|---|---|---|---|
| D15-T01 | Documentar inventario y decisiones guest/auth/rol en spec | Ninguna | [x] |
| D15-T02 | Crear `DrawerItemCatalog.kt` + `AppDrawer.kt` con 12 ítems + sección "Arrendador" + testTags | T01 | [x] |
| D15-T03 | Integrar drawer global en `TheHouseRoot` (`MainActivity.kt`) con navController/sesión/suspensión/bottom nav | T02 | [x] |
| D15-T04 | Retirar `AccountActionsCard`, "Tu cuenta", 12 filas y drawer/hamburger local de `HomeScreen.kt` | T03 | [x] |
| D15-T05 | Cablear 11 callbacks en `MainActivity` + corregir `onOpenNotifications` | T03 | [x] |
| D15-T06 | Aplicar reglas guest/auth/rol (sección temporal Arrendador) y logout vía flujo real | T01, T05 | [x] |
| D15-T07 | Verificar back navigation y bottom nav; sin callbacks vacíos | T05, T06 | [x] |
| D15-T08 | Tests, gates, evidencia y PR separado (sin merge) | T02–T07 | [x] |
