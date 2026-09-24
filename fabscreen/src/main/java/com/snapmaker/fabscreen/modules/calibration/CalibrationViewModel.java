package com.snapmaker.fabscreen.modules.calibration;

import android.annotation.SuppressLint;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;

import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.calibration.CalibrationPoint;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

/**
 * We manage the lifecycle of this ViewModel by ourself, instead of AutoDispose.
 */
@SuppressLint("AutoDispose")
public class CalibrationViewModel extends BaseViewModel {

    private final static int[][] mDirection = {{0, 1}, {-1, 0}, {0, -1}, {1, 0}}; // right, up, left, down, rotate with counterclockwise
    private int mCurrentDir = 0;

    private int mGrid;
    private boolean mIsAutoMode;
    private boolean mIsHeatedLevelingOn;
    private ArrayList<CalibrationPoint> mPoints;
    private CalibrationPoint mCurrentPoint;
    private int mCalibratedPointCount = 0;
    private float mHeatedLevelingTemperature = 0;

    private BehaviorSubject<Boolean> mAutoZOffsetSubject = BehaviorSubject.createDefault(false);
    private BehaviorSubject<Boolean> mIsAdjustZOffsetSubject = BehaviorSubject.createDefault(false);
    private BehaviorSubject<Boolean> mIsMovingSubject = BehaviorSubject.createDefault(false);
    private BehaviorSubject<Boolean> mUpdatePointsSubject = BehaviorSubject.createDefault(false);
    private PublishSubject<Boolean> mAutoCalibrationResultSubject = PublishSubject.create();

    private final CompositeDisposable mDisposables = new CompositeDisposable();

    public CalibrationViewModel() {
        super();

        int calibrationMode = getModel().getPreferences().get3DPCalibrationMode();
        mIsAutoMode = (calibrationMode == 0);

        mIsHeatedLevelingOn = getModel().getPreferences().get3DPCalibrationHeatedLevelingOn();

        mGrid = getModel().getPreferences().get3DPCalibrationGrid();
    }

    public Observable<Boolean> getIsMovingObservable() {
        return mIsMovingSubject.hide();
    }

    public int getGridCount() {
        return mGrid;
    }

    public Observable<Boolean> getUpdatePointsObservable() {
        return mUpdatePointsSubject.hide();
    }

    public boolean isAdjustZOffset() {
        //noinspection ConstantConditions
        return mIsAdjustZOffsetSubject != null && mIsAdjustZOffsetSubject.getValue();
    }

    public Observable<Boolean> getOffsetObservable() {
        return mAutoZOffsetSubject.hide();
    }

    public Observable<Boolean> getAutoCalibrationResultObservable() {
        return mAutoCalibrationResultSubject.hide();
    }

    public boolean isAutoMode() {
        return mIsAutoMode;
    }

    public void setAutoMode(boolean isAutoMode) {
        mIsAutoMode = isAutoMode;
    }

    public boolean isHeatedLevelingOn() {
        return mIsHeatedLevelingOn;
    }

    public void setHeatedLevelingTemperature(float temperature) {
        mHeatedLevelingTemperature = temperature;
    }

    public float getHeatedLevelingTemperature() {
        return mHeatedLevelingTemperature;
    }

