# NV-21 — ANDROID PERMISSIONS / PLATFORM SECURITY

| Campo | Valor |
|---|---|
| Spec ID | NV-21 |
| STATUS | PLANNED |
| AUTHORIZATION | DO NOT EXECUTE |
| Dependencias | NV-03 (manifest/permisos), NV-07 (ubicacion), NV-14 (notificaciones), NV-20 |

## Problema
Cada permiso Android es superficie de ataque y de declaracion en Play; la app necesita un inventario justificado y minimo.

## Alcance
- Inventario de permisos (declarados + transitivos), justificacion por permiso y flujo de solicitud en runtime.
- Seguridad de plataforma: intents, deep links, exported components, backup rules.

## No-alcance
- Agregar permisos sin justificacion; permisos de specs futuras sin su spec.
- Cambiar el manifest sin autorizacion (NV-03).

## Requisitos
- NV21-REQ-001: Tabla permiso->justificacion->spec que lo usa; permiso sin justificacion se elimina.
- NV21-REQ-002: Permisos runtime con rationale y fallback sin permiso (ubicacion NV-07, notificaciones NV-14).
- NV21-REQ-003: Componentes exported/deep links auditados; intents explicitos donde aplique.

## Criterios de aceptacion
- Manifest fusionado revisado permiso por permiso; Data Safety (NV-03) coincide con el inventario.
- Flujos sin permiso probados (denegado/permanente).

## Riesgos
- Permisos transitivos no revisados (NV-01 O-01); deep links que bypassean auth.
- Sobre-permisionado que bloquea revision de Play.

## Impacto seguridad
- Minimo privilegio; datos sensibles (ubicacion, notificaciones) con minimizacion.

## COST-001 ($0)
- Free tier: revision manual del manifest fusionado. Limites: N/A. Tarjeta: no. Riesgo cobro: ninguno. Mitigacion: sin servicios nuevos. Alternativa: N/A.

## Relacion con otras specs
- NV-03 (manifest/Data Safety), NV-07, NV-14, NV-20.

## Evidencia esperada
- Inventario de permisos y pruebas con permiso denegado. Hoy: sin evidencia.
