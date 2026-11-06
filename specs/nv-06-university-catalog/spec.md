# NV-06 — UNIVERSITY CATALOG

| Campo | Valor |
|---|---|
| Spec ID | NV-06 |
| STATUS | PLANNED |
| AUTHORIZATION | DO NOT EXECUTE |
| Dependencias | NV-01, NV-04, NV-27 (contrato backend), NV-05 (acceso) |

## Problema
El usuario necesita explorar las universidades disponibles como punto de partida para buscar alojamiento cercano.

## Alcance
- Listado y detalle basico de universidades (nombre, ubicacion, datos que confirme el contrato NV-27).
- Estados loading/content/empty/error; paginacion solo si el contrato la define.

## No-alcance
- Busqueda avanzada/mapa (NV-07), detalle de propiedades (NV-08).
- Endpoints inventados: solo los confirmados en NV-27 (universities).

## Requisitos
- NV06-REQ-001: Consumir unicamente endpoints confirmados de NV-27; sin contrato, BLOCKED.
- NV06-REQ-002: Repository Domain/Data con mapeo DTO->Domain; UI sin acceso directo a red.
- NV06-REQ-003: Cache offline minima segun NV-16 si aplica.

## Criterios de aceptacion
- Lista renderiza datos reales del contrato o estado vacio/error documentado.
- Errores HTTP se mapean a DomainError/UiError sin filtrar detalle tecnico.

## Riesgos
- Contrato de universities incompleto o cambiante; paginacion no definida.
- Datos sin imagenes/ubicacion degradan experiencia (ver NV-22).

## Impacto seguridad
- Sin PII innecesaria; sin tokens en logs; HTTPS obligatorio.

## COST-001 ($0)
- Free tier: backend existente Azure (tier actual). Limites: los del plan vigente. Tarjeta: la ya contratada por el backend (no nueva en esta spec). Riesgo cobro: sobreuso de llamadas. Mitigacion: cache + paginacion solo si el contrato la define. Alternativa: catalogo estatico versionado si el backend no lo provee.

## Relacion con otras specs
- NV-27 (contrato), NV-07 (busqueda sobre catalogo), NV-16 (offline), NV-22 (contenido multimedia).

## Evidencia esperada
- Tests de repository/ViewModel, captura de estados y transcripcion del contrato usado. Hoy: sin evidencia.
