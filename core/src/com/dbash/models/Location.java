package com.dbash.models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Vector;

import com.dbash.models.IDungeonQuery.AtLocation;




// A Location embodies everything about a particular tile in the dungeon, including which characters can currently see it
// (Each character has a shadowMap that informs Locations that it can see them)
public class Location {

	public enum LocationType {
		WALL,
		FLOOR,
		EXIT};
		
	private enum TileType {
		CLEAR, 
		FRONT_FACE,
		REAR_FACE,
		NO_FACE
	};
		
	public static final float minTint = 0.3f;
	
	public Map map;
	public LocationType locationType;
	public TileType tileType;
	public Creature creature;
	public int	creatureFacingDir; // Dungeon.EAST for example
	public Vector<Ability> itemList;
	public String tileName;  // the type of tile which will be used to work out the Sprite to display it.
	public boolean isDiscovered;
	public DungeonPosition position;
	int x;
	int y;
	public float tint;
	
	// ShadowMaps in which this Location is visible, and the distance of this location form the center of that shadowmap
	HashMap<ShadowMap, Character>  shadowMaps;  
	// will create itself and add itself to the map.
	public Location(Map map, int x, int y)
	{
		clearTint();
		this.map = map;
		this.x = x;
		this.y = y;
		position = new DungeonPosition(x,y);
		locationType = LocationType.WALL;  // defaults to wall
		itemList = new Vector<Ability>();  // no items
		creature = null;   // no creature;
		tileType = null;  // cant be worked out yet
		isDiscovered = false;
		tileName = "CLEAR_FLOOR_IMAGE";
		shadowMaps = new HashMap<ShadowMap, Character>();  // the set of shadowmaps (and hence characters) this Location is visible to.
	}
	
	public Location() {
		locationType = LocationType.WALL;  // defaults to wall
		tileType = TileType.NO_FACE;  // default to solid (encased) rock
	}
	
	public Location(ObjectInputStream in, Map map, AllCreatures allCreatures, IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery) throws IOException, ClassNotFoundException {
		this.map = map;
		itemList = new Vector<Ability>();  // no items
		shadowMaps = new HashMap<ShadowMap, Character>();  // the set of shadowmaps (and hence characters) this Location is visible to.
		position = (DungeonPosition) in.readObject();
		this.x = position.x;
		this.y = position.y;
		locationType = (LocationType) in.readObject();
		tileType = (TileType) in.readObject();
		creatureFacingDir = in.readInt();
		tileName = (String) in.readObject();
		isDiscovered = (Boolean) in.readObject();
	}
	
	public void load(ObjectInputStream in, Map map, AllCreatures allCreatures, IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery) throws IOException, ClassNotFoundException {
		//	5) Each Location reads its Creature
		int cId = in.readInt();
		if (cId == TurnProcessor.NO_CURRENT_CREATURE) {
			creature = null;
		} else {
			setCreature(allCreatures.getCreatureByUniqueId(cId));
		}

		//	6) Each Location reads its list of Abilities (Items)
		int items = in.readInt();
		for (int i=0;i<items;i++) {
			itemList.add(new Ability(in, null, dungeonEvents, dungeonQuery));
		}
		
		map.alertToVisualChangeAtLocation(this);
	}
	
	public DungeonPosition getPosition() {
		return new DungeonPosition(x,y);
	}
	
	public void setCreature(Creature creature) {
		this.creature = creature;
		map.alertToVisualChangeAtLocation(this);   
	}
	
	// Onlt gets lighter due to, err lights.
	public void setTint(float newTint) {
		if (newTint > 1f) {
			newTint = 1f;
		}
		if (newTint > tint) {  // cant make it darker than it already is
			tint = newTint;
			map.alertToVisualChangeAtLocation(this);
		}
	}
	
	public void clearTint() {
		this.tint = minTint;  // darkest tile.  Lights will make it lighter.
	}
	
	public void shadowMapChange(ShadowMap shadowMap, Character character, boolean isVisibleWithin)
	{
		if (shadowMaps.containsKey(shadowMap)) {
			if (isVisibleWithin == false) {
				shadowMaps.remove(shadowMap);
			}
		} else {
			if (isVisibleWithin) {
				shadowMaps.put(shadowMap, character);  // We know are visible in this shadowmap and distance from its center
				isDiscovered = true;  // this location has been seen by a character.
				map.alertToVisualChangeAtLocation(this);
			}
		}
	}
	
