# CONSTITUCIÓN DE THEHOUSE MOBILE

| Campo | Valor |
|---|---|
| Documento | Constitution Mobile |
| Versión | 1.0.0 |
| Fecha | 2026-10-31 |
| Autoridad | Máxima autoridad técnica y de gobernanza de `C:\THEHOUSE\Mobile` |
| Estado | VIGENTE |
| Aplicación | Aplicación Android nativa TheHouse |

Esta Constitución gobierna todas las decisiones, especificaciones, planes, tareas, implementaciones y validaciones de TheHouse Mobile. Cualquier documento de menor jerarquía que la contradiga es inválido y debe corregirse antes de continuar.

---

## 1. Propósito

Definir las reglas obligatorias para construir TheHouse Mobile de forma segura, trazable, mantenible, verificable y económicamente sostenible, preservando una separación clara entre gobernanza, arquitectura, implementación y validación.

El proyecto es académico, pero se desarrolla con estándares profesionales compatibles con una futura publicación en Google Play.

## 2. Alcance

Esta Constitución aplica exclusivamente a la aplicación Android nativa ubicada en `C:\THEHOUSE\Mobile`.

Incluye:

- Código Kotlin y Jetpack Compose.
- Configuración Gradle del proyecto Android.
- Arquitectura, datos, red, persistencia, seguridad y pruebas.
- Integraciones externas presentes o futuras.
- Documentación SDD, agentes y skills del repositorio Mobile.
- Build, validación, versionamiento y preparación de release.

No incluye backend, web, infraestructura ajena al repositorio Mobile ni funcionalidades de negocio no autorizadas.

## 3. Jerarquía normativa

El orden de autoridad es:

```text
constitution.md
    ↓
AGENTS.md
    ↓
memoria.md
    ↓
spec activa
    ↓
plan activo
    ↓
task activo
```

Una instrucción humana puede autorizar trabajo, pero no puede convertir en válida una implementación que contradiga esta Constitución sin seguir el procedimiento de modificación correspondiente.

Cuando exista conflicto:

1. Prevalece la Constitución.
2. Luego `AGENTS.md`.
3. Luego `memoria.md`.
4. Luego la spec aprobada.
5. Luego el plan aprobado.
6. Luego la task aprobada.

## 4. Spec Driven Development

TheHouse Mobile adopta Spec Driven Development como método obligatorio.

> Ninguna funcionalidad puede implementarse sin spec, plan y task aprobados.

Toda spec debe describir problema, alcance, requisitos, criterios de aceptación, riesgos, impacto de seguridad, impacto económico y evidencia esperada.

Todo plan debe traducir requisitos en fases verificables. Toda task debe ser atómica, trazable y ejecutable únicamente dentro de la spec y el plan aprobados.

## 5. Specs independientes

> Cada incremento funcional o arquitectónico relevante tendrá una spec independiente.

Una spec no puede ejecutar tareas de otra spec. No se permite ampliar silenciosamente el alcance de una task. Los cambios de alcance requieren actualización aprobada de spec y plan.

Las specs se almacenan en `specs/<id>/` con `spec.md`, `plan.md` y `task.md`.

## 6. Android nativo

TheHouse Mobile será una aplicación Android nativa. No se adoptarán frameworks multiplataforma para la aplicación móvil sin una spec de arquitectura aprobada y decisión humana.

La base tecnológica es:

- Android nativo.
- Kotlin.
- Jetpack Compose.
- Gradle.
- Un único módulo `:app` en la etapa inicial.

## 7. Kotlin

Kotlin es el lenguaje exclusivo de producción de la aplicación.

Reglas:

- Mantener Kotlin `2.2.10` hasta que una spec autorizada y una decisión humana aprueben un cambio de toolchain.
- No introducir una segunda versión Kotlin.
- Los plugins de Compose y Serialization deben usar la misma versión Kotlin existente.
- El código debe respetar el estilo oficial de Kotlin.
- No se permite lógica de negocio en archivos de infraestructura o configuración.

## 8. Jetpack Compose

Jetpack Compose es la tecnología de UI.

Reglas:

- Usar Material 3 y el Compose BOM aprobado.
- Preferir composables pequeños, stateless y reutilizables.
- Aplicar state hoisting.
- Mantener previews para componentes cuando sea viable.
- Asegurar accesibilidad, semántica y soporte de tamaños de pantalla.
- No agregar pantallas funcionales sin spec aprobada.

## 9. MVVM

Toda pantalla funcional seguirá MVVM:

