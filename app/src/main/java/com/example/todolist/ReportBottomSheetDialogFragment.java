package com.example.todolist;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Integrated Bottom Sheet for collaborative recycling point reporting. */
public class ReportBottomSheetDialogFragment extends BottomSheetDialogFragment {
    private static final String ARG_CATEGORY = "initial_category";
    private static final String ARG_TYPE = "initial_type";
    private static final String ARG_TARGET_ID = "target_point_id";
    private static final String ARG_TARGET_NAME = "target_point_name";

    private RadioGroup rgReportType;
    private LinearLayout layoutTargetPoint, layoutPhotoSection, layoutGpsSection, layoutSiteBlock;
    private Spinner spExistingPoints, spSite, spBlock;
    private EditText etCoordinates, etLocation, etDescription;
    private ImageView ivPhotoPreview;
    private TextView tvStatus, tvSheetTitle, tvPhotoLabel, tvCategoriesLabel;
    private ChipGroup chipGroupMaterials;
    private MaterialButton btnSubmit, btnTakePhoto, btnGetGps;
    private ProgressBar pbLoading;
    private View hsvCategories;

    private String photoBase64 = null;
    private final List<String> existingPointIds = new ArrayList<>();
    private final List<String> existingPointNames = new ArrayList<>();

    private final ActivityResultLauncher<Void> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    ivPhotoPreview.setImageBitmap(bitmap);
                    photoBase64 = bitmapToBase64(bitmap);
                    tvStatus.setText("Foto capturada correctamente.");
                }
            });

    private final ActivityResultLauncher<String> locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) fetchCurrentGps();
                else tvStatus.setText("Permiso de GPS denegado. Puedes ingresar las coordenadas manualmente.");
            });

    public static ReportBottomSheetDialogFragment newInstance(@Nullable String initialCategory) {
        return newInstance(initialCategory, "new_point", null, null);
    }

    public static ReportBottomSheetDialogFragment newInstance(@Nullable String initialCategory,
                                                              @NonNull String type,
                                                              @Nullable String targetId,
                                                              @Nullable String targetName) {
        ReportBottomSheetDialogFragment fragment = new ReportBottomSheetDialogFragment();
        Bundle args = new Bundle();
        if (initialCategory != null) args.putString(ARG_CATEGORY, initialCategory);
        args.putString(ARG_TYPE, type);
        if (targetId != null) args.putString(ARG_TARGET_ID, targetId);
        if (targetName != null) args.putString(ARG_TARGET_NAME, targetName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_report_bottom_sheet, container, false);

        tvSheetTitle = v.findViewById(R.id.tvSheetTitle);
        rgReportType = v.findViewById(R.id.rgReportType);
        layoutTargetPoint = v.findViewById(R.id.layoutTargetPoint);
        layoutPhotoSection = v.findViewById(R.id.layoutPhotoSection);
        layoutGpsSection = v.findViewById(R.id.layoutGpsSection);
        layoutSiteBlock = v.findViewById(R.id.layoutSiteBlock);
        spExistingPoints = v.findViewById(R.id.spExistingPoints);
        spSite = v.findViewById(R.id.spSheetSite);
        spBlock = v.findViewById(R.id.spSheetBlock);
        etCoordinates = v.findViewById(R.id.etCoordinates);
        etLocation = v.findViewById(R.id.etSheetLocation);
        etDescription = v.findViewById(R.id.etSheetDescription);
        ivPhotoPreview = v.findViewById(R.id.ivReportPhotoPreview);
        tvStatus = v.findViewById(R.id.tvSheetStatus);
        tvPhotoLabel = v.findViewById(R.id.tvPhotoLabel);
        tvCategoriesLabel = v.findViewById(R.id.tvCategoriesLabel);
        chipGroupMaterials = v.findViewById(R.id.chipGroupMaterials);
        hsvCategories = v.findViewById(R.id.hsvSheetCategories);
        btnSubmit = v.findViewById(R.id.btnSubmitSheetReport);
        btnTakePhoto = v.findViewById(R.id.btnTakePhoto);
        btnGetGps = v.findViewById(R.id.btnGetGps);
        pbLoading = v.findViewById(R.id.pbSheetLoading);

        setupSpinners();
        setupListeners();
        applyInitialArguments();
        loadExistingPoints();

        // Auto-fetch GPS if permission is already granted
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentGps();
        }

        return v;
    }

    private void setupSpinners() {
        String[] sites = {"Selecciona la sede *", "Sede 2", "Sede 3", "Sede 1", "Sede 4", "Sede 5", "Sede 6"};
        String[] blocks = {"Selecciona el bloque (opcional)", "Bloque A", "Bloque B", "Bloque C", "Bloque D",
                "Bloque E", "Bloque F", "Bloque G", "Bloque H", "Bloque I", "Bloque J", "Bloque K", "Sin bloque / exterior"};

        spSite.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, sites));
        spBlock.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, blocks));
    }

    private void setupListeners() {
        rgReportType.setOnCheckedChangeListener((group, checkedId) -> updateFormForType(checkedId));
        btnTakePhoto.setOnClickListener(v -> takePictureLauncher.launch(null));
        btnGetGps.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fetchCurrentGps();
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        });
        btnSubmit.setOnClickListener(v -> submitReport());
    }

    private void updateFormForType(int checkedId) {
        if (checkedId == R.id.rbNewPoint) {
            tvSheetTitle.setText("Reportar nuevo punto de reciclaje");
            layoutTargetPoint.setVisibility(View.GONE);
            layoutPhotoSection.setVisibility(View.VISIBLE);
            tvPhotoLabel.setText("Foto real del punto *");
            layoutGpsSection.setVisibility(View.VISIBLE);
            layoutSiteBlock.setVisibility(View.VISIBLE);
            tvCategoriesLabel.setVisibility(View.VISIBLE);
            hsvCategories.setVisibility(View.VISIBLE);
            etLocation.setHint("Punto de referencia o ubicación descriptiva *");
        } else if (checkedId == R.id.rbRemovedPoint) {
            tvSheetTitle.setText("Reportar punto de reciclaje retirado");
            layoutTargetPoint.setVisibility(View.VISIBLE);
            layoutPhotoSection.setVisibility(View.VISIBLE);
            tvPhotoLabel.setText("Foto de evidencia (opcional)");
            layoutGpsSection.setVisibility(View.GONE);
            layoutSiteBlock.setVisibility(View.GONE);
            tvCategoriesLabel.setVisibility(View.GONE);
            hsvCategories.setVisibility(View.GONE);
            etLocation.setHint("Motivo del reporte / detalles de retiro *");
        } else if (checkedId == R.id.rbMovedPoint) {
            tvSheetTitle.setText("Reportar punto reubicado o movido");
            layoutTargetPoint.setVisibility(View.VISIBLE);
            layoutPhotoSection.setVisibility(View.VISIBLE);
            tvPhotoLabel.setText("Foto de nueva ubicación (opcional)");
            layoutGpsSection.setVisibility(View.VISIBLE);
            layoutSiteBlock.setVisibility(View.VISIBLE);
            tvCategoriesLabel.setVisibility(View.GONE);
            hsvCategories.setVisibility(View.GONE);
            etLocation.setHint("Nueva ubicación exacta del punto *");
        }
    }

    private void applyInitialArguments() {
        if (getArguments() == null) return;

        String category = getArguments().getString(ARG_CATEGORY);
        if (category != null) {
            selectCategoryChip(category);
        }

        String type = getArguments().getString(ARG_TYPE, "new_point");
        if ("removed_point".equals(type)) {
            rgReportType.check(R.id.rbRemovedPoint);
        } else if ("moved_point".equals(type)) {
            rgReportType.check(R.id.rbMovedPoint);
        } else {
            rgReportType.check(R.id.rbNewPoint);
        }
    }

    private void selectCategoryChip(String category) {
        String catLower = category.toLowerCase(Locale.ROOT);
        if (catLower.contains("plást")) ((Chip) chipGroupMaterials.findViewById(R.id.chipSheetPlastic)).setChecked(true);
        else if (catLower.contains("papel") || catLower.contains("cart")) ((Chip) chipGroupMaterials.findViewById(R.id.chipSheetPaper)).setChecked(true);
        else if (catLower.contains("vidrio")) ((Chip) chipGroupMaterials.findViewById(R.id.chipSheetGlass)).setChecked(true);
        else if (catLower.contains("orgán")) ((Chip) chipGroupMaterials.findViewById(R.id.chipSheetOrganic)).setChecked(true);
        else if (catLower.contains("electr")) ((Chip) chipGroupMaterials.findViewById(R.id.chipSheetElectronics)).setChecked(true);
        else if (catLower.contains("bomb")) ((Chip) chipGroupMaterials.findViewById(R.id.chipSheetBulbs)).setChecked(true);
    }

    private void loadExistingPoints() {
        existingPointIds.clear();
        existingPointNames.clear();
        existingPointNames.add("Selecciona un punto del campus *");
        existingPointIds.add("");

        if (!FirebaseGateway.isConfigured(requireContext())) {
            spExistingPoints.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, existingPointNames));
            return;
        }

        FirebaseGateway.approvedPoints((documents, error) -> {
            if (error != null || documents == null || !isAdded()) return;
            for (com.google.firebase.firestore.DocumentSnapshot doc : documents) {
                String id = doc.getId();
                String name = doc.getString("site") + " — " + doc.getString("location");
                existingPointIds.add(id);
                existingPointNames.add(name);
            }
            if (isAdded()) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, existingPointNames);
                spExistingPoints.setAdapter(adapter);

                String initialTargetId = getArguments() != null ? getArguments().getString(ARG_TARGET_ID) : null;
                if (initialTargetId != null) {
                    int index = existingPointIds.indexOf(initialTargetId);
                    if (index >= 0) spExistingPoints.setSelection(index);
                }
            }
        });
    }

    private void fetchCurrentGps() {
        try {
            LocationManager lm = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
            Location loc = null;
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (loc != null) {
                etCoordinates.setText(String.format(Locale.US, "%.6f, %.6f", loc.getLatitude(), loc.getLongitude()));
                tvStatus.setText("Coordenadas GPS obtenidas.");
            } else {
                tvStatus.setText("Activa el GPS del dispositivo para capturar las coordenadas.");
            }
        } catch (Exception e) {
            tvStatus.setText("No fue posible leer el GPS.");
        }
    }

    private void submitReport() {
        FirebaseUser user = FirebaseGateway.auth().getCurrentUser();
        if (user == null || !user.isEmailVerified()) {
            if (getActivity() instanceof MainActivity) {
                dismiss();
                ((MainActivity) getActivity()).requireAccount("report");
            }
            return;
        }

        int checkedTypeId = rgReportType.getCheckedRadioButtonId();
        String reportType = checkedTypeId == R.id.rbRemovedPoint ? "removed_point"
                : checkedTypeId == R.id.rbMovedPoint ? "moved_point" : "new_point";

        String locationText = etLocation.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String coords = etCoordinates.getText().toString().trim();
        String site = spSite.getSelectedItemPosition() > 0 ? spSite.getSelectedItem().toString() : "";
        String block = spBlock.getSelectedItemPosition() > 0 ? spBlock.getSelectedItem().toString() : "";

        if (TextUtils.isEmpty(locationText)) {
            tvStatus.setText("Ingresa la ubicación descriptiva o punto de referencia.");
            return;
        }

        Map<String, Object> report = new HashMap<>();
        report.put("type", reportType);
        report.put("location", locationText);
        report.put("description", description);

        if ("new_point".equals(reportType)) {
            if (spSite.getSelectedItemPosition() == 0) {
                tvStatus.setText("Selecciona la sede del campus.");
                return;
            }
            List<String> chosenMaterials = getSelectedMaterials();
            if (chosenMaterials.isEmpty()) {
                tvStatus.setText("Selecciona al menos una categoría de residuos que recibe el punto.");
                return;
            }
            if (photoBase64 == null) {
                tvStatus.setText("Por favor toma una fotografía del punto de reciclaje.");
                return;
            }

            report.put("site", site);
            report.put("block", block);
            report.put("materialsAccepted", TextUtils.join(", ", chosenMaterials));
            report.put("photo", photoBase64);
            report.put("coordinates", coords.isEmpty() ? null : coords);

        } else {
            // removed_point or moved_point
            if (spExistingPoints.getSelectedItemPosition() <= 0) {
                tvStatus.setText("Selecciona el punto existente que deseas reportar.");
                return;
            }
            int index = spExistingPoints.getSelectedItemPosition();
            String targetId = existingPointIds.get(index);
            String targetName = existingPointNames.get(index);
            report.put("targetPointId", targetId);
            report.put("targetPointName", targetName);
            report.put("site", site.isEmpty() ? "Campus Unisimón" : site);
            report.put("block", block);
            if (photoBase64 != null) report.put("photo", photoBase64);
            if (!coords.isEmpty()) report.put("coordinates", coords);
        }

        setLoading(true);
        FirebaseGateway.submitReport(report, error -> {
            if (!isAdded()) return;
            setLoading(false);
            if (error == null) {
                Toast.makeText(requireContext(), "¡Reporte enviado! Quedará visible como pendiente hasta que 2 estudiantes o un admin lo confirmen.", Toast.LENGTH_LONG).show();
                dismiss();
            } else {
                tvStatus.setText("Error al enviar: " + error.getMessage());
            }
        });
    }

    private List<String> getSelectedMaterials() {
        List<String> list = new ArrayList<>();
        int[] chipIds = {R.id.chipSheetPlastic, R.id.chipSheetPaper, R.id.chipSheetGlass,
                R.id.chipSheetOrganic, R.id.chipSheetElectronics, R.id.chipSheetBulbs};
        for (int id : chipIds) {
            Chip c = chipGroupMaterials.findViewById(id);
            if (c != null && c.isChecked()) list.add(c.getText().toString());
        }
        return list;
    }

    private void setLoading(boolean loading) {
        pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!loading);
    }

    private static String bitmapToBase64(Bitmap bitmap) {
        // Resize if too large to respect Firestore document size limit (1MB)
        int maxDim = 800;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width > maxDim || height > maxDim) {
            float ratio = (float) width / (float) height;
            if (ratio > 1) {
                width = maxDim;
                height = (int) (maxDim / ratio);
            } else {
                height = maxDim;
                width = (int) (maxDim * ratio);
            }
            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out);
        byte[] bytes = out.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }
}
