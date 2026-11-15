package com.example.todolist;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.example.todolist.classifier.BoundingBoxOverlayView;
import com.example.todolist.classifier.ClassificationResult;
import com.example.todolist.classifier.ClassifierRouter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.FieldValue;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Escáner híbrido de residuos con soporte multi-objeto, CameraX, Gemini + ML Kit,
 * estados visuales (poca luz, análisis láser, no reconocido) y pantalla completa.
 */
public class ScanFragment extends Fragment {

    private NestedScrollView scanScrollView;
    private View layoutScanHeader, layoutChipsContainer, layoutActionButtons;
    private MaterialCardView cardCameraContainer, bannerSuggestMap, cardSuggestReport;
    private PreviewView cameraPreview;
    private ImageView ivCapturedPhoto, ivScanIcon, ivPocaLuzIcon, ivNoReconocidoIcon;
    private BoundingBoxOverlayView boundingBoxOverlay;
    private View viewScanFrame, viewScanLine;
    private TextView badgeOfflineMode, tvScanStatus, tvResultHeader;
    private ProgressBar pbScanning;
    private MaterialButton btnCancelScan, btnPocaLuzFlash, btnPocaLuzRetry, btnNoReconocidoRetry, btnNoReconocidoManual;
    private ImageButton btnScanFlash, btnScanFullscreen;
    private LinearLayout layoutStatusNormal, layoutPocaLuz, layoutNoReconocido, layoutResults, containerResultsList;

    private Camera camera;
    private ImageCapture imageCapture;
    private ClassifierRouter classifierRouter;
    private boolean isTorchOn = false;
    private boolean isFullscreen = false;
    private boolean isAnalyzing = false;

    private ValueAnimator scanLineAnim;
    private ValueAnimator framePulseAnim;
    private ValueAnimator pocaLuzAnim;
    private final List<View> resultCards = new ArrayList<>();

    private OnBackPressedCallback backPressedCallback;

