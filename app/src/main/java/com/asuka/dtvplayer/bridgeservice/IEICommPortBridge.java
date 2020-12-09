package com.asuka.dtvplayer.bridgeservice;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;

import com.asuka.peripheralinterface.ComPort;

import java.util.ArrayList;
import java.util.Arrays;
import java.nio.ByteBuffer;



/**
 * Implementation of  Treadmill common port (UART) communication interface
 */
public class IEICommPortBridge extends CommPortBridge {
    public static final String LOG_TAG = "IEICommPortBridge";

    /**
    * Packet (command/ack/response) start&end bit
    */
    static final byte STX = (byte)0xF1;
    static final byte ETX = (byte)0xF2;

    /** edward
     *  Protocol of command packet
     *  STX (1) | TYPE(1) | LENGTH(2) | CMD(2)) | DATA (N) | CHKSM (1) | ETX(1)
     *  STX : 0xF1
     *  Type    Command : 0x00
     *  LENGTH : TYPE(1) | LENGTH(2) | CMD(2)) | DATA (N) | CHKSM (1) = 1+2+2+N+1
     *  ETX : 0xF2
     *  CHKSM : The value is the result of XOR of packet from <TYPE> to <DATA>

     */

    //TYPE
    static final byte TYPE_COMMAND = (byte)0x00;
    //CMD_1
    static final byte CMD_FIRSTBIT = (byte)0x00;
    //CMD_2
    static final byte CMD_CHECK_MODULE_READY = (byte)0x01;
    static final byte CMD_SET_MODULE_POWER = (byte)0x02;
    static final byte CMD_GET_SW_VERSION = (byte)0x08;
    static final byte CMD_SEND_KEYCODE = (byte)0x0B;
    static final byte CMD_SET_VOLUME = (byte)0x0D;
    static final byte CMD_SET_VOLUME_MUTE = (byte)0x0E;
    static final byte CMD_SET_CH = (byte)0x11;
    static final byte CMD_CH_UP_DN = (byte)0x12;
    static final byte CMD_GET_CHANNEL_LIST = (byte)0x15;
    static final byte CMD_GET_CHANNEL_INFO = (byte)0x21;
    static final byte CMD_GET_VOLUME = (byte)0x26;
    //CMD DATA
        //MODULE_POWER
        static final byte DATA_SET_MODULE_POWER_STANDBY = (byte)0x00;
        static final byte DATA_SET_MODULE_POWER_POWERON = (byte)0x01;

        //CMD_SEND_KEYCODE
        static final byte DATA_KEYCODE_POWER = (byte)0x00;
        static final byte DATA_KEYCODE_SOURCE = (byte)0x01;
        static final byte DATA_KEYCODE_MUTE = (byte)0x02;
        static final byte DATA_KEYCODE_TVRadio = (byte)0x03;
        static final byte DATA_KEYCODE_Menu = (byte)0x04;
        static final byte DATA_KEYCODE_DPAD_UP = (byte)0x05;
        static final byte DATA_KEYCODE_DPAD_DOWN = (byte)0x0D;
        static final byte DATA_KEYCODE_DPAD_RIGHT = (byte)0x0A;
        static final byte DATA_KEYCODE_DPAD_LEFT = (byte)0x08;
        static final byte DATA_KEYCODE_OK = (byte)0x09;
        static final byte DATA_KEYCODE_EXIT = (byte)0x0C;
        //static final byte DATA_KEYCODE_PREVIEW = (byte)0x0B;
        static final byte DATA_KEYCODE_AUDIO = (byte)0x0F;
        static final byte DATA_KEYCODE_INFO = (byte)0x13;
        static final byte DATA_KEYCODE_EPG = (byte)0x07;
        static final byte DATA_KEYCODE_AUTOSEARCH = (byte)0x17;
//        static final byte DATA_KEYCODE_FR = (byte)0x5d;
//        static final byte DATA_KEYCODE_FF = (byte)0x5e;
        static final byte DATA_KEYCODE_1 = (byte)0x10;
        static final byte DATA_KEYCODE_2 = (byte)0x11;
        static final byte DATA_KEYCODE_3 = (byte)0x12;
        static final byte DATA_KEYCODE_4 = (byte)0x14;
        static final byte DATA_KEYCODE_5 = (byte)0x15;
        static final byte DATA_KEYCODE_6 = (byte)0x16;
        static final byte DATA_KEYCODE_7 = (byte)0x18;
        static final byte DATA_KEYCODE_8 = (byte)0x19;
        static final byte DATA_KEYCODE_9 = (byte)0x1A;
        static final byte DATA_KEYCODE_0 = (byte)0x1D;
        static final byte DATA_KEYCODE_POINT = (byte)0x20;
        static final byte DATA_KEYCODE_RECALL = (byte)0x1C;
//        static final byte DATA_KEYCODE_F1 = (byte)0x40;
//        static final byte DATA_KEYCODE_F2 =  (byte)0x41;
//        static final byte DATA_KEYCODE_F3 = (byte)0x42;
//        static final byte DATA_KEYCODE_F4 = (byte)0x43;
//        static final byte DATA_KEYCODE_HOME = (byte)0x4a;
//        static final byte DATA_KEYCODE_PLAYPAUSE = (byte)0x0e;
        static final byte DATA_KEYCODE_SUBTITLE = (byte)0x2f;
        static final byte DATA_KEYCODE_TXT = (byte)0x1f;

