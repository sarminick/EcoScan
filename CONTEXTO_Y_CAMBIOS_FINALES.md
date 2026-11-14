# Contexto del Proyecto y Registro de Cambios Finales — EcoScan

Este documento recopila el contexto arquitectónico, las decisiones de diseño y el historial de cambios aplicados en la aplicación Android **EcoScan** (Universidad Simón Bolívar). Su objetivo es proporcionar contexto inmediato y preciso a cualquier modelo de Inteligencia Artificial o desarrollador que continúe el trabajo en este repositorio.

---

## 1. Información General del Proyecto

- **Nombre de la App**: EcoScan (Campus Sostenible & Reciclaje Inteligente)
- **Institución**: Universidad Simón Bolívar (Colombia)
- **Package ID**: `com.example.todolist` (Histórico mantenido por compatibilidad de configuración de Firebase)
- **Rama activa**: `ecoscan`
- **Lenguaje y Entorno**:
  - **Lenguaje principal**: Java (Android SDK)
  - **Versión de Java / JDK**: OpenJDK 17 (Ubicado localmente en `.jdk-17/`)
  - **minSdk**: 24 (Android 7.0 Nougat)
  - **targetSdk**: 34 (Android 14) / tools targetApi 31
  - **Comando de compilación**: `JAVA_HOME=".jdk-17" ./gradlew clean assembleDebug`
- **Librerías clave**:
  - Android Material Components (Material 3 - `Theme.Material3.DayNight.NoActionBar`)
  - CameraX & ML Kit (`libimage_processing_util_jni.so`, `libmlkitcommonpipeline.so`) para detección y clasificación de residuos por IA
  - Firebase Authentication & Firestore (ver detalles en `SETUP_FIREBASE.md`)

---

## 2. Paleta Institucional y Código Nacional de Canecas

