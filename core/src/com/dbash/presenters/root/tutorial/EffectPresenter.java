package com.dbash.presenters.root.tutorial;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.IEventAction;
import com.dbash.models.TouchEvent;
import com.dbash.models.TouchEventListener;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.root.IDismissListener;
import com.dbash.presenters.root.OverlayPresenter;
import com.dbash.util.EventBus;
import com.dbash.util.Rect;
import com.dbash.util.Rect.HAlignment;
import com.dbash.util.Rect.VAlignment;


public class EffectPresenter extends OverlayPresenter implements TouchEventListener {
	
	FadeBoxPresenter fadeBox;
	
	public EffectPresenter() {
	}
	
	@Override
	public void init(UIDepend gui) {
		this.gui = gui;
	}
	
	@Override
	public void start(Rect theArea, TouchEventProvider touchEventProvider) {
		this.touchEventProvider = touchEventProvider;
		this.area = new Rect(gui.sizeCalculator.dungeonArea, .15f, .2f, .6f, .01f);
		final Object me = this;
		// Needs to swallow all touches to the dungeon area 
		touchEventProvider.addTouchEventListener(this, gui.sizeCalculator.dungeonArea, gui.cameraViewPort.viewPort);  
		
		EventBus.getDefault().onEvent(TutorialPresenter.EFFECT_TAB_ON_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				EventBus.getDefault().event(TutorialPresenter.EFFECT_TAB_BUTTON_OFF_EVENT, null);
				fadeBox.dismiss(); 
				addMoreFaderBoxes();
				EventBus.getDefault().removeListener(TutorialPresenter.EFFECT_TAB_ON_EVENT, me);
			}
		});
		
		this.fadeBox = new FadeBoxPresenter("You can see your characters stats and other abilities effecting it using the Effects tab.  Click on the Effects tab now.", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		fadeBox.setNoTouch();
		gui.overlayQueues.addParallel(fadeBox);
		
		EventBus.getDefault().event(TutorialPresenter.EFFECT_TAB_BUTTON_ON_EVENT, null);
	}
	
	private void addMoreFaderBoxes() {
		FadeBoxPresenter fb1 = new FadeBoxPresenter("bing", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		gui.overlayQueues.addSequential(fb1);
		
		FadeBoxPresenter fb3 = new FadeBoxPresenter("bada", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		gui.overlayQueues.addSequential(fb3);
		
		final OverlayPresenter me = this;
		FadeBoxPresenter fb2 = new FadeBoxPresenter("boom", 
				HAlignment.CENTER, VAlignment.BOTTOM, new IDismissListener() {
			public void dismiss() {
				me.dismiss();
			}
		});
		gui.overlayQueues.addSequential(fb2);
	}
	
	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y) {
	}
	
	@Override
	public boolean touchEvent(TouchEvent event) {
		return true;
	}
	
	public void destroy() {
		touchEventProvider.removeTouchEventListener(this);
	}

}
