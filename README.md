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
