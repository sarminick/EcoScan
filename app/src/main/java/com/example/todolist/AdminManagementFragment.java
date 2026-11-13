package com.example.todolist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;

/** Visible and callable only after a server-issued superadmin claim is verified. */
public class AdminManagementFragment extends Fragment {
    private TextView status, found; private EditText email; private LinearLayout actions, history;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        View view = inflater.inflate(R.layout.fragment_admin_management, parent, false);
        status=view.findViewById(R.id.tvAdminManagementStatus); found=view.findViewById(R.id.tvFoundUser); email=view.findViewById(R.id.etAdminEmail); actions=view.findViewById(R.id.adminActions); history=view.findViewById(R.id.adminHistoryContainer);
        view.findViewById(R.id.btnFindUser).setOnClickListener(v -> find()); view.findViewById(R.id.btnGrantAdmin).setOnClickListener(v -> changeRole(true)); view.findViewById(R.id.btnRevokeAdmin).setOnClickListener(v -> changeRole(false));
        if (!FirebaseGateway.isConfigured(requireContext())) { status.setText("Firebase aún no está configurado."); return view; }
        FirebaseGateway.isSuperAdmin((allowed,error) -> { if(error!=null||!allowed){status.setText("Acceso restringido al superadministrador.");return;} status.setText("Busca un usuario registrado por correo."); loadHistory(); });
        return view;
    }
    private void find() { String value=email.getText().toString().trim(); if(value.isEmpty() || !value.contains("@")){status.setText("Escribe el correo institucional del usuario.");return;} actions.setVisibility(View.VISIBLE); found.setText(value+"\nLa solicitud se revisará con el script seguro del administrador. La app no puede cambiar roles directamente."); }
    private void changeRole(boolean enabled) { String value=email.getText().toString().trim(); status.setText(enabled?"Registrando solicitud de nombramiento…":"Registrando solicitud de retiro…"); FirebaseGateway.requestRoleChange(value,enabled,error->{status.setText(error==null?(enabled?"Solicitud de administrador auxiliar registrada.":"Solicitud de retiro registrada."):"No fue posible registrar la solicitud.");if(error==null){actions.setVisibility(View.GONE);loadHistory();}}); }
    private void loadHistory(){FirebaseGateway.adminHistory((items,error)->{history.removeAllViews();if(error!=null){return;}for(DocumentSnapshot item:items){TextView row=new TextView(requireContext());row.setTextSize(13);row.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_primary));row.setPadding(0,dp(8),0,dp(8));String target=item.getString("reportId");if(target==null)target=item.getString("targetEmail");row.setText(item.getString("action")+" · "+target+"\nResponsable: "+item.getString("actorUid"));history.addView(row);}});}
    private int dp(int value){return(int)(value*getResources().getDisplayMetrics().density+.5f);}
}
