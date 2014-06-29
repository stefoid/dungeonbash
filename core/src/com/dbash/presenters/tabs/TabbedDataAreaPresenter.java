package com.dbash.presenters.tabs;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEvent;
import com.dbash.models.TouchEventListener;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.ImageView;
import com.dbash.platform.SizeCalculator;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.TabSetPresenter;
import com.dbash.util.Rect;


public class TabbedDataAreaPresenter implements TouchEventListener {

	private ImageView				background;
	private TabSetPresenter			tabs;
	private DataHeaderPresenter 	dataHeaderArea;
	private Rect 					area;
	private UIDepend				gui;
	private PresenterDepend 		model;
	
	// The area passed to this presenter, like any presenter, is where it is suppsoed to draw in 'world' coordinates.
	// the cameraViewport passed in the gui dependencies is the the one used to draw this presenter and its children.
	public TabbedDataAreaPresenter(UIDepend gui, PresenterDepend model, TouchEventProvider touchEventProvider, Rect area) {
		
		this.model = model;
		this.gui = new UIDepend(gui);
		background = null;
		setDetails(touchEventProvider, area);
	}
	
	public void createBackground() {
		background = new ImageView(gui, "TAB_PANE_IMAGE", area); 
	}

	public void draw(SpriteBatch spriteBatch) {
		if (background != null) {    // make sure not to draw until details have been set.
			gui.cameraViewPort.use(spriteBatch);
			spriteBatch.begin();
			
			background.draw(spriteBatch);
			dataHeaderArea.draw(spriteBatch);
			tabs.draw(spriteBatch);
			
			spriteBatch.end();
		}
	}


	// If a particular touch event percolates up from the bottom of the view tree as far as here
	// For now we catch it and return it as unused.
	public boolean touchEvent(TouchEvent event) {
		// Put some code here and return true if the datamanager needs to do something with touch events
		// that isnt handled by its children.
		return false;
	}
	
	public void setDetails(TouchEventProvider touchEventProvider, Rect area) {
		this.area = new Rect(area);
		
		// We are the last port of call for any touches in the data area.  Probably not needed.
		touchEventProvider.addTouchEventListener(this, area, gui.cameraViewPort.viewPort);
		
		// start building gui according to passed in coords.
		this.background = new ImageView(gui, "TAB_PANE_IMAGE", area);
		
		// create tabs: they take up the bottom 80% of the data area.
		tabs = new TabSetPresenter();
		Rect tabArea = new Rect(area, 0, 0, SizeCalculator.DATA_HEADER_SCALE, 0);
		tabs.create(model, gui, touchEventProvider, tabArea);
		
		// create data header area.  Takes up top 20% of data area
		Rect headerArea = new Rect(area, 0, 0, 0, SizeCalculator.TAB_AREA_SCALE);
		dataHeaderArea = new DataHeaderPresenter(model, gui, touchEventProvider, headerArea);
	}
}
