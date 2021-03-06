package com.dbash.models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Vector;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.dbash.models.IDungeonQuery.AtLocation;
import com.dbash.presenters.root.tutorial.TutorialPresenter;
import com.dbash.util.EventBus;
import com.dbash.util.L;
import com.dbash.util.Randy;
import com.me.dbash.Dbash;

// A Location embodies everything about a particular tile in the dungeon, including which characters can currently see it
// (Each character has a shadowMap that informs Locations that it can see them)
public class Location {
	
	public static boolean LOG = false && L.DEBUG;
	 
	public static final float minNotVisibleTint = 0.22f;
	public static final float minVisibleTint = 0.3f;
	public static final float maxVisibileTint = 1f;
	
	public enum LocationType {
		WALL,
		FLOOR,
		EXIT
	};
		
	public enum TileType {
		CLEAR, 
		FRONT_FACE,
		REAR_FACE,
		NO_FACE,
		ISLAND
	};
		
	public enum TorchType {
		NONE,
		FRONT,
		CENTRAL,
		EAST,
		WEST,
		EAST_DOUBLE,
		WEST_DOUBLE,
		INVISIBLE
	};
	
	public enum IslandType {
		TORCH_ISLAND ("torch_island", 1),
		GLOW_ISLAND ("glow_island", 2),
		DARK_ISLAND ("dark_island", 3);
		
		private final String islandName;
		private final int num;

	    private IslandType(String islandName, int num) {
	        this.islandName = islandName;
	        this.num = num;
	    }

	    public int getNum() {
	        return num;
	    }
	    
	    public String getIslandName() {
	        return islandName;
	    }
	    
	    public static IslandType fromInt(int n) {
	          for (IslandType b : IslandType.values()) {
	            if (n == b.getNum()) {
	              return b;
	            }
	          }
	        return null;
	     }
	    
	    public static IslandType getRandomType() {
	    	return IslandType.fromInt(Randy.getRand(1,  DARK_ISLAND.getNum()));
	    }
	}
	
	public enum RoughTerrainType {
		BONES ("bones", 1),
		ROCKS ("rocks", 2),
		MUD ("mud", 3),
		HOLE ("hole", 4);
		
	    private final String value;
	    private final int num;

	    private RoughTerrainType(String value, int num) {
	        this.value = value;
	        this.num = num;
	    }

	    public String getValue() {
	        return value;
	    }
	    
	    public int getNum() {
	        return num;
	    }
	    
	    public Boolean isPassable() {
	    	return value != HOLE.getValue();
	    }
	    
	    public static RoughTerrainType fromString(String text) {
	        if (text != null) {
	          for (RoughTerrainType b : RoughTerrainType.values()) {
	            if (text.equalsIgnoreCase(b.getValue())) {
	              return b;
	            }
	          }
	        }
	        return null;
	     }
	    
	    public static RoughTerrainType fromInt(int n) {
	          for (RoughTerrainType b : RoughTerrainType.values()) {
	            if (n == b.getNum()) {
	              return b;
	            }
	          }
	        return null;
	     }
	    
	    public static RoughTerrainType getRandomType() {
	    	return RoughTerrainType.fromInt(Randy.getRand(1,  HOLE.getNum()));
	    }
	}
	
	public Map map;
	public LocationType locationType;
	public TileType tileType;
	public Creature creature;
	public int	creatureFacingDir; // Dungeon.EAST for example
	public Vector<Ability> itemList;
	public boolean isDiscovered;
	public DungeonPosition position;
	int x;
	int y;
	public float tint;
	public float permTint;
	public TorchType torch = TorchType.NONE;
	public TorchType sideWallType = TorchType.NONE;
	public RoughTerrainType roughTerrainType;
	public LocationInfo locationInfo;
	public ItemList itemInfoNoRough;
	public ItemList itemInfoWithRough;
	public IslandType islandType;
	public String islandName;
	public boolean isHardcoded;
	public boolean isShadowed;
	public String shadowName;
	public String prefix;
	
	public String tileName; 
	public String floorName;
	public String roughTerrainName;
	public String overlayName;
	 
	private static HashMap<String, Integer> tileVariants = new HashMap<String, Integer>();
	
