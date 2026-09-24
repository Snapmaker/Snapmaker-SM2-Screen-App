package fabscreen.libraries.legacy.data.print;

public class BatchCode {
    String gcodes;
    int mStartNo;
    int mEndNo;

    public BatchCode(String gcodes, int mStartNo, int mEndNo) {
        this.gcodes = gcodes;
        this.mStartNo = mStartNo;
        this.mEndNo = mEndNo;
    }

    public String getGcodes() {
        return gcodes;
    }

    public void setGcodes(String gcodes) {
        this.gcodes = gcodes;
    }

    public int getStartNo() {
        return mStartNo;
    }

    public void setStartNo(int mStartNo) {
        this.mStartNo = mStartNo;
    }

    public int getEndNo() {
        return mEndNo;
    }

    public void setEndNo(int mEndNo) {
        this.mEndNo = mEndNo;
    }

    public int getLentNum() {
        return mEndNo - mStartNo;
    }
}
