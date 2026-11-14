package com.example.todolist.classifier;

import android.graphics.RectF;

public class ClassificationResult {
    public String objeto;
    public String categoria;
    public boolean reciclable;
    public float confianza;
    public String comoDesechar;
    public String bolsa; // "blanca", "negra", "verde"
    public RectF bbox; // normalizado 0-1 (left, top, right, bottom) relativo a la foto

    public ClassificationResult(String objeto, String categoria, boolean reciclable, float confianza, String comoDesechar, String bolsa, RectF bbox) {
        this.objeto = objeto;
        this.categoria = categoria;
        this.reciclable = reciclable;
        this.confianza = confianza;
        this.comoDesechar = comoDesechar;
        this.bolsa = bolsa;
        this.bbox = bbox;
    }
}
