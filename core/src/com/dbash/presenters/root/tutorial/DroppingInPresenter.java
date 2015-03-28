package com.dbash.presenters.root.tutorial;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.TouchEvent;
import com.dbash.models.TouchEventListener;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.FadeBox;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.root.OverlayPresenter;
import com.dbash.util.Rect;


public class DroppingInPresenter extends OverlayPresenter implements TouchEventListener {
	
	FadeBox fadeBox;
	
	public DroppingInPresenter() {
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
		touchEventProvider.addTouchEventListener(this, theArea, gui.cameraViewPort.viewPort);  
		
		this.fadeBox = new FadeBox(gui, "Characters drop into a level on at a time.  Touch anywhere to continue...", area);
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
	}

}