        //CH_UP_DN
        static final byte DATA_CH_UP = (byte)0x00;
        static final byte DATA_CH_DN = (byte)0x01;



    /** edward
     *  Protocol of acknowledge packet
     *  STX (1) | TYPE(1) | LENGTH(2) | CMD(2)) | DATA (N) | CHKSM (1) | ETX(1)
     *  STX : 0xF1
     *  Type    Acknowledge : 0x01
     *  DATA    Success : 0x01
     *          Busy    : 0x02
     *          Processing: 0x0A
     *          Fail    : 0xFF { Invalid command : 0x01
     *                           Invalid parameter : 0x02
     *                           Checksum error : 0x03
     *                           Invalid state : 0x04
     *          }
     *  LENGTH : TYPE(1) | LENGTH(2) | CMD(2)) | DATA (N) | CHKSM (1) = 1+2+2+N+1
     *  ETX : 0xF2
     *  CHKSM : The value is the result of XOR of packet from <TYPE> to <DATA>

     */
    //TYPE
    static final byte TYPE_ACK = (byte)0x01;
    //CMD (same as command)

    //ACK DATA

    static final byte ACK_DATA_FAIL = (byte)0x0FF;
    static final byte ACK_DATA_FAIL_INVALIDCOMMAND = (byte)0x01;
    static final int ACK_DATA_FAIL_INVALIDCOMMAND_STATE = 1;
    static final byte ACK_DATA_FAIL_INVALIDPARAMETER = (byte)0x02;
    static final int ACK_DATA_FAIL_INVALIDPARAMETER_STATE = 2;
    static final byte ACK_DATA_FAIL_CHECKSUMERROR = (byte)0x03;
    static final int ACK_DATA_FAIL_CHECKSUMERROR_STATE = 3;
    static final byte ACK_DATA_FAIL_INVALIDSTATE = (byte)0x04;
    static final byte ACK_DATA_SUCCESS = (byte)0x01;
    static final int ACK_DATA_SUCCESS_STATE = 4;
    static final byte ACK_DATA_BUSY = (byte)0x02;
    static final byte ACK_DATA_PROCESSING = (byte)0x0A;





    /** edward
     *  Protocol of response packet
     *  STX (1) | TYPE(1) | LENGTH(2) | CMD(2)) | DATA (N) | CHKSM (1) | ETX(1)
     *  STX : 0xF1
     *  Type    Response : 0x02
     *  LENGTH : TYPE(1) | LENGTH(2) | CMD(2)) | DATA (N) | CHKSM (1) = 1+2+2+N+1
     *  ETX : 0xF2
     *  CHKSM : The value is the result of XOR of packet from <TYPE> to <DATA>

     */
    //TYPE
    static final byte TYPE_RESPONSE = (byte)0x02;
    //CMD (same as command)

    //RESPONSE DATA
    /*
        //UARTSTATUS
        static final byte DATA_SYSTEM_READY = (byte)0x01;
        static final byte DATA_SYSTEM_NOTREADY = (byte)0x00;
        //SOFTWAREVERSION
        static final byte RES_DATA_SOFTWAREVERSION_MAJOR = (byte)0x01;
        static final byte RES_DATA_SOFTWAREVERSION_MANOR = (byte)0x00;
        static final byte RES_DATA_SOFTWAREVERSION_BUGFIX = (byte)0x00;
        static final byte RES_DATA_SOFTWAREVERSION_CUSTOMIZATION = (byte)0x00;
     */

    private int MSG_SET_CHANNEL_TIMEOUT = 1;

    private String channelKeyEvent = "";

    private Context context;
    public IEICommPortBridge(Context context, Handler handler, int port) {
        mHandler = handler;
        this.context=context;
        //mDisplayMetrics = context.getResources().getDisplayMetrics();
        mPort = port;
    }

    private Handler mSendHandler = new Handler( ) {
        @Override
        public void dispatchMessage(Message msg)
        {
            if(msg.what == MSG_SET_CHANNEL_TIMEOUT) {
                sendChannelEvent(channelKeyEvent);
                channelKeyEvent = "";
            }
        }
    };

