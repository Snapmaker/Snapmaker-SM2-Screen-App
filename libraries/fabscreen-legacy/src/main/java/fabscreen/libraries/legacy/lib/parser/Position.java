package fabscreen.libraries.legacy.lib.parser;

import androidx.annotation.NonNull;

import java.util.Locale;

public class Position {
    private static byte FLAG_START_POINT = 1;
    private static byte FLAG_END_POINT = 2;

    public float x;
    public float y;
    public float z;
    private byte flag;

    public Position(float x, float y, float z) {
        this(x, y, z, (byte)0);
    }

    Position(float x, float y, float z, byte flag) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.flag = flag;
    }

    public boolean isStartPoint() {
        return (this.flag & FLAG_START_POINT) > 0;
    }

    void setAsStartPoint() {
        this.flag |= FLAG_START_POINT;
    }

    public boolean isEndPoint() {
        return (this.flag & FLAG_END_POINT) > 0;
    }

    void setAsEndPoint() {
        this.flag |= FLAG_END_POINT;
    }

    float distanceTo(@NonNull Position p) {
        float dx = (p.x - x);
        float dy = (p.y - y);
        float dz = (p.z - z);
        return (float)Math.sqrt((double)(dx * dx + dy * dy + dz * dz));
    }

    public String toString() {
        return String.format(Locale.getDefault(), "x%f y%f z%f", x, y, z);
    }
}