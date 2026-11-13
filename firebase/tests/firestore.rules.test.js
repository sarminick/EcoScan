import fs from "node:fs";
import { assertFails, assertSucceeds, initializeTestEnvironment } from "@firebase/rules-unit-testing";
import { Timestamp, doc, getDoc, setDoc, updateDoc, writeBatch, serverTimestamp } from "firebase/firestore";

const projectId = "ecoscan-81b76";
const rules = fs.readFileSync(new URL("../firestore.rules", import.meta.url), "utf8");
const testEnv = await initializeTestEnvironment({
  projectId,
  firestore: { rules, host: "127.0.0.1", port: 8080 }
});

const createdAt = Timestamp.fromDate(new Date("2026-01-10T12:00:00Z"));
const studentId = "student-uid";
const adminId = "admin-uid";
const superadminId = "superadmin-uid";
const student = testEnv.authenticatedContext(studentId, { email: "student@unisimon.edu" }).firestore();
const admin = testEnv.authenticatedContext(adminId, { email: "admin@unisimon.edu", role: "admin" }).firestore();
const superadmin = testEnv.authenticatedContext(superadminId, { email: "root@unisimon.edu", role: "superadmin" }).firestore();

function report(id, userId = studentId) {
  return {
    id, userId, status: "pending", site: "Sede 2", block: "A", floor: "1",
    location: "Entrada principal", materialsAccepted: "Botellas plásticas",
    description: "Punto señalizado", coordinates: null, createdAt, updatedAt: createdAt
  };
}
function pointFromReport(r, reviewer = adminId) {
  return {
    id: r.id, site: r.site, block: r.block, floor: r.floor, location: r.location,
    materialsAccepted: r.materialsAccepted, description: r.description, coordinates: r.coordinates,
    createdAt: r.createdAt, updatedAt: r.updatedAt, sourceReport: r.id, active: true,
    publishedAt: serverTimestamp(), publishedBy: reviewer
  };
}
function reviewBatch(db, r, status = "approved", includePoint = true) {
  const batch = writeBatch(db);
  batch.update(doc(db, "reports", r.id), {
    status, reviewedBy: adminId, reviewedAt: serverTimestamp(), reviewNote: status === "approved" ? "" : "Revisa la información indicada."
  });
  if (includePoint && status === "approved") batch.set(doc(db, "recycling_points", r.id), pointFromReport(r));
  batch.set(doc(db, "report_history", r.id), {
    reportId: r.id, action: status, responsibleUser: adminId,
    notes: status === "approved" ? "" : "Revisa la información indicada.", createdAt: serverTimestamp()
  });
  batch.set(doc(db, "admin_history", `review_${r.id}`), {
    reportId: r.id, actorUid: adminId, action: `review_${status}`, createdAt: serverTimestamp()
  });
  batch.set(doc(db, "notifications", r.id), {
    userId: r.userId, reportId: r.id, status,
    message: `Tu reporte fue ${status}.`, createdAt: serverTimestamp()
  });
  return batch;
}
async function seed(path, data) {
  await testEnv.withSecurityRulesDisabled(async (context) => setDoc(doc(context.firestore(), path[0], path[1]), data));
}

try {
  await assertSucceeds(setDoc(doc(student, "users", studentId), {
    uid: studentId, name: "Estudiante", email: "student@unisimon.edu", createdAt: serverTimestamp()
  }));
  await assertFails(setDoc(doc(student, "users", "other-uid"), {
    uid: "other-uid", name: "Ataque", email: "student@unisimon.edu", role: "admin", createdAt: serverTimestamp()
  }));

  const submitted = report("report-valid");
  await assertSucceeds(setDoc(doc(student, "reports", submitted.id), { ...submitted, createdAt: serverTimestamp(), updatedAt: serverTimestamp() }));
  await assertFails(setDoc(doc(student, "reports", "report-bad"), { ...report("report-bad"), status: "approved", createdAt: serverTimestamp(), updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(doc(student, "reports", submitted.id), { status: "approved" }));
  await assertFails(setDoc(doc(student, "recycling_points", "direct-point"), pointFromReport({ ...submitted, id: "direct-point" })));
  await assertFails(setDoc(doc(student, "role_change_requests", "student-request"), {
    targetEmail: "other@unisimon.edu", action: "grant_admin", requestedBy: studentId, status: "pending", createdAt: serverTimestamp()
  }));

  const approved = report("report-approved");
  await seed(["reports", approved.id], approved);
  await assertSucceeds(reviewBatch(admin, approved).commit());
  await assertSucceeds(getDoc(doc(student, "recycling_points", approved.id)));
  await assertSucceeds(getDoc(doc(student, "notifications", approved.id)));

  const missingPoint = report("report-without-point");
  await seed(["reports", missingPoint.id], missingPoint);
  await assertFails(reviewBatch(admin, missingPoint, "approved", false).commit());

  await assertFails(setDoc(doc(admin, "report_history", "forged"), {
    reportId: approved.id, action: "approved", responsibleUser: adminId, notes: "", createdAt: serverTimestamp()
  }));
  await assertFails(setDoc(doc(admin, "notifications", "forged"), {
    userId: studentId, reportId: approved.id, status: "approved", message: "Tu reporte fue approved.", createdAt: serverTimestamp()
  }));

  await assertSucceeds(setDoc(doc(superadmin, "role_change_requests", "request-1"), {
    targetEmail: "auxiliar@unisimon.edu", action: "grant_admin", requestedBy: superadminId, status: "pending", createdAt: serverTimestamp()
  }));
  await assertFails(updateDoc(doc(student, "users", studentId), { role: "admin" }));
  console.log("OK: reglas permitidas y denegadas verificadas con Emulator Suite.");
} finally {
  await testEnv.cleanup();
}