    /*
    private Runnable mSendRunnable = new Runnable( ) {
        public void run ( ) {
            checkEnterNumState();
            //Log.i(LOG_TAG, "setChNum:"+setChNum);
            enterNum = false;
            mSendHandler.postDelayed(this,2000);
        }
    };
     */


    /**
     * build connecting bridge with Treadmill
     */
    @Override
    void startConnection()
    {
        if (openPort(mPort, ComPort.BAUDRATE_115200, ComPort.NONBLOCKING_MODE) == false) {
            Log.e(LOG_TAG, "open port failed");
            return;
        }
        else {
            Log.i(LOG_TAG, "open port access...");
            sendReadyPacket();
        }

        //mSendHandler.postDelayed(mSendRunnable,0);
    }

    /**
     * build connecting bridge with Treadmil
     */
    @Override
    void stopConnection()
    {
        //mSendHandler.removeCallbacks(mSendRunnable);
        closePort();
    }

    //response
    @Override
    public void processEvent(String event, Object data) {
        switch(event) {
            case Utils.EVENT_GET_SW_VERSION:{
                String version= (String)data;
                //VersionString = x.x.x
                String versionData[] = version.split("\\.");
                int majorImprovement = Integer.parseInt(versionData[0]);
                int minorImprovement = Integer.parseInt(versionData[1]);
                int bugFix = Integer.parseInt(versionData[2]);
                int customization = 0;
                byte [] swVersionData = new byte [4];
                swVersionData[0] = (byte) majorImprovement;
                swVersionData[1] = (byte) minorImprovement;
                swVersionData[2] = (byte) bugFix;
                swVersionData[3] = (byte) customization;
                sendResponsePacket(CMD_GET_SW_VERSION,swVersionData);
            } break;
            case Utils.EVENT_GET_VOLUME:{
                int volume = (int) data;
                byte [] getVolumeData = new byte[1];
                getVolumeData[0] = (byte)volume;
                sendResponsePacket(CMD_GET_VOLUME,getVolumeData);
            }break;
            case Utils.EVENT_CHANNEL_INFO: {
                Log.i(LOG_TAG, "EVENT_CHANNEL_INFO");
                SimpleChannel channel = (SimpleChannel)data;
                Log.i(LOG_TAG, "current number : " + String.format( "%.1f",channel.getNum()) +"current channel : " + channel.getName());
                byte [] chNumByte = convertChNum2Byte(channel.getNum());
                byte [] chNameAsciiByte = convertString2AsciiByte(channel.getName());
                byte [] chInfoData = setChannelData(chNumByte,chNameAsciiByte);
                sendResponsePacket(CMD_GET_CHANNEL_INFO,chInfoData);
            }break;
            case Utils.EVENT_CHANNEL_LIST: {
                ArrayList<SimpleChannel> list = (ArrayList<SimpleChannel>)data;
                Log.i(LOG_TAG, "get channel list with size " + list.size());
                byte [] chListData = {};
                for(int i = 0; i<list.size();i++){
                    byte [] chNumByte = convertChNum2Byte(list.get(i).getNum());
                    byte [] chNameAsciiByte = convertString2AsciiByte(list.get(i).getName());
                    byte [] chInfo = setChannelData(chNumByte,chNameAsciiByte);
                    chListData = Arrays.copyOf(chListData, chListData.length+chInfo.length);
                    System.arraycopy(chInfo, 0, chListData, chListData.length-chInfo.length , chInfo.length);
                }
                sendResponsePacket(CMD_GET_CHANNEL_LIST,chListData);
            }
                break;

            default:
                break;
        }
    }

    @Override
    int checkPacket(byte[] packetBuf) {
        Log.i(LOG_TAG, "Check cmd packet ");
        //  checkState : 1 = STX correct ; 2 = ETX correct ; 3 = LEN correct 4 = checksum correct
//        for (int i = 0 ; i < packetBuf.length ; i++)
//        {
//            Log.i(LOG_TAG, String.format(" %d  : ", i+1 ) + String.format("0x%02X", packetBuf[i]));
//        }

        int packetLen = packetBuf[3]+2; ;

        if(packetBuf[0] == STX) {
         //   Log.i(LOG_TAG, "packet Len " + packetLen);
            if (packetBuf[packetLen-1] == ETX) {
                byte[] cmdPacket = Arrays.copyOfRange(packetBuf, 0, packetLen);
                int checksum = calculateChecksum(cmdPacket);
                // checksum checking
                if (cmdPacket[packetLen - 2] == checksum) {
                    if(packetBuf[1] == TYPE_COMMAND){
                        processPacket(cmdPacket);
                    }
                    return packetLen;
                }
                else {
                    Log.i(LOG_TAG, "CHECKSUM_COMMAND failed! " + String.format("0x%02X", (checksum)) + " / " + String.format("0x%02X", cmdPacket[packetLen - 2]));
                    //return -1;
                    return packetLen;
                }
            }
                else {
                        Log.i(LOG_TAG, "ETX failed! ");
                }

        }
        else {
            Log.i(LOG_TAG, "STX failed! ");
        }
        return packetLen;
    }