- La UI observa estado.
- El ViewModel expone estado y atiende eventos.
- El ViewModel no contiene referencias directas a `Context` de Activity, Composable o vistas.
- La lógica de negocio se delega a casos de uso o repositorios según corresponda.
- La UI no toma decisiones de negocio.

## 10. Clean Architecture

La dirección conceptual de dependencias es:

```text
Presentation
      ↓
Domain
      ↑
Data
```

Reglas:

- Presentation conoce Domain.
- Data implementa contratos de Domain.
- Domain no depende de Android, Retrofit, Room, DataStore ni Firebase.
- La UI no consume Retrofit, Room, DataStore ni Firebase directamente.
- Inicialmente las boundaries se implementan mediante paquetes dentro de `:app`.
- No se crearán módulos Gradle `domain`, `data`, `presentation` o `core` sin una spec aprobada.
- La modularización futura requiere justificación, plan de migración y decisión humana.

## 11. Unidirectional Data Flow

El flujo obligatorio es:

```text
UI Event
   ↓
ViewModel
   ↓
Use Case / Repository
   ↓
StateFlow
   ↓
UI State
   ↓
Compose
```

Se prohíbe:

- Lógica de negocio en Composables.
- Llamadas de red desde Composables.
- Acceso a Room desde Composables.
- Estado mutable global.
- Eventos que salten la cadena de responsabilidad definida.

## 12. Coroutines y Flow

Concurrencia y reactividad se implementan con Coroutines y Flow.

Reglas:

- Los ViewModels usan `viewModelScope`.
- Las operaciones de I/O usan dispatchers adecuados.
- La cancelación debe propagarse correctamente.
- Preferir `StateFlow` para estado observable de UI.
- Evitar `GlobalScope`.
- No bloquear el hilo principal con I/O.
- Los tests futuros deberán usar dispatchers controlados.

## 13. Navigation

Navigation Compose es la base de navegación.

Reglas:

- Las rutas deben ser explícitas y tipadas cuando la spec lo determine.
- No se crearán pantallas ni destinos funcionales sin spec.
- La navegación no debe contener lógica de negocio.
- Los argumentos deben validarse.
- La foundation de navegación se definirá en NV-01.

## 14. Networking

La capa de red utilizará:

- Retrofit.
- OkHttp.
- Kotlin Serialization.
- Coroutines.

Reglas:

- La UI nunca llama Retrofit directamente.
- Los DTO no deben filtrarse a Presentation sin mapeo.
- Deben definirse timeouts, manejo de errores, cancelación y semántica HTTP.
- El logging seguro se habilita solo cuando corresponda; nunca se registran tokens, cookies, credenciales ni datos personales sensibles.
- No se crearán endpoints funcionales sin spec.

## 15. Persistencia local

La persistencia local se apoyará en:

- Room para datos estructurados.
- DataStore para preferencias y estado simple.

Reglas:

- No crear entidades, DAOs o bases de datos sin spec funcional que defina el modelo.
- No configurar KSP ni Room Compiler hasta que exista persistencia Room real.
- No almacenar secretos en DataStore sin una estrategia segura aprobada.
- Los repositorios encapsulan el acceso a persistencia.

## 16. Offline y sincronización

La arquitectura offline-first se definirá cuando las entidades reales y los casos de uso estén aprobados.

Reglas:

- Room y WorkManager son las herramientas previstas.
- No inventar sincronización, conflictos, reintentos ni políticas de caché antes de conocer el dominio.
- Toda sincronización debe ser idempotente, observable y tolerante a fallos.
- La sync se especificará en una spec funcional o en NV-01/NV-18 según corresponda.

## 17. Seguridad móvil

Toda implementación debe considerar OWASP Mobile y buenas prácticas Android.

Reglas:

- Mínimo privilegio.
- Validación de entradas.
- Comunicaciones mediante HTTPS.
- No confiar en el cliente para autorización.
- Almacenamiento seguro de tokens.
- No loggear información sensible.
- Ofuscar/minimizar datos cuando corresponda.
- Considerar capturas de pantalla y overlays en pantallas sensibles.
- Revisar dependencias y configuraciones de red.

## 18. Permisos

Los permisos Android deben ser mínimos, justificados y solicitados en contexto.

Reglas:

- No declarar permisos preventivos.
- Justificar cada permiso en la spec.
- Manejar rechazo y denegación permanente.
- No solicitar permisos sin una acción del usuario cuando sea evitable.
- La disclosure de permisos debe reflejarse en Google Play cuando aplique.

## 19. Sesión persistente

La arquitectura objetivo es:

