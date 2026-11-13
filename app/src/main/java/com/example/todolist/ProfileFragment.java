package com.example.todolist;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

/** Profile screen with circular avatar, user details, and collaborative reports access. */
public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);
        MainActivity a = (MainActivity) requireActivity();

        TextView name = v.findViewById(R.id.tvProfileName);
        TextView mail = v.findViewById(R.id.tvProfileEmail);
        TextView role = v.findViewById(R.id.tvProfileRole);
        TextView status = v.findViewById(R.id.tvProfileStatus);
        TextView badge = v.findViewById(R.id.tvProfileVerificationBadge);
        ImageView ivAvatarIcon = v.findViewById(R.id.ivProfileAvatarIcon);
        TextView tvAvatarInitials = v.findViewById(R.id.tvProfileAvatarInitials);

        View cardReports = v.findViewById(R.id.cardProfileReports);
        View reportsBtn = v.findViewById(R.id.btnProfileReports);
        TextView tvReportsCount = v.findViewById(R.id.tvProfileReportsCount);
        TextView tvReportsSummary = v.findViewById(R.id.tvProfileReportsSummary);

        View admin = v.findViewById(R.id.btnProfileAdmin);
        View manage = v.findViewById(R.id.btnProfileManageAdmins);
        View reset = v.findViewById(R.id.btnProfileReset);
        View signOut = v.findViewById(R.id.btnProfileSignOut);
        View resendVerify = v.findViewById(R.id.btnProfileResendVerification);
        View login = v.findViewById(R.id.btnProfileLogin);
        View create = v.findViewById(R.id.btnProfileCreate);

        if (!FirebaseGateway.isConfigured(requireContext())) {
            name.setText("Modo sin conexión");
            mail.setText("Firebase no está configurado.");
            role.setText("Invitado");
            badge.setVisibility(View.GONE);
            status.setText("Agrega google-services.json para habilitar autenticación.");
            cardReports.setVisibility(View.GONE);
            admin.setVisibility(View.GONE);
            manage.setVisibility(View.GONE);
            reset.setVisibility(View.GONE);
            signOut.setVisibility(View.GONE);
            resendVerify.setVisibility(View.GONE);
            login.setVisibility(View.GONE);
            create.setVisibility(View.GONE);
            return v;
        }

        FirebaseUser u = FirebaseGateway.auth().getCurrentUser();

        if (u == null) {
            // Guest mode
            name.setText("Navegas como invitado");
            mail.setText("Comunidad Universidad Simón Bolívar");
            role.setText("Modo invitado");
            badge.setVisibility(View.GONE);
            status.setText("Inicia sesión con tu correo @unisimon.edu.co para reportar y validar puntos.");
            cardReports.setVisibility(View.GONE);
            admin.setVisibility(View.GONE);
            manage.setVisibility(View.GONE);
            reset.setVisibility(View.GONE);
            signOut.setVisibility(View.GONE);
            resendVerify.setVisibility(View.GONE);
            login.setVisibility(View.VISIBLE);
            create.setVisibility(View.VISIBLE);
            ivAvatarIcon.setVisibility(View.VISIBLE);
            tvAvatarInitials.setVisibility(View.GONE);

            login.setOnClickListener(x -> a.openAuth(false));
            create.setOnClickListener(x -> a.openAuth(true));
        } else {
            // Signed in
            login.setVisibility(View.GONE);
            create.setVisibility(View.GONE);
            signOut.setVisibility(View.VISIBLE);
            reset.setVisibility(View.VISIBLE);
            cardReports.setVisibility(View.VISIBLE);

            String displayName = u.getDisplayName();
            name.setText(displayName == null || displayName.isEmpty() ? "Estudiante Unisimón" : displayName);
            mail.setText(u.getEmail());

            // Compute circular avatar initials
            String initials = extractInitials(u);
            ivAvatarIcon.setVisibility(View.GONE);
            tvAvatarInitials.setVisibility(View.VISIBLE);
            tvAvatarInitials.setText(initials);

            // Reload user to verify real-time status
            u.reload().addOnCompleteListener(t -> {
                if (!isAdded()) return;
                updateVerificationUI(u, badge, status, resendVerify);
            });

            updateVerificationUI(u, badge, status, resendVerify);

            // Mis reportes
            reportsBtn.setOnClickListener(x -> a.openMyReports());
            FirebaseGateway.myReports(u.getUid(), (docs, err) -> {
                if (err == null && docs != null && isAdded()) {
                    int count = docs.size();
                    tvReportsCount.setText(count == 0 ? "No has enviado reportes aún" : count + (count == 1 ? " reporte enviado" : " reportes enviados"));
                    tvReportsSummary.setText(count == 0 ? "Ayuda a la comunidad reportando puntos nuevos o cambios en el campus." : "Toca abajo para consultar el estado y validaciones comunitarias.");
                }
            });

            reset.setOnClickListener(x -> {
                if (u.getEmail() != null) {
                    FirebaseGateway.auth().sendPasswordResetEmail(u.getEmail())
                            .addOnCompleteListener(task -> {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(),
                                        task.isSuccessful() ? "Correo de restablecimiento enviado." : "No fue posible enviar el correo.",
                                        Toast.LENGTH_LONG).show();
                            });
                }
            });

            signOut.setOnClickListener(x -> {
                FirebaseGateway.auth().signOut();
                a.showWelcome();
            });

            // Role handling
            role.setText("Cargando rol…");
            FirebaseGateway.role((r, e) -> {
                if (!isAdded()) return;
                boolean isAdmin = "admin".equals(r) || "superadmin".equals(r);
                boolean isSuper = "superadmin".equals(r);
                role.setText(isSuper ? "Superadministrador" : isAdmin ? "Administrador auxiliar" : "Estudiante");
                admin.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                manage.setVisibility(isSuper ? View.VISIBLE : View.GONE);
                admin.setOnClickListener(x -> a.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in_250, R.anim.fade_out_250)
                        .replace(R.id.fragment_container, new AdminFragment())
                        .addToBackStack(null)
                        .commit());
                manage.setOnClickListener(x -> a.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in_250, R.anim.fade_out_250)
                        .replace(R.id.fragment_container, new AdminManagementFragment())
                        .addToBackStack(null)
                        .commit());
            });
        }

        return v;
    }

    private void updateVerificationUI(FirebaseUser u, TextView badge, TextView status, View resendVerify) {
        if (u.isEmailVerified()) {
            badge.setVisibility(View.VISIBLE);
            badge.setText("✓ Correo verificado");
            badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_recyclable_text));
            badge.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.badge_recyclable_bg));
            status.setText("Tu cuenta institucional está activa y autorizada para reportar.");
            resendVerify.setVisibility(View.GONE);
        } else {
            badge.setVisibility(View.VISIBLE);
            badge.setText("⚠ Correo no verificado");
            badge.setTextColor(0xFFD32F2F);
            badge.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.badge_non_recyclable_bg));
            status.setText("Verifica tu correo institucional para poder enviar y confirmar reportes.");
            resendVerify.setVisibility(View.VISIBLE);
            resendVerify.setOnClickListener(x -> {
                u.sendEmailVerification().addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            task.isSuccessful() ? "Correo de verificación reenviado a " + u.getEmail() : "Error al enviar correo",
                            Toast.LENGTH_LONG).show();
                });
            });
        }
    }

    private String extractInitials(@NonNull FirebaseUser user) {
        String name = user.getDisplayName();
        if (TextUtils.isEmpty(name)) {
            String email = user.getEmail();
            if (!TextUtils.isEmpty(email)) {
                name = email.split("@")[0].replace(".", " ");
            } else {
                name = "U";
            }
        }

        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2 && parts[0].length() > 0 && parts[1].length() > 0) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.ROOT);
        } else if (parts.length >= 1 && parts[0].length() > 0) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }
        return "U";
    }
}
