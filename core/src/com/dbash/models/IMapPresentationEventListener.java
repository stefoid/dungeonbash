package com.dbash.models;

public interface IMapPresentationEventListener {

	public void setMap(Map map);
	// Specifically a change of focus to the a new character to receive user input.
	public void changeCurrentCharacterFocus(int sequenceNumber, Character newFocusCharacter);
	
	// animated scroll to the new focus position.
	public void animatedFocusChange(int sequenceNumber, Character newFocusCharacter, float period, boolean characterMoving, IAnimListener animCompleteListener);

	// This sets the focus position on the map in one step (no anim scroll).
	public void instantFocusChange(DungeonPosition focusPosition, ShadowMap shadowMap);
	
	// When a character dies or goes down stairs, this will be called to tell the map just to keep the current focus
	// until the next focus change is ordered, and when that happens, make a callback.
	public void retainViewUntilNextFocusChange(ShadowMap focussedCharacterMap, UIInfoListener nextFocusChangeListener);
	
}
