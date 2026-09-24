package fabscreen.libraries.legacy.data.print;

public class PrintJobEvent {

    private PrintEventState mPrintEventState;
    private int mErrorCode;

    public PrintJobEvent(PrintEventState state, int errorCode) {
        mPrintEventState = state;
        mErrorCode = errorCode;
    }

    public PrintEventState getPrintEventState() {
        return mPrintEventState;
    }

    public int getErrorCode() {
        return mErrorCode;
    }

    @Override
    public String toString() {
        return "PrintJobEvent{" +
                "PrintEventState=" + mPrintEventState +
                ", ErrorCode=" + mErrorCode +
                '}';
    }
}
