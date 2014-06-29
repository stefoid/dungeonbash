package com.dbash.util;

import com.dbash.models.IAnimListener;

// Something that tweens a single float value and calls you back when its done.
// Dont pass a callback if you dont care.
public class Tween {

	float startValue;
	float endValue;
	float period;
	IAnimListener completeListener;
	
	float value;
	float timePassed;
	float constant;
	boolean tweening;
	
	public Tween() {
		
	}
	
	public Tween init(float startValue, float endValue, float period, IAnimListener completeListener) {
		this.startValue = startValue;
		this.endValue = endValue;
		this.period = period;
		this.completeListener = completeListener;
		
		value = startValue;
		constant = (endValue - startValue) / period;
		timePassed = 0;
		tweening = true;
		
		return this;
	}
	
	public float getValue() {
		return value;
	}
	
	public void deltaTime(float deltaTime) {
		if (tweening == false) {
			return;
		}
		
		timePassed += deltaTime;
		
		if (timePassed >= period) {
			value = endValue;
			tweening = false;
			if (completeListener != null) {
				completeListener.animEvent();
			}
		} else {
			adjustValue(); 
		}
	}
	
	// // this is a linear tween, i.e. x = x1 + c*t   where c = (x2 - x1)/period 
	// subclass this and override this function for different tween functions.
	protected void adjustValue() {
		value = startValue + constant * timePassed;
	}
	
}