	// will create itself and add itself to the map.
	public Location(Map map, int x, int y, String prefix)
	{
		this.map = map;
		this.x = x;
		this.y = y;
		this.prefix = prefix;
		position = new DungeonPosition(x,y);
		locationType = LocationType.WALL;  // defaults to wall
		itemList = new Vector<Ability>();  // no items
		creature = null;   // no creature;
		tileType = null;  // cant be worked out yet
		isDiscovered = false;
		tileName = "CLEAR_FLOOR_IMAGE";
		permTint = minVisibleTint;  // starts off at the base lowest light level.  Permanent lights will permanently raise this level.
		updateLocationInfo();
		clearTint();
	}
	
	public Location() {
		locationType = LocationType.WALL;  // defaults to wall
		tileType = TileType.NO_FACE;  // default to solid (encased) rock
	}
	
	public Location(ObjectInputStream in, Map map, AllCreatures allCreatures, IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery) throws IOException, ClassNotFoundException {
		this.map = map;
		itemList = new Vector<Ability>();  // no items
		position = (DungeonPosition) in.readObject();
		this.x = position.x;
		this.y = position.y;
		locationType = (LocationType) in.readObject();
		tileType = (TileType) in.readObject();
		shadowName = (String) in.readObject();
		if (shadowName != null) {
			isShadowed = true;
		}
		creatureFacingDir = in.readInt();
		tileName = (String) in.readObject();
		floorName = (String) in.readObject();
		roughTerrainName = (String) in.readObject();
		overlayName = (String) in.readObject();
		isDiscovered = (Boolean) in.readObject();
		torch = (TorchType) in.readObject();
		isHardcoded = in.readBoolean();
		permTint = minVisibleTint;  // starts off at the base lowest light level.  Permanent lights will permanently raise this level.
		updateLocationInfo();
		clearTint();
	}
	
	public void addCentralLight(DungeonPosition thePosition) {
		DungeonPosition torchPosition = new DungeonPosition(thePosition);
		
		map.addLight(new Light(torchPosition, 1, Light.CENTRAL_TORCH_STRENGTH, true));
		torchPosition.y--;
		map.addLight(new Light(torchPosition, Light.WALL_TORCH_RANGE-1, Light.CENTRAL_TORCH_STRENGTH, true));
		torchPosition.y += 2;
		map.addLight(new Light(torchPosition, Light.WALL_TORCH_RANGE-1, Light.CENTRAL_TORCH_STRENGTH, true));
		torchPosition.y--;
		torchPosition.x--;
		map.addLight(new Light(torchPosition, Light.WALL_TORCH_RANGE-1, Light.CENTRAL_TORCH_STRENGTH, true));
		torchPosition.x +=2;
		map.addLight(new Light(torchPosition, Light.WALL_TORCH_RANGE-1, Light.CENTRAL_TORCH_STRENGTH, true));
	}
	
	public void addTorch(TorchType torch) {
		calculateTorchSideWall();
		DungeonPosition torchPosition = new DungeonPosition(position);
		this.torch = torch;
		
		if (torch == TorchType.CENTRAL || (torch == TorchType.INVISIBLE)) {
			addCentralLight(torchPosition);
		} else if (torch == TorchType.FRONT ) {
			torchPosition.y--;
			map.addLight(new Light(torchPosition, Light.WALL_TORCH_RANGE, Light.WALL_TORCH_STRENGTH, true));
		} else if (torch == TorchType.WEST || torch == TorchType.WEST_DOUBLE) {
			if (L.NEW_TILES) {
				torchPosition.x--;
			} 
			map.addLight(new Light(torchPosition, Light.WALL_TORCH_RANGE, Light.WALL_TORCH_STRENGTH, true));
 		} else if (torch == TorchType.EAST || torch == TorchType.EAST_DOUBLE) {
			if (L.NEW_TILES) {
				torchPosition.x++;
			} 
			map.addLight(new Light(torchPosition, Light.WALL_TORCH_RANGE, Light.WALL_TORCH_STRENGTH, true));
 		} 
		updateLocationInfo();
	}
	
