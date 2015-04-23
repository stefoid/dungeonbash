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


public class TerrainPresenter extends OverlayPresenter implements TouchEventListener {
	
	FadeBoxPresenter fadeBox;
	
	public TerrainPresenter() {
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
		addMoreFaderBoxes();
	}
	
	private void addMoreFaderBoxes() {
		FadeBoxPresenter fb1 = new FadeBoxPresenter("Terrain such as mud, bones and rocks slow down any creature entering it.\nHowever it may also have good effects.", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		gui.overlayQueues.addSequential(fb1);
		
		FadeBoxPresenter fb3 = new FadeBoxPresenter("Bones and Rocks offer a defence bonus against missile fire and some protection from burst damage, particularly for small creatures.", 
				HAlignment.CENTER, VAlignment.BOTTOM, null);
		gui.overlayQueues.addSequential(fb3);
		
		final OverlayPresenter me = this;
		FadeBoxPresenter fb2 = new FadeBoxPresenter("You can see the effects of entering terrain on the effects tab.", 
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
