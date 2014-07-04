package com.dbash.models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Vector;

import com.dbash.util.Randy;



// This class is basically a two dimension array of 'locations' and the code to build a maze  in it.
// Each location has all the info about that location, including a spritename (TileName) and any creatures or items
// currently at that location.
// Map doesnt know about Monsters, Characters etc...  To move creatures and items around in it, interact with the Locations
// directly.  Those changes to Locations will emit observable Presentation events
public class Map implements IPresenterMap {
	public Location[][] location;
	public int width;
	public int height;
	public DungeonPosition startPoint;
	public DungeonPosition exitPoint;
	public DungeonPosition[] roomPoints;
	protected UIInfoListenerBag retainFocusBag;
	protected ArrayList<Light> lights;
	
	private Vector<UILocationInfoListener> locationInfoListeners;
	private Location solidRock = new Location();
	
	private final int border = 2; // how thick the enclosing rock wall is
	
	public Map(int level) {
		retainFocusBag = new UIInfoListenerBag();
		locationInfoListeners = new Vector<UILocationInfoListener>();
		width = 12 + (level * 2) + border*2 - 2;
		height = width;
		location = new Location[width][height];
		lights = new ArrayList<Light>();
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
		
		// Now determine tiles where characters will dorp in, and exit
		int tries = 0; // just in case it is impossible.

		do
		{
			startPoint = getRandomPoint(true);
			exitPoint = getRandomPoint(true);
			tries++;
		} while (((Math.abs(startPoint.x - exitPoint.x) + Math.abs(startPoint.y - exitPoint.y)) < (height / 2)) && (tries < 200));
		
		// tell the exit Location 
		location(exitPoint).setAsExit();
		
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
	}
	
	public Map (ObjectInputStream in, IDungeonControl dungeon, AllCreatures allCreatures, IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery) throws IOException, ClassNotFoundException {
		retainFocusBag = new UIInfoListenerBag();
		locationInfoListeners = new Vector<UILocationInfoListener>();
		width = in.readInt();
		height = in.readInt();
		startPoint = (DungeonPosition) in.readObject();
		exitPoint = (DungeonPosition) in.readObject();
		location = new Location[width][height];
		// read the locations
		for (int x=0; x<width; x++) {
			for (int y=0; y< height; y++) {
				location[x][y] = new Location(in, this, allCreatures, dungeonEvents, dungeonQuery);
			}
		}
	}
	
	public void load(ObjectInputStream in, IDungeonControl dungeon, AllCreatures allCreatures, IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery) throws IOException, ClassNotFoundException {
		// read the locations
		for (int x=0; x<width; x++) {
			for (int y=0; y< height; y++) {
				location[x][y].load(in, this, allCreatures, dungeonEvents, dungeonQuery);
			}
		}
	}
	
	// Called by Locations - the dungeonmap is one such subscriber and it posts the locationInfo to the 
	// locationPresenter that it belongs to.
	public void alertToVisualChangeAtLocation(Location location)
	{
		for (UILocationInfoListener listeners : locationInfoListeners) {
			listeners.locationInfoChanged(location);
		}
	}
	
	public Map() {
		location = new Location[1][1];
		location[0][0] = new Location(this, 0, 0);
	}

	public DungeonPosition getRandomPoint(boolean isFloorRequired) {
		return getRandomPoint(isFloorRequired, border);
	}
	
	public DungeonPosition getRandomPoint(boolean isFloorRequired, int minDistanceToEdge) {

		DungeonPosition l = new DungeonPosition();

		boolean found = false;
		while (!found) {
            l.x = Randy.getRand(minDistanceToEdge, width - 1 - minDistanceToEdge);
            l.y = Randy.getRand(minDistanceToEdge, height - 1 - minDistanceToEdge);
            if (isFloorRequired)
				found = !location(l).isOpaque();
			else
				found = true;
		}
		
		return l;
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
		clearLighting();
		light.setMap(this);
		lights.add(light);
		shineLighting();
	}
	
	public void moveLight(Light light, DungeonPosition newPosition) {
		clearLighting();
		light.setPosition(newPosition);
		shineLighting();
	}
	
	public void removeLight(Light light) {
		clearLighting();
		lights.remove(light);
		shineLighting();
	}
	
	protected void shineLighting() {
		for (Light light : lights) {
			light.applyLight();
		}
	}
	
	protected void clearLighting() {
		for (Light light : lights) {
			light.clearLight();
		}
	}
	
//	public void dump() {
//		// debug print
//		for (int y=width-1; y>=0; y--) {
//			for (int x=0; x< height; x++) {
//				if (location(x,y).isOpaque()) System.out.print('#'); else System.out.print(' ');
//			}
//			System.out.println();
//		}
//		System.out.println();
//		System.out.println();
//		System.out.println();
//	}
}
