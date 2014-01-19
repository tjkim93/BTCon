package com.tj.android.btrobot;

import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.tj.android.btrobot.R;
import com.tj.android.btrobot.BTConApp;

public class BTConConfig extends Activity implements SeekBar.OnSeekBarChangeListener {
    private final static String TAG = "BTConConfig";
    private static String mBTMac;

    BTConApp app;
    Spinner  stickMode;
    TextView textMacAddress;
    CheckBox checkThrottle;
    CheckBox checkAileron;
    CheckBox checkElevator;
    CheckBox checkRudder;
    SeekBar  seekSensitivity;
    TextView textSensitivity;
    
    SeekBar  seekTakeOff;
    TextView textTakeOff;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.btcon_config);
        
        app =  (BTConApp)getApplication();

        stickMode      = (Spinner) findViewById(R.id.spinner_stickmode);
        textMacAddress = (TextView) findViewById(R.id.textViewMacAddress);
        checkThrottle  = (CheckBox) findViewById(R.id.checkBoxRevThrottle);
        checkAileron   = (CheckBox) findViewById(R.id.checkBoxRevAileron);
        checkElevator  = (CheckBox) findViewById(R.id.checkBoxRevElevator);
        checkRudder    = (CheckBox) findViewById(R.id.checkBoxRevRudder);
        seekSensitivity = (SeekBar) findViewById(R.id.seekBarSensitivity);
        textSensitivity = (TextView)findViewById(R.id.textViewSensitivity);
        seekSensitivity.setMax(10);
        seekSensitivity.setOnSeekBarChangeListener(this);
        
        seekTakeOff = (SeekBar) findViewById(R.id.seekBarTakeOff);
        textTakeOff = (TextView)findViewById(R.id.textViewTakeOff);
        seekTakeOff.setMax(30);
        seekTakeOff.setOnSeekBarChangeListener(this);
        
        stickMode.setOnItemSelectedListener(
                new OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
//                        showToast("Spinner1: position=" + position + " id=" + id);
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
//                        showToast("Spinner1: unselected");
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        
        stickMode.setSelection(app.m_nStickMode);
        textMacAddress.setText(app.m_strMacAddress);
        checkThrottle.setChecked(app.m_bRevThrottle);
        checkAileron.setChecked(app.m_bRevAileron);
        checkElevator.setChecked(app.m_bRevElevator);
        checkRudder.setChecked(app.m_bRevRudder);
        seekSensitivity.setProgress(app.m_nSensitivity);
        textSensitivity.setText(String.format("%d", app.m_nSensitivity));
        seekTakeOff.setProgress(app.m_nTakeOff);
        textTakeOff.setText(String.format("%d", app.m_nTakeOff));
        
        mBTMac = app.m_strMacAddress;
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        SaveSettings();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // Definition of the one requestCode we use for receiving results.
    static final private int GET_CODE = 0;

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	Log.e(TAG, "Activitu result" + requestCode + ", result code" + resultCode);
    	if (requestCode == GET_CODE) {
	    	if (data != null) {
	    		mBTMac = data.getAction();
	    		if (textMacAddress != null) {
	    			textMacAddress.setText(mBTMac);
	    			app.m_strMacAddress = mBTMac;
	    		}
	    	    Log.e(TAG, "RESULT " + mBTMac);
	    	}
    	}
    }

	public void SelectBTdevice(View v) {
		Intent intent = new Intent(this, BTConSetting.class);
		startActivityForResult(intent, GET_CODE);
	}

	public void SaveSettings() {
		app.m_nStickMode    = stickMode.getSelectedItemPosition();
		app.m_strMacAddress = mBTMac;
		app.m_bRevThrottle  = checkThrottle.isChecked();
		app.m_bRevAileron   = checkAileron.isChecked();
		app.m_bRevElevator  = checkElevator.isChecked();
		app.m_bRevRudder    = checkRudder.isChecked();
		app.m_nSensitivity  = seekSensitivity.getProgress();
		app.m_nTakeOff      = seekTakeOff.getProgress();
		app.SaveSettings();
	}
	
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
    	String strProg = String.format("%d", progress);
    	
    	switch (seekBar.getId())
    	{
    	case R.id.seekBarTakeOff:
    		textTakeOff.setText(strProg);
    		Log.e(TAG, "Takeoff : " + progress + " ," + fromTouch);
    		break;
    	case R.id.seekBarSensitivity:
    		textSensitivity.setText(strProg);
    		Log.e(TAG, "sensitivity : " + progress + " ," + fromTouch);
    		break;
    	}
    }
    
    public void onStartTrackingTouch(SeekBar seekBar) {
        //mTrackingText.setText(getString(R.string.seekbar_tracking_on));
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        //mTrackingText.setText(getString(R.string.seekbar_tracking_off));
    }    
}
