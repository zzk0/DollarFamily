package com.test.dollarfamily;

import android.content.Intent;
import android.gesture.GestureOverlayView;
import android.gesture.GesturePoint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // widget
    private GestureOverlayView gestureOverlayView;
    private MaterialBetterSpinner gestureSpinner;
    private ImageView imageView;
    private Button btn_recognize;
    private Button btn_clear;
    private Button btn_add;
    private TextView textViewResult;
    private EditText editTextName;
    private String[] SPINNERLIST = {"$1", "$P", "$Q"};

    // Used for canvas
    private Bitmap baseBitmap;
    private Canvas canvas;
    private Paint paint;
    private int canvasColor = Color.WHITE;
    private float radio = 5.0f;

    // gesture recognizer
    private GestureRecognizer currentRecognizer;
    private GestureRecognizer[] recognizers = new GestureRecognizer[3];
    private List<GPoint2D> lastPoints;
    private int strokeId;
    private boolean needClear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Button event
        btn_add = (Button) findViewById(R.id.btn_add);
        btn_recognize = (Button) findViewById(R.id.btn_recognize);
        btn_clear = (Button) findViewById(R.id.btn_clear);

        btn_add.setOnClickListener(click);
        btn_recognize.setOnClickListener(click);
        btn_clear.setOnClickListener(click);

        // Setup the canvas
        imageView = (ImageView) findViewById(R.id.canvas);
        imageView.setOnTouchListener(touch);
        paint = new Paint();
        paint.setStrokeWidth(radio);
        paint.setColor(Color.BLACK);

        // GestureOverlayView
        gestureOverlayView = (GestureOverlayView) findViewById(R.id.gestureView);
        gestureOverlayView.addOnGestureListener(new GestureOverlayView.OnGestureListener() {
            @Override
            public void onGestureStarted(GestureOverlayView overlay, MotionEvent event) {}

            @Override
            public void onGesture(GestureOverlayView overlay, MotionEvent event) {}

            @Override
            public void onGestureEnded(GestureOverlayView overlay, MotionEvent event) {
                if (currentRecognizer == recognizers[0]) {
                    List<GPoint2D> points = new ArrayList<>();
                    for (GesturePoint point : overlay.getCurrentStroke()) {
                        GPoint2D newPoint = new GPoint2D(point.x, point.y);
                        points.add(newPoint);
                    }
                    lastPoints = points;
                    doRecognize();
                }
                else if (currentRecognizer == recognizers[1]) {
                    if (lastPoints == null) {
                        lastPoints = new ArrayList<>();
                    }
                    for (GesturePoint point : overlay.getCurrentStroke()) {
                        GPoint2D newPoint = new GPoint2D(point.x, point.y, strokeId);
                        lastPoints.add(newPoint);
                    }
                    strokeId = strokeId + 1;
                }
                else if (currentRecognizer == recognizers[2]) {
                    if (lastPoints == null) {
                        lastPoints = new ArrayList<>();
                    }
                    for (GesturePoint point : overlay.getCurrentStroke()) {
                        GPoint2D newPoint = new GPoint2D(point.x, point.y, strokeId);
                        lastPoints.add(newPoint);
                    }
                    strokeId = strokeId + 1;
                }
            }

            @Override
            public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event) {}
        });

        // Spinner
        gestureSpinner = (MaterialBetterSpinner) findViewById(R.id.spinner_recognizers);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, SPINNERLIST);
        gestureSpinner.setAdapter(arrayAdapter);
        gestureSpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                currentRecognizer = recognizers[position];
            }
        });

        // Others
        textViewResult = (TextView) findViewById(R.id.text_result);
        editTextName = (EditText) findViewById(R.id.edit_typename);

        // Gesture Recognizer
        recognizers[0] = new OneDollorRecognizer(64);
        recognizers[1] = new PRecognizer(96, 0.5f);
        recognizers[2] = new QRecognizer(96, 0.5f);
        currentRecognizer = recognizers[0];
        strokeId = 0;
        needClear = false;
    }

    private View.OnTouchListener touch = new View.OnTouchListener() {
        float startX;
        float startY;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (baseBitmap == null || needClear) {
                        if (lastPoints != null) lastPoints.clear();
                        needClear = false;
                        baseBitmap = Bitmap.createBitmap(imageView.getWidth(), imageView.getHeight(), Bitmap.Config.ARGB_8888);
                        canvas = new Canvas(baseBitmap);
                        canvas.drawColor(canvasColor);
                    }
                    startX = event.getX();
                    startY = event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    float stopX = event.getX();
                    float stopY = event.getY();
                    paint.setStrokeWidth(radio);
                    canvas.drawLine(startX, startY, stopX, stopY, paint);
                    startX = event.getX();
                    startY = event.getY();
                    imageView.setImageBitmap(baseBitmap);
                    break;
                case MotionEvent.ACTION_UP:
                    radio = 5;
                    break;
                default:
                    break;
            }
            return true;
        }
    };

    private View.OnClickListener click = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_add:
                    needClear = true;
                    currentRecognizer.addSample(lastPoints, editTextName.getText().toString());
                    break;
                case R.id.btn_recognize:
                    doRecognize();
                    break;
                case R.id.btn_clear:
                    strokeId = 0;
                    lastPoints.clear();
                    clearCanvas();
                default:
                    break;
            }
        }
    };

    protected void clearCanvas() {
        if (baseBitmap != null) {
            baseBitmap = Bitmap.createBitmap(imageView.getWidth(), imageView.getHeight(), Bitmap.Config.ARGB_8888);
            canvas = new Canvas(baseBitmap);
            canvas.drawColor(canvasColor);
            imageView.setImageBitmap(baseBitmap);
        }
    }

    private void doRecognize() {
        long startTime = System.currentTimeMillis();
        String gestureType = currentRecognizer.recognize(lastPoints);
        long endTime = System.currentTimeMillis();
        if (gestureType == null) {
            gestureType = "Try adding sample !";
        }
        gestureType = gestureType + "\nTime cost: " + (endTime - startTime) / 1000.0f;
        textViewResult.setText(gestureType);
        needClear = true;
    }
}
