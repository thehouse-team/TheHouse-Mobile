# CHECKLIST D19.1 — Account Runtime Completion

| # | Requisito | Evidencia | Estado |
|---|---|---|---|
| 1 | Perfil carga con sesión válida (200) | D191ContractDeserializationTest + DTO espejo | TESTED / RUNTIME_PENDING |
| 2 | Nombre editable (PATCH 200) | Código y tests existentes preservados | TESTED / RUNTIME_PENDING |
| 3 | Universidad editable (PUT 204) | Código y tests existentes preservados | TESTED / RUNTIME_PENDING |
| 4 | Perfil persistente al regresar | Repository applyProfile + caché | TESTED / RUNTIME_PENDING |
| 5 | 401 definitivo → autenticación | ProfileViewModel RequireLogin preservado | TESTED |
| 6 | Preferencias 200 → formulario editable | D191ContractDeserializationTest | TESTED / RUNTIME_PENDING |
| 7 | PUT preferencias 200 → confirmación | PreferencesViewModel preservado | TESTED / RUNTIME_PENDING |
| 8 | Preferencias persistentes | Repository + DataStore preservados | TESTED / RUNTIME_PENDING |
| 9 | Sin error genérico tras HTTP 200 | Causa de deserialización corregida | TESTED / RUNTIME_PENDING |
| 10 | Smart Match 422 con `missing` visible | Código D16 preservado, sin bug propio | TESTED |
| 11 | Smart Match 200 con perfil completo | Flujo D16 preservado | TESTED / RUNTIME_PENDING |
| 12 | D16 preservado | Sin cambios en SmartMatchRepository/ViewModel | PASS |
| 13 | D18 preservado | Sin cambios en RoomDetail | PASS |
| 14 | Registro → privacidad → regreso sin sesión | Navegación cableada verificada | TESTED / RUNTIME_PENDING |
| 15 | Legal URL null sin afirmaciones falsas | Mensaje honesto + test | TESTED |
| 16 | Registro envía solo email/password | RegisterRequestDto verificado | TESTED |
| 17 | Aceptación legal solo con consentimiento | Código D19 preservado | TESTED |
| 18 | Arrendador 404 → activación | Factory + notLandlord verificados | TESTED / RUNTIME_PENDING |
| 19 | Arrendador 200 → dashboard | Código D12/D15 preservado | TESTED / RUNTIME_PENDING |
| 20 | `landlord_required` manejado | LandlordErrorMessages verificado | TESTED |
| 21 | Introducir código sin perfil landlord | Sin gating de rol (solo sección visual) | TESTED / RUNTIME_PENDING |
| 22 | Seis regresiones aprobadas preservadas | Sin cambios en header/drawer/logout/delete | PASS |
| 23 | Gates locales verdes (tests reales) | 524 / 0 failures / 0 errors | PASS |
| 24 | PR OPEN/MERGEABLE + CI SUCCESS | Pendiente de push y verificación remota | PENDING |
