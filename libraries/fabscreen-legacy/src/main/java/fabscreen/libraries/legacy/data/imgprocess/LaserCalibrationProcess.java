package fabscreen.libraries.legacy.data.imgprocess;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Queue;

import fabscreen.libraries.legacy.BaseApplication;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.model.LaserPattern;

public class LaserCalibrationProcess {
    private static final String TAG = LaserCalibrationProcess.class.getSimpleName();

    static class Spot {
        int x;
        int y;
        int count = 0;

        Spot(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    static class SortByDistance implements Comparator<Spot> {
        private int x;
        private int y;

        SortByDistance(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int compare(Spot a, Spot b) {
            int da = (a.x - this.x) * (a.x - this.x) + (a.y - this.y) * (a.y - this.y) * 16;
            int db = (b.x - this.x) * (b.x - this.x) + (b.y - this.y) * (b.y - this.y) * 16;
            return da - db;
        }
    }

    static class SortByX implements Comparator<Spot> {
        public int compare(Spot a, Spot b) {
            return a.x - b.x;
        }
    }

    private static ImageView mDebugImageView;

    public static void setDebugImageView(@Nullable ImageView imageView) {
        mDebugImageView = imageView;
    }

    public static int process(Context context, Bitmap bitmap) {
        return process(context, bitmap, LaserPattern.DIRECTION_X);
    }

    public static int process(Context context, Bitmap bitmap, int direction) {
        // TODO: image processing with laser pattern, return index of the pattern
        // post rotate 90 degrees for image processing.
        Matrix m = new Matrix();
        if (direction == LaserPattern.DIRECTION_X) {
            // post rotate 90 degrees for image processing.
            m.postRotate(BaseApplication.getInstance().getModel().getMachineController().getHeadType() == Constants.HEAD_LASER_10W ? 90 : 270);
        }
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);

        Log.d(TAG, "bitmap size = " + bitmap.getWidth() + " " + bitmap.getHeight());

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int scale = 3;
        int scaledWidth = width / scale;
        int scaledHeight = height / scale;

        final Bitmap greyscaleTmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // create greyscale image
        Canvas canvas = new Canvas(greyscaleTmp);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(colorFilter);
        canvas.drawBitmap(bitmap, 0, 0, paint);

        final Bitmap greyscale = Bitmap.createScaledBitmap(greyscaleTmp, scaledWidth, scaledHeight, false);

        // blur
        RenderScript renderScript = RenderScript.create(context);

        Allocation tmpIn = Allocation.createFromBitmap(renderScript, greyscale);
        Allocation tmpOut = Allocation.createTyped(renderScript, tmpIn.getType());

        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        blurScript.setRadius(8f);
        blurScript.setInput(tmpIn);
        blurScript.forEach(tmpOut);

        Bitmap blur = Bitmap.createBitmap(greyscale);
        tmpOut.copyTo(blur);

        Log.e(TAG, "blurred image, size = " + blur.getWidth() + " " + blur.getHeight());

        for (int i = 0; i < scaledWidth; i++) {
            for (int j = 0; j < scaledHeight; j++) {
                int color = greyscale.getPixel(i, j) & 0xFF;
                int colorBlur = blur.getPixel(i, j) & 0xFF;

                if (color - colorBlur > -10) {
                    greyscale.setPixel(i, j, 0xFFFFFFFF);
                } else {
                    greyscale.setPixel(i, j, 0xFF000000);
                }
            }
        }

        boolean[][] visited = new boolean[scaledWidth][scaledHeight];
        int[] dx = {-1, 0, 0, 1};
        int[] dy = {0, -1, 1, 0};

        ArrayList<Spot> spots = new ArrayList<>();
        for (int j = scaledHeight - 1; j >= 0; j--) {
            for (int i = 0; i < scaledWidth; i++) {
                int color = greyscale.getPixel(i, j) & 0xFF;
                if (color == 0 && !visited[i][j]) {
                    Spot spot = new Spot(i, j);
                    spot.count = 1;

                    Queue<Spot> q = new LinkedList<>();
                    q.add(new Spot(i, j));
                    visited[i][j] = true;

                    while (!q.isEmpty()) {
                        Spot item = q.remove();

                        for (int d = 0; d < 4; d++) {
                            int di = item.x + dx[d];
                            int dj = item.y + dy[d];
                            if (0 < di && di < scaledWidth && 0 < dj && dj < scaledHeight && !visited[di][dj]) {
                                int color2 = greyscale.getPixel(di, dj) & 0xFF;
                                if (color2 == 0) {
                                    q.add(new Spot(di, dj));
                                    visited[di][dj] = true;
                                    spot.count += 1;
                                }
                            }
                        }
                    }

                    if (spot.count > 20) {
                        spots.add(spot);
                        Log.e(TAG, "spot " + spot.x + "," + spot.y + " dots = " + spot.count);
                    }
                }
            }
        }

        if (spots.size() < 21) {
            Log.e(TAG, "Not enough spots detected.");
            return -1;
        }

        Log.e(TAG, "sort spots by distance");
        Collections.sort(spots, new SortByDistance(scaledWidth / 2, scaledHeight / 2));

        // blood fill to find connected spots
        ArrayList<Spot> newSpots = new ArrayList<>();
        if (mDebugImageView != null) {
            visited = new boolean[scaledWidth][scaledHeight];
            for (int i = 0; i < 21; i++) {
                Spot spot = spots.get(i);
                newSpots.add(spot);

                // color
                Queue<Spot> q = new LinkedList<>();
                q.add(spot);
                visited[spot.x][spot.y] = true;

                while (!q.isEmpty()) {
                    Spot item = q.remove();

                    for (int d = 0; d < 4; d++) {
                        int di = item.x + dx[d];
                        int dj = item.y + dy[d];
                        if (0 < di && di < scaledWidth && 0 < dj && dj < scaledHeight && !visited[di][dj]) {
                            int color2 = greyscale.getPixel(di, dj) & 0xFF;
                            if (color2 == 0) {
                                q.add(new Spot(di, dj));
                                visited[di][dj] = true;
                                greyscale.setPixel(di, dj, Color.GREEN);
                            }
                        }
                    }
                }
            }
            mDebugImageView.setImageBitmap(greyscale);
        } else {
            for (int i = 0; i < 21; i++) {
                Spot spot = spots.get(i);
                newSpots.add(spot);
            }
        }

        // Sort by X to retrive original order of spots
        Collections.sort(newSpots, new SortByX());
        int minIndex = 0;
        int minCount = newSpots.get(0).count;
        for (int i = 0; i < newSpots.size(); i++) {
            Spot spot = newSpots.get(i);
            Log.e(TAG, "sorted spots " + spot.x + "," + spot.y + " dots = " + spot.count);
            int count = i % 5 == 0 ? spot.count / 2 : spot.count;
            if (count < minCount) {
                minIndex = i;
                minCount = count;
            }
        }

        Log.e(TAG, "thinnest = " + minIndex);
        return minIndex;
//        mFineTuneResultSubject.onNext(minIndex);
    }
}
