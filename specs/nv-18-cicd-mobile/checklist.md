# NV-18 — CHECKLIST

- [x] Triggers/branches/gates definidos (pull_request→main, push→feature/**, workflow_dispatch)
- [x] Jobs definidos (compile→tests→lint→security→secret→dependency→debug build)
- [x] Secrets/signing/artefactos definidos (sin secretos; sin signing/Play en PR)
- [x] COST-001 $0 (Actions free; gitleaks/trivy free; sin herramientas pagadas)
- [x] Workflow creado en D1: `.github/workflows/mobile-ci.yml`
- [ ] Revisión sdd-guardian del diseño
- [x] Ejecución real tras autorización (validación local PASS; run GitHub Actions en evidence.md)
