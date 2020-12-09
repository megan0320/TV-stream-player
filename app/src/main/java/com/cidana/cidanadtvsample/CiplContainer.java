package com.cidana.cidanadtvsample;

/**
 * @author Wyvern
 * @since 2015/5/20
 */

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Rect;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

import com.cidana.cipl.CSplitBlob;
import com.cidana.cipl.Cipl;
import com.cidana.cipl.CiplCallback;
import com.cidana.cipl.CiplError;
import com.cidana.cipl.CiplSession;
import com.cidana.usbtuner.Bridge;

import com.asuka.dtvplayer.R;
import com.asuka.dtvplayer.DtvControl;

/**
 * class of CiplContainer
 */
public class CiplContainer implements CiplCallback {
    private static final String TAG = "CiplContainer";

    private static CiplContainer mInstance;
    private static Context mContext;
    private static Cipl mCipl;
    private static CiplSession mCiplSession;
    private static String mSessionName = "DtvSession";
    private static char mEscCh = 0x1a;

    private static ArrayList<Tuner> mTunerList = new ArrayList<Tuner>();
    private static Tuner mCurrentTuner;
    private static Tuner mTunerActivating;

    private static ArrayList<Channel> mChannelList = new ArrayList<Channel>();
    private static Channel mCurrentChannel;
    /*
    final int tunerIdx_ATSC=0;
    final int tunerIdx_J83B=1;
     */
    /**
     * Enumerates of Tuner Types
     */
    public static final int TUNER_TYPE_ERROR    = -1,
                            TUNER_TYPE_DVBT2    = 0,
                            TUNER_TYPE_DVBT     = 1,
                            TUNER_TYPE_ISDBT    = 2,
                            TUNER_TYPE_ATSC     = 3,
                            TUNER_TYPE_DTMB     = 4,
                            TUNER_TYPE_J83B     = 5;

