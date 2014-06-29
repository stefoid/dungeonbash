package com.dbash.models;

import java.util.Vector;


public interface IDungeonControl extends TouchEventListener {

	// This is mainly for TurnProcessor to control the dungeon
	
	/**
	 * Generates a new level using a room corridor system. The density of the
	 * level is dictated by the level integer.
	 * 
	 * @param level
	 *            A relative level on density for the level (the higher the
	 *            number the denser the level.
	 */
	public void createLevel(TurnProcessor turnProcessor, int level);
	public void restart();
	public void gameOver();
	public void resume(TurnProcessor turnProcessor, int level, AllCreatures allCreatures, Vector<Character> charactersOnMap);
}