    void processPacket(byte[] packetBuf ) {
        //make ackDATA

        //1 = fail - Invalid command ACK_DATA_FAIL_INVALIDCOMMAND_STATE = 1;
        //2 = fail - Invalid parameter ACK_DATA_FAIL_INVALIDPARAMETER_STATE = 2;
        //3 = fail - checksum error ACK_DATA_FAIL_CHECKSUMERROR_STATE = 3;
        //4 = success ACK_DATA_SUCCESS_STATE = 4;


        int checkAckState = 0;
        if (packetBuf [packetBuf.length-2] == calculateChecksum(packetBuf)){
            checkAckState = 0;
        }
        else {
            checkAckState = ACK_DATA_FAIL_CHECKSUMERROR_STATE;
            Log.i(LOG_TAG, "Checksum error");
        }
        //Get Command Packet success >> make ACK Packet

        if (checkAckState == 0) {
            // command packet
            byte cmd_1= packetBuf[4];
            byte cmd_2= packetBuf[5];
            if (cmd_1 == CMD_FIRSTBIT) {
                    switch (cmd_2) {
                        // make Response Packet
                        case CMD_CHECK_MODULE_READY: {
                            Log.i(LOG_TAG, "CMD_CHECK_MODULE_READY");
                            checkAckState = ACK_DATA_SUCCESS_STATE;
                            sendAckPacket( cmd_2 , checkAckState );
                            sendReadyPacket();
                        }break;
                        case CMD_SET_MODULE_POWER: {
                            Log.i(LOG_TAG, "CMD_SET_MODULE_POWER");
                            checkAckState = ACK_DATA_SUCCESS_STATE;
                            sendAckPacket( cmd_2 , checkAckState );

                        }break;
                        case CMD_GET_SW_VERSION: {
                            Log.i(LOG_TAG, "CMD_GET_SW_VERSION");
                            checkAckState = ACK_DATA_SUCCESS_STATE;
                            sendAckPacket( cmd_2 , checkAckState );
                            sendCommandEvent(Utils.EVENT_GET_SW_VERSION);
                        }break;
                        case CMD_SEND_KEYCODE: {
                            Log.i(LOG_TAG, "CMD_SEND_KEYCODE");
                            checkAckState = ACK_DATA_SUCCESS_STATE;
                            sendAckPacket( cmd_2 , checkAckState );
                            byte KeyCode = packetBuf[6];
                            processSend_Keycode(KeyCode);
                        }break;
                        case CMD_SET_VOLUME: {
                            Log.i(LOG_TAG, "CMD_SET_VOLUME");
                            checkAckState = ACK_DATA_SUCCESS_STATE;
                            sendAckPacket( cmd_2 , checkAckState );
                            int Volume = (int)packetBuf [6];
                            sendSetVolume(Volume);
                        }break;
                        case CMD_SET_VOLUME_MUTE: {
                            Log.i(LOG_TAG, "CMD_SET_VOLUME_MUTE");
                            checkAckState = ACK_DATA_SUCCESS_STATE;
                            sendAckPacket( cmd_2 , checkAckState );
                            sendCommandEvent(Utils.EVENT_SET_VOLUME_MUTE);

                        }break;
                        case CMD_SET_CH: {
                            Log.i(LOG_TAG, "CMD_SET_CH");
                            byte [] setChNum = new byte [2];
                            setChNum [0] = packetBuf[6];
                            setChNum [1] = packetBuf[7];
                            int chNum = convertByte2Int32(setChNum);
                            int DecimalOfsetChNum = (int)packetBuf[8];
                            String sChNum = "" + chNum + "." + DecimalOfsetChNum;
                            Log.i(LOG_TAG, "ch number :" + sChNum );
                            checkAckState = ACK_DATA_SUCCESS_STATE;
                            sendAckPacket( cmd_2 , checkAckState );
                            sendChannelEvent(sChNum);
                        }break;
                        case CMD_CH_UP_DN: {
                            Log.i(LOG_TAG, "CMD_CH_UP_DN");
                            checkAckState = ACK_DATA_SUCCESS_STATE;
                            sendAckPacket( cmd_2 , checkAckState );
                            if (packetBuf[6] == DATA_CH_UP){
                                sendKeyCode(KeyEvent.KEYCODE_DPAD_UP);
                            }
                            else if (packetBuf[6] == DATA_CH_DN){
                                sendKeyCode(KeyEvent.KEYCODE_DPAD_DOWN);
                            }
                        }break;
                        case CMD_GET_CHANNEL_LIST: {
                            Log.i(LOG_TAG, "CMD_GET_CHANNEL_LIST");
                            checkAckState = ACK_DATA_SUCCESS_STATE;
                            sendAckPacket( cmd_2 , checkAckState );
                            sendCommandEvent(Utils.EVENT_CHANNEL_LIST);
                        }break;
                        case CMD_GET_CHANNEL_INFO: {
                            Log.i(LOG_TAG, "CMD_GET_CHANNEL_INFO");
                            checkAckState = ACK_DATA_SUCCESS_STATE;
                            sendAckPacket( cmd_2 , checkAckState );
                            sendCommandEvent(Utils.EVENT_CHANNEL_INFO);

                        }break;
                        case CMD_GET_VOLUME: {
                            Log.i(LOG_TAG, "CMD_GET_VOLUME");
                            checkAckState = ACK_DATA_SUCCESS_STATE;
                            sendAckPacket( cmd_2 , checkAckState );
                            sendCommandEvent(Utils.EVENT_GET_VOLUME);
                        }break;
                        default : {
                            checkAckState = ACK_DATA_FAIL_INVALIDCOMMAND_STATE;
                            Log.i(LOG_TAG, "Invalid command");
                            sendAckPacket( cmd_2 , checkAckState );
                        }break;
                    }


            }

        }
    }


