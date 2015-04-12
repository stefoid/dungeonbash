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


public class ItemPresenter extends OverlayPresenter implements TouchEventListener {
	
	FadeBoxPresenter fadeBox;
	
	public ItemPresenter() {
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
		
		EventBus.getDefault().onEvent(TutorialPresenter.ITEM_TAB_ON_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				EventBus.getDefault().event(TutorialPresenter.ITEM_TAB_BUTTON_OFF_EVENT, null);
				fadeBox.dismiss(); 
				addMoreFaderBoxes();
				EventBus.getDefault().removeListener(TutorialPresenter.ITEM_TAB_ON_EVENT, this);
			}
		});
		
		this.fadeBox = new FadeBoxPresenter("If you walk to a tile with stuff on, you can pick it up using the item tab.  Click the item tab now.", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		fadeBox.setNoTouch();
		gui.overlayQueues.addParallel(fadeBox);
		
		EventBus.getDefault().event(TutorialPresenter.ITEM_TAB_BUTTON_ON_EVENT, null);
	}
	
	private void addMoreFaderBoxes() {
		FadeBoxPresenter fb1 = new FadeBoxPresenter("When you walk to the tile with items on it, you will see those items appear in the item list.", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		gui.overlayQueues.addSequential(fb1);
		
		FadeBoxPresenter fb3 = new FadeBoxPresenter("Items on the ground are red.  Items the character is carrying are green.  ITems that cannot be picked up are grey.", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		gui.overlayQueues.addSequential(fb3);
		
		final OverlayPresenter me = this;
		FadeBoxPresenter fb2 = new FadeBoxPresenter("Now walk to the tile where the monster dropped some itemspick up those items by clicking on them.", 
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
