package com.dbash.presenters.root.tutorial;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.IEventAction;
import com.dbash.models.TouchEvent;
import com.dbash.models.TouchEventListener;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.FadeBox;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.root.IDismissListener;
import com.dbash.presenters.root.OverlayPresenter;
import com.dbash.util.EventBus;
import com.dbash.util.Rect;
import com.dbash.util.Rect.HAlignment;
import com.dbash.util.Rect.VAlignment;


public class LeaderModePresenter extends OverlayPresenter implements TouchEventListener {
	
	FadeBoxPresenter fadeBox;
	
	public LeaderModePresenter() {
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
		touchEventProvider.addTouchEventListener(this, gui.sizeCalculator.dungeonArea, gui.cameraViewPort.viewPort);  
		
		EventBus.getDefault().onEvent(TutorialPresenter.LEADER_ON_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				EventBus.getDefault().event(TutorialPresenter.ANIM_LEADER_BUTTON_OFF_EVENT, null);
				fadeBox.dismiss(); 
				addMoreFaderBoxes();
			}
		});
		
		this.fadeBox = new FadeBoxPresenter("When no monsters are around, leader mode can be used to move all characters.\n\nPress the leader mode button now (top right).", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		fadeBox.setNoTouch();
		gui.overlayQueues.addParallel(fadeBox);
		
		EventBus.getDefault().event(TutorialPresenter.ANIM_LEADER_BUTTON_ON_EVENT, null);
	}
	
	private void addMoreFaderBoxes() {
		FadeBoxPresenter fb1 = new FadeBoxPresenter("You can swipe to move your leader. Where your finger touches off sets a target the leader will walk to.", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		gui.overlayQueues.addSequential(fb1);
		
		final OverlayPresenter me = this;
		FadeBoxPresenter fb2 = new FadeBoxPresenter("Once a leader target has been set you can tap again to update the target.  Now guide your leader into the next room to the right...", 
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
		EventBus.getDefault().removeListener(TutorialPresenter.LEADER_ON_EVENT, this);
		touchEventProvider.removeTouchEventListener(this);
	}

}
