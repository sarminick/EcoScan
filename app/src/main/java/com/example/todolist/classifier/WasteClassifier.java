package com.example.todolist.classifier;

import android.graphics.Bitmap;
import java.util.List;

public interface WasteClassifier {
    interface Callback {
        void onResult(List<ClassificationResult> results);
        void onError(Throwable throwable);
    }

    void classify(Bitmap bmp, Callback cb);
}