```text
sesión persistente
    ↓
logout explícito
```

La sesión puede persistir entre aperturas de la aplicación hasta que el usuario cierre sesión o exista una invalidación de seguridad aprobada.

Reglas:

- Los tokens sensibles no se almacenarán en texto plano.
- La estrategia concreta se definirá en la spec funcional de Login/Logout.
- No implementar login, logout funcional ni almacenamiento de sesión en esta etapa.

## 20. Logout

El logout será explícito y seguro.

Debe:

- Invalidar el estado de autenticación local.
- Limpiar tokens y datos sensibles de sesión.
- Cancelar trabajo asociado cuando corresponda.
- Evitar que datos de una cuenta queden visibles para otra.
- Coordinarse con backend cuando la spec lo defina.

No se implementa todavía.

## 21. Push Notifications

Firebase Cloud Messaging será la base prevista para notificaciones push.

Estado actual: no configurado.

Reglas:

- No agregar `google-services.json`, plugin de Google Services, Firebase project ni Firebase Messaging sin spec aprobada.
- La integración requerirá verificar la versión vigente de Firebase Android BoM.
- Referencia técnica actual no instalada: Firebase Android BoM `35.1.0`.
- El tratamiento de notificaciones, tokens y permisos tendrá spec funcional independiente.

## 22. Maps

Google Maps será una integración futura para búsqueda y geolocalización.

Estado actual: no configurado.

Reglas:

- No agregar API keys, Maps SDK, Maps Compose ni secretos en manifest.
- Referencia técnica actual no instalada: Maps Compose `8.4.0`.
- La integración debe usar secrets/configuration segura.
- La integración debe cumplir COST-001 antes de ser aprobada.
- Sin spec y sin evaluación de costo, la funcionalidad queda BLOCKED.

## 23. Privacidad

TheHouse Mobile aplicará minimización de datos y transparencia.

Reglas:

- Recoger solo datos necesarios.
- Explicar finalidad y retención.
- Proveer aviso de privacidad antes de publicar.
- Respetar consentimientos y revocaciones.
- No incluir datos personales en logs, analytics ni reportes de crash sin base y protección.
- Documentar el manejo de datos para Google Play Data Safety.

## 24. Eliminar cuenta

La eliminación de cuenta será una capacidad obligatoria para la publicación cuando el servicio permita cuentas.

Debe:

- Ser accesible desde la aplicación.
- Explicar consecuencias.
- Eliminar o anonimizar datos según política aprobada.
- Coordinarse con backend.
- Tener spec funcional independiente.

No se implementa todavía.

## 25. Moderación de contenido

La moderación de contenido será considerada cuando existan reseñas, chat, propiedades u otro contenido generado por usuarios.

La solución debe:

- Definir políticas y canales de reporte.
- Evaluar herramientas contra COST-001.
- Proteger privacidad.
- Tener spec funcional independiente.

No se implementa todavía.

## 26. Machine Learning e IA generativa

La separación obligatoria es:

```text
Machine Learning
→ calcula resultado/compatibilidad

Generative AI
→ explica resultado
```

Reglas:

- No implementar ML ni IA generativa sin spec aprobada.
- El Mobile no debe contener secretos de proveedores de IA.
- Las llamadas sensibles deben gestionarse mediante backend cuando corresponda.
- Toda integración debe evaluar COST-001, privacidad, sesgos y abuso.
- Smart Match, ML e IA generativa pertenecen a specs futuras.

## 27. Testing

La estrategia objetivo incluye:

- Unit tests.
- ViewModel tests.
- Repository tests.
- Compose UI tests.
- Instrumented tests.
- Integration tests.
- Security tests.

Reglas:

- No crear pruebas artificiales para ocultar fallos.
- Los tests deben verificar comportamiento y requisitos.
- Las pruebas futuras se planifican en NV-02.
- No se reduce cobertura o calidad para forzar un build.

## 28. DevSecOps

La seguridad y calidad deben integrarse en el ciclo de desarrollo.

Herramientas previstas:

- Android Lint.
- Detekt.
- ktlint.
- Semgrep.
- SonarCloud cuando sea viable.
- Dependency scanning.
- MobSF cuando sea viable.
- GitHub Actions.

Estado actual: no instaladas. Su adopción requiere NV-02 autorizada.

## 29. CI/CD

CI/CD se implementará en una spec aprobada.

Reglas:

- Pipelines reproducibles.
- Secretos en el sistema de secretos del CI, nunca en el repositorio.
- Quality gates antes de release.
- Evidencia de build, tests, lint y análisis.
- No publicar artefactos sin autorización humana.

