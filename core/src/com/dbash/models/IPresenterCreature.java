package com.dbash.models;

import com.dbash.presenters.dungeon.CreaturePresenter;


public interface IPresenterCreature {

	public enum HighlightStatus{
		NO_HIGHLIGHT,
		HIGHLIGHTED,
		CURRENTLY_IN_FOCUS
	} 
	
	// There is a 1:1 correspondence between a Creature and a CreaturePresenter so 
	// the best thing to attach the presenter to its creature so we can find it easily as it 
	// is passed around Locations.
	public void setCreaturePresenter(CreaturePresenter creaturePresetner);
	public CreaturePresenter getCreaturePresenter();
	
	// On changes to visual aspects of the creature
	//public void onChangeToVisualStatus(UIInfoListener listener);
	public HighlightStatus getHighlightStatus();
	public DungeonPosition getPosition();
	
	public String getNameUnderscore();


}
