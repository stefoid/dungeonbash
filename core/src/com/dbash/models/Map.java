package com.dbash.models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import com.dbash.models.Location.LocationType;
import com.dbash.models.Location.RoughTerrainType;
import com.dbash.models.Location.TileType;
import com.dbash.models.Location.TorchType;
import com.dbash.util.L;
import com.dbash.util.Randy;
import com.dbash.util.Rect;

@SuppressWarnings("unused")

// This class is basically a two dimension array of 'locations' and the code to build a maze  in it.
// Each location has all the info about that location, including a spritename (TileName) and any creatures or items
// currently at that location.
// Map doesnt know about Monsters, Characters etc...  To move creatures and items around in it, interact with the Locations
// directly.  Those changes to Locations will emit observable Presentation events
public class Map implements IPresenterMap {
	public static final boolean LOG = false && L.DEBUG;
	
	public static int RANGE = 5;
	public static int LOS = -1;
	public static int TUTORIAL = -100;
	
	public Location[][] location;  // all the locations
	public ArrayList<Location> drawableLocations = new ArrayList<Location>(); // for clearing lights.
	public int width;
	public int height;
	public int level;
	public DungeonPosition startPoint;
	public DungeonPosition exitPoint;
	public DungeonPosition[] roomPoints;
	protected UIInfoListenerBag retainFocusBag;
	protected ArrayList<Light> tempLights;
	protected ArrayList<Light> permLights;

	protected IDungeonQuery dungeonQuery;

	protected ArrayList<Room> rooms;

	protected Vector<UILocationInfoListener> locationInfoListeners;
	protected Location solidRock = new Location();
	protected boolean lightingChanged;
	protected final int border = 2; // how thick the enclosing rock wall is
	protected Rect validRect;

	public String statueName = "statue";

	@SuppressWarnings("serial")
	public class MapException extends Exception {
	}

	public Map(int level, IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery) {
		
		String prefix = L.STRING_PREFIX;
		
		if (L.json.has("walls")) {
			prefix = L.json.getString("walls");
		}
		
		boolean dungeonNotCompleted = true;
		width = 13 + level + border * 2 - 2;
		height = width;
		validRect = new Rect(border, border, width - border * 2, height
				- border * 2);
		this.dungeonQuery = dungeonQuery;
		while (dungeonNotCompleted) {
			try {
				rooms = new ArrayList<Room>();
				retainFocusBag = new UIInfoListenerBag();
				locationInfoListeners = new Vector<UILocationInfoListener>();
				this.level = level;
				location = new Location[width][height];
				// initialize array of locations - by default will be WALLS.
				for (int x = 0; x < width; x++) {
					for (int y = 0; y < height; y++) {
						location[x][y] = new Location(this, x, y, prefix);
					}
				}

				// Start with a miniboss room or randomly placed squiggly line.
				DungeonPosition start = addMiniBossRoom();
				if (start == null) {
					start = getRandomPointAnywhere(false);
				}

				location(start).clearLocation();
				drawSquigglyLine(start);
				
				for (int i = 0; i < ((height * 2) / 3); i++) {
					drawSquigglyLine(getRandomPointForTunnels(true));
				}
				
				// now draw some rooms based on where the squigley lines have cleared space.
				roomPoints = new DungeonPosition[height / 2]; 

				for (int i = 0; i < roomPoints.length; i++) {
					//  isClearRequired,  notInRooms,  forTunnels, int minDistanceToEdge,  noHoles
					roomPoints[i] = getRandomPoint(true, true, false, 2 + border, false);
				}
				
				for (int i = 0; i < roomPoints.length; i++) {
					// otherwise all the rooms will be most likely drawn
					// together
					attemptRoom(roomPoints[i]);
				}

				for (Room room : rooms) {
					room.setIslands();
				}
				for (Room room : rooms) {
					room.addRoughTerrain(dungeonEvents, dungeonQuery);
				}
				
				setIslands();
				
				addRoughTerrain(dungeonEvents, dungeonQuery);
				
				setStartAndExitPoints();

				// Now make a preliminary pass to determine Tile types
				for (int x = 0; x < width; x++) {
					for (int y = 0; y < height; y++) {
						location(x, y).setTileType();
					}
				}

				// Then make secondary pass to determine tile names
				for (int x = 0; x < width; x++) {
					for (int y = 0; y < height; y++) {
						location(x, y).setLocationNames();
					}
				}

				setupLighting();
				for (Room room : rooms) {
					room.setTorches();
				}
				// Then make the last pass to determine post process the whole
				// map with appropriae tilenames
				for (int x = 0; x < width; x++) {
					for (int y = 0; y < height; y++) {
						location(x, y).doPostMapGenerationProcessing();
						if (location(x, y).isDrawable()) {
							drawableLocations.add(location(x, y));
						}
					}
				}

				addExitLight();
				dungeonNotCompleted = false;

				if (LOG) dump(null);

			} catch (MapException e) {
				dungeonNotCompleted = true;
				
				if (LOG) { 
					L.log("TRYING AGAIN!");
					e.printStackTrace();
				}
			}
		}
	}

