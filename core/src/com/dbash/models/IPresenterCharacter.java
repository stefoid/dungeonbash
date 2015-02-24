package com.dbash.models;

import com.dbash.presenters.tabs.AbilitySelectionList;



// The interface for Presenters to talk to the model about the current character
// i.e. keeping track of the current characters stats
// keeping track of the current characters abilities
// attempted ability use
//
// The implementer of this interface changes as the current character changes.
// Users of this interface can be aware of which the current character is by listening to the
// IPresenterTurnState interface.

public interface IPresenterCharacter {

	// Character abilities of whichever character this is
	public void onChangeToAbilitySelectionList(UIInfoListener listener);
	public AbilitySelectionList getAbilitySelectionList();
	
	// Ability selected for use.
	public boolean abilitySelected(Ability ability);
	
	// When touchdown event happens on the map.
	public void targetTileSelected(DungeonPosition postition);  // Use a DungeonQuery to see what is at that position.
	public void movementGesture(int direction, DungeonPosition postition);

	// Set by Eye tab presenter when a new character gets focus, and whenever using the eye tab changes.
	public void setCharacterisUsingEye(boolean usingEye);
	
	// Character abilities of whichever character this is
	public void onChangeToCharacterStats(UIInfoListener listener);
	public CreatureStats getCharacterStats();
	
	// Character abilities of whichever character this is
	public void onChangeToStealthStatus(UIInfoListener listener);
	
	// Character effects affecting whichever character this is
	public void onChangeToEffectList(UIInfoListener listener);
	public EffectList getEffectList();
	
	// Character inventory
	public void onChangeToInventory(UIInfoListener listener);
	public ItemList getItemList();
	
	public boolean itemPickupSelected(Ability ability);
	public void performPickup(Ability ability);
	public void itemDropSelected(Ability ability);
	
	// Stairs status
	public boolean isCharacterOnStairs();
}
