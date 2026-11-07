# Solución de problemas

Ámbito: Desarrollo. Separe evidencia Android/dispositivo de
[operaciones del backend](https://raw.githubusercontent.com/lambdawalker/go.attestra.aws.auth/main/docs/agents/troubleshooting.md).

| Síntoma | Diagnóstico | Respuesta |
| --- | --- | --- |
| Gradle falla en activos generados | Revise `auth/build.gradle.kts` y AGP | Use el registro de fuentes generadas; no convierta source sets antiguos ni culpe automáticamente a caché |
| Conflicto modelo/detector | Compare catálogo y `cardModelAssets` | Extracción de activos aislada/no transitiva; no agregue dos versiones de la misma identidad JitPack a runtime |
| Enlace abre navegador | Host, paquete, certificado, `assetlinks.json` e identidad instalada | Siga [firma](../../android-signing.md); el índice no asocia App Links |
| Otro dispositivo solicita código | Falta prueba A local | Use B + C; nunca B solo ni copie secretos desde logs |
| Ajustes no disponibles | Índice, binding, `/v1/app-config`, caché y validación | Reintente configuración confiable; no invente política exitosa ni cambie de servidor |
| Estado `finalizing` no avanza | Validación o transacción de evidencia no concluyó | Revise S3, DynamoDB y la cola del worker |
| `ready/pending` | Extracción en cola/ejecución | Revise la cola del worker y el estado de captura |
| Extracción `failed` | No se pudo leer texto | Formulario manual vacío, no datos parciales de un proveedor fallido |
| Guardado falla/conflicto | Revise `reviewed` y revisión del borrador | Conserve cambios, actualice/reconcilie; no finalice sin confirmación coincidente |
| Requiere retoma tras reiniciar | Bytes solo en memoria | Vuelva a tomar fotos inconclusas; ID/checkpoint no reconstruye imágenes |
| Passkey cancelada/no admitida | `PasskeyCancelled` / `PasskeyUnsupported` | Ofrezca recuperación; no marque registro exitoso |
| Captura de pantalla vacía/faltante | Inventario, estado de fixture y reporte | Genere candidatos e inspeccione; no reemplace baselines durante validación |

## Registros seguros

La captura de documentos omite el registro detallado de OCR. El estado de la captura indica si el procesamiento finalizó.

Nunca registre tokens, contraseñas, secretos de enlaces, JPEG, URLs firmadas,
texto personal, respuestas del proveedor o excepciones que incluyan esos valores.
