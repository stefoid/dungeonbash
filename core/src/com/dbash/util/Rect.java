package com.dbash.util;

import com.dbash.models.DungeonPosition;

public class Rect {

	public enum HAlignment {
		LEFT,
		CENTER,
		RIGHT
	}

	public enum VAlignment {
		TOP,
		CENTER,
		BOTTOM
	}
	
	public float x;
	public float y;
	public float width;
	public float height;
	
	public Rect(float x, float y, float width, float height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	
	// copy constructor
	public Rect(Rect rect) {
		this.x = rect.x;
		this.y = rect.y;
		this.width = rect.width;
		this.height = rect.height;
	}
	
	// gives you a new Rect based on the one pased in, but scaled around its center point.  Useful for animations.
	public Rect(Rect rect, float scale) {
		float xCenter = rect.x + rect.width/2;
		float yCenter = rect.y + rect.height/2;
		
		this.width = rect.width*scale;
		this.height = rect.height*scale;
		this.x = xCenter - this.width/2;
		this.y = yCenter - this.height/2;
	}
	
	// return a new rect that is certain percentage offsets inside the encosing Rect - for laying out children
	public Rect(Rect area, float leftOffPercent, float rightOffPercent, float topOffPercent, float botOffPercent) {
		float leftPer = leftOffPercent*area.width;
		float rightPer = rightOffPercent*area.width;
		float topPer = topOffPercent*area.height;
		float botPer = botOffPercent*area.height;
		
		this.x = area.x + leftPer;
		this.width = area.width - leftPer - rightPer;
		this.y = area.y + botPer;
		this.height = area.height - botPer - topPer;
	}
	
	public Rect(DungeonPosition position, int width, int height) {
		int x = position.x - width/2;
		int y = position.y - height/2;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	
	public boolean overlaps(Rect r) {
	    return x < r.x + r.width && x + width > r.x && y < r.y + r.height && y + height > r.y;
	}
	
	public boolean isInside(Rect r) {
	    return x>=r.x && width <= r.width && y>= r.y && height <= r.height;
	}
	
	// Return a rect of the specified width and height with an x and y pos alligned as specified within the enclosing Rect.
	// for laying out children.
	public Rect (Rect area, HAlignment hAlign, VAlignment vAlign, float width, float height)
	{
		this.width = width;
		this.height = height;
		
		switch (hAlign) {
			case LEFT:
				x = area.x;
				break;
			case CENTER:
				x = area.x + area.width/2 - width/2;
				break;
			case RIGHT:
				x = area.x + area.width - width;
				break;
		}
		
		switch (vAlign) {
			case BOTTOM:
				y = area.y;
				break;
			case CENTER:
				y = area.y + area.height/2 - height/2;
				break;
			case TOP:
				y = area.y + area.height - height;
				break;
		}
	}
		
	public boolean isInside(float xt, float yt) {
		if ((xt >= this.x) && (xt <= (this.x + this.width)) &&
		   (yt >= this.y) && (yt <= (this.y + getHeight()))) 
			return true;
		else	
			return false;
	}
	
	public float getCenterX(Rect rect) {
		return rect.x + rect.width/2;
	}
	
	public float getCenterY(Rect rect) {
		return rect.y + rect.height/2;
	}
	
	public float getX() {
		return x;
	}
	
	public float getY() {
		return y;
	}
	
	public float getHeight() {
		return height;
	}
	
	@Override
	public String toString() {
		return "X: "+x+" Y:"+y+" W:"+width+" H:"+height;
	}
}
