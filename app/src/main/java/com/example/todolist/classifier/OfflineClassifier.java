package com.example.todolist.classifier;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Clasificador fuera de línea usando ML Kit Object Detection + Image Labeling on-device.
 */
public class OfflineClassifier implements WasteClassifier {
    private final ObjectDetector objectDetector;
    private final ImageLabeler imageLabeler;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static class CategoryMeta {
        final String category;
        final String bolsa;
        final boolean reciclable;
        final String comoDesechar;
        final String defaultName;

        CategoryMeta(String category, String bolsa, boolean reciclable, String comoDesechar, String defaultName) {
            this.category = category;
            this.bolsa = bolsa;
            this.reciclable = reciclable;
            this.comoDesechar = comoDesechar;
            this.defaultName = defaultName;
        }
    }

    private static final Map<String, CategoryMeta> STATIC_MAP = new HashMap<>();

    static {
        CategoryMeta plastic = new CategoryMeta("plastico", "blanca", true,
                "Vacía el líquido, enjuaga, seca y deposita en la caneca blanca.", "Envase o botella plástica");
        CategoryMeta glass = new CategoryMeta("vidrio", "blanca", true,
                "Enjuaga con cuidado, retira residuos y deposita en la caneca blanca.", "Objeto o frasco de vidrio");
        CategoryMeta paper = new CategoryMeta("papel_carton", "blanca", true,
                "Mantén limpio y seco, aplana la caja y deposita en la caneca blanca.", "Papel o cartón aprovechable");
        CategoryMeta organic = new CategoryMeta("organico", "verde", true,
                "Deposita sin plásticos ni envolturas en la caneca verde de orgánicos.", "Residuo orgánico");
        CategoryMeta metal = new CategoryMeta("metal", "blanca", true,
                "Limpia y escurre los restos antes de depositar en la caneca blanca.", "Lata o envase metálico");
        CategoryMeta dangerous = new CategoryMeta("peligroso", "negra", false,
                "No arrojar a canecas comunes. Llevar al punto especial ECO COMPUTA en Sede 2.", "Pila, batería o residuo especial");
        CategoryMeta nonRecyclable = new CategoryMeta("no_reciclable", "negra", false,
                "Deposita en la caneca negra de residuos no aprovechables.", "Residuo ordinario no aprovechable");

        // Plastic
        for (String k : new String[]{"bottle", "botella", "plastic", "plástico", "plastico", "cup", "vaso", "jug", "container", "packaging", "water bottle"}) {
            STATIC_MAP.put(k, plastic);
        }
        // Glass
        for (String k : new String[]{"glass", "vidrio", "jar", "frasco", "wine bottle", "beer bottle", "tableware", "stemware"}) {
            STATIC_MAP.put(k, glass);
        }
        // Paper / Cardboard
        for (String k : new String[]{"paper", "papel", "cardboard", "cartón", "carton", "box", "caja", "newspaper", "periódico", "periodico", "book", "libro", "notebook", "cuaderno", "envelope"}) {
            STATIC_MAP.put(k, paper);
        }
        // Organic
        for (String k : new String[]{"food", "comida", "fruit", "fruta", "peel", "cáscara", "cascara", "vegetable", "vegetal", "apple", "banana", "bread", "pan", "orange", "naranja", "plant"}) {
            STATIC_MAP.put(k, organic);
        }
        // Metal
        for (String k : new String[]{"can", "lata", "tin", "metal", "aluminum", "aluminio", "tin can", "beverage can"}) {
            STATIC_MAP.put(k, metal);
        }
        // Dangerous
        for (String k : new String[]{"battery", "batería", "bateria", "pila", "light bulb", "bulb", "bombilla", "lamp", "electronics", "electronic", "electronic waste"}) {
            STATIC_MAP.put(k, dangerous);
        }
        // Non recyclable
        for (String k : new String[]{"tissue", "napkin", "servilleta", "wrapper", "envoltura", "trash", "waste", "garbage", "styrofoam", "icopor"}) {
            STATIC_MAP.put(k, nonRecyclable);
        }
    }

