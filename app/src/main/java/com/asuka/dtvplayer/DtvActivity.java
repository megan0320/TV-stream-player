package com.asuka.dtvplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.asuka.os.AskBuild;
import com.asuka.dtvplayer.bridgeservice.Utils;
import com.cidana.cipl.CSplitBlob;

import android.content.SharedPreferences;
import android.media.AudioManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import com.cidana.cidanadtvsample.CiplContainer;
import com.cidana.cidanadtvsample.CiplEvent;
import com.cidana.cidanadtvsample.Channel;


public class DtvActivity extends Activity
        implements CiplContainer.ciplEventListener,
        CiplContainer.ciplSubtitleListener,
        DialogConfirmCallback,
        SurfaceHolder.Callback {
    private static final String TAG = "DtvActivity";
    private static final String PREF_FILE = DtvControl.PREF_DTV_FILE;

    static final int DELAY=30000;

    static final String KEY_CHANNEL_NUM = "channel";

    private static final int MSG_TIME_UPDATE = 1;
    private static final int MSG_CH_NUM_CLOSE = 2;
    private static final int MSG_SWITCH_CHANNEL = 3;

    private static final String CH_NAME_DEFAULT = "";
    private static final int CH_IDX_DEFAULT = 0;
    private static final int SUB_DEFAULT = 0;
    private static final int AUD_DEFAULT = 0;

    private static final String mOffStr = "Off";

    private CiplContainer mCiplContainer;

    private SurfaceView mSurfViewPlaybackWindow;
    private SurfaceHolder mSurfvHolder;

    private ArrayAdapter<String> mAdptSubtitle;
    private ImageView mWindowSubtitle;
    private Spinner mSpinnerSubtitle;

    private int mLastSubOption;
    private int mLastAudOption;

    private String[] mArrSubtitle={};
    private ArrayList<String> mSubtitleList;

    private int mCurrChannelIdx=-1;

    private ArrayList<Channel> mArylChannelList;

    ViewGroup mPlayViewGroup;


    private ArrayAdapter<String> mAdptAudio;
    private Spinner mSpinnerAudio;
    private AudioManager mAudioManager;

    private TextView mTxtChTitle;
    private ImageView mImgSignalStrength;
    private TextView mTxtTimeStamp;
    private TextView mTxtLoading;

    private Button mBtnChannelInfo;
    private Button mBtnScan;
    private TextView mTxtstrVersion;

    private TextView mTxtOsdString;


    ViewGroup mChannelBarViewGroup;
    ViewGroup mViewScrollMenu;
    int mMenuStatus=0;//menu is not open

    private SharedPreferences mPref;
    SharedPreferences.Editor prefEditor;

    private ArrayAdapter<String> mAdptTimeZone;
    private static SimpleDateFormat mUiDateFormat = new SimpleDateFormat("MMM dd  HH:mm");
    final String[] TimezoneOption = {"US/Alaska","US/Aleutian","US/Arizona","US/Central","US/Estern","US/Hawaii","US/Pacific","Canada/Pacific","Canada/Mountain","Canada/Central","Canada/Eastern","Canada/Atlantic"};
    private Spinner mSpinnerTimeZoneList;
    private AlarmManager mAlarmManager;
    private Calendar mCalendar;
    private Date mCurDateTime;
    TimeZone mTimeZone;

    private Timer mTimer;

    TextView mTxtNoSignal;
    static int mCntNoSignal=0;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_TIME_UPDATE:
                    mTxtTimeStamp.setText(mUiDateFormat.format(mCalendar.getTime()));
                    break;
                case MSG_CH_NUM_CLOSE:
                    mTxtOsdString.setVisibility(View.INVISIBLE);
                    Log.i(TAG, "mTxtOsdString.setVisibility(View.INVISIBLE)");
                    break;
                case MSG_SWITCH_CHANNEL: {
                    String channel = msg.getData().getString(KEY_CHANNEL_NUM);
                    Log.i(TAG, "switch channel " + channel);
                    if(playChannelByChNo(channel) == false) {
                        mTxtOsdString.setText(getText(R.string.main_switch_channel_number));
                    }
                }
                    break;
            }
        }
    };

    @Override
    public void onDialogConfirm(OpenfileDialog dialog, Bundle arg) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        IntentFilter intentFilterEvent = new IntentFilter();
        intentFilterEvent.addAction(DtvControl.EVENT_ENTER_SCAN_REQUEST);
        intentFilterEvent.addAction(Utils.EVENT_CHANNEL);
        intentFilterEvent.addAction(Utils.EVENT_OSD_STRING);
        registerReceiver(broadcastReceiver, intentFilterEvent);

        mTxtNoSignal =(TextView)findViewById(R.id.noSignalWarning);

        mPlayViewGroup=(ViewGroup)findViewById(R.id.playMode);
        mChannelBarViewGroup=(ViewGroup) findViewById(R.id.viewgroup_channelbanner);
        mTxtLoading = (TextView)findViewById(R.id.programTxtViewLoading);

        mViewScrollMenu=(ViewGroup)findViewById(R.id.scrollMenu);
        mSurfViewPlaybackWindow = (SurfaceView)findViewById(R.id.surfv_main_playback_window);
        mSurfViewPlaybackWindow.getViewTreeObserver().
                addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        mSurfViewPlaybackWindow.getViewTreeObserver().removeOnPreDrawListener(this);
                        int width = mSurfViewPlaybackWindow.getMeasuredWidth();
                        int height = width * 9 / 16;
                        mSurfViewPlaybackWindow.setLayoutParams(new RelativeLayout.LayoutParams(width, height));
                        return true;
                    }
                });
        mSurfvHolder = mSurfViewPlaybackWindow.getHolder();
        mSurfvHolder.addCallback(this);
        mSurfvHolder.setKeepScreenOn(true);

        mSpinnerSubtitle = (Spinner)findViewById(R.id.spinner_subtitle);
        mSubtitleList = new ArrayList<String>();
        mSubtitleList.clear();
        mAdptSubtitle = new ArrayAdapter<String>(this, R.layout.listitem_file,R.id.txtv_file_list_item, mSubtitleList);
        mSpinnerSubtitle.setAdapter(mAdptSubtitle);

        mTxtOsdString=(TextView)findViewById(R.id.channel_input_view);

        mSpinnerSubtitle.post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, " setOn Subtitle ItemSelectedListener = " );
                mSpinnerSubtitle.setOnItemSelectedListener(mSpinnerSubtitleListener);
            }
        });

        mWindowSubtitle = (ImageView)findViewById(R.id.subtitle_window);
        mWindowSubtitle.getViewTreeObserver().
                addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        mWindowSubtitle.getViewTreeObserver().removeOnPreDrawListener(this);
                        int width = mWindowSubtitle.getMeasuredWidth();
                        int height = width * 9 / 16;
                        mWindowSubtitle.setLayoutParams(new RelativeLayout.LayoutParams(width, height));
                        return true;
                    }
                });
        mWindowSubtitle.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mWindowSubtitle.setVisibility(View.VISIBLE);

        mTxtChTitle= (TextView)findViewById(R.id.programTxtViewTitle);
        mImgSignalStrength = (ImageView)findViewById(R.id.imgtag_main_signal_strength_0);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        mTxtTimeStamp = (TextView)findViewById(R.id.tv_timestamp);



        mCiplContainer = CiplContainer.getInstance(this);

        if (mCiplContainer != null) {
            mCiplContainer.addEventListener(this);
            mCiplContainer.addSubtitleListener(this);

            int decoderMode = 0;    // software decode
            /* hardware decode, decode error sometimes
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                mDecoderMode = 1;   // hw1 (media codec)
            } else {
                mDecoderMode = 2;   // hw2 (OMX)
            }
            */
            mCiplContainer.setVideoDecoderMode(decoderMode);
        }
        else {
            Log.w(TAG, "Cipl sdk initialization failed.");
            resumeLaunch();
            return;
        }

        ArrayList<Channel> list = mCiplContainer.getChannelList();
        if((list.size() == 0)) {
            mPlayViewGroup.setVisibility(View.INVISIBLE);
            enterScan();
            return;
        }
        else{
            mPlayViewGroup.setVisibility(View.VISIBLE);
        }

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAlarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

        mSpinnerAudio = (Spinner)findViewById(R.id.spinner_audio);

        mCalendar = Calendar.getInstance();
        mTimeZone=TimeZone.getDefault();
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        mCalendar.setTimeZone(mTimeZone);
        mUiDateFormat.setTimeZone(mCalendar.getTimeZone());
        mTxtTimeStamp.setText(mUiDateFormat.format(mCalendar.getTime()));

        mPref = getSharedPreferences(PREF_FILE, MODE_PRIVATE);




        /*menu related*/
        mTxtstrVersion=(TextView)findViewById(R.id.txt_version_info);
        mBtnChannelInfo=(Button)findViewById(R.id.btn_channel_info);
        mBtnScan=(Button)findViewById(R.id.btn_scan);
        mCalendar = Calendar.getInstance();

        mBtnScan.setOnClickListener(mBtnScanListener);
        mBtnChannelInfo.setOnClickListener(mBtnChannelInfoListener);

        /*TimeZoneSpinner begin*/
        mSpinnerTimeZoneList = (Spinner)findViewById(R.id.spinner_main_timezone_list);
        mAdptTimeZone = new ArrayAdapter<String>(this,R.layout.listitem_file,R.id.txtv_file_list_item,TimezoneOption);
        mSpinnerTimeZoneList.setAdapter(mAdptTimeZone);
        mSpinnerTimeZoneList.post(new Runnable() {
            @Override
            public void run() {
                mSpinnerTimeZoneList.setOnItemSelectedListener(mSpinnerTimeZoneListener);
            }
        });
        String compareValue = mCalendar.getTimeZone().getID();

        if (compareValue != null) {
            int spinnerPosition = mAdptTimeZone.getPosition(compareValue);
            mSpinnerTimeZoneList.setSelection(spinnerPosition);
        }
        else
            mSpinnerTimeZoneList.setSelection(0);

        String strVersion = AskBuild.DISPLAY;
        String custName = getResources().getString(R.string.cust_name);
        String custVersion = getResources().getString(R.string.cust_version);
        Log.i(TAG, " strVersion = " + String.format("%s", strVersion));
        String [] splitStrVersion = strVersion.split(" ");

        mTxtstrVersion.setText(custName);
        mTxtstrVersion.append(" ");

        if(splitStrVersion[0].contains("userdebug")){
            String [] GetSWStrVersion = splitStrVersion[2] .split("_");
            mTxtstrVersion.append(String.format("%s", GetSWStrVersion[0]));
        }
        else{
            mTxtstrVersion.append(String.format("%s", splitStrVersion[0]));
        }

        if(custVersion.length()>0)
            mTxtstrVersion.append(".");

        mTxtstrVersion.append(custVersion);

    }

    /**
     * Resume back to LaunchActivity
     * for tuner failed to inited/ tuner disconnected..
     */
    private void resumeLaunch() {
//        Log.i(TAG, "resume LaunchActivity");

        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), LaunchActivity.class);
        startActivity(intent);

        finish();
    }

    private Button.OnClickListener mBtnScanListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(DtvActivity.this);
            builder.setCancelable(true);
            builder.setMessage(R.string.prompt_enter_scan);
            builder.setPositiveButton(R.string.str_confirm,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            enterScan();
                        }
                    });
            builder.setNegativeButton(R.string.str_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    };
    private Button.OnClickListener mBtnChannelInfoListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
        }
    };

    /**
     * Start ScanActivity
     */
    private void enterScan() {
        Log.i(TAG, "into ScanActivity");

        mCiplContainer.clearScan(DtvPlayerApp.DbFilePath);  // clear database first before back to scan
        resetPref();
        updateSignalStatus(false,-1);

        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), ScanActivity.class);
        startActivity(intent);

        finish();
    }

    private void resetPref(){
        Log.i(TAG, "resetPref");
        SharedPreferences.Editor editor = mPref.edit();

        editor.putInt(DtvControl.KEY_PREF_CHANNEL_IDX, CH_IDX_DEFAULT);
        editor.putString(DtvControl.KEY_PREF_CHANNEL_NAME, CH_NAME_DEFAULT);

        editor.putInt(DtvControl.KEY_PREF_SAVE_SUB_IDX, SUB_DEFAULT);
        editor.putInt(DtvControl.KEY_PREF_SAVE_AUD_IDX, AUD_DEFAULT);

        editor.apply();
    }

    private void startTimeUpdateTimer(){
        if(mTimer == null) {
            mTimer = new Timer(true);
            mTimer.schedule(new setTimeUpdateTask(), 5000, 30000);
        } else {
            mTimer.cancel();
            mTimer = new Timer(true);
            mTimer.schedule(new setTimeUpdateTask(), 5000, 30000);
        }
    }

    private class setTimeUpdateTask extends TimerTask {
        public void run(){
            Message message = new Message();
            message.what = MSG_TIME_UPDATE;
            mHandler.sendMessage(message);
        }
    }

    private void stopTimeUpdateTimer(){
        mHandler.removeMessages(MSG_TIME_UPDATE);
        if(mTimer != null)
        {
            mTimer.cancel();
            mTimer=null;
        }
    }
    private AdapterView.OnItemSelectedListener mSpinnerTimeZoneListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mAlarmManager.setTimeZone(TimezoneOption[(int)id]);
            mTimeZone.setDefault(TimeZone.getTimeZone(TimezoneOption[(int)id]));
            mCalendar.setTimeZone(TimeZone.getDefault());

            mUiDateFormat.setTimeZone(mCalendar.getTimeZone());
            mTxtTimeStamp.setText(mUiDateFormat.format(mCalendar.getTime()));
