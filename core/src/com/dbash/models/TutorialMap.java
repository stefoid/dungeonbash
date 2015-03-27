package com.dbash.models;

import java.util.Vector;

import com.dbash.models.Location.RoughTerrainType;
import com.dbash.models.Map.MapException;
import com.dbash.util.L;
import com.dbash.util.Randy;

public class TutorialMap extends Map {

	private String[] tutorialMap = {
			"*************************************************************************************************" ,
			"*************************************************************************************************" ,
			"*****                ********                *********                ********                ***" ,
			"*****                ********                                         ********           X    ***" ,
			"*****                ********                *********                ********                ***" ,
			"*****  S             ********                *********                ********                ***" ,
			"*****                ********                *********                ********                ***" ,
			"*****                                        *********                ********                ***" ,
			"*****                ********                *********                ********                ***" ,
			"*****                ********                *********                                        ***" ,
			"*****                ********                *********                ********                ***" ,
			"*************************************************************************************************" ,
			"*************************************************************************************************" 
		};
	
	public TutorialMap(IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery) {

		this.dungeonQuery = dungeonQuery;

		retainFocusBag = new UIInfoListenerBag();
		locationInfoListeners = new Vector<UILocationInfoListener>();
		
		width = tutorialMap[0].length();
		height = tutorialMap.length;
		
		this.level = 1;
		location = new Location[width][height];
		// initialize array of locations - by default will be WALLS.
		for (int x=0; x<width; x++) {
			for (int y=0; y< height; y++) {
				location[x][y] = new Location(this, x, y);
			}
		}
		
		// clear the spaces
		for (int x=0; x<width; x++) {
			for (int y=height-1; y>=0; y--) {
				if (tutorialMap[y].charAt(x) != '*') {
					location[x][y].clearLocation();
				}
			}
		}

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
				if (location(x,y).isDrawable()) {
					drawableLocations.add(location(x,y));
				}
			}
		}
		
		addExitLight();
		
		addRoughTerrain(dungeonEvents, dungeonQuery);
		
		if (LOG) dump();
	}
	
	protected void setStartAndExitPoints() {
		startPoint = getPosiOfChar('S');
		exitPoint = getPosiOfChar('X');;
		// tell the exit Location 
		location(exitPoint).setAsExit();
	}
	
	private DungeonPosition getPosiOfChar(char c) {
		for (int x=0; x<width; x++) {
			for (int y=0; y< height; y++) {
				if (tutorialMap[y].charAt(x) == c) {
					return new DungeonPosition(x,y);
				}
			}
		}
		
		return null;
	}
	
	protected void addRoughTerrain(IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery) {
		int numberRoughLines = Randy.getRand(width/5,  width/3);
		for (int i=0; i < numberRoughLines; i++) {
			try {
				drawSquigglyRoughTerrainLine(getRandomPoint(true), RoughTerrainType.getRandomType(), dungeonEvents, dungeonQuery);
			} catch (MapException e) {

			}
		}
		
		// now add a few more rock terrain for stealth in wide open spaces.
		int numberRockTerrain = level - 1;
		for (int i=0; i < numberRockTerrain; i++) {
			Location l = getWideSpaceLocation();
			if (l != null) {
				if (LOG) L.log("PLACING EXTRA ROCKS: %s", i);
				drawSquigglyRoughTerrainLine(l.position, RoughTerrainType.ROCKS, dungeonEvents, dungeonQuery);
			}
		}
	}
}
