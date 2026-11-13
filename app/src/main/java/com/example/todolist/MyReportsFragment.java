package com.example.todolist;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class MyReportsFragment extends Fragment {
    private TextView status;
    private LinearLayout container;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        View view = inflater.inflate(R.layout.fragment_reports_list, parent, false);
        ((TextView) view.findViewById(R.id.tvListTitle)).setText("Mis reportes colaborativos");
        status = view.findViewById(R.id.tvListStatus);
        container = view.findViewById(R.id.reportsContainer);

        if (!FirebaseGateway.isConfigured(requireContext())) {
            status.setText("Firebase aún no está configurado.");
            return view;
        }

        FirebaseUser user = FirebaseGateway.auth().getCurrentUser();
        if (user == null) {
            status.setText("Necesitas iniciar sesión para consultar tus reportes.");
            view.post(() -> ((MainActivity) requireActivity()).requireAccount("reports"));
            return view;
        }

        status.setText("Cargando tu historial de reportes…");
        FirebaseGateway.myReports(user.getUid(), (items, error) -> {
            if (!isAdded()) return;
            if (error != null) {
                status.setText("No fue posible cargar tus reportes: " + error.getMessage());
                return;
            }
            container.removeAllViews();
            if (items == null || items.isEmpty()) {
                status.setText("Aún no has enviado reportes.\n\nCuando escanees o explores el campus podrás reportar puntos nuevos, retirados o movidos.");
                return;
            }
            status.setText("Historial de " + items.size() + (items.size() == 1 ? " reporte enviado:" : " reportes enviados:"));
            for (DocumentSnapshot item : items) {
                addCard(item);
            }
        });

        return view;
    }

    private void addCard(DocumentSnapshot item) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(10);
        card.setLayoutParams(p);
        card.setRadius(dp(14));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_card));

        LinearLayout body = new LinearLayout(requireContext());
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(16), dp(16), dp(16));

        // Type & Status Header
        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);

        String rawType = item.getString("type");
        String typeLabel = "removed_point".equals(rawType) ? "Reporte: Punto retirado"
                : "moved_point".equals(rawType) ? "Reporte: Punto movido" : "Reporte: Punto nuevo";

        TextView tvType = new TextView(requireContext());
        tvType.setText(typeLabel);
        tvType.setTextSize(15);
        tvType.setTypeface(null, 1);
        tvType.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        tvType.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        String state = item.getString("status");
        TextView tvBadge = new TextView(requireContext());
        tvBadge.setPadding(dp(8), dp(3), dp(8), dp(3));
        tvBadge.setTextSize(10);
        tvBadge.setTypeface(null, 1);

        List<?> confirmations = (List<?>) item.get("confirmations");
        int confCount = confirmations != null ? confirmations.size() : 0;

        if ("approved".equals(state)) {
            tvBadge.setText("✓ APROBADO");
            tvBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_recyclable_text));
            tvBadge.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.badge_recyclable_bg));
        } else if ("rejected".equals(state)) {
            tvBadge.setText("✕ RECHAZADO");
            tvBadge.setTextColor(0xFFD32F2F);
            tvBadge.setBackgroundColor(0xFFFFCDD2);
        } else {
            tvBadge.setText("⏳ EN VALIDACIÓN (" + confCount + "/2)");
            tvBadge.setTextColor(0xFFE65100);
            tvBadge.setBackgroundColor(0xFFFFECB3);
        }

        header.addView(tvType);
        header.addView(tvBadge);
        body.addView(header);

        // Site and location
        String site = item.getString("site");
        String location = item.getString("location");
        TextView tvLocation = new TextView(requireContext());
        tvLocation.setText((site != null ? site + " — " : "") + (location != null ? location : ""));
        tvLocation.setTextSize(13);
        tvLocation.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        tvLocation.setPadding(0, dp(4), 0, dp(4));
        body.addView(tvLocation);

        // Materials if present
        String materials = item.getString("materialsAccepted");
        if (materials != null && !materials.isEmpty()) {
            TextView tvMat = new TextView(requireContext());
            tvMat.setText("Residuos: " + materials);
            tvMat.setTextSize(12);
            tvMat.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            body.addView(tvMat);
        }

        // Photo thumbnail if present
        String photo = item.getString("photo");
        if (photo != null && !photo.isEmpty()) {
            try {
                byte[] decoded = Base64.decode(photo, Base64.NO_WRAP);
                Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (bmp != null) {
                    ImageView iv = new ImageView(requireContext());
                    LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(140));
                    imgParams.topMargin = dp(8);
                    iv.setLayoutParams(imgParams);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    iv.setImageBitmap(bmp);
                    body.addView(iv);
                }
            } catch (Exception ignored) { }
        }

        // Review note if rejected or reviewed
        String note = item.getString("reviewNote");
        if (note != null && !note.isEmpty()) {
            TextView tvNote = new TextView(requireContext());
            tvNote.setText("Nota de revisión: " + note);
            tvNote.setTextSize(12);
            tvNote.setTextColor(0xFFD32F2F);
            tvNote.setPadding(0, dp(6), 0, 0);
            body.addView(tvNote);
        }

        card.addView(body);
        container.addView(card);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }
}
