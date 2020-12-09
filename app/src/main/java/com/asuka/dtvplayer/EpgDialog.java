package com.asuka.dtvplayer;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import com.cidana.cidanadtvsample.CiplContainer;
import com.cidana.cidanadtvsample.EventInfo;

/**
 * Created by Wyvern on 2015/6/15.
 */
public class EpgDialog extends DialogFragment{
    private static final String TAG = "EpgDialog";

    private ListView mLstvEpg;
    private EpgAdapter mAdptEpg;
    private ArrayList<String> mArylEpg;
    private TextView mCurrSelectedTextview;
    private int mCurrSelectedPosition = -1;

    private Button mButnAction;
    private Button mButnCancel;

    private CiplContainer mCiplContainer;

    @Override
    public void onCreate(Bundle savedInstanceSate) {
        super.onCreate(savedInstanceSate);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_epg, container, false);

        mLstvEpg = (ListView)rootView.findViewById(R.id.lstv_epgdlg_epg);

        mButnAction = (Button)rootView.findViewById(R.id.butn_epg_action);
        mButnCancel = (Button)rootView.findViewById(R.id.butn_epg_cancel);
        mButnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mCiplContainer = CiplContainer.getInstance();
        ArrayList<EventInfo> eventList = mCiplContainer.getEpgEventList();
        mArylEpg = new ArrayList<String>();
        if (eventList != null) {
            for (EventInfo eventInfo : eventList) {
                mArylEpg.add(eventInfo.toString());
            }
        }
        mAdptEpg = new EpgAdapter();
        mLstvEpg.setAdapter(mAdptEpg);
        mLstvEpg.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mCurrSelectedTextview != null) {
                    mCurrSelectedTextview.clearAnimation();
                }
                final TextView textview = (TextView) view.findViewById(R.id.txtv_epg_list_item);
                setTextviewHorizontalScrolling(view, textview);
                mCurrSelectedTextview = textview;
                mCurrSelectedPosition = position;
            }
        });

        return rootView;
    }

    private void setTextviewHorizontalScrolling(View parent_view, TextView text_view) {
        int parent_width = parent_view.getWidth();
        text_view.measure(0, 0);
        int text_width = text_view.getMeasuredWidth();
        if (parent_width < text_width) {
            text_view.setWidth(text_width);
            text_view.invalidate();
            TranslateAnimation animation = new TranslateAnimation(0, parent_width-text_width, 0, 0);
            animation.setDuration(5000);
            animation.setRepeatMode(Animation.REVERSE);
            animation.setRepeatCount(Animation.INFINITE);
            text_view.startAnimation(animation);
        }
    }

    public void show(FragmentManager fragmentManager, String epg_dialog) {

    }

    private class EpgAdapter extends BaseAdapter {
        private LayoutInflater inflater;

        @Override
        public int getCount() {
            if (mArylEpg != null) {
                return mArylEpg.size();
            }
            else {
                return 0;
            }
        }

        @Override
        public String getItem(int position) {
            if (mArylEpg != null &&
                    position >= 0 && position < mArylEpg.size()) {
                return mArylEpg.get(position);
            }
            else {
                return null;
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                if (inflater == null) {
                    inflater = getActivity().getLayoutInflater();
                }
                if (inflater == null) {
                    return null;
                }
                convertView = inflater.inflate(R.layout.listitem_epg, null);
            }
            if (convertView == null) {
                return null;
            }
            final TextView epgtxt = (TextView) convertView.findViewById(R.id.txtv_epg_list_item);
            epgtxt.clearAnimation();
            epgtxt.setText(getItem(position));
            if (position == mCurrSelectedPosition) {
                setTextviewHorizontalScrolling(convertView, epgtxt);
            }
            return convertView;
        }
    }
}
