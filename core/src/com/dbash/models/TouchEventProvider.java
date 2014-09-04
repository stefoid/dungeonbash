package com.dbash.models;

import com.dbash.util.Rect;

public interface TouchEventProvider {

	// If the specified size is null, it means the whole screen.
	// the viewport provided is the viewport that the listener belongs to, so the touchevent coords can be modified to be relative to that viewport
	// i.e. x,y of the viewport subtracted from the x,y of the touchevent.
	void addTouchEventListener(TouchEventListener listener, Rect area, Rect viewport);
	
	void removeTouchEventListener(TouchEventListener listener);
}
