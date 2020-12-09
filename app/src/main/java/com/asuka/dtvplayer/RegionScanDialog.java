package com.asuka.dtvplayer;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.Log;

import com.cidana.cipl.CSplitBlob;
import com.cidana.cidanadtvsample.CiplContainer;
import com.cidana.cidanadtvsample.CiplEvent;

import java.text.DecimalFormat;


/**
 * Created by Wyvern on 2015/5/20.
 */
public class RegionScanDialog extends DialogFragment
        implements CiplContainer.ciplEventListener {
    private static final String TAG = "RegionScanDialog";

    private TextView mTxtvTitle;
    private ProgressBar mProgbarInTitle;

    private View mViewChooseMode;
    //private View mViewManualScan;
    private View mViewRegionScan;
    private View mViewInScan;
    private View mViewScanDone;
    private static final int CHOOSE_MODE = 0;
    private static final int MANUAL_SCAN = 1;
    private static final int REGION_SCAN = 2;
    private static final int IN_SCAN = 3;
    private static final int SCAN_DONE = 4;


    private TextView mTxtvScanAt;
    private ProgressBar mProgbarScan;
    private TextView mTxtvScanProgress;
    private TextView mTxtvChannelScanned;

    private Button mButnCancel;

    private CiplContainer mCiplContainer;
    private int mTunerType;


    @Override
    public void onCreate(Bundle savedInstanceSate) {
        super.onCreate(savedInstanceSate);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_scan, container, false);

        mTxtvTitle = (TextView)rootView.findViewById(R.id.dialog_scan_title_text);
        mProgbarInTitle = (ProgressBar)rootView.findViewById(R.id.dialog_scan_title_wheel);

        mViewChooseMode = rootView.findViewById(R.id.dialog_scan_viewgroup_choose_mode);
        //mViewManualScan = rootView.findViewById(R.id.dialog_scan_viewgroup_manual_scan);
        mViewRegionScan = rootView.findViewById(R.id.dialog_scan_viewgroup_region_scan);
        mViewInScan = rootView.findViewById(R.id.dialog_scan_viewgroup_in_scan);
        mViewScanDone = rootView.findViewById(R.id.dialog_scan_viewgroup_scan_done);

        mTxtvScanAt = (TextView)rootView.findViewById(R.id.txtv_scan_at);
        mProgbarScan = (ProgressBar)rootView.findViewById(R.id.progbar_scan);
        mTxtvScanProgress = (TextView)rootView.findViewById(R.id.txtv_scan_progress);
        mTxtvChannelScanned = (TextView)rootView.findViewById(R.id.txtv_channel_scanned);


        mButnCancel = (Button)rootView.findViewById(R.id.butn_scandlg_cancel);
        mButnCancel.setOnClickListener(mCancelListener);

        mCiplContainer = CiplContainer.getInstance();


        return rootView;
    }

    private void changeDialogContent(int contentMode) {
        switch (contentMode) {

            case IN_SCAN:
                mTxtvTitle.setText(R.string.scandlg_title_in_scan);
                mProgbarInTitle.setVisibility(View.VISIBLE);
                mViewChooseMode.setVisibility(View.GONE);
                mViewRegionScan.setVisibility(View.GONE);
                mViewInScan.setVisibility(View.VISIBLE);
                mViewScanDone.setVisibility(View.VISIBLE);
                mButnCancel.setVisibility(View.VISIBLE);
                mButnCancel.setText(R.string.scandlg_btntxt_stop);
                mButnCancel.setOnClickListener(mStopScanListener);
                break;
            case SCAN_DONE:
                dismiss();
                mTxtvTitle.setText(R.string.scandlg_title_scan_done);
                mProgbarInTitle.setVisibility(View.INVISIBLE);
                mViewChooseMode.setVisibility(View.GONE);
                //mViewManualScan.setVisibility(View.GONE);
                mViewRegionScan.setVisibility(View.GONE);
                mViewInScan.setVisibility(View.GONE);
                mViewScanDone.setVisibility(View.GONE);
                mTxtvChannelScanned.setVisibility(View.GONE);
                mButnCancel.setVisibility(View.VISIBLE);
                mButnCancel.setText(R.string.scandlg_btntxt_ok);
                dismiss();
                break;
        }
    }

    // for ATSC & J83.B only now
    int getFreqMHzByPhyChannel(int ch)
    {
        int freq = 0;

        if( mTunerType == DtvControl.TUNER_ATSC) {
            int freqLowest = 54;
            int chLowest = 2;
            int bandwidth = 6;

            if (ch < chLowest)
                return 0;

            // ATSC VHF low-band
            if (ch < 5) { // 2~4
                //freq = ((ch - chLowest) * bandwidth) + freqLowest;
            }
            else if(ch < 7) {   // 5~6
                chLowest = 5;
                freqLowest = 79;
            }
            // ATSC VHF high-band
            else if(ch < 14) { // 7~13
                chLowest = 7;
                freqLowest = 177;
            }
            // ATSC UHF band (ATSC signal 310 KHz above lower edge)
            else {  // 14~
                chLowest = 14;
                freqLowest = 473;
            }

            freq =  ((ch - chLowest) * bandwidth) + freqLowest;
        }
        else if( mTunerType == DtvControl.TUNER_J83B) {
            int freqLowest = 57;
            int chLowest = 2;
            int bandwidth = 6;

            if (ch < chLowest)
                return 0;

            // ATSC VHF low-band
            if (ch < 5) { // 2~4
                //freq = ((ch - chLowest) * bandwidth) + freqLowest;
            }
            else if(ch < 7) {   // 5~6
                chLowest = 5;
                freqLowest = 79;
            }
            // ATSC UHF band (ATSC signal 310 KHz above lower edge)
            else if(ch < 14) {   // 7~13
                chLowest = 7;
                freqLowest = 177;
            }
            else if(ch < 23) {   // 14~22
                chLowest = 14;
                freqLowest = 123;
            }
            else if(ch < 95) {   // 23~94
                chLowest = 23;
                freqLowest = 219;
            }
            else if(ch < 100) {   // 95~99
                chLowest = 95;
                freqLowest = 93;
            }
            else {  // 100~135
                chLowest = 100;
                freqLowest = 651;
            }

            freq =  ((ch - chLowest) * bandwidth) + freqLowest;
        }

        if(freq == 0)
            freq = ch;

        return freq;
    }

    @Override
    public void onStart() {
        super.onStart();
        mCiplContainer.addEventListener(this);
        mTunerType = mCiplContainer.getCurrentTuner().getDeviceIndex();

        int startFreq = 0;
        if(mTunerType == DtvControl.TUNER_ATSC)
            startFreq = 57;
        else if(mTunerType == DtvControl.TUNER_J83B)
            startFreq = 57;

        mTxtvScanAt.setText(String.valueOf(startFreq) + " MHz");

        changeDialogContent(IN_SCAN);

    }

    @Override
    public void onStop() {
        super.onStop();
        mCiplContainer.removeEventListener(this);
    }


    @Override
    public void onEvent(CiplEvent event) {
        if (event != null) {
            if (event.getEventName().equals(DtvControl.EVENT_DTV_SCAN_PROGRESS)) {
                CSplitBlob blob = event.getBlob(2);
                int channel = Integer.parseInt(blob.getItem(0, 1));
                final int progress = Integer.parseInt(blob.getItem(0, 0));
                final String frequency = getFreqMHzByPhyChannel(channel) + " MHz";
                //Log.d(TAG, "scan freq : " + frequency + "(CH " + channel + ")");
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTxtvScanAt.setText(frequency);
                        mProgbarScan.setProgress(progress);
                        mTxtvScanProgress.setText(new DecimalFormat("##%").format((double)progress/100));
                        mTxtvChannelScanned.setText(
                                String.valueOf(mCiplContainer.updateChannelList()));
                    }
                });
            }
            else if (event.getEventName().equals(DtvControl.EVENT_DTV_SCAN_END)) {
                Log.i(TAG, "Channel scan finished.");
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        changeDialogContent(SCAN_DONE);
                    }
                });
            }
            else if (event.getEventName().equals(DtvControl.EVENT_DTV_SCAN_CANCEL)) {
                Log.i(TAG, "Channel scan stopped.");
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        changeDialogContent(SCAN_DONE);
                    }
                });
            }
        }
    }

    private View.OnClickListener mStopScanListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mCiplContainer.stopScan();
        }
    };

    private View.OnClickListener mCancelListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            dismiss();
        }
    };

    private DialogConfirmCallback mConfirmListener;
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mConfirmListener = (DialogConfirmCallback)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement RegionScanDialogConfirmCallback");
        }
    }
    private View.OnClickListener mOkListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    changeDialogContent(SCAN_DONE);
                }
            });
        }
    };

}