	public void load(ObjectInputStream in, Map map, AllCreatures allCreatures, IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery) throws IOException, ClassNotFoundException {
		//	5) Each Location reads its Creature
		int cId = in.readInt();
		if (cId == TurnProcessor.NO_CURRENT_CREATURE) {
			creature = null;
		} else {
			setCreatureAndUpdatePresenter(allCreatures.getCreatureByUniqueId(cId));
		}

		//	6) Each Location reads its list of Abilities (Items)
		int items = in.readInt();
		for (int i=0;i<items;i++) {
			itemList.add(new Ability(in, null, dungeonEvents, dungeonQuery));
		}
		
		if (torch != TorchType.NONE) {
			addTorch(torch);
		}
		
		updatePresenter(); 
	}
	
	public DungeonPosition getPosition() {
		return new DungeonPosition(x,y);
	}
	
	public void setAsIsland(IslandType islandType) {
		locationType = LocationType.FLOOR;
		tileType = TileType.ISLAND;
		
		if (islandType == null) {
			islandType = IslandType.getRandomType();
		} 
		
		this.islandType = islandType;
		islandName = calcVariantToUse(islandType.islandName);
	}
	
	public void setCreature(Creature creature) {
		this.creature = creature;  
	}
	
	public void updatePresenter() {
		updateLocationInfo();
		map.alertToVisualChangeAtLocation(this);  
	}
	
	public void setCreatureAndUpdatePresenter(Creature creature) {
		setCreature(creature);
		updatePresenter();
	}
	
	public void moveCreature(Creature creature) {
		setCreature(creature);
			for (Ability ability : itemList) {
				if (ability.isRoughTerrain() && roughTerrainType != RoughTerrainType.HOLE) {
					ability.applyToCreature(creature);
					EventBus.getDefault().event(TutorialPresenter.ROUGH_TERRAIN_EVENT, null);
			}
		}
	}
	
	// Onlt gets lighter due to, err lights.
	public void setTint(float newTint) {
		if (newTint > maxVisibileTint) {
			newTint = maxVisibileTint;
		}
		if (newTint > tint) {  // cant make it darker than it already is
			tint = newTint;
			locationInfo.tint = tint;  // just change the tint for efficiency
		}
	}
	
	public float getTint() {
		return tint;
	}
	
	// Only gets lighter due to, err lights.
	public void setPermTint(float newTint) {
		if (newTint > maxVisibileTint) {
			newTint = maxVisibileTint;
		}
		if (newTint > permTint) {  // cant make it darker than it already is
			permTint = newTint;
			setTint(permTint);
		}
	}
	
	public void clearTint() {
		tint = permTint;  // darkest tile.  Lights will make it lighter.
	}
	
	public void setDiscovered() {
		if (isDiscovered == false) {
			isDiscovered = true;
			updatePresenter();
		}
	}
	