    /**
     * get instance of current container
     * @param context application context registered into CiplContainer
     * @return instance of current CiplContainer
     */
    public static CiplContainer getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new CiplContainer();
            if (mInstance == null) {
                return null;
            }
        }
        if (mCipl == null) {
            if (context == null) {
                return null;
            }
            mContext = context;
            makeAssetsCopies();
            if (!mInstance.initCiplSdk(context.getApplicationInfo().nativeLibraryDir+"/")) {
                return null;
            }
        }

        return mInstance;
    }

    /**
     * get instance of current container
     * @return instance of current CiplContainer
     */
    public static CiplContainer getInstance() {
        return mInstance;
    }

    private static boolean initCiplSdk(String appLibDir) {
        Log.i(TAG, "-> initCiplSdk(appLibDir=\""+appLibDir+"\")");
        String errString;
        try {
            if (mCipl == null) {
                mCipl = new Cipl();

                CiplError hr;
                if ((hr = mCipl.loadLibrary(appLibDir)).failed()) {
                    errString = "Cipl.loadLibrary(\""+appLibDir+"\") failed. err="+hr.errname();
                    throw new Exception(errString);
                }
                if ((hr = mCipl.createManager()).failed()) {
                    errString = "Cipl.createManager() failed. err="+hr.errname();
                    throw new Exception(errString);
                }
                if ((hr = mCipl.openManager(0, mEscCh)).failed()) {
                    errString = "Cipl.openManager(0, "+mEscCh+") failed. err="+hr.errname();
                    throw new Exception(errString);
                }
                if ((hr = mCipl.setEventCallback(mInstance)).failed()) {
                    errString = "Cipl.setEventCallback() failed. err="+hr.errname();
                    throw new Exception(errString);
                }
                mCiplSession = new CiplSession(mSessionName, mCipl, mEscCh);
                if ((hr = mCipl.createSession(mSessionName, 0)).failed()) {
                    errString = "Cipl.createSession(\""+mSessionName+"\", 0) failed. err="+hr.errname();
                    throw new Exception(errString);
                }
                mCiplSession.setCachePath(mContext.getFilesDir().getAbsolutePath());

                CSplitBlob licenseInfo = mCiplSession.show("license");
                if (licenseInfo != null) {
                    Log.v(TAG, "Cipl SDK license information: ");
                    //MainActivity.displayLog(mContext, "Cipl SDK license information: ");
                    int rowCnt = licenseInfo.getNumberCols(0);
                    for (int i = 0; i < rowCnt; i++) {
                        String info = licenseInfo.getItem(0, i);
                        Log.v(TAG, info);
                        //MainActivity.displayLog(mContext, info);
                    }
                    if (rowCnt > 4) {
                        String[] strAarray = licenseInfo.getItem(0, 4).split(":");
                        String expirationTime = strAarray[1];
                        String info = "Expiration time: "+expirationTime;
                        Log.i(TAG, info);
                        //MainActivity.displayLog(mContext, info);
                    }
                }
                Log.i(TAG, "<- initCiplSdk() succeeded.");
                //MainActivity.displayLog(mContext, "Cipl SDK initialization succeeded.");
            } else {
                Log.i(TAG, "<- initCiplSdk() ignored, CiplSdk already initialized.");
            }
            return true;
        }
        catch (Exception e){
            Log.e(TAG, e.getMessage());
            if (mCipl != null) {
                mCipl.closeManager();
                mCipl.destroyManager();
                mCipl.freeLibrary();
                mCipl = null;
            }
            mCiplSession = null;
            Log.e(TAG, "<- initCiplSdk() failed!");
            return false;
        }
    }

    /**
     * close and destroy SDK
     */
    public static void deinitCipl() {
        if (mCurrentTuner != null) {
            deactivateCurrentTuner();
            mCurrentTuner = null;
        }
        if (mCipl != null) {
            mCipl.closeManager();
            mCipl.destroyManager();
            mCipl.freeLibrary();
            mCipl = null;
        }
        boolean isCurrentTunerUsb = (mCurrentTuner != null) && (mCurrentTuner.getName().equals("Cidana USB Tuner"));
        if (isCurrentTunerUsb) {
            Bridge.getBridge().DeInit();
        }
        mCiplSession = null;
        mTunerActivating = null;
        mCurrentChannel = null;
    }

    private static void makeAssetsCopies() {
        if (mContext == null) {
            return;
        }

        AssetManager am = mContext.getAssets();
        InputStream is = null;
        OutputStream os = null;
        String fontDirectoryPath = mContext.getFilesDir().getAbsolutePath()+"/font/";
        File fontDirectory = new File(fontDirectoryPath);
        fontDirectory.mkdirs();
        if (fontDirectory.isDirectory()) {
            Log.i(TAG, "Create font directory success.");
        }
        else {
            Log.i(TAG, "Create font directory failed.");
        }
        try {
            String[] assetsList = am.list("font");
            Log.i(TAG, "Font list ["+assetsList.length+"]:");
            byte[] readBuffer = new byte[2048];
            for (String assetName : assetsList) {
                Log.i(TAG, "\t" + assetName);
                is = am.open("font/"+assetName);
                File outputFile = new File(fontDirectoryPath, assetName);
                if (!outputFile.exists()) {
                os = new FileOutputStream(outputFile);
                    int readLength;
                    while ((readLength = is.read(readBuffer)) > 0) {
                        os.write(readBuffer, 0, readLength);
                    }
                    os.close();
                }
                is.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * get tuner list
     * @return list of tuner in array
     */
    public ArrayList<Tuner> getTunerList() {
        if (mTunerList.size() == 0) {
            updateTunerList();
        }
        return mTunerList;
    }

    /**
     * update and refresh tuner list
     * @return count of tuners
     */


    public int updateTunerList() {
        if (mCipl == null) {
            Log.e(TAG, "updateTunerList() cannot proceed: Cipl SDK is not initialized.");
            return -1;
        }

        Log.v(TAG, "Updating tuner list:");
        String[] astrRes = mCipl.execute(null, "show device");
        String strDeviceName;
        String strDeviceType;
        if (astrRes.length > 1 && astrRes[1] != null && !astrRes[1].equals("")) {
            mTunerList.clear();
            CSplitBlob blob = new CSplitBlob(astrRes[1], mEscCh);

            /*only show ATSC and J83B option begin*/

            /*
            strDeviceName = blob.getItem(0, 1);
            strDeviceType = blob.getItem(0, 4);
            //Log.v(TAG, "  Tuner: name-\""+strDeviceName+"\", index-"+tunerIdx_ATSC+", type-"+strDeviceType);
            mTunerList.add(new Tuner().setName(strDeviceName)
                    .setDeviceIndex(0).setDeviceType(strDeviceType));

            strDeviceName = blob.getItem(1, 1);
            strDeviceType = blob.getItem(1, 4);
            //Log.v(TAG, "  Tuner: name-\""+strDeviceName+"\", index-"+tunerIdx_J83B+", type-"+strDeviceType);
            mTunerList.add(new Tuner().setName(strDeviceName)
                    .setDeviceIndex(1).setDeviceType(strDeviceType));


            strDeviceName = blob.getItem(2, 1);
            strDeviceType = blob.getItem(2, 4);
            //Log.v(TAG, "  Tuner: name-\""+strDeviceName+"\", index-"+tunerIdx_J83B+", type-"+strDeviceType);
            mTunerList.add(new Tuner().setName(strDeviceName)
                    .setDeviceIndex(2).setDeviceType(strDeviceType));
            strDeviceName = blob.getItem(3, 1);
            strDeviceType = blob.getItem(3, 4);
            //Log.v(TAG, "  Tuner: name-\""+strDeviceName+"\", index-"+tunerIdx_J83B+", type-"+strDeviceType);
            mTunerList.add(new Tuner().setName(strDeviceName)
                    .setDeviceIndex(3).setDeviceType(strDeviceType));
            strDeviceName = blob.getItem(4, 1);
            strDeviceType = blob.getItem(4, 4);
            //Log.v(TAG, "  Tuner: name-\""+strDeviceName+"\", index-"+tunerIdx_J83B+", type-"+strDeviceType);
            mTunerList.add(new Tuner().setName(strDeviceName)
                    .setDeviceIndex(4).setDeviceType(strDeviceType));



             */
            /*only show ATSC and J83B option end*/

            //show all tuner option
            //int iRowCnt = blob.getNumberRows();

            for (int i = 0; i < 13; i++) {
                strDeviceName = blob.getItem(i, 1);
                strDeviceType = blob.getItem(i, 4);
                Log.v(TAG, "  Tuner: name-\""+strDeviceName+"\", index-"+i+", type-"+strDeviceType);
                mTunerList.add(new Tuner().setName(strDeviceName)
                        .setDeviceIndex(i).setDeviceType(strDeviceType));
            }
        }
        Log.v(TAG, "Totally " + mTunerList.size() + " tuners are found.");
        return mTunerList.size();
    }

    /**
     * get tuner device by index
     * @param iDeviceIndex index of tuner device
     * @return instance of tuner device
     */
    public Tuner getTuner(int iDeviceIndex) {
        for (Tuner tuner : mTunerList) {
            if (tuner.getDeviceIndex() == iDeviceIndex)
                return tuner;
        }
        Log.e(TAG, "getTuner() failed, requested device index(" + iDeviceIndex + ") doesn't exist.");
        return null;
    }

    /**
     * get current activated tuner device
     * @return instance of tuner device
     */
    public static Tuner getCurrentTuner() {
        return mCurrentTuner;
    }

    /**
     * activate tuner device by index
     * @param iDeviceIndex index of tuner device to activate
     * @return 1 -- success, others are in error
     */
    public int activateTuner(int iDeviceIndex) {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "activateTuner() cannot proceed: Cipl SDK is not initialized.");
            return -1;
        }
        Tuner tuner = getTuner(iDeviceIndex);
        if (tuner == null) {
            Log.e(TAG, "activateTuner() failed: deviceIndex=" + iDeviceIndex + " is invalid.");
            return -1;
        }
        CiplError err;
        if (mCurrentTuner != null && !mCurrentTuner.equals(tuner) && mCurrentTuner.isActivated()) {
            if (deactivateCurrentTuner() < 0) {
                Log.e(TAG, "activateTuner() failed: failed to deactivate current tuner.");
                return -1;
            }
        }
        if (mCurrentTuner == null || !mCurrentTuner.equals(tuner) || !mCurrentTuner.isActivated()) {
            Log.i(TAG, "Activate tuner: " + tuner.toString() + ".");
            mTunerActivating = tuner;
            if (!usbTunerOperation(tuner)) {
                Log.e(TAG, "activateTuner() failed: usbTunerOperation() failed.");
                return -1;
            }
            if ((err = mCiplSession.openSource("device", String.valueOf(tuner.getDeviceIndex()))).failed()) {
                Log.e(TAG, "Cannot open source: " + err.errname() + ", source : device.");
                return -1;
            }
            mCiplSession.waitFor("device");
            mCiplSession.setCachePath(mContext.getFilesDir().getAbsolutePath());
            mCiplSession.setString("languagecode", mContext.getResources().getConfiguration().locale.getISO3Language());
            mCiplSession.setString("subtitleoutput", "argb8888");
            mCiplSession.setString("subtitlefontpath", mContext.getFilesDir().getAbsolutePath()+"/font/");
        }
        return 1;
    }

    /**
     * activate current selected tuner device
     * @return 0 -- success, others are in error
     */
    public static int activateCurrentTuner() {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "activateCurrentTuner() cannot proceed: Cipl SDK is not initialized.");
            return -1;
        }
        if (mCurrentTuner == null) {
            Log.e(TAG, "activateCurrentTuner() failed, no tuner is selected.");
            return -1;
        }
        if (mCurrentTuner.isActivated()) {
            Log.i(TAG, "activateCurrentTuner() ignored, current tuner is already activated.");
            return 0;
        }

        CiplError err;
        if ((err = mCiplSession.openSource("device", String.valueOf(mCurrentTuner.getDeviceIndex()))).failed()) {
            Log.e(TAG, "Cannot open source: " + err.errname() + ", source : device.");
            return -1;
        }
        mCiplSession.waitFor("device");
        mCiplSession.setCachePath(mContext.getFilesDir().getAbsolutePath());
        mCiplSession.setString("languagecode", mContext.getResources().getConfiguration().locale.getISO3Language());
        mCiplSession.setString("subtitleoutput", "argb8888");
        mCurrentTuner.activate();
        return 0;
    }

    /**
     * deactivate current selected tuner device
     * @return 0 -- success, others are in error
     */
    public static int deactivateCurrentTuner() {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "deactivateCurrentTuner() cannot proceed: Cipl SDK is not initialized.");
            return -1;
        }
        if (mCurrentTuner == null) {
            Log.i(TAG, "deactivateCurrentTuner() ignored, no tuner is selected.");
            return 0;
        }
        if (!mCurrentTuner.isActivated()) {
            Log.i(TAG, "deactivateCurrentTuner() ignored, current tuner is already deactivated.");
            return 0;
        }
        CiplError err;
        Log.i(TAG, "Deactivate tuner: " + mCurrentTuner.toString() + ".");
        if ((err = mCiplSession.closeSource()).failed()) {
            Log.e(TAG, "closeSource() failed: "+err.errname()+".");
            return -1;
        }
        boolean isCurrentTunerUsb = (mCurrentTuner != null) && (mCurrentTuner.getName().equals("Cidana USB Tuner"));
        if (isCurrentTunerUsb) {
            Bridge.getBridge().DeInit();
        }
        mCurrentTuner.deactivate();
        return 0;
    }

    protected boolean usbTunerOperation(Tuner tuner) {
        if (tuner == null) {
            return true;
        }
        boolean isCurrentTunerUsb = (mCurrentTuner != null) && (mCurrentTuner.getName().equals("Cidana USB Tuner"));
        boolean isNextTunerUsb = tuner.getName().equals("Cidana USB Tuner");
        if ((!isCurrentTunerUsb && isNextTunerUsb) || (isCurrentTunerUsb && !mCurrentTuner.isActivated())) {
            UsbManager um = (UsbManager)mContext.getSystemService(Context.USB_SERVICE);
            String device_type = tuner.getDeviceType();
            int tuner_type = convertTunerType(device_type);

            if (!Bridge.getBridge().Init(um, mContext, R.xml.device_filter, tuner_type)) {
                Log.e(TAG, "USB tuner init failed: Bridge.Init() failed.");
                return false;
            }
            int iRet;
            if ((iRet = Bridge.getBridge().CheckDevice(um)) < 0) {
                Log.e(TAG, "USB tuner init failed: Bridge.CheckDevice() failed, ret="+iRet+".");
                Bridge.getBridge().DeInit();
                return false;
            }
        }
        else if (isCurrentTunerUsb && !isNextTunerUsb) {
            Bridge.getBridge().DeInit();
        }
        return true;
    }

    /**
     * start channel scan progress
     * @param iStartFreq start frequency in KHz
     * @param iEndFreq end frequency in KHz
     * @param iBandWidth bandwidth in KHz
     * @param bMergeResult flag to merge result in history
     */
    public void startScan(int iStartFreq, int iEndFreq, int iBandWidth, boolean bMergeResult) {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "startScan() cannot proceed: Cipl SDK is not initialized.");
            return;
        }
        if (mCurrentTuner == null) {
            Log.e(TAG, "startScan() cannot proceed: No tuner is selected.");
            return;
        }
        if (!mCurrentTuner.isActivated()) {
            activateCurrentTuner();
        }

        //MainActivity.displayLog(mContext, "Start scan: " + iStartFreq + " " + iEndFreq + " " + iBandWidth + ", Merge result: " + bMergeResult);
        if(bMergeResult)
            mCiplSession.execute("startrescan " + iStartFreq + " " + iEndFreq + " " + iBandWidth);
        else
            mCiplSession.execute("startscan " + iStartFreq + " " + iEndFreq + " " + iBandWidth);
    }

    /**
     * start channel scan progress in file simulation
     * @param filePath input file path
     */
    /*
    public void startScan(String filePath) {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "startScan() cannot proceed: Cipl SDK is not initialized.");
            return;
        }
        if (mCurrentTuner == null) {
            Log.e(TAG, "startScan() cannot proceed: No tuner is selected.");
            return;
        }
        if (!mCurrentTuner.getName().equals("Cidana File Tuner")) {
            Log.e(TAG, "startScan() cannot proceed: scan file can only applied with file tuner.");
            return;
        }
        if (!mCurrentTuner.isActivated()) {
            activateCurrentTuner();
        }

        //MainActivity.displayLog(mContext, "Start scan file: \"" + filePath + "\"");
        mCiplSession.execute("startscan file \"" + filePath + "\"");
    }
    */

    /**
     * stop current channel scan progress
     */
    public void stopScan() {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "stopScan() cannot proceed: Cipl SDK is not initialized.");
            return;
        }
        //MainActivity.displayLog(mContext, "Try to stop channel scan...");
        mCiplSession.stopScan();
    }

    /**
     * save results of channel scanning to local cache
     * @param cachePath path of scanning cache
     * @return true--success, false--erred
     */
    public boolean saveScan(String cachePath) {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "saveScan() cannot proceed: Cipl SDK is not initialized.");
            return false;
        }

        Log.i(TAG, "storeScan(), setCachePath = \""+cachePath+"\".");
        mCiplSession.setCachePath(cachePath);
        CiplError err;
        if ((err = mCiplSession.storeScan()).failed()) {
            Log.e(TAG, "storeScan() failed: " + err.errname() + ".");
            return false;
        }
        Log.i(TAG, "storeScan() succeeded, save path is \"" + cachePath + "\".");
        return true;
    }

    /**
     * load pre-saved cache of channel scanning
     * @param cachePath path of scanning cache
     * @return true--success, false--erred
     */
    public boolean loadScan(String cachePath) {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "loadScan() cannot proceed: Cipl SDK is not initialized.");
            return false;
        }

        Log.i(TAG, "restoreScan(), setCachePath = \""+cachePath+"\".");
        mCiplSession.setCachePath(cachePath);
        CiplError err;
        if ((err = mCiplSession.restoreScan()).failed()) {
            Log.e(TAG, "restoreScan() failed: "+err.errname()+".");
            return false;
        }
        Log.i(TAG, "restoreScan() succeeded, load path is \"" + cachePath + "\".");
        return true;
    }

    /**
     * update channel list
     * @return count of channels in list, negative is erred
     */
    public int updateChannelList() {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "updateChannelList() cannot proceed: Cipl SDK is not initialized.");
            return -1;
        }

        //Log.i(TAG, "Updating channel list:");
        mChannelList.clear();
        CSplitBlob channelData = mCiplSession.show("service");
        if (channelData == null) {
            Log.i(TAG, "no channel data");
            return 0;
        }
        int channelCnt = channelData.getNumberRows();
        for (int i = 0; i < channelCnt; i++) {
            Channel channel = new Channel(i).
                    setName(channelData.getItem(i, 1)).
                    setConstInfo(channelData.getItem(i, 4)).
                    setLcn(channelData.getItem(i, 7));
            //Log.d(TAG, "  Channel: "+channel);
            mChannelList.add(channel);
        }
        Log.i(TAG, "Totally " + channelCnt + " channels are found.");
        return mChannelList.size();
    }

    /**
     * get list of channel information
     * @return list of channel
     */
    public ArrayList<Channel> getChannelList() {
        if (mChannelList.size() == 0) {
            updateChannelList();
        }
        return mChannelList;
    }

    /**
     * get channel information by index
     * @param channelId index of channel
     * @return instance of channel information
     */
    public Channel getChannel(int channelId) {
        for (Channel channel : mChannelList) {
            if (channel.getChId() == channelId)
                return channel;
        }
        return null;
    }

    /**
     * select channel by channel ID
     * @param channelId ID of channel
     * @return true--success, false--erred
     */
    public boolean selectChannel(int channelId) {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "selectChannel() cannot proceed: Cipl SDK is not initialized.");
            return false;
        }
        Channel channel = getChannel(channelId);
        if (channel == null) {
            Log.e(TAG, "selectChannel() cannot proceed: No channel has ID=" + channelId + ".");
            //MainActivity.displayLog(mContext, "Select channel(ID="+channelId+") failed. No such channel.");
            return false;
        }

        CiplError error;
        Log.i(TAG, "Select service: channel id="+channelId+".");
        if ((error = mCiplSession.select("service", channelId)).failed()) {
            Log.e(TAG, "selectChannel() failed. error="+error+".");
            //MainActivity.displayLog(mContext, "Select channel(ID=" + channelId + ") failed. error="+error+".");
            mCurrentChannel = null;
            return false;
        }
        mCurrentChannel = channel;
        return true;
    }

    /**
     * get information of current channel
     * @return instance of channel information
     */
    public static Channel getCurrentChannel() {
        return mCurrentChannel;
    }

    /**
     * set video window for display
     * @param videoWindow instance of video window surface
     * @return true--success, false--erred
     */
    public boolean setVideoWindow(Surface videoWindow) {
//        Log.i(TAG, "Set setVideoWindow.");
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "setVideoWindow() cannot proceed: Cipl SDK is not initialized.");
            return false;
        }

        CiplError error;
        if ((error = mCiplSession.setSurface(videoWindow)).failed()) {
            Log.e(TAG, "Set video window failed, error="+error+".");
            return false;
        }
