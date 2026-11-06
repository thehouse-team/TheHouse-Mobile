# NV-03 — CHECKLIST

Fuente: `evidence.md` NV-03 (AAB firmado `jar verified.`, versionName 1.0.0, versionCode 10000, applicationId com.thehouse.app) y `task.md` (NV03-T001..T027 marcados [x]).

- [x] Identidad/versionado (`applicationId`, `versionName 1.0.0`, `versionCode 10000`)
- [x] Signing separado y seguro (upload key fuera del repo)
- [x] AAB release firmado verificado (`jar verified.`)
- [x] Manifest/red (INTERNET, cleartext off, backups excluyen tokens)
- [x] Build/tests/lint verdes (según `evidence.md`)
- [x] COST-001 documentado
- [x] Gate humano de publicacion definido
- [ ] Store listing final (icono/screenshots/feature graphic, content rating, App Access, tester/reviewer credentials, release notes)
- [ ] Privacy Policy: URL real (`BLOCKED-PRIVACY-URL`) — pendiente humano, sin URL inventada
- [ ] Data Safety con valores reales (depende de la URL de privacidad)
- [ ] Subida a Play Internal/Closed/Production — NO publicar sin aprobacion humana explicita