	public boolean okToPlaceTorch(Location location) {

		if (isPointClearOfRooms(location.getPosition()) == false) {
			return false;
		}

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (location(x, y).hasTorch()) {
					if (location(x, y).position.distanceTo(location.position) < 5) {
						return false;
					}
				}
			}
		}
		return true;
	}

	public Map(ObjectInputStream in, IDungeonControl dungeon,
			AllCreatures allCreatures, IDungeonEvents dungeonEvents,
			IDungeonQuery dungeonQuery) throws IOException,
			ClassNotFoundException {
		retainFocusBag = new UIInfoListenerBag();
		this.dungeonQuery = dungeonQuery;
		locationInfoListeners = new Vector<UILocationInfoListener>();
		width = in.readInt();
		height = in.readInt();
		startPoint = (DungeonPosition) in.readObject();
		exitPoint = (DungeonPosition) in.readObject();
		location = new Location[width][height];

		setupLighting();

		// read the locations
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				location[x][y] = new Location(in, this, allCreatures, dungeonEvents, dungeonQuery);
			}
		}
		addExitLight();
	}

	public void load(ObjectInputStream in, IDungeonControl dungeon,
			AllCreatures allCreatures, IDungeonEvents dungeonEvents,
			IDungeonQuery dungeonQuery) throws IOException,
			ClassNotFoundException {
		// read the locations
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				location[x][y].load(in, this, allCreatures, dungeonEvents, dungeonQuery);
				if (location[x][y].isDrawable()) {
					drawableLocations.add(location(x, y));
				}
			}
		}
	}

	protected void setupLighting() {
		tempLights = new ArrayList<Light>();
		permLights = new ArrayList<Light>();
	}

	protected void addExitLight() {
		Light exitLight = new Light(exitPoint, 0, .4f, true); // low level light
																// - permanent
		addLight(exitLight);
	}

	// Called by Locations - the dungeonmap is one such subscriber and it posts
	// the locationInfo to the
	// locationPresenter that it belongs to.
	public void alertToVisualChangeAtLocation(Location location) {
		// if (LOG) L.log("");
		for (UILocationInfoListener listeners : locationInfoListeners) {
			listeners.locationInfoChanged(location);
		}
	}

	public Map() {
		location = new Location[1][1];
		location[0][0] = new Location(this, 0, 0, null);
	}

	public DungeonPosition getRandomPointForMonsterPlacement() throws MapException {
		return getRandomPoint(true, false, false, border, true);
	}

	public DungeonPosition getRandomPointAnywhere(boolean isFloorRequired)
			throws MapException {
		return getRandomPoint(isFloorRequired, false, false, border, false);
	}

	public DungeonPosition getRandomPointNotInRooms(boolean isFloorRequired)
			throws MapException {
		return getRandomPoint(isFloorRequired, true, false, border, false);
	}

	public DungeonPosition getRandomPointForTunnels(boolean isFloorRequired)
			throws MapException {
		return getRandomPoint(isFloorRequired, true, true, border, true);
	}

	public Location getWideSpaceLocationWork(boolean totallyEmpty) {
		try {
			for (int i = 0; i < 5000; i++) {
				DungeonPosition pos = getRandomPoint(true, true, false, border + 1, false);
				boolean good = true;
				for (int x = -1; x <= 1 && good; x++) {
					for (int y = -1; y <= 1 && good; y++) {
						Location loc = location[pos.x + x][pos.y + y];
						if (totallyEmpty) {
							good = loc.isTotallyEmpty();
						} else {
							good = loc.isEmpty();
						}
					}
				}

				if (good) {
					return location[pos.x][pos.y];
				}
			}
		} catch (MapException m) {
			return null;
		}

		return null;
	}

	public Location getWideSpaceLocation() {
		return getWideSpaceLocationWork(true);
	}
	
	private Location findExitLocation() {
		return getWideSpaceLocationWork(false);
	}
	
	/**
	 * Returns a random point, or null if it timed out.
	 */
	public DungeonPosition getRandomPoint(boolean isClearRequired, boolean notInRooms, boolean forTunnels, 
			int minDistanceToEdge, boolean noHoles) throws MapException {

		DungeonPosition posi = new DungeonPosition();
		int tries = 0;
		boolean found = false;
		while (!found && tries++ < 5000) {
			posi.x = Randy.getRand(minDistanceToEdge, width - 1 - minDistanceToEdge);
			posi.y = Randy.getRand(minDistanceToEdge, height - 1 - minDistanceToEdge);

			if (isClearRequired) {
				found = !location(posi).isOpaque();
			} else {
				found = true;
			}

			if (noHoles) {
				if (location(posi).getRoughTerrain() == RoughTerrainType.HOLE) {
					found = false;
				}
			}

			if (found) {
				if (notInRooms) {
					// cant be inside any hard rooms
					for (Room room : rooms) {
						boolean isInside;
						if (forTunnels) {
							isInside = room.isInsideForTunnels(posi);
						} else {
							isInside = room.isInside(posi);
						}

						if (isInside) {
							found = false;
						}
					}
				}
			}
		}

		if (tries < 5000) {
			return posi;
		} else {
			throw new MapException();
		}
	}

	// 1) check through all hardcoded rooms. If it has an exit point, set the
	// exit point
	// 2) if it doesnt, pick a wide space for the exit as normal.
	// 3) check through all hardcoded rooms for a startpoint. if you find one,
	// set it and be happy.
	// 4) otherwise, find any clear location that is far enough away from the
	// exit point, and set that. Do noit use hardcoded rooms for this either.
	// getWideSpaceLocation allready uses loc.totallyEmpty().
	// the start location finder needs to do the same thing, although it doesnt
	// have to be in a wide space.
	protected void setStartAndExitPoints() throws MapException {
		exitPoint = null;
		boolean foundExit = false;
		for (Room room : rooms) {
			if (room.getExitPosition() != null) {
				exitPoint = room.getExitPosition();
				location(exitPoint).setAsExit();
				foundExit = true;
				break;
			}
		}

		int i = 0;
		while (i++ < 500 && foundExit == false) {
			Location exitLocation = findExitLocation();
			if (exitLocation == null) {
				throw new MapException();
			} else {
				if (exitLocation.getRoughTerrain() == null && exitLocation.tileType != TileType.ISLAND) {
					exitPoint = exitLocation.getPosition();
					exitLocation.setAsExit();
					foundExit = true;
				}
			}
		}
		if (!foundExit) {
			throw new MapException();
		}

		// Now set the start position
		for (i = 0; i < 200; i++) {
			startPoint = getRandomPointNotInRooms(true);
			Location loc = location(startPoint);
			if (loc.isTotallyEmpty()) {
				if ((Math.abs(startPoint.x - exitPoint.x) + Math
						.abs(startPoint.y - exitPoint.y)) >= (height / 2)) {
					return;
				}
			}
		}
		throw new MapException();
	}

	protected void setIslands() {
		int numIslands = level / 4 + Randy.getRand(0, 2);
		for (int i = 0; i < numIslands; i++) {
			Location island = getWideSpaceLocation();
			if (island != null && isPointClearOfRooms(island.getPosition())) {
				island.setAsIsland(null); // random island type
			}
		}
	}

	public void drawBlankRoom(DungeonPosition position) {
		int w = Randy.getRand(2, height / 8);
		int h = Randy.getRand(2, height / 8);
		Rect area = new Rect(position, w, h);
		DungeonPosition testPoint = new DungeonPosition(0, 0);

		for (int x = (int) area.x; x < (int) (area.x + area.width); x++) {
			for (int y = (int) area.y; y < (int) (area.y + area.height); y++) {
				testPoint.x = x;
				testPoint.y = y;
				if (isPointClearOfRooms(testPoint)) {
					location(testPoint).clearLocation();
				}
			}
		}

	}

	// pick a random spot - it can’t be in any of the off-limits areas.
	// on a 1:N chance, try to implement a Room. Pick a Room from the list. get
	// its size.
	// Make a proposed rectangle and test it against (a) will it fit inside the
	// entire map and (b) all the implemented rooms.
	// It might be nice to have a Rect constructor that takes a dungeonPoint and
	// a width and height.
	// IF there is no overlap, we have a good spot for a room, so set its point
	// and tell it to room.clear() and add it to the list of rooms.
	// - we remember the room for latter processing stages in an array, along
	// with the miniboss Room, if there is one.
	public void attemptRoom(DungeonPosition position) {
		boolean useBlank = true;

		if (Randy.getRand(1, 1) == 1) {

			Room room = getRandomRoom();
			Rect area = new Rect(position, room.width, room.height);

			if (isRectClearOfRooms(area)) {
				if (LOG) L.log("useRoom");
				useBlank = false;
				Room newRoom = new Room(room);
				newRoom.setPosition(position, location);
				rooms.add(newRoom);
				newRoom.clearSpaces();
			}
		}

		if (useBlank) {
			if (LOG) L.log("useBlank");
			drawBlankRoom(position);
		}
	}

	private Room getRandomRoom() {
		int roomNumber = Randy.getRand(0, hardRooms.size() - 1);
		
		if (L.HARD_ROOM_NAME != null) {
			for (int i=0; i<hardRooms.size(); i++) {
				if (L.HARD_ROOM_NAME.equals(hardRooms.get(i).hardcodeName)) {
					roomNumber = i;
				}
			}
		}
		Room room = hardRooms.get(roomNumber);
		return room;
	}
	
	// check if the rect supplied is inside the map area and also clear of any
	// overlapping rooms.
	private boolean isRectClearOfRooms(Rect rect) {
		boolean result = rect.isInside(validRect);
		for (Room room : rooms) {
			if (room.area.overlaps(rect)) {
				result = false;
				break;
			}
		}
		return result;
	}

	private boolean isPointClearOfRooms(DungeonPosition position) {
		boolean result = validRect.isInside(position.x, position.y);
		for (Room room : rooms) {
			if (room.isInside(position)) {
				result = false;
				break;
			}
		}
		return result;
	}

	protected void drawSquigglyLine(DungeonPosition pos) {
		int x = pos.x;
		int y = pos.y;
		DungeonPosition testPoint = new DungeonPosition(0, 0);
		int dir = Randy.getRand(0, 3);
		int nx;
		int ny;
		
		for (int i = 0; i < ((height * 3) / 2); i++) {
			nx = x;
			ny = y;

			switch (dir) {
			case 0: // South
				if (y > 1)
					ny--;
				break;
			case 1: // North
				if (y < (height - 2))
					ny++;
				break;
			case 2:  // West
				if (x > 1)
					nx--;
				break;

			case 3: // EAST
				if (x < (width - 2))
					nx++;
				break;
			}

			// if (isLegal(nx, ny))
			testPoint.x = nx;
			testPoint.y = ny;
			if (isPointClearOfRooms(testPoint)) {
				x = nx;
				y = ny;
				location(x, y).clearLocation();
			}

			// will only change the direction when the direction is legal, i.e.
			// between 1-4
			int nDir = Randy.getRand(0, height * 2);

			if (nDir < 4) {
				dir = nDir;
			}
		}
	}

