package com.test.dollarfamily;

import java.util.ArrayList;
import java.util.List;

public abstract class GestureRecognizer {

    protected List<List<GPoint2D>> gesturePoints;
    protected List<String> correspondType;
    protected int sampleNumber;

    public GestureRecognizer(int n) {
        gesturePoints = new ArrayList<>();
        correspondType = new ArrayList<>();
        sampleNumber = n;
    }

    public abstract String recognize(List<GPoint2D> points);

    public abstract void addSample(List<GPoint2D> points, String gestureTypename);

    protected abstract float pathLength(List<GPoint2D> points);

    protected abstract List<GPoint2D> resample(List<GPoint2D> points);

    protected void rotate(List<GPoint2D> points) {
        GPoint2D center = centroid(points);
        GPoint2D first = points.get(0);
        float theta = (float) Math.atan2(first.y - center.y, first.x - center.x);
        rotateBy(points, theta);
    }

    protected void rotateBy(List<GPoint2D> points, float theta) {
        GPoint2D center = centroid(points);
        for (GPoint2D point : points) {
            double newPointX = (point.x - center.x) * Math.cos(theta) - (point.x - center.x) * Math.sin(theta) + center.x;
            double newPointY = (point.y - center.y) * Math.sin(theta) + (point.y - center.y) * Math.cos(theta) + center.y;
            point.x = (float) newPointX;
            point.y = (float) newPointY;
        }
    }

    protected void scale(List<GPoint2D> points) {
        float minX = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = Float.MIN_VALUE;
        for (GPoint2D point : points) {
            if (minX > point.x) minX = point.x;
            if (maxX < point.x) maxX = point.x;
            if (minY > point.y) minY = point.y;
            if (maxY < point.y) maxY = point.y;
        }
        float width = maxX - minX;
        float height = maxY - minY;
        for (GPoint2D point : points) {
            point.x = point.x * (1 / width);
            point.y = point.y * (1 / height);
        }
    }

    protected void translate(List<GPoint2D> points) {
        GPoint2D center = centroid(points);
        for (GPoint2D point : points) {
            point.x = point.x - center.x;
            point.y = point.y - center.y;
        }
    }

    protected GPoint2D centroid(List<GPoint2D> points) {
        float sumX = 0.0f;
        float sumY = 0.0f;
        for (GPoint2D point : points) {
            sumX += point.x;
            sumY += point.y;
        }
        GPoint2D point = new GPoint2D(sumX / points.size(), sumY / points.size());
        return point;
    }

    protected float EuclideanDistance(List<GPoint2D> candidate, List<GPoint2D> sample) {
        float sum = 0.0f;
        for (int i = 0; i < candidate.size(); i++) {
            sum = sum + candidate.get(i).distanceTo(sample.get(i));
        }
        return sum / candidate.size();
    }
}