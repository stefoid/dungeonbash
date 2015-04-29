package com.dbash.presenters.root.tutorial;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.IAnimListener;
import com.dbash.models.TouchEvent;
import com.dbash.models.TouchEventListener;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.FadeBox;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.root.IDismissListener;
import com.dbash.presenters.root.OverlayPresenter;
import com.dbash.util.Rect;
import com.dbash.util.Rect.HAlignment;
import com.dbash.util.Rect.VAlignment;
import com.dbash.util.Tween;


public class FadeBoxPresenter extends OverlayPresenter implements TouchEventListener {
	
	FadeBox fadeBox;
	String text;
	HAlignment hAlign;
	VAlignment vAlign;
	IDismissListener onDismiss;
	boolean useTouch;
	boolean touchableNow;
	Tween fadeTween;
	
	public FadeBoxPresenter(String text, HAlignment hAlign, VAlignment vAlign, IDismissListener onDismiss) {
		this.text = text;
		this.hAlign = hAlign;
		this.vAlign = vAlign;
		this.onDismiss = onDismiss;
		fadeTween = new Tween();
		useTouch = true;
		touchableNow = false;
	}
	
	public void setNoTouch() {
		this.useTouch = false;
	}
	
	@Override
	public void init(UIDepend gui) {
		this.gui = gui;
	}
	
	@Override
	public void start(Rect theArea, TouchEventProvider touchEventProvider) {
		fadeTween.init(0f,  1f,  1f, new IAnimListener() {
			@Override
			public void animEvent() {
				touchableNow = true;
			}
		});
		this.touchEventProvider = touchEventProvider;
		this.area = new Rect(gui.sizeCalculator.dungeonArea, .1f, .1f, .6f, .01f);
		
		// Needs to swallow all touches to the dungeon area 
		if (useTouch) {
			touchEventProvider.addTouchEventListener(this, theArea, gui.cameraViewPort.viewPort);
		}  
		
		this.fadeBox = new FadeBox(gui, text, area);
	}
	
	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		fadeTween.deltaTime(Gdx.graphics.getDeltaTime());
		fadeBox.draw(spriteBatch, x, y, fadeTween.getValue());
	}
	
	@Override
	public boolean touchEvent(TouchEvent event) {
		if (touchableNow) {
			dismiss();
		}
		return true;
	}
	
	public void destroy() {
		if (touchEventProvider != null) {
			touchEventProvider.removeTouchEventListener(this);
		}
		
		if (onDismiss != null) {
			onDismiss.dismiss();
		}
	}

}
