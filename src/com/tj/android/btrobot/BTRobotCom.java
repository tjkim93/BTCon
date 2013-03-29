package com.tj.android.btrobot;


import com.tj.android.btrobot.R;
import com.tj.android.btrobot.BMP180;
import com.tj.android.btrobot.BTSerialService;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.content.IntentFilter;
    
public class BTRobotCom {
    private final static String TAG = "BTRobot";

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    
    // Message format
    public static final String MSG_MOTOR_SET_VALUE2 = "i";
    public static final String MSG_MOTOR_SET_VALUE = "m";
    public static final String MSG_ADC_CFG = "c";
    public static final String MSG_ADC_GET_VALUE = "a";
    public static final String MSG_GPIO_INOUT_CFG = "g";
    public static final String MSG_GPIO_SET_VALUE = "o";
    public static final String MSG_GPIO_GET_VALUE = "s";
    public static final String MSG_BATT_GET_VALUE = "b";
    public static final String MSG_ADDR_WRITE = "w";
    public static final String MSG_ADDR_READ = "r";
    
    public static final byte SEND_LETTER_I2C                    = 'i';
    public static final byte SEND_LETTER_BMP180                 = 'b';
    public static final byte SEND_LETTER_FIRMWARE_VERSION       = 'v';
    public static final byte SEND_LETTER_CONSTANT               = 'c';
    public static final byte SEND_LETTER_TEMPERATURE            = 't';
    public static final byte SEND_LETTER_PRESSURE               = 'p';
    public static final byte SEND_LETTER_BATT                   = 'b';
    
    public static final byte RECEIVED_LETTER_BATT               = 'B';
    public static final byte RECEIVED_LETTER_I2C                = 'I';
    public static final byte RECEIVED_LETTER_BMP180             = 'B';
    public static final byte RECEIVED_LETTER_PRESSURE           = 'P';
    public static final byte RECEIVED_LETTER_TEMPERATURE        = 'T';
    public static final byte RECEIVED_LETTER_CONSTANT           = 'C';
    public static final byte RECEIVED_LETTER_FIRMWARE_VERSION   = 'V';
    public static final byte RECEIVED_LETTER_GPIO_CONFIG        = 'G';
    
    public static final byte SEND_LETTER_MOTOR                  = 'm';    

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    
    private byte [] mBuf = new byte[30];
    private int     mIndex = 0;
    private byte [] commandPacket = new byte[5];

    private boolean bluetoothPaired = false;
    private boolean timerStarted = false;
    
    // message type
    public String mMessageType;

    // Name of the connected device
    private String mConnectedDeviceName = null;

    // Member object for the chat services
    private BTSerialService mBTService = null;
    private BMP180 mPressureSensor;
    
    private Timer mRefreshTimer;
    private RefreshTask mRefreshTask;
    private Handler mTimerHandler;
    
    private Context mAppContext;
    
    class RefreshTask extends TimerTask {
        private int count = 0;
        public void run() {
            mTimerHandler.sendEmptyMessage(count);
            count++;
            if (count == 6)
                count = 0;
        }
    }

    private void startTimer() {
        Log.e(TAG, "startTimer");
        mRefreshTimer = new Timer();
        mRefreshTask = new RefreshTask();
        mRefreshTimer.schedule(mRefreshTask, 0, 100);
    }

    private void stopTimer() {
        Log.e(TAG, "stopTimer");
        if (mRefreshTimer != null) {
            mRefreshTimer.cancel();
            //mRefreshTimer.purge();
            mRefreshTimer = null;
        }
        mRefreshTask = null;
    }    

	public BTRobotCom(Context context) {
        mPressureSensor = new BMP180();	    
	    mAppContext     = context;
	    
		mTimerHandler   = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                case 0:
                    getTemperature();
                    break;
                case 3:
                    getBattery();
                    break;
                case 5:
                    getPressure();
                    break;
                }
            }
        };      
        
        if (mBTService == null)
        {
            // Initialize the BluetoothChatService to perform bluetooth connections
            mBTService = new BTSerialService(mAppContext, mHandler);
            mBTService.setIndexOfMessages(MESSAGE_STATE_CHANGE, MESSAGE_READ, MESSAGE_DEVICE_NAME, MESSAGE_TOAST);
            mBTService.setDeviceNameString(DEVICE_NAME);
            mBTService.setToastString(TOAST);
        }
	}
	
	public void pause() {
	    if (timerStarted == true)
	        stopTimer();
	}
	
	public void resume() {
	    if (timerStarted == true)
	        startTimer();
	}
	
    public void stop() {
    	stopTimer();
        if (mBTService != null)
            mBTService.stop();        
    }	
	
	public void connect(BluetoothDevice device)
	{
	    mBTService.connect(device);
	}
    
	public void finalize() {
        // Stop the Bluetooth chat services
        if (mBTService != null)
            mBTService.stop();
        stopTimer();
	}
    
    /*
     *******************************************************************************
     * BT Communication Routines 
     ******************************************************************************* 
    */
    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            short value = 0;

            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BTSerialService.STATE_CONNECTED:
//                    mTitle.setText(R.string.title_connected_to);
//                    mTitle.append(mConnectedDeviceName);
                    mIndex = 0;
                    //initI2C();
                    initDevice();
                    bluetoothPaired = true;
                    break;
                    
                case BTSerialService.STATE_CONNECTING:
//                    mTitle.setText(R.string.title_connecting);
                    break;
                    
                case BTSerialService.STATE_LISTEN:
                case BTSerialService.STATE_NONE:
//                    mTitle.setText(R.string.title_not_connected);
//                    stopTimer();
                    bluetoothPaired = false;
                    break;
                }
                break;
                
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(
                        mAppContext.getApplicationContext(),
                        mAppContext.getString(R.string.connected_to) + mConnectedDeviceName,
                        Toast.LENGTH_SHORT).show();
                break;
                
            case MESSAGE_TOAST:
                if (msg.getData().getString(TOAST).equals("unable connect")) {
                    Toast.makeText(mAppContext.getApplicationContext(),
                            mAppContext.getString(R.string.unable_connect),
                            Toast.LENGTH_SHORT).show();
                } else if (msg.getData().getString(TOAST)
                        .equals("connection lost")) {
                    Toast.makeText(mAppContext.getApplicationContext(),
                            mAppContext.getString(R.string.connection_lost),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mAppContext.getApplicationContext(),
                            msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
                            .show();
                }
                break;
                
            case SEND_LETTER_FIRMWARE_VERSION:
                if (mBTService.getState() != BTSerialService.STATE_CONNECTED) {
                    return;
                }    
     
                commandPacket[0] = SEND_LETTER_FIRMWARE_VERSION;
                commandPacket[1] = 0; 
                commandPacket[2] = 0; 
                commandPacket[3] = 0; 
                commandPacket[4] = 0; 
     
                mBTService.write(commandPacket);
                break;
                
            case SEND_LETTER_BATT:
                if (commandPacket[0] != 0) {
                    return;
                }

                commandPacket[0] = SEND_LETTER_BATT;

                mBTService.write(commandPacket);
                break;
                
            case SEND_LETTER_I2C:

                if (msg.arg1 == SEND_LETTER_I2C) {
                    commandPacket[0] = SEND_LETTER_I2C;
                    commandPacket[1] = SEND_LETTER_I2C;
                }

                if (msg.arg1 == SEND_LETTER_BMP180) {
                    commandPacket[0] = SEND_LETTER_I2C;
                    commandPacket[1] = SEND_LETTER_BMP180;
                }

                if (msg.arg1 == SEND_LETTER_CONSTANT) {
                    commandPacket[0] = SEND_LETTER_I2C;
                    commandPacket[1] = SEND_LETTER_CONSTANT;
                    commandPacket[2] = 0x00;
                    commandPacket[3] = (byte) msg.arg2;
                }

                if (msg.arg1 == SEND_LETTER_TEMPERATURE) {
                    if (commandPacket[0] != 0) {
                        //Log.e(TAG, "returned");
                        return;
                    }
                    commandPacket[0] = SEND_LETTER_I2C;
                    commandPacket[1] = SEND_LETTER_TEMPERATURE;
                }

                if (msg.arg1 == SEND_LETTER_PRESSURE) {
                    if (commandPacket[0] != 0) {
                        //Log.e(TAG, "returned");
                        return;
                    }
                    commandPacket[0] = SEND_LETTER_I2C;
                    commandPacket[1] = SEND_LETTER_PRESSURE;
                }

                mBTService.write(commandPacket);
                break;
                
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;

                if (readBuf[0] == commandPacket[0] + ('A' - 'a')) {
                    commandPacket[0] = 0;
                }

                switch(readBuf[0]) {
                case RECEIVED_LETTER_BATT:
                {
                    value = (short) (readBuf[3] << 8);
                    value |= (short) (readBuf[4] & 0x00ff);
                    updateBattery(value);
                }
                    break;

                case RECEIVED_LETTER_FIRMWARE_VERSION: 
                {   
                    value = (short) (readBuf[1] << 8);
                    value |= (short) readBuf[2];
                    short major = value;
                    value = (short) (readBuf[3] << 8);
                    value |= (short) readBuf[4];
                    short minior = value;
                    
                    updateVersion(major, minior);
                }
                    break;
                    
                case RECEIVED_LETTER_I2C:
                    if (readBuf[1] == RECEIVED_LETTER_I2C) {
                        initBMP180();
                    } else if (readBuf[1] == RECEIVED_LETTER_BMP180) {
                        getCalibrationData(mPressureSensor.getRegisterAddress());
                    } else if (readBuf[1] == RECEIVED_LETTER_TEMPERATURE) {
                        ByteBuffer bb = ByteBuffer.allocate(4);
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        bb.put(readBuf[2]);
                        bb.put(readBuf[3]);
                        bb.rewind();
                        if (mPressureSensor.isAvailable()) {
                            long temperature = mPressureSensor.calculateTrueTemperature(bb.getInt());
                            updateTemperature(temperature);
                        }
                    } else if (readBuf[1] == RECEIVED_LETTER_PRESSURE) {
                        ByteBuffer bb = ByteBuffer.allocate(4);
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        bb.put(readBuf[2]);
                        bb.put(readBuf[3]);
                        bb.rewind();
                        long pressure = 0;
                        if (mPressureSensor.isAvailable()) {
                            pressure = mPressureSensor.calculateTruePressure(bb.getInt());
                            updatePressure(pressure);
                        }
                        long altitude = mPressureSensor.calculateAltitude(pressure);
                        updateAltitude(altitude);
                    } else if (readBuf[1] == RECEIVED_LETTER_CONSTANT) {
                        if (updateConstant(readBuf)) {
                            mPressureSensor.setNextRegisterAddress();
                            getCalibrationData(mPressureSensor.getRegisterAddress());
                        }
                    }
                    break;
                }
            default:
                break;
            }
        }
    };
    
    private void initDevice() {
        Log.i(TAG, "initDevice");
        initI2C();
        initBMP180();
        getCalibrationData(mPressureSensor.getRegisterAddress());
    }
 
    private void getCalibrationData(byte addr) {
        Message msg = Message.obtain(mHandler, SEND_LETTER_I2C, SEND_LETTER_CONSTANT, addr);
        mHandler.sendMessage(msg);
    }

    private void initI2C() {
        Log.i(TAG, "initI2C");
        Message msg = Message.obtain(mHandler, SEND_LETTER_I2C, SEND_LETTER_I2C, 0);
        mHandler.sendMessage(msg);
    }

    private void initBMP180() {
        Log.i(TAG, "initBMP180");
        Message msg = Message.obtain(mHandler, SEND_LETTER_I2C, SEND_LETTER_BMP180, 0);
        mHandler.sendMessage(msg);
    }
    
    private void getVersion() {
        Log.i(TAG, "getVersion");
        Message msg = Message.obtain(mHandler, SEND_LETTER_FIRMWARE_VERSION, 0, 0);
        mHandler.sendMessage(msg);
    }
    
    private void getBattery() {
        //Log.i(TAG, "getBattery");
        Message msg = Message.obtain(mHandler, SEND_LETTER_BATT, 0, 0);
        mHandler.sendMessage(msg);
    }

    private void getTemperature() {
        //Log.i(TAG, "getTemperature");
        Message msg = Message.obtain(mHandler, SEND_LETTER_I2C, SEND_LETTER_TEMPERATURE, 0);
        mHandler.sendMessage(msg);
    }

    private void getPressure() {
        //Log.i(TAG, "getPressure");
        Message msg = Message.obtain(mHandler, SEND_LETTER_I2C, SEND_LETTER_PRESSURE, 0);
        mHandler.sendMessage(msg);
    }
    
    private void updateVersion(short major, short minior) {
//        TextView tv = (TextView)findViewById(R.id.textView_fw_version);
//        tv.setText("Firmware Version : " + major + "." + minior);
        Log.e(TAG, "Major : " + major);
        Log.e(TAG, "Minior : " + minior);
    }

    private void updateBattery(short value) {
//        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
//        progressBar.setProgress(value - 520);
        //Log.e(TAG, "Battery : " + (value - 520));
    }

    private void updateTemperature(long value) {
//        TextView tv = (TextView)findViewById(R.id.textView_temperature);
//        tv.setText(String.format("%.1f", value * 0.1));
        //Log.e(TAG, "Temp : " + String.format("%.1f", value * 0.1));
    }

    private void updatePressure(long value) {
//        TextView tv = (TextView)findViewById(R.id.textView_pressure);
//        tv.setText(String.format("%.2f", value * 0.01));
        //Log.e(TAG, "Pressure : " + String.format("%.2f", value * 0.01));
    }

    private void updateAltitude(long value) {
//        TextView tv = (TextView)findViewById(R.id.textView_altitude);
//        tv.setText("" + value);

//        tv = (TextView)findViewById(R.id.textView_altitude_feet);

//        tv.setText(String.format("%.0f", value * 3.2808399));
        //Log.e(TAG, "Altitude : " + String.format("%.0f", value * 3.2808399));
    }
    
    private boolean updateConstant(byte [] value) {
        mBuf[mIndex++] = value[2];
        mBuf[mIndex++] = value[3];

        if (mIndex == 22) {
        	mIndex = 0;
            fillCalibarationData(mBuf);
            getVersion();
            startTimer();
            timerStarted = true;
            return false;
        } else
            return true;
    }
    
    private void fillCalibarationData(byte [] buf) {
        ByteBuffer bb2short = ByteBuffer.allocate(2);
        bb2short.order(ByteOrder.LITTLE_ENDIAN);
        bb2short.put(buf[0]);
        bb2short.put(buf[1]);
        short AC1 = bb2short.getShort(0);
        Log.e(TAG, String.format("AC1 = %d", AC1));

        bb2short.clear();
        bb2short.put(buf[2]);
        bb2short.put(buf[3]);
        short AC2 = bb2short.getShort(0);
        Log.e(TAG, String.format("AC2 = %d", AC2));

        bb2short.clear();
        bb2short.put(buf[4]);
        bb2short.put(buf[5]);
        short AC3 = bb2short.getShort(0);
        Log.e(TAG, String.format("AC3 = %d", AC3));

        ByteBuffer bb2int= ByteBuffer.allocate(4);
        bb2int.order(ByteOrder.LITTLE_ENDIAN);
        bb2int.put(buf[6]);
        bb2int.put(buf[7]);
        bb2int.rewind();
        int AC4 = bb2int.getInt();
        Log.e(TAG, String.format("AC4 = %d", AC4));

        bb2int.clear();
        bb2int.put(buf[8]);
        bb2int.put(buf[9]);
        bb2int.rewind();
        int AC5 = bb2int.getInt();
        Log.e(TAG, String.format("AC5 = %d", AC5));

        bb2int.clear();
        bb2int.put(buf[10]);
        bb2int.put(buf[11]);
        bb2int.rewind();
        int AC6 = bb2int.getInt();
        Log.e(TAG, String.format("AC6 = %d", AC6));

        bb2short.clear();
        bb2short.put(buf[12]);
        bb2short.put(buf[13]);
        short B1 = bb2short.getShort(0);
        Log.e(TAG, String.format("B1 = %d", + B1));

        bb2short.clear();
        bb2short.put(buf[14]);
        bb2short.put(buf[15]);
        short B2 = bb2short.getShort(0);
        Log.e(TAG, String.format("B2 = %d", B2));

        bb2short.clear();
        bb2short.put(buf[16]);
        bb2short.put(buf[17]);
        short MB = bb2short.getShort(0);
        Log.e(TAG, String.format("MB = %d", MB));
        
        bb2short.clear();
        bb2short.put(buf[18]);
        bb2short.put(buf[19]);
        short MC = bb2short.getShort(0);
        Log.e(TAG, String.format("MC = %d", MC));
        
        bb2short.clear();
        bb2short.put(buf[20]);
        bb2short.put(buf[21]);
        short MD = bb2short.getShort(0);
        Log.e(TAG, String.format("MD = %d", MD));
        
        mPressureSensor.setCalibrationData(AC1, AC2, AC3, AC4, AC5, AC6, B1, B2, MB, MC, MD);
    }    
    
	public void sendMessageToMotor(int id, int value) {
		//Return to the previous version(revision 35), the voice recognition works, with the new code does not work(revision 40).
	   /* Message msg = Message.obtain(mHandler, SEND_LETTER_MOTOR, id, value); 
	    mHandler.sendMessage(msg);
	    */
		//Log.e(TAG, "sendMessageToMotor(" + id + ", " + value + ")");
		if (mBTService.getState() != BTSerialService.STATE_CONNECTED) {
            return;
        } 
		
		commandPacket[0] = SEND_LETTER_MOTOR;
		commandPacket[1] = 0;
		commandPacket[2] = (byte) id;
		if (id == 1)
			value *= -1;
		if (value > 0) {
			commandPacket[3] = '-';
			commandPacket[4] = (byte) value;
		} else {
			commandPacket[3] = '+';
			commandPacket[4] = (byte) (value * -1);
		}
		mBTService.write(commandPacket);
	}
	
	private final static int LEFT_WHEEL = 2;
	private final static int RIGHT_WHEEL = 1;
	
	public void setMotorValue(int id, int value) {
		sendMessageToMotor(id, value);
		
		if (id == LEFT_WHEEL)
			sendMessageToMotor(4, value);
		if (id == RIGHT_WHEEL)
			sendMessageToMotor(3, value);
		sendMessageToMotor(5, value);
	}
}