//        Log.i(TAG, "Set setVideoWindow.");
        return true;
    }

    /**
     * set video display rectangle size
     * @param dstRect rectangle of video display window
     * @return true--success, false--erred
     */
    public boolean setVideoDestRect(Rect dstRect){

        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "setVideoDestRect() cannot proceed: Cipl SDK is not initialized.");
            return false;
        }

        CiplError error;
        if ((error = mCiplSession.setString("destrect",
                dstRect.left+" "+dstRect.top+" "+dstRect.right+" "+dstRect.bottom)).failed()) {
            Log.e(TAG, "Set video window rect("+dstRect.toString()+") failed, error="+error+".");
            //return false;  // wyvern: set destrect will probably fail in android system, but this will not affect normal playback
        }
        return true;
    }

    /**
     * set video display on/off
     * @param on flag of switch video display: true--on, false--off
     * @return true--success, false--erred
     */
    public boolean setVideoOn(Boolean on) {
        CiplError error;
        if ((error = mCiplSession.setValue("video", on?1:0)).failed()) {
            Log.e(TAG, "Set video on failed, error="+error+".");
            return false;
        }

        return true;
    }

    /**
     * remove video window
     * @return true--success, false--erred
     */
    public boolean removeVideoWindow() {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "removeVideoWindow() cannot proceed: Cipl SDK is not initialized.");
            return false;
        }

        CiplError error;
        if ((error = mCiplSession.setValue("video", 0)).failed()) {
            Log.e(TAG, "Set video off failed, error="+error+".");
            return false;
        }
        return true;
    }

    /**
     * play current channel
     * @return true--success, false--erred
     */
    public boolean playCurrentChannel() {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "playCurrentChannel() cannot proceed: Cipl SDK is not initialized.");
            return false;
        }
        if (mCurrentChannel == null) {
            Log.e(TAG, "playCurrentChannel() cannot proceed: No channel has been selected.");
            return false;
        }

        CiplError error;
