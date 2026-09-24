package fabscreen.libraries.legacy.data.serial.fabpacket.content;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.List;

import okio.Buffer;

public class ExtruderModel {

    @IntDef({BRASS, HARD_STEEL})
    public @interface Material {
    }

    public static final int BRASS = 0x01;
    public static final int HARD_STEEL = 0x02;

    @Material
    public int material;
    public float caliber;

    public ExtruderModel() {
    }

    public ExtruderModel(@Material int material, float caliber) {
        this.material = material;
        this.caliber = caliber;
    }

    public String getName() {
        switch (material) {
            case BRASS:
                return "BRASS";
            case HARD_STEEL:
                return "STEEL";
            default:
                return "Not found";
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "ExtruderModel{" +
                "material=" + material +
                ", caliber=" + caliber +
                '}';
    }

    public static List<ExtruderModel> parse(byte[] content) {
        List<ExtruderModel> models = new ArrayList<>();
        ExtruderModel modelL = new ExtruderModel();
        ExtruderModel modelR = new ExtruderModel();
        Buffer buffer = new Buffer();
        buffer.write(content);
        try {
            buffer.skip(1);
            modelL.material = buffer.readByte();
            modelL.caliber = buffer.readInt() / 1000.0f;
            modelR.material = buffer.readByte();
            modelR.caliber = buffer.readInt() / 1000.0f;
        } catch (EOFException e) {
            e.printStackTrace();
        }
        models.add(modelL);
        models.add(modelR);
        return models;
    }
}
