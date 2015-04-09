package com.dbash.models;

import java.util.Vector;

import com.dbash.models.Location.RoughTerrainType;


public class TutorialMap extends Map {

	private String[] tutorialMap = {
			"*************************************************************************************" ,
			"*************ttt*********************************************************************" ,
			"*****     r       ********                                   ********        X    ***" ,
			"*****             ********             *********             ********             ***" ,
			"*****  S          ********             *********             ********             ***" ,
			"*****         m   ********             *********             ********             ***" ,
			"*****     h                0           *********             ********             ***" ,
			"*****     b       ********             *********         1   ********             ***" ,
			"*****             ********             *********                                  ***" ,
			"*************************************************************************************" ,
			"*************************************************************************************" 
		};
	
	private String[] monsters = {
			"halfling",
			"orc"
	};
	
	IDungeonEvents dungeonEvents;
	
	public TutorialMap(IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery) {

		this.dungeonQuery = dungeonQuery;
		this.dungeonEvents = dungeonEvents;
		
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
			for (int y=0; y< height; y++) {
				char c = tutorialMap[y].charAt(x);
				if (c != '*' && c != 't') {
					location[x][height-1-y].clearLocation();
				}
			}
		}

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
		
		setTorches();
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
					return new DungeonPosition(x,height-1 - y);
				}
			}
		}
		
		return null;
	}
	
	protected void setIslands() {
		for (int x=0; x<width; x++) {
			for (int y=0; y< height; y++) {
				if (tutorialMap[y].charAt(x) == 'i') {
					location[x][height-1-y].setAsIsland();
				}
			}
		}
	}
	
	protected void setTorches() {
		for (int x=0; x<width; x++) {
			for (int y=0; y< height; y++) {
				if (tutorialMap[y].charAt(x) == 't') {
					location[x][height-1-y].createTorchAt();
				}
			}
		}
	}
	
	protected void addRoughTerrain(IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery) {
		RoughTerrainType tt;
		for (int x=0; x<width; x++) {
			for (int y=0; y< height; y++) {
				
				tt = null;
				switch (tutorialMap[y].charAt(x)) {
				case 'r':
					tt = RoughTerrainType.ROCKS;
					break;
				case 'h':
					tt = RoughTerrainType.HOLE;
					break;
				case 'm':
					tt = RoughTerrainType.MUD;
					break;
				case 'b':
					tt = RoughTerrainType.BONES;
					break;
				}
				
				if (tt != null) {
					location(x, height - 1 - y).setRoughTerrain(tt, dungeonEvents, dungeonQuery);
				}
			}
		}
		
	}
	
	public void onCreate() {
		int numMonsters = monsters.length;
		for (Integer i=0; i<numMonsters; i++) {
			String monster = monsters[i];
			DungeonPosition dungeonPosition = getPosiOfChar(i.toString().charAt(0));
			dungeonEvents.addMonsterToMap(monster, dungeonPosition);
		}
	}
}
