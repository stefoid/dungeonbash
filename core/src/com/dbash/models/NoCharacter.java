package com.dbash.models;

import java.util.ArrayList;

import com.dbash.presenters.tabs.AbilitySelectionList;


public class NoCharacter extends Character {

	// Exists to be the current character when no actual character is having a turn.
	// Will override any functions that shouldnt do anything when there isnt a valid character
	// Beats the rest of the code having to check for null all the time.
	
	public NoCharacter()
	{
		super(5, new DungeonPosition (0, 0), 99, null, null, null);
		abilities = new ArrayList<Ability>();
		creature = new Data();
		creature.name = "nobody";
	}
	
	public void targetTileSelected(DungeonPosition postition) {
		
	}
	
	@Override
	public boolean isPlayerCharacter()
	{
		return false;
	}
	
	public AbilitySelectionList getAbilitySelectionList() {
		return new AbilitySelectionList(this);
	}
	
	public void movementGesture(int direction, DungeonPosition postition) {

	}

	@Override
	public void onChangeToAbilitySelectionList(UIInfoListener listener) {
		
	};
	
	@Override
	public void onChangeToCharacterStats(UIInfoListener listener) {
		
	};
	
	@Override
	public boolean isCharacterOnStairs() {
		return false;
	}
	
	@Override
	public void setCharacterisUsingEye(boolean usingEye) {
	}
	
}
