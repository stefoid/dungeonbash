package com.dbash.models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.dbash.models.IDungeonQuery.AtLocation;
import com.dbash.util.L;
import com.dbash.util.Randy;
import com.dbash.util.SequenceNumber;

@SuppressWarnings("unused")

public class Monster extends Creature
{
	// ATTRIBUTES

	// direction to move for the monsters turn
	private int					moveDirection				= DungeonPosition.WEST;				
	private Ability				rangedAttack;	 				// ranged attack to use
	private Character			closestCharacter;				// character  to act against
	private boolean 			isNearCharacter;

	private DungeonPosition		LastCharacterPos			= new DungeonPosition(0, 0, 0, 0);
	
	// this constructor returns a monster that fits a certain level, either of a
	// type that swarms or a type that doesnt swarm
	public Monster(int creatureId, int level, DungeonPosition p, IDungeonEvents dungeonEvents, 
			IDungeonQuery dungeonQuery, TurnProcessor turnProcessor)
	{
		super(creatureId, p, dungeonEvents, dungeonQuery, turnProcessor); 

		// but now we need to know how much experience to give this creature,
		// depending on its desired level
		int creatureLevel = calcLevel(creatureId);
		
		if (dungeonQuery.whatIsAtLocation(p) == AtLocation.WALL) {
			if (LOG) L.log("wtf....");
		}
		
		if (creatureLevel < level) {
			addExperience(300 * (level - creatureLevel), true);
		}
	}

	@Override
	protected void death() {
		super.death();
		
		turnProcessor.gameStats.monsterKilled();
		
		// if a monster dies in a dungeon, and there is no character there to see it, did it really die?
		Character observer = dungeonQuery.findClosestCharacterInSight(mapPosition, this, true, false);
		turnProcessor.monsterDied(this, observer);
		dungeonEvents.waitingForAnimToComplete(SequenceNumber.getCurrent(), new IAnimListener() {
			public void animEvent() {
				dropAllPhysicalItems();
			}
		});
	}
	
//	// save the character to the stream in the same order that it will be read
//	public boolean saveObject(DataOutputStream out) throws IOException
//	{
//		//out.writeInt(ScreenObject.MONSTER_OBJECT);
//		super.saveObject(out); // saves all common creature data
//		return true;
//	}

	public Monster(ObjectInputStream stream, IDungeonEvents dungeonEvents, 
			IDungeonQuery dungeonQuery, TurnProcessor turnProcessor) throws IOException, ClassNotFoundException
	{
		super(stream, dungeonEvents, dungeonQuery, turnProcessor); 
	}

	// this must be called after at least one creature (character or monster)
	// has been created.
	public static int getMonsterId(int level, boolean swarm) {
		boolean resultOK = false;
		int random = 0;

		while (!resultOK) {
			random = Randy.getRand(1, creatureData.size() - 1);

			Data cd = (Data) creatureData.elementAt(random);

			if (cd.value < 1000) {
				if ((swarm && cd.swarm == 1) || !swarm) {
					int creatureLevel = calcLevel(random);

					if ((Randy.getRand(1, 20) == 1) && (creatureLevel <= level)) {
						resultOK = true; // throw in any old monster occasionally
					}

					if ((creatureLevel <= level) && (level - creatureLevel < 9)) {
						resultOK = true;
					}
				}
			}
		}

		return random;
	}

	public boolean canSkipTurn() {
		// if a monster is fully fit and has no temporary abilities, then we can skip its
		// turn if it is far from any character to speed up turn processing in lower levels.
		if (creatureIsStable) {
			if (isNearCharacter == false) {
				return true;
			}
		}
		
		return false;
	}
	
	public boolean getIsNearCharacter() {
		return isNearCharacter;
	}
	
