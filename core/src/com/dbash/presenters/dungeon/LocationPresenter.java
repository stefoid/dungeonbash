package com.dbash.presenters.dungeon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.AbilityInfo;
import com.dbash.models.Light;
import com.dbash.models.Location;
import com.dbash.models.Location.TorchType;
import com.dbash.models.LocationInfo;
import com.dbash.models.PresenterDepend;
import com.dbash.models.ShadowMap;
import com.dbash.platform.AnimationView;
import com.dbash.platform.GreyShader;
import com.dbash.platform.ImageView;
import com.dbash.platform.TextImageView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.AnimOp;
import com.dbash.util.L;
import com.dbash.util.Rect;

// a LocationPresenter is the partner of a Location in the model.  it handles the presentation of the corresponding Location, duh.
// it gets updates of presentation-related data in the form of LocationInfo when the visual representation of the Location changes.
public class LocationPresenter {
	
	public static boolean LOG = true && L.DEBUG;
	
	public static float LIGHT_TINT = 1f;
	public static float TINT = .7f;
	
	public LocationInfo locationInfo;
	private ImageView tile;
	private ImageView overlay;
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
	private ImageView fog = null;
	private ArrayList<AnimationView> moveAnims;
	
	public LocationPresenter(UIDepend gui, PresenterDepend model, Rect area, MapPresenter mapPresenter) {
		this.area = new Rect(area);
		this.gui = gui;
		this.model = model;
		this.mapPresenter = mapPresenter;
		this.items = new Vector<ImageView>();
		moveAnims = new ArrayList<AnimationView>();
	}
	
//	public boolean isIsland() {
//		return locationInfo.isIsland;
//	}
	
	public boolean isVisibile(ShadowMap shadowMap) {
		if (shadowMap != null) {
			return shadowMap.locationIsVisible(locationInfo.location);
		}
		return false;
	}
	
	public void addCreatureAnim(AnimationView anim) {
		moveAnims.add(anim);
	}
	
	private float calcTint(float cur_tint, float alpha, boolean prev, boolean current, boolean isBelowCentre) {
		float tint;
		
//		if (isBelowCentre && locationInfo.location.tileType == TileType.FRONT_FACE) {
//			cur_tint = Location.minNotVisibleTint;
//		}
		
//		cur_tint = Location.maxVisibileTint;
//		
//		if (prev && !current) {       // fade out
//			tint = cur_tint + (Location.minNotVisibleTint - cur_tint) * alpha;
//		} else if (!prev && current) {// fade in;
//			tint = Location.minNotVisibleTint + (cur_tint - Location.minNotVisibleTint) * alpha;
//		} else if (prev && current) { // still visible
//			tint = cur_tint;
//		} else {                      //  still not visible
//			tint = Location.minNotVisibleTint;
//		}	
//		
//		return tint;
		
		return Location.maxVisibileTint;
	}
	
	private float calcAlpha(float theAlpha, boolean prev, boolean current) {
		float alpha;
		
		if (prev && !current) {       // fade out
			alpha = 1f - theAlpha;
		} else if (!prev && current) {// fade in;
			alpha = theAlpha;
		} else if (prev && current) { // still visible
			alpha = 1f;
		} else {                      //  still not visible
			alpha = 0f;
		}	
		
		return alpha;
	}
	
