# Configuración de Firebase — EcoScan Unisimón (Fase 2)

Esta guía documenta la configuración necesaria para el funcionamiento de los reportes colaborativos, moderación comunitaria y autenticación institucional.

---

## 1. Reglas de Seguridad en Cloud Firestore

Se actualizaron las reglas en [`firebase/firestore.rules`](firebase/firestore.rules) para proteger:
- Creación de reportes solo para usuarios autenticados con dominio `@unisimon.edu.co` y `email_verified == true`.
- Confirmación comunitaria de puntos pendientes:
  - Solo usuarios con correo verificado pueden confirmar.
  - Se impide que un usuario confirme su propio reporte.
  - Se impide que un usuario confirme más de una vez.
  - Al alcanzar 2 confirmaciones válidas, se permite la transición automática del estado a `approved`.
  - Creación y actualización automática de puntos en `recycling_points` para puntos aprobados.

### Cómo publicar las reglas:
1. Ve a [Firebase Console](https://console.firebase.google.com/) > Tu Proyecto > **Firestore Database** > Pestaña **Rules**.
2. Pega el contenido de [`firebase/firestore.rules`](firebase/firestore.rules).
3. Haz clic en **Publish**.

---

## 2. Índices de Firestore Requeridos

Para la consulta de límite diario de reportes (5 reportes por usuario en 24h), Firestore puede requerir un índice compuesto:
- Colección: `reports`
- Campos a indexar:
  - `userId` (Ascending)
  - `createdAt` (Ascending / Descending)

Si al enviar un reporte la consola de Android Studio muestra un enlace de índice faltante, haz clic en el enlace para crearlo automáticamente en Firebase Console.

---

## 3. Permisos de Android utilizados

- `android.permission.CAMERA`: Para tomar la foto real del punto de reciclaje y para el visor de escáner.
- `android.permission.ACCESS_FINE_LOCATION`: Para capturar automáticamente las coordenadas GPS del punto reportado.
- `android.permission.INTERNET`: Para comunicación con Firebase Authentication y Firestore.

Todos los permisos ya se encuentran declarados en `AndroidManifest.xml` y se solicitan en tiempo de ejecución.

---

## 4. Flujo colaborativo de Fase 2

1. **Escaneo con IA**:
   - Al escanear un objeto, tras el resultado aparece una sugerencia discreta:
     *"¿Conoces un punto nuevo o ha cambiado alguno? Ayúdanos a mantener actualizado el mapa reportándolo."*
   - Al pulsarla, se despliega el formulario **Bottom Sheet** con la categoría ya preseleccionada.
2. **Reporte integrado (Bottom Sheet)**:
   - Permite reportar:
     - **Punto nuevo**: foto real con la cámara, GPS automático/editable, sede, bloque, punto de referencia, categoría y notas.
     - **Punto retirado**: selección del punto existente y motivo.
     - **Punto movido**: selección del punto existente y nueva ubicación.
   - Aplica límite diario de 5 reportes por usuario.
3. **Validación comunitaria en el Mapa**:
   - Los puntos aprobados se visualizan con distintivo verde `✓ Aprobado`.
   - Los puntos comunitarios pendientes se visualizan con distintivo naranja `⏳ En validación (X/2)`.
   - Al tocar cualquier punto se despliega el Bottom Sheet con su detalle completo.
   - Si está pendiente y el usuario no es el autor ni ha confirmado, puede pulsar **"✓ Confirmar existencia de este punto"**.
   - Con **2 confirmaciones**, el punto se aprueba automáticamente y pasa a formar parte oficial del directorio de reciclaje.
