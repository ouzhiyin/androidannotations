package com.googlecode.androidannotations.aspectj.showcase;

import android.app.Activity;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EActivity;

@EActivity(R.layout.main)
public class ShowcaseActivity extends Activity {

	@AfterViews
	void init() {

	}
}