Los colores institucionalizados en [`app/src/main/res/values/colors.xml`](file:///home/juli/Documentos/EcoScan/new%20EcoScan/app/src/main/res/values/colors.xml) son:

- **Fondo de Íconos y Superficies Claras**: `#F3F0E4` (`ic_launcher_background`)
- **Verdes Institucionales Unisimón**:
  - Primario: `#007A3D` (`unisimon_green`)
  - Oscuro: `#00562A` (`unisimon_green_dark`)
  - Claro / Contenedores: `#E8F5E9` (`unisimon_green_light`)
  - Acento: `#00A859` (`unisimon_accent`)
- **Código Nacional de Canecas Colombia (Resolución 2184 de 2019)**:
  - Blanco (Aprovechables limpios): `#FFFFFF`
  - Verde (Orgánicos aprovechables): `#2E7D32`
  - Negro (No aprovechables): `#1E293B`

---

## 3. Resumen de Cambios Recientes

### Fase A: Refactorización UX/UI y Flujo de Navegación

1. **Eliminación de redundancias en "Reportar punto" y "Mis reportes"**:
   - Anteriormente la acción de reporte estaba dispersa en 4 puntos diferentes (Home, FAB en Puntos Verdes, Menú engranaje, Perfil).
   - **Solución**: Se centralizó la acción de reportar exclusivamente dentro de la sección **Puntos Verdes / Mapa**. Se limpiaron los accesos duplicados en el Home y en el menú de configuración.
2. **Menú Desplegable (Popup) en el Avatar de Usuario**:
   - Anteriormente al pulsar el avatar (iniciales de usuario en la barra superior) se abría una pantalla completa aislada sin botón de retorno ni opción de cierre de sesión.
   - **Solución**: Se transformó en un menú flotante popup anclado al avatar con 3 acciones:
     - *Perfil* (con navegación clara y botón de retorno)
     - *Mis reportes*
     - *Cerrar sesión* (con integración a Firebase Auth)
3. **Maquetación del Mapa y Reportes**:
   - Se estructuró la vista de Puntos Verdes con el contenedor para el mapa interactivo y el botón principal integrado para abrir el formulario modal de reporte.
4. **Estilo Glassmorphism y Corrección de Truncamiento**:
   - Se estilizaron popups, tarjetas y diálogos modales con bordes redondeados (`corner-radius` amplios) y acabados translúcidos / glassmorphism.
   - Se corrigieron los selectores (Spinners/Dropdowns) donde los textos se cortaban (ej. "Selecciona la s...").

---

### Fase B: Arreglo del Ícono de la Aplicación (Adaptive Icon & Legacy)

#### Problema Detectado
El logo de la Universidad Simón Bolívar (U grande + "Simón Bolívar" en cursiva + trazo largo del barco abajo a la derecha) es horizontalmente ancho. En Android, el launcher aplica una máscara circular o de cuadrado redondeado (squircle) sobre la zona visible central de 72dp dentro del lienzo de 108dp. Al estar usando anteriormente `logoapp.png` como imagen directa o sin margen de zona segura, el launcher recortaba la U a la izquierda y el trazo del barco a la derecha. Además, `logoapp.png` pesaba ~5.9 MB en `res/drawable/`.

#### Solución Implementada
1. **Zona Segura y Exportación del Foreground (108dp canvas / 66dp safe circle)**:
   - Se utilizó `logo/ic_launcher_foreground.png` (1080×1080 px con logo estrictamente centrado en la zona segura).
   - Se exportó el foreground en lienzo cuadrado centrado a cada densidad estándar de Android:
     - `app/src/main/res/mipmap-mdpi/ic_launcher_foreground.png` (108×108 px)
     - `app/src/main/res/mipmap-hdpi/ic_launcher_foreground.png` (162×162 px)
     - `app/src/main/res/mipmap-xhdpi/ic_launcher_foreground.png` (216×216 px)
     - `app/src/main/res/mipmap-xxhdpi/ic_launcher_foreground.png` (324×324 px)
     - `app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png` (432×432 px)

2. **Íconos Adaptativos XML (API 26+)**:
   - [`app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`](file:///home/juli/Documentos/EcoScan/new%20EcoScan/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml):
     ```xml
     <?xml version="1.0" encoding="utf-8"?>
     <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
         <background android:drawable="@color/ic_launcher_background" />
         <foreground android:drawable="@mipmap/ic_launcher_foreground" />
     </adaptive-icon>
     ```
   - [`app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`](file:///home/juli/Documentos/EcoScan/new%20EcoScan/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml): Idéntica configuración adaptativa para launchers con máscara circular forzada.

3. **Soporte Legacy para `minSdk = 24` (< 26)**:
   - Para dispositivos con Android 7.0 a 7.1.2 (que no soportan `<adaptive-icon>`), se generaron PNGs pre-compuestos sobre `#F3F0E4` en cada densidad (base 48dp):
     - `ic_launcher.png` (forma cuadrada con esquinas suavemente redondeadas)
     - `ic_launcher_round.png` (forma circular)
     - Resoluciones: `mdpi` (48px), `hdpi` (72px), `xhdpi` (96px), `xxhdpi` (144px), `xxxhdpi` (192px).

4. **Configuración en Manifest y Limpieza de Recursos**:
   - En [`app/src/main/AndroidManifest.xml`](file:///home/juli/Documentos/EcoScan/new%20EcoScan/app/src/main/AndroidManifest.xml):
     ```xml
     android:icon="@mipmap/ic_launcher"
     android:roundIcon="@mipmap/ic_launcher_round"
     ```
   - Se eliminó el archivo obsoleto `app/src/main/res/drawable/logoapp.png`.

5. **Verificación en Emulador Real**:
   - Se desinstaló la versión anterior (`adb uninstall com.example.todolist`) para purgar la caché interna de íconos del launcher de Android.
   - Tras reinstalar el nuevo APK debug, se verificó mediante capturas del launcher y mediciones de píxeles:
     - Diámetro del círculo del ícono: 158×158 px
     - Límites del isotipo: x [48, 163], y [61, 135]
     - Holgura izquierda: 20 px
     - Holgura derecha: 23 px
     - Holgura superior: 43 px
     - Holgura inferior: 41 px
     - **Resultado**: La "U", "Simón Bolívar" y el trazo del barco se muestran íntegros y nítidos sin ningún recorte.

---

## 4. Estructura de Archivos Clave del Proyecto

```text
new EcoScan/
├── app/
│   ├── build.gradle.kts / build.gradle
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml              # Declaración de permisos, actividades e iconos
│           ├── java/com/example/todolist/       # Código fuente Java
│           │   ├── MainActivity.java            # Controlador principal y navegación
│           │   ├── ...                          # Fragmentos y adaptadores
│           └── res/
│               ├── drawable/
│               │   ├── ic_launcher_background.xml # Vectorial de fondo color #F3F0E4
│               │   └── logo_unisimon_new.png    # Logo institucional para uso interno en UI
│               ├── mipmap-anydpi-v26/
│               │   ├── ic_launcher.xml          # Adaptive icon estándar
│               │   └── ic_launcher_round.xml    # Adaptive icon round
│               ├── mipmap-mdpi/                 # Foreground 108px, Legacy 48px
│               ├── mipmap-hdpi/                 # Foreground 162px, Legacy 72px
│               ├── mipmap-xhdpi/                # Foreground 216px, Legacy 96px
│               ├── mipmap-xxhdpi/               # Foreground 324px, Legacy 144px
│               ├── mipmap-xxxhdpi/              # Foreground 432px, Legacy 192px
│               └── values/
│                   ├── colors.xml               # Paleta de colores institucional
│                   ├── strings.xml              # Textos de la aplicación
│                   └── themes.xml               # Tema Material 3
├── logo/
│   ├── ic_launcher_foreground.png               # Master 1080x1080 transparente del foreground
│   └── ic_launcher_playstore_512.png            # Master 512x512 para Google Play Store
├── .jdk-17/                                     # JDK 17 embebido local para Gradle
├── SETUP_FIREBASE.md                            # Guía de configuración de Firebase Auth & Firestore
├── CONTEXTO_Y_CAMBIOS_FINALES.md               # Este documento de contexto técnico
└── README.md                                    # Descripción general de EcoScan
```

---

## 5. Reglas y Buenas Prácticas para Futuras Modificaciones

1. **Uso de Logos en UI**:
   - Para pantallas internas (splash, headers o vistas informativas), utilizar [`logo_unisimon_new.png`](file:///home/juli/Documentos/EcoScan/new%20EcoScan/app/src/main/res/drawable/logo_unisimon_new.png) con `android:scaleType="fitCenter"` o `centerInside`. **Nunca usar `centerCrop`** para logos institucionales.
   - En Android 12+, si se personaliza el SplashScreen con `windowSplashScreenAnimatedIcon`, apuntar a `@mipmap/ic_launcher_foreground` para que el enmascarado circular del splash no corte el texto.
2. **Compilación en Entorno Local**:
   - Siempre fijar `JAVA_HOME` a `.jdk-17` antes de ejecutar Gradle:  
     `JAVA_HOME="/home/juli/Documentos/EcoScan/new EcoScan/.jdk-17" ./gradlew assembleDebug`
3. **Instalación y Caché en Emuladores**:
   - Al modificar recursos gráficos del launcher (`mipmap-*`), desinstalar siempre la app previa del dispositivo antes de instalar la nueva (`adb uninstall com.example.todolist`) para evitar que el launcher muestre íconos en caché.
