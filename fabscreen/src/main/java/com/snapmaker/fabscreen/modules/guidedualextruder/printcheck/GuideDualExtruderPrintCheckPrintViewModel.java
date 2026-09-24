package com.snapmaker.fabscreen.modules.guidedualextruder.printcheck;



import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.print.MachinePrintJobState;
import fabscreen.libraries.legacy.lib.file.IFile;
import fabscreen.libraries.legacy.lib.parser.GcodeParser;
import fabscreen.libraries.legacy.lib.parser.IGcodeParser;
import io.reactivex.Observable;

public class GuideDualExtruderPrintCheckPrintViewModel extends BaseViewModel {
    public static final int PRINT_RESULT_ALL_FAILED = -1;
    public static final int PRINT_RESULT_ALL_OK = 0;
    public static final int PRINT_RESULT_Z_OK = 1;
    public static final int PRINT_RESULT_XY_OK = 2;

    private IFile mPrintFile;

    private int mFileTotalLine;

    private boolean mPrintResultXY = false;
    private boolean mPrintResultZ = false;

    public void setPrintCheckFile(IFile ifile) {
        mPrintFile = ifile;
    }

    public IFile getPrintCheckPrintFile() {
        return mPrintFile;
    }

    public int getFileTotalLine() {
        return mFileTotalLine;
    }

    public void setPrintResultXY(boolean printResultXY) {
        mPrintResultXY = printResultXY;
    }

    public void setPrintResultZ(boolean printResultZ) {
        mPrintResultZ = printResultZ;
    }

    public boolean getPrintResultXY() {
        return mPrintResultXY;
    }

    public boolean getPrintResultZ() {
        return mPrintResultZ;
    }

    public int getPrintCheckResult() {
        boolean isAllOK = mPrintResultXY && mPrintResultZ;
        if (isAllOK) {
            return PRINT_RESULT_ALL_OK;
        } else if (mPrintResultZ) {
            return PRINT_RESULT_Z_OK;
        } else if (mPrintResultXY) {
            return PRINT_RESULT_XY_OK;
        } else {
            return PRINT_RESULT_ALL_FAILED;
        }
    }

    public boolean isMachineStatePrinting() {
        int machinePrintStatus = getModel().getMachineController().getMachineStatus().printerStatus;
        final boolean isMachineStatusPrinting = machinePrintStatus == 3 || machinePrintStatus == 4;
        final boolean isPrintJobStatePrinting = MachinePrintJobState.isStatePrinting(getModel().getPrintController().getPrintJobState().getValue());
        return isMachineStatusPrinting && isPrintJobStatePrinting;
    }

    public Observable<Integer> parseFile() {
        if (mPrintFile == null) {
            return Observable.just(-1);
        }

        IGcodeParser gcodeParser = new GcodeParser();
        gcodeParser.startParse(mPrintFile, Constants.FILE_TYPE_3DP);
        return gcodeParser.getParseProgressObservable()
                .doOnNext(progress -> {
                    if (progress == 100) {
                        mFileTotalLine = gcodeParser.getTotalLinesCount();
                    }
                });
    }
}
