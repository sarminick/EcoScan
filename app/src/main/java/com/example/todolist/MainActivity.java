package com.example.todolist;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "EcoScanPrefs";
    private static final String KEY_DARK_MODE = "dark_mode";

    private BottomNavigationView bottomNavigationView;

    private final Fragment homeFragment = new HomeFragment();
    private final Fragment scanFragment = new ScanFragment();
    private final Fragment mapFragment = new MapFragment();
    private final Fragment guideFragment = new GuideFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Aplicar tema guardado ANTES de setContentView
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(
                darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Configurar botón de settings/opciones
        ImageButton btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(this::showOptionsMenu);
        }

        // Cargar por defecto el fragmento de Inicio
        if (savedInstanceState == null) {
            loadFragment(homeFragment);
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                loadFragment(homeFragment);
                return true;
            } else if (itemId == R.id.nav_scan) {
                loadFragment(scanFragment);
                return true;
            } else if (itemId == R.id.nav_map) {
                loadFragment(mapFragment);
                return true;
            } else if (itemId == R.id.nav_guide) {
                loadFragment(guideFragment);
                return true;
            }
            return false;
        });
    }

    /**
     * Muestra el menú de opciones/configuraciones al hacer clic en el botón de settings.
     */
    private void showOptionsMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.main_menu, popup.getMenu());

        // Actualizar label del toggle de tema según el estado actual
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        popup.getMenu().findItem(R.id.action_toggle_theme)
                .setTitle(darkMode ? "Cambiar a Tema Claro" : "Cambiar a Tema Oscuro");

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_toggle_theme) {
                toggleDarkMode();
                return true;
            } else if (id == R.id.action_tips) {
                showTipsDialog();
                return true;
            } else if (id == R.id.action_impact) {
                showImpactDialog();
                return true;
            } else if (id == R.id.action_about) {
                showAboutDialog();
                return true;
            }
            return false;
        });
        popup.show();
    }

    /**
     * Activa/desactiva el modo oscuro y guarda la preferencia.
     */
    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean currentDark = prefs.getBoolean(KEY_DARK_MODE, false);
        boolean newDark = !currentDark;
        prefs.edit().putBoolean(KEY_DARK_MODE, newDark).apply();
        AppCompatDelegate.setDefaultNightMode(
                newDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    /**
     * Diálogo de Guía Rápida de Separación - opción útil y educativa.
     */
    private void showTipsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Guía Rápida de Separación")
                .setMessage(
                        "Caneca Blanca (Aprovechables):\n" +
                        "Plástico, botellas PET, papel, cartón limpio, vidrio y latas. Deben estar limpios y secos.\n\n" +
                        "Caneca Verde (Orgánicos):\n" +
                        "Cáscaras de frutas, restos de comida y café. Van directo al programa de compostaje del campus.\n\n" +
                        "Caneca Negra (No aprovechables):\n" +
                        "Papel higiénico, servilletas usadas, cajas de pizza engrasadas y empaques con restos de comida."
                )
                .setPositiveButton("Entendido", null)
                .show();
    }

    /**
     * Diálogo de Impacto Ecológico en el campus Unisimón.
     */
    private void showImpactDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Impacto Ecológico Campus")
                .setMessage(
                        "Compromiso Ambiental Unisimón:\n\n" +
                        "• Más de 1.2 toneladas de material aprovechable recuperadas este semestre.\n" +
                        "• 4 estaciones de puntos ecológicos con clasificación por código de colores.\n" +
                        "• Al separar una botella PET evitas que tarde hasta 500 años en descomponerse.\n\n" +
                        "¡Cada residuo que clasificas correctamente cuenta para nuestro campus sostenible!"
                )
                .setPositiveButton("Cerrar", null)
                .show();
    }

    /**
     * Diálogo "Acerca de EcoScan".
     */
    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Acerca de EcoScan")
                .setMessage(
                        "EcoScan v1.0\n\n" +
                        "Sistema inteligente de clasificación de residuos sólidos desarrollado para la comunidad de la " +
                        "Universidad Simón Bolívar (Barranquilla, Colombia).\n\n" +
                        "Alineado con el Código Nacional de Colores (Resolución 2184 de 2019 del Ministerio de Ambiente)."
                )
                .setPositiveButton("Cerrar", null)
                .show();
    }

    /**
     * Permite cambiar la pestaña activa del BottomNavigationView desde cualquier Fragmento.
     * @param navItemId ID del item de menú (ej. R.id.nav_scan, R.id.nav_map, etc.)
     */
    public void selectTab(int navItemId) {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(navItemId);
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}
