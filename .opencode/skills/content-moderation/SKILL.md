---
name: content-moderation
description: Valida moderación de contenido generado por usuarios e IA en TheHouse Mobile, sin implementar servicios externos.
---

# Content Moderation

## Nombre

content-moderation

## Objetivo

Validar la moderación de contenido generado por usuarios o IA en TheHouse Mobile: políticas, reportes y bloqueos, sin integrar servicios externos.

## Cuándo utilizarlo

- Al aceptar contenido generado por usuarios.
- Al aceptar contenido generado por IA.
- Al revisar reportes, bloqueos y fluj de moderación.
- Al revisar políticas de contenido de Google Play.

## Entradas

- Tipos de contenido y sus productores (usuarios/IA).
- `constitution.md`, `AGENTS.md`, `memoria.md`.
- Políticas de contenido de Google Play vigentes.

## Procedimiento

1. Leer la jerarquía y confirmar spec/plan/task autorizados.
2. Clasificar tipos de contenido y riesgos (incl. contenido de IA).
3. Revisar reportes, bloqueos y canales de apelación.
4. Verificar que ningún dato personal se envíe a moderación externa sin base legal.
5. Evaluar COST-001 para cualquier servicio externo propuesto.
6. Emitir `PASS`, `PASS WITH WARNINGS` o `BLOCKED` con acción requerida.

## Reglas

- Ninguna skill sustituye spec ni autorización.
- No integrar servicios de moderación sin spec autorizada.
- No inventar endpoints ni proveedores.
- No guardar secretos ni claves de servicios en el repositorio.

## Criterios de finalización

- Mapa contenido → riesgos → controles documentado.
- Costo $0 verificado para servicios externos.
- Dictamen y acción requerida documentados.

## Specs relacionadas

- NV-22 (Multimedia / CDN / Content Moderation) y NV-17 (ML/IA) — PLANNED.
- NV-03 (Release / Play) — políticas de contenido.