	// draw a tile according to its visibility in the passed in shadowmap and alpha
	public void drawTile(SpriteBatch spriteBatch, float alpha, boolean prevVisible, boolean curVisible, boolean isBelowCentre) {
		
		if (locationInfo.isDiscovered) {
			if (!curVisible) {
				spriteBatch.setShader(GreyShader.grayscaleShader);
			}
			
			float tint = calcTint(locationInfo.tint, alpha, prevVisible, curVisible, isBelowCentre);
//			float tint = 1f;
			float lightTint = tint;
			
			float tileTint = locationInfo.tint;
			if (curVisible || prevVisible) {
				tileTint = tileTint + L.DARK_FACTOR;
				if (tileTint > 1f) {
					tileTint = 1f;
				}
				
				lightTint = calcTint(tileTint , alpha, prevVisible, curVisible, isBelowCentre);
			}
			
//			float tint = TINT;
//			float lightTint = LIGHT_TINT;
			
			if (!L.useLights) {
				tint = 1f;
			} 
		
			if (floorImage != null) {
				floorImage.drawTinted(spriteBatch, tint, 1f);
			}
			
			if (tile != null) {
				tile.drawTinted(spriteBatch, lightTint, 1f);
			} 
			
			if (L.floorShadows && shadow != null) {
				shadow.drawTinted(spriteBatch, tint, 1f);
			}
			
			if (roughTerrain != null) {
				roughTerrain.drawTinted(spriteBatch, lightTint, 1f);
			}
			
			for (ImageView item : items) {
				item.drawTinted(spriteBatch, lightTint, 1f);
			}
			
			fog = null;
		} else {
			//drawFog(spriteBatch);
			drawCreature(spriteBatch, alpha, prevVisible, curVisible, isBelowCentre);
		}
		
		spriteBatch.setShader(null);
	}
	
	public void drawOutOfLOSTile(SpriteBatch spriteBatch, float alpha, boolean prevVisible, boolean curVisible, boolean isBelowCentre) {
		
		float r = .4f;
		float g = .4f;
		float b = .4f;
		float a = 1f;
		
		if (curVisible == false) {
			
			spriteBatch.setShader(GreyShader.grayscaleShader);

			if (floorImage != null) {
				floorImage.drawTinted(spriteBatch, r, g, b, a);
			}
			
			if (tile != null) {
				tile.drawTinted(spriteBatch, r, g, b, a);
			} 
			
			if (L.floorShadows && shadow != null) {
				shadow.drawTinted(spriteBatch, r, g, b, a);
			}
			
			if (roughTerrain != null) {
				roughTerrain.drawTinted(spriteBatch, r, g, b, a);
			}
			
			if (overlay != null) {
				overlay.drawTinted(spriteBatch, r, g, b, a);
			}
			
			spriteBatch.setShader(null);
		}
	}

	public void drawFog(SpriteBatch spriteBatch) {
		fog.draw(spriteBatch, 1f);
	}
	
	public void drawCreature(SpriteBatch spriteBatch, float alpha, boolean prevVisible, boolean curVisible, boolean isBelowCentre) {
		
		if (locationInfo.isDiscovered) {
			if (creaturePresenter != null) {
				if (prevVisible || curVisible) {
					creaturePresenter.draw(spriteBatch, calcAlpha(alpha, prevVisible, curVisible));
				}
			}
		}
		
		for (Iterator<AnimationView> it = moveAnims.iterator(); it.hasNext(); ) {  
		    AnimOp animOp = it.next(); 
		    
		    if (animOp.hasCompleted) {
		    	it.remove();  // safely ask the iterator to remove this because its done.
		    } else {  // only call draw on unowned animOps.
		    	animOp.draw(spriteBatch);
		    }
		} 
	}
	
	// Draw the creature either statically, or animated, and the island.  In the appropriate order.
	public void drawOverlays(SpriteBatch spriteBatch, float alpha, boolean prevVisible, boolean curVisible, boolean isBelowCentre) {
		
		if (locationInfo.isDiscovered) {
			if (!curVisible) {
				spriteBatch.setShader(GreyShader.grayscaleShader);
			}
			
			float tint = calcTint(locationInfo.tint, alpha, prevVisible, curVisible, isBelowCentre);
			//float tint = 1f;
			float lightTint = tint;
			
			float tileTint = locationInfo.tint;
			if (curVisible || prevVisible) {
				tileTint = tileTint + L.DARK_FACTOR;
				if (tileTint > 1f) {
					tileTint = 1f;
				}
				
				lightTint = calcTint(tileTint , alpha, prevVisible, curVisible, isBelowCentre);
			} 
			
//			float tint = TINT;
//			float lightTint = LIGHT_TINT;
			
			if (overlay != null) {
				overlay.drawTinted(spriteBatch, lightTint, 1f);
			}
			
			if (curVisible && torchAnimation != null) {
				if (!(isBelowCentre && locationInfo.isStraighFrontWall)) {
					torchAnimation.draw(spriteBatch);
				}
			}
			
			if (drawEye) {
				eyeAnimation.draw(spriteBatch);
			}
			
			spriteBatch.setShader(null);
		} 
	}
	
