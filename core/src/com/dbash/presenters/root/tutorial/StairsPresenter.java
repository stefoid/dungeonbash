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


public class StairsPresenter extends TutorialPopupPresenter implements TouchEventListener {
	
	FadeBoxPresenter fadeBox;
	
	public StairsPresenter() {
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
		
		EventBus.getDefault().onEvent(TutorialPresenter.HOME_TAB_ON_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				EventBus.getDefault().event(TutorialPresenter.HOME_TAB_BUTTON_OFF_EVENT, null);
				fadeBox.dismiss(); 
				addMoreFaderBoxes();
				EventBus.getDefault().removeListener(TutorialPresenter.HOME_TAB_ON_EVENT, me);
				dismiss();
			}
		});
		
		this.fadeBox = new FadeBoxPresenter("Great work!  That's it for the tutorial.\n  For further tips, press the help button on the main menu.", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		fadeBox.setNoTouch();
		addFadeBoxPar(fadeBox);
		
		EventBus.getDefault().event(TutorialPresenter.HOME_TAB_BUTTON_ON_EVENT, null);
	}
	
	private void addMoreFaderBoxes() {
		
		FadeBoxPresenter fb3 = new FadeBoxPresenter("Now find the stairs somewhere on this level and exit your team.  To exit, just put your character on the stairs and the solo button will turn to a stairs button.", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		addFadeBoxSeq(fb3);
		
		final OverlayPresenter me = this;
		FadeBoxPresenter fb2 = new FadeBoxPresenter("Your team will automatically try to hide, although they may be instantly discovered.  Remember they will enter the next level in the same order that they leave.", 
				HAlignment.CENTER, VAlignment.BOTTOM, new IDismissListener() {
			public void dismiss() {
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
		EventBus.getDefault().removeListener(TutorialPresenter.HOME_TAB_ON_EVENT, this);
		touchEventProvider.removeTouchEventListener(this);
	}

}
