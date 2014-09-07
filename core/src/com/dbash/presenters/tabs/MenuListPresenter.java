package com.dbash.presenters.tabs;

import java.util.ArrayList;

import com.dbash.models.Character;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEventListener;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.ImageView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.IListElement;
import com.dbash.presenters.widgets.ListPresenter;
import com.dbash.util.Logger;
import com.dbash.util.Rect;

public class MenuListPresenter extends ListPresenter implements TouchEventProvider {
	
	MenuListElementView menuElement;
	ArrayList<ImageTextListElementView> helpElements;
	
	public MenuListPresenter(PresenterDepend model, UIDepend gui, TouchEventProvider touchEventProvider, Rect area) {
		super(model, gui, touchEventProvider, area);
		
		helpElements = new ArrayList<ImageTextListElementView>();
		ArrayList<IListElement> elements = new ArrayList<IListElement>();
		
		Rect menuArea = new Rect(elementArea);
		menuElement = new MenuListElementView(model, gui, menuArea, this);  // we intercept toucheventprovider calls
		menuElement.addToList(elements);
//		
		helpElements.add(new ImageTextListElementView(gui,
				new ImageView(gui, "MENU_TAB_IMAGE", elementArea),       // the image
				new ImageView(gui, "MENU_BGROUND_IMAGE", elementArea),	 // the backgroud image for the element
				"This is some test text that may or may not do anything intersting."
				+ "Lets see how much text I can type and what happens - do I get the extra Elements that I was after, or what?",
				elementArea));
				
		helpElements.add(new ImageTextListElementView(gui,
				new ImageView(gui, "MENU_TAB_IMAGE", elementArea),       // the image
				new ImageView(gui, "MENU_BGROUND_IMAGE", elementArea),	 // the backgroud image for the element
				null,
				elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				null,       // the image
				new ImageView(gui, "MENU_BGROUND_IMAGE", elementArea),	 // the backgroud image for the element
				"This is some test text that may or may not do anything intersting."
				+ "Lets see how much text I can type and what happens - do I get the extra Elements that I was after, or what?",
				elementArea));
		
		for (ImageTextListElementView helpElement: helpElements) {
			helpElement.addToList(elements);
		}
		
		// always draws at the same position it already was at before.
		scrollingList.setListElements(elements, scrollingList.getListPosition());
	}
	
	@Override
	public void listInfoUpdate() {
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
	public void addTouchEventListener(TouchEventListener listener, Rect touchArea, Rect viewport) {
		Logger.log("adding TEL "+listArea.y);
		Rect vp = new Rect(viewport) {
			@Override
			public float getY() {
				return y +listArea.y + scrollingList.getListPosition();
			}
		};

		touchEventProvider.addTouchEventListener(listener, touchArea, vp);
		
	}

	@Override
	public void removeTouchEventListener(TouchEventListener listener) {
		Logger.log("remove TEL "+listener);;
		touchEventProvider.removeTouchEventListener(listener);
	}
}