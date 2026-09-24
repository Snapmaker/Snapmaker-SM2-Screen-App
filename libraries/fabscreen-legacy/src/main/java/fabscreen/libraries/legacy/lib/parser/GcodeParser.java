package fabscreen.libraries.legacy.lib.parser;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.ArrayList;

import fabscreen.libraries.legacy.BaseApplication;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.model.ModelBoundary;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.lib.StringHelper;
import fabscreen.libraries.legacy.lib.file.IFile;
import fabscreen.libraries.legacy.lib.file.IFileManager;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import okio.BufferedSource;
import okio.Okio;

public class GcodeParser implements IGcodeParser {
    private static String TAG = "GcodeParser";

    // comment marker
    private static String BOUNDS_START_MARK = ";Start GCode end";
    private static String BOUNDS_END_MARK = ";End GCode begin";
    private static String BOUNDS_END_MARK_V1 = ";--- End G-code Begin ---";

    private static final String BOUND_HEADER_START_MARK = ";Header Start";
    private static final String BOUND_HEADER_END_MARK = ";Header End";
    private boolean mIsParsingHeader = false;

    // Reader
    private long totalBytes;
    private long readBytes;
    private BufferedSource source;
    private String[] lineArgs = new String[20];
    private int lineArgCount;

    private int mParseProgress = 0;

    // Header parameters
    // header_type
    private int mFileType = Constants.FILE_TYPE_UNKNOWN;
    private Bitmap mGcodeThumbnail;
    private int mTotalLinesCount = 0;
    private float mEstimateTime = 0;

    // Boundary (x, y, z, b)
    private ModelBoundary mModelBoundary;

    // 3dp params
    private float mNozzleTargetTemperature = 0;
    private float mNozzle1TargetTemperature = 0;
    private float mBedTargetTemperature = 0;

    private float mExtruder0RetractionDistance = 0;
    private float mExtruder0SwitchRetractionDistance = 0;
    private float mExtruder1RetractionDistance = 0;
    private float mExtruder1SwitchRetractionDistance = 0;

    private float mLastEAxisPosition = 0;
    private int mCurrentExtruder = 0;
    private int mT0RetractionCount = 0;
    private int mT1RetractionCount = 0;
    private boolean mFDMRetractionParamAcquired = false;
    private boolean mIsDefineT0 = false;
    private boolean mIsDefineT1 = false;

    // cnc params
    private float mSpindleSpeed = 0;
    private float mCNCPower = 0;

    // Laser
    private float mLaserPower = 0;

    // speed
    private float mWorkSpeed = 0;
    private float mJogSpeed = 0;

    // 4Axis material
    private float mDiameter = 0;

    // G-code parse parameters
    // G-code Position(point) and tool paths
    private boolean mIsCurrentGcodeStateWorking = false;
    @NonNull
    private Position currentPosition;
    private ArrayList<Position> mToolPath = new ArrayList<>();

    // Feed rate Calculation
    private float mCurrentLineFeedRate = 0;
    private double mFeedRateAmount = 0;
    private int mFeedRateCount = 0;

    // Extrusion
    private float extrusionAmount = 0;

    private boolean mShouldUpdateAttribute = true;
    private boolean isAbsoluteCoordinate = true;

    private BehaviorSubject<Integer> mParseProgressSubject = BehaviorSubject.createDefault(0);
    private Scheduler.Worker mParseWorker;
    private HeaderParamsChecker mHeaderChecker;

    public GcodeParser() {
        mModelBoundary = new ModelBoundary();
        currentPosition = new Position(0, 0, 0);
    }

    @Override
    public void destroy() {
        if (mParseWorker != null) {
            mParseWorker.dispose();
            mParseWorker = null;
        }

        if (mToolPath != null) {
            mToolPath.clear();
        }
    }

