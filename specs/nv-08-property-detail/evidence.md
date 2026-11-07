# NV-08 — EVIDENCIA

Estado: D6 completado (Property Detail + network safety).

## Resultados
- compileDebugKotlin: BUILD SUCCESSFUL
- testDebugUnitTest: BUILD SUCCESSFUL
- lintDebug: BUILD SUCCESSFUL
- assembleDebug: BUILD SUCCESSFUL
- git diff --check: sin errores

## Tests
- PropertyDetailViewModelTest: detail 200, 404, network error + retry
- CatalogRepositoryIntegrationTest: property detail 200 con descripcion/amenities
- Network mapping: UnknownHostException/IOException -> DomainError.Network; SocketTimeoutException -> DomainError.Timeout; CancellationException rethrow

## Navegación
- Home -> Detail(propertyId)
- Explore -> Detail(propertyId)
- Detail Back -> popBackStack

## Acciones
- Favorito: Guest -> Login; sin backend
- Contactar: Guest -> Login; Auth -> CTA preparado; sin chat
- Visitas: no implementadas

## Archivos clave
- app/src/main/java/com/thehouse/app/presentation/propertydetail/PropertyDetailScreen.kt
- app/src/main/java/com/thehouse/app/presentation/propertydetail/PropertyDetailViewModel.kt
- app/src/main/java/com/thehouse/app/data/catalog/repository/CatalogRepositoriesImpl.kt
- app/src/test/java/com/thehouse/app/presentation/propertydetail/PropertyDetailViewModelTest.kt