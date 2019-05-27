package com.test.dollarfamily;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class OneDollorRecognizer extends GestureRecognizer {

    private static final float PHI = (float) (0.5 * (-1 + Math.sqrt(5)));

    public OneDollorRecognizer(int n) {
        super(n);
    }

    public String recognize(List<GPoint2D> points) {
        if (gesturePoints.size() == 0) {
            return null;
        }

        List<GPoint2D> processingPoints = resample(points);
        rotate(processingPoints);
        scale(processingPoints);
        translate(processingPoints);

        int matchId = 0;
        float nearest = bestDistance(processingPoints, gesturePoints.get(0), -45.0f, 45.0f, 2.0f);
        for (int i = 1; i < gesturePoints.size(); i++) {
            float distance = bestDistance(processingPoints, gesturePoints.get(i), -45.0f, 45.0f, 2.0f);
            if (nearest > distance) {
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
        List<GPoint2D> processingPoints = resample(points);
        rotate(processingPoints);
        scale(processingPoints);
        translate(processingPoints);

        gesturePoints.add(processingPoints);
        correspondType.add(gestureTypename);
    }

    protected float pathLength(List<GPoint2D> points) {
        float sum = 0.0f;
        for (int i = 1; i < points.size(); i++) {
            sum = sum + points.get(i - 1).distanceTo(points.get(i));
        }
        return sum;
    }

    protected List<GPoint2D> resample(List<GPoint2D> input) {
        // Copy the data to prevent change origin input
        List<GPoint2D> points = new ArrayList<>();
        for (GPoint2D point : input) {
            points.add(new GPoint2D(point.x, point.y));
        }

        float equidistance = pathLength(points) / (sampleNumber - 1);
        float acclumateDis = 0.0f;
        List<GPoint2D> newPoints = new ArrayList<>();
        newPoints.add(points.get(0));

        for (int i = 1; i < points.size(); i++) {
            GPoint2D point0 = points.get(i - 1);
            GPoint2D point1 = points.get(i);
            float distance = point0.distanceTo(point1);
            if (acclumateDis + distance >= equidistance) {
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

        // the last may not include due to float compare
        if (newPoints.size() != sampleNumber) {
            newPoints.add(points.get(points.size() - 1));
        }

        return newPoints;
    }

    private float bestDistance(List<GPoint2D> candidate, List<GPoint2D> sample, float thetaA, float thetaB, float delta) {
        float x1 = PHI * thetaA + (1 - PHI) * thetaB;
        float f1 = distanceAtAngle(candidate, sample, x1);
        float x2 = (1 - PHI) * thetaA + PHI * thetaB;
        float f2 = distanceAtAngle(candidate, sample, x2);
        while (Math.abs(thetaA - thetaB) > delta) {
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
        List<GPoint2D> points = new ArrayList<>();
        for (GPoint2D point : candidate) {
            points.add(new GPoint2D(point.x, point.y));
        }
        rotateBy(points, theta);
        return EuclideanDistance(points, sample);
    }
}