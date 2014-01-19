package com.tj.android.btrobot;


import java.util.Timer;
import java.util.TimerTask;

import com.tj.android.btrobot.BTReceiverController.RefreshTask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

public class BluezIPega {
	private static final String TAG = "BluezIPega";
	
	public static final String SESSION_ID = "com.hexad.bluezime.sessionid";
	
	public static final String EVENT_KEYPRESS = "com.hexad.bluezime.keypress";
	public static final String EVENT_KEYPRESS_KEY = "key";
	public static final String EVENT_KEYPRESS_ACTION = "action";

	public static final String EVENT_DIRECTIONALCHANGE = "com.hexad.bluezime.directionalchange";
	public static final String EVENT_DIRECTIONALCHANGE_DIRECTION = "direction";
	public static final String EVENT_DIRECTIONALCHANGE_VALUE = "value";

	public static final String EVENT_CONNECTED = "com.hexad.bluezime.connected";
	public static final String EVENT_CONNECTED_ADDRESS = "address";

	public static final String EVENT_DISCONNECTED = "com.hexad.bluezime.disconnected";
	public static final String EVENT_DISCONNECTED_ADDRESS = "address";

	public static final String EVENT_ERROR = "com.hexad.bluezime.error";
	public static final String EVENT_ERROR_SHORT = "message";
	public static final String EVENT_ERROR_FULL = "stacktrace";
	
	public static final String REQUEST_STATE = "com.hexad.bluezime.getstate";

	public static final String REQUEST_CONNECT = "com.hexad.bluezime.connect";
	public static final String REQUEST_CONNECT_ADDRESS = "address";
	public static final String REQUEST_CONNECT_DRIVER = "driver";
	
	public static final String REQUEST_DISCONNECT = "com.hexad.bluezime.disconnect";
	
	public static final String EVENT_REPORTSTATE = "com.hexad.bluezime.currentstate";
	public static final String EVENT_REPORTSTATE_CONNECTED = "connected";
	public static final String EVENT_REPORTSTATE_DEVICENAME = "devicename";
	public static final String EVENT_REPORTSTATE_DISPLAYNAME = "displayname";
	public static final String EVENT_REPORTSTATE_DRIVERNAME = "drivername";
	
	public static final String REQUEST_FEATURECHANGE = "com.hexad.bluezime.featurechange";
	public static final String REQUEST_FEATURECHANGE_RUMBLE = "rumble"; //Boolean, true=on, false=off
	public static final String REQUEST_FEATURECHANGE_LEDID = "ledid"; //Integer, LED to use 1-4 for Wiimote
	public static final String REQUEST_FEATURECHANGE_ACCELEROMETER = "accelerometer"; //Boolean, true=on, false=off
	
	public static final String REQUEST_CONFIG = "com.hexad.bluezime.getconfig";
	
	public static final String EVENT_REPORT_CONFIG = "com.hexad.bluezime.config";
	public static final String EVENT_REPORT_CONFIG_VERSION = "version";
	public static final String EVENT_REPORT_CONFIG_DRIVER_NAMES = "drivernames";
	public static final String EVENT_REPORT_CONFIG_DRIVER_DISPLAYNAMES = "driverdisplaynames";

	
	private static final String BLUEZ_IME_PACKAGE = "com.hexad.bluezime";
	private static final String BLUEZ_IME_SERVICE = "com.hexad.bluezime.BluezService";
	
	//These are from API level 9
	public static final int KEYCODE_BUTTON_A = 0x60;
	public static final int KEYCODE_BUTTON_B = 0x61;
	public static final int KEYCODE_BUTTON_C = 0x62;
	public static final int KEYCODE_BUTTON_X = 0x63;
	public static final int KEYCODE_BUTTON_Y = 0x64;
	public static final int KEYCODE_BUTTON_Z = 0x65;

	//A string used to ensure that apps do not interfere with each other
	public static final String SESSION_NAME = "BTCon-Bluez-Interface";
	public static final String DRIVER_NAME = "ipega";
	
	private Context mContext;
	private String[] mStrDriverNames;
	private String[] mStrDriverDispNames;
	private boolean m_bConnected  = false;
	private boolean m_bReqConnect = false;
	private boolean m_bRegistered = false;
	private Handler m_Handler     = null;
	
