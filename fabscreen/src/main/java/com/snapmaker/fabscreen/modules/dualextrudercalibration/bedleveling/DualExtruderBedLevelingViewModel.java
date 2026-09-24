package com.snapmaker.fabscreen.modules.dualextrudercalibration.bedleveling;

import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

public class DualExtruderBedLevelingViewModel extends BaseViewModel {
    private static final int[] GRIDS = {6, 5, 4};
    private boolean mSelectAuto = true;
    private boolean mSelectHeat = true;
    // Indicates what grid will the leveling process run. n for n×n.
    private int mGridPos = 0;

    public void setIfSelectAuto(boolean b) {
        mSelectAuto = b;
    }

    public boolean getIfUserSelectAuto() {
        return mSelectAuto;
    }

    public void setIfSelectHeat(boolean b) {
        mSelectHeat = b;
    }

    public boolean getIfUserSelectHeat() {
        return mSelectHeat;
    }

    /**
     * Set what grid will the leveling run.
     */
    public void setSelectedGridPosition(int position) {
        mGridPos = position;
    }

    public int getSelectedGridPosition() {
        return mGridPos;
    }

    public int getSelectedGrid() {
        return GRIDS[mGridPos];
    }

    public int getLevelingTotalPoints() {
        return getSelectedGrid() * getSelectedGrid();
    }

    public Observable<Boolean> coolDownBed() {
        return getModel().getSlaveComputer().sendGcode("M140 S0").map(response -> true);
    }
}
