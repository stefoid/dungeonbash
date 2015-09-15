package com.dbash.presenters.dungeon;

import java.util.Vector;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.AbilityInfo;
import com.dbash.models.Location;
import com.dbash.models.LocationInfo;
import com.dbash.models.PresenterDepend;
import com.dbash.models.ShadowMap;
import com.dbash.platform.AnimationView;
import com.dbash.platform.ImageView;
import com.dbash.platform.TextBoxView;
import com.dbash.platform.TextImageView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.AnimOp;
import com.dbash.util.L;
import com.dbash.util.Rect;
import com.dbash.util.Rect.HAlignment;

// a LocationPresenter is the partner of a Location in the model.  it handles the presentation of the corresponding Location, duh.
// it gets updates of presentation-related data in the form of LocationInfo when the visual representation of the Location changes.
public class LocationPresenter {
	
	public static boolean LOG = true && L.DEBUG;
	
	public LocationInfo locationInfo;
	private ImageView tile;
	private ImageView floorImage;
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
	private TextImageView[] tileInfo = null;
	
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
	
	public boolean isVisibile(ShadowMap shadowMap) {
		if (shadowMap != null) {
			return shadowMap.locationIsVisible(locationInfo.location);
		}
		return false;
	}
	
	// draw a tile according to its visibility in the passed in shadowmap and alpha
	public void drawTile(SpriteBatch spriteBatch, float alpha, boolean isVisibile) {
		
		float tint = locationInfo.tint;
		
		if (!L.useLights) {
			tint = 100f;
		}
		
		if (isVisibile) {
			drawTile(spriteBatch, tint, alpha);
		} else if (locationInfo.isDiscovered) {
			drawTile(spriteBatch, Location.minNotVisibleTint, alpha);
		} 
	}
	
	private void drawTile(SpriteBatch spriteBatch, float tint, float alpha) {

		if (tile != null) {
			tile.drawTinted(spriteBatch, tint, alpha);;
		}
		
		if (shadow != null && L.floorShadows) {
			shadow.drawTinted(spriteBatch, tint, alpha);
		}
		
		if (roughTerrain != null) {
			roughTerrain.drawTinted(spriteBatch, tint, alpha);
		}
		
		for (ImageView item : items) {
			item.drawTinted(spriteBatch, tint, alpha);
		}
		
//		if (tileInfo != null) {
//			tileInfo.draw(spriteBatch, 0, 0);
//		}
	}
	
	public void drawFloor(SpriteBatch spriteBatch, float alpha, boolean isVisibile) {

		float tint = locationInfo.tint;
		
		if (!L.useLights) {
			tint = 100f;
		}
		
		if (floorImage != null) {
			if (isVisibile) {
				floorImage.drawTinted(spriteBatch, tint, alpha);
			} else if (locationInfo.isDiscovered) {
				floorImage.drawTinted(spriteBatch, Location.minNotVisibleTint, alpha);
			} 
		}
	}
	
//	public void drawTile(SpriteBatch spriteBatch, float alpha, boolean isVisibile) {
//
//		float tint = locationInfo.tint;
//		
//		if (!L.useLights) {
//			tint = 100f;
//		}
//		
//		if (isVisibile) {
//			tile.drawTinted(spriteBatch, tint, alpha);
//		} else if (locationInfo.isDiscovered) {
//			tile.drawTinted(spriteBatch, Location.minNotVisibleTint, alpha);
//		}
//	}
	
	public void drawCreature(SpriteBatch spriteBatch, float alpha, boolean isVisibile) {

		if (creaturePresenter != null) {
			if (isVisibile) {
				creaturePresenter.draw(spriteBatch, alpha);
			}
		}
	}
	
