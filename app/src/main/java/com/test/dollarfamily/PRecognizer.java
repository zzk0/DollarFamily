package com.test.dollarfamily;

import java.util.ArrayList;
import java.util.List;

public class PRecognizer extends GestureRecognizer {

    // 0.5 is recommend
    public float epsilon;

    public PRecognizer(int n, float epsilon) {
        super(n);
        this.epsilon = epsilon;
    }

    public String recognize(List<GPoint2D> points) {
        if (gesturePoints.size() == 0) {
            return null;
        }

        List<GPoint2D> processingPoints = resample(points);
        scale(processingPoints);
        translate(processingPoints);

        int matchId = 0;
        float nearest = greedyCloudMatch(processingPoints, gesturePoints.get(0));
        for (int i = 1; i < gesturePoints.size(); i++) {
            float distance = greedyCloudMatch(processingPoints, gesturePoints.get(i));
            if (nearest > distance) {
                nearest = distance;
                matchId = i;
            }
        }
        return correspondType.get(matchId);
    }

    /*
    Add a gesture sample.
    The points will be resampled, scaled and translated
     */
    public void addSample(List<GPoint2D> points, String gestureTypename) {
        List<GPoint2D> processingPoints = resample(points);
        scale(processingPoints);
        translate(processingPoints);

        gesturePoints.add(processingPoints);
        correspondType.add(gestureTypename);
    }

    private float greedyCloudMatch(List<GPoint2D> candidate, List<GPoint2D> sample) {
        int step = (int) Math.pow(candidate.size(), 1 - epsilon);
        float minLength = Float.MAX_VALUE;
        for (int i = 0; i < candidate.size(); i += step) {
            float d0 = weightedLengthSum(candidate, sample, i);
            float d1 = weightedLengthSum(sample, candidate, i);
            float min = d0 < d1 ? d0 : d1;
            if (minLength > min) minLength = min;
        }
        return minLength;
    }

    private float weightedLengthSum(List<GPoint2D> candidate, List<GPoint2D> sample, int start) {
        int n = candidate.size();
        boolean[] matched = new boolean[n];
        for (int i = 0; i < n; i++) {
            matched[i] = false;
        }
        int i = start;
        int count = 0;
        float sum = 0.0f;
        do {
            count = count + 1;
            GPoint2D point0 = candidate.get(i);
            float minDistance = Float.MAX_VALUE;
            int matchIndex = -1;
            for (int j = 0; j < n; j++) {
                if (matched[j]) continue;
                GPoint2D point1 = sample.get(i);
                float dis = point0.distanceTo(point1);
                if (minDistance > dis) {
                    matchIndex = j;
                    minDistance = dis;
                }
            }
            if (matchIndex == -1) break;
            matched[matchIndex] = true;
            float weight = 1 - (count - 1) / n;
            sum = sum + weight * minDistance;
            i = (i + 1 + n) % n;
        } while (i != start);
        return sum;
    }

    protected float pathLength(List<GPoint2D> points) {
        float sum = 0.0f;
        for (int i = 1; i < points.size(); i++) {
            GPoint2D point0 = points.get(i - 1);
            GPoint2D point1 = points.get(i);
            if (point0.strokeId == point1.strokeId) {
                sum = sum + point0.distanceTo(point1);
            }
        }
        return sum;
    }

    protected List<GPoint2D> resample(List<GPoint2D> input) {
        // Copy the data to prevent change origin input
        List<GPoint2D> points = new ArrayList<>();
        for (GPoint2D point : input) {
            points.add(new GPoint2D(point.x, point.y, point.strokeId));
        }

        float equidistance = pathLength(points) / (sampleNumber - 1);
        float acclumateDis = 0.0f;
        List<GPoint2D> newPoints = new ArrayList<>();
        newPoints.add(points.get(0));

        for (int i = 1; i < points.size(); i++) {
            GPoint2D point0 = points.get(i - 1);
            GPoint2D point1 = points.get(i);
            if (point0.strokeId != point1.strokeId) continue;
            float distance = point0.distanceTo(point1);
            if (acclumateDis + distance >= equidistance) {
                float newPointX = point0.x + ((equidistance - acclumateDis) / distance) * (point1.x - point0.x);
                float newPointY = point0.y + ((equidistance - acclumateDis) / distance) * (point1.y - point0.y);
                GPoint2D newPoint = new GPoint2D(newPointX, newPointY, point0.strokeId);
                newPoints.add(newPoint);
                points.add(i, newPoint);
                acclumateDis = 0;
            }
            else {
                acclumateDis = acclumateDis + distance;
            }
        }

        if (newPoints.size() != sampleNumber) {
            newPoints.add(points.get(points.size() - 1));
        }

        return newPoints;
    }
}