    @Override
    public void startParse(IFile file, int fileType) {
        synchronized (GcodeParser.this) {
            mFileType = fileType;

            try {
                totalBytes = file.length();
                readBytes = 0;
                IFileManager iFileManager = file.isLocal() ?
                        BaseApplication.getInstance().getFabLocalFileManager() :
                        BaseApplication.getInstance().getFabUsbFileManager();
                source = Okio.buffer(Okio.source(iFileManager.getInputStream(file)));
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Create parse worker if not exist
            if (mParseWorker == null) {
                mParseWorker = Schedulers.computation().createWorker();
            }

            // Start parse here in computation scheduler
            mParseWorker.schedule(this::parse);
        }
    }

    private void parse() {
        String line;
        int linesCount = 0;
        int newLineBytes = 1;

        // Peek to count total lines
        try {
            BufferedSource peek = source.peek();
            // https://github.com/square/okio/blob/master/okio/jvm/src/main/java/okio/Buffer.kt#L648
            // Check if the file uses "\n" or "\r\n", which will affect our byte calculation
            long carriage = peek.indexOf((byte) ('\r'), 0, 64);
            if (carriage != -1) {
                newLineBytes = 2;
            }
            peek.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Reset progress
        mParseProgressSubject.onNext(0);
        mTotalLinesCount = 0;
        mIsParsingHeader = false;
        mHeaderChecker = HeaderParamsChecker.getInstance();

        while (true) {
            try {
                line = source.readUtf8Line();

                if (line == null) break;

                // stop parsing if header exists and get totalLinesCount
                if (checkParamsRequired() && !mIsParsingHeader) {
                    break;
                }

                // Update progress
                readBytes += line.length() + newLineBytes;
                mParseProgress = (int) (100 * readBytes / totalBytes);
                if (mParseProgress <= 100 && mParseProgress > mParseProgressSubject.getValue()) {
                    mParseProgressSubject.onNext(mParseProgress);
                }

                // fixme: Need to refactor format number for parsing args instead of throwing exception
                parseLine(line);
                linesCount++;
            } catch (IOException | NumberFormatException e) {
                e.printStackTrace();
                LogHelper.log(e);
                mParseProgressSubject.onNext(-1);
                return;
            }
        }
        if (!mHeaderChecker.isEstimatedTimeCheck()) {
            mTotalLinesCount = (mTotalLinesCount == 0) ? linesCount : mTotalLinesCount;
        }
        checkPathClose();

        // Set progress to 100
        mParseProgressSubject.onNext(100);
    }

    private boolean checkParamsRequired() {
        return mTotalLinesCount != 0 && (mFileType != Constants.FILE_TYPE_3DP || mFDMRetractionParamAcquired);
    }

    private void parseLine(final String line) throws NumberFormatException {
        if (line.isEmpty()) return;

        // Straight comment
        if (line.charAt(0) == ';') {
            if (containsEndGcodeMark(line)) {
                mShouldUpdateAttribute = false;
            }

            //  parse header markers
            if (mIsParsingHeader) {
                if (line.equals(BOUND_HEADER_END_MARK)) {
                    mIsParsingHeader = false;
                } else {
                    parseHeader(line);
                }
            } else {
                if (line.equals(BOUND_HEADER_START_MARK)) {
                    mIsParsingHeader = true;
                }
            }
            return;
        }

        // Parse line to separate arguments
        final int length = line.length();

        lineArgCount = 0;
        int pos = 0;
        while (pos < length) {
            while (pos < length && line.charAt(pos) == ' ') pos++;

            if (pos == length) break;
            if (line.charAt(pos) == ';') break;

            int start = pos;
            pos++;
            while (pos < length) {
                char c = line.charAt(pos);
                if (c == ' ' || c == ';' || StringHelper.isAlphabetic(c)) break;
                pos++;
            }

            lineArgs[lineArgCount++] = line.substring(start, pos);
            if (lineArgCount >= 20) break;
        }

        if (lineArgCount == 0) return;

        // Parse arguments based on G-code command
        switch (lineArgs[0]) {
            case "G0":
            case "G1": {
                parseG0G1();
                break;
            }
            case "G4": {
                parseG4();
                break;
            }
            case "G20":
            case "G21": {
                break;
            }
            case "G28": {
//                parseG28();
                break;
            }
            case "G90": {
                // absolute position
                isAbsoluteCoordinate = true;
                break;
            }
            case "G91": {
                // relative position
                isAbsoluteCoordinate = false;
                break;
            }
            case "G92": {
                parseG92();
                break;
            }
            case "M3":
            case "M5": {
                if (mHeaderChecker.isLaserPowerCheck()) break;

                parseM3M5();
                break;
            }
            case "M83": {
                isAbsoluteCoordinate = false;
            }
            case "M140":
            case "M190": {
                // heated bed
                if (mHeaderChecker.isHeatedBedTempCheck()) break;

                parseHeatedBedTemperature();
                break;
            }
            case "M104":
            case "M109": {
                // nozzle
                if (mHeaderChecker.isNozzleTempCheck()) break;

                parseNozzleTemperature();
                break;
            }
            case "T0":
                mIsDefineT0 = true;
                mCurrentExtruder = 0;
                break;
            case "T1":
                mIsDefineT1 = true;
                mCurrentExtruder = 1;
                break;
            default:
                break;
        }
    }

    private void checkPathStart() {
        if (!mIsCurrentGcodeStateWorking) {
            mIsCurrentGcodeStateWorking = true;

            currentPosition.setAsStartPoint();
            mToolPath.add(currentPosition);
        }
    }

    private void checkPathClose() {
        if (mIsCurrentGcodeStateWorking) {
            mIsCurrentGcodeStateWorking = false;

            currentPosition.setAsEndPoint();
        }
    }

    private void parseHeader(String line) throws NumberFormatException {
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
            case "header_type": {
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
                break;
            }
            case "thumbnail": {
                try {
                    byte[] bitmapArray = Base64.decode(lineArgs[2].split(",")[1], Base64.DEFAULT);
                    mGcodeThumbnail = BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length);
                } catch (IllegalArgumentException e) {
                    LogHelper.log(e);
                }
                break;
            }
            case "file_total_lines": {
                mTotalLinesCount = Integer.parseInt(lineArgs[1]);
                mHeaderChecker.setTotalLinesCheck(true);
                break;
            }
            case "estimated_time(s)": {
                mEstimateTime = Float.parseFloat(lineArgs[1]);
                mHeaderChecker.setEstimatedTimeCheck(true);
                break;
            }
            case "min_x(mm)": {
                mModelBoundary.setMinX(Float.parseFloat(lineArgs[1]));
                mHeaderChecker.setBoundaryCheck(true);
                break;
            }
            case "max_x(mm)": {
                mModelBoundary.setMaxX(Float.parseFloat(lineArgs[1]));
                mHeaderChecker.setBoundaryCheck(true);
                break;
            }
            case "min_y(mm)": {
                mModelBoundary.setMinY(Float.parseFloat(lineArgs[1]));
                mHeaderChecker.setBoundaryCheck(true);
                break;
            }
            case "max_y(mm)": {
                mModelBoundary.setMaxY(Float.parseFloat(lineArgs[1]));
                mHeaderChecker.setBoundaryCheck(true);
                break;
            }
            case "min_z(mm)": {
                mModelBoundary.setMinZ(Float.parseFloat(lineArgs[1]));
                mHeaderChecker.setBoundaryCheck(true);
                break;
            }
            case "max_z(mm)": {
                mModelBoundary.setMaxZ(Float.parseFloat(lineArgs[1]));
                mHeaderChecker.setBoundaryCheck(true);
                break;
            }
            case "min_b(mm)": {
                mModelBoundary.setMinB(Float.parseFloat(lineArgs[1]));
                mHeaderChecker.setBoundaryCheck(true);
                break;
            }
            case "max_b(mm)": {
                mModelBoundary.setMaxB(Float.parseFloat(lineArgs[1]));
                mHeaderChecker.setBoundaryCheck(true);
                break;
            }
            case "nozzle_temperature(°C)": {
                mNozzleTargetTemperature = Float.parseFloat(lineArgs[1]);
                mHeaderChecker.setNozzleTempCheck(true);
                break;
            }
            case "nozzle_1_temperature(°C)": {
                mNozzle1TargetTemperature = Float.parseFloat(lineArgs[1]);
                mHeaderChecker.setNozzleTempCheck(true);
                break;
            }
            case "build_plate_temperature(°C)": {
                mBedTargetTemperature = Float.parseFloat(lineArgs[1]);
                mHeaderChecker.setHeatedBedTempCheck(true);
                break;
            }
            case "spindle_speed(mm/minute)": {
                if (mFileType == Constants.FILE_TYPE_CNC) {
                    mSpindleSpeed = Float.parseFloat(lineArgs[1]);
                }
                break;
            }
            case "power(%)": {
                if (mFileType == Constants.FILE_TYPE_LASER) {
                    mLaserPower = Float.parseFloat(lineArgs[1]);
                    mHeaderChecker.setLaserPowerCheck(true);
                } else if (mFileType == Constants.FILE_TYPE_CNC) {
                    mCNCPower = Float.parseFloat(lineArgs[1]);
                }
                break;
            }
            case "work_speed(mm/minute)": {
                mWorkSpeed = Float.parseFloat(lineArgs[1]);
                break;
            }
            case "jog_speed(mm/minute)": {
                mJogSpeed = Float.parseFloat(lineArgs[1]);
                break;
            }
            case "diameter": {
                mDiameter = Float.parseFloat(lineArgs[1]);
                break;
            }
            case "Extruder 0 Retraction Distance": {
                mExtruder0RetractionDistance = Float.parseFloat(lineArgs[1]);
                mFDMRetractionParamAcquired = true;
                mHeaderChecker.setExtruder0RetractionCheck(true);
                break;
            }
            case "Extruder 0 Switch Retraction Distance": {
                mExtruder0SwitchRetractionDistance = Float.parseFloat(lineArgs[1]);
                break;
            }
            case "Extruder 1 Retraction Distance": {
                mExtruder1RetractionDistance = Float.parseFloat(lineArgs[1]);
                mFDMRetractionParamAcquired = true;
                mHeaderChecker.setExtruder1RetractionCheck(true);
                break;
            }
            case "Extruder 1 Switch Retraction Distance": {
                mExtruder1SwitchRetractionDistance = Float.parseFloat(lineArgs[1]);
                break;
            }
            default:
                break;
        }
    }

