package com.example.todolist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class HomeFragment extends Fragment {

    public HomeFragment() {
        // Constructor público vacío requerido
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        MaterialButton btnGoToScan = view.findViewById(R.id.btnGoToScan);
        MaterialCardView cardCategoryPlastic = view.findViewById(R.id.cardCategoryPlastic);
        MaterialCardView cardCategoryGlass = view.findViewById(R.id.cardCategoryGlass);
        MaterialCardView cardCategoryPaper = view.findViewById(R.id.cardCategoryPaper);
        MaterialCardView cardCategoryOrganic = view.findViewById(R.id.cardCategoryOrganic);

        // Navegación directa hacia el escáner
        btnGoToScan.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(R.id.nav_scan);
            }
        });

        // Clics en tarjetas llevan a la Guía Ambiental
        View.OnClickListener goToGuideListener = v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(R.id.nav_guide);
            }
        };

        cardCategoryPlastic.setOnClickListener(goToGuideListener);
        cardCategoryGlass.setOnClickListener(goToGuideListener);
        cardCategoryPaper.setOnClickListener(goToGuideListener);
        cardCategoryOrganic.setOnClickListener(goToGuideListener);

        return view;
    }
}
