package com.example.todolist;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

/** Single-activity navigation with integrated circular profile avatar and smooth motion. */
public class MainActivity extends AppCompatActivity {
    private BottomNavigationView navigation;
    private View toolbar;
    private boolean guest;

    private FrameLayout btnToolbarAvatar;
    private ImageView ivAvatarGuest;
    private TextView tvAvatarInitials;

    private FirebaseAuth.AuthStateListener authStateListener;

    @Override
    protected void onCreate(Bundle state) {
        AppCompatDelegate.setDefaultNightMode(
                getSharedPreferences("EcoScanPrefs", MODE_PRIVATE).getBoolean("dark_mode", false)
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(state);
        setContentView(R.layout.activity_main);

        navigation = findViewById(R.id.bottom_navigation);
        toolbar = findViewById(R.id.toolbar);
        btnToolbarAvatar = findViewById(R.id.btnToolbarAvatar);
        ivAvatarGuest = findViewById(R.id.ivAvatarGuest);
        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);

        ((ImageButton) findViewById(R.id.btnSettings)).setOnClickListener(this::showOptionsMenu);

        // Bottom Navigation: Home, Scan, Map (Profile is accessed via the top-right modern avatar)
        navigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) showApp(new HomeFragment());
            else if (id == R.id.nav_scan) showApp(new ScanFragment());
            else if (id == R.id.nav_map) showApp(new MapFragment());
            return true;
        });

        setupAvatarListener();

        if (state == null) {
            if (FirebaseGateway.isConfigured(this) && FirebaseGateway.auth().getCurrentUser() != null) {
                FirebaseUser user = FirebaseGateway.auth().getCurrentUser();
                if (user.getEmail() != null && user.getEmail().toLowerCase().endsWith(AuthFragment.REQUIRED_DOMAIN)) {
                    enterAuthenticatedExperience();
                } else {
                    FirebaseGateway.auth().signOut();
                    showWelcome();
                }
            } else {
                showWelcome();
            }
        }
    }

    private void setupAvatarListener() {
        if (!FirebaseGateway.isConfigured(this)) {
            updateAvatarUI(null);
            return;
        }

        authStateListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            updateAvatarUI(user);
        };
        FirebaseGateway.auth().addAuthStateListener(authStateListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (authStateListener != null && FirebaseGateway.isConfigured(this)) {
            FirebaseGateway.auth().removeAuthStateListener(authStateListener);
        }
    }

    /** Updates top-right circular avatar: shows initials when signed in, silhouette when guest. */
    public void updateAvatarUI(@Nullable FirebaseUser user) {
        if (btnToolbarAvatar == null) return;

        if (user == null || isGuest()) {
            // Guest or not logged in: show silhouette; tapping opens Login
            ivAvatarGuest.setVisibility(View.VISIBLE);
            tvAvatarInitials.setVisibility(View.GONE);
            btnToolbarAvatar.setOnClickListener(v -> openAuth(false));
        } else {
            // Signed in: show user initials; tapping opens Profile with smooth transition
            ivAvatarGuest.setVisibility(View.GONE);
            tvAvatarInitials.setVisibility(View.VISIBLE);
            tvAvatarInitials.setText(extractInitials(user));
            btnToolbarAvatar.setOnClickListener(this::showAvatarPopupMenu);
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

    public void openProfile() {
        toolbar.setVisibility(View.VISIBLE);
        navigation.setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in_250, R.anim.fade_out_250)
                .replace(R.id.fragment_container, new ProfileFragment())
                .addToBackStack("profile")
                .commit();
    }

    public void showWelcome() {
        guest = false;
        toolbar.setVisibility(View.GONE);
        navigation.setVisibility(View.GONE);
        replace(new WelcomeFragment());
        updateAvatarUI(null);
    }

    public void enterGuestMode() {
        guest = true;
        showApp(new HomeFragment());
        navigation.setSelectedItemId(R.id.nav_home);
        updateAvatarUI(null);
    }

    public void enterAuthenticatedExperience() {
        guest = false;
        showApp(new HomeFragment());
        navigation.setSelectedItemId(R.id.nav_home);
        if (FirebaseGateway.isConfigured(this)) {
            updateAvatarUI(FirebaseGateway.auth().getCurrentUser());
        }
    }

    public void openAuth(boolean register) {
        toolbar.setVisibility(View.VISIBLE);
        navigation.setVisibility(View.GONE);
        replace(AuthFragment.newInstance(register));
    }

    public boolean isGuest() {
        return guest && (FirebaseGateway.auth().getCurrentUser() == null);
    }

    public void requireAccount(String destination) {
        if (!FirebaseGateway.isConfigured(this)) {
            Toast.makeText(this, "Firebase no está configurado aún.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseGateway.auth().getCurrentUser();
        if (user == null) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Cuenta institucional requerida")
                    .setMessage("Para reportar puntos ambientales necesitas iniciar sesión con tu cuenta institucional @unisimon.edu.co.")
                    .setPositiveButton("Iniciar sesión", (d, w) -> openAuth(false))
                    .setNeutralButton("Crear cuenta", (d, w) -> openAuth(true))
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }

        // Check domain
        if (user.getEmail() == null || !user.getEmail().toLowerCase().endsWith(AuthFragment.REQUIRED_DOMAIN)) {
            FirebaseGateway.auth().signOut();
            Toast.makeText(this, "Solo cuentas @unisimon.edu.co son permitidas.", Toast.LENGTH_LONG).show();
            openAuth(false);
            return;
        }

        // Check email verification status
        user.reload().addOnCompleteListener(t -> {
            if (user.isEmailVerified()) {
                if ("reports".equals(destination)) {
                    openMyReports();
                } else {
                    openReportBottomSheet(null);
                }
            } else {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Verifica tu correo institucional")
                        .setMessage("Tu correo (" + user.getEmail() + ") aún no está verificado.\n\nPor seguridad del campus, debes verificar tu correo antes de poder reportar puntos ambientales.")
                        .setPositiveButton("Reenviar correo", (d, w) -> {
                            user.sendEmailVerification().addOnCompleteListener(task -> {
                                Toast.makeText(this,
                                        task.isSuccessful() ? "Correo de verificación enviado a " + user.getEmail() : "Error al enviar correo.",
                                        Toast.LENGTH_LONG).show();
                            });
                        })
                        .setNeutralButton("Ya lo verifiqué", (d, w) -> requireAccount(destination))
                        .setNegativeButton("Cerrar", null)
                        .show();
            }
        });
    }

    public void openReport() {
        openReportBottomSheet(null);
    }

    public void openReportBottomSheet(@Nullable String category) {
        FirebaseUser user = FirebaseGateway.auth().getCurrentUser();
        if (user == null || !user.isEmailVerified()) {
            requireAccount("report");
            return;
        }
        ReportBottomSheetDialogFragment.newInstance(category)
                .show(getSupportFragmentManager(), "report_sheet");
    }

    public void openReportForPoint(String pointId, String pointName, String type) {
        FirebaseUser user = FirebaseGateway.auth().getCurrentUser();
        if (user == null || !user.isEmailVerified()) {
            requireAccount("report");
            return;
        }
        ReportBottomSheetDialogFragment.newInstance(null, type, pointId, pointName)
                .show(getSupportFragmentManager(), "report_sheet");
    }

    public void openMyReports() {
        toolbar.setVisibility(View.VISIBLE);
        navigation.setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in_250, R.anim.fade_out_250)
                .replace(R.id.fragment_container, new MyReportsFragment())
                .addToBackStack("my_reports")
                .commit();
    }

    public void selectTab(int id) {
        navigation.setSelectedItemId(id);
    }

    private void showApp(Fragment f) {
        toolbar.setVisibility(View.VISIBLE);
        navigation.setVisibility(View.VISIBLE);
        replace(f);
    }

    private void replace(Fragment f) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in_250, R.anim.fade_out_250)
                .replace(R.id.fragment_container, f)
                .commit();
    }

    /** Avatar popup menu: Perfil, Mis reportes, Cerrar sesión. */
    private void showAvatarPopupMenu(View anchor) {
        View popupView = getLayoutInflater().inflate(R.layout.popup_avatar_menu, null);
        android.widget.PopupWindow popup = new android.widget.PopupWindow(
                popupView,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popup.setElevation(16f);
        popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        popup.setOutsideTouchable(true);

        popupView.findViewById(R.id.menuAvatarProfile).setOnClickListener(v -> {
            popup.dismiss();
            openProfile();
        });
        popupView.findViewById(R.id.menuAvatarMyReports).setOnClickListener(v -> {
            popup.dismiss();
            openMyReports();
        });
        popupView.findViewById(R.id.menuAvatarLogout).setOnClickListener(v -> {
            popup.dismiss();
            if (FirebaseGateway.isConfigured(this)) {
                FirebaseGateway.auth().signOut();
            }
            showWelcome();
        });

        popup.showAsDropDown(anchor, 0, 12);
    }

    /** Settings menu. */
    private void showOptionsMenu(View anchor) {
        View popupView = getLayoutInflater().inflate(R.layout.popup_settings_menu, null);
        android.widget.PopupWindow popup = new android.widget.PopupWindow(
                popupView,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popup.setElevation(16f);
        popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        popup.setOutsideTouchable(true);

        popupView.findViewById(R.id.menuSettingsTheme).setOnClickListener(v -> {
            popup.dismiss();
            boolean next = !getSharedPreferences("EcoScanPrefs", MODE_PRIVATE).getBoolean("dark_mode", false);
            getSharedPreferences("EcoScanPrefs", MODE_PRIVATE).edit().putBoolean("dark_mode", next).apply();
            AppCompatDelegate.setDefaultNightMode(next ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });
        popupView.findViewById(R.id.menuSettingsImpact).setOnClickListener(v -> {
            popup.dismiss();
            message("Impacto ecológico", "Cada residuo separado correctamente aporta a un campus sostenible.");
        });
        popupView.findViewById(R.id.menuSettingsAbout).setOnClickListener(v -> {
            popup.dismiss();
            message("Acerca de EcoScan", "EcoScan · Universidad Simón Bolívar\nReciclaje inteligente para el campus.");
        });

        popup.showAsDropDown(anchor, 0, 12);
    }

    private void message(String title, String body) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(body)
                .setPositiveButton("Cerrar", null)
                .show();
    }
}
