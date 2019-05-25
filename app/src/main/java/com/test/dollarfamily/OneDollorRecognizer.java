package com.test.dollarfamily;

import java.util.ArrayList;
import java.util.List;

public class OneDollorRecognizer extends GestureRecognizer {

    private int sampleNumber;
    private static final float PHI = (float) (0.5 * (-1 + Math.sqrt(5)));

    public OneDollorRecognizer(int n) {
        super();
        this.sampleNumber = n;
    }

    public String recognize(List<GPoint2D> points) {
        List<GPoint2D> afterResample = resample(points);
        List<GPoint2D> afterRotate = rotate(afterResample);
        List<GPoint2D> afterScale = scale(afterRotate);
        List<GPoint2D> afterTranslate = translate(afterScale);

        int matchId = 0;
        float nearest = bestDistance(afterTranslate, gesturePoints.get(0), -45.0f, 45.0f, 2.0f);
        for (int i = 1; i < gesturePoints.size(); i++) {
            float distance = bestDistance(afterTranslate, gesturePoints.get(i), -45.0f, 45.0f, 2.0f);
            if (nearest < distance) {
                nearest = distance;
                matchId = i;
            }
        }
        return correspondType.get(matchId);
    }

    /*
    Add a gesture sample.
    The points will be resampled, rotated, scaled and translated
     */
    public void addSample(List<GPoint2D> points, String gestureTypename) {
        List<GPoint2D> afterResample = resample(points);
        List<GPoint2D> afterRotate = rotate(afterResample);
        List<GPoint2D> afterScale = scale(afterRotate);
        List<GPoint2D> afterTranslate = translate(afterScale);

        gesturePoints.add(afterTranslate);
        correspondType.add(gestureTypename);
    }

    private List<GPoint2D> resample(List<GPoint2D> points) {
        float equidistance = pathLength(points) / (sampleNumber - 1);
        float acclumateDis = 0.0f;
        List<GPoint2D> newPoints = new ArrayList<>();
        newPoints.add(points.get(0));

        for (int i = 1; i < points.size(); i++) {
            GPoint2D point0 = points.get(i - 1);
            GPoint2D point1 = points.get(i);
            float distance = point0.distanceTo(point1);
            if (acclumateDis + distance > equidistance) {
                float newPointX = point0.x + ((equidistance - acclumateDis) / distance) * (point1.x - point0.x);
                float newPointY = point0.y + ((equidistance - acclumateDis) / distance) * (point1.y - point0.y);
                GPoint2D newPoint = new GPoint2D(newPointX, newPointY);
                newPoints.add(newPoint);
                points.add(i, newPoint);
                acclumateDis = 0;
            }
            else {
                acclumateDis = acclumateDis + distance;
            }
        }

        return newPoints;
    }

    private List<GPoint2D> rotate(List<GPoint2D> points) {
        GPoint2D center = centroid(points);
        GPoint2D first = points.get(0);
        float theta = (float) Math.atan2(first.y - center.y, first.x - center.x);
        return rotateBy(points, theta);
    }

    private List<GPoint2D> rotateBy(List<GPoint2D> points, float theta) {
        GPoint2D center = centroid(points);
        List<GPoint2D> newPoints = new ArrayList<>();
        for (GPoint2D point : points) {
            double newPointX = (point.x - center.x) * Math.cos(theta) - (point.x - center.x) * Math.sin(theta) + center.x;
            double newPointY = (point.y - center.y) * Math.sin(theta) + (point.y - center.y) * Math.cos(theta) + center.y;
            GPoint2D newPoint = new GPoint2D((float) newPointX, (float) newPointY);
            newPoints.add(newPoint);
        }
        return newPoints;
    }

    private List<GPoint2D> scale(List<GPoint2D> points) {
        float minX = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = Float.MIN_VALUE;
        for (GPoint2D point : points) {
            if (minX > point.x) minX = point.x;
            if (maxX < point.x) maxX = point.x;
            if (minY > point.y) minY = point.y;
            if (maxY > point.y) maxY = point.y;
        }
        float width = maxX - minX;
        float height = maxY - minY;
        List<GPoint2D> newPoints = new ArrayList<>();
        for (GPoint2D point : points) {
            float newPointX = point.x * (1 / width);
            float newPointY = point.y * (1 / height);
            GPoint2D newPoint = new GPoint2D(newPointX, newPointY);
            newPoints.add(newPoint);
        }
        return newPoints;
    }

    private List<GPoint2D> translate(List<GPoint2D> points) {
        GPoint2D center = centroid(points);
        List<GPoint2D> newPoints = new ArrayList<>();
        for (GPoint2D point : points) {
            float newPointX = point.x - center.x;
            float newPointY = point.y - center.y;
            GPoint2D newPoint = new GPoint2D(newPointX, newPointY);
            newPoints.add(newPoint);
        }
        return newPoints;
    }

    private float bestDistance(List<GPoint2D> candidate, List<GPoint2D> sample, float thetaA, float thetaB, float delta) {
        float x1 = PHI * thetaA + (1 - PHI) * thetaB;
        float f1 = distanceAtAngle(candidate, sample, x1);
        float x2 = (1 - PHI) * thetaA + PHI * thetaB;
        float f2 = distanceAtAngle(candidate, sample, x2);
        while ((float) Math.abs(thetaA - thetaB) > delta) {
            if (f1 < f2) {
                thetaB = x2;
                x2 = x1;
                f2 = f1;
                x1 = PHI * thetaA + (1 - PHI) * thetaB;
                f1 = distanceAtAngle(candidate, sample, x1);
            }
            else {
                thetaA = x1;
                x1 = x2;
                f1 = f2;
                x2 = (1 - PHI) * thetaA + PHI * thetaB;
                f2 = distanceAtAngle(candidate, sample, x2);
            }
        }
        return f1 < f2 ? f1 : f2;
    }

    private float distanceAtAngle(List<GPoint2D> candidate, List<GPoint2D> sample, float theta) {
        List<GPoint2D> points = rotateBy(candidate, theta);
        return pathDistance(points, sample);
    }

    private float pathDistance(List<GPoint2D> candidate, List<GPoint2D> sample) {
        float sum = 0.0f;
        for (int i = 0; i < candidate.size(); i++) {
            sum = sum + candidate.get(i).distanceTo(sample.get(i));
        }
        return sum / candidate.size();
    }

    private GPoint2D centroid(List<GPoint2D> points) {
        float sumX = 0.0f;
        float sumY = 0.0f;
        for (GPoint2D point : points) {
            sumX += point.x;
            sumY += point.y;
        }
        GPoint2D point = new GPoint2D(sumX / points.size(), sumY / points.size());
        return point;
    }

}