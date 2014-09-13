package com.dbash.presenters.tabs;

import java.util.ArrayList;

import com.dbash.models.Character;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEventListener;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.ImageView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.IClickListener;
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

		menuElement.helpButton.onClick( new IClickListener() {
			public void processClick() {
				scrollingList.scroll(-40f);
			}
		});
		
		helpElements.add(new ImageTextListElementView(gui,
				null,       // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"HOW TO PLAY",
				HEADING_SIZE, HAlignment.CENTER, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				null,       // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"In dungeon Bash, you control a team of three different characters.  The aim is to descend to level 20 and kill the powerful evil wizard.", 
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				null,       // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"Your three characters drop into a level one at a time.  The next character cannot appear until the landing spot is clear.", 
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				new ImageView(gui, "HIGHLIGHT", elementArea),      // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"Characters have a faint highlight around them.  Monsters do not.    The animated highlight shows whose turn it is.",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				new ImageView(gui, "SWIPE", elementArea),      // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"Swipe in a direction to move or melee attack.",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				new ImageView(gui, "RANGED", elementArea),       // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"Highlight a ranged item (if you have one) on the blue 'abilities' tab and touch the target tile to shoot.  Ranged items have a target symbol",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				new ImageView(gui, "LEADER_BUTTON", elementArea),     // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"Use leader mode to control the team as a group - when no monsters are around the crown button  becomes enabled.",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				new ImageView(gui, "LEADER", elementArea),     // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"Swipe and release on the target tile to walk to that destination.",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				new ImageView(gui, "SOLO", elementArea),     // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"Use Solo mode to control only one character.  Other Characters will skip their turns.",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				new ImageView(gui, "PASS", elementArea),     // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"Use the Hand button for the current character to pass their turn.",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				new ImageView(gui, "INVENTORY", elementArea),     // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"To manage your inventory, walk to a tile with stuff on it, and use this tab to drop and pick up items (max 10).",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				new ImageView(gui, "ITEMS", elementArea),     // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"Green items are in your inventory.  Red items are on the ground.  Items that cannot be picked up by that character up will appear grey.",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				new ImageView(gui, "EQUIP", elementArea),     // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"Equip one weapon, one defensive item and one amulet using the blue tab - click on the item to be equipped.",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				new ImageView(gui, "STAIRS", elementArea),     // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"To go to the next level, find the stairs, and put a character on it - the 'stairs' button will become enabled.",
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
		
		// Pass in a viewport that 'moves' according to the scroll position.
		Rect vp = new Rect(listArea) {
			@Override
			public float getY() {
				return y + scrollingList.getListPosition();
			}
		};
		
		vp.x += viewport.x;
		vp.y += viewport.y;

		touchEventProvider.addTouchEventListener(listener, touchArea, vp);
		
	}

	@Override
	public void removeTouchEventListener(TouchEventListener listener) {
		touchEventProvider.removeTouchEventListener(listener);
	}
}