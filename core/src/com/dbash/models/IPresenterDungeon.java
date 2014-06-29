package com.dbash.models;



//The interface for Presenters to talk to the model about the dungeon.
// like the focus of the eye being changed
// or stuff to do with the stairs.

public interface IPresenterDungeon {

	// Eye related API
	public void onChangeToEyeDetails(UIInfoListener listener);
	public ItemList getItemsAtEyepos(); 
	public Creature getCreatureAtEyePos();
	
	// items - used by inventory list.
	public ItemList getItemsAtPosition(DungeonPosition position);

	// So here we need some functions for the DungeonAreaPresenter to get visible presentation events such as
	// such as creatures moving, attacking, ranged attacks, burst effects, etc... 
	public void onVisibleDungeonEvent(IDungeonPresentationEventListener listener);
	
	// here things get changes of focus events that will require scrolling, etc...
	public void onMapEvent(IMapPresentationEventListener listener);
	
}
