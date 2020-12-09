package com.asuka.dtvplayer;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class DtvControl {
    /**
     * SharedPreference of settings of DtvPlayer
     */
   

    public static final String PREF_DTV_FILE = "dtv_file";
    public static final String KEY_PREF_TUNER_STD = "dtv_tuner_standard";
    public static final String KEY_PREF_CHANNEL_IDX = "dtv_channel_index";
    public static final String KEY_PREF_CHANNEL_NAME = "dtv_channel_name";

    public static final String KEY_PREF_SAVE_SUB_IDX = "dtv_saved_subtitle_index";
    public static final String KEY_PREF_SAVE_AUD_IDX = "dtv_saved_audio_index";

    public static final String AUD_SETTING = "audio_settings";

    // event broadcasting use
    public static final String EVENT_TUNER_READY = "com.asuka.dtvplayer.tuner_ready";
    public static final String EVENT_TUNER_DISCONNECTED = "com.asuka.dtvplayer.tuner_disconnected";


    public static SimpleDateFormat DTV_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    static { DTV_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC")); }

    /**
     * Converter classes/variables of Cidana Dtv SDK with Asuka DtvPlayer app
     */
    // Tuner standard
    public static final int TUNER_ATSC = 11;
    public static final int TUNER_J83B = 12;

    // Event list of Cidana DTV SDK
    /**
     * Initial event with result
     * int result = event.getBlob(1).getItem(0, 1); // result of initialized, 0: success; otherwise: failed
     * */
    public static final String EVENT_DTV_INITIAL_END = "CIPL_TVC_InitialEnd";

    /**
     * Scanning progress report event
     * int progress = event.getBlob(1).getItem(0, 0);   // current scanning progress
     * int channel = event.getBlob(1).getItem(0, 1);    // current scanning channel number
     */
    public static final String EVENT_DTV_SCAN_PROGRESS = "CIPL_TVC_ScanProgress";
    public static final String EVENT_DTV_SCAN_END = "CIPL_TVC_ScanEnd";
    public static final String EVENT_DTV_SCAN_CANCEL = "CIPL_TVC_ScanCanceled";

    public static final String EVENT_DTV_FIRST_DATA = "CIPL_MPL_FirstData";
    public static final String EVENT_DTV_CH_LOCK_UPDATE = "CIPL_TVC_LockUpdating";
    public static final String EVENT_DTV_CH_UNLOCK_UPDATE = "CIPL_TVC_UnlockUpdated";
    public static final String EVENT_DTV_PROGRAM_UPDATE = "CIPL_PGD_ProgramUpdate";
    public static final String EVENT_DTV_SERVICE_UPDATE = "CIPL_PGD_ServiceInfoUpdated";
    public static final String EVENT_DTV_SERVICE_REMOVE = "CIPL_PGD_ServiceRemove";
    public static final String EVENT_DTV_SERVICE_ADD = "CIPL_PGD_ServiceAdd";
    public static final String EVENT_DTV_SERVICE_CHANGING = "CIPL_TVC_ServiceChanging";

    public static final String EVENT_DTV_UOP_CHANGED = "CIPL_MPL_UOPChanged";
    public static final String EVENT_DTV_SPEED_CHANGED = "CIPL_MPL_SpeedChanged";
    public static final String EVENT_DTV_STATE_CHANGED = "CIPL_MPL_StateChanged";
    /**
     * Signal status changing event
     * int signalStatus = event.getBlob(1).getItem(0, 1);   // 2: lost signal; otherwise : locked
     * int signalStrength = event.getBlob(1).getItem(0, 2);
     */
    public static final String EVENT_DTV_SIGNAL_CHANGED = "CIPL_TVC_SignalChanged";
    public static final String EVENT_DTV_SIGNAL_REPORT = "CIPL_TVC_SignalReport";

    public static final String EVENT_DTV_HEART_BEAT = "CIPL_GUI_HeartBeat";
    public static final String EVENT_DTV_PROGRAM_FTA = "CIPL_PGD_ProgramFreeToAir";
    public static final String EVENT_DTV_PARENTAL_PASS = "CIPL_TVC_ParentalPass";

    /**
     * Current time info update event
     * String time = event.getBlob(2).getItem(0, 0);
     */
    public static final String EVENT_DTV_TIME_UPDATE = "CIPL_TVC_StreamTimeGet";
    public static final String EVENT_DTV_AUDIO_VOL = "CIPL_MPL_AudioVolume";
    public static final String EVENT_DTV_EQ_CHANGED = "CIPL_MPL_EQStatusChanged";
    public static final String EVENT_DTV_SUBTITLE_CHANGED = "CIPL_TVC_SubtitleChanged";
    public static final String EVENT_CIPL_MPL_FIRSTVIDEOFRAME = "CIPL_MPL_FirstVideoFrame";
    /**
     * Video resolution change event
     * int width = event.getBlob(1).getItem(0, 1);
     * int height = event.getBlob(1).getItem(0, 2);
     */
    public static final String EVENT_DTV_VIDEO_CHANGE = "CIPL_MPL_VideoSrcChanged";

    /**
     * Recording file new created event
     * String filePath = event.getBlob(2).getItem(0, 0);    // file saved path
     */
    public static final String EVENT_DTV_FILE_SAVE_CREATE = "CIPL_MRE_CreateNewFile";
    /**
     * Recording file saved end event
     */
    public static final String EVENT_DTV_FILE_SAVE_END = "CIPL_MRE_FileSaveEnd";

    /**
     * URL broadcast event ???
     * String url = event.getBlob(2).getItem(0, 0);
     */
    public static final String EVENT_DTV_BROADCAST_URL = "CIPL_TVC_BroadcastUrl";
    public static final String EVENT_ENTER_SCAN_REQUEST = "CIPL_ENTER_SCAN_REQUEST";
}