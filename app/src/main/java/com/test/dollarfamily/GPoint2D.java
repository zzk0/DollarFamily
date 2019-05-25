package com.test.dollarfamily;

public class GPoint2D {
    public float x;
    public float y;

    public GPoint2D(float xx, float yy) {
        this.x = xx;
        this.y = yy;
    }

    public float distanceTo(GPoint2D another) {
        float disX = this.x - another.x;
        float disY = this.y - another.y;
        return (float) Math.sqrt(disX * disX + disY * disY);
    }
}