# EcoScan Unisimón - App Móvil de Reciclaje Inteligente

Aplicación móvil nativa para Android desarrollada en **Java** para la **Universidad Simón Bolívar (Colombia)**, con enfoque en desarrollo sostenible, economía circular y clasificación de residuos asistida por IA.

---

## 🎨 Identidad Visual y Colores Institucionales

La interfaz adopta la identidad visual de la **Universidad Simón Bolívar** y la normativa nacional colombiana de separación de residuos (Resolución 2184 de 2019):

- **Verde Institucional Unisimón**: `#007A3D` (Color primario)
- **Verde Oscuro Institucional**: `#00562A` (Barra de estado / encabezados)
- **Verde Claro Suave**: `#E8F5E9` / `#F0FDF4` (Contenedores y tarjetas)
- **Código de Canecas en Colombia**:
  - **Blanca**: Residuos aprovechables (Plástico, vidrio, papel, cartón, metales).
  - **Verde**: Residuos orgánicos aprovechables (Restos de comida, cáscaras, desechos agrícolas).
  - **Negra**: Residuos no aprovechables (Papel higiénico, servilletas usadas, papeles metalizados).
- **Logo Unisimón**: Integrado en la cabecera superior de la aplicación y en la vista de bienvenida.

---

## 📱 Arquitectura de Pantallas y Navegación (Bottom Navigation)

La navegación se gestiona mediante un **BottomNavigationView** que coordina 4 secciones principales:

1. **🏠 Inicio (`HomeFragment`)**:
   - Tarjeta de bienvenida con el logo oficial de la Universidad Simón Bolívar.
   - Acceso directo al botón **"Escanear con IA"**.
   - Resumen visual interactivo del **Código de Canecas de Colombia** (Blanca, Verde y Negra).
   - Accesos directos a las categorías de residuos (Plástico, Vidrio, Papel/Cartón, Orgánicos).

2. **🔍 Escanear (`ScanFragment`)**:
   - Visor de cámara simulada con marco de enfoque verde institucional y animación de escaneo.
   - Botón para **Tomar Foto** y botón **Demo IA**.
   - Chips interactivos de prueba inmediata (Botella PET, Caja de cartón, Frasco de vidrio, Cáscara de fruta, Servilleta usada).
   - Tarjeta interactiva con el **Resultado del Escaneo**:
     - Nombre del residuo y categoría.
     - Badge de estado: **RECICLABLE** ✅ o **NO RECICLABLE** ❌.
     - Indicación exacta de la **Caneca correspondiente**.
     - **Instrucciones paso a paso** para desecharlo adecuadamente (lavar, secar, aplanar).
     - Botón de acceso directo hacia los puntos de reciclaje cercanos.

3. **🗺️ Puntos Verdes (`MapFragment`)**:
   - Vista de mapa ecológico del campus de la Universidad Simón Bolívar.
   - Filtros por caneca (Todos, Caneca Blanca, Caneca Verde, Caneca Negra).
   - Directorio de puntos ecológicos con distancia en metros y ubicación detallada (Bloque A, Cafetería Central, Biblioteca Unisimón).

4. **📖 Guía Ambiental (`GuideFragment`)**:
   - Manual didáctico sobre materiales reciclables y no reciclables.
   - Tips para reducir el volumen de residuos en el campus y fomentar la cultura ecológica.

---

## 📂 Estructura del Código Fuente

```text
mobile/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml              # Permisos (Cámara, GPS) y tema
│   │   ├── java/com/example/todolist/
│   │   │   ├── MainActivity.java            # Coordinador de navegación y Toolbar
│   │   │   ├── HomeFragment.java            # Pantalla de inicio
│   │   │   ├── ScanFragment.java            # Pantalla de escáner y resultados IA
│   │   │   ├── MapFragment.java             # Mapa y puntos ecológicos
│   │   │   └── GuideFragment.java           # Guía de separación de residuos
│   │   └── res/
│   │       ├── drawable/logo_unisimon.jpg   # Logo institucional Unisimón
│   │       ├── layout/
│   │       │   ├── activity_main.xml        # Toolbar + Container + BottomNav
│   │       │   ├── fragment_home.xml        # Layout de Inicio
│   │       │   ├── fragment_scan.xml        # Layout de Escaneo
│   │       │   ├── fragment_map.xml         # Layout de Mapa
│   │       │   └── fragment_guide.xml       # Layout de Guía
│   │       ├── menu/bottom_nav_menu.xml     # Menú de navegación
│   │       └── values/
│   │           ├── colors.xml               # Colores Unisimón y canecas
│   │           ├── strings.xml              # Textos en español
│   │           └── themes.xml               # Tema Material 3
```

---

