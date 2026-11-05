# NV-04 — AUTH + ONBOARDING

> ## ⛔ NO EJECUTAR
>
> Esta spec está en estado **NO AUTORIZADO**. No implementar pantallas, endpoints, almacenamiento de tokens ni marcar tasks sin autorización humana explícita.

| Campo | Valor |
|---|---|
| Spec ID | NV-04 |
| Nombre | Auth + Onboarding |
| Versión | 1.0.0 |
| Fecha | 2026-11-04 |
| Estado | ⛔ NO EJECUTAR |
| Autorización | PENDIENTE |
| Alcance | U2 |
| Dependencia | NV-01 Foundation (✅ ejecutada) ; NV-03 Release/Google Play se autoriza antes en el orden U2 |
| Módulo | `:app` |

> La numeración NV de Mobile es independiente de la numeración NV del backend, aunque ambos usen NV-04.

## 1. Contexto

NV-01 Foundation está ejecutada y validada (30/30 tasks): boundaries `Presentation → Domain ← Data`, Manual DI con `AppContainer` + `TheHouseApplication`, modelo de errores, red base Retrofit/OkHttp/Kotlin Serialization, DataStore para preferencias no sensibles, Coil y tests de foundation.

El backend TheHouse (.NET 10 en Azure Container Apps) expone una URL pública estable:

```text
https://ca-thehouse-u2.grayisland-cb722b5f.eastus2.azurecontainerapps.io
```

