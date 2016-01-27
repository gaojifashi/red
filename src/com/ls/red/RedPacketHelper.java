package com.ls.red;

import com.ls.red.R;

import android.app.Notification;
import android.content.Context;
import android.os.Parcelable;
import android.view.accessibility.AccessibilityEvent;


class RedPacketHelper {
	public static boolean isRedPacket(AccessibilityEvent event, Context context) {
		if (event != null && event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
			Parcelable parcelable = event.getParcelableData();
			if (parcelable != null && parcelable instanceof Notification) {
				Notification notification = (Notification)parcelable;
				if (notification.tickerText != null) {
					String[] strs = notification.tickerText.toString().split(":");
					if (strs.length > 1 && strs[1].trim().startsWith(context.getString(R.string.wechat_prefix))) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