    /**
     * Parse G0 and G1 command from lineArgs.
     * <p>
     * Examples:
     * G1 F1500
     * G1 X90.6 Y13.8 E22.4 F3000
     * G1 X80 Y20 E36 F1500
     * G0 F2400 X49.071 Y22.466 E0.43903
     */
    private void parseG0G1() throws NumberFormatException {
        float x = 0, y = 0, z = 0, e, feedRate;

        x = currentPosition.x;
        y = currentPosition.y;
        z = currentPosition.z;

        for (int i = 1; i < lineArgCount; i++) {
            switch (lineArgs[i].charAt(0)) {
                case 'X': {
                    x = Float.parseFloat(lineArgs[i].substring(1));
                    break;
                }
                case 'Y': {
                    y = Float.parseFloat(lineArgs[i].substring(1));
                    break;
                }
                case 'Z': {
                    z = Float.parseFloat(lineArgs[i].substring(1));
                    break;
                }
                case 'E': {
                    if (mShouldUpdateAttribute) {
                        e = Float.parseFloat(lineArgs[i].substring(1));
                        extrusionAmount = isAbsoluteCoordinate ? (e) : (extrusionAmount + e);
                        if (mFileType == Constants.FILE_TYPE_3DP && !mHeaderChecker.isExtruder0RetractionCheck() && !mHeaderChecker.isExtruder1RetractionCheck()) {
                            calculateRetraction(e);
                        }
                    }
                    break;
                }
                case 'F': {
                    if (mShouldUpdateAttribute) {
                        feedRate = Float.parseFloat(lineArgs[i].substring(1));
                        if (feedRate != 0 && feedRate != mCurrentLineFeedRate) {
                            mCurrentLineFeedRate = feedRate;
                        }
                    }
                    break;
                }
            }
        }

        boolean pointMoved = (x != currentPosition.x || y != currentPosition.y || z != currentPosition.z);
        if (!pointMoved) {
            return;
        }

        Position position = new Position(x, y, z);

        if (lineArgs[0].equals("G0")) {
            checkPathClose();

            // Calculate ETA
            if (mCurrentLineFeedRate != 0 && !mHeaderChecker.isEstimatedTimeCheck()) {
                final float segmentTime = currentPosition.distanceTo(position) / mCurrentLineFeedRate * 60;
                mEstimateTime += segmentTime;
            }

            // Update current position
            currentPosition = position;

            mModelBoundary.updateBoundary(position);
        } else if (lineArgs[0].equals("G1")) {
            checkPathStart();

            if (mCurrentLineFeedRate != 0 && !mHeaderChecker.isEstimatedTimeCheck()) {
                final float segmentTime = currentPosition.distanceTo(position) / mCurrentLineFeedRate * 60;
                mEstimateTime += segmentTime;

                // Calculate average work speed FeedRate
                mFeedRateAmount += mCurrentLineFeedRate;
                mFeedRateCount++;
            }

            currentPosition = position;

//            mToolPath.add(position);
            if (!mHeaderChecker.isBoundaryCheck()) {
                mModelBoundary.updateBoundary(position);
            }
        }
    }

