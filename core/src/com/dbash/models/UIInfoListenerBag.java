package com.dbash.models;

import java.util.Vector;

@SuppressWarnings("serial")
public class UIInfoListenerBag extends Vector<UIInfoListener> {

	public UIInfoListenerBag() {
		super();
	}
	
	public void alertListeners() {
		for (UIInfoListener listener : this) {
			listener.UIInfoChanged();
		}
	}
	
	public void empty() {
		for (UIInfoListener listener : this) {
			remove(listener);
		}
	}
}
