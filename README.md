# TaxiTicket — APK de prueba Android (WebView local)

Proyecto independiente de TaxiTicket (`com.taxiticket.app`), basado en la versión corregida de la web: la fecha/hora principal es el cierre/escaneo editable y la fecha impresa del ticket se conserva en un campo separado. No modifica ni comparte código con Taxi Ya, Taxi13, VozPuente, TV Fácil, TaxiClick ni Navegador Espejo.

## Qué incluye

- Aplicación Android nativa Java + WebView, sin URL remota ni permiso `INTERNET`.
- JavaScript y `localStorage` activados para la aplicación web.
- Selector de imágenes del sistema y opción de cámara desde el selector (`input type=file`, captura tras solicitar `CAMERA` al usarla).
- `READ_MEDIA_IMAGES` declarado para Android moderno y `READ_EXTERNAL_STORAGE` solo hasta Android 12; para elegir una imagen se usa el selector/document provider del sistema, por lo que no se pide acceso amplio a la galería.
- Exportación CSV/JSON y ventana de factura/borrador con **Imprimir / Guardar PDF**, si el WebView/dispositivo lo permite.
- Sin envío de fotos, tickets o datos a un servidor. La librería OCR se referencia en la web original desde CDN, pero este APK no tiene permiso de red: por privacidad, en esta prueba el OCR remoto no se carga y la fecha se puede introducir/corregir manualmente. No se afirma que el OCR sea fiable.

## Compilar e instalar

Requisitos del equipo de compilación (no incluidos en este paquete):

- JDK 17.
- Android SDK con `platforms;android-35` y `build-tools` recientes.
- Gradle 8.7+ o Android Studio Koala (el proyecto usa Android Gradle Plugin 8.5.2).

Desde esta carpeta, con Gradle instalado:

```bash
gradle :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

También se puede abrir la carpeta en Android Studio y ejecutar la variante `debug`. El APK resultante será `app-debug.apk` (no firmado para distribución).

## Estado de esta entrega

Se deja el proyecto fuente completo y listo para abrir/compilar. En el entorno donde se generó esta entrega no estaban disponibles Java/JDK, Gradle ni Android SDK; por ello **no se ha generado un APK descargable aquí**. No se ha podido verificar la compilación en un dispositivo.

## Avisos

- La foto se mantiene en la memoria temporal de la web durante el alta; los registros/configuración usan almacenamiento local de la WebView.
- La factura es solo un borrador. Revisar numeración, IVA y demás requisitos con asesoría antes de emitirla.
- La función OCR y cualquier uso de facturación requieren revisión y pruebas antes de considerarlos adecuados o conformes a normativa.
