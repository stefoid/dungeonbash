package com.dbash.models;

import java.util.Set;

import com.dbash.models.Location.RoughTerrainType;

// LocationInfo is information created by a Location model object for Presenter consumption.
// It contains everything the Presenter needs to know to visually represent that location.
public class LocationInfo {

	public String tileName;
	public IPresenterCreature creature;
	public boolean isDiscovered;
	public Set<ShadowMap> shadowMaps;
	public ItemList itemList;
	public float tint; // how dark should this tile be?
	public Location.TorchType torch;
	public boolean isShadowedFloor;
	public RoughTerrainType roughTerrainType;
	
	public LocationInfo(Location location)
	{
		this.tileName = location.tileName;
		this.creature = location.creature;
		this.isDiscovered = location.isDiscovered;
		this.shadowMaps = location.shadowMaps.keySet();
		this.itemList = location.getItemList(true);
		this.tint = location.tint;
		this.torch = location.torch;
		this.roughTerrainType = location.getRoughTerrain();
		if (location.locationType == Location.LocationType.FLOOR) {
			if (location.tileName.equals("CLEAR_FLOOR_IMAGE") == false) {
				isShadowedFloor = true;
			} else {
				isShadowedFloor = false;
			}
		}
	}
}
