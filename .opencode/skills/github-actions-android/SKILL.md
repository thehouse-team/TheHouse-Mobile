---
name: github-actions-android
description: Aplica CI Android en GitHub Actions (build, test, lint, caché, artefactos), solo pipeline, sin DevSecOps general ni release.
---

# GitHub Actions Android

## Nombre

github-actions-android

## Objetivo

Definir y revisar la integración continua Android en GitHub Actions: compilación, tests unitarios, lint, caché de Gradle y artefactos, con toolchain congelado.

## Cuándo utilizarlo

- Al crear o modificar workflows de CI de TheHouse Mobile.
- Al validar que un cambio mantenga el pipeline verde.
- Al revisar triggers, caché y artefactos de CI.
- Al revisar que el toolchain congelado se respeta en CI.

## Entradas

- `.github/workflows/` existentes.
- `AGENTS.md`, `constitution.md`, `memoria.md`.
- Spec/plan/task de CI autorizados, si existen.

## Procedimiento

1. Leer la jerarquía y confirmar spec/plan/task autorizados.
2. Verificar JDK 17 y toolchain congelado (AGP `9.5.0`, Gradle `9.6.0`, Kotlin `2.2.10`).
3. Revisar jobs: `assembleDebug`, `testDebugUnitTest`, `lintDebug`.
4. Confirmar caché de Gradle y artefactos con retención acotada.
5. Confirmar que los fallos sean visibles (sin `continue-on-error` que los oculte).
6. Emitir `PASS`, `PASS WITH WARNINGS` o `BLOCKED` con evidencia esperada.

## Reglas

- Ninguna skill sustituye spec ni autorización.
- Solo pipeline: no cubre DevSecOps general, secretos de release ni publicación.
- Sin secretos en workflows ni en el repositorio.
- Todo servicio o action externa debe evaluarse contra COST-001 (costo objetivo $0).

## Criterios de finalización

- Jobs y triggers revisados contra el toolchain congelado.
- Evidencia por job definida (comando, artefacto, resultado).
- Fallos visibles y artefactos acotados.
- Dictamen y acción requerida documentados.

## Specs relacionadas

- NV-18 (CI/CD Mobile) y NV-02 (DevSecOps) — ámbito exclusivo de CI Android.
- NV-01 (Foundation) — baseline compilable.
