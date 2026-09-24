package fabscreen.libraries.legacy.data.print;

import com.orhanobut.logger.Logger;

import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.lib.StringHelper;
import io.reactivex.Observable;

public class OnAirGcodeModifier {
    // overrides
    private float mOverrideFeedRate = -1;
    private boolean mOverrideFeedRateDirty = false;

    private float mOverrideLeftZOffset = 0;
    private float mOverrideRightZOffset = 0;
    private boolean mOverrideLeftZOffsetDirty = false;
    private boolean mOverrideRightZOffsetDirty = false;

    private float mOverrideLeftFlowRate = 0;
    private float mOverrideRightFlowRate = 0;
    private boolean mOverrideLeftFlowRateDirty = false;
    private boolean mOverrideRightFlowRateDirty = false;

    private float mOverrideNozzleTemperature = -1;
    private boolean mOverrideNozzleTemperatureDirty = false;
    private float mOverrideRightNozzleTemperature = -1;
    private boolean mOverrideRightNozzleTemperatureDirty = false;


    private float mOverrideInitialNozzleTemperature = -1;
    private boolean mInitialM104Marker = false;
    private boolean mInitialM109Marker = false;

    private float mOverrideInitialRightNozzleTemperature = -1;

    private float mOverrideHeatedBedTemperature = -1;
    private boolean mOverrideHeatedBedTemperatureDirty = false;

    private float mOverrideInitialHeatedBedTemperature = -1;
    private boolean mInitialM140Marker = false;
    private boolean mInitialM190Marker = false;

    private float mOverrideLaserPower = -1;
    private boolean mOverrideLaserPowerDirty = false;

    OnAirGcodeModifier() {

    }

    void reset() {
        Logger.d("Reset overrides.");

        mOverrideFeedRate = 100;
        mOverrideFeedRateDirty = false;

        mOverrideLeftZOffset = 0;
        mOverrideRightZOffset = 0;
        mOverrideLeftZOffsetDirty = false;
        mOverrideRightZOffsetDirty = false;

        mOverrideLeftFlowRate = 0;
        mOverrideRightFlowRate = 0;
        mOverrideLeftFlowRateDirty = false;
        mOverrideRightFlowRateDirty = false;

        mOverrideNozzleTemperature = -1;
        mOverrideNozzleTemperatureDirty = false;

        mOverrideInitialNozzleTemperature = -1;
        mOverrideInitialRightNozzleTemperature = -1;
        mInitialM104Marker = false;
        mInitialM109Marker = false;
        mOverrideRightNozzleTemperature = -1;
        mOverrideRightNozzleTemperatureDirty = false;

        mOverrideHeatedBedTemperature = -1;
        mOverrideHeatedBedTemperatureDirty = false;

        mOverrideInitialHeatedBedTemperature = -1;
        mInitialM140Marker = false;
        mInitialM190Marker = false;

        mOverrideLaserPower = -1;
        mOverrideLaserPowerDirty = false;
    }

    public float getOverrideFeedRate() {
        return mOverrideFeedRate;
    }

    public void setOverrideFeedRate(float feedRate) {
        if (feedRate != mOverrideFeedRate) {
            Logger.i("Override feed rate " + feedRate);
            mOverrideFeedRate = feedRate;
            mOverrideFeedRateDirty = true;
        }
    }

    public float getOverrideZOffset() {
        return mOverrideLeftZOffset;
    }

    public float getOverrideZOffset(int which) {
        return which == 0 ? mOverrideLeftZOffset : mOverrideRightZOffset;
    }

    /**
     * Z offset will be applied to all Z coordinates in absolute positioning.
     */

    public void setOverrideZOffset(float zOffset) {
        if (mOverrideLeftZOffset != zOffset) {
            Logger.i("Override z offset " + zOffset);
            mOverrideLeftZOffset = zOffset;
            mOverrideLeftZOffsetDirty = true;
        }

    }

    public void setOverrideZOffset(int which, float zOffset) {
        if (which == 0) {
            if (mOverrideLeftZOffset != zOffset) {
                Logger.i("Override left z offset " + zOffset);
                mOverrideLeftZOffset = zOffset;
                mOverrideLeftZOffsetDirty = true;
            }
        } else {
            if (mOverrideRightZOffset != zOffset) {
                Logger.i("Override right z offset " + zOffset);
                mOverrideRightZOffset = zOffset;
                mOverrideRightZOffsetDirty = true;
            }
        }
    }

    public float getOverrideNozzleTemperature() {
        return mOverrideNozzleTemperature;
    }

    public float getOverrideNozzleTemperature(int which) {
        return which == 0 ? mOverrideNozzleTemperature : mOverrideRightNozzleTemperature;
    }

