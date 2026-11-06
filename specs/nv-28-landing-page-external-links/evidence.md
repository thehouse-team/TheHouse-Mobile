# NV-28 — EVIDENCIA

## FASE D2 — Inventario enlaces (2026-11-06, branch feature/privacy-landing-integration)

| Ítem | URL / valor | Estado |
|---|---|---|
| Landing principal | https://lively-llama-5185d1.netlify.app | Real, HTTP 200, contenido real |
| Prototipo Mobile | https://thehouse-prototipo.netlify.app/pantallas/00-onboarding.html | Real, HTTP 200 (referencia visual) |
| Privacy URL | Ancla `#privacidad` sin sección/contenido en Landing | NO válida — BLOCKED-PRIVACY-URL |
| Terms URL | Ancla `#terminos` sin sección/contenido en Landing | NO válida |
| Google Play URL | `PLAY_STORE_URL = "#descarga"` (JS) y `href="#"` en HTML | Placeholder — NO válida; Play NO declarado listo |

### Decisiones D2
- Sin tocar Landing desde Mobile; sin declarar Play listo sin URL real.
- Sin enlazar URLs no válidas en Mobile (sin placeholder, sin WebView, sin resolver Data).
- BLOCKED-PRIVACY-URL se mantiene. Pendiente humano: publicar aviso de privacidad y términos reales en Landing (HTTPS, estables, con contenido), y URL de Play tras publicación.

### Hechos reales preservados
- register 201, login 200, logout 204, tests 41/41, AAB signed `jar verified.`, versionName 1.0.0 / versionCode 10000.
