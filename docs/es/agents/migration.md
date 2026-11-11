# Migración y compatibilidad

Ámbito: Desarrollo. No se afirma una frontera de versión auth publicada.
Use commits inmutables o historia de PR revisada; no existe catálogo de API publicada histórica.

| Patrón anterior | Implementación actual | Acción |
| --- | --- | --- |
| Confirmación de captura y envío por cara desde el host | Captura bilateral, subida/finalización automáticas y revisión | Use `LiveCaptureFlow`; no genere código nuevo contra borradores `/identity/v1/submissions` |
| `id_captured` como guardado/aprobación | Evidencia, extracción, revisión y estado separados | Exija `reviewed` coincidente; no afirme validación de identidad |
| Conversión de source sets Android antiguos | Activos generados por `androidComponents.onVariants` | Use `PrepareCardModelAssets` del repositorio |
| Solo URL API fija | Índice confiable o URL fija explícita, luego ajustes dinámicos | Aísle backend/cuenta; modo fijo no es fallback silencioso |
| Smoke de screenshots regeneraba referencias | Candidatos, aceptación revisada y comparación separados | Siga [capturas](../../screenshots.md); candidatos no son baselines aprobados |

Las bibliotecas dependientes tienen historias independientes; el catálogo auth
fija sus combinaciones desde fuente. La versión APK no versiona esas bibliotecas.
Para futuras versiones auth conserve identidad de código/documentación e instalación
antes de mover latest. Si falta traducción, use el idioma canónico del mismo ámbito,
nunca una API más reciente disfrazada de histórica.

## Espacio de nombres de Apexfission

El paquete fuente ahora es `com.apexfission.android.auth` (antes
`com.apexfission.android.attestra.auth`). Actualice imports y nombres completos,
incluido el espacio de recursos de la biblioteca `com.apexfission.android.auth.library`.
Los directorios fuente, las pruebas y las herramientas de firma usan el nuevo paquete.

El ID de la aplicación demo ahora es `com.apexfission.android.auth`. Android lo trata
como otra aplicación; no hereda la sesión local ni el almacenamiento privado de la
instalación anterior. Actualice la asociación de paquete en `assetlinks.json` del
backend para App Links y passkeys con el nuevo ID y el certificado de firma instalado;
registre la nueva asociación antes de probar esas integraciones.

Las URLs del backend, las propiedades Gradle `attestra*`, las variables `ATTESTRA_*`,
los nombres del almacenamiento privado y los alias de firma conservan sus contratos.
El nombre del repositorio y las coordenadas de publicación son independientes del
paquete Kotlin; este cambio no renombra el repositorio ni publica artefactos.