//        Log.i(TAG, "Execute CiplSession.play().");
        if ((error = mCiplSession.play()).failed()) {
            Log.e(TAG, "Play current selected channel failed, error="+error+".");
            return false;
        }
        return true;
    }

    /**
     * stop current channel
     * @return true--success, false--erred
     */
    public boolean stopCurrentChannel() {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "stopCurrentChannel() cannot proceed: Cipl SDK is not initialized.");
            return false;
        }
        if (mCurrentChannel == null) {
            Log.i(TAG, "Stop current channel ignored, no current channel is selected.");
            return true;
        }

        CiplError error;
        Log.i(TAG, "Execute CiplSession.stop().");
        if ((error = mCiplSession.stop()).failed()) {
            Log.e(TAG, "Stop current selected channel failed, error="+error+".");
            return false;
        }
        return true;
    }

    /**
     * set video decoder mode
     * @param decoderMode mode of video decoder: </br>
     *                    0--software decoder
     *                    1--hardware decoder (media codec, for android sdk version >= 5.0)
     *                    2--hardware decoder (OMX, only available android sdk version < 5.0)
     * @return true--success, false--erred
     */
    public boolean setVideoDecoderMode(int decoderMode) {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "setVideoDecoderMode() cannot proceed: Cipl SDK is not initialized.");
            return false;
        }

        CiplError error;
        Log.i(TAG, "Set video decoder mode="+decoderMode+".");
        if ((error = mCiplSession.setValue("videocodecmode", decoderMode)).failed()) {
            Log.e(TAG, "Set video decoder mode="+decoderMode+" failed, error="+error+".");
            return false;
        }
        return true;
    }

    /**
     * select subtitle track
     * @param index index of subtitle track
     * @return true--success, false--erred
     */
    public boolean setSubtitleTrack(int index)
    {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "setSubtitleTrack() cannot proceed: Cipl SDK is not initialized.");
            return false;
        }

        if (index < 0)
            index = -1;

        CiplError error;
        Log.i(TAG, "Set subtitle track index="+index+".");
        if ((error = mCiplSession.select("subtitle", index)).failed()) {
            Log.e(TAG, "Set subtitle track index="+index+" failed, error="+error+".");
            return false;
        }
        if ((error = mCiplSession.setValue("subtitle", (index < 0)?0:1)).failed()) {
            Log.e(TAG, "Set subtitle display="+(index < 0)+" failed, error="+error+".");
            return false;
        }

        return true;
    }

    /**
     * list subtitle tracks
     * @return string array of subtitle track information
     */
    public String[] showSubtitleTrack()
    {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "showSubtitleTrack() cannot proceed: Cipl SDK is not initialized.");
            return null;
        }

        int nSubCount = 0;
        CSplitBlob sb = mCiplSession.show("subtitle");
        if (sb == null)
            return null;
        nSubCount = sb.getNumberRows();
        Log.i(TAG, "subtitle track count: " + nSubCount);

        if (nSubCount > 0)
        {
            String[] arrSubtitle = new String[nSubCount];
            for(int i = 0; i < nSubCount; i ++)
            {
                arrSubtitle[i] = sb.getItem(i, 0);
                Log.i(TAG, "found subtitle:" + i + "   " + arrSubtitle[i]);
            }
            return arrSubtitle;
        }
        return null;
    }

    /**
     * get current subtitle
     * @return index of current subtitle
     */
    public int getCurrentSubtitle()
    {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "getCurrentSubtitle() cannot proceed: Cipl SDK is not initialized.");
            return -1;
        }
        int currIndex = mCiplSession.getValue("csubtitle");
        Log.i(TAG, "current subtitle index:" + currIndex);
        return currIndex;
    }

    /**
     * select audio track
     * @param index index of audio track
     * @return true--success, false--erred
     */
    public boolean setAudioTrack(int index)
    {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "setAudioTrack() cannot proceed: Cipl SDK is not initialized.");
            return false;
        }

        if (index < 0)
            index = -1;

        CiplError error;
        Log.i(TAG, "Set audio track index="+index+".");
        if ((error = mCiplSession.select("audio", index)).failed()) {
            Log.e(TAG, "Set audio track index="+index+" failed, error="+error+".");
            return false;
        }

        return true;
    }

    /**
     * list audio tracks
     * @return string array of audio track information
     */
    public String[] showAudioTrack()
    {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "showAudioTrack() cannot proceed: Cipl SDK is not initialized.");
            return null;
        }

        int nAudioCount = 0;
        CSplitBlob sb = mCiplSession.show("audio");
        if (sb == null)
            return null;
        nAudioCount = sb.getNumberRows();
        Log.i(TAG, "audio track count: " + nAudioCount);

        if (nAudioCount > 0)
        {
            String[] arrAudio = new String[nAudioCount];
            for(int i = 0; i < nAudioCount; i ++)
            {
                arrAudio[i] = sb.getItem(i, 0);
                Log.i(TAG, "found audio:" + i + "   " + arrAudio[i]);
            }
            return arrAudio;
        }
        return null;
    }

    /**
     * get current audio track
     * @return index of current audio track
     */
    public int getCurrentAudio()
    {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "getCurrentAudio() cannot proceed: Cipl SDK is not initialized.");
            return -1;
        }
        int currIndex = mCiplSession.getValue("caudio");
        Log.i(TAG, "current audio track index:" + currIndex);
        return currIndex;
    }


    /**
     * listener interface of notification events from container
     */
    public interface ciplEventListener {
        void onEvent(CiplEvent event);
    }
    ArrayList<ciplEventListener> mEventListeners = new ArrayList<ciplEventListener>();

    /**
     * add notification events listener
     * @param listener events listener
     */
    public void addEventListener(ciplEventListener listener) {
        if (!mEventListeners.contains(listener)) {
            mEventListeners.add(listener);
        }
    }

    /**
     * remove notification events listener
     * @param listener events listener
     */
    public void removeEventListener(ciplEventListener listener) {
        if (mEventListeners.contains(listener)) {
            mEventListeners.remove(listener);
        }
    }

    /**
     * get program information list
     * @return list of program information in array
     */
    public ArrayList<EventInfo> getEpgEventList() {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "getEpgEventList() cannot proceed: Cipl SDK is not initialized.");
            return null;
        }
        if (mCurrentChannel == null) {
            Log.e(TAG, "getEpgEventList() cannot proceed: no current channel is selected.");
            return null;
        }

        CSplitBlob epgBlob;
        if ((epgBlob = mCiplSession.show("program "+mCurrentChannel.getChId())) == null) {
            Log.e(TAG, "getEpgEventList() failed: CiplSession returns zero epg data.");
            return null;
        }

        int iChannelId = mCurrentChannel.getChId();
        int iRows = epgBlob.getNumberRows();
        Log.i(TAG, "getEpgEventList(): "+iRows+" events returned.");
        ArrayList<EventInfo> eventList = new ArrayList<EventInfo>();
        for (int i = 0; i < iRows; i++) {
            EventInfo eventInfo = new EventInfo();
            eventInfo.setChannelId(iChannelId);
            // 0 -- name
            String data = epgBlob.getItem(i, 0);
            if (data != null) {
                eventInfo.setName(data);
            }
            // 1 -- description
            data = epgBlob.getItem(i, 1);
            if (data != null) {
                eventInfo.setDescription(data);
            }
            // 2 -- start time
            data = epgBlob.getItem(i, 2);
            if (data != null) {
                eventInfo.setStartTime(data);
            }
            // 3 -- end time
            data = epgBlob.getItem(i, 3);
            if (data != null) {
                eventInfo.setEndTime(data);
            }
            // 4 -- condition access
            // 5 -- genre
            // 6 -- rating
            data = epgBlob.getItem(i, 6);
            if (data != null && data.length() > 0) {
                eventInfo.setRating(data);
            }
//            Log.i(TAG, "event: name=\""+eventInfo.getName()+"\", description=\""+eventInfo.getDescription()+
//                "\", start=\""+eventInfo.getStartTime()+"\", end=\""+eventInfo.getEndTime()+"\", rating=\""+
//                eventInfo.getRating()+"\".");
            eventList.add(eventInfo);
        }
        return eventList;
    }

    /**
     * start to record to local file
     * @param savePath path of recorded local file
     * @return true--success, false--erred
     */
    public boolean startRecord(String savePath) {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "startRecord() cannot proceed: Cipl SDK is not initialized.");
            return false;
        }
        if (mCurrentChannel == null) {
            Log.e(TAG, "startRecord() cannot proceed: no current channel is selected.");
            return false;
        }

        CiplError error;
        if ((error = mCiplSession.start("record", savePath)).failed()) {
            Log.e(TAG, "Failed to start recording, error="+error+".");
            return false;
        }
        return true;
    }

    /**
     * stop recording progress
     */
    public void stopRecord() {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "stopRecord() cannot proceed: Cipl SDK is not initialized.");
            return;
        }
        if (mCurrentChannel == null) {
            Log.e(TAG, "stopRecord() cannot proceed: no current channel is selected.");
            return;
        }

        CiplError error;
        if ((error = mCiplSession.stop("record")).failed()) {
            Log.e(TAG, "Failed to stop recording, error="+error+".");
        }
    }

    /**
     * start signal test progress
     * @param frequency frequency to test in KHz
     * @param bandwidth bandwidth in KHz
     * @return true--success, false--erred
     */
    public boolean startSignalTest(int frequency, int bandwidth) {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "startSignalTest() cannot proceed: Cipl SDK is not initialized.");
            return false;
        }

        CiplError error;
        if ((error = mCiplSession.startSignalTest(frequency, bandwidth)).failed()) {
            Log.e(TAG, "Failed start signal test, error="+error+".");
            return false;
        }
        return true;
    }
    /**
     * start signal test progress
     * @param ch_no channel number to test
     * @return true--success, false--erred
     */
    public boolean startSignalTest(int ch_no) {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "startSignalTest() cannot proceed: Cipl SDK is not initialized.");
            return false;
        }

        CiplError error;
        if ((error = mCiplSession.startSignalTest(ch_no, 0)).failed()) {
            Log.e(TAG, "Failed start signal test, error="+error+".");
            return false;
        }
        return true;
    }

    /**
     * stop signal test progress
     */

    public void stopSignalTest() {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "stopSignalTest() cannot proceed: Cipl SDK is not initialized.");
            return;
        }

        CiplError error;
        if ((error = mCiplSession.stopSignalTest()).failed()) {
            Log.e(TAG, "Failed stop signal test, error="+error+".");
        }
    }

    /**
     * check current tuner is exist
     * @param context context of application
     * @return true--exist, false--not exist
     */
    public boolean isCurrentTunerExist(Context context)
    {
        return (Bridge.getBridge().CheckDevice((UsbManager)context.getSystemService(Context.USB_SERVICE)) == 0);
    }

    /**
     * get video information
     * @return video information in string
     */
    public String getVideoInfo() {
        return mCiplSession.getString("videoinfo");
    }

    /**
     * utility for convert tuner type from string to enumerator
     * @param device_type string of tuner device type
     * @return type of tuner device
     */
    public int convertTunerType(String device_type) {
        int tuner_type = TUNER_TYPE_ERROR;
        if(device_type.equalsIgnoreCase("DVB-T")) {
            tuner_type = TUNER_TYPE_DVBT;
        } else if(device_type.equalsIgnoreCase("ISDB-T")) {
            tuner_type = TUNER_TYPE_ISDBT;
        } else if(device_type.equalsIgnoreCase("ATSC")) {
            tuner_type = TUNER_TYPE_ATSC;
        } else if(device_type.equalsIgnoreCase("DVB-T2")) {
            tuner_type = TUNER_TYPE_DVBT2;
        } else if(device_type.equalsIgnoreCase("DTMB")) {
            tuner_type = TUNER_TYPE_DTMB;
        } else if(device_type.equalsIgnoreCase("J83B")) {
            tuner_type = TUNER_TYPE_J83B;
        }
        
        return tuner_type;
    }

    /**
     * callback of notification events
     * @param eventArray notification event, formatted in array of string
     */
    @Override
    public void eventCallback(String[] eventArray) {
        CiplEvent event = new CiplEvent(eventArray, mEscCh);
        //Log.i(TAG, "Event: session=\""+event.getSessionName()+"\", event=\""+event.getEventName()+"\".");
        if (event.getEventName().equals("CIPL_TVC_InitialEnd")) {
            String strRes = event.getBlob(1).getItem(0, 1);
            if (strRes != null)
            {
            if (strRes.equals("0")) {
                    Log.i(TAG, "\tExtra info of \"CIPL_TVC_InitialEnd\", res="+strRes);
                mTunerActivating.activate();
                mCurrentTuner = mTunerActivating;
                mTunerActivating = null;

            }
            else {
                    Log.i(TAG, "\tExtra info of \"CIPL_TVC_InitialEnd\", res="+strRes);
                mTunerActivating.deactivate();
                mCurrentTuner = null;
                mTunerActivating = null;
            }
        }
        }
        for (ciplEventListener listener : mEventListeners) {
            listener.onEvent(event);
        }
    }

    /**
     * interface of subtitle listener
     */
    public interface ciplSubtitleListener {
        void onSubtitle(int[] buffer, int width, int height, int type, boolean display);
    }
    ArrayList<ciplSubtitleListener> mSubtitleListeners = new ArrayList<ciplSubtitleListener>();

    /**
     * add a subtitle listener
     * @param listener subtitle listener
     */
    public void addSubtitleListener(ciplSubtitleListener listener) {
        if (!mSubtitleListeners.contains(listener)) {
            mSubtitleListeners.add(listener);
        }
    }

    /**
     * remove subtitle listener
     * @param listener subtitle listener
     */
    public void removeSubtitleListener(ciplSubtitleListener listener) {
        if (mSubtitleListeners.contains(listener)) {
            mSubtitleListeners.remove(listener);
        }
    }

    /**
     * callback of subtitle events
     * @param buffer buffer of subtitle frame content
     * @param width width of subtitle frame
     * @param height height of subtitle frame
     * @param type type of subtitle
     * @param display flag to display subtitle or not
     */
    @Override
    public void subtitleCallback(int[] buffer, int width, int height, int type, boolean display) {
        if (mSubtitleListeners.size() > 0) {
            for (ciplSubtitleListener listener : mSubtitleListeners) {
                listener.onSubtitle(buffer, width, height, type, display);
            }
        }
    }

    /**
     * interface of video frame listener
     */
    public interface ciplVideoFrameListener {
        void onVideoFrame(String type, long[] buffer, int width, int height, int[] stride);
    }
    ArrayList<ciplVideoFrameListener> mVideoFrameListeners = new ArrayList<ciplVideoFrameListener>();

    /**
     * add a video frame listener
     * @param listener video fraem listener
     */
    public void addVideoFrameListener(ciplVideoFrameListener listener) {
        if (!mVideoFrameListeners.contains(listener)) {
            mVideoFrameListeners.add(listener);
        }
    }

    /**
     * remove video frame listener
     * @param listener video frame listener
     */
    public void removeVideoFrameListener(ciplVideoFrameListener listener) {
        if (mVideoFrameListeners.contains(listener)) {
            mVideoFrameListeners.remove(listener);
        }
    }

    /**
     * callback of video frame display event
     * @param type type of video frame
     * @param buffer buffer of video frame content
     * @param width width of video frame
     * @param height height of video frame
     * @param stride stride of video frame
     */
    @Override
    public void videoFrameCallback(String type, long[] buffer, int width, int height, int[] stride) {
        //Log.i(TAG, "videoFrameCallback(type=\""+type+"\", width="+width+", height="+height+").");
        // wyvern: enable the following line to invoke the native function that process the video data
        // VideoFrameProcessor.processVideoFrame(type, buffer, width, height, stride);
        if (mVideoFrameListeners.size() > 0) {
            for (ciplVideoFrameListener listener : mVideoFrameListeners) {
                listener.onVideoFrame(type, buffer, width, height, stride);
            }
        }
    }

    // ==================== Asuka added function ===========================
    /**
     * check current tuner is activating
     * @return true--activating, false--not
     */
    public boolean isTunerActivaiting()
    {
        return (mTunerActivating!=null)?true:false;
    }

    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    /**
     * clear stored list of channel scanned on local cache path
     * @param cachePath path of stored list cache
     */
    public void clearScan(String cachePath) {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "saveScan() cannot proceed: Cipl SDK is not initialized.");
            return;
        }

        File folder = new File(cachePath);
        deleteRecursive(folder);
    }

    boolean isEventOnTime(EventInfo event, Date time) {
        Date startTime = null;
        Date endTime = null;

        try {
            startTime = DtvControl.DTV_DATE_FORMAT.parse(event.getStartTime());
            endTime = DtvControl.DTV_DATE_FORMAT.parse(event.getEndTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if(startTime == null || endTime == null)
            return false;

        return time.after(startTime) && time.before(endTime);
    }

    /**
     * get current program event
     * @param curTime current time
     * @return the program event info of time
     */
    public EventInfo getCurChannelEvent(Date curTime) {
        if (mCipl == null || mCiplSession == null) {
            Log.e(TAG, "getEpgEventList() cannot proceed: Cipl SDK is not initialized.");
            return null;
        }
        if (mCurrentChannel == null) {
            Log.e(TAG, "getEpgEventList() cannot proceed: no current channel is selected.");
            return null;
        }

        CSplitBlob epgBlob;
        if ((epgBlob = mCiplSession.show("program "+mCurrentChannel.getChId())) == null) {
            Log.e(TAG, "getEpgEventList() failed: CiplSession returns zero epg data.");
            return null;
        }

        int iChannelId = mCurrentChannel.getChId();
        int iRows = epgBlob.getNumberRows();
        EventInfo retEvent = null;

        Log.i(TAG, "getEpgEvent on list with size "+iRows);
        for (int i = 0; i < iRows; i++) {
            EventInfo eventInfo = new EventInfo();
            eventInfo.setChannelId(iChannelId);

            // 2 -- start time
            String data = epgBlob.getItem(i, 2);
            if (data != null) {
                eventInfo.setStartTime(data);
            }
            // 3 -- end time
            data = epgBlob.getItem(i, 3);
            if (data != null) {
                eventInfo.setEndTime(data);
            }

            if(isEventOnTime(eventInfo, curTime)) {
                // 0 -- name
                data = epgBlob.getItem(i, 0);
                if (data != null) {
                    eventInfo.setName(data);
                }
                // 1 -- description
                data = epgBlob.getItem(i, 1);
                if (data != null) {
                    eventInfo.setDescription(data);
                }

                // 4 -- condition access
                // 5 -- genre
                // 6 -- rating
                data = epgBlob.getItem(i, 6);
                if (data != null && data.length() > 0) {
                    eventInfo.setRating(data);
                }
                Log.i(TAG, "event: name=\""+eventInfo.getName()+"\", description=\""+eventInfo.getDescription()+
                    "\", start=\""+eventInfo.getStartTime()+"\", end=\""+eventInfo.getEndTime()+".");

                retEvent = eventInfo;
                break;
            }
        }

        return retEvent;
    }


}
