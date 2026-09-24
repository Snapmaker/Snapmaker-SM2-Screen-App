package fabscreen.libraries.legacy.data.print;

@Deprecated
public class OnAirCommand {
    private float value;
    private boolean dirty;

    OnAirCommand() {
        value = 0;
        dirty = false;
    }

    void set(float value) {
        set(value, true);
    }

    void set(float value, boolean dirty) {
        this.value = value;
        this.dirty = dirty;
    }
}
