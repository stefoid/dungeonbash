package com.dbash.presenters.dungeon;

import java.util.Vector;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.Character;
import com.dbash.models.DungeonPosition;
import com.dbash.models.IAnimListener;
import com.dbash.models.IMapPresentationEventListener;
import com.dbash.models.Light;
import com.dbash.models.Location;
import com.dbash.models.LocationInfo;
import com.dbash.models.Map;
import com.dbash.models.PresenterDepend;
import com.dbash.models.ShadowMap;
import com.dbash.models.TouchEventProvider;
import com.dbash.models.UIInfoListener;
import com.dbash.models.UIInfoListenerBag;
import com.dbash.models.UILocationInfoListener;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.AnimOp;
import com.dbash.util.L;
import com.dbash.util.Rect;
import com.dbash.util.Tween;

@SuppressWarnings("unused")
// This class draws the dungeon map and smoothly scrolls it when required.
// it talks to the dungeon simulation through the presenter dungeon interface.
// it defers drawing Locations to individual LocationPresenters, and creatures to creaturePresenters.
// but it knows about layout and dimensions and so on.
//
// mappresenter is an AnimOp so it can add itself to the anim queue so that things can schedule themselves wrt. scrolling.
// 
public class MapPresenter implements IMapPresentationEventListener{
	public static final boolean LOG = false && L.DEBUG;
	
	private Map map;
	protected LocationPresenter[][] locationPresenters;
	protected DungeonPosition focusPosition;
	protected Rect viewPos;  // the dungeon viewPort will be centered around this point
	protected int minTileX;  // which tiles need to be drawn - work then out when viewport moves rather than at draw time;
	protected int minTileY;
	protected int maxTileX;
	protected int maxTileY;
	protected UIDepend gui;
	protected PresenterDepend model;
	protected Rect area;
	protected float tileSize;
	protected ShadowMap currentShadowMap;
	protected ShadowMap previousShadowMap;
	LocationPresenter[] creatures;
	float[] creatureAlpha;
	protected int range = 5;
	protected Tween currentShadowMapTween;
	
	public MapPresenter(UIDepend gui, PresenterDepend model, TouchEventProvider touchEventProvider, Rect area) {
		super();  // AnimOp allows to place itself on the Anim queue when scrolling.
		this.gui = gui;
		this.locationPresenters = null;
		this.focusPosition = null;
		this.model = model;
		this.area = area;
		this.viewPos = new Rect (area.width/2, area.height/2, 0, 0);
		tileSize = area.width / (2*range+1);
		currentShadowMapTween = new Tween();
		creatures = new LocationPresenter[200];
		creatureAlpha = new float[200];
		model.presenterDungeon.onMapEvent(this);
	}
	
	
	// viewPos is where the dungeon camera is at any given time, so we have to draw enough tiles around that point to show the map
	// LocationPresenters are just squares of tilesize, starting from 0,0 in the bottom left corner, its no probs.
	public void draw(SpriteBatch spriteBatch) {
		if (map == null) {
			return;
		}
		
		int creatureCount = 0;
		float curAlpha = 1.0f;
		float fadeOutAlpha = 1 - curAlpha;
		
		if (previousShadowMap != currentShadowMap && previousShadowMap != null) {
			curAlpha = currentShadowMapTween.getValue();
		}
		
		// draw the tiles that could be visible (pre-calculated when moveView is called)
		for (int x=minTileX; x<=maxTileX; x++) {
			for (int y=minTileY; y<=maxTileY;y++) {
				// draw the current shadowmap details
				LocationPresenter loc = locationPresenter(x,y);
				boolean drawPrevCreature = loc.drawTile(spriteBatch, previousShadowMap, 1f);
				boolean drawOnTopOfTile = loc.drawTile(spriteBatch, currentShadowMap, curAlpha);
				creatures[creatureCount] = loc;
				
				// determine if we need to draw a creature and what alpha to draw it
				if (drawPrevCreature && drawOnTopOfTile) {
					creatureAlpha[creatureCount] = 1f;
					creatureCount++;
				} else if (!drawPrevCreature && drawOnTopOfTile) {
					creatureAlpha[creatureCount] = curAlpha;
					creatureCount++;
				} else if (drawPrevCreature && !drawOnTopOfTile) {
					creatureAlpha[creatureCount] = fadeOutAlpha;
					creatureCount++;
				}
			}
		}
		
		// draw the creatures that we have recorded need to be drawn, at the appropriate alphas
		// draw in reverse order s the bottom creatures appear on top
		for (int i=creatureCount-1; i>=0; i--) {
			creatures[i].drawOverlayOnTile(spriteBatch, currentShadowMap, creatureAlpha[i]);
		}
	}
	
	public void moveView(float x, float y) {
		// work out top left legal dungeon position and bottom right.  Since we could be scrolling, its just safest to draw an 
		// extra ring of tiles around the center point to show partial tiles in any direction without thinking about it too much.
		viewPos.x = x;
		viewPos.y = y;
		int centerTileX = (int) (x / tileSize);
		int centerTileY = (int) (y / tileSize);
		
		minTileX = centerTileX - (range+1);
		if (minTileX < 0) {
			minTileX = 0;
		}
		maxTileX = centerTileX + (range+1);
		if (maxTileX >= map.width) {
			maxTileX = map.width - 1;
		}
		minTileY = centerTileY - (range+1);
		if (minTileY < 0) {
			minTileY = 0;
		}
		maxTileY = centerTileY + (range+1);	
		if (maxTileY >= map.height) {
			maxTileY = map.height - 1;
		}
		
		// move the dungeon camera
		gui.cameraViewPort.moveCamera(viewPos.x, viewPos.y);		
	}
	
