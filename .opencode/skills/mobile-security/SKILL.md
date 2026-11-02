---
name: mobile-security
description: Valida permisos, secretos, red, tokens, almacenamiento, privacidad, logging y controles OWASP Mobile.
---

# Mobile Security

## Nombre

mobile-security

## Objetivo

Validar que TheHouse Mobile proteja datos, credenciales y superficies de ataque conforme a OWASP Mobile y las reglas de la Constitución.

## Cuándo utilizarlo

- Nuevos permisos.
- Manejo de sesión o tokens.
- Almacenamiento local.
- Configuración de red.
- Nuevas integraciones externas.
- Revisión de privacidad y logging.

## Entradas

- Spec, plan y task activos.
- Permisos y manifest.
- Flujos de autenticación y almacenamiento.
- Configuración de red.
- Dependencias y servicios externos.

## Procedimiento

1. Confirmar autorización.
2. Identificar activos y datos sensibles.
3. Revisar permisos y justificación.
4. Revisar transmisión y network security.
5. Revisar almacenamiento de tokens.
6. Revisar logs y reportes.
7. Revisar autenticación y estado de sesión.
8. Revisar exposición de componentes Android.
9. Revisar privacidad y minimización.
10. Revisar capturas/overlays cuando aplique.
11. Revisar dependencias y secretos.
12. Emitir hallazgos con severidad.

## Reglas

- Ningún secreto en el repositorio.
- Ningún token en logs.
- HTTPS obligatorio para datos sensibles.
- Mínimo privilegio.
- Tokens no en texto plano.
- Autorización real en backend, no en cliente.
- No incluir PII en analytics o crash reports sin base.
- Permisos justificados y solicitados en contexto.
- Aplicar controles OWASP Mobile.
- Evaluar COST-001 en servicios externos.

## Restricciones

- No agregar permisos, keys o servicios.
- No deshabilitar controles de seguridad para facilitar desarrollo.
- No almacenar credenciales de forma insegura.
- No implementar autenticación sin spec.
- No exponer componentes sin revisión.

## Criterios de finalización

- Hallazgos clasificados.
- Controles y mitigaciones definidos.
- Secretos y permisos verificados.
- Privacidad y logging revisados.
- Dictamen y riesgo residual documentados.
