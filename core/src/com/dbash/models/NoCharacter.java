package com.dbash.models;

import java.util.ArrayList;

import com.dbash.models.Dungeon.MoveType;
import com.dbash.presenters.dungeon.CreaturePresenter;
import com.dbash.presenters.tabs.AbilitySelectionList;


public class NoCharacter extends Character {

	public class NoCreaturePresenter extends CreaturePresenter {
		public NoCreaturePresenter() {super();}
		@Override
		public void creatureMove(int sequenceNumber, DungeonPosition fromPosition, final DungeonPosition toPosition, int direction, MoveType moveType, IAnimListener animCompleteListener) {}
		@Override
		public void fallIntoLevel(int sequenceNumber, final Character fallingCharacter) {}
		@Override
		public void goDownStairs(int sequenceNumber, Character actingCreature, IAnimListener completeListener) {}
		@Override
		public void creatureMeleeAttack(int sequenceNumber, DungeonPosition fromPosition, DungeonPosition targetPosition, int direction, IAnimListener animCompleteListener) {}
		@Override
		public void creatureDies(int sequenceNumber, Creature deadCreature, DungeonPosition deathPosition, IAnimListener completeListener) {}
		@Override
		public void invokeAbility(int sequenceNumber, Creature actingCreature, DungeonPosition targetPosition, Data ability) {}
	}

	private NoCreaturePresenter presenter = new NoCreaturePresenter();
	
	// Exists to be the current character when no actual character is having a turn.
	// Will override any functions that shouldnt do anything when there isnt a valid character
	// Beats the rest of the code having to check for null all the time.
	
	public NoCharacter()
	{
		super(5, new DungeonPosition (0, 0), 99, null, null, null);
		abilities = new ArrayList<Ability>();
		creature = new Data();
		creature.name = "nobody";
	}
	
	public void targetTileSelected(DungeonPosition postition) {
		
	}
	
	@Override
	public boolean isPlayerCharacter()
	{
		return false;
	}
	
	public AbilitySelectionList getAbilitySelectionList() {
		return new AbilitySelectionList(this);
	}
	
	public void movementGesture(int direction, DungeonPosition postition) {

	}

	@Override
	public void onChangeToAbilitySelectionList(UIInfoListener listener) {
		
	};
	
	@Override
	public void onChangeToCharacterStats(UIInfoListener listener) {
		
	};
	
	@Override
	public boolean isCharacterOnStairs() {
		return false;
	}
	
	@Override
	public void setCharacterisUsingEye(boolean usingEye) {
	}
	
	@Override
	public CreaturePresenter getCreaturePresenter() {
		return presenter;
	}
	
}
