# NV-05 — EVIDENCIA

Sin evidencia todavia — PLANNED, DO NOT EXECUTE.

## Hechos reales preservados (specs anteriores, no re-ejecutados)
- NV-01 ejecutada/validada (30/30); NV-04: register 201 Created {email,password}, login 200, logout 204; bloqueos /auth/refresh y /users/me vigentes; BLOCKED-PRIVACY-URL; sin publicacion en Play.

## Evidencia D4 (2026-11-06, branch feature/home-guest-navigation)
Base D3 verificada (0778948). Fuente visual: 00-onboarding.html + 07-inicio-estudiante.html + landing.
- Home shell con bottom nav (Inicio/Explorar/Smart Match/Favoritos), PropertyCard/MatchBadge/VerifiedBadge/BottomNavigation, fixture PrototypeProperties (El Llano $3,200/8 min/95%, Laureles $2,800/12 min/91%, etiquetada "Demo del prototipo", sin backend).
- Guest real (Anonymous sin sesion falsa): Omitir/Terminar -> Home Guest; Favoritos Guest -> muro con Iniciar sesion; favorito/contactar/publicar Guest -> RequireLogin -> Login.
- Onboarding prototipo: TheHouse, Omitir, Busca tu universidad / Encuentra la que encaja / Rentas cuartos, Siguiente/Comenzar, Inicia sesion.
- `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` BUILD SUCCESSFUL, 62/62 verdes (HomeGuestNavigationTest 8/8; D3 intacta 54/54).

## Evidencia futura esperada
Comandos ejecutados y resultados reales tras autorizacion humana. No inventar resultados.
