# NV-09 — EVIDENCIA

Estado: FAVORITES-BACKEND-BLOCKED.

## Backend read-only
- Revisado C:\THEHOUSE\TheHouse-Backend.
- No existen endpoints de favoritos (list/add/remove/check).
- PropertyEndpoint solo expone list/detail públicos.

## Mobile
- FavoritesScreen mantiene Guest -> Login.
- Auth -> Empty state con CTA Explorar y nota de backend pendiente.
- No se implementa persistencia fake ni Room.
- Home/Explore/Detail siguen usando eventos de favorito sin persistencia real.

## Validación
- compileDebugKotlin: BUILD SUCCESSFUL
- testDebugUnitTest: BUILD SUCCESSFUL
- lintDebug: BUILD SUCCESSFUL
- assembleDebug: BUILD SUCCESSFUL
- git diff --check: sin errores