package com.example.todolist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/** Entry point: Firebase institutional login / registration or guest mode. */
public class WelcomeFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        View view = inflater.inflate(R.layout.fragment_welcome, parent, false);
        MainActivity a = (MainActivity) requireActivity();
        view.findViewById(R.id.btnWelcomeLogin).setOnClickListener(v -> a.openAuth(false));
        view.findViewById(R.id.btnWelcomeCreate).setOnClickListener(v -> a.openAuth(true));
        view.findViewById(R.id.btnWelcomeGuest).setOnClickListener(v -> a.enterGuestMode());
        return view;
    }
}
