package com.asuka.dtvplayer.bridgeservice;

import android.app.Instrumentation;
import android.os.Handler;
import android.util.Log;

import java.util.Arrays;

import com.asuka.peripheralinterface.ComPort;


/**
 * Class implementing com port monitor & works with specified protocol service
 * mainly for implements use
 *
 * @hide
 */

public abstract class CommPortBridge {

    static final String LOG_TAG = "CommPortBridge";

    int mPort = 2;   // common port number to used

    Handler mHandler;
    boolean mIsRunning = false; // flag to declare monitor/task of bridge is running or not

    private ComPort mCom = null;
    private boolean mStop = false; // flag to stop the monitor thread

    /**
     * Super Constructor for CommPortBridge
     */
    public CommPortBridge() {}
    /*
    public CommPortBridge(Handler handler, int port) {
        mHandler = handler;
        mPort = port;
    }
    */
    /**
     * Start the receiver thread for comport
     */

    public void start() {
        if(mIsRunning == true)
        {
            Log.e(LOG_TAG, "duplicate thread startup");
            return;
        }

        startThread();
    }

    /**
     * Stop the receiver thread and close comport
     */

    public void stop()
    {
        stopThread();
    }
    public boolean isRunning()
    {
        return mIsRunning;
    }
    public void processEvent(String event, Object data) {
        return;
    }

    /**  Open serial com port with specified setting
     * @param com             com port to open,  1 ~ 5
     * @param baudrate       baudrate speed,   {@link #ComPort.BAUDRATE_2400},
     *      *                 {@link #ComPort.BAUDRATE_4800},
     *      *                 {@link #ComPort.BAUDRATE_9600},
     *      *                 {@link #ComPort.BAUDRATE_19200},
     *      *                 {@link #ComPort.BAUDRATE_38400},
     *      *                 {@link #ComPort.BAUDRATE_57600},
     *      *                 {@link #ComPort.BAUDRATE_115200},
     *      *                 {@link #ComPort.BAUDRATE_230400}
     * @param  nBits           data bits,  7~8
     * @param nEvent         parity setting,
     *                                     'N' : no parity; 'O'  : odd parity; 'E'  : even parity
     * @param nStop          stop bits, 1~2
     * @param accessMode    Access mode in one {@link #ComPort.BLOCKING_MODE}, {@link #ComPort.NONBLOCKING_MODE}
     * @return  true:success open; false: failed to open com port
     */
    boolean openPort(int port, int baudrate, int bits, char parity, int stop, int accessMode) {
        mCom = new ComPort(port);
        if(mCom.open(baudrate, bits, parity, stop, accessMode) == false) {
            Log.i(LOG_TAG, "open uart failed");
            return false;
        }

        return true;
    }

    /**  Open serial com port with specified setting
     * @param port             com port to open,  1 ~ 5
     * @param baudrate       baudrate speed
     * @param accessMode    Access mode
     * @return  true:success open; false: failed to open com port
     */
    boolean openPort(int port, int baudrate, int accessMode) {
        mCom = new ComPort(port);
        if(mCom.open(baudrate, accessMode) == false) {
            Log.i(LOG_TAG, "open uart failed");
            mCom = null;
            return false;
        }

        return true;
    }

    void closePort() {
        if(mCom != null)
            mCom.close();
    }

    /**
     * Start comport receiver thread
     */
    private void startThread()
    {
        mStop = false;
        mIsRunning = true;
        rcvThread.start();
    }

