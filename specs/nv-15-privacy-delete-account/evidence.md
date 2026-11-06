# NV-15 — EVIDENCIA

## FASE D2 — Auditoría privacidad (2026-11-06, branch feature/privacy-landing-integration)

### Verificación URL_AVISO_PRIVACIDAD: NO VÁLIDA → BLOCKED-PRIVACY-URL se mantiene
- Landing auditada: https://lively-llama-5185d1.netlify.app (HTTP 200).
- Footer contiene solo anclas `#privacidad` y `#terminos` SIN secciones/contenido real (no existen `id="privacidad"`/`id="terminos"` en el HTML).
- No es HTTPS pública estable con contenido real apto para Google Play; es redirección interna rota. NO se inventó URL.
- Mobile: NO se enlazó “Aviso de privacidad” (sin placeholder, sin cambio funcional falso, sin WebView).

### ACTUAL vs PLANNED (sin inventar tratamientos)
| Área | ACTUAL (evidencia real) | PLANNED / BLOCKED |
|---|---|---|
| Auth | register 201, login 200, logout 204 (NV-04) | refresh/users/me BLOCKED (NV-27) |
| Perfil | Sin perfil funcional (BLOCKED) | NV-27 pendiente |
| Favoritos/visitas/chat | No implementados | NV-09/NV-10/NV-11 PLANNED |
| Almacenamiento local | DataStore prefs (onboarding) + refresh token cifrado AES-GCM/Keystore excluido de backup (NV-04) | Sin base de datos de negocio |
| Tokens | Access solo memoria; refresh cifrado | Sin renovación automática (BLOCKED) |
| Analytics | No implementado | NV-23 PLANNED |
| Push | No implementado (FCM sin configurar) | NV-14 PLANNED |
| ML/IA | No implementado | NV-17 PLANNED |
| Delete account | NO implementado | NV-15-T03/T04 PLANNED; contrato BLOCKED |
| Retención/eliminación | Solo limpieza por logout (204) | Sin política de retención/TAFLIGHT |
| Contacto | No publicado | Sin contacto real en landing |
| Data Safety Play | Sin declaraciones completas | NV-03 BLOCKED-PRIVACY-URL; sin URL no se declara |

### Hechos reales preservados
- register 201 `{email,password}`, login 200, logout 204, tests 41/41, AAB signed `jar verified.`, versionName 1.0.0 / versionCode 10000.
- BLOCKED: `/auth/refresh`, `/users/me`, BLOCKED-PRIVACY-URL (se mantiene).