    private final ActivityResultLauncher<String> cameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startCamera();
                else setStatus("Se necesita permiso de cámara para escanear.");
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan, container, false);
        bindViews(view);
        classifierRouter = new ClassifierRouter(requireContext());
        bindActions(view);

        backPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                if (isFullscreen) {
                    toggleFullscreen();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backPressedCallback);

        return view;
    }

    private void bindViews(View view) {
        scanScrollView = view.findViewById(R.id.scanScrollView);
        layoutScanHeader = view.findViewById(R.id.layoutScanHeader);
        layoutChipsContainer = view.findViewById(R.id.layoutChipsContainer);
        layoutActionButtons = view.findViewById(R.id.layoutActionButtons);
        cardCameraContainer = view.findViewById(R.id.cardCameraContainer);
        cameraPreview = view.findViewById(R.id.cameraPreview);
        ivCapturedPhoto = view.findViewById(R.id.ivCapturedPhoto);
        boundingBoxOverlay = view.findViewById(R.id.boundingBoxOverlay);
        viewScanFrame = view.findViewById(R.id.viewScanFrame);
        viewScanLine = view.findViewById(R.id.viewScanLine);
        badgeOfflineMode = view.findViewById(R.id.badgeOfflineMode);
        btnScanFlash = view.findViewById(R.id.btnScanFlash);
        btnScanFullscreen = view.findViewById(R.id.btnScanFullscreen);
        layoutStatusNormal = view.findViewById(R.id.layoutStatusNormal);
        ivScanIcon = view.findViewById(R.id.ivScanIcon);
        tvScanStatus = view.findViewById(R.id.tvScanStatus);
        pbScanning = view.findViewById(R.id.pbScanning);
        btnCancelScan = view.findViewById(R.id.btnCancelScan);

        layoutPocaLuz = view.findViewById(R.id.layoutPocaLuz);
        ivPocaLuzIcon = view.findViewById(R.id.ivPocaLuzIcon);
        btnPocaLuzFlash = view.findViewById(R.id.btnPocaLuzFlash);
        btnPocaLuzRetry = view.findViewById(R.id.btnPocaLuzRetry);

        layoutNoReconocido = view.findViewById(R.id.layoutNoReconocido);
        ivNoReconocidoIcon = view.findViewById(R.id.ivNoReconocidoIcon);
        btnNoReconocidoRetry = view.findViewById(R.id.btnNoReconocidoRetry);
        btnNoReconocidoManual = view.findViewById(R.id.btnNoReconocidoManual);

        layoutResults = view.findViewById(R.id.layoutResults);
        tvResultHeader = view.findViewById(R.id.tvResultHeader);
        containerResultsList = view.findViewById(R.id.containerResultsList);
        bannerSuggestMap = view.findViewById(R.id.bannerSuggestMap);
        cardSuggestReport = view.findViewById(R.id.cardSuggestReport);
    }

    private void bindActions(View view) {
        view.findViewById(R.id.btnCapturePhoto).setOnClickListener(v -> captureAndClassify());
        view.findViewById(R.id.btnSimulateAI).setOnClickListener(v -> simulateMultiObjectDemo());

        btnScanFlash.setOnClickListener(v -> toggleTorch());
        btnPocaLuzFlash.setOnClickListener(v -> toggleTorch());
        btnPocaLuzRetry.setOnClickListener(v -> resetToCamera());
        btnNoReconocidoRetry.setOnClickListener(v -> resetToCamera());
        btnNoReconocidoManual.setOnClickListener(v -> showManualCategoryDialog(null));
        btnCancelScan.setOnClickListener(v -> cancelAnalysis());
        btnScanFullscreen.setOnClickListener(v -> toggleFullscreen());

        // Chips de ejemplo
        view.findViewById(R.id.chipPlastic).setOnClickListener(v -> showSingleResult(sampleResult("Botella Plástica PET", "plastico", true, "blanca")));
        view.findViewById(R.id.chipPaper).setOnClickListener(v -> showSingleResult(sampleResult("Caja de Cartón", "papel_carton", true, "blanca")));
        view.findViewById(R.id.chipGlass).setOnClickListener(v -> showSingleResult(sampleResult("Frasco de Vidrio", "vidrio", true, "blanca")));
        view.findViewById(R.id.chipOrganic).setOnClickListener(v -> showSingleResult(sampleResult("Cáscara de Fruta", "organico", true, "verde")));
        view.findViewById(R.id.chipNonRecyclable).setOnClickListener(v -> showSingleResult(sampleResult("Servilleta Sucia", "no_reciclable", false, "negra")));
        view.findViewById(R.id.chipElectronic).setOnClickListener(v -> showSingleResult(sampleResult("Aparato Electrónico", "peligroso", false, "negra")));
        view.findViewById(R.id.chipBulb).setOnClickListener(v -> showSingleResult(sampleResult("Bombilla", "peligroso", false, "negra")));

        view.findViewById(R.id.btnDismissSuggestMap).setOnClickListener(v -> dismissSuggestBanner());

        boundingBoxOverlay.setListener(index -> {
            if (index >= 0 && index < resultCards.size()) {
                highlightCard(index);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        if (!isAdded()) return;
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                provider.unbindAll();
                camera = provider.bindToLifecycle(getViewLifecycleOwner(), CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture);
                resetToCamera();
            } catch (ExecutionException | InterruptedException e) {
                setStatus("No fue posible iniciar la cámara.");
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void toggleTorch() {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            isTorchOn = !isTorchOn;
            camera.getCameraControl().enableTorch(isTorchOn);
            btnScanFlash.setImageResource(R.drawable.ic_flash_on);
            if (btnPocaLuzFlash != null) {
                btnPocaLuzFlash.setText(isTorchOn ? "Apagar flash" : "Activar flash");
            }
        }
    }

    private void captureAndClassify() {
        if (imageCapture == null || isAnalyzing) return;
        isAnalyzing = true;

        imageCapture.takePicture(ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy proxy) {
                Bitmap bmp = imageProxyToBitmap(proxy);
                proxy.close();
                if (bmp == null) {
                    isAnalyzing = false;
                    setStatus("Error procesando imagen.");
                    return;
                }

                // 1. Verificación previa de luminancia
                float meanLum = calculateMeanLuminance(bmp);
                if (meanLum < 60f) {
                    isAnalyzing = false;
                    showPocaLuzState(bmp);
                    return;
                }

                // 2. Estado ANALIZANDO
                startAnalyzingState(bmp);

                // 3. Clasificación híbrida
                classifierRouter.classify(bmp, new ClassifierRouter.RouterCallback() {
                    @Override
                    public void onResult(List<ClassificationResult> results, boolean isOffline, @Nullable String message) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            isAnalyzing = false;
                            stopAnalysisAnimations();
                            if (message != null) {
                                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                            }
                            handleClassificationOutcome(results, isOffline);
                        });
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            isAnalyzing = false;
                            stopAnalysisAnimations();
                            showNoReconocidoState();
                        });
                    }
                });
            }

            @Override
            public void onError(@NonNull ImageCaptureException error) {
                isAnalyzing = false;
                setStatus("No se pudo tomar la foto.");
            }
        });
    }

    private float calculateMeanLuminance(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int stepX = Math.max(1, width / 24);
        int stepY = Math.max(1, height / 24);
        long sum = 0;
        int count = 0;
        for (int y = 0; y < height; y += stepY) {
            for (int x = 0; x < width; x += stepX) {
                int p = bitmap.getPixel(x, y);
                int r = (p >> 16) & 0xff;
                int g = (p >> 8) & 0xff;
                int b = p & 0xff;
                sum += (long) (0.299f * r + 0.587f * g + 0.114f * b);
                count++;
            }
        }
        return count > 0 ? (float) sum / count : 255f;
    }

    private void startAnalyzingState(Bitmap frozenBitmap) {
        ivCapturedPhoto.setImageBitmap(frozenBitmap);
        ivCapturedPhoto.setVisibility(View.VISIBLE);
        boundingBoxOverlay.setVisibility(View.GONE);
        layoutResults.setVisibility(View.GONE);
        bannerSuggestMap.setVisibility(View.GONE);
        cardSuggestReport.setVisibility(View.GONE);

        layoutPocaLuz.setVisibility(View.GONE);
        layoutNoReconocido.setVisibility(View.GONE);
        layoutStatusNormal.setVisibility(View.VISIBLE);
        pbScanning.setVisibility(View.VISIBLE);
        btnCancelScan.setVisibility(View.VISIBLE);
        setStatus("Analizando residuos...");

        // Línea de escaneo láser en loop
        viewScanLine.setVisibility(View.VISIBLE);
        int height = cardCameraContainer.getHeight() > 0 ? cardCameraContainer.getHeight() : 400;
        scanLineAnim = ObjectAnimator.ofFloat(viewScanLine, "translationY", 0f, (float) height - 30);
        scanLineAnim.setDuration(1200);
        scanLineAnim.setRepeatCount(ValueAnimator.INFINITE);
        scanLineAnim.setRepeatMode(ValueAnimator.REVERSE);
        scanLineAnim.start();

        // Pulso suave en el marco punteado
        framePulseAnim = ObjectAnimator.ofFloat(viewScanFrame, "alpha", 0.5f, 1f);
        framePulseAnim.setDuration(600);
        framePulseAnim.setRepeatCount(ValueAnimator.INFINITE);
        framePulseAnim.setRepeatMode(ValueAnimator.REVERSE);
        framePulseAnim.start();
    }

    private void stopAnalysisAnimations() {
        if (scanLineAnim != null) { scanLineAnim.cancel(); scanLineAnim = null; }
        if (framePulseAnim != null) { framePulseAnim.cancel(); framePulseAnim = null; }
        viewScanLine.setVisibility(View.GONE);
        viewScanFrame.setAlpha(1f);
        pbScanning.setVisibility(View.GONE);
        btnCancelScan.setVisibility(View.GONE);
    }

    private void cancelAnalysis() {
        isAnalyzing = false;
        stopAnalysisAnimations();
        resetToCamera();
    }

    private void showPocaLuzState(Bitmap frozenBmp) {
        stopAnalysisAnimations();
        ivCapturedPhoto.setImageBitmap(frozenBmp);
        ivCapturedPhoto.setVisibility(View.VISIBLE);
        layoutStatusNormal.setVisibility(View.GONE);
        layoutNoReconocido.setVisibility(View.GONE);
        layoutResults.setVisibility(View.GONE);
        boundingBoxOverlay.setVisibility(View.GONE);
        layoutPocaLuz.setVisibility(View.VISIBLE);

        pocaLuzAnim = ValueAnimator.ofFloat(0.85f, 1.15f);
        pocaLuzAnim.setDuration(800);
        pocaLuzAnim.setRepeatCount(ValueAnimator.INFINITE);
        pocaLuzAnim.setRepeatMode(ValueAnimator.REVERSE);
        pocaLuzAnim.addUpdateListener(anim -> {
            float s = (float) anim.getAnimatedValue();
            ivPocaLuzIcon.setScaleX(s);
            ivPocaLuzIcon.setScaleY(s);
        });
        pocaLuzAnim.start();
    }

    private void showNoReconocidoState() {
        stopAnalysisAnimations();
        layoutStatusNormal.setVisibility(View.GONE);
        layoutPocaLuz.setVisibility(View.GONE);
        layoutResults.setVisibility(View.GONE);
        boundingBoxOverlay.setVisibility(View.GONE);
        layoutNoReconocido.setVisibility(View.VISIBLE);

        // Shake corto del marco
        ObjectAnimator shake = ObjectAnimator.ofFloat(viewScanFrame, "translationX", 0f, -25f, 25f, -15f, 15f, -8f, 8f, 0f);
        shake.setDuration(450);
        shake.start();

        // Fade in icono interrogación
        ivNoReconocidoIcon.setAlpha(0f);
        ivNoReconocidoIcon.animate().alpha(1f).setDuration(400).start();
    }

    private void handleClassificationOutcome(List<ClassificationResult> results, boolean isOffline) {
        if (results == null || results.isEmpty()) {
            showNoReconocidoState();
            return;
        }

        // Si todos los objetos tienen confianza < 0.7 -> estado NO_RECONOCIDO
        boolean allLowConfidence = true;
        for (ClassificationResult r : results) {
            if (r.confianza >= 0.7f) {
                allLowConfidence = false;
                break;
            }
        }

        if (allLowConfidence) {
            showNoReconocidoState();
            return;
        }

        // Mostrar resultados
        displayResults(results, isOffline);
    }

    private void displayResults(List<ClassificationResult> results, boolean isOffline) {
        layoutStatusNormal.setVisibility(View.GONE);
        layoutPocaLuz.setVisibility(View.GONE);
        layoutNoReconocido.setVisibility(View.GONE);

        badgeOfflineMode.setVisibility(isOffline ? View.VISIBLE : View.GONE);
        boundingBoxOverlay.setVisibility(View.VISIBLE);
        boundingBoxOverlay.setResults(results);

        layoutResults.setVisibility(View.VISIBLE);
        int count = results.size();
        tvResultHeader.setText(count > 1 ? "Encontré " + count + " objetos" : "Residuo detectado");

        containerResultsList.removeAllViews();
        resultCards.clear();

        Set<String> detectedCategories = new HashSet<>();

        for (int i = 0; i < results.size(); i++) {
            ClassificationResult item = results.get(i);
            detectedCategories.add(item.categoria);
            View card = createResultCard(item, i, isOffline);
            resultCards.add(card);
            containerResultsList.addView(card);
        }

        // Sugerencia hacia Puntos Verdes tras escaneo exitoso
        boolean hasReliableItem = false;
        for (ClassificationResult r : results) {
            if (r.confianza >= 0.7f) {
                hasReliableItem = true;
                break;
            }
        }

        if (hasReliableItem) {
            showSuggestMapBanner(new ArrayList<>(detectedCategories));
        } else {
            bannerSuggestMap.setVisibility(View.GONE);
        }

        if (cardSuggestReport != null) {
            cardSuggestReport.setVisibility(View.VISIBLE);
            cardSuggestReport.findViewById(R.id.btnSuggestReport).setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity && !results.isEmpty()) {
                    ((MainActivity) getActivity()).openReportBottomSheet(results.get(0).categoria);
                }
            });
        }
    }

    private View createResultCard(ClassificationResult item, int index, boolean isOffline) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(10);
        card.setLayoutParams(params);
        card.setRadius(dp(16));
        card.setCardElevation(dp(2));

        boolean isUncertain = item.confianza < 0.7f;
        if (isUncertain) {
            card.setBackgroundResource(R.drawable.bg_card_uncertain);
            card.setStrokeWidth(0);
        } else {
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_card));
            card.setStrokeWidth(dp(1));
            card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.unisimon_green_light));
        }

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(14));

        // Header: Badge número + Nombre + Badge Bolsa/Incierto
        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Badge número redondo
        TextView tvNum = new TextView(requireContext());
        tvNum.setText(String.valueOf(index + 1));
        tvNum.setTextColor(Color.WHITE);
        tvNum.setTextSize(12);
        tvNum.setTypeface(null, 1);
        tvNum.setGravity(android.view.Gravity.CENTER);
        int badgeColor = "verde".equals(item.bolsa) ? Color.parseColor("#007A3D")
                : "blanca".equals(item.bolsa) ? Color.parseColor("#64748B") : Color.parseColor("#1E293B");
        tvNum.setBackground(createCircleDrawable(badgeColor));
        LinearLayout.LayoutParams numParams = new LinearLayout.LayoutParams(dp(24), dp(24));
        numParams.rightMargin = dp(10);
        tvNum.setLayoutParams(numParams);
        header.addView(tvNum);

        // Nombre y Categoría
        LinearLayout colName = new LinearLayout(requireContext());
        colName.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colName.setLayoutParams(nameParams);

        TextView tvName = new TextView(requireContext());
        tvName.setText(item.objeto);
        tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        tvName.setTextSize(16);
        tvName.setTypeface(null, 1);
        colName.addView(tvName);

        TextView tvCat = new TextView(requireContext());
        tvCat.setText("Categoría: " + categoryReadable(item.categoria));
        tvCat.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        tvCat.setTextSize(12);
        colName.addView(tvCat);

        header.addView(colName);

        // Badge bolsa o incierto
        TextView tvBadge = new TextView(requireContext());
        tvBadge.setPadding(dp(8), dp(4), dp(8), dp(4));
        tvBadge.setTextSize(11);
        tvBadge.setTypeface(null, 1);
        if (isUncertain) {
            tvBadge.setText("No estoy seguro");
            tvBadge.setTextColor(Color.parseColor("#64748B"));
            tvBadge.setBackgroundColor(Color.parseColor("#F1F5F9"));
        } else {
            String bolsaText = "Bolsa " + item.bolsa.toLowerCase();
            tvBadge.setText(bolsaText.toUpperCase());
            if ("blanca".equals(item.bolsa)) {
                tvBadge.setTextColor(Color.parseColor("#0F172A"));
                tvBadge.setBackgroundColor(Color.parseColor("#E2E8F0"));
            } else if ("verde".equals(item.bolsa)) {
                tvBadge.setTextColor(Color.parseColor("#15803D"));
                tvBadge.setBackgroundColor(Color.parseColor("#DCFCE7"));
            } else {
                tvBadge.setTextColor(Color.WHITE);
                tvBadge.setBackgroundColor(Color.parseColor("#1E293B"));
            }
        }
        header.addView(tvBadge);
        root.addView(header);

        // Instrucción de cómo desechar
        TextView tvDesechar = new TextView(requireContext());
        tvDesechar.setText(item.comoDesechar);
        tvDesechar.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        tvDesechar.setTextSize(13);
        tvDesechar.setPadding(0, dp(10), 0, dp(6));
        root.addView(tvDesechar);

        // Si es incierto: botón "Elegir categoría"
        if (isUncertain) {
            MaterialButton btnElegir = new MaterialButton(requireContext());
            btnElegir.setText("Elegir categoría");
            btnElegir.setTextSize(12);
            btnElegir.setIconResource(R.drawable.ic_settings);
            btnElegir.setOnClickListener(v -> showManualCategoryDialog(item));
            root.addView(btnElegir);
        }

        // Sección de Feedback (Punto 9)
        LinearLayout feedbackLayout = new LinearLayout(requireContext());
        feedbackLayout.setOrientation(LinearLayout.HORIZONTAL);
        feedbackLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        feedbackLayout.setPadding(0, dp(6), 0, 0);

        TextView tvFeedbackHint = new TextView(requireContext());
        tvFeedbackHint.setText("¿Es correcto?");
        tvFeedbackHint.setTextSize(12);
        tvFeedbackHint.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        tvFeedbackHint.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        feedbackLayout.addView(tvFeedbackHint);

        MaterialButton btnCorrect = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.borderlessButtonStyle);
        btnCorrect.setText("👍 Correcto");
        btnCorrect.setTextSize(11);
        btnCorrect.setOnClickListener(v -> {
            btnCorrect.setEnabled(false);
            Toast.makeText(requireContext(), "¡Gracias por confirmar!", Toast.LENGTH_SHORT).show();
        });
        feedbackLayout.addView(btnCorrect);

        MaterialButton btnIncorrect = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.borderlessButtonStyle);
        btnIncorrect.setText("👎 Incorrecto");
        btnIncorrect.setTextSize(11);
        btnIncorrect.setTextColor(ContextCompat.getColor(requireContext(), R.color.delete_red));
        btnIncorrect.setOnClickListener(v -> {
            btnIncorrect.setEnabled(false);
            saveFeedback(item.objeto, item.categoria, isOffline ? "offline" : "gemini");
            Toast.makeText(requireContext(), "Feedback registrado. Nos ayuda a mejorar el modelo.", Toast.LENGTH_SHORT).show();
        });
        feedbackLayout.addView(btnIncorrect);

        root.addView(feedbackLayout);

        card.addView(root);

        // Click en la tarjeta resalta el recuadro correspondiente
        card.setOnClickListener(v -> {
            highlightCard(index);
            boundingBoxOverlay.setSelectedIndex(index);
        });

        return card;
    }

    private void highlightCard(int index) {
        for (int i = 0; i < resultCards.size(); i++) {
            View c = resultCards.get(i);
            if (c instanceof MaterialCardView) {
                MaterialCardView mcv = (MaterialCardView) c;
                if (i == index) {
                    mcv.setStrokeColor(Color.parseColor("#F59E0B"));
                    mcv.setStrokeWidth(dp(2));
                    mcv.setCardElevation(dp(4));
                } else {
                    mcv.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.unisimon_green_light));
                    mcv.setStrokeWidth(dp(1));
                    mcv.setCardElevation(dp(2));
                }
            }
        }
        if (index >= 0 && index < resultCards.size()) {
            View target = resultCards.get(index);
            scanScrollView.smoothScrollTo(0, target.getTop() + layoutResults.getTop());
        }
    }

    private void showSuggestMapBanner(ArrayList<String> categories) {
        bannerSuggestMap.setVisibility(View.VISIBLE);
        bannerSuggestMap.setTranslationY(150f);
        bannerSuggestMap.setAlpha(0f);
        bannerSuggestMap.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(400)
                .start();

        bannerSuggestMap.findViewById(R.id.btnGoToMap).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToMapWithCategories(categories);
            }
        });
    }

    private void dismissSuggestBanner() {
        bannerSuggestMap.animate()
                .alpha(0f)
                .translationY(80f)
                .setDuration(300)
                .withEndAction(() -> bannerSuggestMap.setVisibility(View.GONE))
                .start();
    }

    private void saveFeedback(String objeto, String categoria, String fuente) {
        if (!FirebaseGateway.isConfigured(requireContext())) return;
        Map<String, Object> data = new HashMap<>();
        data.put("objeto", objeto);
        data.put("categoria", categoria);
        data.put("fuente", fuente);
        data.put("timestamp", FieldValue.serverTimestamp());

        FirebaseGateway.db().collection("feedback_scan").add(data);
    }

    private void showManualCategoryDialog(@Nullable ClassificationResult target) {
        String[] options = new String[]{
                "Plásticos (Bolsa blanca)",
                "Vidrio (Bolsa blanca)",
                "Papel y cartón (Bolsa blanca)",
                "Orgánicos (Bolsa verde)",
                "Metales (Bolsa blanca)",
                "No aprovechables (Bolsa negra)",
                "Residuos especiales o peligrosos"
        };
        String[] categoryCodes = new String[]{
                "plastico", "vidrio", "papel_carton", "organico", "metal", "no_reciclable", "peligroso"
        };
        String[] bolsas = new String[]{"blanca", "blanca", "blanca", "verde", "blanca", "negra", "negra"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Selecciona la categoría correcta")
                .setItems(options, (dialog, which) -> {
                    String cat = categoryCodes[which];
                    String bolsa = bolsas[which];
                    boolean rec = !"no_reciclable".equals(cat) && !"peligroso".equals(cat);
                    String name = target != null ? target.objeto : "Residuo clasificado manualmente";
                    ClassificationResult manual = new ClassificationResult(name, cat, rec, 1.0f,
                            "Clasificado manualmente como " + options[which], bolsa, target != null ? target.bbox : null);
                    showSingleResult(manual);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void simulateMultiObjectDemo() {
        List<ClassificationResult> demoList = new ArrayList<>();
        demoList.add(new ClassificationResult("Botella Plástica PET", "plastico", true, 0.95f,
                "Vacía el líquido, enjuaga, seca y deposita en la caneca blanca.", "blanca",
                new RectF(0.12f, 0.18f, 0.44f, 0.82f)));

        demoList.add(new ClassificationResult("Cáscara de Manzana", "organico", true, 0.88f,
                "Deposita en la caneca verde de orgánicos aprovechables.", "verde",
                new RectF(0.52f, 0.44f, 0.86f, 0.84f)));

        demoList.add(new ClassificationResult("Vaso de Café Sucio", "no_reciclable", false, 0.82f,
                "Los vasos encerados con residuos van en la caneca negra.", "negra",
                new RectF(0.50f, 0.15f, 0.88f, 0.42f)));

        displayResults(demoList, false);
    }

    private void showSingleResult(ClassificationResult item) {
        List<ClassificationResult> list = new ArrayList<>();
        list.add(item);
        displayResults(list, false);
    }

    private ClassificationResult sampleResult(String name, String cat, boolean rec, String bolsa) {
        String instr = rec ? "Vacía, limpia, seca y deposita en la caneca " + bolsa + "."
                : "Deposita en la caneca " + bolsa + " de residuos no aprovechables o especiales.";
        return new ClassificationResult(name, cat, rec, 0.95f, instr, bolsa, new RectF(0.2f, 0.2f, 0.8f, 0.8f));
    }

    private void toggleFullscreen() {
        isFullscreen = !isFullscreen;
        backPressedCallback.setEnabled(isFullscreen);

        btnScanFullscreen.setImageResource(isFullscreen ? R.drawable.ic_fullscreen_exit : R.drawable.ic_fullscreen);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBarsVisibility(!isFullscreen);
        }

        float targetAlpha = isFullscreen ? 0f : 1f;

        layoutScanHeader.animate().alpha(targetAlpha).setDuration(300).withEndAction(() -> {
            if (isFullscreen) layoutScanHeader.setVisibility(View.GONE);
        }).start();
        if (!isFullscreen) layoutScanHeader.setVisibility(View.VISIBLE);

        layoutChipsContainer.animate().alpha(targetAlpha).setDuration(300).withEndAction(() -> {
            if (isFullscreen) layoutChipsContainer.setVisibility(View.GONE);
        }).start();
        if (!isFullscreen) layoutChipsContainer.setVisibility(View.VISIBLE);

        int initialH = cardCameraContainer.getHeight();
        int targetH = isFullscreen ? Math.round(getResources().getDisplayMetrics().heightPixels * 0.76f) : dp(260);

        ValueAnimator anim = ValueAnimator.ofInt(initialH, targetH);
        anim.setDuration(300);
        anim.addUpdateListener(a -> {
            ViewGroup.LayoutParams p = cardCameraContainer.getLayoutParams();
            p.height = (int) a.getAnimatedValue();
            cardCameraContainer.setLayoutParams(p);
        });
        anim.start();
    }

    private void resetToCamera() {
        stopAnalysisAnimations();
        ivCapturedPhoto.setVisibility(View.GONE);
        boundingBoxOverlay.setVisibility(View.GONE);
        layoutStatusNormal.setVisibility(View.VISIBLE);
        layoutPocaLuz.setVisibility(View.GONE);
        layoutNoReconocido.setVisibility(View.GONE);
        layoutResults.setVisibility(View.GONE);
        bannerSuggestMap.setVisibility(View.GONE);
        badgeOfflineMode.setVisibility(View.GONE);
        setStatus("Apunta la cámara y toma una foto");
    }

    private void setStatus(String text) {
        if (tvScanStatus != null) tvScanStatus.setText(text);
    }

    private String categoryReadable(String cat) {
        if ("plastico".equals(cat)) return "Plásticos";
        if ("vidrio".equals(cat)) return "Vidrio";
        if ("papel_carton".equals(cat)) return "Papel y cartón";
        if ("organico".equals(cat)) return "Orgánicos";
        if ("metal".equals(cat)) return "Metales";
        if ("peligroso".equals(cat)) return "Residuo peligroso / especial";
        return "No aprovechable";
    }

    private android.graphics.drawable.Drawable createCircleDrawable(int color) {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        gd.setColor(color);
        return gd;
    }

    private Bitmap imageProxyToBitmap(ImageProxy proxy) {
        try {
            ByteBuffer buffer = proxy.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            // Corregir orientación si viene rotada
            int rotation = proxy.getImageInfo().getRotationDegrees();
            if (rotation != 0 && bitmap != null) {
                android.graphics.Matrix matrix = new android.graphics.Matrix();
                matrix.postRotate(rotation);
                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        stopAnalysisAnimations();
        if (pocaLuzAnim != null) { pocaLuzAnim.cancel(); pocaLuzAnim = null; }
        if (boundingBoxOverlay != null) { boundingBoxOverlay.cancelAnimations(); }
        super.onDestroyView();
    }
}
