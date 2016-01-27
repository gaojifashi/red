package com.ls.red;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import com.ls.red.R;
import com.ls.red.utility.IExecutor;
import com.ls.red.utility.MyAccessibilityEventFilter;
import com.ls.red.utility.WaitForAnyEventPredicate;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

public class MyAccessibilityService extends AccessibilityService {
	private final static String LOG_TAG = MyAccessibilityService.class.getName();
	
	private final Object mLock = new Object();
	
	private long mLastEventTimeMillis;
	private final ArrayList<AccessibilityEvent> mEventArray = new ArrayList<AccessibilityEvent>();
	private boolean mWaitingForEventDelivery = false;
	private volatile boolean mMonitoring;
	private Thread runnerThread;
	private IExecutor mExecutor;
	
	@Override
	protected void onServiceConnected() {
		// TODO Auto-generated method stub
		super.onServiceConnected();
		mMonitoring = true;
		runnerThread = new Thread(new RedPacketRunner());
		runnerThread.start();
		Toast.makeText(this, "抢红包服务已开启", Toast.LENGTH_LONG).show();
	}
	
	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		synchronized (mLock) {
			mLastEventTimeMillis = event.getEventTime();
			if (mWaitingForEventDelivery) {
				mEventArray.add(AccessibilityEvent.obtain(event));
				mLock.notifyAll();
			}
			
			else if (RedPacketHelper.isRedPacket(event, this)) {				
				final Notification notification = (Notification)event.getParcelableData();
				mExecutor = new IExecutor() {
					@Override
					public void execute() {
						try {
							notification.contentIntent.send();
						} catch (CanceledException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				};
				mLock.notifyAll();
			}
		}
	}
	

	@Override
	public void onInterrupt() {
		// TODO Auto-generated method stub
		Log.w(LOG_TAG, "Interrupt");
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		// TODO Auto-generated method stub
		mMonitoring = false;
		runnerThread = null;
		Toast.makeText(this, "抢红包服务已关闭", Toast.LENGTH_LONG).show();
		return super.onUnbind(intent);
	}

	private AccessibilityEvent performActionAndWaitForEvent(IExecutor executor,
				MyAccessibilityEventFilter filter, long timeoutMillis) throws TimeoutException {
		synchronized (mLock) {
			mEventArray.clear();
			mWaitingForEventDelivery = true;
		}
		
		final long executionStartTimeMillis = SystemClock.uptimeMillis();
		executor.execute();
		
		synchronized (mLock) {
			try {
				final long startTimeMillis = SystemClock.uptimeMillis();
				while (true) {
					while (!mEventArray.isEmpty()) {
						AccessibilityEvent event = mEventArray.remove(0);
						if (event.getEventTime() >= executionStartTimeMillis && filter.accept(event)) {
							return event;
						}
						event.recycle();
					}
					
					final long elapsedTimeMillis = SystemClock.uptimeMillis() - startTimeMillis;
					final long remainingTimeMillis = timeoutMillis - elapsedTimeMillis;
					if (remainingTimeMillis <= 0) {
						throw new TimeoutException("Expected event not received within:" + timeoutMillis + " ms.");
					}
					
					try {
						mLock.wait(remainingTimeMillis);
					} catch (Exception e) {
						/* ignore */
					}
				}
			} finally {
				mWaitingForEventDelivery = false;
				mEventArray.clear();
			}
		}
	}
	
	private void turnOffScreen() {
		PowerManager.WakeLock wakeLock = ((PowerManager)getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "off");
		wakeLock.acquire();
		wakeLock.release();
	}

	
	private class RedPacketRunner implements Runnable {
		private final long idleTimeMillis = 500;
		private final long globalTimeMills = 5000;
		
		@Override
		public void run() {
			while (mMonitoring) {
				synchronized (mLock) {
					try {
						mLock.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}											
				}
				
				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MyAccessibilityService.this);
				PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
				PowerManager.WakeLock wakeLock = null;
				boolean alreadySendNotificationContentIntent = false;
				try {
					if (!powerManager.isScreenOn()) {
						if (!sharedPreferences.getBoolean(getString(R.string.wake_up_key), false)) {
							continue;
						}
						wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
						wakeLock.acquire();
					}
					performActionAndWaitForEvent(mExecutor, new WaitForAnyEventPredicate(
							AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED), globalTimeMills);
					alreadySendNotificationContentIntent = true;
					
					waitForIdle(idleTimeMillis, globalTimeMills, true);
					AccessibilityNodeInfo rootnode = getRootInActiveWindow();
					
					if (rootnode == null || !rootnode.getPackageName().equals(getString(R.string.wechat_package_name))) {
						continue;
					}
					
					AccessibilityNodeInfo listnode = null;
					ArrayList<AccessibilityNodeInfo> listnodes = RetrieveWindowHelper.findAccessibilityNodeInfosByClass(rootnode, ListView.class);
					for (AccessibilityNodeInfo each : listnodes) {
						AccessibilityNodeInfo parent = each.getParent();
						if (parent != null 
								&& parent.getClassName().toString().equals(LinearLayout.class.getName())) {
							if (listnode != null) {
								Log.e(LOG_TAG, "Duplicate listnode");
							}
							listnode = each;
						} else {
							each.recycle();
						}
						parent.recycle();
					}
					listnodes.clear();
					Log.i(LOG_TAG, "listnode: " + listnode.toString());
					
					AccessibilityNodeInfo redpacketTextNodeInfo = RetrieveWindowHelper.findLastAccessibilityNodeInfoByText(listnode, "微信红包");
					final AccessibilityNodeInfo redpacketNodeInfo;
					listnode.recycle();
					
					if (redpacketTextNodeInfo == null) {
						continue;
					} else if ((redpacketNodeInfo = redpacketTextNodeInfo.getParent()) == null) {
						redpacketTextNodeInfo.recycle();
						continue;
					}
					
					Log.i(LOG_TAG, "redpacketnode:" + redpacketNodeInfo.toString());
					redpacketTextNodeInfo.recycle();
					
					performActionAndWaitForEvent(
							new IExecutor() {
								@Override
								public void execute() {	
									redpacketNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
								}
							},
							new WaitForAnyEventPredicate(
									AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | 
									AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED), 
							globalTimeMills);
					redpacketNodeInfo.recycle();
					
					waitForIdle(idleTimeMillis, globalTimeMills, false);
					rootnode = getRootInActiveWindow();
					if (rootnode == null)
						continue;
					final AccessibilityNodeInfo openNode = RetrieveWindowHelper.findFirstAccessibilityNodeInfoByClass(rootnode, Button.class);
					
					if (openNode == null)
						continue;
					
					Log.i(LOG_TAG, "openNode:" + openNode.toString());
					performActionAndWaitForEvent(
							new IExecutor() {								
								@Override
								public void execute() {
									openNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
								}
							},
							new WaitForAnyEventPredicate(
									AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
									AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED), globalTimeMills);
					openNode.recycle();
					
				} catch (Exception e) {
					// ignore 
					e.printStackTrace();
				} finally {
					synchronized (mLock) {
						mWaitingForEventDelivery = false;
						mEventArray.clear();
					}
					mExecutor = null;
					
					if (alreadySendNotificationContentIntent
							&& sharedPreferences.getBoolean(getString(R.string.return_home_key), true)) {
						
						waitForIdle(idleTimeMillis, globalTimeMills, true);
						
						if (sharedPreferences.getBoolean(getString(R.string.flyme_key), false)) {
							Intent intent = new Intent(Intent.ACTION_MAIN);
							intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							intent.addCategory(Intent.CATEGORY_HOME);
							startActivity(intent);
						} else {
							performGlobalActionAndWaitWindowChange(GLOBAL_ACTION_HOME);	
						}
					}
					
					if (wakeLock != null && wakeLock.isHeld()) {
						wakeLock.release();
						turnOffScreen();
					}
				}				
			}
		}
		
		public void waitForIdle(long idleTimeoutMillis, long globalTimeoutMillis, boolean resetflag) {
			synchronized (mLock) {
				final long startTimeMillis = SystemClock.uptimeMillis();
				if (mLastEventTimeMillis <= 0 || resetflag)
					mLastEventTimeMillis = startTimeMillis;
				
				while (true) {
					final long currentTimeMillis = SystemClock.uptimeMillis();
					final long elapsedGlobalTimeMillis = currentTimeMillis - startTimeMillis;
					final long remainingGlobalTimeMillis = globalTimeoutMillis - elapsedGlobalTimeMillis;
					if (remainingGlobalTimeMillis <= 0)
						break;
					
					final long elapsedIdleTimeMillis = currentTimeMillis - mLastEventTimeMillis;
					final long remainingIdleTimeMills = idleTimeoutMillis - elapsedIdleTimeMillis;
					if (remainingIdleTimeMills <= 0)
						break;
					
					try {
						mLock.wait(remainingIdleTimeMills);
					} catch (InterruptedException e) {
						// ignore 
					}
				}
				Log.i(LOG_TAG, "start:" + startTimeMillis + "    end:" + SystemClock.uptimeMillis() + "    last:" + mLastEventTimeMillis);
			}
		}
		
		private void performGlobalActionAndWaitWindowChange(final int action) {
			IExecutor executor = new IExecutor() {
				@Override
				public void execute() {
					performGlobalAction(action);			
				}
			};
			try {
				performActionAndWaitForEvent(executor, new WaitForAnyEventPredicate(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED), globalTimeMills);
			} catch (TimeoutException e) {
				// ignore 
			}
		}
		
	}
}
