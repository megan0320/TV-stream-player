package com.asuka.dtvplayer.bridgeservice;


/**
 * class of channel information object
 */
public class SimpleChannel {
    private Float mNum;    // channel number
    private String mName;   // channel name
    private int mFrequency; // channel frequency

    /**
     * create a channel with param
     */
    public SimpleChannel(Float num, String name, int freq) {
        mNum = num;
        mName = name;
        mFrequency = freq;
    }

    /**
     * get name of channel
     * @return string of channel name
     */
    public String getName() {
        return mName;
    }

    /**
     * get frequency of channel
     * @return string of frequency information
     */
    public int getFrequency() {
        return mFrequency;
    }

    /**
     * get channel number
     * @return string of channel LCN
     */
    public Float getNum() {
        return mNum;
    }
}
