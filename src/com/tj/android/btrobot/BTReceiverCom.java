package com.tj.android.btrobot;

import com.tj.android.btrobot.R;
import com.tj.android.btrobot.BTSerialService;

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
    
public class BTReceiverCom {
    private final static String TAG = "BTReceiver";

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    private boolean bluetoothPaired = false;
    
    // message type
    public String mMessageType;

    // Name of the connected device
    private String mConnectedDeviceName = null;

    // Member object for the chat services
    private BTSerialService mBTService = null;
    private Context mAppContext;

	public BTReceiverCom(Context context) {
	    mAppContext     = context;
        
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

	}
	
	public void resume() {

	}
	
    public void stop() {
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
	}
	
	public void write(byte[] out) {
		mBTService.write(out);
	}
	
	public boolean isConnected() {
		return bluetoothPaired;
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
            }
        }
    };
}