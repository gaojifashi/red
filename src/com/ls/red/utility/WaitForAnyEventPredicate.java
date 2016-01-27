package com.ls.red.utility;

import android.view.accessibility.AccessibilityEvent;


public class WaitForAnyEventPredicate implements MyAccessibilityEventFilter {
	private int mMask;
	
	public WaitForAnyEventPredicate(int mask) {
		mMask = mask;
	}
	
	@Override
	public boolean accept(AccessibilityEvent event) {
		// check current event int list
		if ((event.getEventType() & mMask) != 0) {
			return true;
		}
		return false;
	}

}
