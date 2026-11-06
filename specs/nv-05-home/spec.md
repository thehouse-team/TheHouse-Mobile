# NV-05 — HOME

| Campo | Valor |
|---|---|
| Spec ID | NV-05 |
| STATUS | PLANNED |
| AUTHORIZATION | DO NOT EXECUTE |
| Dependencias | NV-01 (foundation), NV-04 (auth/onboarding) |

## Problema
Tras el onboarding, el usuario necesita una pantalla de inicio (Home) que resuma su estado y le de acceso a las funcionalidades principales sin adivinar la navegacion.

## Alcance
- Pantalla Home posterior a sesion valida y onboarding completado.
- Resumen de estado de sesion, accesos directos y contenedor de navegacion principal.
- Estados loading/content/empty/error segun UDF de NV-01.

## No-alcance
- Catalogo (NV-06), busqueda/mapa (NV-07), favoritos (NV-09) u otras funcionalidades: solo enlaces, sin implementarlos aqui.
- Endpoints nuevos o integraciones externas.

## Requisitos
- NV05-REQ-001: Home solo visible con sesion valida (NV-04); si no, redirige a Login/Onboarding.
- NV05-REQ-002: ViewModel con UiState/UiEvent/UiEffect (MVVM+UDF), sin logica de negocio en Composables.
- NV05-REQ-003: Accesos directos accesibles (>=44dp, semantics, contentDescription).

## Criterios de aceptacion
- Usuario autenticado ve Home; usuario sin sesion no la ve.
- Cada accion emite evento tipado y navega por ruta registrada.
- Sin dependencias de Data/Retrofit en Presentation.

## Riesgos
- Home provisional de NV-04 queda obsoleta sin migracion ordenada.
- Espacios en blanco antes de que existan NV-06..NV-12.

## Impacto seguridad
- No exponer datos de sesion/tokens en UI o logs; logout sigue limpiando sesion (NV-04).

## COST-001 ($0)
- Free tier: sin servicios externos. Limites: ninguno. Tarjeta: no. Riesgo cobro: ninguno. Mitigacion: no integrar servicios. Alternativa: N/A (100% local/nativo).

## Relacion con otras specs
- NV-01 (UDF/navegacion), NV-04 (sesion/gate), NV-06..NV-12 (destinos futuros), NV-25 (accesibilidad).

## Evidencia esperada
- Capturas/estados, tests ViewModel, checklist y evidence.md con resultados reales tras autorizacion. Hoy: sin evidencia.
