# Selección y contratos de API

Ámbito: Desarrollo. Las declaraciones exactas están en el [inventario](api-inventory.md).
El [backend](https://raw.githubusercontent.com/lambdawalker/go.attestra.aws.auth/main/docs/agents/index.md)
posee la serialización HTTP. Fuente y pruebas prevalecen sobre prosa.

| API / paquete bajo `com.apexfission.android.auth` | Uso | Contrato |
| --- | --- | --- |
| `EmailOnboardingHost` · `email` | Autenticación e incorporación interactivas | Activity y Compose, origen confiable, política validada y callbacks de enlace. Posee cliente/almacenamiento/controlador recordados y cierra el cliente. ID predeterminado muestra indisponibilidad. |
| `parseVerificationLink` · `email` | Validar origen URI | HTTPS, host esperado, `/verify-email`, puerto por defecto/443; null si no coincide. El controlador valida además formato de pruebas. |
| `EmailFlowController` · `email` | UI de correo personalizada | Gateway y almacenamiento inyectados; prueba/reloj opcionales. Mutaciones suspendidas serializadas por mutex; guarda sesión solo tras éxito remoto. `screen` es estado Compose. Aceptar una petición no confirma correo. |
| `EmailVerificationApi` / `EmailHttpClient` · `email` | Transporte de correo | El llamador posee el cliente; origen HTTPS; sesión o `EmailApiError`. No reintente confirmación a ciegas sin reconciliación. |
| `ReturnAuthApi` · `passkey` | Acceso por correo/passkey/contraseña, refresh y estado | El llamador posee cliente y sesión. No registre contraseñas/pruebas. Finalización proviene del servidor. |
| `PasskeyRegistrationApi` / `AndroidPasskeyManager` · `passkey` | Passkeys de plataforma | Registro requiere bearer; el gestor necesita Activity y proveedor configurado. `PasskeyCancelled` y `PasskeyUnsupported` no son éxito. Confirme después en backend. |
| `EnvironmentIndex` · `discovery` | Descubrimiento público confiable | Cliente anónimo y caché pertenecen al llamador; claves separan URL de índice/entorno. Fallback limitado y validado. |
| `AppConfiguration` / `OnboardingPolicy` · `discovery` | Requisitos dinámicos | Valide política y entorno; ajustes inválidos bloquean admisión. `next()` usa hechos del servidor; omisiones locales no evitan pasos obligatorios. |
| `CaptureHost` / `CaptureController` · `capture` | Captura y revisión con estado | Retenga por cuenta/backend; anfitrión posee clientes y namespace; UI cierra controlador. `startCaptureOnEntry` y `requireReviewedPolicy` son false por defecto; el ejemplo live usa true y lleva divulgación revisada. |
| `CaptureApi` / `CaptureClients` · `capture` | Estado, mutaciones y PUT privado | Cliente de uploads separado y anónimo; valida origen/cabeceras S3. Bearer debe pertenecer a la cuenta; creador cierra ambos clientes. |
| `IdentityCamera` · `identity.capture` | Permiso y detector | Cámara/dispositivo, activos del modelo y entrada explícita del usuario. Transfiere dos arreglos JPEG; no emite decisión de identidad. |
| `SecureAuthStorage` · `email` | Datos por instalación | Cifrado Keystore; datos ilegibles aparecen ausentes. Limpieza local no es revocación. Excluya preferencias de respaldos. |

## Mutaciones de captura

`continueToCapture()` reanuda antes de crear evidencia. `capturedBoth(front, back)`
recibe propiedad de ambos arreglos; retiene uploads inconclusos solo en memoria.
`finalizeCapture()` persiste intención estable antes de pedir finalización;
`restore()` reconcilia respuestas inciertas. `retry()` solo cubre fallos elegibles;
no evade evidencia inválida/caducada. `cancel()` pide eliminación remota;
`close()` solo libera recursos locales.

`editCard()` cambia borrador local. `saveReview()` exige datos no vacíos, campos
acotados y origen admitido; envía revisión y consume confirmación del servidor.
Conflictos conservan cambios. La pantalla final requiere mismos campos/origen,
no `id_captured`. Diagnóstico solo debe incluir ID de captura, estado, revisión y HTTP;
nunca datos personales ni excepciones completas.

## UI y componentes internos

Las pantallas `ui.onboarding.*` reciben estado/callbacks; no implementan autenticación
por sí solas. `OnboardingBusinessActions` tiene hooks vacíos del catálogo, no de producción.
`IdentityDetails` y resultados antiguos son prototipos, no el modelo HTTP actual.
`CaptureHostContent`, `CardReviewForm`, pantallas de entrada y locales de configuración
son componentes internos; no contratos externos admitidos.

Los modelos de captura serializan transporte/estado. No fabrique registros `ready`
o `reviewed` para completar UI. Trate texto de tarjeta como no confiable; validación
de campos no acredita su veracidad. Consulte [captura](../../identity-capture.md).

## Revisión de documentos en desarrollo

La revisión de `CaptureHost` agrupa los datos personales y del documento. El menú
`CardData.documentType` es independiente de `CapturePolicy.documentType`.
Sus valores son `national_id`, `passport`, `drivers_license`, `residence_permit`,
`voter_id` y `other` (Otro documento); `documentName` conserva el título impreso o
personalizado. Los tipos desconocidos usan campos genéricos. Cambiar el tipo
conserva los demás valores. Los campos adicionales se muestran inicialmente si
contienen datos y siguen disponibles para cualquier tipo. Las fechas se mantienen
como aparecen impresas; los datos ausentes pueden quedar en blanco.

`CardData` incorpora las claves opcionales `document_type`, `document_name`,
`issuing_region`, `issuing_city` e `issuing_authority`. Los registros anteriores
usan valores vacíos. Son datos para revisión, no pruebas de autenticidad del
documento o de identidad. El contrato pertenece al [backend de captura](https://github.com/lambdawalker/go.appexfission.aws.card-capture).
Despliegue su revisión correspondiente en el host antes de enviar las nuevas
claves: los servidores anteriores con validación estricta las rechazan.
Actualizar Android no actualiza AWS.

La fila Expedido en abre un editor con país, estado opcional y ciudad opcional.
El selector de país ofrece sugerencias localizadas sin conexión. Los de estado y
ciudad buscan en una copia local de GeoNames (regiones administrativas y ciudades
con más de 15 000 habitantes o capitales), filtrada por los niveles superiores.
El catálogo se carga fuera del hilo principal. Los hashes de las fuentes, la
atribución CC BY 4.0 y el script de reducción reproducible se conservan en
`assets/geonames-NOTICE.txt` y `scripts/geography/build_catalogue.py`. Son sugerencias,
no una autoridad sobre límites políticos ni un directorio completo de municipios.
Los tres permiten entrada libre, incluso si el catálogo no se puede cargar.
Cambiar un nivel superior borra sus descendientes en el editor temporal.
Usar esta ubicación aplica la selección completa; cerrar el panel la descarta.
Los campos de jurisdicción revisados se protegen juntos frente a resultados OCR
posteriores. La autoridad emisora es un campo opcional separado; `origin_country`
del titular mantiene su política y validación independientes.

El formulario hereda el tema del host. No se conservan ni descargan imágenes del
documento para miniaturas de revisión; su ciclo de carga privada no cambia.
Volver a escanear usa la operación existente de recaptura con revisión.
Los escenarios sin conexión cubren la revisión y el editor de ubicación. Las
nuevas imágenes de referencia requieren renderizado, revisión y aceptación
explícita; la validación no sustituye imágenes aceptadas automáticamente.
