package com.example.todolist.classifier;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerationConfig;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.firebase.ai.type.GenerateContentResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Clasificador basado en Gemini a través de Firebase AI Logic SDK.
 *
 * NOTA SOBRE APP CHECK EN EMULADOR:
 * Si App Check bloquea llamadas en el emulador, registrar el debug token en Firebase Console:
 * 1. En MainActivity o Application:
 *    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
 *        DebugAppCheckProviderFactory.getInstance());
 * 2. Copiar el token generado en Logcat ("Enter this debug secret...") en Firebase Console -> App Check -> Apps -> Manage debug tokens.
 */
public class GeminiClassifier implements WasteClassifier {
    public static final String MODEL_NAME = "gemini-2.5-flash";

    private static final String PROMPT =
            "Eres un clasificador de residuos para Colombia (bolsa blanca=aprovechables, negra=no aprovechables, verde=orgánicos). " +
            "Detecta TODOS los residuos distintos visibles en la foto (máximo 6, ignora fondo, muebles y personas). " +
            "Responde SOLO un JSON array; cada elemento: {objeto, categoria(enum anterior), reciclable(bool), confianza(0-1), " +
            "como_desechar(máx 1 frase), bolsa(blanca|negra|verde), box_2d:[ymin,xmin,ymax,xmax] normalizado 0-1000}. " +
            "Si no hay residuos claros responde []. Si dudas de un objeto, ponle confianza baja.";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final GenerativeModelFutures generativeModelFutures;

    public GeminiClassifier() {
        GenerationConfig cfg = new GenerationConfig.Builder()
                .setTemperature(0.2f)
                .setResponseMimeType("application/json")
                .build();

        GenerativeModel model = FirebaseAI.getInstance(GenerativeBackend.googleAI())
                .generativeModel(MODEL_NAME, cfg);
        this.generativeModelFutures = GenerativeModelFutures.from(model);
    }

    @Override
    public void classify(Bitmap bmp, Callback cb) {
        executor.execute(() -> {
            try {
                // 1. Redimensionar a máx 1024 px preservando aspecto
                int width = bmp.getWidth();
                int height = bmp.getHeight();
                float maxDim = Math.max(width, height);
                Bitmap scaled = bmp;
                if (maxDim > 1024f) {
                    float factor = 1024f / maxDim;
                    scaled = Bitmap.createScaledBitmap(bmp, Math.round(width * factor), Math.round(height * factor), true);
                }

                // 2. Comprimir a JPEG 80
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] jpegData = baos.toByteArray();
                Bitmap readyBitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);

                // 3. Crear Content y enviar petición
                Content content = new Content.Builder()
                        .addImage(readyBitmap)
                        .addText(PROMPT)
                        .build();

                ListenableFuture<GenerateContentResponse> future = generativeModelFutures.generateContent(content);
                Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
                    @Override
                    public void onSuccess(GenerateContentResponse response) {
                        try {
                            String text = response.getText();
                            if (text == null) text = "[]";
                            text = text.trim();
                            if (text.startsWith("```json")) {
                                text = text.substring(7);
                            }
                            if (text.startsWith("```")) {
                                text = text.substring(3);
                            }
                            if (text.endsWith("```")) {
                                text = text.substring(0, text.length() - 3);
                            }
                            text = text.trim();

                            List<ClassificationResult> results = parseJson(text);
                            cb.onResult(results);
                        } catch (Exception e) {
                            cb.onError(e);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        cb.onError(t);
                    }
                }, executor);

            } catch (Exception e) {
                cb.onError(e);
            }
        });
    }

    private List<ClassificationResult> parseJson(String jsonStr) throws Exception {
        List<ClassificationResult> list = new ArrayList<>();
        JSONArray array = new JSONArray(jsonStr);
        for (int i = 0; i < Math.min(6, array.length()); i++) {
            JSONObject obj = array.getJSONObject(i);
            String objeto = obj.optString("objeto", "Objeto");
            String categoria = normalizeCategory(obj.optString("categoria", "no_reciclable"));
            boolean reciclable = obj.optBoolean("reciclable", false);
            float confianza = (float) obj.optDouble("confianza", 0.6);
            String comoDesechar = obj.optString("como_desechar", "");
            String bolsa = obj.optString("bolsa", "negra").toLowerCase();
            if (!bolsa.equals("blanca") && !bolsa.equals("verde") && !bolsa.equals("negra")) {
                bolsa = reciclable ? "blanca" : "negra";
            }

            RectF bbox = null;
            JSONArray box2d = obj.optJSONArray("box_2d");
            if (box2d != null && box2d.length() == 4) {
                float ymin = (float) box2d.optDouble(0) / 1000f;
                float xmin = (float) box2d.optDouble(1) / 1000f;
                float ymax = (float) box2d.optDouble(2) / 1000f;
                float xmax = (float) box2d.optDouble(3) / 1000f;

                float left = Math.max(0f, Math.min(1f, xmin));
                float top = Math.max(0f, Math.min(1f, ymin));
                float right = Math.max(left, Math.min(1f, xmax));
                float bottom = Math.max(top, Math.min(1f, ymax));
                bbox = new RectF(left, top, right, bottom);
            }

            list.add(new ClassificationResult(objeto, categoria, reciclable, confianza, comoDesechar, bolsa, bbox));
        }
        return list;
    }

    private String normalizeCategory(String cat) {
        String c = cat.toLowerCase();
        if (c.contains("plast") || c.contains("pet")) return "plastico";
        if (c.contains("vidr") || c.contains("cristal")) return "vidrio";
        if (c.contains("papel") || c.contains("carton") || c.contains("cartón")) return "papel_carton";
        if (c.contains("organ") || c.contains("orgán") || c.contains("comida")) return "organico";
        if (c.contains("metal") || c.contains("alumin")) return "metal";
        if (c.contains("peligr") || c.contains("bater") || c.contains("pila") || c.contains("electr")) return "peligroso";
        return "no_reciclable";
    }
}
