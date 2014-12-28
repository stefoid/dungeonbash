package com.dbash.models;

import com.dbash.util.L;

public class Light {
	public static final boolean LOG = false && L.DEBUG;
	
	public static float CHAR_LIGHT_STRENGTH = 2.5f;
	public static float WALL_TORCH_STRENGTH = 1.75f;
	public static int CHAR_LIGHT_RANGE = Map.RANGE;
	
	public DungeonPosition position;
	public int range;
	protected float fStrength;
	protected ShadowMap shadowMap;
	protected Map map;
	public boolean permanent;
	public float alpha;
	
	public Light (DungeonPosition position, int range, float strength, boolean permanent) {
		this.position = new DungeonPosition(position);
		this.range = range;
		this.fStrength = strength;
		this.shadowMap = new ShadowMap();
		this.permanent = permanent;
		this.alpha = 1f;
	}
	
	public Light(Light parent) {
		this.position = parent.position;
		this.range = parent.range;
		this.fStrength = parent.fStrength;
		this.shadowMap = new ShadowMap(parent.shadowMap);
		this.permanent = parent.permanent;
		this.map = parent.map;
		this.alpha = parent.alpha;
	}
	
	// Work out a shadowmap for this light, and then illuminate the Locations in it according to their distance
	// from it.  Each loaction in the shadowMap allready knows how far it is from the centre, so its 
	// pretty easy.
	public void setMap(Map map) {
		this.map = map;
		shadowMap.setMap(map, position, range);
		map.lightingChanged();
	}
	
	public void setPosition(DungeonPosition position) {
		this.position = new DungeonPosition(position);
		shadowMap.setMap(map, position, (int)range);
		map.lightingChanged();
	}
	
	// Shine its light on the Locations it can see, according to their distance.
	public void applyLight() {
		applyLight(alpha);
	}
	
	
	
	private void applyLight(float alpha) {
		if (LOG) L.log("alpha: %s", alpha);
		for (Location location : shadowMap.locations) {
			float div = location.position.getTrueDistance(position);
			float newTint = fStrength/div;
			if (permanent) {
				location.setPermTint(newTint);
			} else {
				// alpha only applies to temp lighting
				float currentTint = location.getTint();
				float dif = newTint - currentTint;
				if (dif > 0) {
					location.setTint(currentTint + alpha*dif);
					if (LOG) L.log("currentTint: %s, newTint: %s, dif: %s, alpha: %s, result: %s", currentTint, newTint, dif, alpha, location.getTint());
				}
			}
		}
	}
	
	public void setPositionOnly(DungeonPosition position) {
		this.position = position;
	}
	
	public void setAlpha(float alpha) {
		this.alpha = alpha;
		map.lightingChanged();
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
