package com.dbash.models;

import java.util.Set;

// LocationInfo is information created by a Location model object for Presenter consumption.
// It contains everything the Presenter needs to know to visually represent that location.
public class LocationInfo {

	public String tileName;
	public IPresenterCreature creature;
	public boolean isDiscovered;
	public Set<ShadowMap> shadowMaps;
	public ItemList itemList;
	
	public LocationInfo(Location location)
	{
		this.tileName = location.tileName;
		this.creature = location.creature;
		this.isDiscovered = location.isDiscovered;
		this.shadowMaps = location.shadowMaps.keySet();
		this.itemList = location.getItemList();
	}
}
