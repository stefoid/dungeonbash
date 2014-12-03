package com.dbash.models;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;



//
// This interface is for asking the Dungeon about spatial relationships:
// - which things are at a particular location
// - what is in LOS of a particular location, etc...
//
public interface IDungeonQuery {
	
	public enum AtLocation {
		FREE,
		MONSTER,
		WALL,
		CHARACTER,
		HOLE
	}

	// the basic repository of spatial relationships is a 2D array of Locations called a Map
	public Map getMap();
	
	// Is the position occupied by a character or is it free?  return true - used by monsters.
	public AtLocation whatIsAtLocation(DungeonPosition position);
	
	// Is the position occupied by a character or is it free?  return true - used by monsters.
	public Location getLocation(DungeonPosition position);
	
	// used by monsters to make melee attacks and probably characters too.
	public Creature getCreatureAtLocation(DungeonPosition position);
	
	// nearest character in LOS - used by Monsters to hunt characters
	public Character findClosestCharacterInSight(DungeonPosition position, Creature askingCreature);
	
	// all creatures in range of a position (1->LOS) for abilities
	// if range == 0, it means in LOS
	public List<Creature> getCreaturesVisibleFrom(DungeonPosition mapPosition, int range);
	
	// is the entrance free of creatures?
	public boolean isEntranceFree();
	
	// *create* a list of all the monsters created in a dungeon.
	public LinkedList<Monster> getAllMonsters();
	
	// effectively means: is there a monster in LOS of any character?
	public Character leaderModeOK();


	// Is the tile a valid target?
	/**
	 * @param character
	 * @param position
	 * @return
	 */
	public boolean positionIsInLOSOfCharacter(Character character, DungeonPosition position);
	
	// Stair related API - is the character in focus currently over the stairs?
	public boolean isCreatureOnStairs(Creature creature);
	public boolean isCreatureNearEntrance(DungeonPosition creaturePosition);
	
	public Character getCurrentFocusedCharacter();
}
