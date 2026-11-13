# Pruebas Spark: roles y reportes

Las pruebas automatizadas están en `firestore.rules.test.js`. No utilizan ni despliegan
el proyecto Firebase real: `firebase emulators:exec` levanta solamente Firestore local.

| Caso | Cuenta | Acción | Resultado esperado |
|---|---|---|---|
| S1 | Usuario | Registrar e iniciar sesión | Puede crear su perfil, sin campo de rol. |
| S2 | Usuario | Crear reporte pendiente | Solo puede crear `userId == auth.uid` y estado `pending`. |
| S3 | Usuario | Leer reporte ajeno | Firestore lo rechaza. |
| S4 | Usuario | Cambiar estado, rol o punto público | Firestore lo rechaza. |
| S5 | Auxiliar | Aprobar en un lote un reporte pendiente | Se actualizan reporte, punto público, historial y notificación. |
| S6 | Auxiliar | Aprobar sin crear punto en el mismo lote | Firestore lo rechaza por `getAfter`. |
| S7 | Auxiliar | Eliminar un punto | Firestore lo rechaza. |
| S8 | Superadministrador | Crear solicitud de auxiliar | Crea solo una solicitud, sin cambiar ningún claim. |
| S9 | Todos | Escribir `role` en `users` | Firestore lo rechaza. |
| S10 | Usuario | Consultar puntos | Solo ve puntos con `active == true`. |

Comandos de Windows para instalar las dependencias y ejecutar las pruebas:

```powershell
cd firebase\tests
npm install
firebase emulators:exec --only firestore "npm run test:rules" --project ecoscan-81b76
```

El plan Spark no requiere Cloud Storage, Cloud Functions ni Cloud Messaging para este
flujo. Los claims se crean únicamente con los scripts locales de `firebase/admin-scripts`.
