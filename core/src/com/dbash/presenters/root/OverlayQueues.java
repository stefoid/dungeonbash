package com.dbash.presenters.root;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.UIDepend;
import com.dbash.util.Rect;

//OVERLAY_PRESENTER_QUEUES
//Put this in the gui bag.  
//- actually, lets have two add functions - addParallel and addSequential.
//- this is just two collections.  It calls start on everything in the parralel collection when it is added,
//and calls draw on everything in that collection.
//- it only calls start on the first thing in the sequential colection.  
//it only calls draw in the first thing in the sequential collection.
//It is totally up the the overlay presenter itself to handle touch events and whatever.
public class OverlayQueues {

	private ArrayList<OverlayPresenter> parallelList;
	private ArrayList<OverlayPresenter> sequentialList;
	private UIDepend gui;
	private Rect area;
	private TouchEventProvider touchEventProvider;
	
	public OverlayQueues() {
	}
	
	public void init(UIDepend gui, Rect area, TouchEventProvider touchEventProvider) {
		parallelList = new ArrayList<OverlayPresenter>();
		sequentialList = new ArrayList<OverlayPresenter>();
		this.gui = gui;
		this.area = new Rect(area);
		this.touchEventProvider = touchEventProvider;
	}
	
	public void addParallel(OverlayPresenter overlayPresenter) {
		parallelList.add(overlayPresenter);
		overlayPresenter.init(gui);
		overlayPresenter.start(area, touchEventProvider);
	}
	
	public void addSequential(OverlayPresenter overlayPresenter) {
		sequentialList.add(overlayPresenter);
		overlayPresenter.init(gui);
		if (sequentialList.size() == 1) {
			overlayPresenter.start(area, touchEventProvider);
		}
	}
	
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		gui.cameraViewPort.use(spriteBatch);
		spriteBatch.begin();

		for (OverlayPresenter overlayPresenter : parallelList) {
			overlayPresenter.draw(spriteBatch, x, y);
		}
		
		if (sequentialList.size() > 0) {
			OverlayPresenter overlayPresenter = sequentialList.get(0);
			overlayPresenter.draw(spriteBatch, x, y);
		}
		
		spriteBatch.end();
	}
	
	public void remove(OverlayPresenter overlayPresenter) {
		parallelList.remove(overlayPresenter);
		sequentialList.remove(overlayPresenter);
		if (sequentialList.size() > 0) {
			OverlayPresenter nextOne = sequentialList.get(0);
			nextOne.start(area, touchEventProvider);
		}
	}
}
