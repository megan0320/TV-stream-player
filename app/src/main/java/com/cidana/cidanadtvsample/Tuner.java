package com.cidana.cidanadtvsample;

/**
 * Created by Wyvern on 2015/5/21.
 */
public class Tuner {
    private String mName;
    private int mDeviceIndex;
    private String mDeviceType;
    private boolean mIsActivated;

    public Tuner() {
        mName = "Unknown tuner";
        mDeviceIndex = -1;
        mDeviceType = "Unknown";
        mIsActivated = false;
    }

    public Tuner setName(String name) {
        mName = name;
        return this;
    }
    public String getName() {
        return mName;
    }

    public Tuner setDeviceIndex(int index) {
        mDeviceIndex = index;
        return this;
    }
    public int getDeviceIndex() {
        return mDeviceIndex;
    }

    public Tuner setDeviceType(String deviceType) {
        mDeviceType = deviceType;
        return this;
    }
    public String getDeviceType() {
        return mDeviceType;
    }

    public void activate() {
        mIsActivated = true;
    }
    public void deactivate() {
        mIsActivated = false;
    }
    public boolean isActivated() {
        return mIsActivated;
    }

    public boolean equals(Tuner tuner2) {
        if (tuner2 == null) {
            return false;
        }
        else {
            return tuner2.getDeviceIndex() == mDeviceIndex;
        }
    }

    public String toString() {
        return mDeviceIndex+"-"+mName+"-"+mDeviceType;
    }
}
