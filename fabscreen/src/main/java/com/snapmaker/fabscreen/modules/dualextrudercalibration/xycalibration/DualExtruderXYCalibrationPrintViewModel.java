package com.snapmaker.fabscreen.modules.dualextrudercalibration.xycalibration;

import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.print.MachinePrintJobState;
import fabscreen.libraries.legacy.lib.file.IFile;
import fabscreen.libraries.legacy.lib.parser.GcodeParser;
import fabscreen.libraries.legacy.lib.parser.IGcodeParser;
import io.reactivex.Observable;

public class DualExtruderXYCalibrationPrintViewModel extends BaseViewModel {
    private IFile mPrintFile;

    private int mFileTotalLine;
    IGcodeParser mGcodeParser;

    public void setCalibrationPrintFile(IFile ifile) {
        mPrintFile = ifile;
    }

    public IFile getCalibrationPrintFile() {
        return mPrintFile;
    }

    public int getFileTotalLine() {
        return mFileTotalLine;
    }

    public Observable<Integer> parseFile() {
        if (mPrintFile == null) {
            return Observable.just(-1);
        }

        mGcodeParser = new GcodeParser();
        mGcodeParser.startParse(mPrintFile, Constants.FILE_TYPE_3DP);
        return mGcodeParser.getParseProgressObservable()
                .doOnNext(progress -> {
                    if (progress == 100) {
                        handleResult();
                        mFileTotalLine = mGcodeParser.getTotalLinesCount();
                    }
                });
    }

    private void handleResult() {
        getModel().getPrintController().setOverrideFeedRate(100);
    }

    public boolean isMachineStatePrinting() {
        int machinePrintStatus = getModel().getMachineController().getMachineStatus().printerStatus;
        final boolean isMachineStatusPrinting = machinePrintStatus == 3 || machinePrintStatus == 4;
        final boolean isPrintJobStatePrinting = MachinePrintJobState.isStatePrinting(getModel().getPrintController().getPrintJobState().getValue());
        return isMachineStatusPrinting && isPrintJobStatePrinting;
    }

    public void onDestroy() {
        if (mGcodeParser != null) {
            mGcodeParser.destroy();
            mGcodeParser = null;
        }
    }
}