	@Override
	// This is the decision making function for the Monster.  It could be broken up, but for now leave it.
	//
	// 1.  work out a possible move direction.
	// 1a. if there is a character currently in LOS, select a direction towards it.
	// 1b. if there isnt, remember where we last saw a character and select towards that spot in the hope
	//      of picking up the scent again.
	// 1c. If the monster has never seen a character, select a random direction
	// 1d. Test to see if our selected direction is a feasible move (could be something in the way)
	//
	// 2.  work out if there is a ranged attack on the cards.
	//
	// 3.  if we have a ranged attack and/or the selected direction to move is feasible, take the turn.
	//
	// At the end of the monsters turn, it will call endTurn
	public void processTurn() {
		rangedAttack = null; 

		boolean creatureStillAlive = processStatsForTurn();
		
		if (creatureStillAlive == false) {
			endTurn();
			return;
		}

		SequenceNumber.bumpSequenceNumberforNewCharacter();
		
		// 1.  So if we have a character in LOS, we can act towards it.
		if (null != closestCharacter)
		{
			// record this last known siting in case he looses us, we can
			// still try to get there to pick up the scent again
			LastCharacterPos = closestCharacter.getPosition();

			// Work out the best direction
			moveDirection = findBestDirection(LastCharacterPos, true, canMove);
			
			// if you are almost dead, run away!
			if ((health < calculateMaxHealth() / 5) ||
					((health < calculateMaxHealth() / 3) && (health < 10)) ||
					((myId == 0)  && (Randy.getRand(1, 2) < 2)) ) {// 1/2 the time, nashkur will run away!!!
				moveDirection = oppositeDirection(moveDirection); // run
			}
		} else { // Get the strategy when there is no character
			
			// two options: either go to the last known location of a
			// character, or do a random move
			if (((LastCharacterPos.x == 0) && (LastCharacterPos.y == 0)) || LastCharacterPos.equals(mapPosition)) {
				LastCharacterPos.x = 0;
				LastCharacterPos.y = 0;
				moveDirection = findBestDirection(null, true, canMove);  // random direction
			}
			else {
				moveDirection = findBestDirection(LastCharacterPos, true, canMove); // towards last known character position
			}
		}
		
		// we have worked out a direction to move, either towards the nearest character or running
		// away from the nearest character, or maybe not moving at all.
		
		
		// 2. Is there a possible ranged attack to make? Only if a character is in LOS...
		// now determine if we want to use ranged weapons or
		// some kind of artifact on ourself if it is not offensive
		if (null != closestCharacter) {
			rangedAttack = getRandomRangedAttack();

			// there is a possible ranged attack to use, but should we use it?
			if (rangedAttack != null) {
				int chanceRanged = 2; // out of 10

				// if health is lowish, more of a chance to use ranged attacks
				if ((health < calculateMaxHealth() / 2) || (health < 16) || (myId == 0)) 
					chanceRanged = 6;

				if (health < 3) // only seekers or the almost dead have this
								// little health, and they always use ranged
								// attacks if they can
					chanceRanged = 11;

				if (Randy.getRand(1, 10) > chanceRanged)
					rangedAttack = null; // dont use ranged attack
			}
		}
		
		// 3. If there is an action available to take, take it.
		if ((moveDirection != DungeonPosition.NO_DIR) || (rangedAttack != null)) {
			takeTurn();
		} else {
			endTurn();
		}
		
		return;
	}

	// Selects a random ranged or self-targeting ability to use, based on a few factors.
	private Ability getRandomRangedAttack()
	{
		List<Ability> rangedAttacks = new LinkedList<Ability>();

		for (int i = 0; i < abilities.size(); i++)
		{
			Ability ability = abilities.get(i);

			if ((ability.ability.invokingStrategy == Ability.TARGETABLE_ABILITY) ||
					(ability.ability.invokingStrategy == Ability.INSTANT_ABILITY) ||
					(ability.ability.invokingStrategy == Ability.INSTANT_ONESHOT))
			{
				int mag = magic - ability.ability.magicCost;

				// only target others using offensive stuff and if it costs
				// magic, if you have enough to do so
				if ((ability.ability.offensive) && (mag > -1))
				{
					DungeonPosition p = closestCharacter.getPosition();
					int attackRange = ability.ability.executeParam4[0]; // range of the attack 
					int targRange = mapPosition.distanceTo(p);
					
					// what is the range of this attack? if its targetable, we
					// dont want to be too close
					if (ability.ability.invokingStrategy == Ability.TARGETABLE_ABILITY)
					{
						if (targRange > attackRange)
							rangedAttacks.add(ability); // add this to the list of useable ranged attacks
					}
					else // or if its not targetable, we dont want to be too far away
					{
						if ((targRange <= attackRange) || (attackRange == Map.LOS)) // -1 = LOS
							rangedAttacks.add(ability); // add this to the list of usable ranged attacks
					}
				}
			}
		}

		// now choose a random useable one
		int s = rangedAttacks.size();

		if (s > 0)
			return (Ability) rangedAttacks.get(Randy.getRand(0, s - 1));
		else
			return null;
	}
	
	// METHODS


