package com.snapmaker.fabscreen.modules.cncoriginassistant;

public class CNCOriginAssistantBitItem {
    public final static int TYPE_ITEM_BIT_DEFAULT = 0;
    public final static int TYPE_ITEM_BIT_CUSTOM = 1;
    private int itemType;
    private String bitName;
    private int bitResId;
    private float bitDiameter;
    private float bitLength;

    public CNCOriginAssistantBitItem(int type, String name, int resid, float diameter, float length) {
        itemType = type;
        bitName = name;
        bitResId = resid;
        bitDiameter = diameter;
        bitLength = length;
    }

    public int getItemType() {
        return itemType;
    }

    public void setItemType(int type) {
        itemType = type;
    }

    public boolean isDefaultBit() {
        return itemType == TYPE_ITEM_BIT_DEFAULT;
    }

    public String getBitName() {
        return bitName;
    }

    public void setBitName(String bitName) {
        this.bitName = bitName;
    }

    public int getBitResId() {
        return bitResId;
    }

    public void setBitResId(int bitResId) {
        this.bitResId = bitResId;
    }

    public float getBitDiameter() {
        return bitDiameter;
    }

    public void setBitDiameter(float bitDiameter) {
        this.bitDiameter = bitDiameter;
    }

    public float getBitLength() {
        return bitLength;
    }

    public void setBitLength(float bitLength) {
        this.bitLength = bitLength;
    }
}
