package fabscreen.libraries.legacy.lib.parser;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.IOException;
import java.util.Locale;

import fabscreen.libraries.legacy.BaseApplication;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.model.ModelBoundary;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.lib.file.IFile;
import fabscreen.libraries.legacy.lib.file.IFileManager;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import okio.BufferedSource;
import okio.Okio;

public class SnapmakerParser implements IGcodeParser {

    private BufferedSource source;
    private String[] lineArgs = new String[20];
    private int lineArgCount;

    private static final String BOUND_HEADER_START_MARK = ";Header Start";
    private static final String BOUND_HEADER_END_MARK = ";Header End";
    private static boolean mIsHeaderMarkStart = false;

    // G-code state
    private ModelBoundary mModelBoundary;
    private int mFileType = Constants.FILE_TYPE_UNKNOWN;

    private float mBedTargetTemperature = 0;
    private float mNozzleTargetTemperature = 0;
    private float mNozzle1TargetTemperature = 0;

    private float mLaserPower = 0;
    private float mCNCPower = 0;

    private float mSpindleSpeed = 0;

    private float mEstimateTime = 0;
    private int mTotalLine = 0;

    private float mWorkSpeed = 0;
    private float mJogSpeed = 0;

    private float mDiameter = 0;

    private Bitmap mGcodeThumbnail;
    private BehaviorSubject<Integer> mParseProgressSubject = BehaviorSubject.createDefault(0);
    private Scheduler.Worker mParseWorker;

    public SnapmakerParser() {
        mModelBoundary = new ModelBoundary();
    }

