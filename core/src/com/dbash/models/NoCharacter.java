package com.dbash.models;

import java.util.ArrayList;

import com.dbash.models.Dungeon.MoveType;
import com.dbash.models.IDungeonPresentationEventListener.DeathType;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.dungeon.CreaturePresenter;
import com.dbash.presenters.dungeon.MapPresenter;
import com.dbash.presenters.tabs.AbilitySelectionList;


public class NoCharacter extends Character {

	public class NoCreaturePresenter extends CreaturePresenter {
		public NoCreaturePresenter() {super();}
		@Override
		public void creatureMove(int sequenceNumber, DungeonPosition fromPosition, final DungeonPosition toPosition, int direction, MoveType moveType,  float moveTime, IAnimListener animCompleteListener) {}
		@Override
		public void fallIntoLevel(int sequenceNumber, final Character fallingCharacter, int level) {}
		@Override
		public void goDownStairs(int sequenceNumber, Character actingCreature, IAnimListener completeListener) {}
		@Override
		public void creatureMeleeAttack(int sequenceNumber, DungeonPosition fromPosition, DungeonPosition targetPosition, int direction, IAnimListener animCompleteListener) {}
		@Override
		public void creatureDies(int sequenceNumber, Creature deadCreature, DungeonPosition deathPosition, DeathType deathType, IAnimListener completeListener) {}
		@Override
		public void invokeAbility(int sequenceNumber, Creature actingCreature, DungeonPosition targetPosition, Data ability) {}
	}

	private NoCreaturePresenter presenter = new NoCreaturePresenter();
	
	// Exists to be the current character when no actual character is having a turn.
	// Will override any functions that shouldnt do anything when there isnt a valid character
	// Beats the rest of the code having to check for null all the time.
	
	public NoCharacter()
	{
		super(4, new DungeonPosition (0, 0), 99, null, null, null); // wild dog because it only has minimal abilities.  doesnt matter that much.
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
	public CreaturePresenter getCreaturePresenter(UIDepend gui, PresenterDepend model, MapPresenter mapPresenter) {
		return presenter;
	}
	

	@Override
	public boolean abilitySelected(Ability ability) {
		return false;
	}
	
	@Override
	public void defending() {
	
	}
	
	@Override
	public boolean itemPickupSelected(Ability ability) {
		return false;
	}
	
	@Override
	public void itemDropSelected(Ability ability) {
	}
	
	@Override
	public boolean addAbility(Ability ability, Creature giver) {
		return false;
	}
}
