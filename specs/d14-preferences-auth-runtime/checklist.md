# CHECKLIST D14 — Preferences Auth Runtime

- [x] Spec creada con contrato backend confirmado (sin suposiciones)
- [x] Plan creado con fases, dependencias y gate humano
- [x] Tasks D14-T01..T08 creadas
- [x] Decisión humana registrada: pets bool, catálogos código, budget entero, university fuera de Preferences, caché restrictiva, PUT sin GET posterior
- [x] D14-T02 Logging `PreferencesFlow` DEBUG sin PII
- [x] D14-T03 Error mapping HTTP→Domain y caché restrictiva
- [x] D14-T03-bis `@Serializable` en DTOs (root cause real del error genérico)
- [x] D14-T04 Contrato de escritura exacto (tipos/códigos/rangos)
- [x] D14-T05 `missing` del 422 y orientación a university
- [x] D14-T06 Guardado sin GET posterior
- [x] D14-T07 Tests nuevos sin reducir existentes (417 totales)
- [x] D14-T08 Gates locales + PR separado (CI remoto pendiente)
- [ ] Runtime evidence (GET 200 / PUT 200 / recommendations 200 o missing documentado)
- [ ] Aprobación humana antes de merge
