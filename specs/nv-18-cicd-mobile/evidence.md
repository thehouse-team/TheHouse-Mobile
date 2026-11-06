# NV-18 — EVIDENCIA

## FASE D1 — CI real Mobile

### Estados (DoD)
SPECIFIED: OK | IMPLEMENTED: OK (.github/workflows/mobile-ci.yml) | TESTED: OK (local + Actions, ver abajo) | DEPLOYED: N/A (sin deployment) | EVIDENCED: OK (esta página)

### Workflow
- Archivo: `.github/workflows/mobile-ci.yml`
- Triggers: pull_request→main, push→feature/**, workflow_dispatch
- Permisos: `contents: read`
- Jobs: checkout@v4, setup-java@v4 (temurin 17), gradle/actions/setup-gradle@v3, wrapper-validation@v3, testDebugUnitTest, lintDebug, assembleDebug, gitleaks/gitleaks-action@v2, aquasecurity/trivy-action@0.24.0 (fs, HIGH/CRITICAL, exit-code 0), upload-artifact@v4 (unit-test-reports, lint-reports, debug-apk)
- Sin deployment, sin Play publish, sin bundleRelease/signing, sin secretos en YAML (solo referencia a secrets.GITHUB_TOKEN).

### Validación local (2026-11-06)
- Comando: `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr .\gradlew.bat testDebugUnitTest lintDebug assembleDebug`
- Resultado: BUILD SUCCESSFUL in 1m 42s (tests 41/41 heredados, lint sin errores bloqueantes, assembleDebug OK)
- Secret scan básico local: sin secretos reales (solo nombre de paso y referencia a secrets.GITHUB_TOKEN)
- `git diff --check`: limpio

### GitHub Actions (run real)
| Campo | Valor |
|---|---|
| Run ID | 42173546971 |
| Status | completed — success |
| Branch | feature/nv18-mobile-cicd |
| Quality gates | testDebugUnitTest ✓, lintDebug ✓, assembleDebug ✓, gitleaks CLI ✓ (0 secrets), trivy ✓ |
| Artefactos | unit-test-reports, lint-reports, debug-apk |

## Hechos reales preservados
- AAB firmado verificado con jarsigner (`jar verified.`), versionName 1.0.0, versionCode 10000, applicationId com.thehouse.app (NV-03). Sin publicación en Play. register 201, login 200, logout 204, tests 41/41.

### Revisión sdd-guardian T07 (2026-11-06): PASS
- Alcance respetado: solo CI (workflow), sin funcionalidades Mobile, Backend/Azure, Play, /auth/refresh, /users/me, signing/bundleRelease en PR.
- DoD: SPECIFIED→IMPLEMENTED→TESTED→EVIDENCED con run real 42173546971; BLOCKED no cerrado.
- NV-18 REQ-001..004/006 cumplidos en D1; REQ-005/007/008 quedan en T05/T06 (no cerradas).
- Observaciones no bloqueantes: trivy exit-code 0 (REQ-004 incremental); gitleaks CLI free sin licencia.
- NV-02 incremental consistente (6/28, no completa).
