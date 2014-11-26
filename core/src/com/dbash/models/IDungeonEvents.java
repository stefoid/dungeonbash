package com.dbash.models;

import java.util.Vector;

import com.dbash.models.Ability.AbilityEffectType;
import com.dbash.models.Ability.AbilityType;


//
// This interface is for telling the Dungeon about a game logic events, such as:
// - movement
// - ability use
// - change of character focus, etc...
//
// The expectation is that apart from updating the spatial relationship, these events will also have some accompanying animation
// that the caller may be interested in knowing when is finished, so a callback object is supplied.
//
// Rather than have just one function that takes an event object, we will use one function for each event.  The reason being that
// it simplifies and clarifies the calling code, and keeps the implementation of events in the Dungeon hidden.  If we used an 
// event object, the caller would have to pack the event with those parameters that were relevant, and pass it to a vague
// 'eventReceieved' type of function and the Dungeon would then have to interpret the event type and possibly translate it 
// to its own internal representation.  By using individual functions they convey exactly the event that is occurring and
// only those parameters required for that particular event are passed, clearly identifying exactly what this interface does 
// and doesn't do from looking at this file.
//
// Note also that each event has a sequence number as an indication of the order of events and also to indicate which events
// are linked.  ie. The use of an ability is one type of event, such as shooting a fireball a a target.  Linked events are
// the results of that event - creatures near the target area may take varying amount of damage - each one that does is 
// a separate event.  Linked events share the same sequence number.
//
// Also note that events that are visible and to, and played for a certain Character (lets call this a 'scene') must be 
// shown in sequence, or at the very least simultaneously.
// You cannot show an event with a higher sequence number before an event with a lower seuence number in the same scene.
//
// Other than the sequence number and callback, which are always used, events will also include a 'relevenatCharacter' parameter, 
// which could be null.  It is a hint to the Dungeon presenter as to which character's scene the event should be played in.
// This can be supplied by the game logic because each monster will either be moving randomly if it cant see a character, or it will
// be acting with purpose in response to the closest character it CAN see - which will be the 'releventCharacter' - 
// either moving towards, attacking, or running away from that character.
//
// For events that involve modifying the dungeon record, such as moving, picking up and dropping, dungeon will handle setting the positions of the 
// creatures or objects (abilities)
public interface IDungeonEvents {

	public static final int NO_DAMAGE = -1;
	
	// creature move:  The Dungeon will update the creatures position in it, and in its own record.
	void creatureMove(int sequenceNumber, Character releventCharacter, Creature actingCreature, DungeonPosition fromPosition, DungeonPosition toPosition, 
			int direction, Dungeon.MoveType moveType, IAnimListener completeListener);
	
	// object drop
	void objectDrop(int sequenceNumber, Creature releventCharacter, Ability abilityObjectDropped, DungeonPosition position);
	
	// object pickup
	void objectPickup(int sequenceNumber, Character releventCharacter, Ability abilityObjectPickedUp, DungeonPosition position);
	
	// current character focus change (That character is ready to have its turn)
	void changeCurrentCharacterFocus(int sequenceNumber, Character newFocusCharacter);
	
	// fall into level (lands wherever the entrance is)  The Dungeon will update the creatures position, and its record of it.
	void fallIntoLevel(int sequenceNumber, Character fallingCharacter, int level);
	
	// go down stairs
	void goDownStairs(int sequenceNumber, Character actingCreature, IAnimListener completeListener);
	
	// creature dies
	void creatureDies(int sequenceNumber, Character releventCharacter, Creature deadCreature);

	//////////////////////////
	// Abilities use and effects = These can be broken down to save Dungeon from having to interpret.
	/////////////////////////
	
	
	// melee attack
	// damage types
	//	AbilityCommand.NO_PHYSICAL_ATTACK = 0;
	//	AbilityCommand.CHEMICAL_ATTACK = 4;
	//	AbilityCommand.ENERGY_ATTACK = 3;
	//	AbilityCommand.HARD_ATTACK = 1;
	//	AbilityCommand.SHARP_ATTACK = 2;
	//	a diagonal 'slashy' type of blue flash for sharp damage (short cycle anim)
	//	an 'impacty' type of red smash for hard damage (short cycle anim)
	//	a drippy kind of green splash for chemical damage (short cycle anim)
	//	an explosiony/cloudy type of yellow flash for energy damage (short cycle anim)
	void meleeAttack(int sequenceNumber, Character releventCharacter, Creature attackingCreature, DungeonPosition targetPosition);
	
	// ranged attack
	void rangedAttack(int sequenceNumber, Character releventCharacter, Creature attackingCreature, AbilityType abilityType, int damageType, DungeonPosition targetPosition);
	
	// damage inflicted
	void damageInflicted(int sequenceNumber, Character releventCharacter, DungeonPosition targetPosition, int damageType, int damageAmount);	
	
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
	void abilityAdded(int sequenceNumber, Character releventCharacter, Vector<AbilityEffectType> abilityEfectType, DungeonPosition targetPosition);
	
	// ability resisted
	void abilityResisted(int sequenceNumber, Character releventCharacter, Vector<AbilityEffectType> abilityEfectType, DungeonPosition targetPosition);
	
	// area effect explosion type of thing, range in tiles
	void explosion(int sequenceNumber, Character releventCharacter, DungeonPosition targetPosition, int damageType, int range);
	void explosionOver();
	
	// character is using the eye at a certain position
	public void setCharacterisUsingEye(boolean usingEye, DungeonPosition position, boolean showEyeAnim);
	
	public void highlightTile(DungeonPosition position, boolean showIt);
	
	// At the end of a creatures turn, the turn processor will send this to the dungeon.
	// it will result in a 'dummy' animation event that goes onto the end of the aniamtion queue, to be 'executed' when 
	// all previous events in the queue have completed, then the callback will rest
	void waitingForAnimToComplete(int sequenceNumber, IAnimListener animCompleteListener);
}
