package com.dbash.models;

import com.dbash.util.Rect;



public class TouchEvent {

	public static enum TouchType {
		DOWN, MOVE, UP, UP_INSIDE, CLICK
	};

	private TouchType touchType;
	
	// original screen coords recorded
	public float screenX;  // original touch x,y at screen coords
	public float screenY;
	public float screenDownX;  // original down touch x,y at screen coords
	public float screenDownY;
	
	// these coords will be adjusted for the viewport of the listener.
	private float x;		// modified touch x,y after adjusting for viewport of the listener
	private float y;
	public float downX;  // position where the down event occurred (will also be modified by adjustForViewport)
	public float downY; 
	
	// dx and dy is always the same
	public float dx;
	public float dy;
	public boolean hasMoved;

	public TouchEvent(float x2, float y2) {
		this(x2,y2,TouchType.DOWN);
	}

	public TouchEvent(float x, float y, TouchType touchType) {
		screenX = x;
		screenY = y;
		this.x = x;
		this.y = y;
		this.touchType = touchType;
		if (touchType == TouchType.DOWN) {
			downX = screenDownX = x;
			downY = screenDownY = y;
			hasMoved = false;
			dx = 0;
			dy = 0;
		} 
		
		if (touchType == TouchType.MOVE) {
			hasMoved = true;
		}		
		
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	// This will be called when handing the touch event off for processing to a listener so that the position is adjusted for that listeners viewport
	// We record the original screen coords so we can call this multipel times with different viewports.
	public void adjustForViewPort(Rect viewPort) {
		x = screenX - viewPort.x;
		y = screenY - viewPort.y;
		downX = screenDownX - viewPort.x;
		downY = screenDownY - viewPort.y;
	}
	
	public TouchType getTouchType() {
		return touchType;
	}
	
	public void setTouchType(TouchType touchType) {
		this.touchType = touchType;
	}
	
	private int translateToDirection() {
		
		int mdx = getDelta((int) (x - downX));
		int mdy = getDelta((int) (y - downY));

		int dir = DungeonPosition.NO_DIR;
		
		switch (mdx) {
			case -1:
				switch (mdy) {
					case -1:
						dir = DungeonPosition.SOUTHWEST;
						break;
					case 0:
						dir = DungeonPosition.WEST;
						break;
					case 1:
						dir = DungeonPosition.NORTHWEST;
						break;
				}
				break;
			case 0:
				switch (mdy) {
					case -1:
						dir = DungeonPosition.SOUTH;
						break;
					case 0:
						dir = DungeonPosition.NO_DIR;
						break;
					case 1:
						dir = DungeonPosition.NORTH;
						break;
				}
				break;
			case 1:
				switch (mdy) {
					case -1:
						dir = DungeonPosition.SOUTHEAST;
						break;
					case 0:
						dir = DungeonPosition.EAST;
						break;
					case 1:
						dir = DungeonPosition.NORTHEAST;
						break;
				}
				break;
			}
		return dir;
	}

	private int getDelta(int v) {
		return (v < -48) ? -1 : (v > 48) ? 1 : 0;
	}
	
	public int getMoveDirection() {
		return translateToDirection();	
	}
	
	// So the idea is that we remember the original coords where the gesture started (screenDown)
	// and we determine the dx and dy from the current screenXY to the previous screenXY
	public void setTouchMemory(TouchEvent previousEvent) {
		dx = screenX - previousEvent.screenX;
		dy = screenY - previousEvent.screenY;
		screenDownX = previousEvent.screenDownX;
		screenDownY = previousEvent.screenDownY;
		hasMoved = previousEvent.hasMoved;
	}
	
	public float getTotalMaxMovement() {
		float dx = Math.abs(screenDownX - x);
		float dy = Math.abs(screenDownY - y);
		
		if (dx < dy) {
			return dy;
		} else {
			return dx;
		}
	}
	
	@Override
	public String toString() {
		return x+":"+y;
	}

	public boolean insideRectangle(Rect area) {
		return area.isInside(x, y);
	}

}
