package com.dbash.models;

import com.dbash.models.Location.RoughTerrainType;
import com.dbash.models.Location.TileType;
import com.dbash.util.L;

// LocationInfo is information created by a Location model object for Presenter consumption.
// It contains everything the Presenter needs to know to visually represent that location.
public class LocationInfo {

	public String tileName;
	public String floorName;
	public String roughTerrainName;
	public String overlayName;
	
	public IPresenterCreature creature;
	public boolean isDiscovered;
	public ItemList itemList;
	public float tint; // how dark should this tile be?
	public Location.TorchType torch;
	public boolean isShadowedFloor;
	public Location location;
	public boolean isStraighFrontWall;
	public boolean drawEnlarged;
	
	public LocationInfo(Location location) {
		this.location = location;
		update(location);
	}
	
	public void update(Location location) {		
		roughTerrainName = location.roughTerrainName;
		tileName = location.tileName;
		overlayName = location.overlayName;
		floorName = location.floorName;
		creature = location.creature;
		isDiscovered = location.isDiscovered;
		itemList = location.getItemList(true);
		tint = location.tint;
		torch = location.torch;
		
		isShadowedFloor = location.isShadowed;
		
		if (location.tileType == TileType.FRONT_FACE) {
			if (tileName.contains("FrontMiddle")) {
				isStraighFrontWall = true;
			}
		}
		
		if (location.hasIsland() && location.tileName == null) {
			drawEnlarged = true;
		}
		
	}
	
	public String getShadowName() {
		return location.shadowName;
	}
	
	public String toString() {
		return "TORCH: "+torch;
	}
}
