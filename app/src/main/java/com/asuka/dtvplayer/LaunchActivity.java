package com.asuka.dtvplayer;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.AsyncTask;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.widget.ImageView;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation;
import android.widget.Toast;

import android.util.Log;

import java.util.ArrayList;

import com.cidana.licenseserverlibrary.VerificationManager;
import com.cidana.cidanadtvsample.CiplContainer;
import com.cidana.cidanadtvsample.Tuner;
import com.cidana.cidanadtvsample.CiplEvent;

public class LaunchActivity extends Activity
        implements CiplContainer.ciplEventListener {
    private static final String LOG_TAG = "LaunchActivity";
    private static final String PREF_FILE = DtvControl.PREF_DTV_FILE;

    private CiplContainer mCiplContainer;
    private SharedPreferences mPref;

    private int mTunerIdx;
    private ArrayList<Tuner> mTunerList;
    Animation mAnimation;

    // not recommended use looper in activity, should use another way in next version
    private static class LooperThread extends Thread {
        public Handler mHandler;
        public void run() {
            Looper.prepare();
            mHandler = new Handler();
            Looper.loop();
        }
    }
    static private LooperThread mActivateTunerThread;
    static {
        mActivateTunerThread = new LooperThread();
        mActivateTunerThread.start();
    }

    private class TaskUpdateTunerList extends AsyncTask<CiplContainer, Void, Boolean> {
        @Override
        protected Boolean doInBackground(CiplContainer... params) {
            if (params.length == 0) {
                Log.e(LOG_TAG, "TaskUpdateTunerList: param length is 0, abort task.");
                return false;
            }
            if (params[0].updateTunerList() < 0)
                return false;
            return true;
        }
        protected void onPreExecute() {
            Log.i(LOG_TAG, "Update tuner list task begins...");
        }
        protected void onPostExecute(Boolean success) {
            if (success) {
                mTunerList = mCiplContainer.getTunerList();

                if (mCiplContainer.getCurrentTuner() == null) {
                    mActivateTunerThread.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            int ret = mCiplContainer.activateTuner(mTunerIdx);
                            if (ret < 0) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.e(LOG_TAG, "[onPostExecute] Failed activate tuner");
                                        Toast.makeText(LaunchActivity.this, "Failed to activate tuner!", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    });
                }
            }
        }
    }
    private TaskUpdateTunerList mTaskUpdateTunerList = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(LOG_TAG, "onCreate");

        setContentView(R.layout.loading);

        mCiplContainer = CiplContainer.getInstance(this);
        if (mCiplContainer != null) {
            mCiplContainer.addEventListener(this);
        }

        mPref = getSharedPreferences(PREF_FILE, MODE_PRIVATE);

        mAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_anim);

        VerificationManager.init(this, new VerificationManager.VerifyListener() {
            @Override
            public void onVerifySuccess(String regCode) {
                Log.d(LOG_TAG, "[onVerifySuccess] " + regCode);
            }

            @Override
            public void onVerifyFailure(int errorCode, String errorDesc) {
                Log.e(LOG_TAG, "[onVerifyFailure] " + errorDesc);
                LaunchActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(LaunchActivity.this, "License Authorization Failed !", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        // show error message
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String error = extras.getString("error");
            if(error != null)
            {
                Toast.makeText(LaunchActivity.this, error, Toast.LENGTH_LONG).show();
            }
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DtvControl.EVENT_TUNER_READY);
        intentFilter.addAction(DtvControl.EVENT_TUNER_DISCONNECTED);
        this.registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "onDestory");
        if(mAnimation != null)
            mAnimation.cancel();

        //if(mActivateTunerThread != null)
        //    mActivateTunerThread.stop();

        if (mCiplContainer != null)
            mCiplContainer.removeEventListener(this);

        super.onDestroy();
    }

    @Override
    public void onResume() {
        Log.i(LOG_TAG, "onResume");
        mTunerIdx = mPref.getInt(DtvControl.KEY_PREF_TUNER_STD, DtvControl.TUNER_ATSC);

        if(activateTuner(mTunerIdx)) {
            startDtvActivity();
            return;
        }

        ImageView imageLoad = (ImageView)findViewById(R.id.load_img);
        imageLoad.startAnimation(mAnimation);

        Log.i(LOG_TAG, "waiting tuner ready");

        super.onResume();
    }

    private void startDtvActivity() {
        Intent intentDtv = new Intent();

        // start dtv player or scan menu depends on channel existed or not
        if(mCiplContainer.loadScan(DtvPlayerApp.DbFilePath)) {
            int totalChannel = mCiplContainer.updateChannelList();

            if(totalChannel > 0) {
                intentDtv.setClass(getApplicationContext(), DtvActivity.class);
                Log.i(LOG_TAG, "Start DtvActivity directly");
            }
            else
            {
                intentDtv.setClass(getApplicationContext(), ScanActivity.class);
                Log.i(LOG_TAG, "No channel found: start ScanActivity");
            }
        }
        else {
            intentDtv.setClass(getApplicationContext(), ScanActivity.class);
            Log.i(LOG_TAG, "Database load failed start ScanActivity");
        }
        startActivity(intentDtv);

        finish();
    }

    /**
     * Activate the tuner with tuner index
     * @param tunerIndex : Tuner to activate
     * @return true if tuner be activated, false far otherwise
     */
    private boolean activateTuner(int tunerIndex) {
        boolean isTunerExist = false;
        boolean isTunerActivated = false;

        if(mCiplContainer == null)
            return false;

        if(mTunerIdx != tunerIndex)
            mTunerIdx = tunerIndex;

        Log.i(LOG_TAG, "activateTuner " + tunerIndex);

        isTunerExist = mCiplContainer.isCurrentTunerExist(getApplicationContext());
        if(isTunerExist) {
            Tuner curTuner = mCiplContainer.getCurrentTuner();
            isTunerExist = false;
            if(curTuner != null)
            {
                if(curTuner.getDeviceIndex() == tunerIndex)
                {
                    isTunerExist = true;
                    isTunerActivated = curTuner.isActivated();
                }
            }
        }

        if(!isTunerActivated)
        {
            //mActivateTunerThread.start();
            if(isTunerExist)
            {
                mActivateTunerThread.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        int ret = mCiplContainer.activateCurrentTuner();
                        if (ret < 0) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.e(LOG_TAG, "Failed activate tuner");
                                    Toast.makeText(LaunchActivity.this, "Failed to activate tuner!", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                });
            }
            else {
                mTunerList = mCiplContainer.getTunerList();
                if(mTunerList.size() > 0) {
                    mActivateTunerThread.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            int ret = mCiplContainer.activateTuner(mTunerIdx);
                            if (ret < 0) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.e(LOG_TAG, "Failed activate tuner");
                                        Toast.makeText(LaunchActivity.this, "Failed to activate tuner!", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    });
                }
                else if(mTaskUpdateTunerList == null) {
                    mTaskUpdateTunerList = new TaskUpdateTunerList();
                    mTaskUpdateTunerList.execute(mCiplContainer);
                }
            }
        }

        return isTunerActivated;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action == DtvControl.EVENT_TUNER_READY) {
                Log.i(LOG_TAG, "EVENT_TUNER_READY");

                if (activateTuner(mTunerIdx)) {
                    startDtvActivity();
                }
            }
            else if(action == DtvControl.EVENT_TUNER_DISCONNECTED) {
                Log.i(LOG_TAG, "EVENT_TUNER_DISCONNECTED");
                Toast.makeText(LaunchActivity.this, "Tuner disconnected!", Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public void onEvent(CiplEvent event) {
        if (event != null) {
            if (event.getEventName().equals(DtvControl.EVENT_DTV_INITIAL_END)) {
                String strRes = event.getBlob(1).getItem(0, 1);
                Log.i(LOG_TAG, "Tuner activation succeeded.");
                if (strRes.equals("0")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startDtvActivity();
                        }
                    });
                }
                else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.e(LOG_TAG, "Failed to activate tuner!");
                            Toast.makeText(LaunchActivity.this, "Failed to activate tuner!", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }
    }

}