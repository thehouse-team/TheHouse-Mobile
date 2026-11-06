# NV-27 - TASKS

| Campo | Valor |
|---|---|
| Spec | NV-27 Backend Contract Integration |
| STATUS | PARTIAL (D3 ejecutada) |
| AUTHORIZATION | D3 autorizada por orden humana explícita 2026-11-06 |

- [ ] NV-27-T01 - Transcribir register 201 {email,password} (M-MOB-044).
- [ ] NV-27-T02 - Transcribir login 200 y logout 204 (M-MOB-044).
- [ ] NV-27-T03 - Verificar properties con prueba real (o BLOCKED parcial).
- [ ] NV-27-T04 - Verificar universities con prueba real (o BLOCKED parcial).
- [ ] NV-27-T05 - Verificar health con prueba real.
- [x] NV-27-T06 - (D3) Implementar POST /api/v1/auth/refresh (antes BLOCKED, desbloqueado por contrato confirmado D3).
- [x] NV-27-T07 - (D3) Implementar GET /api/v1/users/me (antes BLOCKED, desbloqueado por contrato confirmado D3).
- [x] NV-27-D3-01 - Integrar refresh 200 con rotación (siempre reemplaza anterior).
- [x] NV-27-D3-02 - Integrar /me 200 solo id/email/createdAt (sin inventar campos).
