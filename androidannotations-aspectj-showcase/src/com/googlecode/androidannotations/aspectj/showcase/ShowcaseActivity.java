package com.googlecode.androidannotations.aspectj.showcase;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EActivity;

@EActivity(R.layout.main)
public class ShowcaseActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		Log.i("Activity", "Will now call init()");
		init();
	}
	
	@AfterViews
	void init() {
		Log.i("Activity", "in init()");
	}
}