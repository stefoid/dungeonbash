package com.dbash.models;

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
	public int width;
	public int height;
	
	public Room(String[] charMap, String[] monsters, int edgeLimit) {
		this.charMap = charMap;
		this.monsters = monsters;
		width = charMap[0].length();
		height = charMap.length;
		this.edgeLimit = edgeLimit;
	}
	
	public Room(Room room) {
		this.charMap = room.charMap;
		this.monsters = room.monsters;
		this.edgeLimit = room.edgeLimit;
		this.width = room.width;
		this.height = room.height;
	}
	
	public void setPosition(DungeonPosition position, Location[][] location) {
		this.location = location;
		area = new Rect(position, width, height);
		this.mx = (int) area.x;
		this.my = (int) area.y;
	}
	
	public boolean isInside(DungeonPosition position) {
		return area.isInside(position.x, position.y);
	}
	
	public DungeonPosition getStartPosition() {
		return getPosiOfChar('S');
	}
	
	public DungeonPosition getExitPosition() {
		return getPosiOfChar('X');
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
				}
			}
		}
	}
	
	protected void setIslands() {
		for (int x=0; x<width; x++) {
			for (int y=0; y< height; y++) {
				if (charMap[y].charAt(x) == 'i') {
					location[mx+x][my+height-1-y].setAsIsland();
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