    void sendAckPacket(byte command , int checkACKstate){
        int ackDATAlen = 0;
        byte [] ackDATA = new byte[2] ;
        switch (checkACKstate){
            case ACK_DATA_FAIL_INVALIDCOMMAND_STATE:{
                ackDATA[0] = ACK_DATA_FAIL;
                ackDATA[1] = ACK_DATA_FAIL_INVALIDCOMMAND;
                ackDATAlen = 2;
            }break;
            case ACK_DATA_FAIL_INVALIDPARAMETER_STATE:{
                ackDATA[0] = ACK_DATA_FAIL;
                ackDATA[1] = ACK_DATA_FAIL_CHECKSUMERROR;
                ackDATAlen = 2;
            }break;
            case ACK_DATA_FAIL_CHECKSUMERROR_STATE:{
                ackDATA[0] = ACK_DATA_FAIL;
                ackDATA[1] = ACK_DATA_FAIL_INVALIDPARAMETER;
                ackDATAlen = 2;
            }break;
            case ACK_DATA_SUCCESS_STATE:{
                ackDATA[0] = ACK_DATA_SUCCESS;
                ackDATAlen = 1;
            }break;
        }
        byte[] ackPacketBuf = new byte [8 + ackDATAlen];
        //type
        ackPacketBuf [0] = STX;
        ackPacketBuf [1] = TYPE_ACK;
        byte [] lenByte = convertInt2ByteArray(ackPacketBuf.length-2);
        ackPacketBuf[2] = lenByte[0];
        ackPacketBuf[3] = lenByte[1];
        ackPacketBuf[4] = CMD_FIRSTBIT;
        ackPacketBuf[5] = command;
        for ( int i= 0 ; i < ackDATAlen ; i++){
            ackPacketBuf [6+i] = ackDATA [i];
        }
        ackPacketBuf [ackPacketBuf.length-2] = calculateChecksum(ackPacketBuf);
        ackPacketBuf [ackPacketBuf.length-1] = ETX;
//        Log.i(LOG_TAG, "make AckPacket success");
//        for (int i = 0 ; i < ackPacketBuf.length ; i++)
//        {
//            Log.i(LOG_TAG, String.format(" %d  : ", i+1 ) + String.format("0x%02X", ackPacketBuf[i]));
//        }
        sendPacket(ackPacketBuf, ackPacketBuf.length);
        Log.i(LOG_TAG, "send AckPacket success ");


    }

    byte calculateChecksum(byte[] packetBuf){
        byte checksum = 0;
        for(int i = 1; i < packetBuf.length-2; i++) {
            checksum ^= packetBuf[i];
        }

        checksum &= 0xFF;

        return checksum;
    }