	public boolean isOpaque() {
		if (locationType == LocationType.WALL) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isFrontFacing() {
		if (tileType == TileType.FRONT_FACE) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isClear() {
		return !isOpaque();
	}
	
	public boolean isTotallyEmpty() {
		boolean result = isClear();
		if (locationType == LocationType.EXIT) {
			result = false;
		}
		
		if (tileType == TileType.ISLAND) {
			result = false;
		}
		
		if (getRoughTerrain() != null) {
			result = false;
		}
		
		return result;
	}
	
	public boolean isEmpty() {
		boolean result = isClear();
		if (locationType == LocationType.EXIT) {
			result = false;
		}
		
		return result;
	}
	
	public boolean isNearWall() {
		if (map.safeLocation(x-1, y).isOpaque()) { return true;}
		if (map.safeLocation(x+1, y).isOpaque()) { return true;}
		if (map.safeLocation(x, y-1).isOpaque()) { return true;}
		if (map.safeLocation(x, y+1).isOpaque()) { return true;}
		return false;
	}
	
	public LocationInfo getLocationInfo() {
		if (locationInfo == null) {
			locationInfo = new LocationInfo(this);
		}
		return locationInfo;
	}
	
	private void updateLocationInfo() {
		setItemInfos();
		locationInfo = new LocationInfo(this);
	}
	
	public int distanceTo(DungeonPosition p) {
		return position.distanceTo(p);
	}
	
	
	public void clearLocation() {
		locationType = LocationType.FLOOR;
	}
	
	public void setLocationToWall() {
		locationType = LocationType.WALL;
	}
	
	public void setRoughTerrain(RoughTerrainType roughTerrainType, IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery) {
		int id = Ability.getIdForName(roughTerrainType.getValue());
		Ability ability = new Ability(id, null, 1, dungeonEvents, dungeonQuery); 
		itemList.add(ability);
		roughTerrainType = getRoughTerrain();
		if (!isHardcoded) {
			roughTerrainName = calcVariantToUse(roughTerrainType.getValue());
		}
		updateLocationInfo();
	}
	
	public RoughTerrainType getRoughTerrain() {
		if (roughTerrainType != null) {
			return roughTerrainType;
		}
		
		for (Ability ability : itemList) {
			roughTerrainType = RoughTerrainType.fromString(ability.ability.name);
			if (roughTerrainType != null) {
				break;
			}
		}
		return roughTerrainType;
	}
	
	public boolean hasRoughTerrain() {
		if (getRoughTerrain() == null) {
			return false;
		} else {
			return true;
		}
	}
	
	public boolean hasIsland() {
		if (tileType == Location.TileType.ISLAND) {
			return true;
		} else {
			return false;
		}
	}
	
	public void setAsExit() {
		locationType = LocationType.EXIT;
	}
	
	public void pickupItem(Ability item) {
		itemList.remove(item);
		updatePresenter();  
	}
	
	public void dropItem(Ability item) {
		itemList.add(item);
		updatePresenter();  
	}
	
	// cant be called until the basic dungeon map has been fully determined (as far as setting LcoationType)
	// This is called for the entire map before the tile name can be set.
	public void setTileType() {
		if (tileType != TileType.ISLAND) {
			if (map.safeLocation(x,y).isClear()) {
				tileType =  TileType.CLEAR;
			} else if (map.safeLocation(x,y-1).isClear()) {
				tileType =  TileType.FRONT_FACE;
			} else if (map.safeLocation(x,y+1).isClear()) {
				tileType = TileType.REAR_FACE;
			} else {
				tileType =  TileType.NO_FACE;
			}
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
			RoughTerrainType roughTerrainType = getRoughTerrain();
			if (roughTerrainType != null) {
				if (roughTerrainType == RoughTerrainType.HOLE) {
					return AtLocation.HOLE;
				}
			} 
				
			return AtLocation.FREE;
		}
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
	public void setLocationNames() {

		floorName = calcFloorVariantToUse(prefix.concat("CLEAR_FLOOR_IMAGE"));

		if (!isHardcoded) {
			tileName = calculateTileName();
			if (tileName != null) {
				tileName = calcVariantToUse(prefix.concat(tileName));
			}
			if (hasIsland()) {
				overlayName = islandName;
			}
		} else {
			isHardcoded = true;
		}
		
		shadowName = getShadowName();
		if (shadowName != null) {
			isShadowed = true;
		}
	}
	
	public void setHardcodeTilename(String hardcodeTilename) {
		this.tileName = hardcodeTilename;
		isHardcoded = true; 
	}
	
	public void setOverlayTilename(String hardcodeOverlayName) {
		this.overlayName = hardcodeOverlayName;
	}
	
	private Integer getVariantCount(String filename) {
		Integer count = tileVariants.get(filename);
		if (count == null) {
			count = 1;
			Sprite sprite = null;
			
			do {
				Integer postfix = count+1;
				sprite = Dbash.theSpriteManager.fetchSprite(filename.concat(postfix.toString()));  // oh dear
				if (sprite != null) {
					count++;
				}
			} while (sprite != null);
			
			tileVariants.put(filename, count);
		}
		
		return count;
	}
	
	//public static final double firstTileProbability = 70.0;
	
	private String calcFloorVariantToUse(String tilename) {
		if (hasRoughTerrain()) {
			return tilename;
		} else {
			return calcVariantToUse(tilename);
		}
	}
	
	private String calcVariantToUse(String tilename) {
		String tilenameToUse = tilename;
		Integer count = getVariantCount(tilename);
		int random = Randy.getRand(1, 100);
		
		if ((count > 1) && (random > L.NORMAL_TILE_PROB)) {
			double gap = (100.0 - L.NORMAL_TILE_PROB) / (double) (count - 1);
			double index = 2.0 + (random - L.NORMAL_TILE_PROB) / gap;
			Integer tileNum = (int) index;
			if (tileNum > count) {
				tileNum = count;
			}
			
			tilenameToUse = tilename.concat(tileNum.toString());
		} 
		
//		for (Integer i=count; i > 1; i--) {
//		    int threashold = 100/(i*2);
//			if (random <= threashold) {
//				tilenameToUse = tilename.concat(i.toString());
//				break;
//			}
//		}
		
		return tilenameToUse;
	}
	
	/**
	 * Once all the tileNames have been identified, we can do things like factor in terrain, torches and 
	 * whatnot.
	 */
	public void doPostMapGenerationProcessing() {
		if (LOG) L.log("tileName: %s", tileName);
		// torches
		if (tileType == TileType.ISLAND) {
			locationType = LocationType.WALL; // the location of an island needs to be FLOOR for calculating tilenames but WALL in practice.
			if (islandType == IslandType.TORCH_ISLAND) {
				addTorch(TorchType.CENTRAL);
			} else if (islandType == IslandType.GLOW_ISLAND) {
				addTorch(TorchType.INVISIBLE);
			}
		} else if (Randy.getRand(1, L.TORCH_DENSITY) == 1 && map.okToPlaceTorch(this)) {
			if (tileName != null) {
				createTorchAt();
			}
		}
		updateLocationInfo();
	}
	
	public void createTorchAt() {
		
		calculateTorchSideWall();
		
		if (tileType == TileType.FRONT_FACE) {
			addTorch(TorchType.FRONT);
		} else if (sideWallType == TorchType.WEST) {
			if (L.NEW_TILES) {
				map.location[x][y].addTorch(TorchType.WEST);
			} else {
				map.location[x-1][y].addTorch(TorchType.WEST);
			}
		} else if (sideWallType == TorchType.EAST) {
			if (L.NEW_TILES) {
				map.location[x][y].addTorch(TorchType.EAST);
			} else {
				map.location[x+1][y].addTorch(TorchType.EAST);
			}
		} else if (sideWallType == TorchType.WEST_DOUBLE) {
			map.location[x][y].addTorch(TorchType.WEST_DOUBLE);
		} else if (sideWallType == TorchType.EAST_DOUBLE) {
			map.location[x][y].addTorch(TorchType.EAST_DOUBLE);
		}
	}
	
	public boolean hasTorch() {
		if (torch == TorchType.NONE) {
			return false;
		} else {
			return true;
		}
	}
	
	private void calculateTorchSideWall() {
		if (locationType == LocationType.WALL && tileType == TileType.NO_FACE) {
			
			TileType westSide = getTileTypeForTileNames(x-1,y);
			TileType eastSide = getTileTypeForTileNames(x+1,y);
			
			switch (westSide) {
				case CLEAR:
					switch (eastSide) {
						case CLEAR:
							if (Randy.getRand(1, 2) == 1) {
								sideWallType = TorchType.WEST_DOUBLE;
							} else {
								sideWallType = TorchType.EAST_DOUBLE;
							}
							break;
						case NO_FACE:
							sideWallType = TorchType.WEST;
							break;
						default:
							break;
					}
				case NO_FACE:
					switch (eastSide) {
						case CLEAR:
							sideWallType = TorchType.EAST;
							break;
						default:
							break;
					}
				default:
					break;
			}
		}
	}
	
	private String calculateTileName() {

		String tileName = null;
		
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
						
//						// special case - front walls join with vertical walls to its north
//						boolean northBlank = map.safeLocation(x,y+1).isClear();
//						if (northBlank == false) {
//							boolean northEastBlank = map.safeLocation(x+1,y+1).isClear();
//							boolean northWestBlank = map.safeLocation(x-1,y+1).isClear();
//							
//							tileName = northEastBlank && northWestBlank ? "FrontDouble" : northWestBlank ? "FrontWest" : northEastBlank ? "FrontEast" : "FrontMiddle";	
//						}
					}
					
					return tileName;
				}
				
				TileType westSide = getTileTypeForTileNames(x-1,y);
				TileType eastSide = getTileTypeForTileNames(x+1,y);
				
				if (L.NEW_TILES == false) {
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
				} else {
					// rear facing tiles
					// if the north of the tile is empty it will contain a rear-facing aspect.
					if (tileType == TileType.REAR_FACE) {
						switch (westSide) {
						case CLEAR:
							switch (eastSide) {
								case CLEAR:
									return "RearDouble";
								case FRONT_FACE:
									return "RearWestCornerEast";
								case REAR_FACE:
									return "RearWest";
								case NO_FACE:
								default:
									return "RearWest";
							}
						case FRONT_FACE:
							switch (eastSide) {
								case CLEAR:
									return "RearEastCornerWest";
								case FRONT_FACE:
									return "FrontDoubleCorner";
								case REAR_FACE:
									return "RearCornerWest";
								case NO_FACE:
								default:
									return "RearCornerWest";  // LAtest prob?
							}
						case REAR_FACE:
							switch (eastSide) {
								case CLEAR:
									return "RearEast";
								case FRONT_FACE:
									return "RearCornerEast";
								case REAR_FACE:
									return "RearMiddle";
								case NO_FACE:
								default:
									return "RearMiddle";
							}
						case NO_FACE:
						default:
							switch (eastSide) {
								case CLEAR:
									return "RearEast";
								case FRONT_FACE:
									return "RearCornerEast";
								case REAR_FACE:
									return "RearMiddle";
								case NO_FACE:
								default:
									return "RearMiddle";  // is this case possible?
							}
						}
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
								return null;
						}
					}
				
			case EXIT:
				tileName = "STAIRS_DOWN_IMAGE";
				break;
				
			case FLOOR:
			default:
				break;
		}
		
		return tileName;
	}
	
