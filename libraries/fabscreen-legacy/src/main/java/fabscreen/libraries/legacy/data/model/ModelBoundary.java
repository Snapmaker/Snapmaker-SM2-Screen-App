package fabscreen.libraries.legacy.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Locale;

import fabscreen.libraries.legacy.lib.parser.Position;

public class ModelBoundary implements Parcelable {
    public static int DIMENSION_XY = 1;
    public static int DIMENSION_BY = 2;
    public static int MAX_DELTA_B = 360;

    private float minX;
    private float maxX;
    private float minY;
    private float maxY;
    private float minZ;
    private float maxZ;
    private float minB;
    private float maxB;

    private int mDimension;

    public ModelBoundary() {
        this(false);
    }

    public ModelBoundary(boolean is4AxisModel) {
        this(is4AxisModel, DIMENSION_XY);
    }

    public ModelBoundary(boolean is4AxisModel, int dimension) {
        minX = minY = minZ = Float.MAX_VALUE;
        maxX = maxY = maxZ = -Float.MAX_VALUE;
        minB = 0;
        maxB = 360;

        mDimension = dimension;
    }

    public void setDimension(int dimension) {
        mDimension = dimension;
    }

    public int getDimension() {
        return mDimension;
    }

    public float getMinX() {
        return minX;
    }

    public void setMinX(float x) {
        minX = x;
    }

    public float getMaxX() {
        return maxX;
    }

    public void setMaxX(float x) {
        maxX = x;
    }

    public float getMinY() {
        return minY;
    }

    public void setMinY(float y) {
        minY = y;
    }

    public float getMaxY() {
        return maxY;
    }

    public void setMaxY(float y) {
        maxY = y;
    }

    public float getMinZ() {
        return minZ;
    }

    public void setMinZ(float z) {
        minZ = z;
    }

    public float getMaxZ() {
        return maxZ;
    }

    public void setMaxZ(float z) {
        maxZ = z;
    }

    public float getMinB() {
        return minB;
    }

    public void setMinB(float b) {
        minB = b;
    }

    public float getMaxB() {
        return maxB;
    }

    public void setMaxB(float b) {
        maxB = b;
    }

    public void updateBoundary(Position position) {
        minX = Math.min(minX, position.x);
        maxX = Math.max(maxX, position.x);

        minY = Math.min(minY, position.y);
        maxY = Math.max(maxY, position.y);

        minZ = Math.min(minZ, position.z);
        maxZ = Math.max(maxZ, position.z);

        // FIXME: update minB and MaxB with updateBoundary.
    }

    public float[] getBoundaryPoint(int point) {
        if (minX == Float.MAX_VALUE || maxX == -Float.MAX_VALUE) {
            return null;
        }
        if (point > 4 || point < 0) {
            return null;
        }

        float targetMinHorizontalPoint = 0;
        float targetMaxHorizontalPoint = 0;

        if (mDimension == DIMENSION_BY) {
            // Reduce machine movement if delta value of B axis dimension is over 360 degrees.
            // There's some questions here and we don't have conclusion for now.
            // If minB = -30.0 and maxB = 690.0, delta B > 360.0.
            // Which values should we return? -30/330 or 0/360?
            targetMinHorizontalPoint = ((maxB - minB) < MAX_DELTA_B) ? minB : 0;
            targetMaxHorizontalPoint = ((maxB - minB) < MAX_DELTA_B) ? maxB : 360;
        } else if (mDimension == DIMENSION_XY) {
            targetMinHorizontalPoint = minX;
            targetMaxHorizontalPoint = maxX;
        }

        final float[] boundaryPoint = new float[2];
        switch (point) {
            case 0: {
                boundaryPoint[0] = targetMinHorizontalPoint;
                boundaryPoint[1] = minY;
                break;
            }
            case 1: {
                boundaryPoint[0] = targetMaxHorizontalPoint;
                boundaryPoint[1] = minY;
                break;
            }
            case 2: {
                boundaryPoint[0] = targetMaxHorizontalPoint;
                boundaryPoint[1] = maxY;
                break;
            }
            case 3: {
                boundaryPoint[0] = targetMinHorizontalPoint;
                boundaryPoint[1] = maxY;
                break;
            }
            case 4: {
                boundaryPoint[0] = targetMinHorizontalPoint;
                boundaryPoint[1] = minY;
            }
        }
        return boundaryPoint;
    }

    public float[] getCenterPoint() {
        final float centerPoint[] = new float[3];
        centerPoint[0] = minX + 0.5f * (maxX - minX);
        centerPoint[1] = minY + 0.5f * (maxY - minY);
        centerPoint[2] = minZ + 0.5f * (maxZ - minZ);
        return centerPoint;
    }

    public float getRadius() {
        return (float) (0.5f * Math.sqrt((maxX - minX) * (maxX - minX) + (maxY - minY) * (maxY - minY) + (maxZ - minZ) * (maxZ - minZ)));
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "ModelBoundary \n" +
                        "minX %.5f maxX %.5f\n" +
                        "minY %.5f maxY %.5f\n" +
                        "minZ %.5f maxZ %.5f\n" +
                        "minB %.5f maxB %.5f",
                minX, maxX,
                minY, maxY,
                minZ, maxZ,
                minB, maxB);
    }

    public static final Creator<ModelBoundary> CREATOR = new Creator<ModelBoundary>() {

        @Override
        public ModelBoundary createFromParcel(Parcel source) {
            ModelBoundary boundary = new ModelBoundary();
            boundary.setDimension(source.readInt());
            boundary.setMinX(source.readFloat());
            boundary.setMaxX(source.readFloat());
            boundary.setMinY(source.readFloat());
            boundary.setMaxY(source.readFloat());
            boundary.setMinZ(source.readFloat());
            boundary.setMaxZ(source.readFloat());
            boundary.setMinB(source.readFloat());
            boundary.setMaxB(source.readFloat());
            return boundary;
        }

        @Override
        public ModelBoundary[] newArray(int size) {
            return new ModelBoundary[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mDimension);
        dest.writeFloat(minX);
        dest.writeFloat(maxX);
        dest.writeFloat(minY);
        dest.writeFloat(maxY);
        dest.writeFloat(minZ);
        dest.writeFloat(maxZ);
        dest.writeFloat(minB);
        dest.writeFloat(maxB);
    }
}
