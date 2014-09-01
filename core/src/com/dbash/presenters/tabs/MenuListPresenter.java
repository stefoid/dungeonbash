package com.dbash.presenters.tabs;

import java.util.ArrayList;

import com.dbash.models.Character;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.IListElement;
import com.dbash.presenters.widgets.ListPresenter;
import com.dbash.util.Rect;

public class MenuListPresenter extends ListPresenter{
	
	private boolean haveCreatedList = false;
	
	public MenuListPresenter(PresenterDepend model, UIDepend gui, TouchEventProvider touchEventProvider, Rect area) {
		super(model, gui, touchEventProvider, area);
	}
	
	// Create the menu list and all its things.  Only needs to be done once, so we set a flag.
	@Override
	public void listInfoUpdate() {
		if (haveCreatedList == false) {
			haveCreatedList = true;
			
			ArrayList<IListElement> elements = new ArrayList<IListElement>();
			
			Rect menuArea = new Rect(elementArea);
			MenuListElementView menuElement = new MenuListElementView(gui, menuArea, touchEventProvider);
			menuElement.addToList(elements);
			
			// always draws at the same position it already was at before.
			scrollingList.setListElements(elements, scrollingList.getListPosition());
		}
	}
	
	@Override
	protected void newCharacter(Character character) {	
		super.newCharacter(character);
	}
}