package com.asuka.dtvplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;

import com.cidana.cidanadtvsample.CiplContainer;
import com.cidana.cidanadtvsample.Tuner;

import android.util.Log;

public class DtvReceiver extends BroadcastReceiver {
    static final String LOG_TAG = "DtvReceiver";

    private CiplContainer mCiplContainer;

    boolean mTunerReady = false;

    public DtvReceiver() {
        mCiplContainer = CiplContainer.getInstance();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(mCiplContainer == null)
        {
            mCiplContainer = mCiplContainer.getInstance();
            if(mCiplContainer == null)
                mCiplContainer = mCiplContainer.getInstance(context);
        }

        //set tuner
        if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action))
        {
            Log.i(LOG_TAG, "ACTION_USB_DEVICE_ATTACHED");

            boolean tunerReady = mCiplContainer.isCurrentTunerExist(context);
            if(tunerReady)
            {
                if(mTunerReady == false)
                {
                    mTunerReady = true;
                    Intent sendIntent = new Intent(DtvControl.EVENT_TUNER_READY);
                    context.sendBroadcast(sendIntent);

                    Log.i(LOG_TAG, "EVENT_TUNER_READY");
                }
            }
            else if(mTunerReady) // Tuner was deactived by unplug
            {
                Intent sendIntent = new Intent(DtvControl.EVENT_TUNER_DISCONNECTED);
                context.sendBroadcast(sendIntent);
            }
        }
        else if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
        {
            // USB device detached will be receive if tuner be activaitng, ignore this case
            if(mCiplContainer.isTunerActivaiting())
                return;

            Log.i(LOG_TAG, "ACTION_USB_DEVICE_DETACHED");

            boolean tunerReady = mCiplContainer.isCurrentTunerExist(context);
            if(!tunerReady)
            {
                if(mTunerReady) {
                    Intent sendIntent = new Intent(DtvControl.EVENT_TUNER_DISCONNECTED);
                    context.sendBroadcast(sendIntent);

                    // Get current USB tuner index
                    Tuner currTuner = mCiplContainer.getCurrentTuner();
                    if (currTuner != null) {
                        if (mCiplContainer.saveScan(context.getFilesDir().getAbsolutePath() + "/")) {
                            Log.i(LOG_TAG, "The scanned channel list has been saved.");
                        } else {
                            Log.d(LOG_TAG, "Failed to save the scanned channel list.");
                        }

                        // Deactive current tuner
                        mCiplContainer.deactivateCurrentTuner();
                    }

                    Intent launchIntent = new Intent();
                    launchIntent.setClass(context, LaunchActivity.class);
                    launchIntent.putExtra("error", "Tuner Disconnected!!!");
                    context.startActivity(launchIntent);

                    mTunerReady = false;
                }
            }
        }
    }

}