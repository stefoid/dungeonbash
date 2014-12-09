package com.dbash.models;

public interface IMapPresentationEventListener {

	public void setMap(Map map);
	// Specifically a change of focus to the a new character to receive user input.
	public void changeCurrentCharacterFocus(int sequenceNumber, Character newFocusCharacter);
	
	// animated change focus to position rather than a character.
	public void changeFocusToPosition(int sequenceNumber, DungeonPosition position);
	
	// This sets the focus position on the map in one step (no anim scroll).
	public void instantFocusChange(DungeonPosition focusPosition, ShadowMap shadowMap);
	
	// animated scroll to the new focus position.  basic implementation used by above functions.
	public void animatedFocusChange(int sequenceNumber, ShadowMap shadowMap, DungeonPosition positiotn, float period, boolean characterMoving, IAnimListener animCompleteListener);
	
	public void updateMapPresentation();
}
