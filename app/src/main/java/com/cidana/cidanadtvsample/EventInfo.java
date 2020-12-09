package com.cidana.cidanadtvsample;

/**
 * Created by Wyvern on 2015/6/15.
 */
public class EventInfo {
    private int mChannelId;
    private String mName;
    private String mDescription;
    private String mStartTime;
    private String mEndTime;

    public EventInfo() {
        mChannelId = -1;
        mName = "Unknown";
        mDescription = "Unknown";
        mStartTime = "null";
        mEndTime = "null";
    }

    public EventInfo setChannelId(int channelId) {
        mChannelId = channelId;
        return this;
    }
    public int getChannelId() {
        return mChannelId;
    }

    public EventInfo setName(String name) {
        mName = name;
        return this;
    }
    public String getName() {
        return mName;
    }

    public EventInfo setDescription(String description) {
        mDescription = description;
        return this;
    }
    public String getDescription() {
        return mDescription;
    }

    public EventInfo setStartTime(String startTime) {
        mStartTime = startTime;
        return this;
    }
    public String getStartTime() {
        return mStartTime;
    }

    public EventInfo setEndTime(String endTime) {
        mEndTime = endTime;
        return this;
    }
    public String getEndTime() {
        return mEndTime;
    }

    public String toString() {
        return "["+mStartTime+" - "+mEndTime+"] - ["+mName+"] - ["+mDescription+"]";
    }

    public void setRating(String data) {
    }
}
