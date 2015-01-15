package com.dbash.models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Vector;

import com.dbash.models.Location.RoughTerrainType;
import com.dbash.util.L;
import com.dbash.util.Randy;

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
	
	public Location[][] location;
	public int width;
	public int height;
	public DungeonPosition startPoint;
	public DungeonPosition exitPoint;
	public DungeonPosition[] roomPoints;
	protected UIInfoListenerBag retainFocusBag;
	protected ArrayList<Light> tempLights;
	protected ArrayList<Light> permLights;
	
	private Vector<UILocationInfoListener> locationInfoListeners;
	private Location solidRock = new Location();
	private boolean lightingChanged;
	private final int border = 2; // how thick the enclosing rock wall is
	
	@SuppressWarnings("serial")
	public class MapException extends Exception {
	}
	
	public Map(int level, IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery) {
		boolean dungeonNotCompleted = true;
		while (dungeonNotCompleted) {
			try {
				retainFocusBag = new UIInfoListenerBag();
				locationInfoListeners = new Vector<UILocationInfoListener>();
				width = 8 + level + border*2 - 2;
				height = width;
				location = new Location[width][height];
				// initialize array of locations - by default will be WALLS.
				for (int x=0; x<width; x++) {
					for (int y=0; y< height; y++) {
						location[x][y] = new Location(this, x, y);
					}
				}
				
				startPoint = getRandomPoint(false);
				location(startPoint).clearLocation();
				drawSquigglyLine(startPoint);

				for (int i = 0; i < ((height * 2) / 3); i++) {
					drawSquigglyLine(getRandomPoint(true));
				}
				
				// now draw some rooms based on where the squigley lines have cleared space.
				roomPoints = new DungeonPosition[height / 8]; 

				for (int i = 0; i < roomPoints.length; i++) {
					// calculate where to draw the rooms before starting to draw them
					roomPoints[i] = getRandomPoint(true, 2+ border);
				}

				for (int i = 0; i < roomPoints.length; i++)
					// otherwise all the rooms will be most likely drawn together
					drawRoom(roomPoints[i]);
				
				setStartAndExitPoints();
				
				// Now make a preliminary pass to determine Tile types
				for (int x=0; x<width; x++) {
					for (int y=0; y< height; y++) {
						location(x,y).setTileType();
					}
				}
				
				// Then make secondary pass to determine tile names
				for (int x=0; x<width; x++) {
					for (int y=0; y< height; y++) {
						location(x,y).setTileName();
					}
				}
				
				setupLighting();
				// Then make the last pass to determine post process the whole map with appropriae tilenames
				for (int x=0; x<width; x++) {
					for (int y=0; y< height; y++) {
						location(x,y).doPostMapGenerationPrcessing();
					}
				}
				
				addExitLight();
				addRoughTerrain(dungeonEvents, dungeonQuery);
				dungeonNotCompleted = false;
			} catch (MapException e) {
				dungeonNotCompleted = true;
				if (LOG) L.log("TRYING AGAIN!");
			}
		}
	}
	
	public Map (ObjectInputStream in, IDungeonControl dungeon, AllCreatures allCreatures, IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery) throws IOException, ClassNotFoundException {
		retainFocusBag = new UIInfoListenerBag();
		locationInfoListeners = new Vector<UILocationInfoListener>();
		width = in.readInt();
		height = in.readInt();
		startPoint = (DungeonPosition) in.readObject();
		exitPoint = (DungeonPosition) in.readObject();
		location = new Location[width][height];
		
		setupLighting();
		
		// read the locations
		for (int x=0; x<width; x++) {
			for (int y=0; y< height; y++) {
				location[x][y] = new Location(in, this, allCreatures, dungeonEvents, dungeonQuery);
			}
		}
		addExitLight();
	}
	
	public void load(ObjectInputStream in, IDungeonControl dungeon, AllCreatures allCreatures, IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery) throws IOException, ClassNotFoundException {
		// read the locations
		for (int x=0; x<width; x++) {
			for (int y=0; y< height; y++) {
				location[x][y].load(in, this, allCreatures, dungeonEvents, dungeonQuery);
			}
		}
	}
	
	protected void setupLighting() {
		tempLights = new ArrayList<Light>();
		permLights = new ArrayList<Light>();
	}
	
	protected void addExitLight() {
		Light exitLight = new Light(exitPoint, 0, 1f, true);  // low level light - permanent
		addLight(exitLight);
	}
	
	// Called by Locations - the dungeonmap is one such subscriber and it posts the locationInfo to the 
	// locationPresenter that it belongs to.
	public void alertToVisualChangeAtLocation(Location location)
	{
		if (LOG) L.log("");
		for (UILocationInfoListener listeners : locationInfoListeners) {
			listeners.locationInfoChanged(location);
		}
	}
	
	public Map() {
		location = new Location[1][1];
		location[0][0] = new Location(this, 0, 0);
	}

	public DungeonPosition getRandomPoint(boolean isFloorRequired) throws MapException {
		return getRandomPoint(isFloorRequired, border);
	}
	
	/**
	 * Returns a random point, or null if it timed out.
	 */
	public DungeonPosition getRandomPoint(boolean isFloorRequired, int minDistanceToEdge) throws MapException {

		DungeonPosition l = new DungeonPosition();
		int tries = 0;
		boolean found = false;
		while (!found && tries<500) {
			tries++;
            l.x = Randy.getRand(minDistanceToEdge, width - 1 - minDistanceToEdge);
            l.y = Randy.getRand(minDistanceToEdge, height - 1 - minDistanceToEdge);
            if (isFloorRequired)
				found = !location(l).isOpaque();
			else
				found = true;
		}
		
		if (tries < 500) {
			return l;
		} else {
			throw new MapException();
		}
	}

	protected void setStartAndExitPoints() throws MapException {
		int tries = 0; // just in case it is impossible.
		do
		{
			startPoint = getRandomPoint(true);
			exitPoint = getRandomPoint(true);
			tries++;
		} while (((Math.abs(startPoint.x - exitPoint.x) + Math.abs(startPoint.y - exitPoint.y)) < (height / 2)) && (tries < 200));
		
		if (tries >= 200) {
			throw new MapException();
		}
		// tell the exit Location 
		location(exitPoint).setAsExit();
	}
	
	public void drawRoom(DungeonPosition dungeonLocation) {
		int roomW = Randy.getRand(5, height / 5);
		int roomH = Randy.getRand(5, height / 5);
		DungeonPosition min = new DungeonPosition(dungeonLocation.x - roomW / 2, dungeonLocation.y - roomH / 2);
		if (min.x < border) min.x = border;
		if (min.y < border) min.y = border;
		DungeonPosition max = new DungeonPosition(min.x + roomW, min.y + roomH);
		if (max.x >= width-(border-1)) max.x = width-(border+1);
		if (max.y >= height-(border-1)) max.y = height-(border+1);
		
		drawRectangle(min, max);
	}
	
	public void drawRectangle(DungeonPosition min, DungeonPosition max) {
		for (int x = min.x; x <= max.x; x++) {
			for (int y = min.y; y <= max.y; y++) {
				location(x,y).clearLocation();
			}
		}
	}
	
	private void drawSquigglyLine(DungeonPosition pos) {
		int x = pos.x;
		int y = pos.y;
		int dir = Randy.getRand(0, 4);
		int nx;
		int ny;

		for (int i = 0; i < ((height * 3) / 2); i++) {
			nx = x;
			ny = y;

			switch (dir) {
				case 0:
					if (y > 1)
						ny--;
					break;
				case 1:
					if (y < (height - 2))
						ny++;
					break;
				case 2:
					if (x > 1)
						nx--;
					break;

				case 3:
					if (x < (width - 2))
						nx++;
					break;
			}

			if (isLegal(nx, ny))
			{
				x = nx;
				y = ny;
				location(x,y).clearLocation();
			}

			// will only change the direction when the direction is legal, i.e.
			// between 1-4
			int nDir = Randy.getRand(0, height * 2);

			if (nDir < 5) {
				dir = nDir;
			}
		}
	}
	
	private void drawSquigglyRoughTerrainLine(DungeonPosition pos, IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery) {
		int x = pos.x;
		int y = pos.y;
		int dir = Randy.getRand(0, 4);
		int nx;
		int ny;
		RoughTerrainType roughTerrainType = RoughTerrainType.getRandomType();

		for (int i = 0; i < (Randy.getRand(1, height)); i++) {
			nx = x;
			ny = y;

			switch (dir) {
				case 0:
					if (y > 1)
						ny--;
					break;
				case 1:
					if (y < (height - 2))
						ny++;
					break;
				case 2:
					if (x > 1)
						nx--;
					break;

				case 3:
					if (x < (width - 2))
						nx++;
					break;
			}
			
			if (isOKForRoughTerrain(roughTerrainType, x, y)) {
				location(x,y).setRoughTerrain(roughTerrainType, dungeonEvents, dungeonQuery);
			}
			
			x = nx;
			y = ny;

			// will only change the direction when the direction is legal, i.e.
			// between 1-4
			int nDir = Randy.getRand(0, 6);

			if (nDir < 5) {
				dir = nDir;
			}
		}
	}
	
	private boolean isOKForRoughTerrain(RoughTerrainType roughTerrainType, int x, int y) {
		if (isLegal(x, y) == false) {
			return false;
		}
		
		Location location = location(x,y);
		
		if (location.isTotallyEmpty() == false) {
			return false;
		}
		
		if (location.position.equals(startPoint)) {
			return false;
		}

		if (roughTerrainType == RoughTerrainType.HOLE && location.isNearWall()) {
			return false;
		}
		
		return true;
	}
	
	public boolean inBounds(DungeonPosition p) {
		if (safeLocation(p.x, p.y) == solidRock) {
			return false;
		} else {
			return true;
		}
	}
	
	private boolean isLegal(int x, int y) {
		if (x>(border-1) && x<(width-border) && y>(border-1) && y<(height-border)) {
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
	
	// Used by Location when calculating its type - assumes out of bounds indexes are solid rock.
	public Location safeLocation(int x, int y) {
		if (x>=0 && x<width && y>=0 && y<height) {
			return location(x,y);
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
		for (int x=0; x<width; x++) {
			for (int y=0; y< height; y++) {
				location(x,y).persistLocation(out);
			}
		}
		
		// write whats at the locations
		for (int x=0; x<width; x++) {
			for (int y=0; y< height; y++) {
				location(x,y).persistThings(out);
			}
		}
	}
	
	public void addLight(Light light) {
		if (light.permanent){
			if (permLights.contains(light) == false) {
				clearTempLighting();
				light.setMap(this);
				permLights.add(light);
				light.applyLight();  // permanent lights apply their effects once only and it sticks.
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
			light.setMap(this);
		} 
		light.setPosition(newPosition);
		lightingChanged();
	}
	
	// remove the effects of temp lighting, returning tile to its base level of permanent lighting.
	public void removeLight(Light light) {
		if (tempLights.contains(light)) {
			light.clearLight();
			tempLights.remove(light);
			lightingChanged();
		}
	}
	
	// Adds temporary lighting
	public void shineTempLighting() {
		for (Light light : tempLights) {
			light.applyLight();
		}
	}
	
	// removes the effect of any temporary lighting, returning locations to base permanent lighting levels
	public void clearTempLighting() {
		for (Light light : tempLights) {
			light.clearLight();
		}
	}
	
	protected void addRoughTerrain(IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery) {
		int numberRoughLines = Randy.getRand(width/5,  width/3);
		for (int i=0; i < numberRoughLines; i++) {
			try {
				drawSquigglyRoughTerrainLine(getRandomPoint(true), dungeonEvents, dungeonQuery);
			} catch (MapException e) {

			}
		}
	}
	
	public void refreshLighting() {
		if (lightingChanged) {
			lightingChanged = false;
			if (LOG) L.log("");
			clearTempLighting();
			shineTempLighting();
		}
	}
	
	public void lightingChanged() {
		lightingChanged = true;
	}
//	public void dump() {
//		// debug print
//		for (int y=width-1; y>=0; y--) {
//			for (int x=0; x< height; x++) {
//				if (location(x,y).isOpaque()) if (LOG) Logger.log('#'); else if (LOG) Logger.log(' ');
//			}
//			if (LOG) Logger.log();
//		}
//		if (LOG) Logger.log();
//		if (LOG) Logger.log();
//		if (LOG) Logger.log();
//	}
}
