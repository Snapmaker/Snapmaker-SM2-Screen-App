package com.snapmaker.fabscreen.modules.settings;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.Log;

import com.orhanobut.logger.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.util.ArrayList;

import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

public class SettingsCameraCalibrationViewModel extends BaseViewModel {
    public static final int STATUS_IDLE = 0;
    public static final int STATUS_PROCESSING = 1;
    public static final int STATUS_COMPLETE = 2;
    public static final int STATUS_ERROR = 3;

    private static int d = 100;
    private int cameraZ = 170;
    private float mZPos = 0;
    private Point mTakePhotoPoint;
    private Point mCenter;
    private ArrayList<Point> mCorners = new ArrayList<>();

    private BehaviorSubject<Integer> mCalibrationStatusSubject = BehaviorSubject.createDefault(STATUS_IDLE);
    private BehaviorSubject<Integer> mProcessProgressSubject = BehaviorSubject.createDefault(0);

    public SettingsCameraCalibrationViewModel() {
        super();

        String machineModel = getModel().getPreferences().getMachineModel();
        switch (machineModel) {
            case Constants.MACHINE_TYPE_A150:
                cameraZ = 140;
                d = 80;
                mTakePhotoPoint = new Point(155, 82);
                break;
            case Constants.MACHINE_TYPE_A250:
                cameraZ = 170;
                d = 100;
                mTakePhotoPoint = new Point(146, 122);
                break;
            case Constants.MACHINE_TYPE_A350:
                cameraZ = 170;
                d = 100;
                mTakePhotoPoint = new Point(192, 170);
                break;
            default:
                cameraZ = 140;
                d = 80;
                mTakePhotoPoint = new Point(getModel().getMachineController().getSizeX() / 2, getModel().getMachineController().getSizeY() / 2);
        }

        Logger.d("cameraZ is %d , d is %d.", cameraZ, d);

        // get center point and calculate corners position
        mCenter = new Point(getModel().getMachineController().getSizeX() / 2, getModel().getMachineController().getSizeY() / 2);
        mCorners.add(new Point(mCenter.x - d / 2, mCenter.y + d / 2));
        mCorners.add(new Point(mCenter.x + d / 2, mCenter.y + d / 2));
        mCorners.add(new Point(mCenter.x + d / 2, mCenter.y - d / 2));
        mCorners.add(new Point(mCenter.x - d / 2, mCenter.y - d / 2));
    }

    public Observable<Integer> getCalibrationStatus() {
        return mCalibrationStatusSubject;
    }

    public Observable<Integer> getProcessProgressObservable() {
        return mProcessProgressSubject;
    }

    public void start() {
        if (!getModel().getLaserCameraController().isConnected()) {
            mCalibrationStatusSubject.onNext(STATUS_ERROR);
            return;
        }

        // reset status
        if (mCalibrationStatusSubject.getValue() != STATUS_IDLE) {
            mCalibrationStatusSubject.onNext(STATUS_IDLE);
            mProcessProgressSubject.onNext(0);
        }

        // close auto white balance before move
        setAutoWhiteBalance(false);

        turnOnLight();

        initPosition();
    }

    private void turnOnLight() {
        boolean cameraLightOn = getModel().getPreferences().getLaserCameraLightOn();
        if (cameraLightOn) {
            getModel().getLaserCameraController().setCameraLighting(true)
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(success -> {
                        // do nothing
                    }, LogHelper::log);
        }
    }

    private void turnOffLight() {
        boolean cameraLightOn = getModel().getPreferences().getLaserCameraLightOn();
        if (cameraLightOn) {
            getModel().getLaserCameraController().setCameraLighting(false)
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(success -> {
                        // do nothing
                    }, LogHelper::log);
        }
    }

