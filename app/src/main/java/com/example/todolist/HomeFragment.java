package com.example.todolist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseUser;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        MainActivity a = (MainActivity) requireActivity();

        TextView greeting = v.findViewById(R.id.tvHomeGreeting);
        if (a.isGuest()) {
            greeting.setText("Bienvenido, invitado");
        } else {
            FirebaseUser u = FirebaseGateway.auth().getCurrentUser();
            String name = u != null && u.getDisplayName() != null && !u.getDisplayName().isEmpty()
                    ? u.getDisplayName().split(" ")[0]
                    : "estudiante";
            greeting.setText("¡Hola, " + name + "!");
        }

        v.findViewById(R.id.btnGoToScan).setOnClickListener(x -> a.selectTab(R.id.nav_scan));
        v.findViewById(R.id.btnHomePoints).setOnClickListener(x -> a.selectTab(R.id.nav_map));
        v.findViewById(R.id.btnHomeReport).setOnClickListener(x -> a.openReport());

        return v;
    }
}