    public boolean getOverrideNozzleTemperatureDirty() {
        return mOverrideNozzleTemperatureDirty;
    }

    public void setOverrideNozzleTemperature(float temp) {
        if (temp != mOverrideNozzleTemperature) {
            Logger.i("Override nozzle temperature " + temp);
            mOverrideNozzleTemperature = temp;
            mOverrideNozzleTemperatureDirty = true;
        }
    }

    public void setOverrideNozzleTemperature(int which, float temp) {
        if (which == 0) {
            if (temp != mOverrideNozzleTemperature) {
                Logger.i("Override left nozzle temperature " + temp);
                mOverrideNozzleTemperature = temp;
                mOverrideNozzleTemperatureDirty = true;
            }
        } else {
            if (temp != mOverrideRightNozzleTemperature) {
                Logger.i("Override right nozzle temperature " + temp);
                mOverrideRightNozzleTemperature = temp;
                mOverrideRightNozzleTemperatureDirty = true;
            }
        }

    }

    public float getOverrideInitialNozzleTemperature() {
        return mOverrideInitialNozzleTemperature;
    }

    public float getOverrideInitialNozzleTemperature(int which) {
        return which == 0 ? mOverrideInitialNozzleTemperature : mOverrideInitialRightNozzleTemperature;
    }

    public void setOverrideInitialNozzleTemperature(float temp) {
        if (temp != mOverrideInitialNozzleTemperature) {
            Logger.i("Set initial nozzle temperature " + temp);
            mOverrideInitialNozzleTemperature = temp;
        }
    }

    public void setOverrideInitialNozzleTemperature(int which, float temp) {
        if (temp != mOverrideInitialNozzleTemperature && which == 0) {
            Logger.i("Set initial nozzle temperature " + temp);
            mOverrideInitialNozzleTemperature = temp;
        }

        if (temp != mOverrideInitialRightNozzleTemperature && which == 1) {
            Logger.i("Set initial right nozzle temperature " + temp);
            mOverrideInitialRightNozzleTemperature = temp;
        }
    }

    public boolean getInitialM109Marker() {
        return mInitialM109Marker;
    }

    public float getOverrideHeatedBedTemperature() {
        return mOverrideHeatedBedTemperature;
    }

    public boolean getOverrideHeatedBedTemperatureDirty() {
        return mOverrideHeatedBedTemperatureDirty;
    }

    public boolean getInitialM190Marker() {
        return mInitialM190Marker;
    }

    public void setOverrideHeatedBedTemperature(float temp) {
        if (temp != mOverrideHeatedBedTemperature) {
            Logger.i("Override heated bed temperature " + temp);
            mOverrideHeatedBedTemperature = temp;
            mOverrideHeatedBedTemperatureDirty = true;
        }
    }

    public float getOverrideInitialHeatedBedTemperature() {
        return mOverrideInitialHeatedBedTemperature;
    }

    public void setOverrideInitialHeatedBedTemperature(float temp) {
        if (temp != mOverrideInitialHeatedBedTemperature) {
            Logger.i("Set initial heated bed temperature " + temp);
            mOverrideInitialHeatedBedTemperature = temp;
        }
    }

    public float getOverrideLaserPower() {
        return mOverrideLaserPower;
    }

    public boolean getOverrideLaserPowerDirty() {
        return mOverrideLaserPowerDirty;
    }

    public void setOverrideLaserPower(float power) {
        if (power != mOverrideLaserPower) {
            Logger.i("Override laser power " + power);
            mOverrideLaserPower = power;
            mOverrideLaserPowerDirty = true;
        }
    }

    public void setOverrideLeftFlowRate(float flowRate) {
        if (flowRate != mOverrideLeftFlowRate) {
            Logger.i("Override left Flow Rate " + flowRate);
            mOverrideLeftFlowRate = flowRate;
            mOverrideLeftFlowRateDirty = true;
        }
    }

    public void setOverrideRightFlowRate(float flowRate) {
        if (flowRate != mOverrideRightFlowRate) {
            Logger.i("Override left Flow Rate " + flowRate);
            mOverrideRightFlowRate = flowRate;
            mOverrideRightFlowRateDirty = true;
        }
    }

    public float getOverrideLeftFlowRate() {
        return mOverrideLeftFlowRate;
    }

    public float getOverrideRightFlowRate() {
        return mOverrideRightFlowRate;
    }

