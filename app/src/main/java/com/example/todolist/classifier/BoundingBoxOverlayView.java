package com.example.todolist.classifier;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Vista superpuesta que dibuja recuadros normalizados sobre la imagen analizada
 * con animación pop-in escalonada (60 ms), colores por tipo de bolsa y selección táctil.
 */
public class BoundingBoxOverlayView extends View {

    public interface OnBoxSelectedListener {
        void onBoxSelected(int index);
    }

    private final List<ClassificationResult> results = new ArrayList<>();
    private final List<ValueAnimator> animators = new ArrayList<>();
    private float[] progresses = new float[0];
    private int selectedIndex = -1;
    private OnBoxSelectedListener listener;

    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgeBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BoundingBoxOverlayView(Context context) {
        super(context);
        init();
    }

    public BoundingBoxOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(6f);

        fillPaint.setStyle(Paint.Style.FILL);

        badgeBgPaint.setStyle(Paint.Style.FILL);

        badgeTextPaint.setColor(Color.WHITE);
        badgeTextPaint.setTextSize(32f);
        badgeTextPaint.setFakeBoldText(true);
        badgeTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setListener(OnBoxSelectedListener listener) {
        this.listener = listener;
    }

    public void setResults(List<ClassificationResult> list) {
        cancelAnimations();
        results.clear();
        if (list != null) results.addAll(list);

        progresses = new float[results.size()];
        selectedIndex = -1;

        for (int i = 0; i < results.size(); i++) {
            final int index = i;
            ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(300);
            anim.setStartDelay(i * 60L);
            anim.addUpdateListener(animation -> {
                progresses[index] = (float) animation.getAnimatedValue();
                invalidate();
            });
            animators.add(anim);
            anim.start();
        }
        invalidate();
    }

    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
        invalidate();
    }

    public void cancelAnimations() {
        for (ValueAnimator anim : animators) {
            anim.cancel();
        }
        animators.clear();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (results.isEmpty()) return;

        int w = getWidth();
        int h = getHeight();

        for (int i = 0; i < results.size(); i++) {
            ClassificationResult item = results.get(i);
            if (item.bbox == null) continue;

            float p = (i < progresses.length) ? progresses[i] : 1f;
            if (p <= 0.01f) continue;

            // Coordenadas absolutas
            float left = item.bbox.left * w;
            float top = item.bbox.top * h;
            float right = item.bbox.right * w;
            float bottom = item.bbox.bottom * h;

            // Escala pop-in respecto al centro del recuadro
            float cx = (left + right) / 2f;
            float cy = (top + bottom) / 2f;
            float hw = ((right - left) / 2f) * p;
            float hh = ((bottom - top) / 2f) * p;

            RectF rect = new RectF(cx - hw, cy - hh, cx + hw, cy - hh + (hh * 2));

            // Colores según bolsa (blanca=blanco/gris claro, negra=negro, verde=verde)
            int mainColor;
            String b = item.bolsa != null ? item.bolsa.toLowerCase() : "negra";
            if ("blanca".equals(b)) {
                mainColor = Color.parseColor("#E2E8F0");
            } else if ("verde".equals(b)) {
                mainColor = Color.parseColor("#007A3D");
            } else {
                mainColor = Color.parseColor("#1E293B");
            }

            boolean isSelected = (i == selectedIndex);

            // Relleno semitransparente
            fillPaint.setColor(mainColor);
            fillPaint.setAlpha(isSelected ? 65 : 35);
            canvas.drawRoundRect(rect, 12f, 12f, fillPaint);

            // Borde
            strokePaint.setColor(isSelected ? Color.parseColor("#F59E0B") : mainColor);
            strokePaint.setStrokeWidth(isSelected ? 9f : 5f);
            canvas.drawRoundRect(rect, 12f, 12f, strokePaint);

            // Badge con el número (1-indexed)
            float badgeRadius = 22f;
            float badgeX = Math.max(badgeRadius + 6f, rect.left + badgeRadius);
            float badgeY = Math.max(badgeRadius + 6f, rect.top + badgeRadius);

            badgeBgPaint.setColor(isSelected ? Color.parseColor("#F59E0B") : mainColor);
            canvas.drawCircle(badgeX, badgeY, badgeRadius, badgeBgPaint);

            // Borde blanco al badge para contraste
            strokePaint.setColor(Color.WHITE);
            strokePaint.setStrokeWidth(3f);
            canvas.drawCircle(badgeX, badgeY, badgeRadius, strokePaint);

            // Número dentro del badge
            String numStr = String.valueOf(i + 1);
            float textOffset = (badgeTextPaint.descent() + badgeTextPaint.ascent()) / 2;
            canvas.drawText(numStr, badgeX, badgeY - textOffset, badgeTextPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && !results.isEmpty()) {
            float xNorm = event.getX() / getWidth();
            float yNorm = event.getY() / getHeight();

            for (int i = 0; i < results.size(); i++) {
                ClassificationResult r = results.get(i);
                if (r.bbox != null && r.bbox.contains(xNorm, yNorm)) {
                    selectedIndex = i;
                    invalidate();
                    if (listener != null) {
                        listener.onBoxSelected(i);
                    }
                    return true;
                }
            }
        }
        return true;
    }
}
