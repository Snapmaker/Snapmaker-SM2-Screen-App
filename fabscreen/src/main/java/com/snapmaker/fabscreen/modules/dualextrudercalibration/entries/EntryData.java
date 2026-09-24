package com.snapmaker.fabscreen.modules.dualextrudercalibration.entries;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

public class EntryData {
    @DrawableRes
    public int icon;
    @StringRes
    public int title;
    public boolean showBadge;
    public Entry entry;

    public EntryData(@DrawableRes int icon, @StringRes int title, Entry entry) {
        this.icon = icon;
        this.title = title;
        this.entry = entry;
        this.showBadge = false;
    }

    public EntryData(@DrawableRes int icon, @StringRes int title, Entry entry, boolean showBadge) {
        this.icon = icon;
        this.title = title;
        this.entry = entry;
        this.showBadge = showBadge;
    }

    enum Entry {
        CALIBRATION_HEATED_BED,
        CALIBRATION_Z_HEIGHT,
        CALIBRATION_XY_OFFSET,
        CALIBRATION_SENSOR,
        CALIBRATION_CHECK
    }
}
