package com.dbash.models;

import com.dbash.models.Location.IslandType;
import com.dbash.models.Location.RoughTerrainType;
import com.dbash.util.Rect;

public class Room {

	private String[] charMap;;
	private String[] monsters;
	
	private int mx;
	private int my;
	private Location[][] location;
	private int edgeLimit;
	
	public Rect area;
	private Rect insideArea;
	public int width;
	public int height;
	public String hardcodeName;
	
	public Room(String[] charMap, String[] monsters, String hardcodeName, int edgeLimit) {
		this.charMap = charMap;
		this.monsters = monsters;
		width = charMap[0].length();
		height = charMap.length;
		this.edgeLimit = edgeLimit;
		this.hardcodeName = hardcodeName;
	}
	
	public Room(Room room) {
		this.charMap = room.charMap;
		this.monsters = room.monsters;
		this.edgeLimit = room.edgeLimit;
		this.width = room.width;
		this.height = room.height;
		this.hardcodeName = room.hardcodeName;
	}
	
	public void setPosition(DungeonPosition position, Location[][] location) {
		this.location = location;
		area = new Rect(position, width, height);
		this.mx = (int) area.x;
		this.my = (int) area.y;
		insideArea = new Rect(mx+edgeLimit, my+edgeLimit, width-edgeLimit*2, height-edgeLimit*2);
		if (hardcodeName != null) {
			setHardcodeTilenames();
		}
	}
	
	//calls the Location.setHardcodedTilename on each of its locations with the appropriate String and extensions.
	private void setHardcodeTilenames() {
		for (int x=0;x<width;x++) {
			for (int y=0;y<height;y++) {
				location[mx+x][my+height-1-y].setHardcodeTilename(hardcodeName+"-"+x+"-"+y);
			}
		}
	}
	
	public boolean isInside(DungeonPosition position) {
		return area.isInside(position.x, position.y);
	}
	
	public boolean isInsideForTunnels(DungeonPosition position) {
		return insideArea.isInside(position.x, position.y);
	}
	
	public DungeonPosition getStartPosition() {
		return getPosiOfChar('S');
	}
	
	public DungeonPosition getExitPosition() {
		return getPosiOfChar('X');
	}
	
	public DungeonPosition getEntrance() {
		return getPosiOfChar('@');
	}
	
	private DungeonPosition getPosiOfChar(char c) {
		for (int x=0; x<width; x++) {
			for (int y=0; y< height; y++) {
				if (charMap[y].charAt(x) == c) {
					return new DungeonPosition(mx+x,my+height-1 - y);
				}
			}
		}
		return null;
	}
	
	public void clearSpaces() {
		// clear the spaces
		for (int x=0; x<width; x++) {
			for (int y=0; y< height; y++) {
				char c = charMap[y].charAt(x);
				if (c != '*' && c != 't') {
					location[mx+x][my+height-1-y].clearLocation();
				} else {
					location[mx+x][my+height-1-y].setLocationToWall();
				}
			}
		}
	}
	
	protected void setIslands() {
		for (int x=0; x<width; x++) {
			for (int y=0; y< height; y++) {
				if (charMap[y].charAt(x) == 'I') {
					location[mx+x][my+height-1-y].setAsIsland(IslandType.TORCH_ISLAND);
				} else if (charMap[y].charAt(x) == 'O') {
					location[mx+x][my+height-1-y].setAsIsland(IslandType.DARK_ISLAND);
				} else if (charMap[y].charAt(x) == 'G') {
					location[mx+x][my+height-1-y].setAsIsland(IslandType.GLOW_ISLAND);
				}
			}
		}
	}
	
	protected void setTorches() {
		for (int x=0; x<width; x++) {
			for (int y=0; y< height; y++) {
				if (charMap[y].charAt(x) == 't') {
					location[mx+x][my+height-1-y].createTorchAt();
				}
			}
		}
	}
	
	protected void addRoughTerrain(IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery) {
		RoughTerrainType tt;
		for (int x=0; x<width; x++) {
			for (int y=0; y< height; y++) {
				
				tt = null;
				switch (charMap[y].charAt(x)) {
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
					location[mx+x][my+height - 1 - y].setRoughTerrain(tt, dungeonEvents, dungeonQuery);
				}
			}
		}
	}
	
	public void setMonsters(IDungeonEvents dungeonEvents) {
		int numMonsters = monsters.length;
		for (Integer i=0; i < numMonsters; i++) {
			String monster = monsters[i];
			DungeonPosition dungeonPosition = getPosiOfChar(i.toString().charAt(0));
			dungeonEvents.addMonsterToMap(monster, dungeonPosition);
		}
	}
}