    public OfflineClassifier() {
        ObjectDetectorOptions detectorOptions = new ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .build();
        this.objectDetector = ObjectDetection.getClient(detectorOptions);

        ImageLabelerOptions labelerOptions = new ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.2f)
                .build();
        this.imageLabeler = ImageLabeling.getClient(labelerOptions);
    }

    @Override
    public void classify(Bitmap bmp, Callback cb) {
        executor.execute(() -> {
            try {
                InputImage image = InputImage.fromBitmap(bmp, 0);
                Task<List<DetectedObject>> detectTask = objectDetector.process(image);
                List<DetectedObject> objects = Tasks.await(detectTask);

                List<ClassificationResult> results = new ArrayList<>();

                if (objects == null || objects.isEmpty()) {
                    // Si no devuelve cajas, clasificar la foto completa como un solo objeto con bbox null
                    ClassificationResult single = classifyCrop(bmp, null, bmp.getWidth(), bmp.getHeight());
                    results.add(single);
                } else {
                    int maxObjs = Math.min(6, objects.size());
                    for (int i = 0; i < maxObjs; i++) {
                        DetectedObject obj = objects.get(i);
                        Rect box = obj.getBoundingBox();

                        // Validar límites del recorte
                        int left = Math.max(0, box.left);
                        int top = Math.max(0, box.top);
                        int right = Math.min(bmp.getWidth(), box.right);
                        int bottom = Math.min(bmp.getHeight(), box.bottom);
                        int width = Math.max(1, right - left);
                        int height = Math.max(1, bottom - top);

                        Bitmap crop = Bitmap.createBitmap(bmp, left, top, width, height);

                        RectF normalizedBbox = new RectF(
                                (float) left / bmp.getWidth(),
                                (float) top / bmp.getHeight(),
                                (float) right / bmp.getWidth(),
                                (float) bottom / bmp.getHeight()
                        );

                        ClassificationResult res = classifyCrop(crop, normalizedBbox, bmp.getWidth(), bmp.getHeight());
                        results.add(res);
                    }
                }

                cb.onResult(results);
            } catch (Exception e) {
                cb.onError(e);
            }
        });
    }

    private ClassificationResult classifyCrop(Bitmap crop, RectF bbox, int fullW, int fullH) {
        try {
            InputImage cropImage = InputImage.fromBitmap(crop, 0);
            Task<List<ImageLabel>> labelTask = imageLabeler.process(cropImage);
            List<ImageLabel> labels = Tasks.await(labelTask);

            float bestScore = 0f;
            CategoryMeta bestMeta = null;
            String bestLabelText = null;

            if (labels != null) {
                for (ImageLabel label : labels) {
                    String text = label.getText().toLowerCase(Locale.ROOT);
                    for (Map.Entry<String, CategoryMeta> entry : STATIC_MAP.entrySet()) {
                        if (text.contains(entry.getKey())) {
                            if (label.getConfidence() > bestScore) {
                                bestScore = label.getConfidence();
                                bestMeta = entry.getValue();
                                bestLabelText = label.getText();
                            }
                        }
                    }
                }
            }

            if (bestMeta != null) {
                String name = translateLabel(bestLabelText, bestMeta.defaultName);
                return new ClassificationResult(name, bestMeta.category, bestMeta.reciclable, bestScore, bestMeta.comoDesechar, bestMeta.bolsa, bbox);
            } else {
                // Ninguna etiqueta mapeó
                return new ClassificationResult("Objeto no identificado", "no_reciclable", false, 0f,
                        "No se pudo determinar el tipo de residuo.", "negra", bbox);
            }
        } catch (Exception e) {
            return new ClassificationResult("Objeto no identificado", "no_reciclable", false, 0f,
                    "No se pudo clasificar el objeto.", "negra", bbox);
        }
    }

    private String translateLabel(String labelText, String fallback) {
        if (labelText == null) return fallback;
        String l = labelText.toLowerCase(Locale.ROOT);
        if (l.contains("bottle") || l.contains("botella")) return "Botella plástica";
        if (l.contains("cup") || l.contains("vaso")) return "Vaso desechable";
        if (l.contains("box") || l.contains("caja")) return "Caja de cartón";
        if (l.contains("paper") || l.contains("papel")) return "Papel";
        if (l.contains("newspaper") || l.contains("periódico")) return "Periódico";
        if (l.contains("glass") || l.contains("vidrio") || l.contains("jar")) return "Frasco de vidrio";
        if (l.contains("can") || l.contains("lata")) return "Lata de bebida";
        if (l.contains("fruit") || l.contains("fruta") || l.contains("food")) return "Residuo orgánico";
        if (l.contains("battery") || l.contains("pila")) return "Pila o batería";
        return fallback;
    }
}
