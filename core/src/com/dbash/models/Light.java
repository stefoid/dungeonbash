package com.dbash.models;

public class Light {
	public DungeonPosition position;
	public int strength;
	protected float fStrength;
	protected ShadowMap shadowMap;
	protected Map map;
	public boolean permanent;
	
	public Light (DungeonPosition position, int strength, boolean permanent) {
		this.position = new DungeonPosition(position);
		this.strength = strength;
		this.fStrength = (float) strength / 2.6f;
		this.shadowMap = new ShadowMap();
		this.permanent = permanent;
	}
	
	// Work out a shadowmap for this light, and then illuminate the Locations in it according to their distance
	// from it.  Each loaction in the shadowMap allready knows how far it is from the centre, so its 
	// pretty easy.
	public void setMap(Map map) {
		this.map = map;
		shadowMap.setMap(map, position, strength);
	}
	
	public void setPosition(DungeonPosition position) {
		this.position = new DungeonPosition(position);
		shadowMap.setMap(map, position, (int)strength);
	}
	
	// Shine its light on the Locations it can see, according to their distance.
	public void applyLight() {
		for (Location location : shadowMap.locations) {
			if (permanent) {
				location.setPermTint(fStrength/(location.getPosition().getTrueDistance(position)));
			} else {
				location.setTint(fStrength/(location.getPosition().getTrueDistance(position)));
			}
		}
	}
	
	public void clearLight() {
		for (Location location : shadowMap.locations) {
			location.clearTint();  // will reset this location to the minimum light level.
		}
	}
}
