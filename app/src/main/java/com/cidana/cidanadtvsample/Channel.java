package com.cidana.cidanadtvsample;

/**
 * @author Wyvern
 * @since 2015/6/3
 */

/**
 * class of channel information object
 */
public class Channel {
    private int mChId;
    private String mName;
    private String mConstInfo;
    private String mFrequency;
    private String mLcn;

    /**
     * create a channel with ID
     * @param chId channel ID
     */
    public Channel(int chId) {
        mChId = chId;
        mName = "unknown";
        mConstInfo = "unknown";
        mFrequency = "null";
        mLcn = "unknown";
    }

    /**
     * get channel ID
     * @return number of channel ID
     */
    public int getChId() {
        return mChId;
    }

    /**
     * get name of channel
     * @return string of channel name
     */
    public String getName() {
        return mName;
    }

    /**
     * set name of channel
     * @param name name of channel in string
     * @return the channel info object
     */
    public Channel setName(String name) {
        if (name != null) {
            mName = name;
        }
        return this;
    }

    /**
     * get constant information of channel
     * @return string of constant information
     */
    public String getConstInfo() {
        return mConstInfo;
    }

    /**
     * set constant information
     * @param constInfo constant information in string
     * @return the channel info object
     */
    public Channel setConstInfo(String constInfo) {
        if (constInfo != null) {
            mConstInfo = constInfo;
            String[] strAry = constInfo.split("/");
            if (strAry.length > 2) {
                mFrequency = strAry[2];
            }
        }
        return this;
    }

    /**
     * get frequency of channel
     * @return string of frequency information
     */
    public String getFrequency() {
        return mFrequency;
    }

    /**
     * get LCN of channel
     * @return string of channel LCN
     */
    public String getLcn() {
        return mLcn;
    }

    /**
     * set channel LCN
     * @param lcn LCN in string
     * @return the channel info object
     */
    public Channel setLcn(String lcn) {
        if (lcn != null) {
            mLcn = lcn;
        }
        return this;
    }

    /**
     * transfer channel information in string
     * @return string of channel information
     */
    public String toString() {
        return mChId+"-"+mName+" ("+mLcn+")";
    }
}
