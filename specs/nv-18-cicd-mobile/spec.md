# NV-18 — CI/CD MOBILE (ARQUITECTURA OBJETIVO)

| Campo | Valor |
|---|---|
| Spec ID | NV-18 |
| STATUS | PLANNED |
| AUTHORIZATION | DO NOT EXECUTE (solo arquitectura objetivo; NO crear .github/workflows) |
| Dependencias | NV-02 (CI/quality), NV-03 (release/Play), NV-19 (testing), NV-20 (seguridad) |

## Problema
TheHouse Mobile necesita un pipeline reproducible y auditado que lleve un cambio de codigo desde PR hasta un AAB firmado en Play Internal Testing, con aprobacion humana y sin exponer secretos.

## Arquitectura objetivo (SOLO objetivo, sin implementar)
```text
PR -> compile -> unit tests -> lint -> security scan -> secret scan -> dependency scan
   -> debug build -> human approval -> merge -> release build -> signed AAB
   -> Play Internal Testing track -> controlled promotion (Closed/Production solo manual)
```

## Alcance
- Diseno de triggers, ramas, jobs, quality gates, secretos/signing, artefactos, approvals y rollback (esta spec).
- Definir que NV-02 cubre CI/quality/security y NV-03 cubre release/Play; NV-18 los compone sin duplicarlos.

## No-alcance
- NO crear `.github/workflows/` ni ejecutar pipelines en esta fase documental.
- NO publicar en Play, NO firmar con credenciales reales, NO instalar herramientas sin autorizacion.

## Requisitos (objetivo)
- NV18-REQ-001 Triggers: `pull_request` a ramas protegidas (CI completo); `push` a main (CI + build); `workflow_dispatch` manual solo para release.
- NV18-REQ-002 Branches: proteccion de `main` (revision + checks verdes + aprobacion humana); sin merge directo.
- NV18-REQ-003 GitHub Actions objetivo: jobs separados y cacheados (compile, tests, lint, security scan, secret scan, dependency scan, debug build) con permisos minimos (`contents: read` por defecto) y versiones fijadas de actions.
- NV18-REQ-004 Quality gates: 0 errores de lint, tests verdes, secret scan sin secretos, dependency scan sin Critical/High sin excepcion humana documentada; cualquier fallo bloquea el merge/release.
- NV18-REQ-005 Secrets/signing: keystore y passwords solo como GitHub Secrets/`Environment` (nunca en repo); firma release solo en job de release con aprobacion; Play upload via service account en Secrets.
- NV18-REQ-006 Artifacts: APK debug y reportes como artefactos de CI con retencion corta; AAB firmado solo como artefacto del job de release; ningun secreto en logs/artefactos.
- NV18-REQ-007 Approvals: `Environment` de release con aprobacion humana requerida antes de subir a Play Internal Testing; el primer AAB se sube manualmente (NV-02 §10) y la automatizacion solo tras exito manual verificado.
- NV18-REQ-008 Rollback: versionCode monotono (NV-03/VERSIONING.md); rollback = promocion de la version anterior estable en Play o halt de la promocion; sin re-firmar versiones viejas.

## Criterios de aceptacion (documentales)
- Diagrama y tabla de jobs/gates/secretos aprobados por sdd-guardian.
- Ningun workflow creado ni ejecutado; esta spec queda PLANNED hasta autorizacion expresa.

## Riesgos
- Secretos filtrados en logs/artefactos; permisos excesivos de Actions; promocion accidental a Production.
- Toolchain/cache no reproducible entre local y CI.

## Impacto seguridad
- Superficie (supply chain, Actions, signing) cubierta por NV-20; minimo privilegio y secretos cifrados en el proveedor.

## COST-001 ($0)
- Free tier: GitHub Actions en repo publico / minutos gratuitos del plan vigente (documentar minutos y limites antes de implementar). Limites: minutos mensuales y artefactos. Tarjeta: no nueva. Riesgo cobro: exceder minutos/almacenamiento. Mitigacion: cache, jobs paralelos acotados, retencion corta de artefactos, sin runners de pago. Alternativa: CI local + subida manual del AAB (NV-03) si Actions no es viable a $0.

## Relacion con otras specs
- NV-02 (CI/quality/security, sin CD), NV-03 (release/Play/signing), NV-19 (gates de test), NV-26 (versionado/entornos).

## Evidencia esperada
- Diseno aprobado; tras autorizacion futura: ejecuciones reales de Actions con logs y artefactos. Hoy: sin evidencia de ejecucion.
