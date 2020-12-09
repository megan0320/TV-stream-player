package com.asuka.dtvplayer;

import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cidana.cipl.CSplitBlob;
import com.cidana.cidanadtvsample.CiplContainer;
import com.cidana.cidanadtvsample.CiplEvent;

/**
 * Created by Wyvern on 2015/9/17.
 */
public class SignalTestDialog extends DialogFragment
        implements CiplContainer.ciplEventListener {
    private static final String TAG = "SignalTestDialog";

    private CiplContainer mCiplContainer;

    private EditText mEdtvTestFrequency;
    private EditText mEdtvBandwidth;
    private TextView mTxtvStatus;
    private Button mBtnTest;
    private Button mBtnOk;

    private boolean mIsTesting;

    private SharedPreferences mSavedSettings;
    private static final String mSettingFileName = "signal_test_settings";
    private static final String SETTING_TEST_FREQ = "test_frequency";
    private static final String SETTING_BAND_WIDTH = "band_width";

    @Override
    public void onCreate(Bundle savedInstanceSate) {
        super.onCreate(savedInstanceSate);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_signal_test, container, false);

        mCiplContainer = CiplContainer.getInstance();

        mEdtvTestFrequency = (EditText) rootView.findViewById(R.id.edtv_sigtestdlg_frequency);
        mEdtvBandwidth = (EditText) rootView.findViewById(R.id.edtv_sigtestdlg_band_width);
        mTxtvStatus = (TextView) rootView.findViewById(R.id.txtv_sigtestdlg_status);
        mBtnTest = (Button) rootView.findViewById(R.id.butn_sigtestdlg_test);
        mBtnOk = (Button) rootView.findViewById(R.id.butn_sigtestdlg_ok);

        mSavedSettings = getActivity().getSharedPreferences(mSettingFileName, 0);
        mEdtvTestFrequency.setText(String.valueOf(
                mSavedSettings.getInt(SETTING_TEST_FREQ,
                        Integer.valueOf(getString(R.string.sigtestdlg_edttxt_default_test_freq)))));
        mEdtvBandwidth.setText(String.valueOf(
                mSavedSettings.getInt(SETTING_BAND_WIDTH,
                        Integer.valueOf(getString(R.string.sigtestdlg_edttxt_default_band_width)))));

        mBtnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIsTesting) {
                    String testFreq = mEdtvTestFrequency.getText().toString();
                    String bandWidth = mEdtvBandwidth.getText().toString();
                    if (mCiplContainer.startSignalTest(Integer.valueOf(testFreq), Integer.valueOf(bandWidth))) {
                        mIsTesting = true;
                        SharedPreferences.Editor editor = mSavedSettings.edit();
                        editor.putInt(SETTING_TEST_FREQ, Integer.valueOf(testFreq));
                        editor.putInt(SETTING_BAND_WIDTH, Integer.valueOf(bandWidth));
                        editor.commit();
                        mEdtvTestFrequency.setEnabled(false);
                        mEdtvBandwidth.setEnabled(false);
                        mBtnTest.setText(getString(R.string.sigtestdlg_btntxt_stop_test));
                    } else {
                        Toast.makeText(getActivity(),
                                "Failed to start signal test. Please check the test frequency and bandwidth value.",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    mCiplContainer.stopSignalTest();
                    mIsTesting = false;
                    mEdtvTestFrequency.setEnabled(true);
                    mEdtvBandwidth.setEnabled(true);
                    mTxtvStatus.setText(getString(R.string.sigtestdlg_default_text_status));
                    mBtnTest.setText(getString(R.string.sigtestdlg_btntxt_start_test));
                }
            }
        });

        mBtnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsTesting) {
                    mCiplContainer.stopSignalTest();
                    mIsTesting = false;
                    mEdtvTestFrequency.setEnabled(true);
                    mEdtvBandwidth.setEnabled(true);
                    mTxtvStatus.setText(getString(R.string.sigtestdlg_default_text_status));
                    mBtnTest.setText(getString(R.string.sigtestdlg_btntxt_start_test));
                }
                dismiss();
            }
        });

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        mCiplContainer.addEventListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mCiplContainer.removeEventListener(this);
    }

    @Override
    public void onEvent(CiplEvent event) {
        if (event != null) {
            if (event.getEventName().equals("CIPL_TVC_SignalReport")) {
                CSplitBlob blob = event.getBlob(2);
                final String status = "Str: " + blob.getItem(0, 0) + ", Qlt: " + blob.getItem(0, 1);
                if (status != null) {
                    Log.i(TAG, "Event - SignalReport: "+status);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTxtvStatus.setText(status);
                        }
                    });
                }
            }
        }
    }
}
