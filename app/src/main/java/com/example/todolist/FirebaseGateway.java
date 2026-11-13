package com.example.todolist;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Firebase Spark implementation: Auth + Firestore only; collaborative moderation and reports. */
public final class FirebaseGateway {
    private FirebaseGateway() { }

    public static final int DAILY_REPORT_LIMIT = 5;
    public static final int CONFIRMATIONS_REQUIRED_FOR_APPROVAL = 2;

    public interface Result { void done(Exception error); }
    public interface Documents { void done(QuerySnapshot documents, Exception error); }
    public interface AdminResult { void done(boolean isAdmin, Exception error); }
    public interface RoleResult { void done(String role, Exception error); }

    public static boolean isConfigured(@Nullable Context context) {
        try {
            if (context != null) FirebaseApp.initializeApp(context);
            return FirebaseApp.getInstance() != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static FirebaseAuth auth() { return FirebaseAuth.getInstance(); }
    public static FirebaseFirestore db() { return FirebaseFirestore.getInstance(); }

    public static void createProfile(FirebaseUser user, String name) {
        if (user == null || user.getEmail() == null || !user.getEmail().toLowerCase().endsWith("@unisimon.edu.co")) return;
        Map<String, Object> p = new HashMap<>();
        p.put("uid", user.getUid());
        p.put("name", name);
        p.put("email", user.getEmail());
        p.put("createdAt", FieldValue.serverTimestamp());
        db().collection("users").document(user.getUid()).set(p);
    }

    /**
     * Submits a collaborative report (new point, removed point, or moved point).
     * Enforces @unisimon.edu.co verification, user authentication, and daily limit (max 5/day).
     */
    public static void submitReport(@NonNull Map<String, Object> report, Result callback) {
        FirebaseUser user = auth().getCurrentUser();
        if (user == null) {
            callback.done(new IllegalStateException("Debes iniciar sesión con tu cuenta @unisimon.edu.co."));
            return;
        }
        if (user.getEmail() == null || !user.getEmail().toLowerCase().endsWith("@unisimon.edu.co")) {
            callback.done(new IllegalStateException("Solo miembros con correo @unisimon.edu.co pueden enviar reportes."));
            return;
        }
        if (!user.isEmailVerified()) {
            callback.done(new IllegalStateException("Debes verificar tu correo institucional antes de reportar."));
            return;
        }

        // Validate daily limit: maximum 5 reports per user in the last 24 hours
        long oneDayAgoMillis = System.currentTimeMillis() - (24L * 60L * 60L * 1000L);
        db().collection("reports")
                .whereEqualTo("userId", user.getUid())
                .whereGreaterThanOrEqualTo("createdAt", new Date(oneDayAgoMillis))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().size() >= DAILY_REPORT_LIMIT) {
                        callback.done(new IllegalStateException("Has alcanzado el límite diario de " + DAILY_REPORT_LIMIT + " reportes por usuario. Intenta mañana."));
                        return;
                    }

                    // Prepare report record
                    DocumentReference ref = db().collection("reports").document();
                    report.put("id", ref.getId());
                    report.put("userId", user.getUid());
                    report.put("userEmail", user.getEmail());
                    report.put("status", "pending");
                    if (!report.containsKey("confirmations")) {
                        report.put("confirmations", new ArrayList<String>());
                    }
                    report.put("confirmationsCount", 0);
                    if (!report.containsKey("type")) {
                        report.put("type", "new_point");
                    }
                    report.put("createdAt", FieldValue.serverTimestamp());
                    report.put("updatedAt", FieldValue.serverTimestamp());

                    ref.set(report).addOnCompleteListener(t -> callback.done(t.getException()));
                });
    }

    /**
     * Confirms a pending point report by another verified student.
     * Prevents self-confirmation and duplicates.
     * Automatically approves point when reaching 2 distinct confirmations.
     */
    public static void confirmReport(String reportId, Result callback) {
        FirebaseUser user = auth().getCurrentUser();
        if (user == null || !user.isEmailVerified()) {
            callback.done(new IllegalStateException("Debes iniciar sesión con tu correo verificado para confirmar puntos."));
            return;
        }

        DocumentReference ref = db().collection("reports").document(reportId);
        ref.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                callback.done(new IllegalStateException("El reporte no existe o fue retirado."));
                return;
            }

            DocumentSnapshot doc = task.getResult();
            String creatorUid = doc.getString("userId");
            if (user.getUid().equals(creatorUid)) {
                callback.done(new IllegalStateException("No puedes confirmar tu propio reporte."));
                return;
            }

            String currentStatus = doc.getString("status");
            if (!"pending".equals(currentStatus)) {
                callback.done(new IllegalStateException("Este reporte ya no está pendiente de validación."));
                return;
            }

            List<?> rawList = (List<?>) doc.get("confirmations");
            List<String> confirmations = new ArrayList<>();
            if (rawList != null) {
                for (Object item : rawList) {
                    if (item instanceof String) confirmations.add((String) item);
                }
            }

            if (confirmations.contains(user.getUid())) {
                callback.done(new IllegalStateException("Ya has confirmado este punto previamente."));
                return;
            }

            int newCount = confirmations.size() + 1;
            boolean autoApprove = newCount >= CONFIRMATIONS_REQUIRED_FOR_APPROVAL;

            WriteBatch batch = db().batch();
            Map<String, Object> updates = new HashMap<>();
            updates.put("confirmations", FieldValue.arrayUnion(user.getUid()));
            updates.put("confirmationsCount", newCount);
            updates.put("updatedAt", FieldValue.serverTimestamp());

            if (autoApprove) {
                updates.put("status", "approved");
                updates.put("approvedAt", FieldValue.serverTimestamp());
                updates.put("approvedBy", "community_auto_approval");

                String type = doc.getString("type");
                if ("removed_point".equals(type)) {
                    String targetId = doc.getString("targetPointId");
                    if (targetId != null) {
                        DocumentReference target = db().collection("recycling_points").document(targetId);
                        batch.update(target, "active", false, "updatedAt", FieldValue.serverTimestamp());
                    }
                } else if ("moved_point".equals(type)) {
                    String targetId = doc.getString("targetPointId");
                    if (targetId != null) {
                        DocumentReference target = db().collection("recycling_points").document(targetId);
                        Map<String, Object> moveChanges = new HashMap<>();
                        if (doc.get("coordinates") != null) moveChanges.put("coordinates", doc.get("coordinates"));
                        if (doc.getString("location") != null) moveChanges.put("location", doc.getString("location"));
                        moveChanges.put("updatedAt", FieldValue.serverTimestamp());
                        batch.update(target, moveChanges);
                    }
                } else {
                    // New point auto-published
                    DocumentReference pointRef = db().collection("recycling_points").document(doc.getId());
                    Map<String, Object> publicPoint = new HashMap<>(doc.getData());
                    publicPoint.put("id", doc.getId());
                    publicPoint.put("sourceReport", doc.getId());
                    publicPoint.put("active", true);
                    publicPoint.put("publishedAt", FieldValue.serverTimestamp());
                    publicPoint.put("publishedBy", "community");
                    publicPoint.remove("userId");
                    publicPoint.remove("status");
                    batch.set(pointRef, publicPoint);
                }
            }

            batch.update(ref, updates);
            batch.commit().addOnCompleteListener(t -> callback.done(t.getException()));
        });
    }

    public static void myReports(String uid, Documents callback) {
        db().collection("reports")
                .whereEqualTo("userId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(s -> callback.done(s, null))
                .addOnFailureListener(e -> callback.done(null, e));
    }

    public static void pendingReports(Documents callback) {
        db().collection("reports")
                .whereEqualTo("status", "pending")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(s -> callback.done(s, null))
                .addOnFailureListener(e -> callback.done(null, e));
    }

    public static void approvedPoints(Documents callback) {
        db().collection("recycling_points")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(s -> callback.done(s, null))
                .addOnFailureListener(e -> callback.done(null, e));
    }

    public static void role(RoleResult callback) {
        FirebaseUser u = auth().getCurrentUser();
        if (u == null) { callback.done("user", null); return; }
        u.getIdToken(true).addOnSuccessListener(t -> {
            Object r = t.getClaims().get("role");
            callback.done(r instanceof String ? (String) r : "user", null);
        }).addOnFailureListener(e -> callback.done("user", e));
    }

    public static void isAdmin(AdminResult callback) {
        role((r, e) -> callback.done("admin".equals(r) || "superadmin".equals(r), e));
    }

    public static void isSuperAdmin(AdminResult callback) {
        role((r, e) -> callback.done("superadmin".equals(r), e));
    }

    /** Atomic Spark-compatible administrative review. */
    public static void reviewReport(DocumentSnapshot report, String status, String note, Result callback) {
        FirebaseUser reviewer = auth().getCurrentUser();
        if (reviewer == null) { callback.done(new IllegalStateException("Sesión no disponible.")); return; }
        isAdmin((admin, error) -> {
            if (error != null || !admin) {
                callback.done(new SecurityException("Solo administradores autorizados pueden revisar."));
                return;
            }
            WriteBatch batch = db().batch();
            Map<String, Object> change = new HashMap<>();
            change.put("status", status);
            change.put("reviewedBy", reviewer.getUid());
            change.put("reviewedAt", FieldValue.serverTimestamp());
            change.put("reviewNote", note);
            batch.update(report.getReference(), change);

            if ("approved".equals(status)) {
                DocumentReference point = db().collection("recycling_points").document(report.getId());
                Map<String, Object> publicPoint = new HashMap<>(report.getData());
                publicPoint.put("id", point.getId());
                publicPoint.put("sourceReport", report.getId());
                publicPoint.put("active", true);
                publicPoint.put("publishedAt", FieldValue.serverTimestamp());
                publicPoint.put("publishedBy", reviewer.getUid());
                publicPoint.remove("userId");
                publicPoint.remove("status");
                publicPoint.remove("reviewNote");
                batch.set(point, publicPoint);
            }

            Map<String, Object> event = new HashMap<>();
            event.put("reportId", report.getId());
            event.put("action", status);
            event.put("responsibleUser", reviewer.getUid());
            event.put("notes", note);
            event.put("createdAt", FieldValue.serverTimestamp());
            batch.set(db().collection("report_history").document(report.getId()), event);

            Map<String, Object> notification = new HashMap<>();
            notification.put("userId", report.getString("userId"));
            notification.put("reportId", report.getId());
            notification.put("status", status);
            notification.put("message", "Tu reporte fue " + status + ".");
            notification.put("createdAt", FieldValue.serverTimestamp());
            batch.set(db().collection("notifications").document(report.getId()), notification);

            batch.commit().addOnCompleteListener(t -> callback.done(t.getException()));
        });
    }

    public static void requestRoleChange(String email, boolean grant, Result callback) {
        FirebaseUser u = auth().getCurrentUser();
        if (u == null) { callback.done(new IllegalStateException("Sesión no disponible.")); return; }
        isSuperAdmin((allowed, error) -> {
            if (error != null || !allowed) {
                callback.done(new SecurityException("Solo el superadministrador puede solicitar este cambio."));
                return;
            }
            Map<String, Object> request = new HashMap<>();
            request.put("targetEmail", email);
            request.put("action", grant ? "grant_admin" : "revoke_admin");
            request.put("requestedBy", u.getUid());
            request.put("status", "pending");
            request.put("createdAt", FieldValue.serverTimestamp());
            db().collection("role_change_requests").add(request).addOnCompleteListener(t -> callback.done(t.getException()));
        });
    }

    public static void adminHistory(Documents callback) {
        db().collection("admin_history").orderBy("createdAt", Query.Direction.DESCENDING).limit(50).get().addOnSuccessListener(s -> callback.done(s, null)).addOnFailureListener(e -> callback.done(null, e));
    }
}
