package fabscreen.libraries.legacy.data.imgprocess;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.SystemClock;

import com.orhanobut.logger.Logger;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import fabscreen.libraries.legacy.BaseApplication;
import fabscreen.libraries.legacy.data.Constants;

public class LaserDistanceMeasureProcess {
    static class Spot {
        int x;
        int y;
        int minX;
        int maxX;
        int minY;
        int maxY;

        int count = 0;

        Spot(int x, int y) {
            this.x = x;
            this.y = y;
            minX = maxX = x;
            minY = maxY = y;
        }

    }

    /**
     * Return spot x position detected from bitmap.
     *
     * The x value is relative to center of the image (so it can be negative).
     */
    public static float process(Bitmap bitmap) {
        long start = SystemClock.elapsedRealtime();
        Logger.d("Start processing photo, time = " + start);
        Matrix m = new Matrix();
        // post rotate 90 degrees for image processing.
        m.postRotate(BaseApplication.getInstance().getModel().getMachineController().getHeadType() == Constants.HEAD_LASER_10W ? 90 : 270);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);

        Logger.d("(DEBUG) bitmap size = " + bitmap.getWidth() + "x" + bitmap.getHeight());

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        final Bitmap greyscaleTmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // create greyscale image
        Canvas canvas = new Canvas(greyscaleTmp);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(colorFilter);
        canvas.drawBitmap(bitmap, 0, 0, paint);

        // crop
        final Bitmap greyscale = Bitmap.createBitmap(greyscaleTmp, 0, height * 2 / 5, width, height / 5);
        int greyscaleWidth = greyscale.getWidth();
        int greyscaleHeight = greyscale.getHeight();

        // Get pixels color from bitmap.
        int[] rawGreyscaleColors = new int[greyscaleWidth * greyscaleHeight];
        greyscale.getPixels(rawGreyscaleColors, 0, greyscaleWidth, 0, 0, greyscaleWidth, greyscaleHeight);

        // normalize colors
        int minColor = 255, maxColor = 0;
        for (int x = 0; x < greyscaleWidth; x++) {
            for (int y = 0; y < greyscaleHeight; y++) {
                int v = rawGreyscaleColors[x + y * greyscaleWidth] & 0xff;
                minColor = Math.min(minColor, v);
                maxColor = Math.max(maxColor, v);
            }
        }
        maxColor = Math.max(maxColor, minColor + 1); // make sure maxColor > minColor to avoid zero division

        int[][] binarization = new int[greyscaleWidth][greyscaleHeight];
        for (int x = 0; x < greyscaleWidth; x++) {
            for (int y = 0; y < greyscaleHeight; y++) {
                int v = rawGreyscaleColors[x + y * greyscaleWidth] & 0xff;
                v = (int)(255.0 * (v - minColor) / (maxColor - minColor));
                // We use low expose capture photo for detecting light spot, lower the threshold to adapt it.
                if (v >= 128) {
//                    greyscale.setPixel(x, y, Color.WHITE);
                    binarization[x][y] = 1;
                } else {
//                    greyscale.setPixel(x, y, Color.BLACK);
                    binarization[x][y] = 0;
                }
            }
        }

        boolean[][] visited = new boolean[greyscaleWidth][greyscaleHeight];
        int[] dx = {-1, 0, 0, 1};
        int[] dy = {0, -1, 1, 0};

        ArrayList<Spot> spots = new ArrayList<>();
        for (int j = greyscaleHeight - 1; j >= 0; j--) {
            for (int i = 0; i < greyscaleWidth; i++) {
                // test
                int color = binarization[i][j];
                if (color != 0 && !visited[i][j]) {
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
                            if (0 < di && di < greyscaleWidth && 0 < dj && dj < greyscaleHeight && !visited[di][dj]) {
                                int color2 = binarization[di][dj];
                                if (color2 != 0) {
                                    q.add(new Spot(di, dj));
                                    visited[di][dj] = true;
                                    // Locate the pixel and count spot boundary.
                                    spot.count += 1;
                                    spot.minX = Math.min(spot.minX, di);
                                    spot.maxX = Math.max(spot.maxX, di);
                                    spot.minY = Math.min(spot.minY, dj);
                                    spot.maxY = Math.max(spot.maxY, dj);
                                }
                            }
                        }
                    }


                    if (1 < spot.count && spot.count < 500) {
                        spots.add(spot);
                    }
                }
            }
        }

        Spot spot;
        if (spots.size() == 1) {
            spot = spots.get(0);
        } else {
            // Temporary magic number
            return -255;
        }

        float centerX = (spot.maxX + spot.minX) * 0.5f;
        float centerY = (spot.maxY + spot.minY) * 0.5f;

        // Algorithm improvement by parachute.
        // Re-calculate Spot x position.
        float numerator = 0, denominator = 0;
        for (int x = spot.minX; x <= spot.maxX; x++) {
            for (int y = spot.minY; y <= spot.maxY; y++) {
                int color = rawGreyscaleColors[x + y * greyscaleWidth] & 0xff;

                float w = (float)(color) * (50 - Math.abs(centerX - x)) * (50 - Math.abs(centerY - y));
                numerator += x * w;
                denominator += w;
            }
        }
        float spotX = numerator / denominator;
        return (greyscaleWidth * 0.5f - spotX);
    }
}
