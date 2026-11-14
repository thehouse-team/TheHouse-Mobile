# NV-17 — CHECKLIST

| Campo | Valor |
|---|---|
| Spec | NV-17 Smart Match ML |
| STATUS | EXECUTED (RUNTIME-BLOCKED) |

## Gobernanza / documentación
- [x] Spec NV-17 con enfoque **ML real** (no reglas/ifs/ponderaciones como solución final).
- [x] Implementación autorizada y ejecutada; sin merge/force push; sin servicios de pago/infra nueva.
- [x] Descubrimiento acotado: sin endpoint ML backend; candidatos `GET /api/v1/properties`; preferencias D11 reales.
- [x] Pipeline `INPUT→FEATURES→NORMALIZATION→MODEL→INFERENCE→SCORE/RANKING` implementado y documentado.
- [x] COST-001 $0 y privacidad/PII documentados y respetados.
- [x] Riesgos/limitaciones (dataset sintético, vector reducido, runtime bloqueado) documentados.

## Gate F0
- [x] Contratos preferences/properties verificados y feature vector final cerrado con datos reales mapeables.
- [x] Decisión **GO** (features suficientes: budget_fit, services_overlap, price_affinity, currency_match). Sin degradación a reglas.

## Implementación
- [x] Modelo ML real (regresión logística) entrenado offline determinista y versionado (artefacto en repo/app, sin servicios externos).
- [x] Inferencia on-device con coeficientes del artefacto (no pesos hardcodeados), fuera de main thread (`dispatchers.default`), sin recálculo en recomposición por diseño, sin requests desde Compose.
- [x] Score 0–100 derivado matemáticamente de la salida del modelo; ranking determinista.
- [x] Factores de compatibilidad separados del ML score; sin GenAI.
- [x] UI: Loading / RequiresAuth / NoPreferences (CTA) / Empty / Content / Error; datos reales; reutiliza PropertyCard/MatchBadge/Detalle.
- [x] Sin PII/tokens en features ni logs; sin propiedades fake en producción.
- [x] Tests: features, normalización, inferencia, determinismo, rango, ranking, cambios preferencias, incompatibilidades, vacíos/errores, malformado/hash, privacidad, integración lógica.
- [x] `compileDebugKotlin` verde; `testDebugUnitTest lintDebug assembleDebug` verdes (213 tests, 0 failures, 0 errors; lint errores 0).
- [ ] Runtime en emulador/dispositivo — **RUNTIME-BLOCKED** (sin adb/emulador en entorno). No marcado como validado.
- [x] `evidence.md` con resultados reales (algoritmo, por qué ML, entrenamiento/carga/hash, costo $0, privacidad, limitaciones).