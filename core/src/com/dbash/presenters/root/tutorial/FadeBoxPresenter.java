package com.dbash.presenters.root.tutorial;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.TouchEvent;
import com.dbash.models.TouchEventListener;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.FadeBox;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.root.IDismissListener;
import com.dbash.presenters.root.OverlayPresenter;
import com.dbash.util.Rect;
import com.dbash.util.Rect.HAlignment;
import com.dbash.util.Rect.VAlignment;


public class FadeBoxPresenter extends OverlayPresenter implements TouchEventListener {
	
	FadeBox fadeBox;
	String text;
	HAlignment hAlign;
	VAlignment vAlign;
	IDismissListener onDismiss;
	boolean useTouch;
	
	
	public FadeBoxPresenter(String text, HAlignment hAlign, VAlignment vAlign, IDismissListener onDismiss) {
		this.text = text;
		this.hAlign = hAlign;
		this.vAlign = vAlign;
		this.onDismiss = onDismiss;
		useTouch = true;
	}
	
	public void setNoTouch() {
		this.useTouch = false;
	}
	
	@Override
	public void init(UIDepend gui) {
		this.gui = gui;
	}
	
	@Override
	public void start(Rect theArea, TouchEventProvider touchEventProvider) {
		this.touchEventProvider = touchEventProvider;
		this.area = new Rect(gui.sizeCalculator.dungeonArea, .15f, .2f, .6f, .01f);
		
		// Needs to swallow all touches to the dungeon area 
		if (useTouch) {
			touchEventProvider.addTouchEventListener(this, theArea, gui.cameraViewPort.viewPort);
		}  
		
		this.fadeBox = new FadeBox(gui, text, area);
	}
	
	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		fadeBox.draw(spriteBatch, x, y);
	}
	
	@Override
	public boolean touchEvent(TouchEvent event) {
		dismiss();
		return true;
	}
	
	public void destroy() {
		touchEventProvider.removeTouchEventListener(this);
		if (onDismiss != null) {
			onDismiss.dismiss();
		}
	}

}
