# NV-09 — FAVORITES

| Campo | Valor |
|---|---|
| Spec ID | NV-09 |
| STATUS | READY |
| AUTHORIZATION | EXECUTED (D7.1) |
| Dependencias | NV-01, NV-04, NV-08, NV-27, NV-16 (offline) |

## Problema
El usuario necesita guardar propiedades de interes y consultarlas rapidamente, incluso sin conexion.

## Alcance
- Marcar/desmarcar favorito y listar favoritos del usuario.
- Persistencia local (Room) y sincronizacion solo si NV-27 confirma endpoints de favoritos.

## No-alcance
- Recomendaciones ML (NV-17).
- Endpoints de favoritos no confirmados: sin ellos, solo persistencia local documentada como alcance parcial.

## Requisitos
- NV09-REQ-001: Identidad de favorito por id de propiedad confirmado; sin id estable, BLOCKED.
- NV09-REQ-002: Room solo desde Data; Domain sin tipos Room (NV-01).
- NV09-REQ-003: Toggle optimista con rollback ante error y estado persistente.

## Criterios de aceptacion
- Favorito persiste tras reiniciar app; desmarcar lo elimina local y remotamente si hay contrato.
- Lista vacia muestra empty state.

## Riesgos
- Conflictos local/remoto sin estrategia (NV-16); duplicados por ids inestables.

## Impacto seguridad
- Favoritos son datos del usuario: no se exponen en logs ni backups sensibles sin revision (NV-20).

## COST-001 ($0)
- Free tier: Room local + backend existente. Limites: almacenamiento local. Tarjeta: no. Riesgo cobro: ninguno. Mitigacion: sin servicios nuevos. Alternativa: N/A.

## Relacion con otras specs
- NV-08 (origen), NV-16 (offline/sync), NV-17 (match), NV-27 (contrato).

## Evidencia esperada
- Tests DAO/repository, migracion Room y estados. Hoy: sin evidencia.
