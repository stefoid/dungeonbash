package com.dbash.platform;

import com.badlogic.gdx.Gdx;
import com.dbash.util.Logger;

public class SizeCalculator {
	float ppcy;
	
	public static float DATA_HEADER_SCALE = 0.23f;
	public static float TAB_AREA_SCALE = 1.0f - DATA_HEADER_SCALE;
	public static float TAB_BUTTON_SCALE = 0.15f;
	public static float LIST_AREA_SCALE = 1.0f - TAB_BUTTON_SCALE;
	public static float MIN_DRAG_DISTANCE = 0.2f;  // 2mm
	
	public static float LIST_ELEMENT_HEIGHT;
	public float ELEMENTS_PER_SCREEN;
	public int MIN_ELEMENTS;
	public float MAX_ELEMENTS = 7f;
	
	// the number of pixels the finger has to move to constitute a drag rather than a touch.  depends on pixel density
	public float MIN_DRAG_PIXELS; 
	
	
	public SizeCalculator() {
		ppcy = Gdx.graphics.getPpcY();
	}
	
//	given the pixels per centimetre, I can work out how many centimetres the list will be
//	the ideal list element height is around 12mm.
//	divide the area by 11mm for the number of list elements, and if this number is less than 4, make it 4.
	public void setListHeight(float totalHeight) {
		float tabHeight = (totalHeight*TAB_AREA_SCALE);
		float listHeight = tabHeight*LIST_AREA_SCALE;
		
		Logger.log("ppcy: "+ppcy);
		Logger.log("height: "+listHeight);
		Logger.log("tabHeight: "+tabHeight);
		Logger.log("listHeight: "+listHeight);
		
		if (ppcy < 30) {
			Logger.log("something happened");;
			ppcy = 80;  // something wrong with desktop pixel density
		}

		MIN_DRAG_PIXELS = MIN_DRAG_DISTANCE * ppcy;
		float cms = listHeight/ppcy;
		Logger.log("cms: "+cms);
		
		ELEMENTS_PER_SCREEN = Math.round(cms/1.2f);
		if (ELEMENTS_PER_SCREEN < 4.0f) {
			ELEMENTS_PER_SCREEN = 4.0f;
		}
		
		if (ELEMENTS_PER_SCREEN > MAX_ELEMENTS) {
			ELEMENTS_PER_SCREEN = MAX_ELEMENTS;
		}
		Logger.log("ELEMENTS_PER_SCREEN: "+ELEMENTS_PER_SCREEN);
		MIN_ELEMENTS = (int) ELEMENTS_PER_SCREEN;
		Logger.log("MIN_ELEMENTS: "+MIN_ELEMENTS);
		LIST_ELEMENT_HEIGHT = listHeight / MIN_ELEMENTS;
		Logger.log("LIST_ELEMENT_HEIGHT: "+LIST_ELEMENT_HEIGHT);
	}  
	
}
