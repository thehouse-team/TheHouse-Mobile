# VERSIONING.md — THEHOUSE MOBILE

| Campo | Valor |
|---|---|
| Documento | Versionamiento Mobile |
| Versión | 1.0.0 |
| Fecha | 2026-11-04 |
| Estado | VIGENTE (definición; sin tags creados) |
| Spec responsable | NV-03 Release / Google Play |

## 1. Convención de versión

```text
versionName = MAJOR.MINOR.PATCH
versionCode = MAJOR*10000 + MINOR*100 + PATCH
```

Ejemplo: `1.0.0 → 10000`.

Incremento necesario para publicar:

- **MAJOR:** cambios incompatibles de funcionalidad o modelo de datos.
- **MINOR:** nuevas funcionalidades de negocio (specs funcionales).
- **PATCH:** correcciones y ajustes sin nuevas capacidades.

`versionCode` es entero, monotónicamente creciente y único por release.

## 2. Tag

```text
mobile-v<versionName>
```

Ejemplo: `mobile-v1.0.0`.

Los tags Git se crean solo con autorización humana explícita y en coordinación con NV-03. No se crean tags en tareas de documentación.

## 3. Registro de releases

| Fecha | versionName | versionCode | Pista | Resultado |
|---|---|---|---|---|
| 2026-11-04 | 1.0.0 | 10000 | interna (U2) | AAB generado en build U2 |
| 2026-11-05 | 1.0.0 | 10000 | interna (U2) | AAB re-firmado por el autorizador: `jarsigner -verify` → `jar verified.` (CN=TheHouse Team, RSA 2048). Sin subida a Play |

## 4. Reglas

- No marcar un release sin evidencia real de build, tests, lint y AAB firmado.
- No asignar `versionCode` arbitrario fuera de la fórmula.
- Los cambios de esta política requieren actualización de spec y decisión humana.
