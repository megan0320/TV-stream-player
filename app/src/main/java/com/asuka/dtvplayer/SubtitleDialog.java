package com.asuka.dtvplayer;

import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.cidana.cidanadtvsample.CiplContainer;
import com.cidana.cidanadtvsample.CiplEvent;

/**
 * Created by Edmond on 2016/3/17.
 */
public class SubtitleDialog extends DialogFragment
        implements CiplContainer.ciplEventListener
{
    private static final String TAG = "SubtitleDialog";

    private CiplContainer mCiplContainer;

    private SharedPreferences mSavedSettings;
    private static final String mSettingFileName = "subtitle_settings";

    private static final String mOffStr = "Off";

    private RadioGroup mRadioGroup;

    @Override
    public void onCreate(Bundle savedInstanceSate)
    {
        super.onCreate(savedInstanceSate);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,  Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.option_dialog, container, false);
        mCiplContainer = CiplContainer.getInstance();
        mSavedSettings = getActivity().getSharedPreferences(mSettingFileName, 0);

        TextView textTitle = (TextView) rootView.findViewById(R.id.title);
        textTitle.setText("Subtitle Track");

        String[] arrSubtitle = mCiplContainer.showSubtitleTrack();
        int currSubtitle = mCiplContainer.getCurrentSubtitle();

        mRadioGroup = (RadioGroup)rootView.findViewById(R.id.radioGroup);
        if (mRadioGroup != null)
        {
            for (int i = 0; i <= ((arrSubtitle==null)?0:arrSubtitle.length); i++)
            {
                RadioButton radio = (RadioButton)inflater.inflate(R.layout.option_radio, container);
                radio.setId(i);
                if (i == 0)
                    radio.setText(mOffStr);
                else
                    radio.setText(arrSubtitle[i-1]);
                mRadioGroup.addView(radio);
                if (currSubtitle == i-1)
                {
                    radio.setChecked(true);
                    radio.requestFocus();
                }
            }

            mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
            {
                @Override
                public void onCheckedChanged(RadioGroup arg0, int arg1)
                {
                    Log.i(TAG, "onCheckedChanged, arg0: " + arg0 + ", arg1: " + arg1);
                    mCiplContainer.setSubtitleTrack(arg1-1);
                    dismiss();
                }
            });
        }

        return rootView;
    }

    @Override
    public void onEvent(CiplEvent event)
    {
        if (event != null)
        {
            if (event.getEventName().equals("CIPL_TVC_SubtitleChanged"))
            {
                Log.i(TAG, "CIPL_TVC_SubtitleChanged");
            }
        }
    }
}
