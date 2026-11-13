package com.example.todolist;

import android.app.AlertDialog;
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

/** Review is guarded twice: custom claim in the app and Firestore rules on the server. */
public class AdminFragment extends Fragment {
    private TextView status; private LinearLayout container;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        View view=inflater.inflate(R.layout.fragment_reports_list,parent,false); ((TextView)view.findViewById(R.id.tvListTitle)).setText("Panel de administrador"); status=view.findViewById(R.id.tvListStatus); container=view.findViewById(R.id.reportsContainer);
        if (!FirebaseGateway.isConfigured(requireContext())) { status.setText("Firebase aún no está configurado."); return view; }
        FirebaseGateway.isAdmin((admin,error)-> { if(error!=null||!admin){status.setText("Acceso restringido: se requiere el rol administrativo emitido por el servidor.");return;} load(); }); return view;
    }
    private void load(){ status.setText("Cargando reportes pendientes…"); FirebaseGateway.pendingReports((items,error)->{if(error!=null){status.setText("No fue posible cargar los reportes.");return;} status.setText(items.isEmpty()?"No hay reportes pendientes.":"Toca un reporte para revisarlo.");for(DocumentSnapshot item:items)add(item);}); }
    private void add(DocumentSnapshot item){TextView card=new TextView(requireContext());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(10);card.setLayoutParams(p);card.setPadding(dp(14),dp(14),dp(14),dp(14));card.setBackgroundColor(ContextCompat.getColor(requireContext(),R.color.surface_card));card.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_primary));card.setTextSize(15);card.setText(item.getString("site")+" · "+item.getString("block")+"\n"+item.getString("location")+"\nAcepta: "+item.getString("materialsAccepted")+"\nToca para aprobar o rechazar.");card.setOnClickListener(v->review(item));container.addView(card);}
    private void review(DocumentSnapshot report){EditText note=new EditText(requireContext());note.setHint("Motivo u observación (obligatorio al rechazar)");new AlertDialog.Builder(requireContext()).setTitle("Revisar reporte").setView(note).setPositiveButton("Aprobar",(d,w)->save(report,"approved",note.getText().toString())).setNegativeButton("Rechazar",(d,w)->save(report,"rejected",note.getText().toString())).setNeutralButton("Cancelar",null).show();}
    private void save(DocumentSnapshot report,String result,String note){if("rejected".equals(result)&&note.trim().isEmpty()){status.setText("Indica un motivo para el rechazo.");return;}FirebaseGateway.reviewReport(report,result,note,error->{status.setText(error==null?"Reporte "+(result.equals("approved")?"aprobado y publicado.":"rechazado."):"No fue posible revisar el reporte.");if(error==null){container.removeAllViews();load();}});}
    private int dp(int value){return(int)(value*getResources().getDisplayMetrics().density+.5f);}
}
