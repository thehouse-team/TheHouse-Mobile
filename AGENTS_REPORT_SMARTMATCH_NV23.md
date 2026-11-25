# Smart Match NV-23 v1.2.0 Implementation Report

## BRANCH
feature/smart-match-v1-2

## CONTRACT_VERSION
NV-23 v1.2.0

## ENDPOINTS
- POST /api/v1/smart-match/scores - Calcula scores para roomIds (1-50)
- GET /api/v1/smart-match/recommendations - Recomendaciones autenticadas con paginación
- POST /api/v1/smart-match/recommendations/query - Query pública (sin token), universityId + preferences
- POST /api/v1/smart-match/dismissals - Descarta una recomendación
- DELETE /api/v1/smart-match/dismissals/{roomId} - Restaura una recomendación descartada

## DTO_MAPPING
- SmartMatchScoreRequest: roomIds + optional universityId + preferences
- SmartMatchScoreItem: roomId, matchScore (Double?), breakdown List<SmartMatchBreakdownItem>, dismissed Boolean, excludedBy List<String>
- SmartMatchBreakdownItem: factor String, label String, points Double (decimal), maxPoints Double
- SmartMatchPreferencesDto: budget (SmartMatchBudgetDto), essentialServices, livingWith, placeType, pets, transport, propertyTypes, currency
- SmartMatchBudgetDto: min Double?, max Double?, currency String?
- SmartMatchQueryRequest: universityId + preferences
- SmartMatchRecommendationDto: roomId, title, price, currency, matchScore (Double?), dismissed, excludedBy
- SmartMatchRecommendationResponse: items, page, pageSize, totalItems, totalPages
- SmartMatchListResponse: items, page, pageSize, totalItems, totalPages
- SmartMatchProfileIncompleteError: code "smart_match_profile_incomplete", missing List<String>

## MATCH_SCORE
- Mostrar SIEMPRE matchScore del contrato (no sumar breakdown.points)
- matchScore nullable: room no elegible devuelve null/ "Sin puntuación"
- Diferencia de hasta 0.8 por redondeo permitida - no forzar cálculo local
- Formato: 1 decimal (ej. 73.5%)

## BREAKDOWN
- Mostrar factors usando label del backend (no traducir manualmente)
- Orden esperado: Presupuesto, Distancia, Servicios esenciales, Tipo de lugar, Tipo de inmueble, Extras
- Mostrar points / maxPoints con 1 decimal cuando corresponda
- points soporta decimal, NO redondear a entero
- Breakdown vacío si room no elegible

## EXCLUDED_BY
- List<String> con múltiples razones simultáneas
- Valores posibles: budget, living_with, pets, unavailable
- Mapeo UX seguro:
  - budget → "Fuera de tu presupuesto"
  - living_with → "No coincide con tu preferencia de convivencia"
  - pets → "No compatible con tus preferencias de mascotas"
  - unavailable → "No disponible"
- Mostrar razones cuando eligible=false (no mostrar score falso)

## PROFILE_INCOMPLETE
- Error 422 code=smart_match_profile_incomplete
- Campos faltantes posibles: university, budget.max, livingWith, placeType
- Estado UI: "Completa tu perfil para obtener recomendaciones"
- Señalar campos faltantes específicamente
- CTA: "Completa tu perfil" → navega a Profile/Preferences
- CTA rápida: "Quick Setup"

## DISMISSELS
- Usar campo dismissed del backend
- Los descartados ven al final según orden Backend (dismissed=false primero, matchScore descendente)
- Acción UI: "Descartar" → POST /api/v1/smart-match/dismissals { roomId }
- Restaurar → DELETE /api/v1/smart-match/dismissals/{roomId}
- El descarte dura 30 días según Backend
- NO simular estado permanente local

## PAGINATION
- GET authenticated: /api/v1/smart-match/recommendations?page=1&pageSize=20
- Máximo pageSize=50
- Orden Backend: dismissed=false primero, matchScore descendente
- NO reordenar arbitrariamente en Mobile
- Query pública sin token respetando errores 400/404/422/429/503

## LOCAL_ML_ROLE_AFTER_NV23
- El contrato Backend NV-23 es autoridad para: matchScore, breakdown, eligibility, recommendations, dismissals
- El modelo local LR (LogisticRegression) queda como fallback académico/solo si necesario
- NO mostrar simultáneamente dos scores distintos
- Decisión: modelo local obsoleto para Smart Match v1.2.0, mantener solo para compatibilidad hacia atrás si se requiere requisito académico separado

## TESTS
- Tests existentes basados en ML on-device requieren actualización a repositorio remoto
- Tests necesarios para cobertura NV-23:
  - DTO mapping v1.2
  - matchScore nullable
  - breakdown decimals
  - excludedBy multiple
  - 422 missing profile
  - recommendations pagination
  - dismissed ordering/state
  - dismiss success
  - restore success
  - 429, 503 error handling
  - query pública
  - scores 1-50
  - no sum breakdown para score

## LINT
- Ejecutar: .\gradlew.bat lintDebug
- Verificar: git diff --check
- Máximo 2 ciclos correctivos

## BUILD
- Ejecutar: .\gradlew.bat assembleDebug
- Debe compilar sin errores
- No romper: Profile, Preferences, Quick Setup, Property Detail, Search/Map, Favorites, Offline/cache, Auth/session

## RUNTIME
- Probar con emulator-5554 si disponible
- Validar: Profile completo → Smart Match → recomendaciones → score → breakdown → dismiss → reload → dismissed al final → restore
- Probar perfil incompleto si hay cuenta/datos aptos

## RESULTADO
SMART-MATCH-V1.2-READY