	public boolean isOpaque() {
		if (locationType == LocationType.WALL) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isClear() {
		return !isOpaque();
	}
	
	public LocationInfo getLocationInfo() {
		return new LocationInfo(this);
	}
	
	public int distanceTo(DungeonPosition p) {
		return position.distanceTo(p);
	}
	
	
	public void clearLocation() {
		locationType = LocationType.FLOOR;
	}
	
	public void setAsExit() {
		locationType = LocationType.EXIT;
	}
	
	public void pickupItem(Ability item) {
		itemList.remove(item);
		map.alertToVisualChangeAtLocation(this);  
	}
	
	public void dropItem(Ability item) {
		itemList.add(item);
		map.alertToVisualChangeAtLocation(this);  
	}
	
	// cant be called until the basic dungeon map has been fully determined (as far as setting LcoationType)
	// This is called for the entire map before the tile name can be set.
	public void setTileType() {
		if (map.safeLocation(x,y).isClear()) {
			tileType =  TileType.CLEAR;
		} else 
		
		if (map.safeLocation(x,y-1).isClear()) {
			tileType =  TileType.FRONT_FACE;
		} else
		
		if (map.safeLocation(x,y+1).isClear()) {
			tileType = TileType.REAR_FACE;
		} else {

			tileType =  TileType.NO_FACE;
		}
	}
	
	public AtLocation whatIsAtLocation() {
		if (creature != null) {
			if (creature instanceof Character) {
				return AtLocation.CHARACTER;
			} else {
				return AtLocation.MONSTER;
			}
		}
	
		if (isOpaque()) {
			return AtLocation.WALL;
		} else {
			return AtLocation.FREE;
		}

	}
	
	
	// Monsters use this to zero in on the closest visible character to them.
	public Character getClosestVisibleCharacter() {
		int rangeOfClosestChar = 100;
		Character closestVisibleChar = null;
		
		// Each entry in this list of shadowmaps is a character that can see the monster, and hence, vice-versa.
		for (ShadowMap shadowMap : shadowMaps.keySet()) {
			Character character = shadowMaps.get(shadowMap);
			if (character != null) {
				int d = position.distanceToSpecialSpecial(character.getPosition());
				if (d < rangeOfClosestChar && character.isAlive()) {
					rangeOfClosestChar = d;
					closestVisibleChar = character;
				}
			}
		}
		
		return closestVisibleChar;
	}
	
	// To work out which tile to use, there are the following rules.
	// 1. anything with a space to its south *must* be a front-facing wall (1 of 4 front-facing types)
	// 2. anything with space to its north, but not to its south, *must* be a rear-facing wall. (1 of 4 rear-facing types)
	// 3. After determining types 1 and 2,  anything left must be either a vertical side-wall, or a corner, or a combination of both.  
	//    i.e.  one thing on one side of the tile and another thing on the other. 
	// 4. You just need to look to the immediate right and left of any such tile to determine its specific type on each side.
	// 5. If there is space to that side, it has a vertical (straight) wall on that side
	// 6. If there is a front-facing wall to that side, it has a front-facing corner on that side.
	// 7. If there is a rear-facing wall to that side, it has a rear-facing corner on that side.
	
//		// front facing walls - they have space to their south
//		FrontWest("FrontWest"), FrontMiddle("FrontMiddle"), FrontEast("FrontEast"), FrontDouble("FrontDouble"),
//		
//		// rear facing walls - they have space to their north but not to their south.
//		RearWest("RearWest"), RearMiddle("RearMiddle"), RearEast("RearEast"), RearDouble("RearDouble"),
//		
//		// vertical side-walls
//		VertWest("VertWest"), VertEast("VertEast"), VertDouble("VertDouble"),   
//		
//		// Rear facing corners - these are corners meeting rear facing walls  _|  |_
//		RearWestCorner("RearWestCorner"),RearEastCorner("RearEastCorner"), RearDoubleCorner("RearDoubleCorner"),
//		
//		//                                                                       _   _
//		// front facing corners - these are corners meeting front facing walls  |     |
//		FrontWestCorner("FrontWestCorner"), FrontEastCorner("FrontEastCorner"), FrontDoubleCorner("FrontDoubleCorner"),  
//		
//		// Now we come to combinations of the above where there is one thing on one side and another thing on the other.
//		// A vertical wall on one side, and a rear facing corner on the other
//		VertWestRearEastCorner("VertWestRearEastCorner"), VertEastRearWestCorner("VertEastRearWestCorner"), 
//		
//		// A vertical wall on one side, and a front facing corner on the other
//		VertWestFrontEastCorner("VertWestFrontEastCorner"), VertEastFrontWestCorner("VertEastFrontWestCorner"),  
//		
//		// A front facing corner on one side and a rear facing corer on the other.
//		FrontWestCornerRearEastCorner("FrontWestCornerRearEastCorner"), FrontEastCornerRearWestCorner("FrontEastCornerRearWestCorner");  
	
	
	// the procedure for fully calulating the map is to first draw the basic map with squigley lines and so on
	// then iterate across the entire map and setTileType
	// then iterate across it again to setTileName which can be used to work out the sprite to draw this location with.
	public void setTileName() {
		tileName = calculateTileName();
		if (tileName == null) {
			System.out.println("*****");
		}
	}
	
	private String calculateTileName() {

		String tileName = "CLEAR_FLOOR_IMAGE";
		
		switch(locationType) {
			case WALL:
				tileName = null;

				// if the south of the tile is empty, it definitely a front facing tile
				boolean eastBlank = map.safeLocation(x + 1,y).isClear();
				boolean westBlank = map.safeLocation(x - 1,y).isClear();

				// front facing tiles
				if (tileType == TileType.FRONT_FACE) {
					if (eastBlank && westBlank) {
						tileName = "FrontDouble";
					} else if (westBlank) {
						tileName = "FrontWest";
					} else if (eastBlank) {
						tileName = "FrontEast";
					} else {
						tileName = "FrontMiddle";
						
						// special case - front walls join with vertical walls to its north
						boolean northBlank = map.safeLocation(x,y+1).isClear();
						if (northBlank == false) {
							boolean northEastBlank = map.safeLocation(x+1,y+1).isClear();
							boolean northWestBlank = map.safeLocation(x-1,y+1).isClear();
							
							tileName = northEastBlank && northWestBlank ? "FrontDouble" : northWestBlank ? "FrontWest" : northEastBlank ? "FrontEast" : "FrontMiddle";	
						}
					}
					
					return tileName;
				}
				
				TileType westSide = map.safeLocation(x-1,y).tileType;
				TileType eastSide = map.safeLocation(x+1,y).tileType;
				
				// rear facing tiles
				// if the north of the tile is empty it will contain a rear-facing aspect.
				// For the purposes of rear-facing tiles, 'blank' is either a clear floor tile, or a front-facing tile.  Strange but true.
				if (westSide == TileType.FRONT_FACE) {
					westBlank = true;
				}
				if (eastSide == TileType.FRONT_FACE) {
					eastBlank = true;
				}
				if (tileType == TileType.REAR_FACE) {
					tileName = eastBlank && westBlank ? "RearDouble" : westBlank ? "RearWest" : eastBlank ? "RearEast" : "RearMiddle";
					if (tileName != null) {
						return tileName;
					}
				}
				
				// Now we are down to vertical walls, front facing corners or rear facing corners 
				// possibly a different type on each side of the tile.  
				// We have to test each side to determine which.
				switch (westSide) {
					case CLEAR:
						switch (eastSide) {
							case CLEAR:
								return "VertDouble";
							case FRONT_FACE:
								return "VertWestFrontEastCorner";
							case REAR_FACE:
								return "VertWestRearEastCorner";
							case NO_FACE:
							default:
								return "VertWest";
						}
					case FRONT_FACE:
						switch (eastSide) {
							case CLEAR:
								return "VertEastFrontWestCorner";
							case FRONT_FACE:
								return "FrontDoubleCorner";
							case REAR_FACE:
								return "FrontWestCornerRearEastCorner";
							case NO_FACE:
							default:
								return "FrontWestCorner";
						}
					case REAR_FACE:
						switch (eastSide) {
							case CLEAR:
								return "VertEastRearWestCorner";
							case FRONT_FACE:
								return "FrontEastCornerRearWestCorner";
							case REAR_FACE:
								return "RearDoubleCorner";
							case NO_FACE:
							default:
								return "RearWestCorner";
						}
					case NO_FACE:
					default:
						switch (eastSide) {
							case CLEAR:
								return "VertEast";
							case FRONT_FACE:
								return "FrontEastCorner";
							case REAR_FACE:
								return "RearEastCorner";
							case NO_FACE:
							default:
								return "RearDouble";  // is this case possible?
						}
					}

			case FLOOR:
				
				boolean northWall = map.safeLocation(x,y + 1).isOpaque();
				boolean eastWall = map.safeLocation(x + 1,y).isOpaque();
				boolean neWall = map.safeLocation(x + 1,y + 1).isOpaque();

				if (northWall) {
					tileName = eastWall ? "TOP_AND_SIDE_SHADOW_FLOOR_IMAGE" : neWall ? "TOP_SHADOW_FLOOR_IMAGE" : "ANGLE_TOP_SHADOW_FLOOR_IMAGE";
				} else {
					if (eastWall)
						tileName = neWall ? "SIDE_SHADOW_FLOOR_IMAGE" : "ANGLE_SIDE_SHADOW_FLOOR_IMAGE";
					else if (neWall) {
						tileName = "CORNER_SHADOW_FLOOR_IMAGE";
					}
				}
				break;
				
			case EXIT:
				tileName = "STAIRS_DOWN_IMAGE";
				break;
				
			default:
				tileName = "CLEAR_FLOOR_IMAGE";
				break;
		}
		return tileName;
	}
	
	public ItemList getItemList() {
		return new ItemList(itemList);
	}
	
	public void persistLocation(ObjectOutputStream out) throws IOException {
		//	4) Each Location saves its details
		out.writeObject(position);
		out.writeObject(locationType);
		out.writeObject(tileType);
		out.writeInt(creatureFacingDir); 
		out.writeObject(tileName);  // the type of tile which will be used to work out the Sprite to display it.
		out.writeObject(isDiscovered);
	}
	
	public void persistThings(ObjectOutputStream out) throws IOException {

		
		//	5) Each Location saves its Creature
		if (creature == null) {
			out.writeInt(TurnProcessor.NO_CURRENT_CREATURE);
		} else {
			out.writeInt(creature.uniqueId);
		}

		//	6) Each Location saves its list of Abilities (Items)
		out.writeInt(itemList.size());
		for (Ability item : itemList) {
			item.persist(out);
		}
	}
	
}