    private Timer       		m_tmrStick;
    private RefreshStickTask 	m_tskStick;
    private Handler     		m_hndStick = null;
    
    private int					mIntStickValue;
    private int					mIntStickDir;
    private int					mIntX1 = 0;
    private int					mIntY1 = 0;
    private int					mIntX2 = 0;
    private int					mIntY2 = 0;
	
	public BluezIPega(Context context, Handler handler) {
		this.mContext = context;
		this.m_Handler = handler;
		
		context.registerReceiver(stateCallback, new IntentFilter(EVENT_REPORT_CONFIG));
		context.registerReceiver(stateCallback, new IntentFilter(EVENT_REPORTSTATE));
		context.registerReceiver(stateCallback, new IntentFilter(EVENT_CONNECTED));
		context.registerReceiver(stateCallback, new IntentFilter(EVENT_DISCONNECTED));
		context.registerReceiver(stateCallback, new IntentFilter(EVENT_ERROR));
        
		context.registerReceiver(statusMonitor, new IntentFilter(EVENT_DIRECTIONALCHANGE));
		context.registerReceiver(statusMonitor, new IntentFilter(EVENT_KEYPRESS));
		
        //Request config, not present in version < 9
        Intent serviceIntent = new Intent(REQUEST_CONFIG);
        serviceIntent.setClassName(BLUEZ_IME_PACKAGE, BLUEZ_IME_SERVICE);
        serviceIntent.putExtra(SESSION_ID, SESSION_NAME);
        context.startService(serviceIntent); 
        
        //Request device connection state
        serviceIntent = new Intent(REQUEST_STATE);
        serviceIntent.setClassName(BLUEZ_IME_PACKAGE, BLUEZ_IME_SERVICE);
        serviceIntent.putExtra(SESSION_ID, SESSION_NAME);
        context.startService(serviceIntent);
        
        m_bRegistered = true;
        
        m_hndStick = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	            super.handleMessage(msg);
	            if (!m_bConnected)
	            	return;
	            
	            switch (msg.what) {
	            case 1:
	            	msg = Message.obtain(m_Handler, 1, mIntX1, mIntY1);
					m_Handler.sendMessage(msg);
					msg = Message.obtain(m_Handler, 2, mIntX2, mIntY2);
					m_Handler.sendMessage(msg);
	            	break;
	            }
	        }
		};
        
        m_tmrStick = new Timer();
        m_tskStick = new RefreshStickTask();
        m_tmrStick.schedule(m_tskStick, 0, 50);
	}

    class RefreshStickTask extends TimerTask {
        public void run() {
        	m_hndStick.sendEmptyMessage(1);
        }
    }	
	
	public void stop() {
		Log.e(TAG, "stop !!!");
/*		
		StringBuffer stacktrace = new StringBuffer();
		StackTraceElement[] stackTrace = new Exception().getStackTrace();
		for(int x=0; x<stackTrace.length; x++)
		{
		 stacktrace.append(stackTrace[x].toString() + " ");
		}
		Log.e(TAG, "stacktrace");
		Log.e(TAG, stacktrace.toString());		
*/
		
		if (m_bReqConnect)
			connect(false);
		
		try{ Thread.sleep(100); }catch(Exception e){}
		
		if (m_bRegistered) {
			mContext.unregisterReceiver(stateCallback);
			mContext.unregisterReceiver(statusMonitor);
			m_bRegistered = false;
		}
		
		m_tmrStick.cancel();
		m_tmrStick = null;
		m_tskStick = null;
	}

	private void connect(boolean bConnect) {
        if (!bConnect) {
	        Intent serviceIntent = new Intent(REQUEST_DISCONNECT);
	        serviceIntent.setClassName(BLUEZ_IME_PACKAGE, BLUEZ_IME_SERVICE);
	        serviceIntent.putExtra(SESSION_ID, SESSION_NAME);
	        mContext.startService(serviceIntent);
	        Log.e(TAG, "Disconnect !!!");
		} else {
	        Intent serviceIntent = new Intent(REQUEST_CONNECT);
	        serviceIntent.setClassName(BLUEZ_IME_PACKAGE, BLUEZ_IME_SERVICE);
	        serviceIntent.putExtra(SESSION_ID, SESSION_NAME);
	        //serviceIntent.putExtra(REQUEST_CONNECT_ADDRESS, m_mac.getText().toString());
	        serviceIntent.putExtra(REQUEST_CONNECT_DRIVER, DRIVER_NAME);
	        mContext.startService(serviceIntent);
	        Log.e(TAG, "Connect !!!");
		}
        m_bReqConnect = bConnect;
	}

    private void populateDriverBox(String[] keys, String[] displays) {
    	mStrDriverNames = keys;
    	mStrDriverDispNames = displays;
    }	

    private BroadcastReceiver stateCallback = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction() == null)
				return;
			
			//Filter everything that is not related to this session
			if (!SESSION_NAME.equals(intent.getStringExtra(SESSION_ID)))
				return;
			
			if (intent.getAction().equals(EVENT_REPORT_CONFIG)) {
				Toast.makeText(mContext.getApplicationContext(), "Bluez-IME version " + intent.getIntExtra(EVENT_REPORT_CONFIG_VERSION, 0), Toast.LENGTH_SHORT).show();				
				populateDriverBox(intent.getStringArrayExtra(EVENT_REPORT_CONFIG_DRIVER_NAMES), intent.getStringArrayExtra(EVENT_REPORT_CONFIG_DRIVER_DISPLAYNAMES));
				connect(true);
			} else if (intent.getAction().equals(EVENT_REPORTSTATE)) {
				m_bConnected = intent.getBooleanExtra(EVENT_REPORTSTATE_CONNECTED, false);
				//After we connect, we rumble the device for a second if it is supported
				if (m_bConnected) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							Intent req = new Intent(REQUEST_FEATURECHANGE);
							req.putExtra(REQUEST_FEATURECHANGE_LEDID, 2);
							req.putExtra(REQUEST_FEATURECHANGE_RUMBLE, true);
							mContext.startService(req);
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
							}
							req.putExtra(REQUEST_FEATURECHANGE_LEDID, 1);
							req.putExtra(REQUEST_FEATURECHANGE_RUMBLE, false);
							mContext.startService(req);
						}
					});
				}
			} else if (intent.getAction().equals(EVENT_CONNECTED)) {
				Toast.makeText(mContext.getApplicationContext(), "bluezime_connected", Toast.LENGTH_SHORT).show();
				m_bConnected = true;
			} else if (intent.getAction().equals(EVENT_DISCONNECTED)) {
				Toast.makeText(mContext.getApplicationContext(), "bluezime_disconnected", Toast.LENGTH_SHORT).show();
				m_bConnected = false;
			} else if (intent.getAction().equals(EVENT_ERROR)) {
				Toast.makeText(mContext.getApplicationContext(), "Error: " + intent.getStringExtra(EVENT_ERROR_SHORT), Toast.LENGTH_SHORT).show();
				m_bConnected = false;
			}
		}
	};
	
	private BroadcastReceiver statusMonitor = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction() == null)
				return;
			if (!SESSION_NAME.equals(intent.getStringExtra(SESSION_ID)))
				return;

			Message msg;
			
			if (intent.getAction().equals(EVENT_DIRECTIONALCHANGE)) {
				int value = intent.getIntExtra(EVENT_DIRECTIONALCHANGE_VALUE, 0);
				int dir   = intent.getIntExtra(EVENT_DIRECTIONALCHANGE_DIRECTION, 100);
				
				switch (dir) {
					case 0:
						//X1
						mIntX1 = value;
						break;
					case 1:
						// Y1
						mIntY1 = value;
						break;
					case 2:
						// X2
						mIntX2 = value;
						break;
					case 3:
						// Y2
						mIntY2 = value;
						break;
				}
				//Log.d(TAG, String.format("DIR : %d : %d", mIntStickDir, mIntStickValue));
			} else if (intent.getAction().equals(EVENT_KEYPRESS)) {
				int key = intent.getIntExtra(EVENT_KEYPRESS_KEY, 0);
				int action = intent.getIntExtra(EVENT_KEYPRESS_ACTION, 100);
				
				msg = Message.obtain(m_Handler, 3, key, action);
				m_Handler.sendMessage(msg);

				//Log.d(TAG, String.format("KEY : %d : %s", key, action == KeyEvent.ACTION_DOWN ? "DOWN" : "UP"));
			}
		}
	};	
}
