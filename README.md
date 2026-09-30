# Mis Tareas (To-Do List) - Android Mobile App en Java

Aplicación móvil nativa para Android desarrollada en **Java**, con interfaz moderna siguiendo las directrices de **Material Design 3**.

---

## 📱 Características de la Aplicación

- **Gestión de tareas**: Agrega nuevas tareas pendientes de forma ágil.
- **Marcar como completadas**: Casilla interactiva con efecto visual tachado.
- **Eliminación con confirmación**: Diálogo de alerta para evitar borrados accidentales.
- **Contador en tiempo real**: Visualiza cuántas tareas tienes pendientes.
- **Persistencia local**: Guarda automáticamente las tareas usando `SharedPreferences` y `Gson`, de modo que no se pierden al cerrar la aplicación.
- **Interfaz moderna y limpia**: Tarjetas con esquinas redondeadas (`MaterialCardView`) y paleta de colores Material 3.

---

## 📂 Estructura del Proyecto

```text
mobile/
├── app/
│   ├── build.gradle                       # Dependencias y configuración Android
│   ├── src/main/
│   │   ├── AndroidManifest.xml            # Declaración de componentes y permisos
│   │   ├── java/com/example/todolist/
│   │   │   ├── MainActivity.java          # Controlador principal y persistencia
│   │   │   ├── Task.java                  # Modelo de datos de la tarea
│   │   │   └── TaskAdapter.java           # Adaptador de RecyclerView
│   │   └── res/
│   │       ├── layout/
│   │       │   ├── activity_main.xml      # Vista principal (pantalla)
│   │       │   └── item_task.xml          # Diseño de cada elemento de la lista
│   │       ├── values/
│   │       │   ├── colors.xml             # Paleta de colores
│   │       │   ├── strings.xml            # Textos y recursos de cadenas
│   │       │   └── themes.xml             # Tema de la app
│   │       └── drawable/                  # Iconos vectoriales
├── gradle/wrapper/                        # Gradle Wrapper configurado (v8.9)
├── build.gradle                           # Configuración raíz de plugins
├── settings.gradle                        # Definición de repositorios y módulos
└── gradlew / gradlew.bat                  # Scripts de compilación listos para usar
```

---

## 🚀 Cómo abrir y ejecutar el proyecto

### Opción 1: Abrir directamente en Android Studio (Recomendado)
1. Abre **Android Studio**.
2. Selecciona **Open** (Abrir proyecto) y navega hasta la carpeta:
   ```
   /home/diego/projects/java/mobile
   ```
3. Espera a que Android Studio sincronice las dependencias con Gradle automáticamente.
4. Conecta tu teléfono Android por USB (con depuración USB activa) o inicia un **Emulador Android** desde el *Device Manager*.
5. Presiona el botón verde de **Run** (`Shift + F10`) para compilar e instalar la app.

### Opción 2: Compilar por consola con Gradle
Si tienes el Android SDK configurado en tu variable de entorno `ANDROID_HOME`:
```bash
./gradlew assembleDebug
```
El archivo APK generado se ubicará en:
```
app/build/outputs/apk/debug/app-debug.apk
```
