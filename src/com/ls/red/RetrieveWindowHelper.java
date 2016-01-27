package com.ls.red;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import android.view.accessibility.AccessibilityNodeInfo;

class RetrieveWindowHelper {
	public static <T> AccessibilityNodeInfo findFirstAccessibilityNodeInfoByClass(
			AccessibilityNodeInfo node, Class<T> type) {
		if (node == null || type == null)
			return null;
		else if (node.getClassName().toString().equals(type.getName()))
			return node;
		else {
			AccessibilityNodeInfo resultNode = null;
			AccessibilityNodeInfo childNode = null;
			for (int i = 0; i < node.getChildCount(); i++) {
				childNode = node.getChild(i);
				resultNode = findFirstAccessibilityNodeInfoByClass(childNode, type);
				if (resultNode != null)
					break;
				childNode.recycle();
			}
			return resultNode;
		}
	}
	
	public static <T> ArrayList<AccessibilityNodeInfo> findAccessibilityNodeInfosByClass(
			AccessibilityNodeInfo node, Class<T> type) {
		if (node == null || type == null)
			return null;
		
		ArrayList<AccessibilityNodeInfo> result = new ArrayList<AccessibilityNodeInfo>();
		Queue<AccessibilityNodeInfo> queue = new LinkedList<AccessibilityNodeInfo>();
		
		queue.add(node);
		
		AccessibilityNodeInfo childNode = null;
		while (!queue.isEmpty()) {
			AccessibilityNodeInfo parentNode = queue.poll();
			if (parentNode.getClassName().toString().equals(type.getName())) {
				result.add(parentNode);
			} else {
				for (int i = 0; i < parentNode.getChildCount(); i++) {
					childNode = parentNode.getChild(i);
					if (childNode != null) {
						if (childNode.isVisibleToUser()) {
							queue.add(childNode);
						} else {
							childNode.recycle();
						}
					}
				}
				parentNode.recycle();
			}
		}
		return result;
	}
	
	public static AccessibilityNodeInfo findLastAccessibilityNodeInfoByText(
			AccessibilityNodeInfo node, String text) {
		if (node == null || text == null)
			return null;
		else if (node.getText() != null && node.getText().toString().equals(text))
			return node;
		else {
			AccessibilityNodeInfo resultNode = null;
			AccessibilityNodeInfo childNode = null;
			for (int i = node.getChildCount() - 1; i >= 0; i--) {
				childNode = node.getChild(i);
				resultNode = findLastAccessibilityNodeInfoByText(childNode, text);
				if (resultNode != null)
					break;
				childNode.recycle();
			}
			return resultNode;
		}
	}
}
