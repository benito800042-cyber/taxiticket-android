# TaxiTicket — APK Android con OCR local

Aplicación independiente (`com.taxiticket.app`) para registrar servicios y jornadas. La pantalla web se ejecuta dentro de un WebView local; mantiene facturas/borradores, registro diario/semanal/mensual y exportación CSV/JSON.

## OCR real y privacidad

Al tomar o elegir una foto, la actividad nativa ejecuta **Google ML Kit Text Recognition (modelo latino incluido en el APK)**. El procesamiento se realiza en el dispositivo: no requiere permiso `INTERNET`, no sube fotos ni datos a ningún servidor y la foto no se almacena como parte del registro. El texto reconocido se devuelve al formulario mediante un puente controlado `evaluateJavascript` y propone automáticamente número de servicio, kilómetros totales, importe total, fecha y hora de cierre. Los campos siguen siendo editables y el guardado exige marcar la confirmación de revisión.

La fecha impresa, cuando se reconoce, se conserva también como dato informativo separado. La fecha/hora de cierre se propone desde el ticket cuando hay una lectura válida; si no, se mantiene la fecha/hora del móvil. La interfaz siempre exige que el usuario revise y confirme.

El modelo incluido aumenta el tamaño del APK, pero evita descargar modelos al primer uso y permite trabajar sin conexión. En tickets borrosos, inclinados, con reflejos, poca luz, tipografías inusuales o campos recortados ML Kit puede devolver texto incompleto o no reconocer un campo: en esos casos se muestra un aviso y hay que completar/corregir manualmente. OCR no interpreta datos que no aparezcan visibles en la foto.

## Compilar

Requisitos: JDK 17, Android SDK `platforms;android-35` y Gradle 8.7+ (o Android Studio Koala+).

```bash
gradle --no-daemon :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions (`.github/workflows/build-apk.yml`) compila el APK debug en cada push a `main` o manualmente y publica el archivo como artefacto `TaxiTicket-app-debug`. El enlace descargable se encuentra en la ejecución del workflow, sección **Artifacts** (requiere acceso al repositorio).

## Cámara y almacenamiento

La cámara se abre mediante `MediaStore.ACTION_IMAGE_CAPTURE`; la salida usa `FileProvider` dentro del almacenamiento específico de la aplicación. El selector manual usa `ACTION_OPEN_DOCUMENT`, sin permiso amplio de galería. Los registros y la configuración se guardan en `localStorage` de esta WebView. La factura generada es un borrador: revisar normativa española antes de emitirla.
