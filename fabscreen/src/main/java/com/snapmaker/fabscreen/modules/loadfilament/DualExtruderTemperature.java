package com.snapmaker.fabscreen.modules.loadfilament;

public class DualExtruderTemperature {
    private float mExtruder0Temperature;
    private float mExtruder0TargetTemperature;
    private float mExtruder1Temperature;
    private float mExtruder1TargetTemperature;

    public DualExtruderTemperature() {

    }

    public void setExtruder0Temp(float e0Temp) {
        mExtruder0Temperature = e0Temp;
    }

    public void setExtruder0TargetTemp(float e0TargetTemp) {
        mExtruder0TargetTemperature = e0TargetTemp;
    }

    public void setExtruder1Temperature(float e1Temp) {
        mExtruder1Temperature = e1Temp;
    }

    public void setExtruder1TargetTemperature(float e1TargetTemp) {
        mExtruder1TargetTemperature = e1TargetTemp;
    }

    public float getExtruder0Temperature() {
        return mExtruder0Temperature;
    }

    public float getExtruder0TargetTemperature() {
        return mExtruder0TargetTemperature;
    }

    public float getExtruder1Temperature() {
        return mExtruder1Temperature;
    }

    public float getExtruder1TargetTemperature() {
        return mExtruder1TargetTemperature;
    }

    @Override
    public String toString() {
        return "DualExtruderTemperature{" +
                "Extruder0Temperature=" + mExtruder0Temperature +
                ", Extruder0TargetTemperature=" + mExtruder0TargetTemperature +
                ", Extruder1Temperature=" + mExtruder1Temperature +
                ", Extruder1TargetTemperature=" + mExtruder1TargetTemperature +
                '}';
    }
}