 void processSend_Keycode(byte KeyCode) {

     switch (KeyCode) {
         case DATA_KEYCODE_POWER: {

         }
         break;
         case DATA_KEYCODE_SOURCE: {

         }
         break;
         case DATA_KEYCODE_MUTE : {
             sendAckPacket( CMD_SET_VOLUME_MUTE , ACK_DATA_SUCCESS_STATE );
             sendCommandEvent(Utils.EVENT_SET_VOLUME_MUTE);
         }
         break;
         case DATA_KEYCODE_TVRadio : {

         }
         break;
         case DATA_KEYCODE_Menu : {
             sendKeyCode(KeyEvent.KEYCODE_MENU);
         }
         break;
         case DATA_KEYCODE_DPAD_UP : {
             sendKeyCode(KeyEvent.KEYCODE_DPAD_UP);
         }
         break;
         case DATA_KEYCODE_DPAD_DOWN : {
             sendKeyCode(KeyEvent.KEYCODE_DPAD_DOWN);
         }
         break;
         case DATA_KEYCODE_DPAD_RIGHT : {
             sendKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT);
         }
         break;
         case DATA_KEYCODE_DPAD_LEFT : {
             sendKeyCode(KeyEvent.KEYCODE_DPAD_LEFT);
         }
         break;
         case DATA_KEYCODE_OK : {
             if(!channelKeyEvent.equals("")){
                 Log.i(LOG_TAG, "setChannel");
                 sendChannelEvent(channelKeyEvent);
             }else {
                 sendKeyCode(KeyEvent.KEYCODE_ENTER);
             }
         }
         break;
         case DATA_KEYCODE_EXIT : {
             sendKeyCode(KeyEvent.KEYCODE_NAVIGATE_PREVIOUS);
         }
         break;
         case DATA_KEYCODE_AUDIO : {

         }
         break;
         case DATA_KEYCODE_INFO : {

         }
         break;
         case DATA_KEYCODE_EPG : {

         }
         break;
         case DATA_KEYCODE_AUTOSEARCH : {
             sendKeyCode(KeyEvent.KEYCODE_SEARCH);
         }
         break;

         case DATA_KEYCODE_1 :{
             channelKeyEvent = channelKeyEvent + "1";
             sendOSDString(channelKeyEvent);
             mSendHandler.removeMessages(MSG_SET_CHANNEL_TIMEOUT);
             mSendHandler.sendEmptyMessageDelayed(MSG_SET_CHANNEL_TIMEOUT, 2000);
         }break;
         case DATA_KEYCODE_2 :{
             channelKeyEvent = channelKeyEvent + "2";
             sendOSDString(channelKeyEvent);
             mSendHandler.removeMessages(MSG_SET_CHANNEL_TIMEOUT);
             mSendHandler.sendEmptyMessageDelayed(MSG_SET_CHANNEL_TIMEOUT, 2000);
         }break;
         case DATA_KEYCODE_3 :{
             channelKeyEvent = channelKeyEvent + "3";
             sendOSDString(channelKeyEvent);
             mSendHandler.removeMessages(MSG_SET_CHANNEL_TIMEOUT);
             mSendHandler.sendEmptyMessageDelayed(MSG_SET_CHANNEL_TIMEOUT, 2000);
         }break;
         case DATA_KEYCODE_4 :{
             channelKeyEvent = channelKeyEvent + "4";
             sendOSDString(channelKeyEvent);
             mSendHandler.removeMessages(MSG_SET_CHANNEL_TIMEOUT);
             mSendHandler.sendEmptyMessageDelayed(MSG_SET_CHANNEL_TIMEOUT, 2000);
         }break;
         case DATA_KEYCODE_5 :{
             channelKeyEvent = channelKeyEvent + "5";
             sendOSDString(channelKeyEvent);
             mSendHandler.removeMessages(MSG_SET_CHANNEL_TIMEOUT);
             mSendHandler.sendEmptyMessageDelayed(MSG_SET_CHANNEL_TIMEOUT, 2000);
         }break;
         case DATA_KEYCODE_6 :{
             channelKeyEvent = channelKeyEvent + "6";
             sendOSDString(channelKeyEvent);
             mSendHandler.removeMessages(MSG_SET_CHANNEL_TIMEOUT);
             mSendHandler.sendEmptyMessageDelayed(MSG_SET_CHANNEL_TIMEOUT, 2000);
         }break;
         case DATA_KEYCODE_7:{
             channelKeyEvent = channelKeyEvent + "7";
             sendOSDString(channelKeyEvent);
             mSendHandler.removeMessages(MSG_SET_CHANNEL_TIMEOUT);
             mSendHandler.sendEmptyMessageDelayed(MSG_SET_CHANNEL_TIMEOUT, 2000);
         }break;
         case DATA_KEYCODE_8 :{
             channelKeyEvent = channelKeyEvent + "8";
             sendOSDString(channelKeyEvent);
             mSendHandler.removeMessages(MSG_SET_CHANNEL_TIMEOUT);
             mSendHandler.sendEmptyMessageDelayed(MSG_SET_CHANNEL_TIMEOUT, 2000);
         }break;
         case DATA_KEYCODE_9 :{
             channelKeyEvent = channelKeyEvent + "9";
             sendOSDString(channelKeyEvent);
             mSendHandler.removeMessages(MSG_SET_CHANNEL_TIMEOUT);
             mSendHandler.sendEmptyMessageDelayed(MSG_SET_CHANNEL_TIMEOUT, 2000);
         }break;
         case DATA_KEYCODE_0 :{
             channelKeyEvent = channelKeyEvent + "0";
             sendOSDString(channelKeyEvent);
             mSendHandler.removeMessages(MSG_SET_CHANNEL_TIMEOUT);
             mSendHandler.sendEmptyMessageDelayed(MSG_SET_CHANNEL_TIMEOUT, 2000);
         }break;
         case DATA_KEYCODE_POINT :{
             channelKeyEvent = channelKeyEvent + ".";
             sendOSDString(channelKeyEvent);
             mSendHandler.removeMessages(MSG_SET_CHANNEL_TIMEOUT);
             mSendHandler.sendEmptyMessageDelayed(MSG_SET_CHANNEL_TIMEOUT, 2000);
         }break;
         /*
         case DATA_KEYCODE_RECALL :{

         }break;
         case DATA_KEYCODE_SUBTITLE :{

         }break;
         case DATA_KEYCODE_TXT :{

         }break;
        */
         default:
             Log.w(LOG_TAG, "invalid keycode "  + KeyCode);
             break;
     }
 }

 //Broadcast
    public void sendSetVolume(int Volume){
        Intent sendSetVolumeIntent = new Intent(Utils.EVENT_SET_VOLUME);
        sendSetVolumeIntent.putExtra(Utils.MSG_SETVOLUME, Volume);
        context.sendBroadcast(sendSetVolumeIntent);
    }
    public void sendChannelEvent(String channelNo){
        Log.i(LOG_TAG, "sendChannelEvent " + channelNo);

        Intent sendChannelEventIntent = new Intent(Utils.EVENT_CHANNEL);
        sendChannelEventIntent.putExtra(Utils.MSG_CHANNEL_NUM, channelNo);
        context.sendBroadcast(sendChannelEventIntent);
    }

    public void sendOSDString(String OSDString){
        Intent sendOSDStringIntent = new Intent(Utils.EVENT_OSD_STRING);
        sendOSDStringIntent.putExtra(Utils.MSG_OSD_STRING, OSDString);
        context.sendBroadcast(sendOSDStringIntent);
        Log.i(LOG_TAG, "OSD String " + OSDString);
    }

    public void sendKeyCode(int KeyCode){
        Intent sendKeyCodeIntent = new Intent(Utils.EVENT_KEY);
        sendKeyCodeIntent.putExtra(Utils.MSG_KEYCODE, KeyCode);
        context.sendBroadcast(sendKeyCodeIntent);
    }

    public void sendCommandEvent(String CommandEvent){
        Intent sendCommandEventIntent = new Intent(CommandEvent);
        context.sendBroadcast(sendCommandEventIntent);
    }




 //Send CHECK_MODULE_READY packet
    public void sendReadyPacket(){
        Log.i(LOG_TAG, "Check Mouble Ready");
        byte[] readyPacketBuf = new byte[9];
        readyPacketBuf[0] =  STX;
        readyPacketBuf[1] = TYPE_RESPONSE;
        readyPacketBuf[2] = (byte) 0x00;
        readyPacketBuf[3] = (byte) 0x07;
        readyPacketBuf[4] = CMD_FIRSTBIT;
        readyPacketBuf[5] = CMD_CHECK_MODULE_READY;
        readyPacketBuf[6] = (byte) 0x01; //UART ready: 0x01
        readyPacketBuf[7] = calculateChecksum(readyPacketBuf);
        readyPacketBuf[8] = ETX;
        sendPacket(readyPacketBuf, readyPacketBuf.length);
    }

    /**
     * ChannelData
     *   DVBT type (DVBT type): 1 byte  TV: 0x00
     *   CH number (LCN Channel number): 2 bytes
     *   CH name (Channel name) Plain ASCII string.
     *   Symbol 0x03,0x22 & 0x03,0x21 will be sent to denote start & end of string.
     *   Example: TV channel 01 “BBC NEWS” will be sent as:
     *   <0x00,0x00,0x01,0x03,0x22,0x00, 0x42, 0x00, 0x42, 0x00, 0x43, 0x00, 0x20, 0x00, 0x4E, 0x00, 0x45, 0x00,0x57, 0x00, 0x53, 0x03,0x21 >
     * */
    byte [] setChannelData(byte[] chNum,byte[] chName){
        byte [] responseData = new byte [5+chNum.length+chName.length];
        byte [] responseDataDVBT_type = {(byte)0x00};//DVBT type TV: 0x00
        byte [] responseDataSymbolStart = {(byte)0x03,(byte)0x22};
        byte [] responseDataSymbolEnd = {(byte)0x03,(byte)0x21};
        //add DVBT type TV: 0x00
        System.arraycopy(responseDataDVBT_type, 0, responseData, 0 , responseDataDVBT_type.length);
        //add chnum
        System.arraycopy(chNum, 0, responseData, responseDataDVBT_type.length , chNum.length);
        //add name start
        System.arraycopy(responseDataSymbolStart, 0, responseData, responseDataDVBT_type.length + chNum.length , responseDataSymbolStart.length);
        //add chname
        System.arraycopy(chName, 0, responseData, responseDataDVBT_type.length + chNum.length + responseDataSymbolStart.length , chName.length);
        //add name end
        System.arraycopy(responseDataSymbolEnd, 0, responseData, responseDataDVBT_type.length + chNum.length + responseDataSymbolStart.length + chName.length , responseDataSymbolEnd.length);
        return responseData;
    }

    void sendResponsePacket(byte command , byte [] responseDATA){
        int responseDATAlen = responseDATA.length;
        byte[] responsePacketBuf = new byte [8 + responseDATAlen];
        //type
        responsePacketBuf [0] = STX;
        responsePacketBuf [1] = TYPE_RESPONSE;

        byte [] lenByte = convertInt2ByteArray(responsePacketBuf.length-2);
        responsePacketBuf[2] = lenByte[0];
        responsePacketBuf[3] = lenByte[1];

        responsePacketBuf [4] = CMD_FIRSTBIT;
        responsePacketBuf [5] = command;
        for ( int i= 0 ; i < responseDATAlen ; i++){
            responsePacketBuf [6+i] = responseDATA [i];
        }
        responsePacketBuf [responsePacketBuf.length-2] = calculateChecksum(responsePacketBuf);
        responsePacketBuf [responsePacketBuf.length-1] = ETX;
//        Log.i(LOG_TAG, "make ResponsePacket success");
//        for (int i = 0 ; i < responsePacketBuf.length ; i++)
//        {
//            Log.i(LOG_TAG, String.format(" %d  : ", i+1 ) + String.format("0x%02X", responsePacketBuf[i]));
//        }
        sendPacket(responsePacketBuf, responsePacketBuf.length);
        Log.i(LOG_TAG, "send ResponsePacket success ");
    }

    public static byte[] convertInt2ByteArray(int convertInt) {
        byte [] Int2Byte = new byte[2] ;
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(convertInt);
        byte[] result = b.array();
        System.arraycopy(result, 2, Int2Byte, 0 , 2);
        return Int2Byte ;
    }
    public int convertByte2Int32(byte[] data)
    {
        int factor = data.length - 1;
        int result = 0;
        for (int i = 0; i < data.length; i++) {
            if (i == 0) {
                result |= data[i] << (8 * factor--);
            } else {
                result |= data[i] << (8 * factor--);
            }
        }
        return result;
    }

    byte[] convertChNum2Byte(float ChNum){
        int chNumInt = (int)(ChNum * 10);
        int channelNum = (chNumInt/10);
        int decimalOfChannelNum = (chNumInt%10);
        //add ch Num
        byte [] chNum2Byte = new byte[3] ;
        byte[] byteChannelNum = ByteBuffer.allocate(4).putInt(channelNum).array();
        System.arraycopy(byteChannelNum, 2, chNum2Byte, 0 , 2);
        //add Decimal of ch Num
        byte[] byteDecimalOfChannelNum = ByteBuffer.allocate(4).putInt(decimalOfChannelNum).array();
        System.arraycopy(byteDecimalOfChannelNum, 3, chNum2Byte, 2 , 1);
        return chNum2Byte;
    }
    public byte [] convertString2AsciiByte(String s) {
        int sLenght = s.length(); // length of the string used for the loop
        int [] ascii = new int [sLenght];
        for(int i = 0; i < sLenght ; i++){   // while counting characters if less than the length add one
            char character = s.charAt(i); // start on the first character
            ascii[i] = (int) character; //convert the first character
        }

        int ascii2byteLen = 0 ;
        byte [] ascii2byte = {} ;
        for (int i = 0 ; i < ascii.length ; i++) {
            ascii2byte = Arrays.copyOf(ascii2byte, ascii2byteLen+2);
            ByteBuffer b = ByteBuffer.allocate(4);
            b.putInt(ascii[i]);
            byte[] result = b.array();
            System.arraycopy(result, 2, ascii2byte, ascii2byte.length- 2 , 2);
            ascii2byteLen = ascii2byte.length;
        }
        return ascii2byte ;
    }
}
