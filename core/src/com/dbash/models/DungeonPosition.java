package com.dbash.models;

import java.io.Serializable;

@SuppressWarnings("serial")
public class DungeonPosition implements Serializable {
	
	// ATTRIBUTES
	// no point providing set and get methods for these, it will make code look
	// ugly and waste bytecodes
	public int x;
	public int y;
	public int z;
	public int level;
	public static final int NO_DIR = 10;
	public static final int SOUTHWEST = 7;
	public static final int SOUTHEAST = 6;
	public static final int NORTHWEST = 5;
	public static final int NORTHEAST = 4;
	public static final int SOUTH = 3;
	public static final int NORTH = 2;
	public static final int EAST = 1;
	public static final int WEST = 0;
	
	// INTERFACE
	public DungeonPosition() {
		level=1;
	}
	
	/**
	 * Take a copy of an existing position.
	 * @param dungeonPosition
	 */
	public DungeonPosition(DungeonPosition dungeonPosition) {
		this(dungeonPosition.x, dungeonPosition.y, dungeonPosition.z, dungeonPosition.level);
	}

	// Create a new point based on an existing position and a direction
	public DungeonPosition(DungeonPosition position, int direction) {
		this(position.x, position.y, position.z, position.level);
		
		applyDirection(this, direction);
	}
	
	// modify an existing position according to a direction
	public void applyDirection(DungeonPosition dungeonPosition, int direction) {
		switch (direction) {
			case DungeonPosition.EAST:
				dungeonPosition.x++;
				break;
			case DungeonPosition.WEST:
				dungeonPosition.x--;
				break;
			case DungeonPosition.NORTH:
				dungeonPosition.y++;
				break;
			case DungeonPosition.SOUTH:
				dungeonPosition.y--;
				break;
			case DungeonPosition.NORTHEAST:
				dungeonPosition.x++;
				dungeonPosition.y++;
				break;
			case DungeonPosition.NORTHWEST:
				dungeonPosition.x--;
				dungeonPosition.y++;
				break;
			case DungeonPosition.SOUTHEAST:
				dungeonPosition.x++;
				dungeonPosition.y--;
				break;
			case DungeonPosition.SOUTHWEST:
				dungeonPosition.x--;
				dungeonPosition.y--;
				break;
		}
	}
	
	@Override
	public String toString() {
		return "(level"+level+":" + x + "," + y + "," + z + ")";
	}

//	public DungeonPosition(DataInputStream stream) {
//		// read position
//
//		// now read and set the variable attributes
////		x = GameControl.readInt(stream);
////		y = GameControl.readInt(stream);
////		z = GameControl.readInt(stream);
////		level = GameControl.readInt(stream);
//	}

	public DungeonPosition(int x, int y, int z, int level) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.level = level;
	}

	public DungeonPosition(int x, int y) {
		this(x,y,0,1);
	}

	// the current DungeonPsosition is the fromPos.
	public int getDirection(DungeonPosition toPos) {
		if (toPos.x == x) {
			if (toPos.y > y) return NORTH;
			else if (toPos.y < y) return SOUTH;
			else return NO_DIR;
		} 
		
		if (toPos.y == y) {
			if (toPos.x > x) return EAST;
			else if (toPos.x < x) return WEST;
		} 
		
		if (toPos.y < y) {
			if (toPos.x > x) return SOUTHEAST;
			else return SOUTHWEST;
		} 
		
		if (toPos.y > y) {
			if (toPos.x > x) return NORTHEAST;
			else return NORTHWEST;
		} 
		
		return NO_DIR;
		
	}
	
//	public boolean saveObject(DataOutputStream out) throws IOException {
//		// write out the position
//		out.writeInt(x);
//		out.writeInt(y);
//		out.writeInt(z);
//		out.writeInt(level);
//		return true;
//	}

	public boolean equals(DungeonPosition p) {
		if (p == null) {
			return false;
		}
		
		return (x == p.x) && (y == p.y);
	}

	public synchronized DungeonPosition copy() {
		return new DungeonPosition(this);
	}

	
//	@Override
//	public int compareTo(Object arg0) {
//		if (arg0 instanceof DungeonPosition) {
//			DungeonPosition p = (DungeonPosition) arg0;
//			return p.x < x ? -1 : p.x > x ? 1 : p.y < y ? -1 : p.y > y ? 1 : 0;
//		}
//		return 0;
//	}

	public int distanceTo(DungeonPosition p) {
		int distance;
		int xDif = Math.abs(p.x - x);
		int yDif = Math.abs(p.y - y);
		
		distance = xDif;
		if (yDif > xDif) {
			distance = yDif;
		}
		
		return distance;
	}
	
	// a special version of distance that treats orthogonal as 'closer' than diagonal for creatures in melee range
	protected int distanceToSpecialSpecial(DungeonPosition p2) {
		int distance;
		int xDif = Math.abs(x - p2.x);
		int yDif = Math.abs(y - p2.y);
		
		distance = xDif;
		if (yDif > xDif) {
			distance = yDif;
		}
		
		if (distance == 1) {
			if (xDif == 0 || yDif == 0) {
				distance = 0;
			}
		}
		
		return distance;
	}

	static float[][] distances;
	
	public float getTrueDistance(DungeonPosition position) {
		return getTrueDistance(position.x, position.y);
	}
	
	public float getTrueDistance(int x, int y) {
		if (distances == null) {
			calcDistances();
		}
		
		int posX = Math.abs(this.x - x);
		int posY = Math.abs(this.y - y);
		return distances[posX][posY];
	}
	
	protected void calcDistances() {
		distances = new float[6][6];
		for (int x=0; x<6; x++) {
			for (int y=0; y<6; y++) {
				float X = x + 0.5f;
				float Y = y + 0.5f;
				distances[x][y] = (float) Math.sqrt((float) X*X+Y*Y);
			}
		}
	}
	
	static public int oppositeDirection(int direction)
	{
		int result = DungeonPosition.NO_DIR;

		// Identify the exact opposition direction
		switch (direction) {
			case DungeonPosition.NORTH:
				result = DungeonPosition.SOUTH;
				break;
			case DungeonPosition.SOUTH:
				result = DungeonPosition.NORTH;
				break;
			case DungeonPosition.EAST:
				result = DungeonPosition.WEST;
				break;
			case DungeonPosition.WEST:
				result = DungeonPosition.EAST;
				break;
			case DungeonPosition.NORTHEAST:
				result = DungeonPosition.SOUTHWEST;
				break;
			case DungeonPosition.NORTHWEST:
				result = DungeonPosition.SOUTHEAST;
				break;
			case DungeonPosition.SOUTHEAST:
				result = DungeonPosition.NORTHWEST;
				break;
			case DungeonPosition.SOUTHWEST:
				result = DungeonPosition.NORTHEAST;
				break;
		}

		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		DungeonPosition p = (DungeonPosition) obj;
		return p.x == x && p.y==y && p.level==level && p.z==z;
	}
	
}
