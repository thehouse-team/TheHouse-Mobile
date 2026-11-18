# NV-16 — CHECKLIST

- [x] Flujos clasificados (cacheable reads / online-only / sin sync inventado)
- [x] Cache local reutilizando infraestructura existente (sin segunda base Room)
- [x] Cache-first + network refresh con stale/offline y sin éxito falso
- [x] UX mínima "Sin conexión" / "Mostrando información guardada" sin rediseño
- [x] Logout/delete account limpian cache privada; usuario B no ve datos de A
- [x] No se persisten JWT/refresh/FCM/agreement codes/qrToken/secretos
- [x] Build/tests/lint verdes con evidencia real (runtime ADB no disponible)