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
	
	public LocationInfo(Location location) {
		this.location = location;
		update(location);
	}
	
	public void update(Location location) {
		tileName = location.tileName;
		if (location.hardcodeTilename == null) {
			addPrefix = true;
		}
		creature = location.creature;
		isDiscovered = location.isDiscovered;
		itemList = location.getItemList(true);
		tint = location.tint;
		torch = location.torch;
		roughTerrainType = location.getRoughTerrain();
		if (location.locationType == Location.LocationType.FLOOR) {
			if (location.tileName.equals("CLEAR_FLOOR_IMAGE") == false) {
				isShadowedFloor = true;
			} else {
				isShadowedFloor = false;
			}
		}
		if (location.tileType == Location.TileType.ISLAND) {
			isIsland = true;
		}
	}
	
	public String toString() {
		return "TORCH: "+torch;
	}
}
