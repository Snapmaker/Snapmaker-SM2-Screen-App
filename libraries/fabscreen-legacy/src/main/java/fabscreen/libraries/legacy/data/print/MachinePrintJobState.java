package fabscreen.libraries.legacy.data.print;

import java.util.HashSet;

public enum MachinePrintJobState {
    PRINT_JOB_STATE_IDLE(0),
    PRINT_JOB_STATE_STARTING(1),
    PRINT_JOB_STATE_PRINTING(2),
    PRINT_JOB_STATE_PAUSING(3),
    PRINT_JOB_STATE_PAUSED(4),
    PRINT_JOB_STATE_STOPPING(5),
    PRINT_JOB_STATE_STOPPED(6),
    PRINT_JOB_STATE_FINISHING(7),
    PRINT_JOB_STATE_FINISHED(8),
    PRINT_JOB_STATE_RECOVERING(9),
    PRINT_JOB_STATE_RESUMING(10);

    private static final HashSet<MachinePrintJobState> PRINT_STATE = new HashSet<MachinePrintJobState>() {{
        add(PRINT_JOB_STATE_STARTING);
        add(PRINT_JOB_STATE_PRINTING);
        add(PRINT_JOB_STATE_PAUSING);
        add(PRINT_JOB_STATE_PAUSED);
        add(PRINT_JOB_STATE_STOPPING);
        add(PRINT_JOB_STATE_STOPPED);
        add(PRINT_JOB_STATE_FINISHING);
        add(PRINT_JOB_STATE_RECOVERING);
        add(PRINT_JOB_STATE_RESUMING);
    }};

    private static final HashSet<MachinePrintJobState> PRINT_STATE_CHANGING = new HashSet<MachinePrintJobState>() {{
       add(PRINT_JOB_STATE_STARTING);
       add(PRINT_JOB_STATE_PAUSING);
       add(PRINT_JOB_STATE_RESUMING);
       add(PRINT_JOB_STATE_STOPPING);
       add(PRINT_JOB_STATE_FINISHING);
       add(PRINT_JOB_STATE_RECOVERING);
    }};


    private int value = 0;

    private MachinePrintJobState(int value) {
        this.value = value;
    }

    public static MachinePrintJobState valueOf(int value) {
        switch (value) {
            case 0:
                return PRINT_JOB_STATE_IDLE;
            case 1:
                return PRINT_JOB_STATE_STARTING;
            case 2:
                return PRINT_JOB_STATE_PRINTING;
            case 3:
                return PRINT_JOB_STATE_PAUSING;
            case 4:
                return PRINT_JOB_STATE_PAUSED;
            case 5:
                return PRINT_JOB_STATE_STOPPING;
            case 6:
                return PRINT_JOB_STATE_STOPPED;
            case 7:
                return PRINT_JOB_STATE_FINISHING;
            case 8:
                return PRINT_JOB_STATE_FINISHED;
            case 9:
                return PRINT_JOB_STATE_RECOVERING;
            case 10:
                return PRINT_JOB_STATE_RESUMING;
            default:
                return null;
        }
    }

    public int getValue() {
        return value;
    }

    public boolean valueEqual(int value) {
        return this.value == value;
    }

    public static boolean isStatePrinting(int value) {
        MachinePrintJobState machinePrintJobState = valueOf(value);
        if (machinePrintJobState == null) return false;

        return PRINT_STATE.contains(machinePrintJobState);
    }

    public static boolean isPrintStateChanging(int value) {
        MachinePrintJobState machinePrintJobState = valueOf(value);
        if (machinePrintJobState == null) return false;

        return PRINT_STATE_CHANGING.contains(machinePrintJobState);
    }
}
