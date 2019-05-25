package com.test.dollarfamily;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GestureRecognizer {

    protected List<List<GPoint2D>> gesturePoints;
    protected List<String> correspondType;

    public GestureRecognizer() {
        gesturePoints = new ArrayList<>();
        correspondType = new ArrayList<>();
    }

    public abstract String recognize(List<GPoint2D> points);

    protected float pathLength(List<GPoint2D> points) {
        float sum = 0.0f;
        for (int i = 1; i < points.size(); i++) {
            sum = sum + points.get(i - 1).distanceTo(points.get(i));
        }
        return sum;
    }

}