    @Override
    public void startParse(IFile file, int fileType) {
        synchronized (SnapmakerParser.this) {
            mFileType = fileType;
            try {
                IFileManager iFileManager = file.isLocal() ?
                        BaseApplication.getInstance().getFabLocalFileManager() :
                        BaseApplication.getInstance().getFabUsbFileManager();
                source = Okio.buffer(Okio.source(iFileManager.getInputStream(file)));
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (mParseWorker == null) {
                mParseWorker = Schedulers.computation().createWorker();
            }

            mParseWorker.schedule(this::parse);
        }
    }

    @Override
    public void destroy() {
        if (mParseWorker != null) {
            mParseWorker.dispose();
            mParseWorker = null;
        }
    }

    private void parse() {
        String line;
        // Reset progress
        mParseProgressSubject.onNext(0);

        while (true) {
            try {
                line = source.readUtf8Line();

                if (line == null) {
                    mParseProgressSubject.onNext(100);
                    break;
                }

                // check end position
                if (line.equals(BOUND_HEADER_END_MARK)) {
                    mParseProgressSubject.onNext(100);
                    break;
                } else {
                    // fixme: Need to refactor format number for parsing args instead of throwing exception
                    parseLine(line);
                }
            } catch (IOException | NumberFormatException e) {
                e.printStackTrace();
                LogHelper.log(e);
                mParseProgressSubject.onNext(-1);
                return;
            }
        }
    }

    private void parseLine(final String line) throws NumberFormatException {
        if (line.isEmpty()) {
            return;
        }

        // skip line until find header start mark
        if (!checkHeaderStartMark(line)) {
            return;
        }

        if (line.charAt(0) == ';') {
            // parse here
            final int length = line.length();

            lineArgCount = 0;
            // skip comment mark
            int pos = 1;

            while (pos < length) {
                while (pos < length && line.charAt(pos) == ' ') pos++;

                if (pos == length) break;

                int start = pos;
                while (pos < length && line.charAt(pos) != ':') pos++;

                lineArgs[lineArgCount++] = line.substring(start, pos);

                pos++;
            }

            if (lineArgCount == 0) return;

            // Parse arguments based on G-code command
            switch (lineArgs[0]) {
                case "header_type":
                    parseHeaderType();
                    break;
                case "file_total_lines":
                    parseTotalLine();
                    break;
                case "estimated_time(s)":
                    parseEstimatedTime();
                    break;
                case "min_x(mm)":
                    parseMinX();
                    break;
                case "max_x(mm)":
                    parseMaxX();
                    break;
                case "min_y(mm)":
                    parseMinY();
                    break;
                case "max_y(mm)":
                    parseMaxY();
                    break;
                case "min_z(mm)":
                    parseMinZ();
                    break;
                case "max_z(mm)":
                    parseMaxZ();
                    break;
                case "min_b(mm)":
                    parseMinB();
                case "max_b(mm)":
                    parseMaxB();
                case "nozzle_temperature(°C)":
                    parseNozzleTemperature();
                    break;
                case "build_plate_temperature(°C)":
                    parseHeatedBedTemperature();
                    break;
                case "spindle_speed(mm/minute)":
                    parseSpindleSpeed();
                    break;
                case "power(%)":
                    parsePower();
                    break;
                case "work_speed(mm/minute)":
                    parseWorkSpeed();
                    break;
                case "jog_speed(mm/minute)":
                    parseJogSpeed();
                    break;
                case "diameter":
                    parseDiameter();

                default:
                    break;
            }
        }
    }

    private boolean checkHeaderStartMark(String line) {
        if (mIsHeaderMarkStart) {
            return true;
        }

        mIsHeaderMarkStart = line.equals(BOUND_HEADER_START_MARK);
        return mIsHeaderMarkStart;
    }

    private void parseHeaderType() {
        switch (lineArgs[1]) {
            case "3dp":
                mFileType = Constants.FILE_TYPE_3DP;
                break;
            case "laser":
                mFileType = Constants.FILE_TYPE_LASER;
                break;
            case "cnc":
                mFileType = Constants.FILE_TYPE_CNC;
                break;
            default:
                break;
        }
    }

    private void parseEstimatedTime() throws NumberFormatException {
        mEstimateTime = Float.valueOf(lineArgs[1]);
    }

    private void parseMinX() throws NumberFormatException {
        mModelBoundary.setMinX(Float.valueOf(lineArgs[1]));
    }

    private void parseMaxX() throws NumberFormatException {
        mModelBoundary.setMaxX(Float.valueOf(lineArgs[1]));
    }

    private void parseMinY() throws NumberFormatException {
        mModelBoundary.setMinY(Float.valueOf(lineArgs[1]));
    }

    private void parseMaxY() throws NumberFormatException {
        mModelBoundary.setMaxY(Float.valueOf(lineArgs[1]));
    }

    private void parseMinZ() throws NumberFormatException {
        mModelBoundary.setMinZ(Float.valueOf(lineArgs[1]));
    }

    private void parseMaxZ() throws NumberFormatException {
        mModelBoundary.setMaxZ(Float.valueOf(lineArgs[1]));
    }

    private void parseMinB() throws NumberFormatException {
        mModelBoundary.setMinB(Float.valueOf(lineArgs[1]));
    }

    private void parseMaxB() throws NumberFormatException {
        mModelBoundary.setMaxB(Float.valueOf(lineArgs[1]));
    }

    private void parseTotalLine() throws NumberFormatException {
        mTotalLine = Integer.valueOf(lineArgs[1]);
    }

    private void parseNozzleTemperature() throws NumberFormatException {
        mNozzleTargetTemperature = Float.valueOf(lineArgs[1]);
    }

    private void parseHeatedBedTemperature() throws NumberFormatException {
        mBedTargetTemperature = Float.valueOf(lineArgs[1]);
    }

    private void parseSpindleSpeed() throws NumberFormatException {
        // spindle speed?
        if (mFileType == Constants.FILE_TYPE_CNC) {
            mSpindleSpeed = Float.valueOf(lineArgs[1]);
        }
    }

    private void parsePower() throws NumberFormatException {
        if (mFileType == Constants.FILE_TYPE_LASER) {
            mLaserPower = Float.valueOf(lineArgs[1]);
        } else if (mFileType == Constants.FILE_TYPE_CNC) {
            mCNCPower = Float.valueOf(lineArgs[1]);
        }
    }

    private void parseWorkSpeed() throws NumberFormatException {
        mWorkSpeed = Float.valueOf(lineArgs[1]);
    }

    private void parseJogSpeed() throws NumberFormatException {
        mJogSpeed = Float.valueOf(lineArgs[1]);
    }

    private void parseDiameter() throws NumberFormatException {
        mDiameter = Float.valueOf(lineArgs[1]);
    }

    @Override
    public int getFileType() {
        return mFileType;
    }

    @Override
    public Bitmap getGcodeThumbnail() {
        return mGcodeThumbnail;
    }

    @Override
    public int getTotalLinesCount() {
        return mTotalLine;
    }

    @Override
    public float getEstimatedTime() {
        return mEstimateTime;
    }

    @Override
    public float getBedTargetTemperature() {
        return mBedTargetTemperature;
    }

    @Override
    public float getNozzleTargetTemperature() {
        return mNozzleTargetTemperature;
    }

    @Override
    public float getNozzle1TargetTemperature() {
        return mNozzle1TargetTemperature;
    }

    @Override
    public float getLaserPower() {
        return mLaserPower;
    }

    @Override
    public float getCNCPower() {
        return mCNCPower;
    }

    @Override
    public float getSpindleSpeed() {
        return mSpindleSpeed;
    }

    @Override
    public float getWorkSpeed() {
        return mWorkSpeed;
    }

    @Override
    public float getJogSpeed() {
        return mJogSpeed;
    }

    @Override
    public float getDiameter() {
        return mDiameter;
    }

    @Override
    public ModelBoundary getBoundary() {
        return mModelBoundary;
    }

    @Override
    public Observable<Integer> getParseProgressObservable() {
        return mParseProgressSubject;
    }

    @Override
    public float getExtruder0RetractionDistance() {
        return 0;
    }

    @Override
    public float getExtruder1RetractionDistance() {
        return 0;
    }

    @Override
    public float getExtruder0SwitchRetractionDistance() {
        return 0;
    }

    @Override
    public float getExtruder1SwitchRetractionDistance() {
        return 0;
    }

    private void debug() {
        Log.e("DEBUG", String.format(Locale.getDefault(),
                "HeadType %d EstimatedTime %.2f TotalLine %d Nozzle %.2f Bed %.2f WorkSpeed %.2f JogSpeed %.2f Laser Power %.2f CNC Power %.2f SpindleSpeed %.2f boundary %s",
                mFileType, mEstimateTime, mTotalLine,
                mNozzleTargetTemperature, mBedTargetTemperature,
                mWorkSpeed, mJogSpeed,
                mLaserPower, mCNCPower,
                mSpindleSpeed, mModelBoundary));
    }
}
