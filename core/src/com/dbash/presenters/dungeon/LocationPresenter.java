package com.dbash.presenters.dungeon;

import java.util.Vector;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.AbilityInfo;
import com.dbash.models.Location;
import com.dbash.models.LocationInfo;
import com.dbash.models.PresenterDepend;
import com.dbash.models.ShadowMap;
import com.dbash.platform.AnimationView;
import com.dbash.platform.ImageView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.AnimOp;
import com.dbash.util.L;
import com.dbash.util.Rect;

// a LocationPresenter is the partner of a Location in the model.  it handles the presentation of the corresponding Location, duh.
// it gets updates of presentation-related data in the form of LocationInfo when the visual representation of the Location changes.
public class LocationPresenter {
	
	public static boolean LOG = true && L.DEBUG;
	
	public LocationInfo locationInfo;
	private ImageView tile;
	private ImageView islandImage;
	public CreaturePresenter creaturePresenter;
	
	private Rect area;
	private UIDepend gui;
	private PresenterDepend model;
	private MapPresenter mapPresenter;
	private Vector<ImageView> items;
	private AnimationView eyeAnimation = null;
	private AnimationView torchAnimation = null;
	private boolean drawEye = false;
	private ImageView shadow = null;
	private ImageView roughTerrain = null;
	
	public LocationPresenter(UIDepend gui, PresenterDepend model, Rect area, MapPresenter mapPresenter) {
		this.area = new Rect(area);
		this.gui = gui;
		this.model = model;
		this.mapPresenter = mapPresenter;
		this.items = new Vector<ImageView>();
	}
	
	public boolean isIsland() {
		return locationInfo.isIsland;
	}
	
	// draw a tile according to its visibility in the passed in shadowmap and alpha
	public boolean drawTile(SpriteBatch spriteBatch, ShadowMap shadowMap, float alpha) {
		boolean drawOverlay = false;
		float tint = locationInfo.tint;
		
		if (shadowMap != null && shadowMap.locationIsVisible(locationInfo.location)) {
			drawTile(spriteBatch, tint, alpha);
			
			if (shadow != null) {
				shadow.drawTinted(spriteBatch, tint, alpha);
			}
			
			if (torchAnimation != null) {
				torchAnimation.draw(spriteBatch);
			}
			
			for (ImageView image : items) {
				image.draw(spriteBatch);
			}
			
			// does this tile need to draw a creature or eye overlay?.
			if (creaturePresenter != null || drawEye) {
				drawOverlay = true;
			}
		} else if (locationInfo.isDiscovered) {
			drawTile(spriteBatch, Location.minNotVisibleTint, alpha);
		} 

		return drawOverlay;
	}
	
	
	private void drawTile(SpriteBatch spriteBatch, float tint, float alpha) {
		tile.drawTinted(spriteBatch, tint, alpha);
		if (roughTerrain != null) {
			roughTerrain.drawTinted(spriteBatch, tint, alpha);
		}
	}
	
	public void drawOverlayOnTile(SpriteBatch spriteBatch, ShadowMap shadowMap, float alpha) {
		
		if (creaturePresenter != null) {
			creaturePresenter.draw(spriteBatch, alpha);
		}
		if (drawEye) {
			eyeAnimation.draw(spriteBatch);
		}
	}
	
	public void drawIsland(SpriteBatch spriteBatch, ShadowMap shadowMap, float alpha) {
		if (islandImage != null) {
			if (shadowMap != null && shadowMap.locationIsVisible(locationInfo.location)) {
				islandImage.drawTinted(spriteBatch, locationInfo.tint, alpha);
			} else if (locationInfo.isDiscovered) {
				islandImage.drawTinted(spriteBatch, Location.minNotVisibleTint, alpha);
			} 
		}
	}
	
