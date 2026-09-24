package com.snapmaker.fabscreen.modules.dualextrudercalibration.entries;

import com.snapmaker.fabscreen.R;

import java.util.ArrayList;
import java.util.List;

import fabscreen.libraries.legacy.base.BaseViewModel;

public class CalibrationDispatcherViewModel extends BaseViewModel {

    private boolean mShowCaliZBadge;
    private boolean mShowCaliXYBadge;
    private List<EntryData> mEntries;

    public List<EntryData> getEntryList() {
        refreshBadge();
        if (mEntries != null) return mEntries;
        mEntries = new ArrayList<>();
        mEntries.add(new EntryData(R.drawable.ic_bed_leveling, R.string.calibration_heated_bed_leveling, EntryData.Entry.CALIBRATION_HEATED_BED));
        mEntries.add(new EntryData(R.drawable.ic_calibration_z, R.string.calibration_z_height, EntryData.Entry.CALIBRATION_Z_HEIGHT, mShowCaliZBadge));
        mEntries.add(new EntryData(R.drawable.ic_calibration_xy, R.string.calibration_xy_offset, EntryData.Entry.CALIBRATION_XY_OFFSET, mShowCaliXYBadge));
        mEntries.add(new EntryData(R.drawable.ic_calibration_check, R.string.calibration_model_check, EntryData.Entry.CALIBRATION_CHECK));
        mEntries.add(new EntryData(R.drawable.ic_calibration_sensor, R.string.calibration_sensor, EntryData.Entry.CALIBRATION_SENSOR));
        return mEntries;
    }

    public void refreshBadge() {
        mShowCaliZBadge = getModel().getPreferences().getNeedDoZHeightCalibration();
        mShowCaliXYBadge = getModel().getPreferences().getNeedDoXYOffsetCalibration();
        if (mEntries != null) {
            mEntries.get(1).showBadge = mShowCaliZBadge;
            mEntries.get(2).showBadge = mShowCaliXYBadge;
        }
    }
}
