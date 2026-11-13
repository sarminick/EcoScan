// Trusted workstation only: node set-auxiliary-admin.js <email> grant|revoke [role-change-request-id]
const admin = require("firebase-admin");
const [email, action, requestId] = process.argv.slice(2);
if (!email || !["grant", "revoke"].includes(action) || !process.env.FIREBASE_SERVICE_ACCOUNT) throw new Error("Provide email, grant|revoke, and FIREBASE_SERVICE_ACCOUNT.");
admin.initializeApp({ credential: admin.credential.cert(JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT)) });
(async () => {
  const auth = admin.auth(), db = admin.firestore(); const user = await auth.getUserByEmail(email);
  if (user.customClaims?.role === "superadmin") throw new Error("A superadministrator cannot be changed by this script.");
  const role = action === "grant" ? "admin" : "user"; await auth.setCustomUserClaims(user.uid, { role });
  const batch = db.batch(); batch.set(db.collection("users").doc(user.uid), { role, roleUpdatedAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });
  batch.set(db.collection("admin_history").doc(), { action: action === "grant" ? "grant_auxiliary_admin" : "revoke_auxiliary_admin", targetUid: user.uid, targetEmail: email, executedLocally: true, createdAt: admin.firestore.FieldValue.serverTimestamp() });
  if (requestId) batch.update(db.collection("role_change_requests").doc(requestId), { status: "completed", completedAt: admin.firestore.FieldValue.serverTimestamp() });
  await batch.commit(); console.log(`${email} is now ${role}.`);
})();
