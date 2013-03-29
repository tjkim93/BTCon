package com.tj.android.btrobot;
import com.tj.android.btrobot.R;

import android.app.Activity;
import android.os.Bundle;

public class About extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		setTitle(R.string.app_name);
	}
}
