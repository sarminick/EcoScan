package com.example.todolist;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/** Uses CameraX and ML Kit's on-device labeler; captured photos are not uploaded. */
public class ScanFragment extends Fragment {
    private PreviewView cameraPreview;
    private ImageView ivScanIcon, ivResultIcon;
    private TextView tvScanStatus, tvResultObjectName, tvResultCategory, tvResultBadge, tvResultBin, tvResultInstructions;
    private ProgressBar pbScanning;
    private MaterialCardView cardResult, cardSuggestReport;
    private View btnSuggestReport;
    private ImageCapture imageCapture;
    private ImageLabeler imageLabeler;

    private final ActivityResultLauncher<String> cameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startCamera(); else setStatus("Se necesita permiso de cámara para escanear.");
            });

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan, container, false);
        cameraPreview = view.findViewById(R.id.cameraPreview);
        ivScanIcon = view.findViewById(R.id.ivScanIcon); tvScanStatus = view.findViewById(R.id.tvScanStatus);
        pbScanning = view.findViewById(R.id.pbScanning); cardResult = view.findViewById(R.id.cardResult);
        cardSuggestReport = view.findViewById(R.id.cardSuggestReport); btnSuggestReport = view.findViewById(R.id.btnSuggestReport);
        ivResultIcon = view.findViewById(R.id.ivResultIcon); tvResultObjectName = view.findViewById(R.id.tvResultObjectName);
        tvResultCategory = view.findViewById(R.id.tvResultCategory); tvResultBadge = view.findViewById(R.id.tvResultBadge);
        tvResultBin = view.findViewById(R.id.tvResultBin); tvResultInstructions = view.findViewById(R.id.tvResultInstructions);
        imageLabeler = ImageLabeling.getClient(new ImageLabelerOptions.Builder().setConfidenceThreshold(.55f).build());
        bindActions(view);
        return view;
    }

    @Override public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera();
        else cameraPermission.launch(Manifest.permission.CAMERA);
    }

    private void bindActions(View view) {
        view.findViewById(R.id.btnCapturePhoto).setOnClickListener(v -> captureAndClassify());
        view.findViewById(R.id.btnSimulateAI).setOnClickListener(v -> showResult(eco("Botella plástica", "Plásticos")));
        view.findViewById(R.id.chipPlastic).setOnClickListener(v -> showResult(eco("Botella Plástica PET", "Plásticos")));
        view.findViewById(R.id.chipPaper).setOnClickListener(v -> showResult(eco("Caja de cartón", "Papel y cartón")));
        view.findViewById(R.id.chipGlass).setOnClickListener(v -> showResult(eco("Frasco de vidrio", "Vidrio")));
        view.findViewById(R.id.chipOrganic).setOnClickListener(v -> showResult(eco("Cáscara de fruta", "Orgánicos")));
        view.findViewById(R.id.chipNonRecyclable).setOnClickListener(v -> showResult(nonRecyclable("Servilleta usada")));
        view.findViewById(R.id.chipElectronic).setOnClickListener(v -> showResult(electronics("Aparato eléctrico o electrónico")));
        view.findViewById(R.id.chipBulb).setOnClickListener(v -> showResult(bulb("Bombilla")));
        view.findViewById(R.id.btnViewMapFromScan).setOnClickListener(v -> { if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).selectTab(R.id.nav_map); });
    }

    private void startCamera() {
        if (!isAdded()) return;
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build(); preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();
                provider.unbindAll(); provider.bindToLifecycle(getViewLifecycleOwner(), CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture);
                setStatus("Apunta la cámara y toma una foto");
            } catch (ExecutionException | InterruptedException e) { setStatus("No fue posible iniciar la cámara."); }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void captureAndClassify() {
        if (imageCapture == null) { setStatus("La cámara se está preparando."); return; }
        setLoading(true, "Analizando con IA en el dispositivo…");
        imageCapture.takePicture(ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageCapturedCallback() {
            @Override public void onCaptureSuccess(@NonNull ImageProxy proxy) {
                InputImage image = InputImage.fromMediaImage(proxy.getImage(), proxy.getImageInfo().getRotationDegrees());
                Task<List<ImageLabel>> task = imageLabeler.process(image);
                task.addOnSuccessListener(labels -> { proxy.close(); Classification result = fromLabels(labels);
                    if (result == null) { setLoading(false, "No pude identificar el material. Prueba con mejor luz."); Toast.makeText(requireContext(), "No hubo una clasificación confiable.", Toast.LENGTH_LONG).show(); }
                    else showResult(result);
                }).addOnFailureListener(error -> { proxy.close(); setLoading(false, "No se pudo analizar la foto."); });
            }
            @Override public void onError(@NonNull ImageCaptureException error) { setLoading(false, "No se pudo tomar la foto."); }
        });
    }

    private Classification fromLabels(List<ImageLabel> labels) {
        for (ImageLabel label : labels) {
            String value = label.getText().toLowerCase(Locale.ROOT);
            if (value.contains("battery") || value.contains("bater")) return battery(label.getText());
            if (value.contains("light bulb") || value.contains("bulb") || value.contains("lamp")) return bulb(label.getText());
            if (value.contains("computer") || value.contains("laptop") || value.contains("keyboard") || value.contains("phone") || value.contains("electronic")) return electronics(label.getText());
            if (value.contains("plastic") || value.contains("bottle")) return plastic(label.getText());
            if (value.contains("paper") || value.contains("cardboard") || value.contains("carton")) return eco(label.getText(), "Papel y cartón");
            if (value.contains("glass")) return eco(label.getText(), "Vidrio");
            if (value.contains("food") || value.contains("fruit") || value.contains("vegetable")) return eco(label.getText(), "Orgánicos");
            if (value.contains("can") || value.contains("metal")) return eco(label.getText(), "Metales");
            if (value.contains("tissue") || value.contains("napkin")) return nonRecyclable(label.getText());
        }
        return null;
    }

    private Classification eco(String name, String category) {
        int icon = R.drawable.ic_category_plastic, color = R.color.cat_plastic;
        if (category.contains("Papel")) { icon = R.drawable.ic_category_paper; color = R.color.cat_paper; }
        else if (category.contains("Vidrio")) { icon = R.drawable.ic_category_glass; color = R.color.cat_glass; }
        else if (category.contains("Orgánicos")) { icon = R.drawable.ic_category_organic; color = R.color.cat_organic; }
        String point = category.contains("Orgánicos")
                ? "Verifica con la gestión ambiental del campus antes de depositar orgánicos."
                : "En Sede 2 usa el Punto 4, frente a ECO COMPUTA, en la caneca blanca.";
        return new Classification(name, category, true, "Caneca blanca (aprovechables)", "1. Vacía y limpia el objeto.\n2. Déjalo seco.\n3. " + point, "RECICLABLE", icon, color);
    }
    private Classification plastic(String name) { return new Classification(name, "Plásticos", true, "Puntos 1 o 2 (según sede)", "En Sede 3: Bloque C (Punto 1) recibe botellas y tapas. En Sede 2: primer piso junto al ascensor (Punto 2) recibe botellas plásticas.", "RECICLABLE", R.drawable.ic_category_plastic, R.color.cat_plastic); }
    private Classification electronics(String name) { return new Classification(name, "Residuo electrónico", true, "Punto 3: ECO COMPUTA, Sede 2", "Llévalo a la entrada de la sede 2. ECO COMPUTA recibe exclusivamente aparatos eléctricos y electrónicos. No dejes allí pilas ni baterías.", "PUNTO ESPECIAL", R.drawable.ic_settings, R.color.unisimon_green); }
    private Classification bulb(String name) { return new Classification(name, "Bombilla", true, "Recolector de bombillas, Punto 3", "Llévala a la entrada de la sede 2. El recolector de bombillas está junto a ECO COMPUTA.", "PUNTO ESPECIAL", R.drawable.ic_recycling, R.color.unisimon_gold); }
    private Classification battery(String name) { return new Classification(name, "Pila o batería", false, "No usar ECO COMPUTA", "ECO COMPUTA no recibe pilas ni baterías. Consulta con la gestión ambiental del campus para el punto autorizado.", "NO DEPOSITAR AQUÍ", R.drawable.ic_bin, R.color.bin_black_non_recyclable); }
    private Classification nonRecyclable(String name) { return new Classification(name, "No aprovechable", false, "Punto 4, Sede 2: caneca negra", "Deposítalo en la caneca negra frente a ECO COMPUTA. No lo mezcles con materiales limpios y aprovechables.", "NO APROVECHABLE", R.drawable.ic_bin, R.color.bin_black_non_recyclable); }
    private void showResult(Classification item) {
        setLoading(false, "Identificación completada");
        ivScanIcon.setImageResource(item.icon); ivScanIcon.setImageTintList(ContextCompat.getColorStateList(requireContext(), item.color));
        ivResultIcon.setImageResource(item.icon); ivResultIcon.setImageTintList(ContextCompat.getColorStateList(requireContext(), item.color));
        tvResultObjectName.setText(item.name); tvResultCategory.setText("Categoría: " + item.category); tvResultBin.setText(item.bin); tvResultInstructions.setText(item.instructions);
        tvResultBadge.setText(item.badge);
        int text = item.recyclable ? R.color.badge_recyclable_text : R.color.badge_non_recyclable_text;
        int background = item.recyclable ? R.color.badge_recyclable_bg : R.color.badge_non_recyclable_bg;
        tvResultBadge.setTextColor(ContextCompat.getColor(requireContext(), text)); tvResultBadge.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), background)); cardResult.setVisibility(View.VISIBLE);
        if (cardSuggestReport != null) {
            cardSuggestReport.setVisibility(View.VISIBLE);
            btnSuggestReport.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openReportBottomSheet(item.category);
                }
            });
        }
        appendApprovedRecommendations(item);
    }
    /** Pending or rejected reports are never queried by the scanner. */
    private void appendApprovedRecommendations(Classification item) {
        if (!FirebaseGateway.isConfigured(requireContext())) return;
        FirebaseGateway.approvedPoints((points, error) -> {
            if (error != null || points == null || !isAdded()) return;
            StringBuilder recommendation = new StringBuilder();
            for (com.google.firebase.firestore.DocumentSnapshot point : points) {
                String accepted = point.getString("materialsAccepted");
                if (accepted != null && matches(item.category, accepted.toLowerCase(Locale.ROOT))) {
                    if (recommendation.length() > 0) recommendation.append("\n");
                    recommendation.append("• ").append(point.getString("site")).append(": ").append(point.getString("location"));
                }
            }
            if (recommendation.length() > 0) tvResultInstructions.append("\n\nPuntos comunitarios aprobados:\n" + recommendation);
        });
    }
    private boolean matches(String category, String accepted) {
        if (category.contains("Plástico")) return accepted.contains("plást") || accepted.contains("pet") || accepted.contains("botella");
        if (category.contains("Papel")) return accepted.contains("papel") || accepted.contains("cartón");
        if (category.contains("Vidrio")) return accepted.contains("vidrio");
        if (category.contains("electrónico")) return accepted.contains("electr") || accepted.contains("comput");
        if (category.contains("Bombilla")) return accepted.contains("bombilla");
        return false;
    }
    private void setLoading(boolean loading, String message) { pbScanning.setVisibility(loading ? View.VISIBLE : View.GONE); if (loading) { cardResult.setVisibility(View.GONE); if (cardSuggestReport != null) cardSuggestReport.setVisibility(View.GONE); } setStatus(message); }
    private void setStatus(String message) { if (tvScanStatus != null) tvScanStatus.setText(message); }
    @Override public void onDestroyView() { if (imageLabeler != null) imageLabeler.close(); super.onDestroyView(); }
    private static class Classification { final String name, category, bin, instructions, badge; final boolean recyclable; final int icon, color;
        Classification(String name, String category, boolean recyclable, String bin, String instructions, String badge, int icon, int color) { this.name=name; this.category=category; this.recyclable=recyclable; this.bin=bin; this.instructions=instructions; this.badge=badge; this.icon=icon; this.color=color; } }
}
