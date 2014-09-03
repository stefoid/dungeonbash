package com.dbash.presenters.tabs;

import java.util.ArrayList;

import com.dbash.models.Character;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEventListener;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.IListElement;
import com.dbash.presenters.widgets.ListPresenter;
import com.dbash.util.Logger;
import com.dbash.util.Rect;

public class MenuListPresenter extends ListPresenter implements TouchEventProvider {
	
	private boolean haveCreatedList = false;
	MenuListElementView menuElement;
	
	public MenuListPresenter(PresenterDepend model, UIDepend gui, TouchEventProvider touchEventProvider, Rect area) {
		super(model, gui, touchEventProvider, area);
	}
	
	// Create the menu list and all its things.  Only needs to be done once, so we set a flag.
	// Unlike normal list elements, that are single-selection, the menuListelemnt has multiple touchable widgets
	// over a number of list element positions, so we let it handle touch events itself.
	@Override
	public void listInfoUpdate() {
		if (haveCreatedList == false) {
			haveCreatedList = true;
		
			ArrayList<IListElement> elements = new ArrayList<IListElement>();
			
			Rect menuArea = new Rect(elementArea);
			menuElement = new MenuListElementView(model, gui, menuArea, this);  // we intercept toucheventprovider calls
			menuElement.addToList(elements);
			
			// always draws at the same position it already was at before.
			scrollingList.setListElements(elements, scrollingList.getListPosition());
		}
	}
	
	@Override
	protected void newCharacter(Character character) {	
		super.newCharacter(character);
	}
	
	@Override
	public void activate() {
		super.activate();
		menuElement.activate();
		
	}
	@Override
	public void deactivate() {
		super.deactivate();
		menuElement.deactivate();
	}

	@Override
	/**
	 * intercept touch registration from the menu element view so we can add a y offset and list position.
	 */
	public void addTouchEventListener(TouchEventListener listener, Rect area,
			Rect viewport) {
		Rect newViewPort = new Rect(viewport);
		newViewPort.y += listArea.y;
		touchEventProvider.addTouchEventListener(listener, area, newViewPort);
	}

	@Override
	public void removeTouchEventListener(TouchEventListener listener) {
		touchEventProvider.removeTouchEventListener(listener);
	}
}