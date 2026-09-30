package com.example.todolist;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    private final Fragment homeFragment = new HomeFragment();
    private final Fragment scanFragment = new ScanFragment();
    private final Fragment mapFragment = new MapFragment();
    private final Fragment guideFragment = new GuideFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

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
