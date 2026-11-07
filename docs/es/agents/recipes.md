# Recetas de integración

Ámbito: Desarrollo. Use [inicio rápido](quickstart.md), no una versión Maven supuesta.

## Agregar captura

Reutilice las responsabilidades de [LiveCaptureFlow](../../../app/src/main/java/com/apexfission/android/attestra/auth/LiveCaptureFlow.kt):
retener clientes API/upload, aislar checkpoint por cuenta/backend, suministrar token
solo de esa cuenta, cerrar clientes y conectar `IdentityCamera` con `CaptureHost`.
Lleve `acceptedPolicy` desde Continuar y active `requireReviewedPolicy = true`.
Nunca acepte una política diferente sin conocimiento del usuario.
`allowNewCapture` refleja ajustes vigentes, no autorización del servidor.

Flujo esperado: Continuar → permiso/cámara → ambas fotos listas → subir/validar/OCR
durante revisión de fotos → confirmar → resultados editables o carga mientras termina
OCR → **Details saved** confirmado.
Cancelación, 401, consentimiento obsoleto, evidencia inválida y desconexión tienen
recuperación propia. Reanude fotos existentes sin reabrir cámara innecesariamente.

## Descubrimiento seguro

Use `environmentIndexClient()` con `EnvironmentCache`; cierre el cliente con su
propietario. Resuelva primero el entorno. Consulte
[EnvironmentViewModel](../../../app/src/main/java/com/apexfission/android/attestra/auth/EnvironmentViewModel.kt),
[BackendConfiguration](../../../app/src/main/java/com/apexfission/android/attestra/auth/BackendConfiguration.kt)
y [entornos](../../environment-discovery.md). Nunca use un cliente con bearer para el índice público.

## Personalizar UI

Use pantallas con estado bajo `ui/onboarding` dentro de `AttestraAuthTheme`.
Sus callbacks deben llamar al controlador, no navegar a éxito anticipadamente.
`OnboardingFrame` admite acciones primarias/secundarias y contenido; acciones omitidas
no se muestran. Callbacks de Compose se disparan en UI; lance funciones suspendidas
en el scope correspondiente. El [catálogo](../../../auth/src/main/java/com/apexfission/android/attestra/auth/ui/onboarding/OnboardingCatalog.kt)
es visual. Sus resultados antiguos de identidad son diseños, no resultados live.

## Limpiar datos y diagnosticar

`SecureAuthStorage.clearLocalUser()` solo limpia el dispositivo. No revoca tokens
ni elimina passkeys/evidencia remota. Para evidencia use cancelación autenticada;
no reinicie infraestructura para probar salida de un dispositivo.

Revise el estado devuelto por la API autenticada de captura. En `finalizing`, compruebe la validación de las imágenes. En `ready/pending`, el OCR está pendiente.

## Iniciar OCR durante la revisión de fotos

Use `CaptureHost(earlyCamera = { events -> … })` y conecte `events.onImagesReady`,
`events.onImagesInvalidated` y `events.onConfirmed` con los argumentos correspondientes
de `IdentityCamera`, como en `LiveCaptureFlow`. El slot `camera` anterior conserva el envío
tras confirmar. Use una ruta por cámara.

El detector fijado presta los bitmaps durante el callback síncrono. Auth crea
copias antes de regresar, limita el lado mayor a 1600 píxeles y codifica JPEG a color en
un dispatcher de trabajo, hasta 500 KiB por lado. Los bytes pasan al controlador; no los
conserve ni registre. El modelo mantiene su versión independiente `tfmodel~v0.1.2`.

`EarlyCaptureSession` conserva la revisión de fotos durante procesamiento. Repetir una
foto cancela el trabajo local, borra el borrador anterior y llama con autenticación a
`POST /onboarding/id/captures/{id}/retake`. El mutex serializa las sustituciones, incluso
respuestas tardías de solicitudes canceladas. Se reutiliza la captura sin otra admisión;
siguen vigentes los límites del backend. Las claves de retake persistidas permiten
recuperar respuestas inciertas. El checkpoint también conserva si se confirmaron las fotos.
Reiniciar antes de confirmar requiere capturarlas de nuevo porque los bitmaps locales
ya no existen; reiniciar después de confirmar reanuda el procesamiento del servidor.

Auth calcula `image_revision` esperado mediante el contrato de finalización. Solo una
coincidencia exacta de `(capture_id, image_revision)` permite llenar el formulario; las
revisiones de estado del worker pueden cambiar por separado. Confirmar no vuelve a subir
fotos: presenta el resultado correspondiente o carga mientras termina procesamiento.

Despliegue retake e `image_revision` en el backend antes de activar `earlyCamera`.
Un backend incompatible conduce a recuperación sin presentar resultados obsoletos.

Los datos usan `issued_identifier` en el contrato HTTP. La propiedad Kotlin
`personalNumber` conserva compatibilidad; también se leen respuestas antiguas con
`personal_number`, pero las solicitudes de revisión emiten `issued_identifier`.

Android confirma la carga del frente en cuanto termina el PUT. Así inicia el OCR
del frente mientras la cámara todavía puede capturar el reverso. Tras validar
las imágenes, el primer fallo de OCR abre la revisión manual. El único reintento
se ejecuta de inmediato y puede completar campos sin editar que no tengan el foco.
