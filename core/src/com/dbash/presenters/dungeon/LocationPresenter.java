package com.dbash.presenters.dungeon;

import java.util.Vector;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.LocationInfo;
import com.dbash.models.PresenterDepend;
import com.dbash.models.ShadowMap;
import com.dbash.platform.AnimationView;
import com.dbash.platform.ImageView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.AnimOp;
import com.dbash.util.Rect;

// a LocationPresenter is the partner of a Location in the model.  it handles the presentation of the corresponding Location, duh.
// it gets updates of presentation-related data in the form of LocationInfo when the visual representation of the Location changes.
public class LocationPresenter {
	
	public LocationInfo locationInfo;
	private ImageView tile;
	public CreaturePresenter creaturePresenter;
	
	private Rect area;
	private UIDepend gui;
	private PresenterDepend model;
	private MapPresenter dungeonPresenter;
	private Vector<ImageView> items;
	private AnimationView eyeAnimation = null;
	private boolean drawEye = false;
	private ImageView shadow;
	
	public LocationPresenter(UIDepend gui, PresenterDepend model, Rect area, MapPresenter dungeonPresenter) {
		this.area = new Rect(area);
		this.gui = gui;
		this.model = model;
		this.dungeonPresenter = dungeonPresenter;
		this.items = new Vector<ImageView>();
		model.presenterTurnState.setAlpha(BLACKNESS_ALPHA);
		model.presenterTurnState.setRange(BLACKNESS_SIZE);
	}
	
	// draw a tile according to its visibility in the passed in shadowmap and alpha
	public boolean drawTile(SpriteBatch spriteBatch, ShadowMap shadowMap, float alpha) {
		boolean drawCreature = false;
		
		if (locationInfo.shadowMaps.contains(shadowMap)) {
			tile.draw(spriteBatch, alpha);
			for (ImageView image : items) {
				image.draw(spriteBatch);
			}
			
			// does this tile need to darw a creature or eye overlay?.
			if (creaturePresenter != null || drawEye) {
				drawCreature = true;
			}
		} else if (locationInfo.isDiscovered) {
			tile.drawTinted(spriteBatch, alpha);
		} 
		else {
			if (model.presenterTurnState.getUseBlack()) {
				drawCreature = true;
			}
		}
		
		return drawCreature;
	}
	
	private /*final*/ float BLACKNESS_SIZE = 3.6f;
	private /*final*/ float BLACKNESS_ALPHA = 1.0f;
	
	public void drawOverlayOnTile(SpriteBatch spriteBatch, ShadowMap shadowMap, float alpha) {
		if (model.presenterTurnState.getRange() != BLACKNESS_ALPHA) {
			BLACKNESS_ALPHA = model.presenterTurnState.getRange();
			shadow.setArea(new Rect(area, BLACKNESS_ALPHA));
		}
		if (locationInfo.shadowMaps.contains(shadowMap) == false && locationInfo.isDiscovered == false) {
			BLACKNESS_ALPHA = model.presenterTurnState.getAlpha();
			if (model.presenterTurnState.getUseBlack()) {
				shadow.draw(spriteBatch,BLACKNESS_ALPHA);
			}
		} else {
			if (creaturePresenter != null) {
				creaturePresenter.draw(spriteBatch, alpha);
			}

			if (drawEye) {
				eyeAnimation.draw(spriteBatch);
			}	
		}
	}
	
	public void setLocationInfo(LocationInfo locationInfo) {
		this.locationInfo = locationInfo;
		
		if (tile == null) {
			String tileName = "sw_";
			tileName = tileName.concat(locationInfo.tileName);
			this.tile = new ImageView(gui, tileName, area); 
			BLACKNESS_SIZE = model.presenterTurnState.getRange();
			shadow = new ImageView(gui, "blacktile", new Rect(area, BLACKNESS_SIZE)); 
		}
		
		// set a creature presenter if there is a creature at this location.
		if (locationInfo.creature != null) {
			// When a creature makes it onto any Location for the first time, we can create a presenter for it and attach
			// it to that creature for future reference.
			creaturePresenter = locationInfo.creature.getCreaturePresenter();
			if (creaturePresenter == null) {
				creaturePresenter = new CreaturePresenter(gui, model, locationInfo.creature, dungeonPresenter);
				locationInfo.creature.setCreaturePresenter(creaturePresenter);
			}
		} else {
			creaturePresenter = null;
		}
		
		setItemImages();
	}
	
	protected void setItemImages() {
		final float x[] = {.38f, .25f, .50f, .30f, .45f, .38f, .00f, .70f, .15f, .63f, .38f};
		final float y[] = {.00f, .00f, .00f, .10f, .10f, .10f, .00f, .00f, .01f, .10f, .25f};

		Rect itemArea = new Rect(area);
		itemArea.width = area.width/3;
		itemArea.height = area.width/3;
		items.clear();
		for (int i=0; i<locationInfo.itemList.size(); i++) {
			int posIndex = i%(x.length);
			itemArea.x = area.x + area.width*x[posIndex];  
			itemArea.y = area.y + area.height*y[posIndex];  
			items.add(new ImageView(gui, locationInfo.itemList.get(i).getAbilityTypeImageName(), itemArea));
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