//	protected void drawSquigglyRoughTerrainLine(DungeonPosition pos, RoughTerrainType roughTerrainType, IDungeonEvents dungeonEvents,
//			IDungeonQuery dungeonQuery) {
//		int x = pos.x;
//		int y = pos.y;
//		int dir = Randy.getRand(0, 4);
//		int nx;
//		int ny;
//
//		for (int i = 0; i < (Randy.getRand(1, height)); i++) {
//			nx = x;
//			ny = y;
//
//			switch (dir) {
//			case 0:
//				if (y > 1)
//					ny--;
//				break;
//			case 1:
//				if (y < (height - 2))
//					ny++;
//				break;
//			case 2:
//				if (x > 1)
//					nx--;
//				break;
//
//			case 3:
//				if (x < (width - 2))
//					nx++;
//				break;
//			}
//
//			if (isOKForRoughTerrain(roughTerrainType, x, y)) {
//				location(x, y).setRoughTerrain(roughTerrainType, dungeonEvents, dungeonQuery);
//			}
//
//			x = nx;
//			y = ny;
//
//			// will only change the direction when the direction is legal, i.e.
//			int nDir = Randy.getRand(0, 6);
//
//			if (nDir < 4) {
//				dir = nDir;
//			}
//		}
//	}

	protected boolean isOKForRoughTerrain(RoughTerrainType roughTerrainType, int x, int y) {
		if (isLegal(x, y) == false) {
			return false;
		}

		Location location = location(x, y);
		
		if (location.isTotallyEmpty() == false) {
			return false;
		}

		if (location.position.equals(startPoint)) {
			return false;
		}

		if (roughTerrainType == RoughTerrainType.HOLE && location.isNearWall()) {
			return false;
		}
		
		return isPointClearOfRooms(location.getPosition());
	}

	public boolean inBounds(DungeonPosition p) {
		if (safeLocation(p.x, p.y) == solidRock) {
			return false;
		} else {
			return true;
		}
	}

	protected boolean isLegal(int x, int y) {
		if (x > (border - 1) && x < (width - border) && y > (border - 1)
				&& y < (height - border)) {
			return true;
		} else {
			return false;
		}
	}

	// if either index is -ve, it is solid stone.
	public Location location(int x, int y) {
		return location[x][y];
	}

	public Location location(DungeonPosition dungeonPosition) {
		return location(dungeonPosition.x, dungeonPosition.y);
	}

	public void setRoomMonsters(IDungeonEvents dungeonEvents) {
		for (Room room : rooms) {
			room.setMonsters(dungeonEvents);
		}
	}

	// Used by Location when calculating its type - assumes out of bounds
	// indexes are solid rock.
	public Location safeLocation(int x, int y) {
		if (x >= 0 && x < width && y >= 0 && y < height) {
			return location(x, y);
		} else {
			return solidRock;
		}
	}

	@Override
	public void onChangeToLocationInfo(UILocationInfoListener listener) {
		locationInfoListeners.add(listener);
	}

	public void persist(ObjectOutputStream out) throws IOException {
		// write width and height;
		out.writeInt(width);
		out.writeInt(height);
		out.writeObject(startPoint);
		out.writeObject(exitPoint);

		// write the locations
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				location(x, y).persistLocation(out);
			}
		}

		// write whats at the locations
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				location(x, y).persistThings(out);
			}
		}
	}

	public void addLight(Light light) {
		if (light.permanent) {
			if (permLights.contains(light) == false) {
				clearTempLighting();
				light.setMap(this);
				permLights.add(light);
				light.applyLight(); // permanent lights apply their effects once
									// only and it sticks.
				shineTempLighting();
			}
		} else {
			if (tempLights.contains(light) == false) {
				light.setMap(this);
				tempLights.add(light);
				lightingChanged();
			}
		}
	}

	public void moveLight(Light light, DungeonPosition newPosition) {
		if (tempLights.contains(light) == false) {
			tempLights.add(light);
			light.setPosition(newPosition);
			light.setMap(this);
		} else {
			// light.clearLight();
			light.setPosition(newPosition);
		}
	}

	// remove the effects of temp lighting, returning tile to its base level of
	// permanent lighting.
	public void removeLight(Light light) {
		// if (tempLights.contains(light)) {
		// light.clearLight();
		// tempLights.remove(light);
		// lightingChanged();
		// }
		if (light != null) {
			tempLights.remove(light);
			lightingChanged();
		}
	}

	// Adds temporary lighting if the light is somewhere that can be seen.
	public void shineTempLighting() {
		for (Light light : tempLights) {
			if (dungeonQuery.positionCouldBeSeen(light.position)) {
				light.applyLight();
			}
		}
	}

	// removes the effect of any temporary lighting, returning locations to base
	// permanent lighting levels
	public void clearTempLighting() {
		for (Location location : drawableLocations) {
			location.clearTint();
		}
		// for (Light light : tempLights) {
		// light.clearLight();
		// }
	}

	public void clearAllLighting() {

	}

	protected void addRoughTerrain(IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery) {
		int numberRoughTerrains = width;
		for (int i = 0; i < numberRoughTerrains; i++) {
			try {
				RoughTerrainType roughTerrainType = RoughTerrainType.getRandomType();
				DungeonPosition pos = getRandomPointNotInRooms(true);
				if (isOKForRoughTerrain(roughTerrainType, pos.x, pos.y)) {
					location(pos.x, pos.y).setRoughTerrain(roughTerrainType, dungeonEvents, dungeonQuery);
			}
			} catch (MapException e) {
			}
		}

		for (int i = 0; i < numberRoughTerrains/3; i++) {
			try {
				DungeonPosition pos = getRandomPointNotInRooms(true);
				if (isOKForRoughTerrain(RoughTerrainType.ROCKS, pos.x, pos.y)) {
					location(pos.x, pos.y).setRoughTerrain(RoughTerrainType.ROCKS, dungeonEvents, dungeonQuery);
			}
			} catch (MapException e) {
			}
		}
	}

	public void refreshLighting() {
		if (lightingChanged) {
			lightingChanged = false;
			// if (LOG) L.log("");
			clearTempLighting();
			shineTempLighting();
		}
	}

	public void lightingChanged() {
		lightingChanged = true;
	}

	public void dump(DungeonPosition[] roomPoints) {
		// debug print
		if (LOG) {
			for (int y = height - 1; y >= 0; y--) {
				for (int x = 0; x < width; x++) {
					boolean roomPoint = false;
					if (roomPoints != null) {
						for (int i=0; i<roomPoints.length; i++) {
							if (x == roomPoints[i].x && y == roomPoints[i].y) {
								roomPoint = true;
							}
						}
					}
					
					if (roomPoint) {
						System.out.print("++");
					} else if (location(x, y).tileType == TileType.ISLAND) {
						System.out.print("<>");
					} else if (location(x, y).isOpaque()) {
						if (location(x,y).torch == TorchType.NONE) {
							System.out.print("##");
						} else {
							System.out.print("**");
						}
					} else if (location(x, y).locationType == LocationType.EXIT) {
						System.out.print("[]");
					} else {
						String s = "  ";
						RoughTerrainType rtt = location(x,y).getRoughTerrain();
						if (rtt != null) {
							switch (location(x,y).getRoughTerrain()) {
								case HOLE:
									s = "()";
									break;
								case MUD:
									s = "mm";
									break;
								case BONES:
									s = "bb";
									break;
								case ROCKS:
									s = "rr";
									break;
								default:
									break;
							}
						}
						if (location(x,y).torch == TorchType.NONE) {
							System.out.print(s);
						} else {
							System.out.print("**");
						}
					}
					// if (LOG) L.log("location: %s - locationInfo: %s",
					// location(x,y), location(x,y).locationInfo);
				}
				System.out.println();
			}
		}

	}

	public String getStatueName() {
		return statueName;
	}

	public void onCreate() {
	}

	private DungeonPosition addMiniBossRoom() throws MapException {
		Room room = null;

		switch (level) {
		case 3:
			room = new Room(level3Map, level3Monsters, null, 1);
			break;
		case 4:
			statueName = "statue_dwarf";
			break;
		case 6:
			room = new Room(level6Map, level6Monsters, null, 1);
			break;
		case 7:
			statueName = "statue_zombie";
			break;
		case 9:
			room = new Room(level9Map, level9Monsters, null, 1);
			break;
		case 10:
			statueName = "statue";
			break;
		case 12:
			// room = new Room(level3Map, level3Monsters, 1);
			break;
		case 13:
			statueName = "statue";
			break;
		case 15:
			// room = new Room(level3Map, level3Monsters, 1);
			break;
		case 16:
			statueName = "statue";
			break;
		case 18:
			// room = new Room(level3Map, level3Monsters, 1);
			break;
		case 19:
			statueName = "statue";
			break;
		case 20:
			statueName = "statue";
			// room = new Room(level3Map, level3Monsters, 1);
			break;
		default:
			break;
		}

		if (room != null) {
			for (int i = 0; i < 200; i++) {
				DungeonPosition position = getRandomPointAnywhere(false);
				Rect area = new Rect(position, room.width, room.height);
				if (isRectClearOfRooms(area)) {
					room.setPosition(position, location);
					rooms.add(room);
					room.clearSpaces();
					return room.getEntrance();
				}
			}
		} else {
			return null;
		}

		throw new MapException();
	}
	
	private static String[] holeMap = {
		"      ",
		"  hh  ",
		" hhhh ",
		" hhhh ",
		"  hh  ",
		"      "};
	
	private static String[] holeMonsters = {};
	
	private static String[] mudMap = {
		"   mmmm ",
		" mmmmmmm",
		"mmmmm   ",
		"  mm    "};
	private static String[] mudMonsters = {};
	
	private static String[] boneMap = {
		" bbb      ",
		"b bb      ",
		"  bb bbb  ",
		" bbbb bb b",
		"bb bbbbbb ",
		"   bb     "};
	private static String[] boneMonsters = {};
	
	private static String[] rockMap = {
		"  Or rr r",
		"rrrrOOrr ",
		"  rOrrOrr",
		" rrrOr   ",
		"rr  rrr r"};
	private static String[] rockMonsters = {};
	
	private static String[] ravVertMap = {
		"   ",
		" h ",
		" h ",
		" h ",
		" h ",
		" h ",
		" h ",
		" h ",
		"   "};
	private static String[] ravVertMonsters = {};
	
	private static String[] ravHorMap = {
		"              ",
		" hhhhhhhhhhhh ",
		"              "};
	private static String[] ravHorMonsters = {};
	
	private static String[] gridMap = {
		"        ",
		"* * * * ",
		"        ",
		" * * * *",
		"        ",
		"* * * * ",
		"        ",
		" * * * *",
		"        "};
	private static String[] gridMonsters = {};
	
	private static String[] holegridMap = {
		  "h  h    h ", 
		  " h h  h h ", 
		  "  h h  h h", 
		  " h h  h h ", 
		  "  h  h h  "};
	private static String[] holegridMonsters = {};
	
	private static String[] bridgeMap = {
      "         ",
	  "  hhhhh  ",
	  " hhhhhhh ",
	  " hhhhhhh ",
	  "         ",    
	  " hhhhhhh ",
	  " hhhhhhh ",
	  "  hhhhh  ",
	  "         "};
	private static String[] bridgeMonsters = {};
	
	private static String[] crossMap = {
    "         ",
	" *** *** ",
	" *** *** ",
	" *** *** ",
	"    I    ",
	" *** *** ",
	" *** *** ",
	" *** *** ",
	"         "};
	private static String[] crossMonsters = {};
	
	private static String[] rocks1Map = {
		"rr",
		"r "};
	private static String[] rocks1Monsters = {};
	
	private static String[] rocks2Map = {
		"rr",
		" r"};
	private static String[] rocks2Monsters = {};
	
	private static String[] rocks3Map = {
		"r ",
		"rr"};
	private static String[] rocks3Monsters = {};
	
	private static String[] rocks4Map = {
		" r",
		"rr"};
	private static String[] rocks4Monsters = {};
	
	private static String[] rocks5Map = {
		"rr"};
	private static String[] rocks5Monsters = {};
	
	private static String[] rocks6Map = {
		"r",
		"r"};
	private static String[] rocks6Monsters = {};
	
	private static String[] rocks7Map = {
		"rr",
		"rr"};
	private static String[] rocks7Monsters = {};
	
	
	
	private static String[] holes1Map = {
		"    ",
		" hh ",
		" h  ",
		"    "};
	private static String[] holes1Monsters = {};
	
	private static String[] holes2Map = {
		"    ",
		" hh ",
		"  h ",
		"    "};
	private static String[] holes2Monsters = {};
	
	private static String[] holes3Map = {
		"    ",
		" h  ",
		" hh ",
		"    "};
	private static String[] holes3Monsters = {};
	
	private static String[] holes4Map = {
		"    ",
		"  h ",
		" hh ",
		"    "};
	private static String[] holes4Monsters = {};
	
	private static String[] holes5Map = {
		"    ",
		" hh ",
		"    "};
	private static String[] holes5Monsters = {};
	
	private static String[] holes6Map = {
		"   ",
		" h ",
		" h ",
		"   "};
	private static String[] holes6Monsters = {};
	
	private static String[] holes7Map = {
		"    ",
		" hh ",
		" hh ",
		"    "};
	private static String[] holes7Monsters = {};


	
	private static String[] mud1Map = {
		"mm",
		"m "};
	private static String[] mud1Monsters = {};
	
	private static String[] mud2Map = {
		"mm",
		" m"};
	private static String[] mud2Monsters = {};
	
	private static String[] mud3Map = {
		"m ",
		"mm"};
	private static String[] mud3Monsters = {};
	
	private static String[] mud4Map = {
		" m",
		"mm"};
	private static String[] mud4Monsters = {};
	
	private static String[] mud5Map = {
		"mm"};
	private static String[] mud5Monsters = {};
	
	private static String[] mud6Map = {
		"m",
		"m"};
	private static String[] mud6Monsters = {};
	
	private static String[] mud7Map = {
		"mm",
		"mm"};
	private static String[] mud7Monsters = {};


	
	
	private static String[] bones1Map = {
		"bb",
		"b "};
	private static String[] bones1Monsters = {};
	
	private static String[] bones2Map = {
		"bb",
		" b"};
	private static String[] bones2Monsters = {};
	
	private static String[] bones3Map = {
		"b ",
		"bb"};
	private static String[] bones3Monsters = {};
	
	private static String[] bones4Map = {
		" b",
		"bb"};
	private static String[] bones4Monsters = {};
	
	private static String[] bones5Map = {
		"bb"};
	private static String[] bones5Monsters = {};
	
	private static String[] bones6Map = {
		"b",
		"b"};
	private static String[] bones6Monsters = {};
	
	private static String[] bones7Map = {
		"bb",
		"bb"};
	private static String[] bones7Monsters = {};
	
	
	
	private static ArrayList<Room> hardRooms = Map.makeHardRooms();
	
	private static ArrayList<Room> makeHardRooms() {
		
		ArrayList<Room> theRooms = new ArrayList<Room>();
		
		theRooms.add(new Room(holeMap, holeMonsters, "holeMap", 0));
		theRooms.add(new Room(mudMap, mudMonsters, "mudMap", 0));
		theRooms.add(new Room(ravVertMap, ravVertMonsters, "ravVertMap", 0));
		theRooms.add(new Room(ravHorMap, ravHorMonsters, "ravHorMap", 0));
		theRooms.add(new Room(gridMap, gridMonsters, "gridMap", 0));
		theRooms.add(new Room(boneMap, boneMonsters,  "boneMap", 0));
		theRooms.add(new Room(rockMap, rockMonsters,  "rockMap", 0));
		theRooms.add(new Room(holegridMap, holegridMonsters,  "holegridMap", 0));
		theRooms.add(new Room(bridgeMap, bridgeMonsters, "bridgeMap", 0));
		theRooms.add(new Room(crossMap, crossMonsters, "crossMap", 0)); 
		
		theRooms.add(new Room(rocks1Map, rocks1Monsters,  "rocks1Map", 0));
		theRooms.add(new Room(rocks2Map, rocks2Monsters,  "rocks2Map", 0));
		theRooms.add(new Room(rocks3Map, rocks3Monsters,  "rocks3Map", 0));
		theRooms.add(new Room(rocks4Map, rocks4Monsters,  "rocks4Map", 0));
		theRooms.add(new Room(rocks5Map, rocks5Monsters,  "rocks5Map", 0));
		theRooms.add(new Room(rocks6Map, rocks6Monsters,  "rocks6Map", 0));
		theRooms.add(new Room(rocks7Map, rocks7Monsters,  "rocks7Map", 0));
		
		theRooms.add(new Room(bones1Map, bones1Monsters,  "bones1Map", 0));
		theRooms.add(new Room(bones2Map, bones2Monsters,  "bones2Map", 0));
		theRooms.add(new Room(bones3Map, bones3Monsters,  "bones3Map", 0));
		theRooms.add(new Room(bones4Map, bones4Monsters,  "bones4Map", 0));
		theRooms.add(new Room(bones5Map, bones5Monsters,  "bones5Map", 0));
		theRooms.add(new Room(bones6Map, bones6Monsters,  "bones6Map", 0));
		theRooms.add(new Room(bones7Map, bones7Monsters,  "bones7Map", 0));
		
		theRooms.add(new Room(mud1Map, mud1Monsters,  "mud1Map", 0));
		theRooms.add(new Room(mud2Map, mud2Monsters,  "mud2Map", 0));
		theRooms.add(new Room(mud3Map, mud3Monsters,  "mud3Map", 0));
		theRooms.add(new Room(mud4Map, mud4Monsters,  "mud4Map", 0));
		theRooms.add(new Room(mud5Map, mud5Monsters,  "mud5Map", 0));
		theRooms.add(new Room(mud6Map, mud6Monsters,  "mud6Map", 0));
		theRooms.add(new Room(mud7Map, mud7Monsters,  "mud7Map", 0));
		
		theRooms.add(new Room(holes1Map, holes1Monsters,  "holes1Map", 0));
		theRooms.add(new Room(holes2Map, holes2Monsters,  "holes2Map", 0));
		theRooms.add(new Room(holes3Map, holes3Monsters,  "holes3Map", 0));
		theRooms.add(new Room(holes4Map, holes4Monsters,  "holes4Map", 0));
		theRooms.add(new Room(holes5Map, holes5Monsters,  "holes5Map", 0));
		theRooms.add(new Room(holes6Map, holes6Monsters,  "holes6Map", 0));
		theRooms.add(new Room(holes7Map, holes7Monsters,  "holes7Map", 0));

		
		return theRooms;
	}
	
	// mini boss rooms
	private static String[] level3Map = {
		"*******",
		"*IhXhI*",
		"* h0h *",
		"* h h *",
		"*4 5 6*",
		"* hhh *",
		"*1 2 3*",
		"***@***"};
	private static String[] level3Monsters = {"crazed priest",
		"crazed minion", "crazed minion", "crazed minion", "crazed minion", "crazed minion", "crazed minion"};
	
	private static String[] level6Map = {
		"*******",
		"*   24*",
		"*  IrO*",
		"@ 560X*",
		"*  IrO*",
		"*   13*",
	    "*******"};
	private static String[] level6Monsters = {"dwarf king", "dwarf","dwarf","dwarf","dwarf","dwarf","dwarf"};
	
	private static String[] level9Map = {
		"*************",
		"*           @",
		"* ***********",
		"* *   5     *",
		"*9* ******* *",
		"* * * 20 3* *",
		"* * *1X7t *6*",
		"* * ***** * *",
		"* * 4     * *",
		"* ********* *",
		"*  8        *",
		"*************"};
	private static String[] level9Monsters = {"zombie giant", "zombie","zombie","zombie","zombie","zombie","zombie","zombie","zombie","zombie"};
}
