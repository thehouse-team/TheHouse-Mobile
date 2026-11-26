# PLAN D15 — Home Account Drawer

| Campo | Valor |
|---|---|
| Spec | D15-Home-Account-Drawer |
| Branch | `fix/home-account-drawer-navigation` |
| PR | Separado, contra `main`. SIN merge automático. |

## Fases

### F1 — Inventario y decisiones (EJECUTADA en solo lectura)
11/11 rutas reales confirmadas con pantalla y VM. Decisiones guest/auth/arrendador registradas en `spec.md` §3.

### F2 — Drawer global
- D15-T02: crear `AppDrawer.kt` reutilizable con 12 ítems + sección Arrendador + testTags.
- D15-T03: integrar en `TheHouseRoot` (`MainActivity.kt`) con `navController`, `isAuthenticated`, `isSuspended`, `showBottomNav`.

### F3 — Limpieza de Home y cableado
- D15-T04: retirar `AccountActionsCard`/`AccountActionRow` y drawer interno de `HomeScreen.kt`.
- D15-T05: cablear 11 callbacks en `MainActivity` + corregir `onOpenNotifications`.
- D15-T06: reglas guest/auth/rol y logout vía evento real.
- D15-T07: verificar back y bottom nav.

### F4 — Tests, gates, PR y evidencia
- D15-T08: tests, gates, PR separado, evidencia. Runtime como condición de merge.

## Orden y dependencias

```text
D15-T01 → D15-T02 → D15-T03 → D15-T04 → D15-T05 → D15-T06 → D15-T07 → D15-T08
```

## Gates

```powershell
.\gradlew.bat clean compileDebugKotlin
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
git diff --check
```

CI remoto verde obligatorio. No reducir tests existentes.

## Condición de merge

Evidencia runtime (drawer, rutas, guest, arrendador, logout, back) y aprobación humana posterior.