	public void drawTorches(SpriteBatch spriteBatch, float alpha, boolean isVisibile) {

		float tint = locationInfo.tint;
		
		if (!L.useLights) {
			tint = 100f;
		}
		
		if (torchAnimation != null) {
			if (isVisibile) {
				torchAnimation.draw(spriteBatch);
			} 
		}
	}
	
//	public void drawIsland(SpriteBatch spriteBatch, ShadowMap shadowMap, float alpha, boolean isVisibile) {
//
//		float tint = locationInfo.tint;
//		
//		if (!L.useLights) {
//			tint = 100f;
//		}
//		
//		if (locationInfo.isIsland) {
//			if (isVisibile) {
//				tile.drawTinted(spriteBatch, tint, alpha);
//			} else if (locationInfo.isDiscovered) {
//				tile.drawTinted(spriteBatch, Location.minNotVisibleTint, alpha);
//			} 
//		}
//	}
	
	public void drawOverlayOnTile(SpriteBatch spriteBatch) {
		if (drawEye) {
			eyeAnimation.draw(spriteBatch);
		}
	}
	
//	public void drawTorches(SpriteBatch spriteBatch, ShadowMap shadowMap, float alpha, boolean isVisibile) {
//		if (shadowMap != null && shadowMap.locationIsVisible(locationInfo.location)) {
//			if (torchAnimation != null) {
//				torchAnimation.draw(spriteBatch);
//			}
//		} 
//		
//		if (torchAnimation != null) {
//			if (isVisibile) {
//				torchAnimation.draw(spriteBatch);
//			} else if (locationInfo.isDiscovered) {
//				torchAnimation.draw(spriteBatch);
//			} 
//		}
//	}
	
	public void setLocationInfo(LocationInfo locationInfo) {
		this.locationInfo = locationInfo;
		
		// Done once at setup.
		if (tile == null) {

			String tileName;
			String prefix = "sw_";
			
			if (L.json.has("walls")) {
				prefix = L.json.getString("walls");
			}

			// set the floor tile to use, if required;
			if (locationInfo.requiresFloor()) {
				floorImage = new ImageView(gui, prefix.concat("CLEAR_FLOOR_IMAGE"), area);
			}
			
			// add correct prefix to tilename, if required.
			if (locationInfo.addPrefix) {
				tileName = prefix.concat(locationInfo.tileName);
			} else {
				tileName = locationInfo.tileName;
			}
			
			// create shadow image overlay
			if (locationInfo.isShadowedFloor) {
				shadow = new ImageView(gui, locationInfo.getShadowName(), area);
			} 
			
			// island image is bigger than normal.
			if (locationInfo.isIsland) {
				Rect islandArea = new Rect(area, 1.25f);
				islandArea.y = area.y;
				tileName = locationInfo.tileName;  // island tilenames dont require a prefix because they are the same for any theme
				this.tile = new ImageView(gui, tileName, islandArea);
			} else {
				// Dont draw the floor tile if its just clear floor and not an island or hardcoded, because it makes it fade funny and its
				// also redundant becuse the floor gets drawn anyway.
				if (locationInfo.requiresFloor()) {
					if (locationInfo.isHardcoded) {
						this.tile = new ImageView(gui, tileName, area);
					} else {
						this.tile = null;
					}
				} else {
					this.tile = new ImageView(gui, tileName, area);
				}
			}
			 
			if (L.DARK_PERCENTAGE != 100 && locationInfo.location.tileType != Location.TileType.CLEAR) {
//				String text = tileName + " " + locationInfo.location.tileType.toString();
//				String[] lines = locationInfo.tileName.split("(?=C)");
//				for (int i=0; i<lines.length; i++) {
//					
//				}
//				
//				tileInfo = new TextBoxView(gui, null, text, new Rect(area, .1f, .1f, .88f,.1f), HAlignment.LEFT, Color.PINK);
				
			} else {
				tileInfo = null;
			}
			
			Rect torchArea; 
			
			//if (LOG) L.log("tilename: %s, location info: %s, location * %s", tileName, locationInfo, locationInfo.location);
			
			switch (locationInfo.torch) {
				case FRONT:
					torchArea = new Rect(area, 0.8f);
					torchAnimation = new AnimationView(gui, "torch", torchArea, torchArea, 1f, 1f, 1f, AnimationView.LOOP_FOREVER, null);
					torchAnimation.startPlaying();
					break;
				case CENTRAL:
					torchArea = new Rect(area, 0.8f);
					torchArea.x -= area.width/4f;
					torchArea.y += area.height/9f;
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
