package com.dbash.presenters.widgets;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.TouchEvent;
import com.dbash.models.TouchEventListener;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.Audio;
import com.dbash.platform.ImageView;
import com.dbash.platform.UIDepend;
import com.dbash.util.Rect;


// 'pressed' is a state that is toggled by user pressing the button, rather than an absolute
// So the button can represent a toggle button by the click listener setting its state appropriately.
public class ButtonView implements TouchEventListener {

	private IClickListener clickListener;
	private ImageView onImage;
	private ImageView offImage;
	private ImageView disabledImage;
	private boolean pressed;
	private boolean enabled;
	private UIDepend gui;
	private TouchEventProvider touchEventProvider;
	private Rect area;
	private String clickSound;
	
	public ButtonView(UIDepend gui, TouchEventProvider touchEventProvider, Rect area, String onImage, String offImage, 
			String disabledImgae, String clickSound) {
		this.touchEventProvider = touchEventProvider;
		this.onImage = new ImageView(gui, onImage, area);
		this.offImage = new ImageView(gui, offImage, area);
		this.disabledImage = new ImageView(gui, disabledImgae, area);
		this.gui = gui;
		enabled = true;
		pressed = false;
		this.clickSound = clickSound;
		this.area = new Rect(area);
		addYourself();
	}
	
	public void onClick(IClickListener listener) {
		this.clickListener = listener;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public void setEnabled( boolean enabled) {
		this.enabled = enabled;
	}
	
	public void draw(SpriteBatch spriteBatch) {
		draw(spriteBatch, 0, 0);
	}
	
	public void setEnabledAndTouch( boolean enabled, boolean touch) {
		this.enabled = enabled;
		if (touch) {
			addYourself();
		} else {
			removeYourself();
		}
	}
	
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		if (enabled) {
			if (pressed) {
				onImage.draw(spriteBatch, x, y);
			} else {
				offImage.draw(spriteBatch, x, y);
			} 
		} else {
			disabledImage.draw(spriteBatch, x, y);
		}
	}
	
	@Override
	public boolean touchEvent(TouchEvent event) {
		
		// disabled buttons dont prodess touch events.
		if (enabled == false) {
			return false;
		}
		
		switch (event.getTouchType()) {
			case DOWN:
				toggleState();
				if (clickSound != null) {
					gui.audio.playSound(clickSound);
				}
				break;
			case UP:
			case UP_INSIDE:
				pressed = false;
				break;
			case CLICK:
				unToggleState();  // before processing click - toggle state back.
				if (clickListener != null) {
					clickListener.processClick();
				}
				break;
			case MOVE:
				break;
			default:
				break;
		}
		
		return true;
	}
	
	public void toggleState() {
		if (pressed) {
			pressed = false;
		} else {
			pressed = true;
		}
	}
	
	// the standard button behavior is to return to the 'popped up' state when user lifts finger
	// override this to do nthing if you want the button to stay 'pushed'
	public void unToggleState() {
		toggleState();
	}
	
	public void setState(boolean pressed) {
		this.pressed = pressed;
	}
	
	public boolean getState() {
		return pressed;
	}
	
	public void removeYourself() {
		touchEventProvider.removeTouchEventListener(this);
	}
	
	public void addYourself() {
		removeYourself();
		touchEventProvider.addTouchEventListener(this, area, gui.cameraViewPort.viewPort);
	}
}
