package com.dbash.presenters.root.tutorial;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.TouchEvent;
import com.dbash.models.TouchEventListener;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.root.IDismissListener;
import com.dbash.presenters.root.OverlayPresenter;
import com.dbash.util.Rect;
import com.dbash.util.Rect.HAlignment;
import com.dbash.util.Rect.VAlignment;


public class DroppingInPresenter extends OverlayPresenter implements TouchEventListener {
	
	public DroppingInPresenter() {
	}
	
	@Override
	public void init(UIDepend gui) {
		this.gui = gui;
	}
	
	@Override
	public void start(Rect theArea, TouchEventProvider touchEventProvider) {
		FadeBoxPresenter fb1 = new FadeBoxPresenter("Characters drop into a level one at a time.\n\nTouch anywhere to continue...", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		gui.overlayQueues.addSequential(fb1);
		
		FadeBoxPresenter fb2 = new FadeBoxPresenter("The landing spot must be clear before the next one can arrive.", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		gui.overlayQueues.addSequential(fb2);
	
		FadeBoxPresenter fb3 = new FadeBoxPresenter("The animated highlight shows which character is having a turn.\n\nYou can swipe in a direction to move.", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		gui.overlayQueues.addSequential(fb3);
		
		final OverlayPresenter me = this;
		FadeBoxPresenter fb4 = new FadeBoxPresenter("Move four times with swiping right now.", 
				HAlignment.CENTER, VAlignment.BOTTOM, new IDismissListener() {
			public void dismiss() {
				me.dismiss();
			}
		});
		gui.overlayQueues.addSequential(fb4);
	}
	
	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y) {
	}
	
	@Override
	public boolean touchEvent(TouchEvent event) {
		return false;
	}
	
	@Override
	public void destroy() {
	}

}
