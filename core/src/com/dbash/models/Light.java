package com.dbash.models;

public class Light {
	
	public static float CHAR_LIGHT_STRENGTH = 2.5f;
	public static float WALL_TORCH_STRENGTH = 1.75f;
	
	public DungeonPosition position;
	public int range;
	protected float fStrength;
	protected ShadowMap shadowMap;
	protected Map map;
	public boolean permanent;
	
	public Light (DungeonPosition position, int range, float strength, boolean permanent) {
		this.position = new DungeonPosition(position);
		this.range = range;
		this.fStrength = strength;
		this.shadowMap = new ShadowMap();
		this.permanent = permanent;
	}
	
	// Work out a shadowmap for this light, and then illuminate the Locations in it according to their distance
	// from it.  Each loaction in the shadowMap allready knows how far it is from the centre, so its 
	// pretty easy.
	public void setMap(Map map) {
		this.map = map;
		shadowMap.setMap(map, position, range);
	}
	
	public void setPosition(DungeonPosition position) {
		this.position = new DungeonPosition(position);
		shadowMap.setMap(map, position, (int)range);
	}
	
	// Shine its light on the Locations it can see, according to their distance.
	public void applyLight() {
		for (Location location : shadowMap.locations) {
			float div = location.getPosition().getTrueDistance(position);
			if (permanent) {
				location.setPermTint(fStrength/div);
			} else {
				location.setTint(fStrength/div);
			}
		}
	}
	
	protected float getLightDivisor(float distance) {
//		if (distance > 1f) {
//			return distance*.8f; 
//		}
//		else 
			return distance;
	}
	
	public void clearLight() {
		for (Location location : shadowMap.locations) {
			location.clearTint();  // will reset this location to the minimum light level.
		}
	}
}
