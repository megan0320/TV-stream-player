package com.asuka.dtvplayer;

import android.app.Application;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


import com.asuka.dtvplayer.bridgeservice.SimpleChannel;
import com.asuka.dtvplayer.bridgeservice.Utils;
import com.asuka.dtvplayer.bridgeservice.DtvBridgeService;

import com.cidana.cidanadtvsample.CiplContainer;
import com.cidana.cidanadtvsample.Channel;

import java.io.IOException;
import java.lang.Thread;
import java.util.ArrayList;
import java.util.Arrays;

import com.asuka.os.AskBuild;


public class DtvPlayerApp extends Application {

    static final String LOG_TAG = "DtvPlayerApp";


    /**
     * preference file name
     */
    static final String PREF_FILE = "DtvBridgePref";
    static final String PREF_DtvBridge_MODEL = "DtvBridge_model";

    /**
     * handler message
     */
    private static final int BONDED_MSG = 1;

    static final String DBFOLDER = "database"; // scanned channel database folder

    public static String DbFilePath = DBFOLDER;   // scanned channel database path

    private DtvBridgeService mBridgeServer;
    boolean mBound = false;
    private InstThread mInstThread;

    private CiplContainer mCiplContainer;
    private AudioManager mAudioManager;

    /**
     * Current connected device model
     */
    String mModel = Utils.MODEL_NONE;

    void loadPreference() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        mModel = sharedPreferences.getString(PREF_DtvBridge_MODEL, getResources().getString(R.string.cfg_DtvBridge_model));

        Log.i(LOG_TAG, "current DtvModel = " + mModel);

        if (mBound)
            mBridgeServer.startBridgeModel(mModel);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(LOG_TAG, "DtvPlayerApp on");

        DbFilePath = getFilesDir().getAbsolutePath() + "/" + DBFOLDER + "/";

        Intent intent = new Intent(this, DtvBridgeService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mCiplContainer = CiplContainer.getInstance(this);

        IntentFilter intentFilterEvent = new IntentFilter();

        intentFilterEvent.addAction(Utils.EVENT_GET_SW_VERSION);
        intentFilterEvent.addAction(Utils.EVENT_SET_VOLUME_MUTE);
        intentFilterEvent.addAction(Utils.EVENT_SET_VOLUME);
        intentFilterEvent.addAction(Utils.EVENT_GET_VOLUME);
        intentFilterEvent.addAction(Utils.EVENT_KEY);
        intentFilterEvent.addAction(Utils.EVENT_CHANNEL);
        intentFilterEvent.addAction(Utils.EVENT_CHANNEL_INFO);
        intentFilterEvent.addAction(Utils.EVENT_CHANNEL_LIST);
        registerReceiver(broadcastReceiver, intentFilterEvent);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case BONDED_MSG:
                    Log.d(LOG_TAG, "BONDED_MSG");
                    loadPreference();
                    mInstThread = new InstThread();
                    mInstThread.start();
                    break;
            }
        }
    };

    class InstThread extends Thread {
        Object mutex = new Object();
        int message;

        public InstThread() {

        }

        public void setMessage(int msg) {
            synchronized (mutex) {
                message = msg;
                mutex.notify();
            }
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            DtvBridgeService.LocalBinder binder = (DtvBridgeService.LocalBinder) service;
            mBridgeServer = binder.getService();
            mBound = true;

            Message msg = mHandler.obtainMessage(BONDED_MSG);
            mHandler.sendMessageDelayed(msg, 2000);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
//String.format("%s", AskBuild.DISPLAY)
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.i(LOG_TAG, "Receive intent = " + intent);
            if (intent.getAction() == null)
                return;

            switch (intent.getAction()) {

                case Utils.EVENT_GET_SW_VERSION: {
                    String version = AskBuild.DISPLAY;
                    Log.i(LOG_TAG, " Version = " + String.format("%s", version));
                    String [] splitVersion = version.split(" ");
                    String [] softwareVersion = splitVersion[2] .split("_");
                    Log.i(LOG_TAG, " Version = " + String.format("%s", softwareVersion[0]));
                    Object data = softwareVersion[0];
                    mBridgeServer.sendEventData(intent.getAction(),data);
                }break;
                case Utils.EVENT_GET_VOLUME: {
                    int volNow = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    Log.i(LOG_TAG, " Volume = " + volNow);
                    Object data = volNow;
                    mBridgeServer.sendEventData(intent.getAction(),data);
                }break;
                case Utils.EVENT_SET_VOLUME: {
                    int vol = intent.getIntExtra(Utils.MSG_SETVOLUME, 0);
                    Log.i(LOG_TAG, " Volume = " + vol);
                    setVolume(vol);
                }break;
                case Utils.EVENT_SET_VOLUME_MUTE: {
//                    int volNow = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    if(mAudioManager.isStreamMute(AudioManager.STREAM_MUSIC)){
                        mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI);
                    }
                    else {
                        mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI);
                    }
                }break;
                case Utils.EVENT_KEY: {
                    int keyEventCode = intent.getIntExtra(Utils.MSG_KEYCODE, 0);
                    Log.i(LOG_TAG, " Get KeyCode = " + keyEventCode);
                    sendKeyEvent(keyEventCode);
                }
                break;

                case Utils.EVENT_CHANNEL: {
                    String channel = intent.getStringExtra(Utils.MSG_CHANNEL_NUM);
                    Log.i(LOG_TAG, "switch to channel " + channel);
                }
                break;

                case Utils.EVENT_CHANNEL_INFO: {
                    Channel channel = mCiplContainer.getCurrentChannel();
                    if(channel == null){
                    }
                    else {
                        SimpleChannel sChannel = new SimpleChannel(Float.parseFloat(channel.getLcn()), channel.getName(), 0);
                        Object data = sChannel;
                        mBridgeServer.sendEventData(intent.getAction(), data);
                    }
                }
                break;

                case Utils.EVENT_CHANNEL_LIST: {
                    ArrayList<Channel> list = mCiplContainer.getChannelList();
                    ArrayList<SimpleChannel> sList = new ArrayList<>();
                    int i;

                    sList.clear();
                    for (i = 0; i < list.size(); i++) {
                        SimpleChannel sChannel = new SimpleChannel(Float.parseFloat(list.get(i).getLcn()), list.get(i).getName(), 0);
                        sList.add(sChannel);
                    }
                    Object data = sList;
                    mBridgeServer.sendEventData(intent.getAction(), data);
                }
                break;

                default:
                    Log.i(LOG_TAG, "unknown event action " + intent.getAction());
                    break;
            }
        }
    };

    private void sendKeyEvent(final int KeyCode) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Instrumentation inst = new Instrumentation();
                    inst.sendKeyDownUpSync(KeyCode);
                    Log.i(LOG_TAG, " Send KeyCode = " + KeyCode);
                } catch (Exception e) {
                    e.printStackTrace();
                }
           }
         }).start();
    }

    private void setVolume(int vol) {
        int volMax = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if(vol < 0)
            vol = 0;
        else if(vol > volMax)
            vol = volMax;
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,vol,AudioManager.FLAG_SHOW_UI);
        Log.i(LOG_TAG, " Set success = " + vol);
    }
}