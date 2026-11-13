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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

/** Bottom sheet dialog to view details of an approved or pending point and confirm it collaboratively. */
public class PointDetailBottomSheetDialogFragment extends BottomSheetDialogFragment {
    public static final String ARG_ID = "point_id";
    public static final String ARG_NAME = "point_name";
    public static final String ARG_SITE = "point_site";
    public static final String ARG_BLOCK = "point_block";
    public static final String ARG_LOCATION = "point_location";
    public static final String ARG_MATERIALS = "point_materials";
    public static final String ARG_COORDS = "point_coords";
    public static final String ARG_STATUS = "point_status";
    public static final String ARG_PHOTO = "point_photo";
    public static final String ARG_CREATOR_UID = "creator_uid";
    public static final String ARG_CONFIRMATIONS = "confirmations";

    public interface PointActionListener {
        void onPointConfirmed();
    }

    private PointActionListener listener;

    public void setListener(PointActionListener listener) {
        this.listener = listener;
    }

    public static PointDetailBottomSheetDialogFragment newInstance(Bundle args) {
        PointDetailBottomSheetDialogFragment fragment = new PointDetailBottomSheetDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_point_detail_bottom_sheet, container, false);

        Bundle args = getArguments() != null ? getArguments() : new Bundle();
        String id = args.getString(ARG_ID, "");
        String name = args.getString(ARG_NAME, "Punto de reciclaje");
        String site = args.getString(ARG_SITE, "Campus Unisimón");
        String block = args.getString(ARG_BLOCK, "");
        String location = args.getString(ARG_LOCATION, "");
        String materials = args.getString(ARG_MATERIALS, "Residuos aprovechables");
        String coords = args.getString(ARG_COORDS, "");
        String status = args.getString(ARG_STATUS, "approved");
        String photo = args.getString(ARG_PHOTO, null);
        String creatorUid = args.getString(ARG_CREATOR_UID, "");
        ArrayList<String> confirmations = args.getStringArrayList(ARG_CONFIRMATIONS);
        if (confirmations == null) confirmations = new ArrayList<>();

        TextView tvTitle = v.findViewById(R.id.tvDetailTitle);
        TextView tvBadge = v.findViewById(R.id.tvDetailStatusBadge);
        TextView tvSiteBlock = v.findViewById(R.id.tvDetailSiteBlock);
        TextView tvLocation = v.findViewById(R.id.tvDetailLocation);
        TextView tvMaterials = v.findViewById(R.id.tvDetailMaterials);
        TextView tvCoords = v.findViewById(R.id.tvDetailCoords);
        ImageView ivPhoto = v.findViewById(R.id.ivDetailPhoto);

        View cardValidation = v.findViewById(R.id.cardValidationSection);
        TextView tvProgress = v.findViewById(R.id.tvConfirmationProgress);
        MaterialButton btnConfirm = v.findViewById(R.id.btnConfirmPoint);
        TextView tvConfirmStatus = v.findViewById(R.id.tvConfirmStatus);

        LinearLayout layoutApprovedActions = v.findViewById(R.id.layoutApprovedActions);
        MaterialButton btnReportRemoved = v.findViewById(R.id.btnReportRemoved);
        MaterialButton btnReportMoved = v.findViewById(R.id.btnReportMoved);

        tvTitle.setText(name);
        tvSiteBlock.setText(block.isEmpty() ? site : site + " · " + block);
        tvLocation.setText(location);
        tvMaterials.setText(materials);
        tvCoords.setText(coords.isEmpty() ? "" : "GPS: " + coords);
        tvCoords.setVisibility(coords.isEmpty() ? View.GONE : View.VISIBLE);

        if (photo != null && !photo.isEmpty()) {
            try {
                byte[] decoded = Base64.decode(photo, Base64.NO_WRAP);
                Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (bmp != null) {
                    ivPhoto.setImageBitmap(bmp);
                    ivPhoto.setVisibility(View.VISIBLE);
                }
            } catch (Exception ignored) { }
        }

        boolean isPending = "pending".equals(status);
        if (isPending) {
            tvBadge.setText("EN VALIDACIÓN");
            tvBadge.setTextColor(0xFFE65100);
            tvBadge.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.badge_non_recyclable_bg));
            cardValidation.setVisibility(View.VISIBLE);
            layoutApprovedActions.setVisibility(View.GONE);

            int confCount = confirmations.size();
            tvProgress.setText("Confirmaciones: " + confCount + " de " + FirebaseGateway.CONFIRMATIONS_REQUIRED_FOR_APPROVAL + " requeridas para validación automática.");

            FirebaseUser currentUser = FirebaseGateway.auth().getCurrentUser();
            if (currentUser == null) {
                btnConfirm.setText("Inicia sesión para confirmar este punto");
                btnConfirm.setOnClickListener(x -> {
                    dismiss();
                    if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).openAuth(false);
                });
            } else if (currentUser.getUid().equals(creatorUid)) {
                btnConfirm.setEnabled(false);
                btnConfirm.setText("Tú enviaste este reporte");
                tvConfirmStatus.setText("Esperando validación de otros compañeros del campus.");
            } else if (confirmations.contains(currentUser.getUid())) {
                btnConfirm.setEnabled(false);
                btnConfirm.setText("✓ Ya has confirmado este punto");
                tvConfirmStatus.setText("Gracias por tu colaboración comunitaria.");
            } else {
                btnConfirm.setEnabled(true);
                final ArrayList<String> finalConfirmations = confirmations;
                btnConfirm.setOnClickListener(x -> {
                    btnConfirm.setEnabled(false);
                    tvConfirmStatus.setText("Validando...");
                    FirebaseGateway.confirmReport(id, error -> {
                        if (!isAdded()) return;
                        if (error == null) {
                            int updated = finalConfirmations.size() + 1;
                            tvConfirmStatus.setText("¡Confirmación registrada!");
                            Toast.makeText(requireContext(), updated >= 2
                                    ? "¡Punto aprobado automáticamente por la comunidad!"
                                    : "¡Gracias por confirmar la existencia de este punto!", Toast.LENGTH_LONG).show();
                            if (listener != null) listener.onPointConfirmed();
                            dismiss();
                        } else {
                            btnConfirm.setEnabled(true);
                            tvConfirmStatus.setText("Error: " + error.getMessage());
                        }
                    });
                });
            }
        } else {
            tvBadge.setText("APROBADO");
            tvBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_recyclable_text));
            tvBadge.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.badge_recyclable_bg));
            cardValidation.setVisibility(View.GONE);
            layoutApprovedActions.setVisibility(View.VISIBLE);

            btnReportRemoved.setOnClickListener(x -> {
                dismiss();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openReportForPoint(id, name, "removed_point");
                }
            });

            btnReportMoved.setOnClickListener(x -> {
                dismiss();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openReportForPoint(id, name, "moved_point");
                }
            });
        }

        return v;
    }
}
