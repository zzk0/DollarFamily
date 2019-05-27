package com.test.dollarfamily;

import java.util.List;

public class QRecognizer extends GestureRecognizer {

    float epsilon;

    public QRecognizer(int n, float epsilon) {
        super(n);
        this.epsilon = epsilon;
    }

    @Override
    public String recognize(List<GPoint2D> points) {
        return null;
    }

    @Override
    public void addSample(List<GPoint2D> points, String gestureTypename) {

    }

    @Override
    protected List<GPoint2D> resample(List<GPoint2D> points) {
        return null;
    }

    @Override
    protected float pathLength(List<GPoint2D> points) {
        return 0;
    }
}
