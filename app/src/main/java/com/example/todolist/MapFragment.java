package com.example.todolist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;

public class MapFragment extends Fragment {

    public MapFragment() {
        // Constructor público vacío requerido
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        Chip chipFilterAll = view.findViewById(R.id.chipFilterAll);
        Chip chipFilterWhite = view.findViewById(R.id.chipFilterWhite);
        Chip chipFilterGreen = view.findViewById(R.id.chipFilterGreen);
        Chip chipFilterBlack = view.findViewById(R.id.chipFilterBlack);

        chipFilterAll.setOnClickListener(v -> Toast.makeText(getContext(), "Mostrando todos los puntos ecológicos del campus", Toast.LENGTH_SHORT).show());
        chipFilterWhite.setOnClickListener(v -> Toast.makeText(getContext(), "Filtrando: Puntos con Caneca Blanca (Aprovechables)", Toast.LENGTH_SHORT).show());
        chipFilterGreen.setOnClickListener(v -> Toast.makeText(getContext(), "Filtrando: Puntos con Caneca Verde (Orgánicos)", Toast.LENGTH_SHORT).show());
        chipFilterBlack.setOnClickListener(v -> Toast.makeText(getContext(), "Filtrando: Puntos con Caneca Negra (No aprovechables)", Toast.LENGTH_SHORT).show());

        return view;
    }
}