    private void calculateRetraction(float ePosition) {
        // Absolute mode
        float delta = isAbsoluteCoordinate ? ePosition - mLastEAxisPosition : ePosition;
        if (delta < 0 && delta > -8.0f) {
//            Logger.d("retraction detected T%d %.4f", mCurrentExtruder, delta);
//            Logger.d("gcode %s", Arrays.toString(lineArgs));
            if (mCurrentExtruder == 0) {
                if (mT0RetractionCount == 0) {
                    mExtruder0RetractionDistance = -delta;
                }

                if (mExtruder0RetractionDistance != -delta) {
                    mT0RetractionCount = 1;
                }

                if (mT0RetractionCount > 3 && mExtruder0RetractionDistance > -delta) {
                    mExtruder0RetractionDistance = -delta;
                }
                mT0RetractionCount++;
            } else {
                if (mT1RetractionCount == 0) {
                    mExtruder1RetractionDistance = -delta;
                }

                if (mExtruder1RetractionDistance != -delta) {
                    mT1RetractionCount = 1;
                }

                if (mT1RetractionCount > 3 && mExtruder1RetractionDistance > -delta) {
                    mExtruder1RetractionDistance = -delta;
                }
                mT1RetractionCount++;
            }
//            Logger.d("T0 retracted %.2f at %d times, T1 retracted %.2f at %d times",
//                    mExtruder0RetractionDistance, mT0RetractionCount,
//                    mExtruder1RetractionDistance, mT1RetractionCount);
        }
        mLastEAxisPosition = ePosition;
    }


