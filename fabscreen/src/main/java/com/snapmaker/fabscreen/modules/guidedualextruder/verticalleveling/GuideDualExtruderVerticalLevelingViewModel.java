package com.snapmaker.fabscreen.modules.guidedualextruder.verticalleveling;

import fabscreen.libraries.legacy.base.BaseViewModel;
import io.reactivex.Observable;

public class GuideDualExtruderVerticalLevelingViewModel extends BaseViewModel {
    private static final int[] GRIDS = {6, 5, 4};
    // Indicates what grid will the leveling process run. n for n×n.
    private int mGridPos = 0;

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
