package com.dbash.presenters.root.tutorial;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.IEventAction;
import com.dbash.models.TouchEvent;
import com.dbash.models.TouchEventListener;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.UIDepend;
import com.dbash.util.EventBus;
import com.dbash.util.Rect;
import com.dbash.util.Rect.HAlignment;
import com.dbash.util.Rect.VAlignment;


public class PassingPresenter extends TutorialPopupPresenter implements TouchEventListener {
	
	FadeBoxPresenter fadeBox;
	
	public PassingPresenter() {
	}
	
	@Override
	public void init(UIDepend gui) {
		super.init(gui);
		this.gui = gui;
	}
	
	@Override
	public void start(Rect theArea, TouchEventProvider touchEventProvider) {
		super.start(theArea, touchEventProvider);
		
		this.touchEventProvider = touchEventProvider;
		this.area = new Rect(gui.sizeCalculator.dungeonArea, .15f, .2f, .6f, .01f);
		
		// Needs to swallow all touches to the dungeon area 
		touchEventProvider.addTouchEventListener(this, gui.sizeCalculator.dungeonArea, gui.cameraViewPort.viewPort);  
		
		EventBus.getDefault().onEvent(TutorialPresenter.PASS_ON_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				EventBus.getDefault().event(TutorialPresenter.ANIM_PASS_BUTTON_OFF_EVENT, null);
				fadeBox.dismiss();
				dismiss();
			}
		});
		
		this.fadeBox = new FadeBoxPresenter("The pass button gives a slow penalty but a big defensive bonus.\n\nTest the pass turn button 4 times now.", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		fadeBox.setNoTouch();
		addFadeBoxPar(fadeBox);
		EventBus.getDefault().event(TutorialPresenter.ANIM_PASS_BUTTON_ON_EVENT, null);
	}
	
	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y) {
	}
	
	@Override
	public boolean touchEvent(TouchEvent event) {
		return true;
	}
	
	public void destroy() {
		EventBus.getDefault().removeListener(TutorialPresenter.PASS_ON_EVENT, this);
		touchEventProvider.removeTouchEventListener(this);
	}

}