    /**
     * G4
     */
    private void parseG4() throws NumberFormatException {
        for (int i = 1; i < lineArgCount; i++) {
            switch (lineArgs[i].charAt(0)) {
                case 'P': {
                    final float dwellTime = Float.parseFloat(lineArgs[i].substring(1));
                    mEstimateTime += dwellTime * 0.001;
                    break;
                }
            }
        }
    }

    private void parseG28() {
        // TODO: implement an offset for absolute position
        if (lineArgCount < 2) {
            checkPathClose();
            currentPosition = new Position(0, 0, 0);
        } else {
            float x = currentPosition.x;
            float y = currentPosition.y;
            float z = currentPosition.z;

            for (int i = 1; i < lineArgCount; i++) {
                switch (lineArgs[i].charAt(0)) {
                    case 'X': {
                        x = 0;
                        break;
                    }
                    case 'Y': {
                        y = 0;
                        break;
                    }
                    case 'Z': {
                        z = 0;
                        break;
                    }
                }
            }

            checkPathClose();
            currentPosition = new Position(x, y, z);
        }
    }

    /**
     * G92
     */
    private void parseG92() {
        // TODO: implement an offset for absolute position
        for (int i = 1; i < lineArgCount; i++) {
            if (lineArgs[i].charAt(0) == 'E') {
                mLastEAxisPosition = Float.parseFloat(lineArgs[i].substring(1));
            }
        }
    }