    /**
     * Stop comport receiver thread
     */
    private void stopThread()
    {
        Log.i(LOG_TAG, "stop ComPortListener");

        synchronized (CommPortBridge.class) {
            mStop = true;
            int times = 0;
            final int timeoutTime = 100;
            while (mIsRunning && (times < timeoutTime)) {
                times++;
            }

            if(mIsRunning) {
                stopConnection();
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    if (rcvThread.isAlive()) {
                        rcvThread.stop();
                    }
                }
            }
        }
    }

    /**
     *  Open common port connection & initialize connection bridge with device, e.g.
     *  Check whether the peripheral device exist while startup in monitor thread(normally will send a reply and check ack) , or
     *  Initialize the sub-device
     */
    abstract void startConnection();
    /**
     * Stop the connection in end of monitor thread, include close the used common port
     */
    abstract void stopConnection();

    /**
     * Check conditions whether to temporary stop receiving uart buffer
     *
     * @return  true stop, false continue
     */
    boolean isPauseReceive()
    {
        return false;
    }

    /**
     * Check whether received packet match protcol (header/length/checksum..)
     *chceking procedure:
     * step 1. first byte  is header or not
     * step 2. packet length check
     * step 3. checksum check
     *
     * @param packetBuf the received packet to check
     * @return -1 if no match protocol,
     * else return the real packet length (header~end)
     */
    abstract int checkPacket(byte[] packetBuf);

    void sendPacket(byte[] packetBuf, int length)
    {
        if(mCom == null)
            return;

        synchronized (CommPortBridge.class) {
            //Log.d(LOG_TAG, "send packet");
            if(mCom.write(packetBuf, length) < 0)
                Log.e(LOG_TAG, "write com port failed");
        }
    }

    /**
     * Receiver thread to receive data from com port, and save in buffer
     * call the above function in thread to evalute packet checking and processing
     */
    Thread rcvThread = new Thread() {
        @Override
        public void run() {
            byte[] readBuf = new byte[100];
            byte[] packetBuf = null;
            int packetIndex = 0;
            int count;

            startConnection();
            Log.i(LOG_TAG, "ComPortBridge receiver thread start ...");

            mIsRunning = true;
            while ((mStop == false) && (mCom!=null)) {
                if(mCom.isDataReady(500)) {
                    count = mCom.read(readBuf, readBuf.length);

                    if (isPauseReceive()) {
                        continue;
                    }

                    if (count > 0) {
                        int index = 0;
                        int length = 0;

                        Log.i(LOG_TAG, " Receiving  ...");
                        for (int i = 0 ; i < count ; i++)
                        {
                            Log.i(LOG_TAG, String.format("Read Packet : %d ", i+1 ) + String.format("0x%02X", readBuf[i]));
                        }
//                        Log.i(LOG_TAG, "packet : " + String.format("0x%02X", readBuf[0]) + String.format("0x%02X", readBuf[1]) + String.format("0x%02X", readBuf[2]));

                        //Log.i(LOG_TAG, "read length = " + count + String.format(", %02X:%02X:%02X", readBuf[0], readBuf[1], readBuf[2]));
                        //Log.d(LOG_TAG, "read length = " + count);

                        // if last packet buffer not processed completed yet, add the new received data in end and check again
                        if ((packetIndex > 0) && (packetBuf != null)) {
                            packetIndex++;
                            for (int i = packetIndex; i < packetBuf.length; i++) {
                                packetBuf[i] = readBuf[i - packetIndex];
                            }

                            index = packetBuf.length - packetIndex;
                            if ((length = checkPacket(packetBuf)) > 0) {
                                if (length > packetBuf.length) {
                                    byte[] tempBuf = new byte[length];
                                    for (int i = 0; i < packetBuf.length; i++) {
                                        tempBuf[i] = packetBuf[i];
                                    }
                                    for (int i = packetBuf.length - 1; i < length; i++) {
                                        tempBuf[i] = readBuf[index + i];
                                    }
                                    packetBuf = new byte[length];
                                    for (int i = 0; i < length; i++) {
                                        packetBuf[i] = tempBuf[i];
                                    }
                                }
                                // drop the proceed packet
                                index = length - packetIndex;
                            }

                            packetBuf = null;
                            packetIndex = 0;
                        }

                        // new received packet data processing
                        while (index < count) {
                            if ((length = checkPacket(Arrays.copyOfRange(readBuf, index, count))) > 0) {
                                // not received complete packet in this time, save in packetbuffer first
                                if ((index + length - 1) >= count) {
                                    packetBuf = new byte[length];
                                    for (int i = index; i < count; i++) {
                                        packetBuf[i - index] = readBuf[i];
                                    }
                                    packetIndex = count - index;
                                    index = count;
                                } else {
                                    index += length;
                                }
                            } else {
                                index++;
                            }
                        }
                    }
                }
            }

            mIsRunning = false;
            stopConnection();
            Log.i(LOG_TAG, "ComPortBridge receiver thread stopped...");
        }
    };

}
