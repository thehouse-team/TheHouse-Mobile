# PLAN D19.1 — Account Runtime Completion

## Fase A — Verificación y diagnóstico (solo lectura + instrumentación)

- A1. Confirmar基线: rama desde `50febcd`, presencia de D18/D19 y del fix de `LandlordProfileViewModel`.
- A2. Perfil: auditar `ProfileApi`, `ProfileDto`, `ProfileRepository`, `GetProfileUseCase`, `ProfileViewModel` y el estado UI. Agregar `ProfileFlow` DEBUG seguro.
- A3. Preferencias: auditar `PreferencesApi`, `PreferencesDto`, `PreferencesContract`, mapper y events. Agregar `PreferencesFlow` DEBUG seguro.
- A4. Smart Match: auditar el mapper del 422 (`missing`) y la navigación a RoomDetail.
- A5. Registro → privacidad: auditar `RegisterScreen`, ruta pública de privacidad y `GET /legal`.
- A6. Arrendador: verificar la rama del factory y los estados 404/200/403.

## Fase B — Correcciones verificadas y regresiones

- B1. Corregir únicamente las causas confirmadas en A2–A6.
- B2. Agregar tests de regresión por causa, sin borrar tests existentes.
- B3. Actualizar `evidence.md` y `memoria.md` (ID nuevo, sin duplicados).

## Fase C — Gates y entrega

- C1. `clean compileDebugKotlin`, `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `git diff --check`.
- C2. Commit y push a `fix/d19-runtime-completion`; abrir PR nuevo hacia `main`.
- C3. Verificar PR OPEN/MERGEABLE y CI `Android quality gates` SUCCESS.
- C4. Runtime con ADB si hay credenciales autorizadas; si no, `RUNTIME_PENDING` con pasos exactos para el usuario.

## Criterios de cierre

- Gates locales verdes con conteo real de tests (baseline 514).
- PR OPEN, MERGEABLE, sin conflictos, CI SUCCESS.
- Sin merge a `main`. Sin PR B/C. Sin cambios en Backend/GenAI/infra.