    private void parseM3M5() throws NumberFormatException {
        for (int i = 1; i < lineArgCount; i++) {
            switch (lineArgs[i].charAt(0)) {
                case 'S': {
                    if (mFileType == Constants.FILE_TYPE_LASER) {
                        mLaserPower = Float.parseFloat(lineArgs[i].substring(1)) / 255 * 100;
                    } else if (mFileType == Constants.FILE_TYPE_CNC) {
                        mCNCPower = Float.parseFloat(lineArgs[i].substring(1)) / 255 * 100;
                    }
                    break;
                }
                case 'P': {
                    if (mFileType == Constants.FILE_TYPE_LASER) {
                        mLaserPower = Float.parseFloat(lineArgs[i].substring(1));
                    } else if (mFileType == Constants.FILE_TYPE_CNC) {
                        mCNCPower = Float.parseFloat(lineArgs[i].substring(1));
                    }
                    break;
                }
            }
        }
    }

    private void parseHeatedBedTemperature() throws NumberFormatException {
        for (int i = 1; i < lineArgCount; i++) {
            if (lineArgs[i].charAt(0) == 'S') {
                float temperature = Float.parseFloat(lineArgs[i].substring(1));
                if (mBedTargetTemperature < temperature) {
                    mBedTargetTemperature = temperature;
                }
            }
        }
    }

    private void parseNozzleTemperature() throws NumberFormatException {
        for (int i = 1; i < lineArgCount; i++) {
            if (lineArgs[i].charAt(0) == 'S') {
                float temperature = Float.parseFloat(lineArgs[i].substring(1));
                if (mNozzleTargetTemperature < temperature) {
                    mNozzleTargetTemperature = temperature;
                }
            }
        }
    }

    private boolean containsEndGcodeMark(String line) {
        return line.contains(BOUNDS_END_MARK) || line.contains(BOUNDS_END_MARK_V1);
    }

    public ArrayList<Position> getToolPath() {
        return mToolPath;
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
        return mTotalLinesCount;
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
    public float getWorkSpeed() {
        if (mWorkSpeed != 0) {
            return mWorkSpeed;
        } else {
            if (mFeedRateCount == 0) return 0;

            return (float) mFeedRateAmount / mFeedRateCount;
        }
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
    public float getSpindleSpeed() {
        return mSpindleSpeed;
    }

    public float getAverageFeedRate() {
        if (mFeedRateCount == 0) return 0;

        return (float) mFeedRateAmount / mFeedRateCount;
    }

    @Override
    public float getExtruder0RetractionDistance() {
        return mExtruder0RetractionDistance;
    }

    @Override
    public float getExtruder1RetractionDistance() {
        return mExtruder1RetractionDistance;
    }

    @Override
    public float getExtruder0SwitchRetractionDistance() {
        return mExtruder0SwitchRetractionDistance;
    }

    @Override
    public float getExtruder1SwitchRetractionDistance() {
        return mExtruder1SwitchRetractionDistance;
    }

    @Override
    public float getEstimatedTime() {
        return mEstimateTime;
    }

    @Override
    public ModelBoundary getBoundary() {
        return mModelBoundary;
    }

    @Override
    public Observable<Integer> getParseProgressObservable() {
        return mParseProgressSubject;
    }


}
