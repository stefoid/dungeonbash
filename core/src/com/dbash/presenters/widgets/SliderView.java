package com.dbash.presenters.widgets;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.TouchEvent;
import com.dbash.models.TouchEventListener;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.ImageView;
import com.dbash.platform.UIDepend;
import com.dbash.util.Rect;


// A scrolling list is a collection of list elements - the list controls scrolling up and down and selection
// It gets input from the overlay manager in the form of touch-ups, touch-downs and movement.
// It turns these into scrolling and selection events.  It passes the selection event to the selected ListElement
// so the listElement can decide what, if anything, to do about it.
public class SliderView implements TouchEventListener {
	
	Rect area;
	Rect buttonArea;
	ImageView backgroundImage;
	ImageView buttonImage;
	
	TouchEventProvider touchEventProvider;
	UIDepend gui;
	ISelectionListener sliderListener = null;
	float slideHeight;
	float maxButtonY;
	float minButtonY;
	
	// This is a percentage value from 0 to 100.0, with 100 being at the very top, 0 at the very bottom
	private float sliderPosition;
	
	public SliderView(UIDepend gui, TouchEventProvider touchEventProvider, Rect area, float buttonSize, String backgroundImage,
			String buttonImage) {
		this.area = new Rect(area);
		this.touchEventProvider = touchEventProvider;
		this.gui = gui;
		
		this.buttonArea = new Rect(area);
		buttonArea.width = buttonSize * area.width * .8f;  // width is 80% of slider width.
		buttonArea.height = buttonSize * area.height * .15f; // width = 15% of slider height
		buttonArea.x = (area.x + area.width/2) - (buttonArea.width / 2);  // center the button
		sliderPosition = 100f;
		slideHeight = area.height - buttonArea.height; 
		minButtonY = area.y;
		maxButtonY = area.y + slideHeight;
		
		this.backgroundImage = new ImageView(gui, backgroundImage, area);
		this.buttonImage = new ImageView(gui, buttonImage, buttonArea);
		setButtonPositionFromSlidePercentage();
	}
		
	public void setSliderPosition(float sliderPosition) {
		this.sliderPosition = sliderPosition;
		setButtonPositionFromSlidePercentage();
	}
	
	// given the size of the slider area and the current slider position, work out where the button should be.
	private void setButtonPositionFromSlidePercentage()
	{
		// the 'slideable' height is the total height minus the button height.
		buttonArea.y = area.y + (sliderPosition * slideHeight/100);
		buttonImage.setArea(buttonArea);
	}
	
	// given the size of the slider area and the current slider position, work out where the button should be.
	private void setSlidePercentageFromButtonPosition()
	{
		// the 'slideable' height is the total height minus the button height.
		float buttonPos = buttonArea.y - area.y;
		sliderPosition = buttonPos / slideHeight * 100;
	}
	
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		backgroundImage.draw(spriteBatch, x, y);
		buttonImage.draw(spriteBatch, x, y);
	}
	
	
	public void activate() {
		touchEventProvider.addTouchEventListener(this, area, gui.cameraViewPort.viewPort);
	}
	
	public void deactivate() {
		touchEventProvider.removeTouchEventListener(this);
	}
	
	public void onSliderChange(ISelectionListener sliderListener) {
		this.sliderListener = sliderListener;
	}
	
	// returns a number between 0 and 100  
	public float getSliderPosition() 
	{
		setSlidePercentageFromButtonPosition();
		return sliderPosition;
	}
	
	@Override
	public boolean touchEvent(TouchEvent event) {
		boolean useTouched = false;

		switch (event.getTouchType()) {
			case DOWN:
				useTouched = true; // return true on DOWN to get further touches associated with this gesture
				break;
				
			case MOVE:
				buttonArea.y = event.getY() - buttonArea.height/2;  
				if (buttonArea.y < minButtonY) {
					buttonArea.y = minButtonY;
				} else if (buttonArea.y > maxButtonY) {
					buttonArea.y = maxButtonY;
				}
				buttonImage.setArea(buttonArea);
				
				if (sliderListener != null) {
					sliderListener.processSelection();
				}
				break;

			case CLICK:
				break;
			case UP:
				break;
			case UP_INSIDE:
				break;
		}

		return useTouched;  
	}
	
}
