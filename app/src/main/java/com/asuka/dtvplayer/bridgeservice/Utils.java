package com.asuka.dtvplayer.bridgeservice;


import java.util.HashMap;

public class Utils {
    /**
     * Model name list of head-unit to connect
     */
    public static final String MODEL_NONE = "none";
    public static final String MODEL_IEI_TREADMILL = "IEI_Treadmill";
    public static final String MODEL_PF_TREADMILL = "PF_Treadmill";


    /**
     *   Event broadcast
     */

    public static final String EVENT_GET_SW_VERSION = "com.asuka.dtvplayer.get_sw_version";
    public static final String EVENT_SET_VOLUME_MUTE = "com.asuka.dtvplayer.set_volume_mute";
    public static final String EVENT_SET_VOLUME = "com.asuka.dtvplayer.set_volume";
    public static final String EVENT_GET_VOLUME = "com.asuka.dtvplayer.get_volume";
    public static final String EVENT_KEY = "com.asuka.dtvplayer.key_event";
    public static final String EVENT_CHANNEL = "com.asuka.dtvplayer.channel_event";
    public static final String EVENT_CHANNEL_INFO = "com.asuka.dtvplayer.getchannel_event";
    public static final String EVENT_CHANNEL_LIST = "com.asuka.dtvplayer.getchannellist_event";
    public static final String EVENT_OSD_STRING = "com.asuka.dtvplayer.sendo_osd_string_event";

    public static final String MSG_SETVOLUME = "SetVolume";
    public static final String MSG_KEYCODE = "KeyCode";
    public static final String MSG_CHANNEL_NUM = "ChannelNumber";
    public static final String MSG_OSD_STRING = "OSDString";





}