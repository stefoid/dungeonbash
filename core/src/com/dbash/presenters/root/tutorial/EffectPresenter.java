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


public class EffectPresenter extends TutorialPopupPresenter implements TouchEventListener {
	
	FadeBoxPresenter fadeBox;
	
	public EffectPresenter() {
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
		
		this.fadeBox = new FadeBoxPresenter("You can see your characters stats and other abilities using the Effects tab.  Click on the Effects tab now.", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		fadeBox.setNoTouch();
		addFadeBoxPar(fadeBox);
		
		EventBus.getDefault().event(TutorialPresenter.EFFECT_TAB_BUTTON_ON_EVENT, null);
	}
	
	private void addMoreFaderBoxes() {
		FadeBoxPresenter fb1 = new FadeBoxPresenter("Abilities are shown at the top of the list.  Stats are shown beneath those.", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		addFadeBoxSeq(fb1);
		
		final OverlayPresenter me = this;
		FadeBoxPresenter fb2 = new FadeBoxPresenter("Have a look at your characters stats, then walk to the next room.", 
				HAlignment.CENTER, VAlignment.BOTTOM, new IDismissListener() {
			public void dismiss() {
				EventBus.getDefault().event(TutorialPresenter.ABILITY_PRESENTER_SHOWN, null);
				me.dismiss();
			}
		});
		addFadeBoxSeq(fb2);
	}
	
	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y) {
	}
	
	@Override
	public boolean touchEvent(TouchEvent event) {
		return true;
	}
	
	public void destroy() {
		EventBus.getDefault().removeListener(TutorialPresenter.EFFECT_TAB_ON_EVENT, this);
		touchEventProvider.removeTouchEventListener(this);
	}

}
