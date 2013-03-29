package com.tj.android.btrobot;

//import com.tj.android.btrobot.R;

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
    
public class BTConMain extends Activity {
    private final static String TAG = "BTRobot";
    private static String mBTMac;
    private static View   mButtonController;
    private static View   mButtonRobotController;
    
    BTConApp app;
    
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    private static boolean mBluetoothEnabled = false;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        
        app =  (BTConApp)getApplication();
        
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        Log.e(TAG, "MAC " + app.m_strMacAddress + " len : " + app.m_strMacAddress.length());

        mButtonController = this.findViewById(R.id.buttonController);
        //mButtonRobotController = this.findViewById(R.id.buttonRobotController);
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
        	mBluetoothEnabled = false;
        	mBluetoothAdapter.enable();
        } else {
        	mBluetoothEnabled = true;
        }
    }
   
    @Override
    public synchronized void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        if (app.m_strMacAddress == null || app.m_strMacAddress.length() == 0) {
	        mButtonController.setEnabled(false);
	        //mButtonRobotController.setEnabled(false);
        } else {
        	mBTMac = app.m_strMacAddress;
        	mButtonController.setEnabled(true);
	        //mButtonRobotController.setEnabled(true);
        }
    }
    
    @Override
    public synchronized void onPause() {
        super.onPause();
        Log.e(TAG, "onPause");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy " + mBluetoothEnabled);
        if (mBluetoothEnabled == false) {
        	mBluetoothAdapter.disable();
        	Log.e(TAG, "BT disable !!!!");
        }
    }

	public void BTConConfigOnClick(View v) {
		Intent intent = new Intent(this, BTConConfig.class);
		startActivity(intent);
	}
	
	public void ControllerOnClick(View v) {
		Intent intent = new Intent(this, BTReceiverController.class);
		intent.putExtra("BT_MAC", mBTMac);
		startActivity(intent);
	}

	public void RobotControllerOnClick(View v) {
		Intent intent = new Intent(this, BTRobotController.class);
		Log.e(TAG, "Controller " + mBTMac);
		intent.putExtra("BT_MAC", mBTMac);
		startActivity(intent);
	}
	
	public void AboutOnClick(View v) {
		Intent intent = new Intent(this, About.class);
		startActivity(intent);
	}
	
	public void QuitOnClick(View v) {
		finish();
        return;
	}
   

}
