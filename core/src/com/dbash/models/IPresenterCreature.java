package com.dbash.models;

import com.dbash.platform.UIDepend;
import com.dbash.presenters.dungeon.CreaturePresenter;
import com.dbash.presenters.dungeon.MapPresenter;


public interface IPresenterCreature {

	public enum HighlightStatus{
		NO_HIGHLIGHT,
		HIGHLIGHTED,
		CURRENTLY_IN_FOCUS
	} 
	
	public CreaturePresenter getCreaturePresenter(UIDepend gui, PresenterDepend model, MapPresenter mapPresenter);
	
	// On changes to visual aspects of the creature
	//public void onChangeToVisualStatus(UIInfoListener listener);
	public HighlightStatus getHighlightStatus();
	public DungeonPosition getPosition();
	
	public String getNameUnderscore();


}
