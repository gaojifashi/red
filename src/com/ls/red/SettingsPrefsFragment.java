package com.ls.red;

import com.ls.red.R;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class SettingsPrefsFragment extends PreferenceFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferencescreen);
	}
}