	public void setLocationInfo(LocationInfo locationInfo) {
		this.locationInfo = locationInfo;
		
		// Done once at setup.
		if (tile == null) {
			String tileName = "sw_";

			if (locationInfo.isShadowedFloor) {
				tileName = tileName.concat("CLEAR_FLOOR_IMAGE");
				shadow = new ImageView(gui, locationInfo.tileName, area); 
			} else if (locationInfo.isIsland) {
				tileName = tileName.concat("CLEAR_FLOOR_IMAGE");
				Rect islandArea = new Rect(area, 1.25f);
				islandArea.y = area.y;
				islandImage = new ImageView(gui, locationInfo.tileName, islandArea); 
			} else {
				tileName = tileName.concat(locationInfo.tileName);
			}

			this.tile = new ImageView(gui, tileName, area); 
			
			Rect torchArea; 
			
			if (LOG) L.log("location info: %s, location * %s", locationInfo, locationInfo.location);
			
			switch (locationInfo.torch) {
				case FRONT:
					torchArea = new Rect(area, 0.8f);
					torchAnimation = new AnimationView(gui, "torch", torchArea, torchArea, 1f, 1f, 1f, AnimationView.LOOP_FOREVER, null);
					torchAnimation.startPlaying();
					break;
				case WEST:
					torchArea = new Rect(area, .2f, 0f, 0.0f, .3f);
					torchAnimation = new AnimationView(gui, "torche", torchArea, torchArea, 1f, 1f, 1f, AnimationView.LOOP_FOREVER, null);
					torchAnimation.startPlaying();
					break;
				case EAST:
					torchArea = new Rect(area, 0f, 0.2f, .0f, .3f);
					torchAnimation = new AnimationView(gui, "torchw", torchArea, torchArea, 1f, 1f, 1f, AnimationView.LOOP_FOREVER, null);
					torchAnimation.startPlaying();
					break;
				default:
					break;
			}
		}
		
		if (locationInfo.roughTerrainType != null) {
			this.roughTerrain = new ImageView(gui, locationInfo.roughTerrainType.getValue(), area);
		}
		
		// set a creature presenter if there is a creature at this location.
		if (locationInfo.creature != null) {
			creaturePresenter = locationInfo.creature.getCreaturePresenter(gui, model, mapPresenter);
		} else {
			creaturePresenter = null;
		}
		
		setItemImages();
	}
	
	protected void setItemImages() {
		final float x[] = {.38f, .25f, .50f, .30f, .45f, .38f, .00f, .70f, .15f, .63f, .38f};
		final float y[] = {.00f, .00f, .00f, .10f, .10f, .10f, .00f, .00f, .01f, .10f, .25f};

		Rect itemArea = null;
		if (locationInfo.itemList.size() > 0) {
			itemArea = new Rect(area);
			itemArea.width = area.width/3;
			itemArea.height = area.width/3;
		}
		
		items.clear();
		int i = 0;
		for (AbilityInfo abilityInfo : locationInfo.itemList) {
			if (abilityInfo.isRoughTerrain == false) {
				int posIndex = i%(x.length);
				itemArea.x = area.x + area.width*x[posIndex];  
				itemArea.y = area.y + area.height*y[posIndex];  
				items.add(new ImageView(gui, abilityInfo.getAbilityTypeImageName(), itemArea));
				i++;
			}
		}
	}
	
	// might as well set the width while we are at it.
	public Rect getScreenCenterPoint() {
		float x = area.x+area.width/2;
		float y = area.y+area.height/2;

		Rect cp = new Rect (x,y,area.width,area.height);
		return cp;
	}
	
	public Rect getScreenArea() {
		return area;
	}
	
	// the eye animation is  never ending, but only gets drawn when this tile is drawn.
	public void showEyeAnimation(boolean showEyeAnim){
		drawEye = showEyeAnim;
		if (showEyeAnim) {
			if (eyeAnimation == null) {
				Rect fromRect = getScreenArea();
				Rect toRect = new Rect(fromRect, 1.5f);
				eyeAnimation = new AnimationView(gui, "missed", fromRect, toRect, 0.6f, 0f, 1f, -1, null);
				eyeAnimation.animType = AnimOp.AnimType.TARGETED;
				eyeAnimation.startPlaying();
			}
		}
	}
}
