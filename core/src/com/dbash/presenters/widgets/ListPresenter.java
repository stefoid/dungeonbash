package com.dbash.presenters.widgets;

import java.util.HashMap;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.Character;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEventProvider;
import com.dbash.models.UIInfoListener;
import com.dbash.platform.ImagePatchView;
import com.dbash.platform.SizeCalculator;
import com.dbash.platform.UIDepend;
import com.dbash.util.Rect;



// Takes a list of AbilityInfo derived from the owners abilities and 
// displays it as a ScrollingList of ListElements.
public abstract class ListPresenter {
	
	protected ScrollingListView scrollingList;
	protected UIDepend gui;
	
	protected float elementHeight;
	protected float elementWidth;
	protected Rect elementArea;
	protected PresenterDepend model;
	protected HashMap<Character, Float> characters;
	protected Character oldCharacter;
	protected ImagePatchView border;
	protected TouchEventProvider touchEventProvider;
	
	public ListPresenter(PresenterDepend model, UIDepend gui, TouchEventProvider touchEventProvider, Rect area) {
		elementWidth = area.width; // TODO not neccessarilly!
		elementHeight = SizeCalculator.LIST_ELEMENT_HEIGHT;
		elementArea = new Rect(0, 0, elementWidth, elementHeight);
		this.gui = gui;
		this.model = model;
		this.characters = new HashMap<Character, Float>();
		scrollingList = new ScrollingListView(gui, touchEventProvider, area, elementHeight);
		this.border = new ImagePatchView(gui, "9patchlistsides", area); 
		oldCharacter = null;
		this.touchEventProvider = touchEventProvider;
		setup();
	}

	protected void setup()
	{
		// Subscribe to changes to the current character.
		newCharacter(model.presenterTurnState.getCurrentCharacter());
		model.presenterTurnState.onChangeToCurrentCharacter(new UIInfoListener() {
			public void UIInfoChanged() {
				Character character = model.presenterTurnState.getCurrentCharacter();
				if (character.isPlayerCharacter()) {
					newCharacter(character);
				}
			}
		});
	}
	
	// Called when the underlying ability list in the model changes in some way.
	// We take the new list of <whatever>, create a new list of <whatever>ListElements out of it and
	// tell our ScrollingList to use that.
	protected abstract void listInfoUpdate();
	
	protected void newCharacter(Character character)
	{	
		// save the listPosition of the previous character for next time it is called up.
		if (oldCharacter != null) {
			characters.put(oldCharacter, new Float(scrollingList.getListPosition()));
		}
		
		oldCharacter = character;
		
		// used to save the list position for the character, if we havent seen this character before.
		if (characters.containsKey(character) == false) {
			characters.put(character, new Float(0f));
		} 
		
		listInfoUpdate();
	}
	
	public void draw(SpriteBatch spriteBatch, float x, float y)
	{
		if (scrollingList != null) {
			scrollingList.draw(spriteBatch, x, y);
		}
		
		border.draw(spriteBatch);
	}

	public void activate() {
		listInfoUpdate();
		if (scrollingList != null) 
			scrollingList.activate();
	}
	
	public void deactivate() {
		// save the old list position for this character
		saveListPosition();
		if (scrollingList != null)
			scrollingList.deactivate();
	}
	
	protected void saveListPosition()
	{
		characters.put(model.presenterTurnState.getCurrentCharacter(), new Float(scrollingList.getListPosition()));
	}
}