Contrato backend **confirmado** (no se inventan endpoints adicionales):

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/logout
GET  /api/v1/properties
GET  /api/v1/properties/{id}
GET  /api/v1/universities
GET  /api/v1/universities/{id}
GET  /health/live
GET  /health/ready
```

**NO confirmados** por el backend:

```text
POST /api/v1/auth/refresh
GET  /api/v1/users/me
```

Los flujos que dependan de estos endpoints se documentan como diseño y quedan `BLOCKED-BACKEND` hasta que exista evidencia del contrato real.

## 2. Objetivo

Entregar, dentro del alcance U2, el flujo completo de acceso a la app: registro, login, logout, persistencia de sesión segura, onboarding de primer ingreso y pantalla de inicio provisional, con arquitectura MVVM + UDF, accesibilidad y calidad verificable.

## 3. Alcance

- Flujo de entrada: `_inicio_` muestra la pantalla correspondiente según sesión válida.
- Registro (RF-01): nombre, correo, contraseña, aceptar términos y aceptar aviso de privacidad.
- Login (RF-02): correo y contraseña.
- Logout: backend + limpieza local garantizada.
- Onboarding de primer ingreso: 3 pantallas de introducción, botón `Omitir`, bienvenida personalizada (condicionada a `/users/me`), inicio.
- `onboardingCompleted` por usuario con DataStore (dato no sensible).
- Sesión persistente: access token en memoria, refresh token cifrado (AES-GCM, Android Keystore).
- Caché HTTP con OkHttp disk cache (`Cache-Control`, `ETag`).
- Caché de auth: **no se inventa caching para auth**.
- Pruebas unitarias, de integración (MockWebServer compatible con OkHttp 5), de seguridad y UI (Compose Test).

## 4. Fuera de alcance

- Google Sign-In (`Posterior`).
- Renovación automática implementada mientras `/auth/refresh` no esté confirmado (`BLOCKED-BACKEND`).
- Identificación de usuario remota mientras `/users/me` no esté confirmado (`BLOCKED-BACKEND`).
- Room/KSP (`Posterior`).
- Push notifications, eliminación de cuenta, moderación, ML, IA generativa (`Posterior`, cada una con spec futura).
- `FLAG_SECURE` (PROPUESTO, solo requisito obligatorio si el humano lo aprueba).

## 5. Requisitos

### NV04-REQ-001 — Arranque por sesión

`Inicio`: si existe sesión válida → pantalla Inicio. Si no → Login con opción "Crear cuenta".

### NV04-REQ-002 — Registro (RF-01)

Campos: nombre, correo, contraseña, aceptar términos, aceptar aviso de privacidad con enlace `<URL_AVISO_PRIVACIDAD>`. Backend: `POST /api/v1/auth/register`. No asumir login automático si el contrato de respuesta no lo permite; reflejar el comportamiento real del backend en el flujo.

### NV04-REQ-003 — Login (RF-02)

Campos: correo, contraseña. Backend: `POST /api/v1/auth/login`. Google Sign-In: `Posterior`.

### NV04-REQ-004 — Onboarding de primer ingreso

1. Introducción 3 pantallas; 2. botón `Omitir`; 3. bienvenida personalizada; 4. inicio. Guardar `onboardingCompleted` por usuario mediante DataStore (no es secreto). La bienvenida personalizada requiere identificar al usuario vía `GET /api/v1/users/me`; si no existe: `BLOCKED-BACKEND` (no inventar respuesta).

### NV04-REQ-005 — Inicio provisional

Saludo y botón `Cerrar sesión`. Backend: `POST /api/v1/auth/logout`. Logout intenta backend, limpia siempre el estado local, borra tokens y cancela trabajos asociados. Sin red: logout local inmediato; reintento remoto solo si la arquitectura lo permite sin conservar secretos.

### NV04-REQ-006 — Sesión y seguridad

- Access token solo en memoria.
- Refresh token cifrado: AES-GCM con Android Keystore; preferir implementación directa con Keystore; cualquier librería adicional debe demostrar compatibilidad con Kotlin `2.2.10`; si no, se evalúa su versión más cercana compatible (precedente Coil M-MOB-014) o queda `BLOCKED`.
- NO usar `EncryptedSharedPreferences`.
- Renovación (diseño; implementación `BLOCKED-BACKEND`): OkHttp `Authenticator`, refresh single-flight con `Mutex`, máximo una renovación concurrente, fallo → logout local.
- Nunca: tokens o contraseñas en logs; credenciales en backups (excluir almacenamiento sensible en `data_extraction_rules`).

### NV04-REQ-007 — Arquitectura

- Domain: modelos y casos de uso `Register`, `Login`, `Logout`, `ObserveSession`; `GetMe` solo cuando el backend lo soporte.
- Data: DTO, API, `AuthRepository`, token storage, caché HTTP con OkHttp disk cache (respeta `Cache-Control` y `ETag`), autenticación HTTP.
- Presentation: ViewModels, Compose (Material 3), UDF, `AppRoute`.
- DI: `AppContainer`. Room/KSP: `Posterior`.

### NV04-REQ-008 — Referencia visual (maqueta)

Usar SOLO como referencia visual los 5 archivos HTML en la carpeta de prototipos del humano (`00-onboarding.html`, `01-v1-bienvenida.html`, `02-iniciar-sesion.html`, `03-crear-cuenta.html`, `07-inicio-estudiante.html`): únicamente textos, jerarquía, orden, colores y composición visual. No copiar su arquitectura ni JavaScript.

### NV04-REQ-009 — Accesibilidad

Controles táctiles >= 44dp; `semantics`/`contentDescription` donde corresponda; contraste y textos legibles.

### NV04-REQ-010 — Pruebas

- Unitarias: ViewModels, validadores, UseCases, dispatchers controlados.
- Integración (MockWebServer compatible con OkHttp 5): register, login, logout, errores Problem Details; refresh solo cuando el backend lo soporte.
- Seguridad: tokens ausentes de logs, almacenamiento cifrado, logout elimina sesión, backup excluye almacenamiento sensible, cleartext deshabilitado.
- UI (Compose Test): Login (obligatorio); Register opcional si no aumenta excesivamente el alcance.
- Toda dependencia nueva debe ser compatible con Kotlin `2.2.10`; si no, `BLOCKED`.

### NV04-REQ-011 — Red segura

`android.permission.INTERNET` como único permiso; tráfico en claro deshabilitado; `network_security_config` restringido al dominio del backend.

## 6. Criterios de aceptación

U2 se considera completa cuando:

- `register` funciona contra el backend real.
- `login` funciona contra el backend real.
- `logout` funciona (backend + limpieza local verificada).
- `onboarding` funciona y `onboardingCompleted` persiste por usuario.
- La sesión persiste de forma segura (access en memoria, refresh cifrado en Keystore).
- El flujo dependiente de `/auth/refresh` y `/users/me` queda resuelto por contrato backend real.
- `assembleDebug` verde; `testDebugUnitTest` verde; `lintDebug` verde.
- AAB release firmado disponible (coordinado con NV-03).

Si el backend sigue sin `/auth/refresh` o `/users/me`: no falsificar aceptación → `BLOCKED-BACKEND`.

## 7. Riesgos

| Riesgo | Mitigación |
|---|---|
| Endpoints `/auth/refresh` y `/users/me` sin confirmar | Tasks dependientes marcadas `BLOCKED-BACKEND`; no implementar alrededor de endpoints inexistentes |
| Librerías de cifrado incompatibles con Kotlin 2.2.10 (precedente Coil M-MOB-014) | Implementación directa con Android Keystore; compatibilidad de dependencias evaluada antes de agregar |
| Desalineación con maqueta | Revisión respecto a los 5 archivos de referencia visual |
| Filtración de tokens (logs, backups) | Regla de logging seguro, `data_extraction_rules`, tests de seguridad |

## 8. COST-001

Servicios externos previstos: ninguno nuevo. Backend Azure ya existe fuera de alcance Mobile. Google Sign-In y futuras integraciones evaluarán COST-001 en su propia spec. Objetivo `$0` se mantiene.

## 9. Trazabilidad

| Requisito | Fase |
|---|---|
| NV04-REQ-001..003, 005 | F1 (Domain/Data), F3 (UI/UX) |
| NV04-REQ-004 | F1, F3 |
| NV04-REQ-006 | F2 (Sesión) |
| NV04-REQ-007 | F1, F2, F3 |
| NV04-REQ-008..009 | F3 |
| NV04-REQ-010 | F4 |
| NV04-REQ-011 | F2 |

## 10. Evidencia esperada

Registros reales en `specs/nv-04-auth-onboarding/evidence.md`: build, tests, lint, verificaciones de seguridad y resultados contra el backend real.
