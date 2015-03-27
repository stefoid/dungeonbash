package com.dbash.models;

import java.util.Vector;

import com.dbash.models.Map.MapException;
import com.dbash.util.L;

public class TutorialMap extends Map {

	private String[] tutorialMap = {
			"*************************************************************************************************" ,
			"*************************************************************************************************" ,
			"*****                ********                *********                ********                ***" ,
			"*****                ********                                         ********                ***" ,
			"*****                ********                *********                ********                ***" ,
			"*****                ********                *********                ********                ***" ,
			"*****                ********                *********                ********                ***" ,
			"*****                                        *********                ********                ***" ,
			"*****                ********                *********                ********                ***" ,
			"*****                ********                *********                                        ***" ,
			"*****                ********                *********                ********                ***" ,
			"*************************************************************************************************" ,
			"*************************************************************************************************" 
		};
	
	public TutorialMap(IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery) {
		boolean dungeonNotCompleted = true;
		this.dungeonQuery = dungeonQuery;
		while (dungeonNotCompleted) {
			try {
				retainFocusBag = new UIInfoListenerBag();
				locationInfoListeners = new Vector<UILocationInfoListener>();
				width = 8 + level + border*2 - 2;
				height = width;
				this.level = 1;
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
				
				setIslands();
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
				dungeonNotCompleted = false;
				
				if (LOG) dump();
				
			} catch (MapException e) {
				dungeonNotCompleted = true;
				if (LOG) L.log("TRYING AGAIN!");
			}
		}
	}
}
