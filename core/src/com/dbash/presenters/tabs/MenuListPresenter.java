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
import com.dbash.util.Rect;
import com.dbash.util.Rect.HAlignment;

public class MenuListPresenter extends ListPresenter implements TouchEventProvider {
	
	public static final int HELP_TEXT_SIZE = 6;
	public static final int HEADING_SIZE = 5;
	
	MenuListElementView menuElement;
	ArrayList<ImageTextListElementView> helpElements;
	
	public MenuListPresenter(PresenterDepend model, UIDepend gui, TouchEventProvider touchEventProvider, final Rect area) {
		super(model, gui, touchEventProvider, area);
		
		helpElements = new ArrayList<ImageTextListElementView>();
		ArrayList<IListElement> elements = new ArrayList<IListElement>();
		
		Rect menuArea = new Rect(elementArea);
		menuElement = new MenuListElementView(model, gui, menuArea, this);  // we intercept toucheventprovider calls
		menuElement.addToList(elements);

		menuElement.helpButton.onClick( new IClickListener() {
			public void processClick() {
				scrollingList.scroll(-1*area.height/9.5f);
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
				"In dungeon Bash, you control a team of three different characters.  Your task is to descend to level 20 and kill the powerful evil wizard.", 
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				new ImageView(gui, "TUTORIAL", elementArea),      // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"Use the tutorial button when you start a new game to learn the basics of Dungeon Bash.",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				null,       // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"POWERUP!",
				HEADING_SIZE, HAlignment.CENTER, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				new ImageView(gui, "POWERUP", elementArea),      // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"Every time you kill a monster or exit a level, you recieve XP you can use to increase your character's stats and buy new abilities.", 
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				null,       // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"FLANKING",
				HEADING_SIZE, HAlignment.CENTER, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				new ImageView(gui, "FLANK", elementArea),     // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"In Melee, monsters prefer to attack orthogonal enemies, allowing flank attacks from the diagonal.",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				null,       // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"TERRAIN",
				HEADING_SIZE, HAlignment.CENTER, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				new ImageView(gui, "TERRAIN", elementArea),     // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"Some tiles have rough terrain: bones, mud or rocks.  These slow down non-flying creatures entering them.",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				new ImageView(gui, "HOLES", elementArea),     // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"There are also holes that only flying creatures can pass over.  But beware - stunning a flyer will cause it to fall into the hole and die, as will knocking a non-flyer into a hole.",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				null,       // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"STEALTH",
				HEADING_SIZE, HAlignment.CENTER, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				new ImageView(gui, "STEALTH", elementArea),     // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"The stealth button enables when the character has the option to hide.  Characters automatically try to hide when entering a new level.  Hiding characters get a big attack skill bonus for ambush.",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				new ImageView(gui, "LIGHT", elementArea),     // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"Sticking to the shadows helps characters hide.  Rocks are great cover for stealth, particularly for small-sized characters.  Getting noticed by monsters makes it harder to hide - being the closest to a monster attracts the most attention (displayed by light around the noticed character).",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				new ImageView(gui, "HIDING", elementArea),     // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"Use larger characetrs with poor steath skills to attract attention away from stealthy characters.  Then use shadow and rocks to move into ambush position.",
				HELP_TEXT_SIZE, HAlignment.LEFT, elementArea));
		
		helpElements.add(new ImageTextListElementView(gui,
				null,       // the image
				new ImageView(gui, "MENU_HELP_BACKGROUND", elementArea),	 // the backgroud image for the element
				"GOOD LUCK!!",
				HEADING_SIZE, HAlignment.CENTER, elementArea));
		
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