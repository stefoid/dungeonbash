package com.dbash.presenters.widgets;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEvent;
import com.dbash.models.TouchEvent.TouchType;
import com.dbash.models.TouchEventListener;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.ImagePatchView;
import com.dbash.platform.ImageView;
import com.dbash.platform.UIDepend;
import com.dbash.util.EventBus;
import com.dbash.util.Rect;


public abstract class TabPresenter implements TouchEventListener {

	protected ImageView		backImageCurrent;
	protected ImageView		backImageNotCurrent;
	protected IClickListener clickListener;
	
	protected Rect tabArea;
	protected Rect bodyArea;
	protected ImagePatchView setBorder;
	protected ImagePatchView unSetBorder;
	
	protected TouchEventProvider touchEventProvider;
	
	boolean current;
	protected boolean shouldDrawBody;
	
	
	public TabPresenter(PresenterDepend model, UIDepend gui, TouchEventProvider touchEventProvider, Rect tabArea, Rect bodyArea) {
		this.tabArea = new Rect(tabArea);
		this.bodyArea = new Rect(bodyArea);
		this.touchEventProvider = touchEventProvider;
		
		setBorder = new ImagePatchView(gui, "9patchsettabborder", tabArea);
		unSetBorder = new ImagePatchView(gui, "9patchmidtabborder", tabArea);
		
		current = false;
		shouldDrawBody = false;
		clickListener = null;
		touchEventProvider.addTouchEventListener(this, this.tabArea, gui.cameraViewPort.viewPort);
	}

	// When this tab is clicked, invoke the clickListener passed here.
	public void onClick(IClickListener clickListener) {
		this.clickListener = clickListener;
	}
	
	// We are pretty cool about accepting any and all input in our area.
	public boolean touchEvent(TouchEvent event) {
		if (clickListener != null) {
			if (event.getTouchType() == TouchType.CLICK) {  // means user has clicked down and up inside area.
				clickListener.processClick();
			}
		}
		return true;
	}
	
	// draws the background for the tab (subclass puts individual icon on top)
	public void drawTab(SpriteBatch spriteBatch, float x, float y) {
		if (current) {
			backImageCurrent.draw(spriteBatch);
			setBorder.draw(spriteBatch);
		} else {
			backImageNotCurrent.draw(spriteBatch);
			unSetBorder.draw(spriteBatch);
		}
	}
	
	public void setShouldDrawBody(boolean drawBody) {
		shouldDrawBody = drawBody;
	}
	
	public void setCurrent() {
		current = true;
		shouldDrawBody = true;
	}
	
	public void unsetCurrent() {
		current = false;
		shouldDrawBody = false;
	}
	
	public void onDestroy() {
		EventBus.getDefault().removeAll(this);
		touchEventProvider.removeTouchEventListener(this);
	}
}