package com.ls.red.utility;

import android.view.accessibility.AccessibilityEvent;

public interface MyAccessibilityEventFilter {
	public boolean accept(AccessibilityEvent event);
}