    public void turnOffBed() {
        // Turn off heated bed using M140 G-code.
        mDisposables.add(getModel().getSlaveComputer()
                .sendGcode("M140 S0")
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {/**/}, LogHelper::log));
    }

    public void startCalibration() {
        mIsMovingSubject.onNext(true);
        if (mIsAutoMode) {
            // listen auto calibration progress
            mDisposables.add(getModel().getSlaveComputer()
                    .getAutoCalibrationProgress()
                    .subscribe(order -> {
                        Logger.d("Point %d calibrated.", order);
                        mCalibratedPointCount++;

                        moveToNextPoint();
                        mUpdatePointsSubject.onNext(true);
                    }));

            mDisposables.add(getModel().getSlaveComputer()
                    .startAutoCalibration(mGrid)
                    .subscribe(completed -> {
                        if (completed) {
                            // Set ZOffset after auto calibration completed.
                            mAutoZOffsetSubject.onNext(true);
                            mCurrentPoint.setActivated(false);
                            mIsMovingSubject.onNext(false);
                        } else {
                            // Auto calibration fail.
                            mAutoCalibrationResultSubject.onNext(false);
                        }
                    }, e -> {
                        LogHelper.log(e);
                        mIsMovingSubject.onNext(false);
                        mAutoCalibrationResultSubject.onNext(false);
                    }));

            // start with the corner
            mCurrentPoint = getPoint(mGrid, 1);
        } else {
            mDisposables.add(getModel().getSlaveComputer()
                    .startManualCalibration(mGrid)
                    .subscribe(completed -> {
                        mIsMovingSubject.onNext(false);
                        // start with the corner
                        mCurrentPoint = getPoint(mGrid, 1);
                        if (mCurrentPoint != null) {
                            gotoPointManual(mCurrentPoint.getCoordinateOrder());
                        }
                    }, e -> {
                        LogHelper.log(e);
                        mIsMovingSubject.onNext(false);
                    }));
        }
        mUpdatePointsSubject.onNext(true);
    }

    public void gotoPointManual(int order) {
        mIsMovingSubject.onNext(true);

        mDisposables.add(getModel().getSlaveComputer()
                .gotoCalibrationPoint(order)
                .subscribe(success -> {
                    Logger.d("Goto manual point %d", order);
                    mCalibratedPointCount++;
                    switchToPoint(order);
                    mUpdatePointsSubject.onNext(true);
                    mIsMovingSubject.onNext(false);
                }, e -> {
                    LogHelper.log(e);
                    mIsMovingSubject.onNext(false);
                }));
    }

    public void adjustCalibrationPoint(double offset) {
        mIsMovingSubject.onNext(true);

        mDisposables.add(getModel().getSlaveComputer()
                .moveCalibrationPoint(offset)
                .subscribe(success -> {
                    if (success) {
                        mIsAdjustZOffsetSubject.onNext(true);
                    }
                    mIsMovingSubject.onNext(false);
                }, e -> {
                    LogHelper.log(e);
                    mIsMovingSubject.onNext(false);
                }));
    }

    public Observable<Boolean> saveCalibration() {
        mIsMovingSubject.onNext(true);

        return getModel().getSlaveComputer()
                .saveCalibration()
                .doOnNext(success -> mIsMovingSubject.onNext(false))
                .doOnError(e -> mIsMovingSubject.onNext(false));
    }

    public Observable<Boolean> exitCalibration() {
        mIsMovingSubject.onNext(true);

        return getModel().getSlaveComputer()
                .exitCalibration()
                .doOnNext(success -> {
                    resetBehaviors();
                })
                .doOnError(e -> mIsMovingSubject.onNext(false));
    }

    public void resetBehaviors() {
        mIsMovingSubject.onNext(false);
        mAutoZOffsetSubject.onNext(false);
        mIsAdjustZOffsetSubject.onNext(false);
    }

    /**
     * Re-init fields and status ,clear disposables so we can start calibration again.
     *
     * @param fragmentSurvive If true, we need to re-init the fields, otherwise they'll be init in the
     *                        onCreate... method of the fragment.
     */
    public void clearCalibrationPointData(boolean fragmentSurvive) {
        if (fragmentSurvive) {
            mCalibratedPointCount = 0;
            mCurrentDir = 0;
            for (CalibrationPoint point : mPoints) {
                point.setActivated(false);
                point.setSelected(false);
            }
        }
        // Clear the disposables, otherwise we may get multiple results though we only need one.
        mDisposables.clear();
    }

    public ArrayList<CalibrationPoint> initPoints() {
        if (mCalibratedPointCount != 0) {
            mCalibratedPointCount = 0;
        }
        final int[][] orders = generateViewOrders();
        mPoints = new ArrayList<>();
        for (int i = 1; i <= mGrid; i++) {
            for (int j = 1; j <= mGrid; j++) {
                int coordinateOrder = j + (mGrid * (mGrid - i));
                mPoints.add(new CalibrationPoint(i, j, orders[i - 1][j - 1], coordinateOrder));
            }
        }

        return mPoints;
    }

    private int[][] generateViewOrders() {
        int[][] orders = new int[mGrid][mGrid];
        int order = 1;
        int direction = 0;

        // init first element
        int currentRow = mGrid - 1;
        int currentColumn = 0;
        orders[currentRow][currentColumn] = order;
        order++;

        while (order <= mGrid * mGrid) {
            int nextRow = currentRow + mDirection[direction][0];
            int nextColumn = currentColumn + mDirection[direction][1];

            if (nextRow > mGrid - 1 || nextRow < 0 || nextColumn > mGrid - 1 || nextColumn < 0 || orders[nextRow][nextColumn] != 0) {
                // change direction
                direction = (direction + 1) % 4;
            } else {
                orders[nextRow][nextColumn] = order;
                currentRow = nextRow;
                currentColumn = nextColumn;
                order++;
            }
        }

        return orders;
    }

    private CalibrationPoint getPoint(int coordinateOrder) {
        if (coordinateOrder > mPoints.size() || coordinateOrder < 1) return null;

        for (CalibrationPoint point : mPoints) {
            if (point.getCoordinateOrder() == coordinateOrder)
                return point;
        }

        return null;
    }

    private CalibrationPoint getPoint(int row, int column) {
        if (row > mGrid || row < 1 || column > mGrid || column < 1) return null;

        int position = column + mGrid * (row - 1);
        if (position > mPoints.size() || position < 1) {
            return null;
        }

        return mPoints.get(position - 1);
    }

    private CalibrationPoint getNextPoint() {
        if (isAllPointSelected() || mCalibratedPointCount >= mPoints.size()) {
            // all points are selected, return current point
            Logger.d("All points are selected.");
            return mCurrentPoint;
        }

        // Check point if surrounded
        if (isCurrentPointSurrounded()) {
            // return unselected point
            for (CalibrationPoint point : mPoints) {
                if (!point.isSelected() && !point.isActivated()) {
                    return point;
                }
            }
        }

        // Find next point according to current direction.
        int nextRow = mCurrentPoint.getRow() + mDirection[mCurrentDir][0];
        int nextColumn = mCurrentPoint.getColumn() + mDirection[mCurrentDir][1];

        CalibrationPoint nextPoint = getPoint(nextRow, nextColumn);

        if (nextPoint == null) {
            // change direction
            mCurrentDir = (mCurrentDir + 1) % 4;
            Logger.d("Out of bound, mCurrentDir is %d", mCurrentDir);
            return getNextPoint();
        } else if (nextPoint.isSelected()) {
            // change direction
            mCurrentDir = (mCurrentDir + 1) % 4;
            Logger.d("NextPoint is Selected, mCurrentDir is %d", mCurrentDir);
            return getNextPoint();
        }

        return nextPoint;
    }

    private boolean isCurrentPointSurrounded() {
        boolean isSurrounded = true;
        for (int i = 0; i < 4; i++) {
            CalibrationPoint nextPoint = getPoint(mCurrentPoint.getRow() + mDirection[i][0],
                    mCurrentPoint.getColumn() + mDirection[i][1]);
            if (nextPoint == null) continue;
            if (!nextPoint.isSelected()) {
                isSurrounded = false;
            }
        }

        return isSurrounded;
    }

    // manual selected
    private void switchToPoint(int coordinateOrder) {
        if (mCurrentPoint.getViewOrder() != 0) {
            mCurrentPoint.setSelected(true);
            mCurrentPoint.setActivated(false);
        }

        mCurrentPoint = getPoint(coordinateOrder);
        if (mCurrentPoint == null) return;
        mCurrentPoint.setActivated(true);
        mCurrentPoint.setSelected(true);
    }

    // auto
    private void moveToNextPoint() {
        mCurrentPoint.setSelected(true);
        mCurrentPoint.setActivated(false);

        mCurrentPoint = getNextPoint();
        mCurrentPoint.setActivated(true);
    }

    public boolean isAllPointSelected() {
        for (CalibrationPoint point : mPoints) {
            if (!point.isSelected()) {
                return false;
            }
        }
        return true;
    }

    public void checkNextPointManual() {
        if (mCurrentPoint.getCoordinateOrder() == 0) {
            gotoPointManual(1);
        } else {
            CalibrationPoint nextPoint = getNextPoint();
            gotoPointManual(nextPoint.getCoordinateOrder());
        }
    }

    /**
     * Retry calibration: clear data and start again.
     */
    public void retryCalibration() {
        clearCalibrationPointData(true);
        resetBehaviors();
        startCalibration();
    }

    public void dispose() {
        mDisposables.dispose();
    }
}