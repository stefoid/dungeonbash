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
import com.dbash.util.Rect.HAlignment;

public class MenuListPresenter extends ListPresenter implements TouchEventProvider {
	
	public static final int HELP_TEXT_SIZE = 6;
	public static final int HEADING_SIZE = 5;
	
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
				null,       // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"HOW TO PLAY",
				HEADING_SIZE, HAlignment.CENTER, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				null,       // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"Your three characters drop into a level one at a time.  Characters have a faint highlight around them.  Monsters do not.",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				new ImageView(gui, "HIGHLIGHT", elementArea),      // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"The animated highlight shows whose turn it is.",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				new ImageView(gui, "SWIPE", elementArea),      // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"Swipe in a direction to move or melee attack.",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				new ImageView(gui, "RANGED", elementArea),       // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"Highlight a ranged weapon on the blue tab (if you have one) and touch the target tile to shoot.",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				new ImageView(gui, "LEADER", elementArea),     // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"Use leader mode to control the team as a group - when no monsters are around the crown button  becomes enabled - Swipe and release on the destination tile to walk to that destination.",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				null,       // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"To manage your inventory, walk to a tile with stuff on it, and use the green tab to pickup and drop items (max 10).",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				null,       // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"Equip one weapon, one defensive item and one amulet using the blue tab - click on the item to be equipped.",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				null,       // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"To go to the next level, find the stairs, and put a character on it - the 'stairs' button will become enabled.",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				null,       // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"Characters appear on the next level in the same order that they left the previous one, and the next character cannot appear until the landing spot is vacated, so be careful.﻿",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
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
		Rect vp = new Rect(viewport) {
			@Override
			public float getY() {
				return y + listArea.y + scrollingList.getListPosition();
			}
		};

		touchEventProvider.addTouchEventListener(listener, touchArea, vp);
		
	}

	@Override
	public void removeTouchEventListener(TouchEventListener listener) {
		touchEventProvider.removeTouchEventListener(listener);
	}
}