## 🚀 Cómo ejecutar y probar en Android Studio o IntelliJ IDEA

1. Abre el proyecto seleccionando la carpeta `/home/diego/projects/java/mobile`.
2. Espera la sincronización automática de Gradle.
3. Conecta un dispositivo físico por USB o inicia un emulador Android desde el **Device Manager**.
4. Haz clic en **Run ▶** (`Shift + F10`).

---

## IA y cámara (implementación actual)

La pantalla **Escanear** usa CameraX para tomar la foto y ML Kit Image Labeling para
reconocer etiquetas en el propio dispositivo. No requiere clave de API, servidor ni
subir fotos a Internet. El resultado solo se muestra cuando una etiqueta coincide con
una categoría de reciclaje conocida; de lo contrario pide una foto con mejor luz en
lugar de inventar una clasificación.

Este clasificador genérico es adecuado como asistencia inicial, pero no garantiza la
identificación de materiales. Para una versión de producción se recomienda entrenar y
validar un modelo TensorFlow Lite específico para los residuos aceptados por la
universidad (por ejemplo PET, cartón limpio, vidrio, orgánicos y no aprovechables) y
reemplazar la conversión de etiquetas en `ScanFragment`.

## Directorio de puntos ecológicos

El directorio no contiene lugares de ejemplo: únicamente lee ubicaciones verificadas
desde `app/src/main/assets/recycling_points.json`. Agrega una entrada por punto con
este formato:

```json
[
  {
    "name": "Estación ecológica Edificio X",
    "location": "Primer piso, junto a la entrada principal",
    "bins": "white, green, black",
    "latitude": "10.000000",
    "longitude": "-74.000000"
  }
]
```

Las coordenadas quedan preparadas para una futura vista de mapa. La versión actual
es un directorio filtrable, por lo que no requiere una clave de Google Maps. Si se
añade Google Maps posteriormente, la clave debe guardarse fuera del control de
versiones, mediante `local.properties` o el mecanismo de secretos de compilación.

### Puntos verificados de Unisimón Barranquilla

El archivo de datos ya incorpora los cuatro puntos reportados para la Universidad
Simón Bolívar de Barranquilla: bloque C de la Sede 3, ascensor de la Sede 2,
ECO COMPUTA/recolector de bombillas en la entrada de la Sede 2 y las canecas blanca
y negra frente a ese punto. La IA dirige botellas y tapas a los puntos de plástico,
equipos electrónicos a ECO COMPUTA, bombillas a su recolector y evita recomendar
ECO COMPUTA para pilas o baterías.

---

## Comunidad EcoScan y Firebase

La aplicación usa exclusivamente Firebase Authentication y Cloud Firestore, aptos
para el plan Spark. Incluye registro/inicio de sesión, verificación y recuperación de
correo, reportes de texto pendientes, consulta de reportes propios y revisión
administrativa atómica. Por ahora no se adjuntan fotografías, porque no se usa Cloud
Storage.

Antes de activar estas funciones, el propietario debe:

1. Crear un proyecto Firebase y registrar la aplicación Android con el paquete
   `com.example.todolist`.
2. Habilitar Email/Password en Firebase Authentication.
3. Descargar `google-services.json` y colocarlo en `app/google-services.json`.
4. Aplicar `firebase/firestore.rules` y `firebase/firestore.indexes.json` usando
   Firebase CLI o la consola.
5. Desde un equipo confiable, instalar las dependencias de `firebase/admin-scripts`
   y ejecutar una sola vez `bootstrap-superadmin.js` con el UID de tu cuenta
   principal. Para auxiliares, el superadministrador crea una solicitud dentro de la
   app y el operador confiable ejecuta `set-auxiliary-admin.js` localmente.

No se debe añadir una cuenta de servicio, clave privada ni token administrativo al
APK ni al repositorio. Las reglas impiden que un usuario cambie su rol o el estado de
su reporte, y las publicaciones pasan por un lote atómico que actualiza el reporte,
el punto público, el historial y una notificación dentro de la aplicación. No se
despliegan Cloud Functions ni se activa Cloud Storage.

### Roles administrativos

- **Superadministrador:** claim `role: superadmin`; puede gestionar auxiliares y
  consultar todo el historial administrativo. Este claim no tiene flujo de alta desde
  el APK.
- **Administrador auxiliar:** claim `role: admin`; puede revisar reportes y editar
  puntos publicados mediante Firestore, pero no cambia roles ni elimina puntos.
- **Usuario:** claim ausente o `role: user`; solo usa las funciones públicas y sus
  propios reportes.

Las reglas validan transiciones de estado y la publicación atómica de puntos mediante
`getAfter`. Los cambios de roles se hacen solo desde scripts locales con Firebase
Admin SDK; nunca desde el APK.
