---
name: privacy-data-safety
description: Valida privacidad y Data Safety en TheHouse Mobile (datos personales, permisos, política y declaraciones de Play).
---

# Privacy Data Safety

## Nombre

privacy-data-safety

## Objetivo

Verificar datos personales, permisos y declaraciones de privacidad (política de privacidad y Google Play Data Safety) de TheHouse Mobile.

## Cuándo utilizarlo

- Antes de una release en Google Play.
- Al agregar o modificar permisos o datos personales.
- Al revisar la política de privacidad.
- Al revisar la declaración Data Safety en Play Console.

## Entradas

- `AndroidManifest.xml` (permisos), política de privacidad, Data Safety vigente.
- `AGENTS.md`, `constitution.md`, `memoria.md`.
- Spec/plan/task de privacidad autorizados, si existen.

## Procedimiento

1. Leer la jerarquía y confirmar spec/plan/task autorizados.
2. Inventariar datos personales, finalidad y base legal.
3. Revisar permisos y su justificación.
4. Contrastar código/manifest con política y Data Safety.
5. Detectar almacenamiento en texto plano o transmisión innecesaria.
6. Emitir `PASS`, `PASS WITH WARNINGS` o `BLOCKED` con acción requerida.

## Reglas

- Ninguna skill sustituye spec ni autorización.
- No es seguridad general: solo privacidad, datos personales y Data Safety.
- No agregar permisos ni SDKs sin spec.
- No guardar secretos ni datos personales en el repositorio.

## Criterios de finalización

- Inventario de datos/permisos documentado.
- Política y Data Safety consistentes con la app.
- Riesgos y acción requerida documentados.
- Dictamen emitido.

## Specs relacionadas

- NV-03 (Release / Play) — privacidad y Data Safety.
- NV-04 (Auth + Onboarding) — datos de cuenta/onboarding.
- NV-15 (Privacy / Delete Account) y NV-03 BLOCKED-PRIVACY-URL.