	// Move or attack, and tell the Dungeon about it.
	private void moveMonster(int direction)
	{
		DungeonPosition newPosition = new DungeonPosition(mapPosition, direction);
		switch (dungeonQuery.whatIsAtLocation(newPosition)) {
			case CHARACTER:
				closestCharacter = (Character) dungeonQuery.getCreatureAtLocation(newPosition);  // in case it was blundering into a hidden character by accident.
				makeMeleeAttack(closestCharacter);
				break;
			case FREE:
			case HOLE:
				Character releventCharacter = closestCharacter;
				boolean specialMove = false;
				// This could be a random move that just happens to end up in a character LOS.
				if (releventCharacter == null) {
					releventCharacter = dungeonQuery.findClosestCharacterInSight(newPosition, this, true, false);
				} else {
					specialMove = performCharge(newPosition, direction, AtLocation.CHARACTER, releventCharacter);
					if (!specialMove) {
						specialMove = performDash(newPosition, direction, releventCharacter);
					}
				}

				if (!specialMove) {
					dungeonEvents.creatureMove(SequenceNumber.getNext(), releventCharacter, this, mapPosition, newPosition, direction,  Dungeon.MoveType.NORMAL_MOVE, null);
				}
			break;
			case MONSTER:
				break;
			case WALL:
				break;
			default:
				break;
		}
	}

	// Whenever a monster does something that is in LOS of a character, the focus may have to change from the current character to observe it
	// This performs that function - it works out if both positions involved in the action would be currently visible.
	// If they are, then no focus change is required.  If they arent, then focus is changed to the target character of the action.
	// targetPos will either be where the monster wants to walk, or the position of a character targeted for a ranged attack
	protected void optionalFocusChange(DungeonPosition monsterPos, DungeonPosition targetPos) {
	}
	
	@Override
	protected Ability canDash() {
		Ability result = null;
		for (Ability ability : abilities) {
			if (ability.hasTag(Ability.DASH_TAG)) {
				if (ability.isCool()) {
					result= ability;
				}
			}
		}
		return result;
	}
	
	// if the monster actually does something, pass the turnFinishedBlock to the dungeon for asynch callback, otherwise call it immediately.
	// this is used to tell the turn processor to wait until that something has been animated before proceeding.
	private void takeTurn()
	{	
		// Move first or ranged attack?
		boolean useRanged = false;

		if (rangedAttack != null)
		{
			// use ranged attack on target character
			AbilityCommand command = new AbilityCommand(AbilityCommand.EXECUTE, 0, getCreature().head, getCreature().hands, getCreature().humanoid);
			// we have already	 verified	 that	 we	 can	 use	 this	 ability
			
			boolean wasSet = rangedAttack.set;
			rangedAttack.set = true;
			if (rangedAttack.ability.invokingStrategy == Ability.TARGETABLE_ABILITY)
				useRanged = rangedAttack.executeCommandTarget(command, closestCharacter, closestCharacter.getPosition(), this);
			else
			{
				useRanged = rangedAttack.executeCommandTarget(command, null, mapPosition, this);
				if (rangedAttack.ability.invokingStrategy == Ability.INSTANT_ONESHOT)
					abilities.remove(rangedAttack);
			}
			rangedAttack.set = wasSet;
			rangedAttack = null; //
		}

		// only move if there is a valid move to make
		// either that ranged attack did not happen, or we didnt decide to use a ranged attack
		if ((useRanged == false) && (moveDirection != DungeonPosition.NO_DIR)) {
			moveMonster(moveDirection);
		} 
		
		endTurn();
		
		return;
	}

	// Rather than scatter calls to this around Character and Monster, we will make it public
	// and have TurnProcessor call it when it wants to move onto the next creature.
	// Do whatever is required when a creatures turn ends, here.
	@Override
	public void endTurn()
	{
		// Creature::EndTurn winds down temporary abilities.
		super.endTurn();
	}

	@Override
	public Character getReleventCharacter() {
		return closestCharacter;
	}
	
	@Override 
	public void setReleventCharacter() {
		closestCharacter = dungeonQuery.findClosestCharacterInSight(mapPosition, this, true, false);
	}

	// the monster version of this function simply returns true if there are any temproary 
	// abilities active and false otherwise
	protected boolean calculateHighlightAbility() {
		for (Ability ability : abilities) {
			if (ability.ability.duration > 0)
				return true;  	
		}
		
		return false;
	}
	
	// Determine if a character is visible and/or the closest
	public void findClosestCharacter(Set<Character> characters) {
		for (Character character : characters) {
			ShadowMap shadowMap = character.shadowMap;
			closestCharacter = dungeonQuery.findClosestCharacterInSight(mapPosition, this, false, true);
		}
	}
	
	public void persist(ObjectOutputStream out) throws IOException {
		out.writeObject(CreatureType.MONSTER);
		super.persist(out);
	}
	
	public void setClosestCharacter(Character val) {
		closestCharacter = val;
	}
	
	public void setIsNearCharacter(boolean val) {
		isNearCharacter = val;
	}
}