    Observable<Integer> getModifyAction(ISlaveComputer slaveComputer) {
        if (mOverrideFeedRateDirty) {
            mOverrideFeedRateDirty = false;
            Logger.d("Setting feed rate to " + mOverrideFeedRate);
            return slaveComputer.requestAdjustSettingFeedRate(mOverrideFeedRate);
        }
        if (mOverrideLeftZOffsetDirty) {
            float zOffset = mOverrideLeftZOffset;
            mOverrideLeftZOffsetDirty = false;
            Logger.d("Adjust (l) z offset " + zOffset);
            return slaveComputer.requestAdjustSettingZOffset(0, zOffset);
        }
        if (mOverrideRightZOffsetDirty) {
            float zOffset = mOverrideRightZOffset;
            mOverrideRightZOffsetDirty = false;
            Logger.d("Adjust right z offset " + zOffset);
            return slaveComputer.requestAdjustSettingZOffset(1, zOffset);
        }
        if (mOverrideLeftFlowRateDirty) {
            float flowRate = mOverrideLeftFlowRate;
            mOverrideLeftFlowRateDirty = false;
            Logger.d("Adjust left flow rate " + flowRate);
            return slaveComputer.requestAdjustSettingFlowRate(0, flowRate);
        }
        if (mOverrideRightFlowRateDirty) {
            float flowRate = mOverrideRightFlowRate;
            mOverrideRightFlowRateDirty = false;
            Logger.d("Adjust right flow rate " + flowRate);
            return slaveComputer.requestAdjustSettingFlowRate(1, flowRate);
        }
        if (mOverrideNozzleTemperatureDirty) {
            mOverrideNozzleTemperatureDirty = false;
            Logger.d("Setting nozzle temperature to " + mOverrideNozzleTemperature);
            return slaveComputer.requestAdjustSettingNozzleTemp(mOverrideNozzleTemperature);
        }
        if (mOverrideRightNozzleTemperatureDirty) {
            mOverrideRightNozzleTemperatureDirty = false;
            Logger.d("Setting right nozzle temperature to " + mOverrideRightNozzleTemperature);
            return slaveComputer.requestAdjustSettingNozzleTemp(1, mOverrideRightNozzleTemperature);
        }
        if (mOverrideHeatedBedTemperatureDirty) {
            mOverrideHeatedBedTemperatureDirty = false;
            Logger.d("Setting heated bed temperature to " + mOverrideHeatedBedTemperature);
            return slaveComputer.requestAdjustSettingHeatedBedTemp(mOverrideHeatedBedTemperature);
        }
        if (mOverrideLaserPowerDirty) {
            mOverrideLaserPowerDirty = false;
            Logger.d("Setting laser power to " + mOverrideLaserPower);
            return slaveComputer.requestAdjustSettingLaserPower(mOverrideLaserPower);
        }

        return null;
    }

    String override(String line) {
        if (line.isEmpty()) {
            return line;
        }

        // Straight comment
        if (line.charAt(0) == ';') {
            return "";
        }
        return newOverride(line);
    }

    public boolean IsAllInitialCodeOverride() {
        return mInitialM104Marker && mInitialM109Marker && mInitialM140Marker && mInitialM190Marker;
    }

    public String newOverride(String line) {
        // Parse line to separate arguments
        final int length = line.length();

        int lineArgCount = 0;
        String[] lineArgs = new String[20];

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
        }

        if (lineArgCount == 0) return "";

        switch (lineArgs[0]) {
            case "M104": {
                if (!mInitialM104Marker) {
                    mInitialM104Marker = true;
                    if (mOverrideInitialNozzleTemperature != -1) {
                        Logger.d("Override M104 S" + mOverrideInitialNozzleTemperature);
                        return lineArgs[0] + " S" + mOverrideInitialNozzleTemperature;
                    }
                }
                break;
            }
            case "M109": {
                if (!mInitialM109Marker) {
                    mInitialM109Marker = true;
                    if (mOverrideInitialNozzleTemperature != -1) {
                        Logger.d("Override M109 S" + mOverrideInitialNozzleTemperature);
                        return lineArgs[0] + " S" + mOverrideInitialNozzleTemperature;
                    }
                }
                break;
            }
            case "M140": {
                if (!mInitialM140Marker) {
                    mInitialM140Marker = true;
                    if (mOverrideInitialHeatedBedTemperature != -1) {
                        Logger.d("Override M140 S" + mOverrideInitialHeatedBedTemperature);
                        return lineArgs[0] + " S" + mOverrideInitialHeatedBedTemperature;
                    }
                }
                break;
            }
            case "M190": {
                if (!mInitialM190Marker) {
                    mInitialM190Marker = true;
                    if (mOverrideInitialHeatedBedTemperature != -1) {
                        Logger.d("Override M190 S" + mOverrideInitialHeatedBedTemperature);
                        return lineArgs[0] + " S" + mOverrideInitialHeatedBedTemperature;
                    }
                }
                break;
            }
        }

        // unmodified
        return line;
    }
}
