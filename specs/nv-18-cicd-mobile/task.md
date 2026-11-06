# NV-18 - TASKS (FASE D1)

| Campo | Valor |
|---|---|
| Spec | NV-18 CI/CD Mobile |
| STATUS | IN PROGRESS — Fase D1 (CI real) |
| AUTHORIZATION | Autorización D1: crear/configurar CI/CD Mobile y ejecutar controles gratuitos NV-02 |

- [x] NV-18-T01 - Triggers y ramas: pull_request→main, push→feature/**, workflow_dispatch.
- [x] NV-18-T02 - Jobs: checkout → JDK 17 (temurin) → setup-gradle/cache → wrapper-validation → testDebugUnitTest → lintDebug → assembleDebug → gitleaks → trivy → upload-artifact.
- [x] NV-18-T03 - Quality gates: tests + lint + assembleDebug bloqueantes; secret/dep scans según configuración en workflow.
- [x] NV-18-T04 - Secrets/signing/artefactos: sin secretos en YAML; sin keystore/signing/bundleRelease/Play en PR CI; artefactos reports/test-results/lint/APK debug.
- [ ] NV-18-T05 - Approvals (release) y promoción controlada a Play Internal Testing. [Fuera de alcance D1]
- [ ] NV-18-T06 - Rollback y versionado (NV-03/VERSIONING.md). [Fuera de alcance D1]
- [ ] NV-18-T07 - Revisión sdd-guardian del diseño. [Pendiente]
- [x] NV-18-T08 - Implementar workflows y ejecutar (D1); registrar evidencia real en evidence.md.
