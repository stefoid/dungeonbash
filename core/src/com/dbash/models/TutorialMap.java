package com.dbash.models;

import java.util.Vector;

import com.dbash.models.Location.RoughTerrainType;
import com.dbash.util.L;


public class TutorialMap extends Map {

	private String[] tutorialMap = {
"*****************************************************************************************************" ,
"*********t*******************t*******t***t***t***t*******************t*******************************" ,
"*****             ********            r bb mm r              ********                rrrr         ***" ,
"*****             ********    hhhh     *********   bbb       ********             ***   rrrrrr    ***" ,
"****t   S         t*******       h     *********     b       t*******             ***  rr         t**" ,
"*****             ***t****       h     ********t             ********      2      ***   rrrrrr    ***" ,
"*****                      0     h     *********       mmm   ********             **t     rr    3 ***" ,
"*****     b       ********             t********   mmmmm 1   *t******             ***      r      ***" ,
"****t             ********             *********                                  ***            X***" ,
"*****************************************************************************************************" ,
"*****************************************************************************************************"};
	
	private String[] tutorialMonsters = {
			"dummy",
			"dummy",
			"dummy",
			"warrior"};
	
	Room room;
	IDungeonEvents dungeonEvents;
	
	public TutorialMap(IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery) {

		this.dungeonQuery = dungeonQuery;
		this.dungeonEvents = dungeonEvents;
		
		String prefix = L.STRING_PREFIX;
		
		if (L.json.has("walls")) {
			prefix = L.json.getString("walls");
		}
		
		retainFocusBag = new UIInfoListenerBag();
		locationInfoListeners = new Vector<UILocationInfoListener>();
		width = tutorialMap[0].length();
		height = tutorialMap.length;
		
		this.level = 1;
		location = new Location[width+1][height+1];
		
		// initialize array of locations - by default will be WALLS.
		for (int x=0; x<width; x++) {
			for (int y=0; y< height; y++) {
				location[x][y] = new Location(this, x, y, prefix);
			}
		}
		
		room = new Room(tutorialMap, tutorialMonsters, null, 0);
		room.setPosition(new DungeonPosition(width/2,height/2), location);
		room.clearSpaces();
		room.setIslands();
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
				location(x,y).updatePresenter();
				if (location(x,y).isDrawable()) {
					drawableLocations.add(location(x,y));
				}
			}
		}
		
		room.setTorches();
		addExitLight();
		
		room.addRoughTerrain(dungeonEvents, dungeonQuery);
		
		if (LOG) dump(null);
	}
	
	protected void setStartAndExitPoints() {
		startPoint = room.getStartPosition();
		exitPoint = room.getExitPosition();
		// tell the exit Location 
		location(exitPoint).setAsExit();
	}
	
	public void onCreate() {
		room.setMonsters(dungeonEvents);
	}
}
