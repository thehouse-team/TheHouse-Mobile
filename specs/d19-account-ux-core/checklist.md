# CHECKLIST D19 — Account + UX Core

## Gobernanza
- [x] constitution.md leída
- [x] AGENTS.md leída
- [x] memoria.md leída
- [x] spec/plan/task creados y autorizados
- [x] Branch `fix/account-ux-core` desde `origin/main`

## Sesión (1-4)
- [x] 1. access token válido → request con Bearer
- [x] 2. access expirado + refresh válido → refresh + retry una vez
- [x] 3. refresh inválido → logout/Anonymous
- [x] 4. no refresh loop

## Profile (5-8)
- [x] 5. GET profile 200
- [x] 6. 401 definitivo → RequireLogin
- [x] 7. update name (PATCH solo `{name}`)
- [x] 8. update university (PUT `{universityId}`)

## Preferences (9-11)
- [x] 9. GET 200 → formulario
- [x] 10. PUT 200 → estado/cache actualizados
- [x] 11. 401 → RequireLogin

## Header (12-14)
- [x] 12. un solo `home_menu_button`
- [x] 13. header contiene BrandMark/TheHouse
- [x] 14. hamburger pertenece al global topBar

## Drawer (15-18)
- [x] 15. guest items exactos
- [x] 16. auth items exactos
- [x] 17. auth incluye Aviso de privacidad
- [x] 18. logout funciona

## Registro/Legal (19-23)
- [x] 19. register body solo email+password
- [x] 20. link Aviso privacidad navegable
- [x] 21. GET /legal parsea URLs null
- [x] 22. PUT /users/me/legal correcto
- [x] 23. 409 legal_version_outdated manejado

## Delete (24-30)
- [x] 24. preview 200
- [x] 25. password_mismatch 403
- [x] 26. deletion_blocked 409
- [x] 27. delete 204
- [x] 28. local purge
- [x] 29. sesión Anonymous
- [x] 30. back stack limpio

## Gates
- [x] `.\gradlew.bat clean compileDebugKotlin`
- [x] `.\gradlew.bat testDebugUnitTest` (501 tests / 0 failures)
- [x] `.\gradlew.bat lintDebug`
- [x] `.\gradlew.bat assembleDebug`
- [x] `git diff --check`
- [x] memoria.md actualizada
- [x] PR abierto, sin merge

## Runtime (pendiente de dispositivo)
- [ ] A. Header visual `[logo] TheHouse … ☰`
- [ ] B. Perfil carga 200
- [ ] C. Preferencias carga 200 + PUT 200
- [ ] D. Registro abre aviso de privacidad
- [ ] E. Drawer guest exacto
- [ ] F. Drawer auth incluye aviso
- [ ] G. Delete preview + password incorrecta + correcta (cuenta controlada)
