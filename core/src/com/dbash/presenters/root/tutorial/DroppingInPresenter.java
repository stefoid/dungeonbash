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


public class DroppingInPresenter extends TutorialPopupPresenter implements TouchEventListener {
	
	public DroppingInPresenter() {
	}
	
	@Override
	public void init(UIDepend gui) {
		super.init(gui);
		this.gui = gui;
	}
	
	@Override
	public void start(Rect theArea, TouchEventProvider touchEventProvider) {
		super.start(theArea, touchEventProvider);
		
		FadeBoxPresenter fb1 = new FadeBoxPresenter("Characters drop into a level one at a time.\n\nTouch anywhere to continue...", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		addFadeBoxSeq(fb1);
		
		FadeBoxPresenter fb2 = new FadeBoxPresenter("The landing spot must be clear before the next one can arrive.", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		addFadeBoxSeq(fb2);
	
		FadeBoxPresenter fb3 = new FadeBoxPresenter("The animated highlight shows which character is having a turn.", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		addFadeBoxSeq(fb3);
		
		FadeBoxPresenter fb32 = new FadeBoxPresenter("You can swipe in a direction to move.\n\nA swipe does not have to start on the character tile.", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		addFadeBoxSeq(fb32);
		
		final OverlayPresenter me = this;
		FadeBoxPresenter fb4 = new FadeBoxPresenter("Move 4 times by swiping right now.", 
				HAlignment.CENTER, VAlignment.BOTTOM, new IDismissListener() {
			public void dismiss() {
				me.dismiss();
			}
		});
		addFadeBoxSeq(fb4);
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
