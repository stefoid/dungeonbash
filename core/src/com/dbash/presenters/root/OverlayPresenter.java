package com.dbash.presenters.root;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.UIDepend;
import com.dbash.util.Rect;

//OVERLAY PRESENTER BASE
//constructor takes the gui bag - it registers itself on construction
//it has a dimiss function that deregisters itself
//it has an abstract start function
//it has an abstract draw function
//probably has a couple of functions to register and deregister itself with the touch event provider.
public abstract class OverlayPresenter {

	protected UIDepend gui;
	protected Rect area;
	protected TouchEventProvider touchEventProvider;
	
	public abstract void init(UIDepend gui);
	
	public abstract void start(Rect area, TouchEventProvider touchEventProvider);
	
	public abstract void draw(SpriteBatch spriteBatch, float x, float y);
	
	public void dismiss() {
		destroy();
		gui.overlayQueues.remove(this);
	}
	
	public abstract void destroy();
}
