package com.asuka.dtvplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.content.SharedPreferences;
import android.widget.Toast;

import java.util.ArrayList;

import com.cidana.cidanadtvsample.CiplContainer;
import com.cidana.cidanadtvsample.CiplEvent;
import com.cidana.cidanadtvsample.Tuner;


public class ScanActivity extends Activity
        implements CiplContainer.ciplEventListener,
        DialogConfirmCallback{

    private static final String LOG_TAG = "ScanActivity";
    private static final String PREF_FILE = DtvControl.PREF_DTV_FILE;

    private CiplContainer mCiplContainer;
    private SharedPreferences mPref;


    private ArrayList<Tuner> mArylTunerList;

    private Button mButnATSC;
    private Button mButnJ83B;

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

    @Override
    public void onDialogConfirm(OpenfileDialog dialog, Bundle arg) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);

        mPref = getSharedPreferences(PREF_FILE, MODE_PRIVATE);


        mCiplContainer = CiplContainer.getInstance(this);

        setContentView(R.layout.scan);
        mButnATSC = (Button) findViewById(R.id.butn_scandlg_region_scan_ATSC);
        mButnJ83B = (Button) findViewById(R.id.butn_scandlg_region_scan_J83B);

        if (mCiplContainer != null) {
            mCiplContainer.addEventListener(this);
        } else {
            resumeLaunch();
            return;
        }

        mArylTunerList = mCiplContainer.getTunerList();
        if(mArylTunerList.size() == 0)
        {
            resumeLaunch();
            return;
        }

        mButnATSC.setEnabled(true);
        mButnJ83B.setEnabled(true);

        mButnATSC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                int StartFreq = 2;
                int EndFreq = 69;
                int BandWidth = 6000;
                 */
                /*activate ATSC */
                final int tunerID = DtvControl.TUNER_ATSC;
                mActivateTunerThread.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        int ret = mCiplContainer.activateTuner(tunerID);
                        if (ret < 0) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.w(LOG_TAG, "failed activate");
                                    //changeUIState(UISTATE_TUNER_NOT_SELECTED);
                                }
                            });
                        }
                        else
                        {
                            RegionScanDialog RegionScanDialog = new RegionScanDialog();
                            RegionScanDialog.show(getFragmentManager(), "Scan dialog");

                            // start scan action should execute in scan dialog, should be correct in next time
                            mCiplContainer.startScan(
                                    2,
                                    69,
                                    6000,
                                    false);
                        }
                    }
                });
            }
        });
        mButnJ83B.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                int StartFreq = 2;
                int EndFreq = 135;
                int BandWidth = 6000;
                 */
                /*activate j83b*/
                final int channelID = DtvControl.TUNER_J83B;
                mActivateTunerThread.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        int ret = mCiplContainer.activateTuner(channelID);
                        if (ret < 0) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.w(LOG_TAG, "failed activate");
                                    //changeUIState(UISTATE_TUNER_NOT_SELECTED);
                                }
                            });
                        }
                        else
                        {
                            RegionScanDialog RegionScanDialog = new RegionScanDialog();
                            RegionScanDialog.show(getFragmentManager(), "Scan dialog");

                            // start scan action should execute in scan dialog, should be correct in next time
                            mCiplContainer.startScan(
                                    2, /*2,*/
                                    135,
                                    6000,
                                    false);
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCiplContainer != null) {
            mCiplContainer.removeEventListener(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * Resume back to LaunchActivity
     * for tuner failed to inited/ tuner disconnected..
     */
    private void resumeLaunch() {
        Log.i(LOG_TAG, "resume LaunchActivity");

        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), LaunchActivity.class);
        startActivity(intent);

        finish();
    }

    /**
     * Start DtvActivity
     */
    private void startDtvPlayer() {
        Log.i(LOG_TAG, "into DtvActivity");

        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), DtvActivity.class);
        startActivity(intent);

        finish();
    }

    @Override
    public void onEvent(CiplEvent event) {
        if (event != null) {
            if (event.getEventName().equals(DtvControl.EVENT_DTV_INITIAL_END)) {
                String strRes = event.getBlob(1).getItem(0, 1);
                if (strRes.equals("0")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //changeUIState(UISTATE_TUNER_ACTIVATED);
                        }
                    });
                }
                else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //changeUIState(UISTATE_TUNER_NOT_SELECTED);
                        }
                    });
                }
            }
            else if (event.getEventName().equals(DtvControl.EVENT_DTV_SCAN_END)) {
                Log.i(LOG_TAG, "scan end");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int totalChannel = mCiplContainer.updateChannelList();

                        if(totalChannel > 0) {
                            if (mCiplContainer.saveScan(DtvPlayerApp.DbFilePath)) {
                                Log.i(LOG_TAG, "Scanned channel list be saved");
                                mPref.edit().putInt(DtvControl.KEY_PREF_TUNER_STD, mCiplContainer.getCurrentTuner().getDeviceIndex()).apply();
                                startDtvPlayer();
                            } else {
                                Log.i(LOG_TAG, "Failed to save the scanned channel list.");
                                Toast.makeText(ScanActivity.this, "Failed to save channel", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
            }
            else if (event.getEventName().equals(DtvControl.EVENT_DTV_SCAN_CANCEL)) {
                Log.i(LOG_TAG, "scan cancel");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int totalChannel = mCiplContainer.updateChannelList();
                        Log.i(LOG_TAG, "Scanned channel number"+totalChannel);
                        if(totalChannel > 0) {
                            if (mCiplContainer.saveScan(DtvPlayerApp.DbFilePath)) {
                                Log.i(LOG_TAG, "Scanned channel list be saved");
                                mPref.edit().putInt(DtvControl.KEY_PREF_TUNER_STD, mCiplContainer.getCurrentTuner().getDeviceIndex()).apply();
                                startDtvPlayer();
                            } else {
                                Log.i(LOG_TAG, "Failed to save the scanned channel list.");
                                Toast.makeText(ScanActivity.this, "Failed to save channel", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
            }
        }
    }

}
