# NV-07 — EVIDENCIA

Sin evidencia todavia — PLANNED, DO NOT EXECUTE.

## Hechos reales preservados
- Contrato confirmado M-MOB-044: register/login/logout/properties/universities/health. Sin coordenadas/filtros confirmados en esta conversacion: no inventar endpoints ni campos.

## Evidencia D4 parcial (2026-11-06, branch feature/home-guest-navigation)
- ExploreScreen (tab Explorar) con fixture del prototipo (PrototypeProperties), etiquetas Match/Verificado y nota "Mapa completo pendiente de NV-07". Sin SDK de mapas, sin permisos, sin endpoint inventado, sin busqueda con filtros confirmados.
- Verificado con `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` BUILD SUCCESSFUL 62/62 (suite compartida NV-05).
- NO se marca Explore/Map completo, ni Smart Match ML, ni Favorites backend.

## Evidencia D5 parcial - catalogo/Explore (2026-11-06, branch feature/universities-properties-explore)
Contrato real AllowAnonymous (solo lectura Backend):
- GET /api/v1/properties?page&pageSize -> 200 {items[{id,title,price,currency,city,universityId,bedrooms,bathrooms,areaM2,createdAt}],page,pageSize,totalItems,totalPages}; 400/429/503/500.
- GET /api/v1/properties/{id} -> 200 {id,title,description,price,currency,city,universityId,bedrooms,bathrooms,areaM2,amenities,createdAt}; 400/404/429/503/500.
Implementacion: PropertyApi/DTOs/mapper/PropertyRepositoryImpl + ExploreViewModel (lista real, estados, busqueda local titulo/ciudad) + Home destacada real (primera propiedad, regla UI local), PropertyCard real sin Match/verificacion/distancia/filtros inventados, mapa placeholder, Smart Match/Favoritos sin cambios.
Tests: CatalogRepositoryIntegrationTest 9/9 + CatalogViewModelTest 4/4; suite 75/75 verde, BUILD SUCCESSFUL.
NO se marca mapa/Maps SDK, ML, Favorites backend (siguen BLOCKED/pendientes).

## Evidencia futura esperada
Matriz de contrato, evaluacion de costo de mapas y resultados reales tras autorizacion.
