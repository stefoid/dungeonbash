package com.dbash.presenters.widgets;

import java.util.List;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.TouchEvent;
import com.dbash.models.TouchEventListener;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.UIDepend;
import com.dbash.util.Rect;


// A scrolling list is a collection of list elements - the list controls scrolling up and down and selection
// It gets input from the overlay manager in the form of touch-ups, touch-downs and movement.
// It turns these into scrolling and selection events.  It passes the selection event to the selected ListElement
// so the listElement can decide what, if anything, to do about it.
public class ScrollingListView implements TouchEventListener {
	
	List<IListElement> listElements = null;
	Rect area;
	float elementHeight;
	int lastElementIndex;
	TouchEventProvider touchEventProvider;
	UIDepend gui;
	
	// A scrolling list containing N elements is N*element_height pixels_tall.  When only part of the list is visible in the scroll window, listPosition records
	// the position of the scroll window with respect to the list.  i.e. if the very top of the first element is visible (top of the list) then this will be 0.
	private float listPosition; 
	private float speed = 0;
	private float listPositionMin;
	private float listPositionMax;
	private float listPositionBottom; // by defn, 0 is Top.
	private float bounceSpeed;
	
	public ScrollingListView(UIDepend gui, TouchEventProvider touchEventProvider, Rect area, float elementHeight) {
		this.area = new Rect(area);
		this.elementHeight = elementHeight;
		this.bounceSpeed = elementHeight / 50f;
		listPosition = 0;
		listPositionMax = 0;
		this.touchEventProvider = touchEventProvider;
		this.gui = gui;
	}
		
	public void setListElements(List<IListElement> listElements, float listPosition) {
		this.listElements = listElements;
		
		lastElementIndex = listElements.size()-1;
		listPositionMin = 1- elementHeight/10;
		listPositionBottom = listElements.size() * elementHeight - area.height;
		listPositionMax =  listPositionBottom + elementHeight/10;
		
		this.listPosition = listPosition;
		if (listPosition < listPositionMin) {
			listPosition = listPositionMin;
		}
		if (listPosition > listPositionMax) {
			listPosition = listPositionMax;
		}
	}
	
	public void scroll(float speed) {
		this.speed = speed;
	}
	
	float listPos;
	void draw(SpriteBatch spriteBatch, float x, float y) {
		// adjust list position by the current speed
		if (speed != 0) {
			listPosition -= speed;
			if ((speed < 1f) && (speed > -1f)) {
				speed = 0;
			} else {
				speed /= 1.1f;  // list will slow by this amount every draw until it stops.
			}
		}
		
		// make sure list doesnt go too far off track
		if (listPosition < listPositionMin) {
			listPosition = listPositionMin;
			speed = -bounceSpeed;  // bounce it back a bit
		}
		
		// make sure list doesnt go too far off track
		if (listPosition > listPositionMax) {
			listPosition = listPositionMax;
			speed = bounceSpeed;  // bounce it back a bit
		}
		
		// If we are below or above true top and bottom, and have no speed, reset to top or bottom
		if ((listPosition < 0) && (speed == 0)) {
			listPosition = 0;
		}
		
		// If we are below or above true top and bottom, and have no speed, reset to top or bottom
		if ((listPosition > listPositionBottom) && (speed == 0)) {
			listPosition = listPositionBottom;
		}	
		
		listPosition = Math.round(listPosition);
		
		// Clip the top and bottom of the entire list so the top or bottom elements 
		gui.cameraViewPort.startClipping(spriteBatch, area);
		
		// clear the draw flags
		for (IListElement element : listElements) {
			element.clearDrawFlag();
		}
		
		// draw each ListElement that is at least partially visible.
		int firstVisibleElement = getElementIndexForYPosition(listPosition);
		int lastVisibleElement = getElementIndexForYPosition(listPosition + area.height);
		
		if (lastVisibleElement > lastElementIndex) {
			lastVisibleElement = lastElementIndex;
			//firstVisibleElement = lastVisibleElement - gui.sizeCalculator.MIN_ELEMENTS;
		}

		// where should the first visible element be drawn? (x,y passed to us as bottom left of area)
		float elemY = area.y + area.height - elementHeight + (float) (listPosition %  elementHeight);
		
//		if (Logger.DEBUG) {
//			if (listPos != listPosition) {
//				listPos = listPosition;
//			Logger.log("lastVE: "+lastVisibleElement + "  FirstVE: "+firstVisibleElement+ " LP: "+listPosition + " elemY: "+elemY +
//					"  mod: " +(float) (listPosition %  elementHeight) + " float 1st: "+listPosition/elementHeight);
//			}
//		}
		
		for (int i=firstVisibleElement; i <= lastVisibleElement; i++) {
			IListElement element = listElements.get(i);
			element.draw(spriteBatch, area.x + x, elemY);
			elemY -= elementHeight;
		}
		
		gui.cameraViewPort.endClipping(spriteBatch);
	}
	
	
	public void activate() {
		touchEventProvider.addTouchEventListener(this, area, gui.cameraViewPort.viewPort);
	}
	
	public void deactivate() {
		touchEventProvider.removeTouchEventListener(this);
		speed = 0;
	}
	
	// given a y position in the overall list, which element would it fall inside of?
	private int getElementIndexForYPosition(float y) {
		int elementIndex = (int) (y / elementHeight);
		
		return elementIndex;
	}

	public float getListPosition()
	{
		return listPosition;
	}
	
	@Override
	public boolean touchEvent(TouchEvent event) {
		boolean useTouched = false;

		switch (event.getTouchType()) {
			case DOWN:
				useTouched = true; // return true on DOWN to get further touches associated with this gesture
				break;
				
			case MOVE:
				speed = -event.dy;  // return false for moves so higher layers can also get moves
				break;
				
			// ypos of list = y, which is at the bottom of the list.	
			case CLICK:
				float touchPosInList = listPosition + area.height - (event.getY() - area.y);
				int elementIndex = getElementIndexForYPosition(touchPosInList);
				listElements.get(elementIndex).gotSelection(event.getX() - area.x, 0); // TODO y is wrong, but we dont need it yet
				useTouched = true;
				break;
			case UP:
				break;
			case UP_INSIDE:
				break;
		}

		return useTouched;  
	}
	
}