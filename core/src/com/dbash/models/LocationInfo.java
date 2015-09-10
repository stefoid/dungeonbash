package com.dbash.models;

import com.dbash.models.Location.RoughTerrainType;

// LocationInfo is information created by a Location model object for Presenter consumption.
// It contains everything the Presenter needs to know to visually represent that location.
public class LocationInfo {

	public String tileName;
	public boolean addPrefix;
	public IPresenterCreature creature;
	public boolean isDiscovered;
	public ItemList itemList;
	public float tint; // how dark should this tile be?
	public Location.TorchType torch;
	public boolean isShadowedFloor;
	public RoughTerrainType roughTerrainType;
	public Location location;
	public boolean isIsland;
	public boolean isHardcoded;
	
	public LocationInfo(Location location) {
		this.location = location;
		update(location);
	}
	
	public void update(Location location) {
		tileName = location.tileName;
		if (location.isHardcoded) {
			isHardcoded = true;
			roughTerrainType = null;
		} else {
			addPrefix = true;
			isHardcoded = false;
			roughTerrainType = location.getRoughTerrain();
		}
		
		creature = location.creature;
		isDiscovered = location.isDiscovered;
		itemList = location.getItemList(true);
		tint = location.tint;
		torch = location.torch;
		
		isShadowedFloor = location.isShadowed;
		
		if (location.tileType == Location.TileType.ISLAND) {
			isIsland = true;
		}
	}
	
	public String getShadowName() {
		return location.shadowName;
	}
	
	public boolean requiresFloor() {
		return (isHardcoded || isIsland);
	}
	
	public String toString() {
		return "TORCH: "+torch;
	}
}