//            Log.i(TAG, "TimeZone "+mCalendar.getTime());
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    private AdapterView.OnItemSelectedListener mSpinnerAudioListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            //Log.i(TAG, "onCheckedChanged, arg0: " + position);
            //Log.i(TAG, "onItemSelected " );
            if(position>=0) {
                mCiplContainer.setAudioTrack(position - 1);
            }
            mPref.edit().putInt(DtvControl.KEY_PREF_SAVE_AUD_IDX, position).apply();

        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            Log.i(TAG, "onNothingSelected " );
        }
    };
    
    private void loadChannel(){
        if (mCiplContainer.loadScan(DtvPlayerApp.DbFilePath)) {
            mCiplContainer.updateChannelList();
            mArylChannelList = mCiplContainer.getChannelList();


        }
        else {
            Toast.makeText(DtvActivity.this, "Failed to load the scanned channel list.", Toast.LENGTH_SHORT)
                    .show();
            enterScan();
        }
    }


    /**
     * update signal status by showing text and images depending on signalStrength if enableSignalUpdate is true, otherwise reset signal status and hide text
     * @param enableSignalUpdate true is to enable signal update, false is to disable signal update
     * @param signalStrength current signal strength
     */

    void updateSignalStatus(boolean enableSignalUpdate, int signalStrength){
        if(enableSignalUpdate) {
            if(signalStrength<=0){
                mImgSignalStrength.setImageResource(R.drawable.signal_lvl0);
                if(mViewScrollMenu.getVisibility() == View.INVISIBLE) {
                    if (mTxtNoSignal.getVisibility() == View.INVISIBLE) {
                        if (mCntNoSignal >= 5) {
                            mTxtNoSignal.setVisibility(View.VISIBLE);
                        } else
                            mCntNoSignal++;
                    }
                }
            }
            else {
                mTxtNoSignal.setVisibility(View.INVISIBLE);
                mCntNoSignal=0;
                if(signalStrength==1){
                    mImgSignalStrength.setImageResource(R.drawable.signal_lvl1);
                }
                else {
                    mImgSignalStrength.setImageResource(R.drawable.signal_lvl2);
                }
            }
        }
        else {
            mTxtNoSignal.setVisibility(View.INVISIBLE);
            mCntNoSignal=0;
        }
    }

    void showChannelBar(int delay){
        Channel channel = mCiplContainer.getCurrentChannel();

        mChannelBarViewGroup.setVisibility(View.GONE);

        mTxtChTitle.setText(channel.getLcn());
        mTxtChTitle.append("  ");
        mTxtChTitle.append(channel.getName());

        mChannelBarViewGroup.setVisibility(View.VISIBLE);

        /*
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                // Hide your View after 3 seconds
                mChannelBarViewGroup.setVisibility(View.GONE);
            }
        }, delay);
        */
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
//        Log.i(TAG, "onResume");
        loadChannel();
        if(mCurrChannelIdx==-1){//first play including restart & rescan
            int savedChIdx=mPref.getInt(DtvControl.KEY_PREF_CHANNEL_IDX, CH_IDX_DEFAULT);

            if(mCiplContainer.getChannel(savedChIdx).getName().equals(//restart the same channel
                    mPref.getString(DtvControl.KEY_PREF_CHANNEL_NAME, CH_NAME_DEFAULT))){
                playChannelByIndex(savedChIdx);
            }
            else{//restart different channel
                playChannelByIndex(CH_IDX_DEFAULT);
            }
        }
        else if(!playChannelByIndex(mCurrChannelIdx)) {
            Toast.makeText(DtvActivity.this, "Failed to play TV", Toast.LENGTH_SHORT).show();
            enterScan();
            return;
        }

        showChannelBar(DELAY);
        startTimeUpdateTimer();

        updateSignalStatus(false,-1);

        super.onResume();
    }

    @Override
    protected void onPause() {
        mCiplContainer.stopCurrentChannel();
        stopTimeUpdateTimer();
        super.onPause();
    }

    private AdapterView.OnItemSelectedListener mChannelListListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            if(id>=mArylChannelList.size()){
                id=0;
            }
            playChannelByIndex((int)id);

        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };
    private AdapterView.OnItemSelectedListener mSpinnerSubtitleListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Log.i(TAG, "position "+position);
            Log.i(TAG, "mLastSubOption "+mLastSubOption);

            if(mLastSubOption!=position)//if subtitle option changed
                clearSubtitle();
            if(position==0)
                mWindowSubtitle.setVisibility(View.INVISIBLE);
            else{
                mWindowSubtitle.setVisibility(View.VISIBLE);
                mCiplContainer.setSubtitleTrack(position-1);
            }

            //Log.i(TAG, "onCheckedChanged, arg0: " + position);
            //Log.i(TAG, "onItemSelected " );

            mPref.edit().putInt(DtvControl.KEY_PREF_SAVE_SUB_IDX, position).apply();
            mLastSubOption=position;
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            Log.i(TAG, "onNothingSelected " );
        }
    };

    private class ChannelSpinnerAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            if (mArylChannelList == null) {
                return 1;
            }
            else {
                return mArylChannelList.size();
            }
        }

        @Override
        public Channel getItem(int position) {
            if (mArylChannelList == null) {
                return null;
            }
            return mArylChannelList.get(position);
        }

        @Override
        public long getItemId(int position) {
            if (mArylChannelList == null) {
                return -1;
            }
            else {
                return (long)(getItem(position).getChId());
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = DtvActivity.this.getLayoutInflater();
                convertView = inflater.inflate(R.layout.listitem_channel, null);
            }
            TextView itemText = (TextView)convertView.findViewById(R.id.txtv_channel_list_item);
            if (mArylChannelList == null || mArylChannelList.size() == 0) {
                itemText.setText(getString(R.string.prompt_no_channel_available));
                itemText.setTextColor(Color.DKGRAY);
                return convertView;
            }
            else if (mCiplContainer.getCurrentChannel() == null) {
                //no channel selected
                itemText.setText(mArylChannelList.size()+" "+getString(R.string.prompt_no_channel_selected));
                itemText.setTextColor(Color.DKGRAY);
                return convertView;
            }
            else {
                return getCustomView(position, convertView, parent);
            }
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = DtvActivity.this.getLayoutInflater();
                convertView = inflater.inflate(R.layout.listitem_channel, null);
            }
            TextView itemText = (TextView)convertView.findViewById(R.id.txtv_channel_list_item);
            if (mArylChannelList == null || mArylChannelList.size() == 0) {
                itemText.setText(getString(R.string.prompt_no_channel_available));
                itemText.setTextColor(Color.DKGRAY);
                return convertView;
            }
            else {
                return getCustomView(position, convertView, parent);
            }
        }

        private View getCustomView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = DtvActivity.this.getLayoutInflater();
                convertView = inflater.inflate(R.layout.listitem_channel, null);
            }
            TextView itemText = (TextView)convertView.findViewById(R.id.txtv_channel_list_item);
            itemText.setText(getItem(position).toString());
            return convertView;
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //Log.i(TAG, "Set video window surface.");
        if (mCiplContainer.setVideoWindow(holder.getSurface())) {
            //Log.i(TAG, "Video window set successfully.");
        }
        else {
            Log.w(TAG, "Video window set failed.");
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //Log.i(TAG, "surfaceChanged, isValid="+holder.getSurface().isValid()+".");
        //Log.i(TAG, "Video window changed: format="+format+ ", width="+width+", height="+height+".");
        Rect dstRect = new Rect(0, 0, width, height);
        //Log.i(TAG, "Set video window, size=" + dstRect + ".");
        if (mCiplContainer.setVideoDestRect(dstRect)) {
        }
        else {
            Log.w(TAG, "Video window set failed.");
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
//        Log.i(TAG, "Destroy video window.");
        if (mCiplContainer.removeVideoWindow()) {
        }
        else {
            Log.w(TAG, "Video window removal failed.");
        }
        if(holder!=null && holder.getSurface()!=null){
            mSurfViewPlaybackWindow.destroyDrawingCache();
            if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                mSurfViewPlaybackWindow.releasePointerCapture();
            if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                holder.getSurface().release();
        }
    }

    // return value: save channle list success/failed
    private boolean updateChannelList() {
        mCiplContainer.updateChannelList();
        mArylChannelList = mCiplContainer.getChannelList();
        boolean bSaved = true;
        if (!mCiplContainer.saveScan(DtvPlayerApp.DbFilePath)) {
            bSaved = false;
        }

        return bSaved;
    }
    @Override
    public void onEvent(CiplEvent event) {
        if (event != null) {
            //Log.i(TAG, "onEvent  " + event.getEventName());
            if (event.getEventName().equals(DtvControl.EVENT_DTV_SIGNAL_CHANGED)) {
                final CSplitBlob blob = event.getBlob(1);
                final String signalStatus = blob.getItem(0, 1);


                if (signalStatus == null || signalStatus.equals("2")) {
                    mImgSignalStrength.post(new Runnable() {
                        @Override
                        public void run() {
                            updateSignalStatus(true, -1);
                        }
                    });
                }
                else{
                    final String signalStrength = blob.getItem(0, 2);
                    mImgSignalStrength.post(new Runnable() {
                        @SuppressLint("WrongConstant")
                        @Override
                        public void run() {
                            int intSignalStrength = 0;
                            if (signalStrength!= null){
                                intSignalStrength = Integer.parseInt(signalStrength);
                                updateSignalStatus(true, intSignalStrength);
                            }

                        }
                    });
                }
            }
            else if(event.getEventName().equals(DtvControl.EVENT_DTV_TIME_UPDATE)) {
                CSplitBlob blob = event.getBlob(2);
                final String stream_time = blob.getItem(0, 0);
                mTxtTimeStamp.post(new Runnable() {
                    @Override
                    public void run() {
                        if (stream_time != null) {
//                            Log.i(TAG, "stream time is " + stream_time);
                            try {
                                mCurDateTime = DtvControl.DTV_DATE_FORMAT.parse(stream_time);
//                                Log.i(TAG, "time info " + mCurDateTime);
                                mCalendar.setTime(mCurDateTime);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }

                            mTxtTimeStamp.setText(mUiDateFormat.format(mCalendar.getTime()));
                            //mAlarmManager.setTime(mCalendar.getTimeInMillis()); // update system time
                        }
                    }
                });
            }
            else if(event.getEventName().equals(DtvControl.EVENT_DTV_CH_LOCK_UPDATE)) {
                Log.i(TAG, "Channel list is updating.");
            }
            else if(event.getEventName().equals(DtvControl.EVENT_DTV_CH_UNLOCK_UPDATE)) {
                Log.i(TAG, "Channel list updated.");

                updateChannelList();
            }
            else if(event.getEventName().equals(DtvControl.EVENT_DTV_SERVICE_UPDATE)) {
                Log.i(TAG, "Channel info updated.");

                updateChannelList();
            }
            else if(event.getEventName().equals(DtvControl.EVENT_DTV_VIDEO_CHANGE)) {
                CSplitBlob blob = event.getBlob(1);
                final String width = blob.getItem(0, 1);
                final String height = blob.getItem(0, 2);

                DtvActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // the setting maybe adjusts sharpness in video images
                        mSurfvHolder.setFixedSize(Integer.valueOf(width), Integer.valueOf(height));
                    }
                });
                Log.i(TAG, String.format("video size: %s X %s", width, height));

                String videoInfo = mCiplContainer.getVideoInfo();
                if(videoInfo != null && !videoInfo.isEmpty()) {
                    char escCh = 0x1a;
                    blob = new CSplitBlob(videoInfo, escCh);
                    Log.i(TAG, String.format("aspect radio: %s", blob.getItem(5, 0)));
                }
            }
            else if(event.getEventName().equals(DtvControl.EVENT_DTV_SIGNAL_CHANGED)){
                DtvActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTxtLoading.setVisibility(View.VISIBLE);//show loading
                    }
                });
            }
            else if(event.getEventName().equals(DtvControl.EVENT_CIPL_MPL_FIRSTVIDEOFRAME)){
                DtvActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTxtLoading.setVisibility(View.INVISIBLE);
                    }
                });
            }
        }
    }

    @Override
    public void onSubtitle(int[] buffer, int width, int height, int type, boolean display)
    {
        if (type != 1) {
            return;
        }
        if (display){
            drawSubtitle(buffer, width, height);
        }
        else {
            clearSubtitle();
        }

    }

    private void drawSubtitle(int[] buffer, int width, int height)
    {
        if (buffer.length == 0 || width == 0 || height == 0) {
            clearSubtitle();
            return;
        }
        final Bitmap bitmap = Bitmap.createBitmap(buffer, width, height, Bitmap.Config.ARGB_8888);
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                mWindowSubtitle.setImageBitmap(bitmap);
            }
        });
    }

    private void clearSubtitle()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                mWindowSubtitle.setImageBitmap(null);
            }
        });
    }

    /*USE KEY INPUT COMMAND BEGIN*/
    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        if(keyEvent.getAction() == KeyEvent.ACTION_UP) {
            int keyCode = keyEvent.getKeyCode();
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:

                    if (mMenuStatus == 0) {
                        int adj_Ch;
                        int channel = mCurrChannelIdx;
                        adj_Ch = (keyCode == KeyEvent.KEYCODE_DPAD_UP) ? 1 : -1;
                        channel += adj_Ch;
                        if (channel >= mArylChannelList.size())
                            channel = 0;
                        else if (channel < 0)
                            channel = mArylChannelList.size() - 1;

                        playChannelByIndex(channel);

                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (mMenuStatus == 0) {
                        int adj_Vol = (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
                        mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, adj_Vol, AudioManager.FLAG_SHOW_UI);
                    }
                    break;
                case KeyEvent.KEYCODE_SEARCH:
                    enterScan();
                    break;
                case KeyEvent.KEYCODE_NAVIGATE_PREVIOUS:
                    mViewScrollMenu.setVisibility(View.INVISIBLE);
                    mMenuStatus = 0;//menu is off
                    break;

                case KeyEvent.KEYCODE_MENU:
                    if (mMenuStatus == 0) {
                        mViewScrollMenu.setVisibility(View.VISIBLE);
                        updateSignalStatus(false, -1);
                        mMenuStatus = 1;//menu is open
                    } else {
                        mViewScrollMenu.setVisibility(View.INVISIBLE);
                        mMenuStatus = 0;//menu is off
                    }
                    return true;

                default:
                    break;
            }
        }
            return super.dispatchKeyEvent(keyEvent);

    }

    private boolean playChannelByChNo(String chNum) {
        int chIdx;

        for(chIdx = 0; chIdx < mArylChannelList.size(); chIdx++) {
            if(mArylChannelList.get(chIdx).getLcn().equals(chNum))
                break;
        }

        if(chIdx == mArylChannelList.size()) {
            Log.e(TAG, "Channel " + chNum + "not found!");
            return false;
        }

        return playChannelByIndex(chIdx);
    }


    public boolean playChannelByIndex(int idx){
        //Log.i(TAG, "Start to select channel(ID=" + idx + ")...");

        if(mCurrChannelIdx == idx){//no action on same channel
            return true;
        }

        updateSignalStatus(false, -1);
        mCiplContainer.stopCurrentChannel();

        if((mCurrChannelIdx != idx) && (mCiplContainer.getCurrentChannel() != null)) {
            if(mCiplContainer.stopCurrentChannel() == false) {
                Log.w(TAG, "stop Channel failed");
                return false;
            }
        }

        if (!mCiplContainer.selectChannel(idx)) {
            Log.w(TAG, "Select channel failed.");
            if(mCurrChannelIdx != idx)
            {
                if(!mCiplContainer.selectChannel(mCurrChannelIdx))
                {
                    if(mCurrChannelIdx != 0) {
                        mCurrChannelIdx = 0;
                        if (!mCiplContainer.selectChannel(mCurrChannelIdx)) {
                            Log.e(TAG, "Failed to play channel 0");
                            return false;
                        }
                    }
                    else {
                        Log.e(TAG, "Failed to play channel 0");
                        return false;
                    }
                }
            }
        }


        mSurfViewPlaybackWindow.setVisibility(View.INVISIBLE);
        mSurfViewPlaybackWindow.setVisibility(View.VISIBLE);

        updateSubSpinner();
        updateAudioSpinner();

        if(mPref.getInt(DtvControl.KEY_PREF_CHANNEL_IDX, CH_IDX_DEFAULT) == idx) {
            setSavedSettings();
        } else {
            resetSettings();
        }

        mCurrChannelIdx = idx;

        showChannelBar(DELAY);

        if (!mCiplContainer.playCurrentChannel()) {
            Log.i(TAG, "Channel play failed.");
            return false;
        }
        if (!mCiplContainer.setVideoOn(true)) {
            Log.i(TAG, "Turn video on failed.");
            return false;
        }

        mPref.edit().putInt(DtvControl.KEY_PREF_CHANNEL_IDX, mCurrChannelIdx).apply();
        mPref.edit().putString(DtvControl.KEY_PREF_CHANNEL_NAME, mCiplContainer.getChannel(mCurrChannelIdx).getName()).apply();


        return true;
    }
    public void resetSettings(){
        mSpinnerAudio.setSelection(AUD_DEFAULT);
        mCiplContainer.setAudioTrack(AUD_DEFAULT);//set to subtitle track 0 for default
        mSpinnerSubtitle.setSelection(SUB_DEFAULT);
        mCiplContainer.setSubtitleTrack(SUB_DEFAULT-1);//set to subtitle track -1 for default

        mPref.edit().putInt(DtvControl.KEY_PREF_SAVE_AUD_IDX, AUD_DEFAULT).apply();
        mPref.edit().putInt(DtvControl.KEY_PREF_SAVE_SUB_IDX, SUB_DEFAULT).apply();
    }

    public void setSavedSettings(){
        mLastAudOption=mPref.getInt(DtvControl.KEY_PREF_SAVE_AUD_IDX, AUD_DEFAULT);
        mLastSubOption=mPref.getInt(DtvControl.KEY_PREF_SAVE_SUB_IDX, SUB_DEFAULT);

        mSpinnerAudio.setSelection(mLastAudOption);
        mCiplContainer.setAudioTrack(mLastAudOption);//set to subtitle track saved last time
        mSpinnerSubtitle.setSelection(mLastSubOption);
        mCiplContainer.setSubtitleTrack(mLastSubOption-1);//set to subtitle track saved last time
    }

    public void updateAudioSpinner(){

        ArrayList<String> audioList = new ArrayList<String>();
        String[] arrAudio = mCiplContainer.showAudioTrack();
        for (int i = 0; i < ((arrAudio==null)?0:arrAudio.length); i++)
        {
            audioList.add(arrAudio[i]);
        }

        mAdptAudio = new ArrayAdapter<String>(this, R.layout.listitem_file,R.id.txtv_file_list_item, audioList);
        mSpinnerAudio.setAdapter(mAdptAudio);


        mSpinnerAudio.post(new Runnable() {
            @Override
            public void run() {
                mSpinnerAudio.setOnItemSelectedListener(mSpinnerAudioListener);
            }
        });

    }

    public void updateSubSpinner(){
        clearSubtitle();

        mSpinnerSubtitle.setOnItemSelectedListener(null);
        mSubtitleList.clear();

        mArrSubtitle= mCiplContainer.showSubtitleTrack();
        for (int i = 0; i <= ((mArrSubtitle==null)?0:mArrSubtitle.length); i++)
        {
            if (i == 0)
                mSubtitleList.add(mOffStr);
            else
                mSubtitleList.add(mArrSubtitle[i-1]);
        }
        mAdptSubtitle.notifyDataSetChanged();

        mSpinnerSubtitle.post(new Runnable() {
            @Override
            public void run() {
                mSpinnerSubtitle.setOnItemSelectedListener(mSpinnerSubtitleListener);
            }
        });


    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
//            Log.i(TAG, "Receive intent = " + intent);
            if (intent.getAction() == null)
                return;
            switch (intent.getAction()) {
                case DtvControl.EVENT_ENTER_SCAN_REQUEST:
                    Log.i(TAG, "Receive EVENT_ENTER_SCAN_REQUEST");
                    enterScan();
                    break;
                case Utils.EVENT_CHANNEL:
                    String channel = intent.getStringExtra(Utils.MSG_CHANNEL_NUM);

                    mHandler.removeMessages(MSG_CH_NUM_CLOSE);
                    mTxtOsdString.setText(channel);
                    mTxtOsdString.setVisibility(View.VISIBLE);

                    Message msg = mHandler.obtainMessage(MSG_SWITCH_CHANNEL);
                    Bundle bundle = new Bundle();
                    bundle.putString(KEY_CHANNEL_NUM, channel);
                    msg.setData(bundle);
                    mHandler.sendMessageDelayed(msg, 100);

                    mHandler.sendEmptyMessageDelayed(MSG_CH_NUM_CLOSE, 5000);


                    break;
                case Utils.EVENT_OSD_STRING:
                    String channel_osd = intent.getStringExtra(Utils.MSG_OSD_STRING);

//                    mHandler.removeMessages(MSG_CH_NUM_CLOSE);
                    mTxtOsdString.setText(channel_osd);
                    mTxtOsdString.setVisibility(View.VISIBLE);


                    break;
            }
        }
    };
}
