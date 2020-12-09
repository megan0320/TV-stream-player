package com.asuka.dtvplayer.bridgeservice;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

public class DtvBridgeService extends Service {

    static final String LOG_TAG = "DtvBridgeService";
    public static final String SERVICE_NAME = "DtvBridgeService";

    public static final String Dtv_EVENT = "android.intent.action.headunitbridge.huevent";

    static final int DtvBRIDGE_PORT = 2;

    CommPortBridge mBridge = null;

    // Binder given to client
    private final IBinder mBinder = new LocalBinder();
    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public DtvBridgeService getService() {
            // Return this instance of LocalService so clients can call public methods
            return DtvBridgeService.this;
        }
    }

    private ServiceHandler mServiceHandler;
    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

    }

    @Override
    public void onCreate() {
        Log.i(LOG_TAG, "service created");
        super.onCreate();

        initHandlerThread();
    }

    @Override
    public void onDestroy() {

        Log.i(LOG_TAG, "InterfaceService destroyed");

        if(mBridge != null) {
            mBridge.stop();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    //init handler thread
    private void initHandlerThread() {
        HandlerThread handlerThread = new HandlerThread("servicehandler", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        mServiceHandler = new ServiceHandler(handlerThread.getLooper());
    }

    public void startBridgeModel(String model)
    {
        Log.i(LOG_TAG, "startService");


        if(mBridge != null) {
            Log.i(LOG_TAG, "stop previous model service");
            mBridge.stop();
            mBridge = null;
        }

        if(model.equals((Utils.MODEL_IEI_TREADMILL))) {
            Log.i(LOG_TAG, "new IEI_Treadmill CommonPort Bridge");
            mBridge = new IEICommPortBridge(this, mServiceHandler, DtvBRIDGE_PORT);
        }else if(model.equals((Utils.MODEL_NONE))) {
            Log.i(LOG_TAG, "None Module");
        }



        if(mBridge != null)
            mBridge.start();
    }

    public boolean isActivated()
    {
        if(mBridge != null)
        {
            if(mBridge.isRunning())
                return true;
        }

        return false;
    }

    public void sendEventData(String event, Object data)
    {
        if(mBridge != null)
            mBridge.processEvent(event, data);
    }




}