	public void setLocationInfo(LocationInfo locationInfo) {
		this.locationInfo = locationInfo;
		
		// Done once at setup.
		if (tile == null) {
			// set the floor tile to use, if required;
			if (locationInfo.floorName!= null) {
				floorImage = new ImageView(gui, locationInfo.floorName, area);
			}
			
			// create shadow image overlay
			if (L.floorShadows && locationInfo.isShadowedFloor) {
				shadow = new ImageView(gui, locationInfo.getShadowName(), area);
			} 
			
			if (locationInfo.tileName != null) {
				tile = new ImageView(gui, locationInfo.tileName, area);
			}
			
			// island image is bigger than normal.
			Rect overlayArea = area;
			if (locationInfo.drawEnlarged) {
				overlayArea = new Rect(area, 1.25f);
				overlayArea.y = area.y;
			}
			
			if (locationInfo.overlayName != null) {
				overlay = new ImageView(gui, locationInfo.overlayName, overlayArea);
			}
			 
			if (locationInfo.isDiscovered == false) {
				fog = new ImageView(gui, "fogtile", area);
			}
			
			if (locationInfo.roughTerrainName != null) {
				this.roughTerrain = new ImageView(gui, locationInfo.roughTerrainName, area);
			}
			
			if (locationInfo.location.tileType != Location.TileType.CLEAR) {
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
			
			//if (LOG) L.log("tilename: %s, location info: %s, location * %s", tileName, locationInfo, locationInfo.location);
			
			if (locationInfo.torch != TorchType.NONE && locationInfo.torch != TorchType.INVISIBLE) {
				
				Rect torchArea = null; 
				
				switch (locationInfo.torch) {
				case FRONT:
					torchArea = new Rect(area, 0.8f);
                    torchAnimation = new AnimationView(gui, "torch", torchArea, torchArea, 1f, 1f, 1f, AnimationView.LOOP_FOREVER, null);
					break;
				case CENTRAL:
					torchArea = new Rect(area, 0.8f);
					torchArea.x -= area.width/4f;
					torchArea.y += area.height/9f;
					torchAnimation = new AnimationView(gui, "torch", torchArea, torchArea, 1f, 1f, 1f, AnimationView.LOOP_FOREVER, null);
					break;
				case WEST:
					torchArea = new Rect(area, .2f, 0f, 0.0f, .3f);
					if (L.NEW_TILES) {
						torchArea.x -= area.width/3.3f;
					}
					torchAnimation = new AnimationView(gui, "torche", torchArea, torchArea, 1f, 1f, 1f, AnimationView.LOOP_FOREVER, null);
					break;
				case WEST_DOUBLE:
					torchArea = new Rect(area, .2f, 0f, 0.0f, .3f);
					if (L.NEW_TILES) {
						torchArea.x -= area.width/4f;
					}
					torchAnimation = new AnimationView(gui, "torche", torchArea, torchArea, 1f, 1f, 1f, AnimationView.LOOP_FOREVER, null);
					break;
				case EAST:
					torchArea = new Rect(area, 0f, 0.2f, .0f, .3f);
					if (L.NEW_TILES) {
						torchArea.x += area.width/3.3f;	
					}
					torchAnimation = new AnimationView(gui, "torchw", torchArea, torchArea, 1f, 1f, 1f, AnimationView.LOOP_FOREVER, null);
					break;
				case EAST_DOUBLE:
					torchArea = new Rect(area, 0f, 0.2f, .0f, .3f);
					if (L.NEW_TILES) {
						torchArea.x += area.width/4f;	
					}
					torchAnimation = new AnimationView(gui, "torchw", torchArea, torchArea, 1f, 1f, 1f, AnimationView.LOOP_FOREVER, null);
					break;
				default:
					break;
				}
				
				torchAnimation.startPlaying();
			}
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