    private void initPosition() {
        mCalibrationStatusSubject.onNext(STATUS_PROCESSING);

        final float laserFocus = getModel().getMachineController().getLaserFocus();
        final float bottomZ = getModel().getPreferences().getLaserBottomZ();
        final float materialThickness = getModel().getPreferences().getLaserMaterialThickness();

        mZPos = Math.max(laserFocus, bottomZ - materialThickness + 0.1f);

        Point lastPoint = mCorners.get(mCorners.size() - 1);

        getModel().getSlaveComputer().gotoAbsolutePosition(mCenter.x, mCenter.y, mZPos)
                .flatMap(success -> getModel().getSlaveComputer().gotoAbsolutePosition(lastPoint.x, lastPoint.y, mZPos))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    startEngraveRectangle();
                }, Throwable::printStackTrace);
    }

    private void startEngraveRectangle() {
        BehaviorSubject<Integer> pointIndexSubject = BehaviorSubject.createDefault(0);

        pointIndexSubject
                .flatMap(index -> {
                    Point point = mCorners.get(index);
                    return engraveLine(point, index);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    if (pointIndexSubject.getValue() == mCorners.size() - 1) {
                        detectingRectangle();
                        // Engrave rectangle complete
                        pointIndexSubject.onComplete();
                    } else {
                        pointIndexSubject.onNext(pointIndexSubject.getValue() + 1);
                    }
                });
    }

    private Observable<Boolean> engraveLine(Point point, int index) {
        final int x = point.x;
        final int y = point.y;

        final int engraveX;
        final int engraveY;

        switch (index) {
            case 0:
                engraveX = x;
                engraveY = y - 10;
                break;
            case 1:
                engraveX = x - 10;
                engraveY = y;
                break;
            case 2:
                engraveX = x;
                engraveY = y + 10;
                break;
            case 3:
                engraveX = x + 10;
                engraveY = y;
                break;
            default:
                engraveX = 0;
                engraveY = 0;
                break;
        }

        return getModel().getSlaveComputer().sendGcode("M3 P70")
                .flatMap(response -> getModel().getSlaveComputer().gotoAbsolutePosition(engraveX, engraveY, mZPos, 300))
                .flatMap(success -> getModel().getSlaveComputer().sendGcode("M5"))
                .flatMap(success -> getModel().getSlaveComputer().gotoAbsolutePosition(x, y, mZPos, 300));
    }

    private void detectingRectangle() {
        getModel().getSlaveComputer().gotoAbsolutePosition(mTakePhotoPoint.x, mTakePhotoPoint.y, cameraZ)
                .flatMap(success -> getModel().getLaserCameraController().requestCapturePhoto())
                .flatMap(success -> getModel().getLaserCameraController().watchPhotoReceive())
                .subscribeOn(Schedulers.computation())
                .as(bindToLifecycle())
                .subscribe(bitmap -> {
                    Logger.d("Capture image succeed.");

                    setAutoWhiteBalance(true);
                    turnOffLight();

                    String path = getModel().getCacheDir() + "/calibration.jpg";

                    Matrix m = new Matrix();
                    m.postRotate(270);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);

                    FileOutputStream out = new FileOutputStream(path);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

                    process(bitmap);
                    mCalibrationStatusSubject.onNext(STATUS_COMPLETE);
                }, e -> {
                    Logger.w("Capture image failed.");
                    mCalibrationStatusSubject.onNext(STATUS_ERROR);
                });
    }

    private void setAutoWhiteBalance(boolean enabled) {
        getModel().getLaserCameraController()
                .setCameraAutoWhiteBalance(enabled)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    Log.d("DEBUG", "camera auto white balance " + enabled + success);
                });
    }

    private Bitmap getGreyscaleImage(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();

        Bitmap greyscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(greyscale);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(colorFilter);
        canvas.drawBitmap(image, 0, 0, paint);

        return greyscale;
    }

    private void normalize(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();

        long total = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int v = image.getPixel(x, y) & 0xff;
                total += v;
            }
        }
        double avg = total / (width * height);

        // divide image into cells and normalize the cell use image average color.
        final int cellSize = 30;
        for (int i = 0; i < width; i += cellSize) {
            for (int j = 0; j < height; j += cellSize) {
                int maxX = Math.min(i + cellSize, width);
                int maxY = Math.min(j + cellSize, height);

                total = 0;
                for (int x = i; x < maxX; x++) {
                    for (int y = j; y < maxY; y++) {
                        int v = image.getPixel(x, y) & 0xff;
                        total += v;
                    }
                }

                double cellAvg = total / ((maxX - i) * (maxY - j));
                int diff = (int) Math.round((cellAvg - avg) * 0.8);

                for (int x = i; x < maxX; x++) {
                    for (int y = j; y < maxY; y++) {
                        int v = image.getPixel(x, y) & 0xff;
                        int newValue = v - diff;
                        image.setPixel(x, y, 0xff000000 | (0x010101 * newValue));
                    }
                }
            }
        }

        // classic normalize
        int min = 255;
        int max = 0;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int v = image.getPixel(x, y) & 0xff;

                min = Math.min(min, v);
                max = Math.max(max, v);
            }
        }

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int v = image.getPixel(x, y) & 0xff;
                int newValue = (int) Math.round(255.0 * (v - min) / (max - min));
                image.setPixel(x, y, 0xff000000 | (0x010101 * newValue));
            }
        }
    }

    private void binarization(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int v = image.getPixel(x, y) & 0xff;

                if (v >= 128) {
                    image.setPixel(x, y, Color.WHITE);
                } else {
                    image.setPixel(x, y, Color.BLACK);
                }
            }
        }
    }

    private float round(float number) {
        int factor = 1;
        for (int i = 0; i < 4; i++) {
            factor *= 10;
        }
        return 1.0f * Math.round(number * factor) / factor;
    }

    class Equation {
        float m;
        float c;

        Equation(float gradient, float intercept) {
            this.m = gradient;
            this.c = intercept;
        }
    }

    private Equation regression(ArrayList<Point> points) {
        long[] sum = new long[4];

        for (int i = 0; i < 4; i++) {
            sum[i] = 0;
        }

        for (Point point : points) {
            sum[0] += point.x;
            sum[1] += point.y;
            sum[2] += point.x * point.x;
            sum[3] += point.x * point.y;
        }

        long size = (long) points.size();
        long run = size * sum[2] - sum[0] * sum[0];
        long rise = size * sum[3] - sum[0] * sum[1];
        float gradient = run == 0 ? 0 : round(1.0f * rise / run);
        float intercept = round(1.0f * sum[1] / size - gradient * sum[0] / size);

        return new Equation(gradient, intercept);
    }

    private Equation getLine(Bitmap image, int x0, int y0, int width, int height) {
        // crop
        Bitmap cropped = Bitmap.createBitmap(image, x0, y0, width, height);

        // binarization
        normalize(cropped);
        binarization(cropped);

        boolean useYRegression = (width < height);

        ArrayList<Point> points = new ArrayList<>();

        long[] sum = new long[4];
        Equation eq;
        while (true) {
            points.clear();
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int v = cropped.getPixel(x, y) & 0xff;
                    if (v == 0) {
                        if (useYRegression) {
                            points.add(new Point(y, x));
                        } else {
                            points.add(new Point(x, y));
                        }
                    }
                }
            }

            eq = regression(points);

            // calculate distance
            float maxDistance = 0;
            for (Point point : points) {
                float y = point.x * eq.m + eq.c;
                float dist = Math.abs(y - point.y);
                maxDistance = Math.max(maxDistance, dist);
            }

            if (maxDistance < 1) {
                break;
            }

            for (Point point : points) {
                float y = point.x * eq.m + eq.c;
                float dist = Math.abs(y - point.y);
                if (dist > maxDistance * 0.8) {
                    if (useYRegression) {
                        cropped.setPixel(point.y, point.x, Color.WHITE);
                    } else {
                        cropped.setPixel(point.x, point.y, Color.WHITE);
                    }
                }
            }
        }

        if (useYRegression) {
            eq.m = 1 / eq.m;
            eq.c = -(x0 + eq.c) * eq.m + y0;
        } else {
            eq.c = eq.c - eq.m * x0 + y0;
        }

        return eq;
    }

    private Point getCross(Equation a, Equation b) {
        float x = -(a.c - b.c) / (a.m - b.m);
        float y = a.m * x + a.c;
        return new Point((int) x, (int) y);
    }

    private void process(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();
        Bitmap greyscale = getGreyscaleImage(image);

        mProcessProgressSubject.onNext(1);
        Equation top = getLine(greyscale, width / 3, 0, width / 3, height / 3);

        mProcessProgressSubject.onNext(2);
        Equation bottom = getLine(greyscale, width / 3, height * 2 / 3, width / 3, height / 3);

        mProcessProgressSubject.onNext(3);
        Equation left = getLine(greyscale, 0, height / 3, width / 3, height / 3);

        mProcessProgressSubject.onNext(4);
        Equation right = getLine(greyscale, width * 2 / 3, height / 3, width / 3, height / 3);

        for (int x = 0; x < width; x++) {
            int y = Math.round(top.m * x + top.c);
            if (0 <= y && y < height) {
                image.setPixel(x, y, Color.GREEN);
            }
        }
        for (int x = 0; x < width; x++) {
            int y = Math.round(bottom.m * x + bottom.c);
            if (0 <= y && y < height) {
                image.setPixel(x, y, Color.GREEN);
            }
        }
        for (int y = 0; y < height; y++) {
            int x = Math.round((y - left.c) / left.m);
            if (0 <= x && x < width) {
                image.setPixel(x, y, Color.GREEN);
            }
        }
        for (int y = 0; y < height; y++) {
            int x = Math.round((y - right.c) / right.m);
            if (0 <= x && x < width) {
                image.setPixel(x, y, Color.GREEN);
            }
        }

        ArrayList<Point> points = new ArrayList<>();

        points.add(getCross(top, left));
        points.add(getCross(top, right));
        points.add(getCross(bottom, right));
        points.add(getCross(bottom, left));

        Log.d("DEBUG", "points " + points);
        Log.d("DEBUG", "mCorners " + mCorners);

        // save result into a JSONObject
        JSONObject result = new JSONObject();
        JSONArray jsonPoints = new JSONArray();
        JSONArray jsonCorners = new JSONArray();
        try {
            for (Point p : points) {
                JSONObject point = new JSONObject();
                point.put("x", p.x);
                point.put("y", p.y);
                jsonPoints.put(point);
            }

            for (Point c : mCorners) {
                JSONObject point = new JSONObject();
                point.put("x", c.x);
                point.put("y", c.y);
                jsonCorners.put(point);
            }

            result.put("points", jsonPoints);
            result.put("corners", jsonCorners);
        } catch (JSONException e) {
            LogHelper.log(e);
        }

        Log.d("DEBUG", "JSON \n " + result.toString());

        // save result in preferences
        Logger.d("Camera calibration result : %s", result.toString());
        getModel().getPreferences().setCameraCalibration(result.toString());
    }
}
