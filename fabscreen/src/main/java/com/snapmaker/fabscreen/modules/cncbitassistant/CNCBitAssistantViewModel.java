package com.snapmaker.fabscreen.modules.cncbitassistant;

import fabscreen.libraries.legacy.base.BaseViewModel;

public class CNCBitAssistantViewModel extends BaseViewModel {
    private float mBitPositionX;
    private float mBitPositionY;
    private float mBitPositionZ;

    public void setBitPosition(float x, float y, float z) {
        mBitPositionX = x;
        mBitPositionY = y;
        mBitPositionZ = z;
    }

    public float getBitPositionX() {
        return mBitPositionX;
    }

    public float getBitPositionY() {
        return mBitPositionY;
    }

    public float getBitPositionZ() {
        return mBitPositionZ;
    }
}
