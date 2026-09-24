package fabscreen.libraries.legacy.data.serial.fabpacket.content;

import androidx.annotation.NonNull;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.List;

import okio.Buffer;

public class ExtruderTemperature {
    public int current;
    public int target;

    public ExtruderTemperature() {
    }

    public ExtruderTemperature(int current, int target) {
        this.current = current;
        this.target = target;
    }

    @NonNull
    @Override
    public String toString() {
        return "ExtruderTemperature{" +
                "current=" + current +
                ", target=" + target +
                '}';
    }

    public static List<ExtruderTemperature> parse(byte[] content) {
        List<ExtruderTemperature> temperatures = new ArrayList<>();
        ExtruderTemperature temperatureL = new ExtruderTemperature();
        ExtruderTemperature temperatureR = new ExtruderTemperature();

        Buffer buffer = new Buffer();
        buffer.write(content);

        try {
            buffer.skip(1);
            temperatureL.current = buffer.readShort();
            temperatureL.target = buffer.readShort();
            temperatureR.current = buffer.readShort();
            temperatureR.target = buffer.readShort();
        } catch (EOFException e) {
            e.printStackTrace();
        }

        temperatures.add(temperatureL);
        temperatures.add(temperatureR);
        return temperatures;
    }
}
