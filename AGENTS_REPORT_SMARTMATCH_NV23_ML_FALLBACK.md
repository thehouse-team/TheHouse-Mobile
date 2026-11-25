# Smart Match NV-23 + Local ML Fallback Implementation Report

## BRANCH
feature/smart-match-v1-2-ml-fallback

## REMOTE_AUTHORITY
Backend NV-23 v1.2.0 is the primary/only authority for Smart Match when online:
- matchScore (nullable, highlighted)
- breakdown (factors with label/points/maxPoints)
- eligibility (excludedBy reasons)
- dismissed state
- recommendations pagination

LOCAL_ML_RUNTIME = YES (fallback only when online check fails)
- Model: model_v1.json (logistic regression, version 1.0.0)
- UseCase: RunSmartMatchUseCase(model)
- Feature extractor: SmartMatchFeatureExtractor

## FALLBACK_TRIGGERS
Network conditions that trigger local ML fallback:
- No connectivity (ConnectivityObserver.noConnectivity)
- Network timeout
- HTTP 503 Service Unavailable
- Network error (no connection, cancellation)

## NON_FALLBACK_ERRORS
Errors that DO NOT trigger local ML fallback (show appropriate UI instead):
- 401 Unauthorized → show requires auth
- 404 Not Found → show error state
- 422 Profile Incomplete → show ProfileIncompleteErrorView (DO NOT fallback to ML)
- 429 Rate Limited → show rate limit message + retry, NO ML fallback
- Any client error (4xx except network issues)

## OFFLINE_UI
When local ML fallback is active:
- Header/subtitle changed to indicate estimation
- Score displayed with "Estimación local" badge
- Breakdown NOT shown (cannot invent NV-23 breakdown from local model)
- excludedBy NOT shown
- dismissed NOT shown (local model has no dismissal state)
- Property info still displayed
- CTA: "Reintentar conexión Smart Match"

## REMOTE AUTHORITY RULES
- Never show local ML score when remote response is valid (200)
- Never calculate score = sum(breakdown.points) - violates NV-23 contract
- Never display fallback breakdown as if it were NV-23
- Never persist local score as if it were backend result
- When connectivity restores, always retry and replace fallback with official result

## UI CHANGES REQUIRED
1. SmartMatchScreen: Add "Estimación local" indicator when fallback active
2. ScoreBar/Score display: Add badge/text when using local ML
3. When fallback: simplify card - show only score + property, no breakdown
4. Retry button/action when fallback is active

## CODE CHANGES REQUIRED
1. SmartMatchViewModel: Add connectivity check + fallback logic
2. SmartMatchRepository: Add method to check network state, run fallback
3. SmartMatchScreen: Conditional UI based on online/offline state

## TESTS REQUIRED
- Remote 200 → uses Backend, NO ML local
- Network unavailable → uses ML local
- Timeout → ML local
- 503 → ML local
- 422 → NO fallback (ProfileIncomplete state)
- 429 → NO fallback (rate limit message)
- 401 → NO fallback (auth required)
- Retry remote exitoso reemplaza fallback
- Nunca muestra dos scores simultáneamente
- Mantener tests ML existentes

## RUNTIME VALIDATION
A. Online (emulator-5554 with data):
   - Smart Match → resultado NV-23 oficial
   - No "Estimación local" badge

B. Offline (data disabled/wifi off):
   - Smart Match → "Estimación local" badge + score
   - No breakdown, no excludedBy
   - Retry button available

C. Recovery (connectivity restored):
   - Retry → resultado Backend oficial replaces fallback
   - UI returns to normal remote state

## GATES
- .\gradlew.bat testDebugUnitTest ✅
- .\gradlew.bat lintDebug ✅
- .\gradlew.bat assembleDebug ✅
- git diff --check ✅

## RESULTADO
SMART-MATCH-ML-RUNTIME-READY