## 30. Versionamiento

El versionamiento de la aplicación seguirá:

- `versionCode` entero, monotónicamente creciente.
- `versionName` legible y coherente con release.
- Releases documentados.
- Cambios de versión coordinados con la spec de release.

Versión base actual: `versionCode = 1`, `versionName = "1.0"`.

## 31. AAB

Google Play requiere Android App Bundle.

Reglas:

- El artefacto de publicación será `.aab`.
- El build debug no es artefacto de publicación.
- El signing de release se definirá en NV-03.
- No se generará ni cargará un AAB de release sin autorización.

## 32. Google Play

La preparación para Google Play incluye:

- Ficha de aplicación.
- Política de privacidad.
- Data Safety.
- Disclosure de permisos.
- Screenshots y assets.
- Signing y AAB.
- Revisión de COST-001.

Estado actual: no configurado y no publicado. Requiere NV-03 autorizada.

## 33. Gestión de secretos

Nunca se versionarán:

- API keys.
- Passwords.
- JWT secrets.
- Signing passwords.
- Keystore privado.
- Service-account JSON.
- Secretos de `google-services`.
- Cualquier credencial de producción.

> Ningún secreto podrá almacenarse dentro del repositorio.

Los secretos se inyectarán mediante variables de entorno, archivos locales ignorados o el almacén de secretos del CI, según la spec correspondiente.

## 34. COST-001

> Todo servicio externo deberá evaluarse contra COST-001.

COST-001 exige:

```text
Costo obligatorio objetivo = $0
```

Para cualquier Firebase, Maps, API externa, servicio cloud, ML, IA generativa, moderación, analytics o servicio equivalente, la spec debe documentar:

- Free tier.
- Límites.
- Si requiere tarjeta.
- Riesgo de cobro.
- Mecanismo para impedir sobrecostos.
- Alternativa gratuita.

Si no puede demostrarse viabilidad de costo $0:

```text
BLOCKED
```

hasta decisión humana.

Ningún agente puede asumir que un servicio gratuito seguirá siendo gratuito sin evidencia y mitigaciones.

## 35. Agentes IA

Todo agente que opere en este repositorio debe:

- Respetar esta Constitución.
- Leer `AGENTS.md` y `memoria.md`.
- Declarar spec, plan, task y estado de autorización antes de implementar.
- Mantenerse dentro del alcance autorizado.
- Reportar bloqueos sin inventar aprobaciones.
- No ejecutar specs no autorizadas.
- No crear secretos, funcionalidades o integraciones no aprobadas.

## 36. Prohibiciones

Queda prohibido, salvo spec aprobada que indique lo contrario:

- Implementar registro, login, logout funcional, usuarios, propiedades, edificios, habitaciones, universidades, mapas funcionales, favoritos, visitas, chat, rentas, reseñas, notificaciones funcionales, Smart Match, ML o IA generativa.
- Recrear el proyecto Android o agregar módulos Gradle no aprobados.
- Modificar el toolchain sin autorización.
- Configurar Firebase, Maps, Google Play o servicios externos sin spec.
- Introducir Hilt, Koin u otro framework de DI sin decisión de NV-01.
- Configurar KSP o Room Compiler antes de persistencia Room real.
- Crear pantallas funcionales o APIs funcionales.
- Versionar secretos.
- Ejecutar `git init`, commits o push en tareas que no lo autoricen.
- Publicar artefactos sin autorización humana.

## 37. Modificación de la Constitución

Esta Constitución solo puede modificarse mediante:

1. Propuesta escrita.
2. Justificación técnica y de impacto.
3. Evaluación de seguridad y COST-001 cuando aplique.
4. Revisión de trazabilidad e impacto en specs activas.
5. Autorización humana explícita.
6. Incremento de versión.
7. Registro en `memoria.md`.

Ningún agente puede modificar esta Constitución por iniciativa propia ni para desbloquear una implementación.

---

## Reglas constitucionales destacadas

> Ninguna funcionalidad puede implementarse sin spec, plan y task aprobados.

> Cada incremento funcional o arquitectónico relevante tendrá una spec independiente.

> Los Composables no contendrán lógica de negocio ni acceso directo a infraestructura.

> Ningún secreto podrá almacenarse dentro del repositorio.

> Todo servicio externo deberá evaluarse contra COST-001.

## Declaración de vigencia

Esta Constitución entra en vigor en la fecha indicada y subordina cualquier práctica, documento, agente, skill, spec, plan o task del repositorio Mobile.
