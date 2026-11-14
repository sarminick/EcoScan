package com.example.todolist.classifier;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.LruCache;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enruta inteligentemente la clasificación entre Gemini (online) y ML Kit (offline)
 * con fallback de 15 segundos y caché en memoria.
 */
public class ClassifierRouter implements WasteClassifier {
    private final Context context;
    private final WasteClassifier geminiClassifier;
    private final WasteClassifier offlineClassifier;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public interface RouterCallback {
        void onResult(List<ClassificationResult> results, boolean isOffline, @Nullable String message);
        void onError(Throwable throwable);
    }

    private static class CacheEntry {
        final List<ClassificationResult> results;
        final boolean isOffline;

        CacheEntry(List<ClassificationResult> results, boolean isOffline) {
            this.results = results;
            this.isOffline = isOffline;
        }
    }

    // Caché en memoria por hash del bitmap redimensionado
    private final LruCache<Integer, CacheEntry> cache = new LruCache<>(25);

    public ClassifierRouter(Context context) {
        this.context = context.getApplicationContext();
        WasteClassifier gemini;
        try {
            gemini = new GeminiClassifier();
        } catch (Exception e) {
            gemini = null;
        }
        this.geminiClassifier = gemini;
        this.offlineClassifier = new OfflineClassifier();
    }

    @Override
    public void classify(Bitmap bmp, Callback cb) {
        classify(bmp, (results, isOffline, message) -> cb.onResult(results));
    }

    public void classify(Bitmap bmp, RouterCallback cb) {
        int hash = computeBitmapHash(bmp);
        CacheEntry cached = cache.get(hash);
        if (cached != null) {
            cb.onResult(cached.results, cached.isOffline, null);
            return;
        }

        boolean online = isOnline();
        if (online && geminiClassifier != null) {
            classifyWithGeminiAndFallback(bmp, hash, cb);
        } else {
            classifyOffline(bmp, hash, cb, online ? "No fue posible inicializar Gemini. Usando clasificador local." : null);
        }
    }

    private void classifyWithGeminiAndFallback(Bitmap bmp, int hash, RouterCallback cb) {
        AtomicBoolean completed = new AtomicBoolean(false);

        ScheduledFuture<?> timeoutTask = scheduler.schedule(() -> {
            if (completed.compareAndSet(false, true)) {
                // Timeout de 15 segundos alcanzado -> fallback offline
                classifyOffline(bmp, hash, cb, "Tiempo de espera agotado. Modo sin conexión activado.");
            }
        }, 15, TimeUnit.SECONDS);

        geminiClassifier.classify(bmp, new Callback() {
            @Override
            public void onResult(List<ClassificationResult> results) {
                if (completed.compareAndSet(false, true)) {
                    timeoutTask.cancel(true);
                    cache.put(hash, new CacheEntry(results, false));
                    cb.onResult(results, false, null);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                if (completed.compareAndSet(false, true)) {
                    timeoutTask.cancel(true);
                    // Fallback amable a offline ante errores de red o cuota
                    classifyOffline(bmp, hash, cb, "Fallo de conexión o límite de cuota. Modo sin conexión activado.");
                }
            }
        });
    }

    private void classifyOffline(Bitmap bmp, int hash, RouterCallback cb, @Nullable String reason) {
        offlineClassifier.classify(bmp, new Callback() {
            @Override
            public void onResult(List<ClassificationResult> results) {
                cache.put(hash, new CacheEntry(results, true));
                cb.onResult(results, true, reason);
            }

            @Override
            public void onError(Throwable throwable) {
                cb.onError(throwable);
            }
        });
    }

    private boolean isOnline() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            Network active = cm.getActiveNetwork();
            if (active == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(active);
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } catch (Exception e) {
            return false;
        }
    }

    private int computeBitmapHash(Bitmap bmp) {
        try {
            // Muestreo a 64x64 para calcular hash perceptual estable
            Bitmap small = Bitmap.createScaledBitmap(bmp, 64, 64, false);
            int[] pixels = new int[64 * 64];
            small.getPixels(pixels, 0, 64, 0, 0, 64, 64);
            return Arrays.hashCode(pixels);
        } catch (Exception e) {
            return bmp.hashCode();
        }
    }
}