	// When a map is set, we subscribe to changes at its locations
	@Override
	public void setMap(Map map)
	{
		this.map = map;
		
		map.onChangeToLocationInfo(new UILocationInfoListener() {
			public void locationInfoChanged(Location location) {
				LocationInfo locationInfo = location.getLocationInfo();
				LocationPresenter locationPresenter = locationPresenter(location.getPosition());
				locationPresenter.setLocationInfo(locationInfo);
			}
		});
		
		Rect tileArea = new Rect(0,0,tileSize, tileSize);
		
		// create a new set of locations from the map
		locationPresenters = new LocationPresenter[map.width][map.width];
		for (int x=0; x<map.width; x++) {
			tileArea.y = 0;
			for (int y=0; y<map.height; y++) {
				locationPresenters[x][y] = new LocationPresenter(gui, model, tileArea, this);
				locationPresenters[x][y].setLocationInfo(map.location(x, y).getLocationInfo()); // set initial info
				tileArea.y += tileSize;
			}
			tileArea.x += tileSize;
		}
	}
	
	
	public LocationPresenter locationPresenter(int x, int y) {
		return locationPresenters[x][y];
	}
	
	public LocationPresenter locationPresenter(DungeonPosition dungeonPosition) {
		return locationPresenter(dungeonPosition.x, dungeonPosition.y);
	}
	
	public DungeonPosition convertXYToDungeonPosition(float x, float y) {
		DungeonPosition dp = new DungeonPosition((int)(x/tileSize), (int)(y/tileSize));
		dp.x += focusPosition.x - range ; // TODO range;
		dp.y += focusPosition.y - range ;  // TODO range
		return dp;
	}

	@Override
	// animated scroll to the new focus position.
	// if we are already scrolling, chain the next scroll to the when that is completed.
	public void changeCurrentCharacterFocus(int sequenceNumber, Character newFocusCharacter) {
		if (LOG) L.log("sequenceNumber: %s, newFocusCharacter: %s", sequenceNumber, newFocusCharacter);
		animatedFocusChange(sequenceNumber, newFocusCharacter, DungeonAreaPresenter.scrollPeriod, false, null);
	}

	
	// animated scroll to the new focus position.  It is not that characters turn, its just scrolling to that character
	// so they can see something interesting.
	public void animatedFocusChange(int sequenceNumber, final Character newFocusCharacter, final float period, boolean characterMoving, IAnimListener animCompleteListener) {
		if (LOG) L.log("sn: %s, newFocusCharacter: %s, characterMoving: %s", sequenceNumber,newFocusCharacter, characterMoving);

		final ShadowMap newShadowMap = new ShadowMap(newFocusCharacter.shadowMap);
		final DungeonPosition targetDungeonPosition = newFocusCharacter.getPosition();
		LocationPresenter centerLocation = locationPresenter(targetDungeonPosition);
		Rect targetPoint = centerLocation.getScreenCenterPoint();
		final IAnimListener animCallback = animCompleteListener;
		
		MapAnim mapAnim = new MapAnim(currentShadowMapTween, viewPos, targetPoint, period, this);
		mapAnim.sequenceNumber = sequenceNumber;
		
		mapAnim.onStart(new IAnimListener() {
		    public void animEvent() {
		        previousShadowMap = currentShadowMap;
		        currentShadowMap = newShadowMap;
		        if (previousShadowMap != currentShadowMap && previousShadowMap != null) {
		            currentShadowMapTween.init(0f, 1.0f, period, null);
		        }
		    }
		});

		mapAnim.onComplete(new IAnimListener() {
			public void animEvent() {
				focusPosition = targetDungeonPosition;
				previousShadowMap = null;
				// if the turn processor wants to know about this, tell it.
				if (animCallback != null) {
					animCallback.animEvent();
				}
			}
		});
		
		// startPlaying will be called by the animQueue when the last anim on the queue has started.
		if (characterMoving) {
			model.animQueue.chainConcurrentWithLast(mapAnim, 0f, false);
		} else {
			model.animQueue.chainSequential(mapAnim, false);
		}
	}

	
	@Override
	// This sets the focus position on the map in one step (no anim scroll).
	public void instantFocusChange(DungeonPosition focusPosition, ShadowMap shadowMap) {
		if (LOG) L.log("focusPosition: %s, shadowMap: %s", focusPosition, shadowMap);
		
		this.focusPosition = new DungeonPosition(focusPosition);
		if (shadowMap != null) {
			previousShadowMap = currentShadowMap;
			currentShadowMap = new ShadowMap(shadowMap);
		} 
		
		LocationPresenter centerLocation = locationPresenter(focusPosition);
		moveView(centerLocation.getScreenCenterPoint());
	}

	// moves the view of the map and works out which tiles will need to be drawn at drawtime.
	public void moveView(Rect newViewPos) {
		moveView(newViewPos.x, newViewPos.y);
	}

	public void addLight(Light light) {
		map.addLight(light);
	}
	
	public void moveLight(Light light, DungeonPosition newPosition) {
		map.moveLight(light, newPosition);
	}
	
	public void removeLight(Light light) {
		map.removeLight(light);
	}
}
