package com.tj.android.btrobot;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

public class BTConApp extends Application {
	public String TAG = "BTConApp";
	
	private static String STICKMODE = "STICKMODE";
	public int m_nStickMode;
	
	private static String MACADDERSS = "MACADDERSS";
	public String m_strMacAddress = "";
	
	private static String REVTHROTTLE = "REVTHROTTLE";
	public boolean m_bRevThrottle = false;

	private static String REVAILERON = "REVAILERON";
	public boolean m_bRevAileron = false;
	
	private static String REVELEVATOR = "REVELEVATOR";
	public boolean m_bRevElevator = false;
	
	private static String REVRUDDER = "REVRUDDER";
	public boolean m_bRevRudder = false;
	
	private static String TRIM_THR = "TRIM_THR";
	public int  m_nTrimThrottle = 0;

	private static String TRIM_AIL = "TRIM_AIL";
	public int  m_nTrimAileron  = 127;
	
	private static String TRIM_ELE = "TRIM_ELE";
	public int  m_nTrimElevator = 127;
	
	private static String TRIM_RUD = "TRIM_RUD";
	public int	m_nTrimRudder   = 127;
	
	private static String SENSOR_SENSITIVITY = "SENSOR_SENSITIVITY";
	public int	m_nSensitivity   = 0;
	
	
	private SharedPreferences m_spBTCon;
	private Editor m_editorBTCon;
	
	@Override
	public void onCreate() {
		super.onCreate();

		Log.e(TAG, "HERE !!!");
		m_spBTCon = PreferenceManager.getDefaultSharedPreferences(this);
		m_editorBTCon = m_spBTCon.edit();
		ReadSettings();
	}
	
	public void ReadSettings() {
		m_nStickMode    = m_spBTCon.getInt(STICKMODE, 0);
		m_strMacAddress = m_spBTCon.getString(MACADDERSS, "");
		m_bRevThrottle  = m_spBTCon.getBoolean(REVTHROTTLE, false);
		m_bRevAileron   = m_spBTCon.getBoolean(REVAILERON, false);
		m_bRevElevator  = m_spBTCon.getBoolean(REVELEVATOR, false);
		m_bRevRudder    = m_spBTCon.getBoolean(REVRUDDER, false);
		m_nTrimThrottle = m_spBTCon.getInt(TRIM_THR, 0);
		m_nTrimAileron  = m_spBTCon.getInt(TRIM_AIL, 127);
		m_nTrimElevator = m_spBTCon.getInt(TRIM_ELE, 127);
		m_nTrimRudder   = m_spBTCon.getInt(TRIM_RUD, 127);
		m_nSensitivity  = m_spBTCon.getInt(SENSOR_SENSITIVITY, 0);
	}

	public void SaveSettings() {
		m_editorBTCon.putInt(STICKMODE, m_nStickMode);
		m_editorBTCon.putString(MACADDERSS, m_strMacAddress);
		m_editorBTCon.putBoolean(REVTHROTTLE, m_bRevThrottle);
		m_editorBTCon.putBoolean(REVAILERON, m_bRevAileron);
		m_editorBTCon.putBoolean(REVELEVATOR, m_bRevElevator);
		m_editorBTCon.putBoolean(REVRUDDER, m_bRevRudder);
		m_editorBTCon.putInt(TRIM_THR, m_nTrimThrottle);
		m_editorBTCon.putInt(TRIM_AIL, m_nTrimAileron);
		m_editorBTCon.putInt(TRIM_ELE, m_nTrimElevator);
		m_editorBTCon.putInt(TRIM_RUD, m_nTrimRudder);
		m_editorBTCon.putInt(SENSOR_SENSITIVITY, m_nSensitivity);
		m_editorBTCon.commit();
	}

	@Override
	public void onTerminate() {
		super.onTerminate();

	}	
}
