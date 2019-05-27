package com.test.dollarfamily;

import java.util.ArrayList;
import java.util.List;

public class QRecognizer extends GestureRecognizer {

    /*
    The coordinate transform matrix:
    [0, 64] x [0, 64] -> [-1, 1] x [-1, 1]
    [2/64, 0, -1,
     0, 2/64, -1,
     0, 0,     1]

    [-1, 1] x [-1, 1] -> [0, 64] x [0, 64]
    [32, 0, 32,
     0, 32, 32,
     0, 0,   1]
    */
    class Grid {
        // The table is 64x64 as recommend in the paper
        int correspondPointId[][];

        // Input the preprocessed points
        public Grid(List<GPoint2D> points) {
            correspondPointId = new int[64][];
            for (int i = 0; i < 64; i++) {
                correspondPointId[i] = new int[64];
            }
            GPoint2D point = new GPoint2D(-1.0f, -1.0f);
            for (int i = 0; i < 64; i++) {
                point.x = -1.0f;
                point.y = point.y + i * 2.0f / 63;
                for (int j = 0; j < 64; j++) {
                    point.x = point.x + j * 2.0f / 63;
                    int minId = 0;
                    float minDis = point.distanceTo(points.get(0));
                    for (int k = 1; k < sampleNumber; k++) {
                        float dis = point.distanceTo(points.get(k));
                        if (minDis > dis) {
                            minDis = dis;
                            minId = k;
                        }
                    }
                    correspondPointId[i][j] = minId;
                }
            }
        }

        // The input x, y coordinate have been processed
        public int lookUp(float x, float y) {
            return 0;
        }
    }

    float epsilon;
    List<Grid> grids;

    public QRecognizer(int n, float e) {
        super(n);
        epsilon = e;
        grids = new ArrayList<>();
    }

    @Override
    public String recognize(List<GPoint2D> points) {
        if (gesturePoints.size() == 0) {
            return null;
        }

        List<GPoint2D> processingPoints = resample(points);
        scale(processingPoints);
        translate(processingPoints);
        Grid grid = new Grid(processingPoints);

        int matchId = 0;
        float nearest = CloudMatch(processingPoints, gesturePoints.get(0), grids.get(0), grid);
        for (int i = 1; i < gesturePoints.size(); i++) {
            float distance = CloudMatch(processingPoints, gesturePoints.get(i), grids.get(i), grid);
            if (nearest > distance) {
                nearest = distance;
                matchId = i;
            }
        }
        return correspondType.get(matchId);
    }

    @Override
    public void addSample(List<GPoint2D> points, String gestureTypename) {
        List<GPoint2D> processingPoints = resample(points);
        scale(processingPoints);
        translate(processingPoints);

        gesturePoints.add(processingPoints);
        correspondType.add(gestureTypename);
        grids.add(new Grid(processingPoints));
    }

    @Override
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

    @Override
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

    private float CloudMatch(List<GPoint2D> candidate, List<GPoint2D> sample, Grid candidateGrid, Grid sampleGrid) {
        int step = (int) Math.pow(candidate.size(), 1 - epsilon);
        float minLength = Float.MAX_VALUE;
        for (int i = 0; i < candidate.size(); i += step) {
            float lb0 = lowerBound(candidate, sample, sampleGrid, i);
            float lb1 = lowerBound(sample, candidate, candidateGrid, i);
            float d0 = Float.MAX_VALUE;
            float d1 = Float.MAX_VALUE;
            if (lb0 < minLength) {
                d0 = weightedLengthSum(candidate, sample, i, minLength);
            }
            if (lb1 < minLength) {
                d1 = weightedLengthSum(sample, candidate, i, minLength);
            }
            float min = d0 < d1 ? d0 : d1;
            if (minLength > min) minLength = min;
        }
        return minLength;
    }

    private float weightedLengthSum(List<GPoint2D> candidate, List<GPoint2D> sample, int start, float minSoFar) {
        int n = candidate.size();
        boolean[] matched = new boolean[n];
        for (int i = 0; i < n; i++) {
            matched[i] = false;
        }
        int i = start;
        int weight = n;
        float sum = 0.0f;
        do {
            GPoint2D point0 = candidate.get(i);
            float minDistance = Float.MAX_VALUE;
            int matchIndex = -1;
            for (int j = 0; j < n; j++) {
                if (matched[j]) continue;
                GPoint2D point1 = sample.get(i);
                float dis = point0.squareDistanceTo(point1);
                if (minDistance > dis) {
                    matchIndex = j;
                    minDistance = dis;
                }
            }
            if (matchIndex == -1) break;
            matched[matchIndex] = true;
            sum = sum + weight * minDistance;
            if (sum >= minSoFar) return sum;
            weight = weight - 1;
            i = (i + 1 + n) % n;
        } while (i != start);
        return sum;
    }

    private float lowerBound(List<GPoint2D> candidate, List<GPoint2D> sample, Grid sampleGrid, int start) {
        int n = candidate.size();
        float sum = 0.0f;
        float weight = n;
        int i = start;
        do {
            GPoint2D point = candidate.get(i);
            int nearestId = sampleGrid.lookUp(point.x, point.y);
            sum = sum + weight * point.squareDistanceTo(sample.get(nearestId));
            weight = weight - 1;
            i = (i + 1 + n) % n;
        } while (i != start);
        return sum;
    }
}