	private String getShadowName() {
		
		String shadowName = null;
		
		if (locationType == LocationType.FLOOR) {
			boolean northWall = map.safeLocation(x,y + 1).castsShadow();
			boolean eastWall = map.safeLocation(x + 1,y).castsShadow();
			boolean neWall = map.safeLocation(x + 1,y + 1).castsShadow();

			if (northWall) {
				shadowName = eastWall ? "TOP_AND_SIDE_SHADOW_FLOOR_IMAGE" : neWall ? "TOP_SHADOW_FLOOR_IMAGE" : "ANGLE_TOP_SHADOW_FLOOR_IMAGE";
			} else {
				if (eastWall)
					shadowName = neWall ? "SIDE_SHADOW_FLOOR_IMAGE" : "ANGLE_SIDE_SHADOW_FLOOR_IMAGE";
				else if (neWall) {
					shadowName = "CORNER_SHADOW_FLOOR_IMAGE";
				}
			}
		}
		
		return shadowName;
	}
	
	private TileType getTileTypeForTileNames(int x, int y) {
		TileType tileType = map.safeLocation(x,y).tileType;
		if (tileType == TileType.ISLAND) {
			tileType = TileType.CLEAR;
		}
		return tileType;
	}
	
	public ItemList getItemList(boolean includeRoughTerrain) {
		if (itemInfoWithRough == null) {
			setItemInfos();
		}
		
		if (includeRoughTerrain) {
			return itemInfoWithRough;
		} else {
			return itemInfoNoRough;
		}
	}
	
	public boolean isDrawable() {
		if (tileType == TileType.NO_FACE && tileName == null) {
			return false;
		} else {
			return true;
		}
	}
	
	public void persistLocation(ObjectOutputStream out) throws IOException {
		//	4) Each Location saves its details
		out.writeObject(position);
		out.writeObject(locationType);
		out.writeObject(tileType);
		out.writeObject(shadowName);
		out.writeInt(creatureFacingDir); 
		out.writeObject(tileName);  // the type of tile which will be used to work out the Sprite to display it.
		out.writeObject(floorName);
		out.writeObject(roughTerrainName);
		out.writeObject(overlayName);
		out.writeObject(isDiscovered);
		out.writeObject(torch);
		out.writeBoolean(isHardcoded);
	}
	
	public boolean castsShadow() {
		boolean result = isOpaque();
		if (result && tileType == TileType.ISLAND) {
			result = false;
		}
		return result;
	}
	
	private void setItemInfos() {
		itemInfoWithRough = new ItemList(itemList, true);
		itemInfoNoRough = new ItemList(itemList, false);
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
	
	public String toString() {
		return "x:"+x+" y:"+y+" name:"+tileName+" torch:"+torch;
	}
}
