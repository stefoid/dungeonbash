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


public class CoverPresenter extends TutorialPopupPresenter implements TouchEventListener {
	
	FadeBoxPresenter fadeBox;
	
	public CoverPresenter() {
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
		addMoreFaderBoxes();
	}
	
	private void addMoreFaderBoxes() {
		FadeBoxPresenter fb1 = new FadeBoxPresenter("The detection skill of monsters is shown by how much light they emit.  Torches also make it hard to hide.", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		addFadeBoxSeq(fb1);
		
		FadeBoxPresenter fb3 = new FadeBoxPresenter("When a character has a monsters attention, its harder to hide, which is shown by how much light the character emits.", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		addFadeBoxSeq(fb3);
		
		FadeBoxPresenter fb4 = new FadeBoxPresenter("The basic rule for stealth is: stick to the shadows and avoid other characters who are drawing monster attention.", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		addFadeBoxSeq(fb4);
		
		FadeBoxPresenter fb42 = new FadeBoxPresenter("Rocks give great cover for stealth, particularly for small characters.  Bones and mud dont help Stealth.", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		addFadeBoxSeq(fb42);
		
		final OverlayPresenter me = this;
		FadeBoxPresenter fb2 = new FadeBoxPresenter("Use rocks to try to ambush the creature in the next room.  It has better perception.", 
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
		touchEventProvider.removeTouchEventListener(this);
	}

}
