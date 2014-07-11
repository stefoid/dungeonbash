package com.dbash.platform;

import com.badlogic.gdx.Gdx;

public class SizeCalculator {

	public static float DATA_HEADER_SCALE = 0.23f;
	public static float TAB_AREA_SCALE = 1.0f - DATA_HEADER_SCALE;
	public static float TAB_BUTTON_SCALE = 0.15f;
	public static float LIST_AREA_SCALE = 1.0f - TAB_BUTTON_SCALE;
	
	public static float MIN_DRAG_DISTANCE = 0.2f;  // 2mm
	
	
	public float ELEMENTS_PER_SCREEN;
	public int MIN_ELEMENTS;
	
	// the number of pixels the finger has to move to constitute a drag rather than a touch.  depends on pixel density
	public float MIN_DRAG_PIXELS; 
	
	
	public SizeCalculator() {
		float ppcy = Gdx.graphics.getPpcY();
		float height = Gdx.graphics.getHeight();
		float tabHeight = (height*TAB_AREA_SCALE);
		float listHeight = tabHeight*LIST_AREA_SCALE;
		
		if (ppcy < 30) {
			ppcy = 80;  // something wrong with desktop pixel density
		}

		setListHeight(listHeight, ppcy);  // set the number of elements in the list.  (bit over 1cm per element)
		
		MIN_DRAG_PIXELS = MIN_DRAG_DISTANCE * ppcy;
	}
	
//	given the pixels per centimetre, I can work out how many centimetres the list will be
//	the ideal list element height is 11mm.
//	divide the area by 11mm for the number of list elements, and if this number is less than 4, make it 4.
	private void setListHeight(float height, float ppcy) {

		float cms = height/ppcy;
		ELEMENTS_PER_SCREEN = Math.round(cms/1.5f);
		if (ELEMENTS_PER_SCREEN < 4.0f) {
			ELEMENTS_PER_SCREEN = 4.0f;
		}
		if (ELEMENTS_PER_SCREEN > 8f) {
			ELEMENTS_PER_SCREEN = 8f;
		}
		MIN_ELEMENTS = (int) ELEMENTS_PER_SCREEN;
	}  
	
}
