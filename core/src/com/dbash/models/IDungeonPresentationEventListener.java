package com.dbash.models;

import com.dbash.models.Ability.AbilityEffectType;
import com.dbash.models.Ability.AbilityType;
import com.dbash.models.Dungeon.MoveType;
import com.dbash.models.IPresenterTurnState.LeaderStatus;



public interface IDungeonPresentationEventListener {
	
	public static enum DeathType {
		NORMAL,
		HOLE
	}
	
	// creature move:  The Dungeon will update the creatures position in it, and in its own record.
	void creatureMove(int sequenceNumber, Character releventCharacter, Creature actingCreature, DungeonPosition fromPosition, DungeonPosition toPosition, 
			int direction, Dungeon.MoveType moveType, Character focussedCharacter, IAnimListener completeListener);
	
	void meleeAttack(int sequenceNumber, Character releventCharacter, Creature attackingCreature, DungeonPosition targetPosition);
	
	// object drop
	void objectDrop(int sequenceNumber, Creature releventCharacter, Ability abilityObjectDropped,
			DungeonPosition position);
	
	// object pickup
	void objectPickup(int sequenceNumber, Character releventCharacter, Ability abilityObjectPickedUp,
			DungeonPosition position);
	
	// fall into level (lands wherever the entrance is)  The Dungeon will update the creatures position, and its record of it.
	void fallIntoLevel(int sequenceNumber, Character fallingCharacter, int level);
	
	// These two events have complete listeners so the dungeon can remove the creature from the map after it is shown to be dead.
	void creatureDies(int sequenceNumber, Character releventCharacter, Creature deadCreature, DungeonPosition deadPosition, DeathType deathType, IAnimListener completeListener);
	// go down stairs
	void goDownStairs(int sequenceNumber, Character actingCreature,  IAnimListener completeListener);

	
	//////////////////////////
	// Abilities use and effects = These can be broken down to save Dungeon from having to interpret.
	/////////////////////////
	
	// ranged attack
	void rangedAttack(int sequenceNumber, Character releventCharacter, Creature attackingCreature, AbilityType abilityType, int damageType, DungeonPosition targetPosition);
	
	// damage inflicted
	void damageInflicted(int sequenceNumber, Character releventCharacter, Creature damagedCreature, DungeonPosition targetPosition, int damageType, int damageAmount);	
	
	// miss - either melee or ranged target miss
	void missed(int sequenceNumber, Character releventCharacter, DungeonPosition targetPosition);
	
	// This is when a creature uses an ability that isnt melee or a targeted ranged attack, such as a drinking a potion, or activating an Orb.
	// Does it even have a graphical representation?  Or do you just see the effects? Its probably magic... We will put this here and decide latter.
	void invokeAbility(int sequenceNumber, Character releventCharacter, Creature actingCreature, DungeonPosition targetPosition, Data ability);
	
	// ability added - could be good or bad.
	//	a light purple skull cloud for poison effect (fade anim)
	//	a crystal icon for magic-blesings (short cycle anim)
	//	a 'red heart' type of icon for healing (fade anim)
	//	a shield icon (fade anim) for extra protection 
	//  a sword or spear icon (fade anim) for exra attack skill 
	//	a speech bubble curse  for curses and similar negative effects
	//	a winged icon  (fade anim) for speeding
	//	a ball and chain icon (fade anim) for slowing
	//  spinning stars for stunned
	//	a grasping hand (short cycle anim)  for holding/imobilizing effect
	void abilityAdded(int sequenceNumber, Character releventCharacter, AbilityEffectType abilityEfectType, DungeonPosition targetPosition);
	
	// ability resisted
	void abilityResisted(int sequenceNumber, Character releventCharacter, AbilityEffectType abilityEfectType, DungeonPosition targetPosition);
	
	// area effect explosion type of thing, range in tiles
	void explosion(int sequenceNumber, Character releventCharacter, DungeonPosition targetPosition, int damageType, int range);
	
	public void waitingForAnimToComplete(int sequenceNumber, IAnimListener animCompleteListener);
	
	public void usingEye(DungeonPosition position, boolean showEyeAnim);
	
	public void gameOver();
	
	public void changeInLeaderStatus(LeaderStatus status);

	void creatureMovedOutOfLOS(int sequenceNumber, Creature actingCreature,
			DungeonPosition fromPosition, DungeonPosition toPosition,
			int direction, MoveType moveType);

	public void newCharacterFocus(Character newFocusCharacter);
}
