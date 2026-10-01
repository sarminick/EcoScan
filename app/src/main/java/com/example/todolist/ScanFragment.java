package com.example.todolist;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

public class ScanFragment extends Fragment {

    private ImageView ivScanIcon;
    private TextView tvScanStatus;
    private ProgressBar pbScanning;
    private MaterialCardView cardResult;

    private ImageView ivResultIcon;
    private TextView tvResultObjectName;
    private TextView tvResultCategory;
    private TextView tvResultBadge;
    private TextView tvResultBin;
    private TextView tvResultInstructions;

    public ScanFragment() {
        // Constructor público vacío requerido
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan, container, false);

        initViews(view);
        setupListeners(view);

        return view;
    }

    private void initViews(View view) {
        ivScanIcon = view.findViewById(R.id.ivScanIcon);
        tvScanStatus = view.findViewById(R.id.tvScanStatus);
        pbScanning = view.findViewById(R.id.pbScanning);
        cardResult = view.findViewById(R.id.cardResult);

        ivResultIcon = view.findViewById(R.id.ivResultIcon);
        tvResultObjectName = view.findViewById(R.id.tvResultObjectName);
        tvResultCategory = view.findViewById(R.id.tvResultCategory);
        tvResultBadge = view.findViewById(R.id.tvResultBadge);
        tvResultBin = view.findViewById(R.id.tvResultBin);
        tvResultInstructions = view.findViewById(R.id.tvResultInstructions);
    }

    private void setupListeners(View view) {
        MaterialButton btnCapturePhoto = view.findViewById(R.id.btnCapturePhoto);
        MaterialButton btnSimulateAI = view.findViewById(R.id.btnSimulateAI);
        MaterialButton btnViewMapFromScan = view.findViewById(R.id.btnViewMapFromScan);

        Chip chipPlastic = view.findViewById(R.id.chipPlastic);
        Chip chipPaper = view.findViewById(R.id.chipPaper);
        Chip chipGlass = view.findViewById(R.id.chipGlass);
        Chip chipOrganic = view.findViewById(R.id.chipOrganic);
        Chip chipNonRecyclable = view.findViewById(R.id.chipNonRecyclable);

        btnCapturePhoto.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Abriendo cámara para capturar residuo...", Toast.LENGTH_SHORT).show();
            simulateAIProcessing(
                    R.drawable.ic_category_plastic,
                    R.color.cat_plastic,
                    "Botella Plástica PET",
                    "Plásticos",
                    true,
                    "Caneca Blanca (Aprovechables)",
                    "1. Vacía cualquier líquido residual.\n2. Enjuaga y deja secar.\n3. Aplasta la botella para reducir espacio y deposítala con la tapa cerrada."
            );
        });

        btnSimulateAI.setOnClickListener(v -> {
            simulateAIProcessing(
                    R.drawable.ic_category_paper,
                    R.color.cat_paper,
                    "Caja de Cartón",
                    "Papel y Cartón",
                    true,
                    "Caneca Blanca (Aprovechables)",
                    "1. Desarma la caja para aplanarla.\n2. Asegúrate de que no tenga restos de grasa o comida.\n3. Deposítala en la caneca blanca."
            );
        });

        chipPlastic.setOnClickListener(v -> simulateAIProcessing(
                R.drawable.ic_category_plastic,
                R.color.cat_plastic,
                "Botella Plástica PET",
                "Plásticos",
                true,
                "Caneca Blanca (Aprovechables)",
                "1. Vacía cualquier líquido residual.\n2. Enjuaga y deja secar.\n3. Aplasta la botella para reducir espacio y deposítala con la tapa cerrada."
        ));

        chipPaper.setOnClickListener(v -> simulateAIProcessing(
                R.drawable.ic_category_paper,
                R.color.cat_paper,
                "Caja de Cartón",
                "Papel y Cartón",
                true,
                "Caneca Blanca (Aprovechables)",
                "1. Desarma la caja para que ocupe menos espacio.\n2. Verifica que esté limpia y seca.\n3. Deposita en la caneca blanca."
        ));

        chipGlass.setOnClickListener(v -> simulateAIProcessing(
                R.drawable.ic_category_glass,
                R.color.cat_glass,
                "Frasco de Vidrio",
                "Vidrio",
                true,
                "Caneca Blanca (Aprovechables)",
                "1. Enjuaga bien para retirar residuos orgánicos.\n2. Retira la tapa metálica o plástica (va por separado si aplica).\n3. Deposita con cuidado en la caneca blanca."
        ));

        chipOrganic.setOnClickListener(v -> simulateAIProcessing(
                R.drawable.ic_category_organic,
                R.color.cat_organic,
                "Cáscara de Fruta",
                "Orgánicos",
                true,
                "Caneca Verde (Orgánicos Aprovechables)",
                "1. Asegúrate de no mezclar con envolturas plásticas o servilletas.\n2. Deposítalo directamente en la caneca verde para compostaje."
        ));

        chipNonRecyclable.setOnClickListener(v -> simulateAIProcessing(
                R.drawable.ic_bin,
                R.color.bin_black_non_recyclable,
                "Servilleta Usada",
                "No Aprovechable",
                false,
                "Caneca Negra (No Aprovechables)",
                "1. Este material no es reciclable debido a la contaminación biológica.\n2. Deposítalo en la caneca negra para disposición final."
        ));

        btnViewMapFromScan.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(R.id.nav_map);
            }
        });
    }

    private void simulateAIProcessing(int iconResId, int iconColorResId, String objectName, String category,
                                     boolean isRecyclable, String bin, String instructions) {
        if (!isAdded() || getContext() == null) return;

        // Estado visual de escaneo
        pbScanning.setVisibility(View.VISIBLE);
        ivScanIcon.setImageResource(R.drawable.ic_scan_search);
        ivScanIcon.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.unisimon_green));
        tvScanStatus.setText(R.string.scan_simulating);
        cardResult.setVisibility(View.GONE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isAdded() || getContext() == null) return;

            pbScanning.setVisibility(View.GONE);
            ivScanIcon.setImageResource(iconResId);
            ivScanIcon.setImageTintList(ContextCompat.getColorStateList(requireContext(), iconColorResId));
            tvScanStatus.setText("Identificación completada");

            // Cargar datos en la tarjeta de resultado
            ivResultIcon.setImageResource(iconResId);
            ivResultIcon.setImageTintList(ContextCompat.getColorStateList(requireContext(), iconColorResId));
            tvResultObjectName.setText(objectName);
            tvResultCategory.setText("Categoría: " + category);

            if (isRecyclable) {
                tvResultBadge.setText("RECICLABLE");
                tvResultBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_recyclable_text));
                tvResultBadge.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.badge_recyclable_bg));
            } else {
                tvResultBadge.setText("NO RECICLABLE");
                tvResultBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_non_recyclable_text));
                tvResultBadge.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.badge_non_recyclable_bg));
            }

            tvResultBin.setText(bin);
            tvResultInstructions.setText(instructions);

            cardResult.setVisibility(View.VISIBLE);
        }, 1000); // 1 segundo de simulación de IA
    }
}
