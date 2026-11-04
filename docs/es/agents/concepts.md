# Conceptos y propiedad de recursos

Ámbito: Desarrollo. La [arquitectura central](https://raw.githubusercontent.com/lambdawalker/design.attestra/main/architecture.md)
conserva los requisitos del sistema.

## Límites

`:auth` posee UI, controladores, almacenamiento Android y adaptadores HTTP.
`:app` posee navegación, App Links, vinculación de entorno y privacidad de ventana.
Ambas variantes de la app comparten la captura real en `app/src/main`; no hay módulo mock local. El backend Go
posee autorización, evidencia/extracción duraderas y hechos de la cuenta.

## Correo y sesiones

`EmailFlowController` posee estado de UI y un mutex. `ProofGenerator` genera A y
su desafío S256; `AuthStorage` conserva la prueba pendiente y la sesión.
El enlace B usa A cuando coincide; si falta A, se requiere C + B.
`ReturnAuthApi` realiza intercambios HTTP, pero no posee el cliente ni navegación.
`AndroidPasskeyManager` delega en Credential Manager. Cancelar no registra una passkey;
el backend debe confirmar la operación.

## Autoridad de configuración y captura

`EnvironmentIndex` resuelve configuración pública confiable y solo conserva caché validada.
Un fallo no autoriza cambiar de servidor. `AppConfiguration` determina requisitos
y colocación de contraseña; pasos obligatorios no se pueden omitir. Deshabilitar
el registro de contraseña no elimina el acceso con una contraseña existente.

`uploading` acepta ambas caras; `finalizing` valida archivos; `ready` confirma esa
evidencia, no extracción ni identidad. Extracción `pending` espera, `complete`
precarga revisión y `failed` abre entrada manual vacía. Solo `reviewed` del servidor,
con los mismos valores y origen, confirma guardado. Guardar no verifica identidad.

## Recursos y ciclo de vida

| Recurso | Propietario | Limpieza y límites |
| --- | --- | --- |
| Clientes HTTP | Anfitrión o `EmailOnboardingHost` | Su creador los cierra; los adaptadores API no |
| `CaptureController` | Anfitrión, retenido por cuenta/backend | `CaptureHost` lo cierra al salir; `close()` borra JPEG pendientes |
| Arreglos JPEG | El callback transfiere al controlador | Solo reintento en memoria; se borran después de uso/salida; no reutilizar |
| Bitmaps del detector | `IdentityCamera` los consume | Codifica píxeles orientados y recicla resultados |
| Contraseña pendiente | Memoria Compose | Nunca estado guardado, almacenamiento, URLs o registros; se borra en transiciones pertinentes |
| Prueba/sesión | `SecureAuthStorage` | AES-GCM con clave Keystore por instalación; excluir preferencias sensibles de respaldos |
| Checkpoint | `SecureCaptureCheckpointStore` | Solo identificadores e intención, aislados por cuenta/backend; sin imágenes ni URLs firmadas |
| Corrutinas | Propietario Compose | Cancelación al salir no demuestra que la petición remota haya fallado |

Los controladores pertenecen al propietario de UI; no son servicios seguros para
consumidores multihilo arbitrarios. Llámelos desde su scope. Los mutex evitan
mutaciones solapadas, no autorizan cualquier patrón de concurrencia. No agregue
E/S bloqueante a UI. El almacenamiento usa commits síncronos; no lo recree en cada recomposición.

`clearLocalUser()` limpia esta instalación; no revoca sesiones remotas ni elimina
passkeys/evidencia del servidor. Aísle autenticación al cambiar la vinculación de backend/cuenta.


## Apariencia e idioma del anfitrión

Coloque `EmailOnboardingHost`, `CaptureHost` y las pantallas individuales dentro del
`MaterialTheme` de Material 3 del anfitrión. Las pantallas reutilizables heredan sus
colores, tipografía y formas; las alturas mínimas de acciones y celdas de código
permiten que crezca el texto. El espaciado sigue en píxeles independientes de la
densidad porque MaterialTheme no define una escala de espaciado.
`AttestraAuthTheme` es un tema de marca explícito para la app de ejemplo y las vistas
previas; los consumidores no necesitan aplicarlo.

Los recursos de UI siguen la configuración Android del anfitrión: inglés como idioma
de respaldo, alemán, español, francés, italiano, japonés, coreano, polaco, portugués,
ruso, ucraniano, chino simplificado y chino tradicional. Las APIs String de los
controladores y la política de contraseña conservan sus valores en inglés por
compatibilidad; las pantallas de la biblioteca resuelven los mensajes conocidos al
renderizar, incluidos sus argumentos. El texto del anfitrión o servicio que no coincida con un mensaje conocido se conserva.
Una UI personalizada que consume esos String directamente debe localizar sus mensajes.
Los avisos de captura siguen siendo contenido proporcionado por el servicio.
