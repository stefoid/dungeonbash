package com.dbash.models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import com.dbash.models.Ability.AbilityEffectType;
import com.dbash.models.Ability.AbilityType;
import com.dbash.util.Randy;
// Dungeon delegates creating a new map to the Map model, but when it has done so, it populates it with monsters.
// When the turn processor and various creatures do stuff (dungeon events) it updates the various models and if the 
// event has visible effects, it will let various dungeon presentation event presenters know about them so they can be displayed.
//
//FOCUS CHANGES
//1) the only focus changes sent from the turn processor are those that indicate a change of focus of the character whose turn it is next.
//2) other focus changes are:
//- a) when character moves, the focus concurrently moves with it (initiated by the MapPresenter.)
//- b) when a monster moves, dungeon determines if the source and target tile are both within the current focus, 
//     the focus doesn't change, otherwise it changes to the relevant characters focus
//- c) when a monster attacks (mellee/ranged) or invokes an ability, the focus must move to the relevant characters. (dungeon).
public class Dungeon implements IDungeonControl, IDungeonEvents,
								IDungeonQuery, IPresenterDungeon {

	public enum MoveType {
		NORMAL_MOVE,
		LEADER_MOVE,
		FOLLOWER_MOVE
	}
	
	public final static int FINAL_LEVEL = 20;

	int currentLevel;
	Map map;
	TurnProcessor turnProcessor;
	LinkedList<Monster> mobs;
	DungeonPosition		eyePos;
	DungeonPosition currentFocus;
	IDungeonPresentationEventListener dungeonEventListener;
	IMapPresentationEventListener mapEventListener;
	HashMap<Character, ShadowMap> shadowMaps;
	private UIInfoListener	eyeDetailsListener;
	Character currentlyFocussedCharacter;
	int focusCharId;
	
	public Dungeon(boolean newGame)
	{	
		initLevel();
		map = new Map();  // empty map to start
		shadowMaps = new HashMap<Character, ShadowMap>();
	}
	
	
	public void load(ObjectInputStream in, AllCreatures allCreatures) throws IOException, ClassNotFoundException {
		shadowMaps = new HashMap<Character, ShadowMap>();
		initLevel();
		
		// Put all the Monsters in the mobs list
		for (Creature creature : allCreatures) {
			if (creature instanceof Monster) {
				mobs.add((Monster) creature);
			}
		}
		
		// currently focused character id
		focusCharId = in.readInt();
		
		// read the focus position
		currentFocus = (DungeonPosition) in.readObject();
		
		// Load the map info with Locationtypes.
		map = new Map(in, this, allCreatures, this, this);
		
		// must set the map now so that it can observe changes to Locations as monsters are added to the level.
		mapEventListener.setMap(map);
		
		// now load those creatures and items
		map.load(in, this, allCreatures, this, this);
	}
	
	public void persist(ObjectOutputStream out) throws IOException {
		// write the uniqueId of the currently focused character
		if (currentlyFocussedCharacter == null) {
			out.writeInt(TurnProcessor.NO_CURRENT_CREATURE);
		} else {
			out.writeInt(currentlyFocussedCharacter.uniqueId);
		}
		
		out.writeObject(currentFocus);
		map.persist(out);
	}

	public void restart() {
		initLevel();
		map = new Map();  // empty map to start
		shadowMaps = new HashMap<Character, ShadowMap>();
	}
	
	@Override
	public void resume(TurnProcessor turnProcessor, int level, AllCreatures allCreatures, Vector<Character> charactersOnMap) {
		this.turnProcessor = turnProcessor;
		this.currentLevel = level;
		
		// This is how we tell the map that Characters are associated with it.
		for (Character character : charactersOnMap) {
			character.resume(map);
			shadowMaps.put(character, character.shadowMap);
		}
		
		// focus the map on the currently focused character
		if (focusCharId != TurnProcessor.NO_CURRENT_CREATURE) {
			currentlyFocussedCharacter = (Character) allCreatures.getCreatureByUniqueId(focusCharId);
			setMapFocus(currentlyFocussedCharacter.mapPosition, currentlyFocussedCharacter.shadowMap);
		} else {
			// Create a new shadowmap that sees everything in its range, and a light to see with.
			ShadowMap shadowMap = new ShadowMap();
			shadowMap.setMap(map, currentFocus, 5);
			shadowMap.emptyShadowMap(true);
			setMapFocus(currentFocus, shadowMap);
			map.addLight(new Light(currentFocus, 5, Light.CHAR_LIGHT_STRENGTH, false));
		}
	}
	
	protected void initLevel() {
		mobs = new LinkedList<Monster>();
		eyePos = null;
	}
	
	
	@Override
	public void createLevel(TurnProcessor turnProcessor, int level)
	{
		this.turnProcessor = turnProcessor;
		currentLevel = level;
		// will create a map of Locations all ready to use.
		map = new Map(level);
		initLevel();
		// If its the last level, put nashkur at the exit instead of an exit.
		if (level == Dungeon.FINAL_LEVEL) {
			Monster nashkur = new Monster(0, level, map.exitPoint, this, this, turnProcessor);
			map.location(map.exitPoint).creature = nashkur;
			mobs.add(nashkur);
		}
		else {
			map.location(map.exitPoint).setAsExit();
		}

		// must set the map now so that it can observe changes to Locations as monsters are added to the level.
		mapEventListener.setMap(map);

		// TOTO turn off monsters to test character anim for now
		// add monsters to level
		for (int i = 0; i < ((map.height - 12) * 3 / 2); i++)
		{
			if ((level > 4) && (Randy.getRand(1, 10) == 1))
			{
				placeSwarm(map.roomPoints[Randy.getRand(0, map.roomPoints.length - 1)]);
			}
			else
			{
				placeMonster(map.getRandomPoint(true));
			}
		}

		setMapFocus(map.startPoint, null);
	}
	
	private void setMapFocus(DungeonPosition pos, ShadowMap shadowMap) {
		currentFocus = new DungeonPosition(pos);
		mapEventListener.setFocusPosition(pos, shadowMap);
	}
	
	private void placeMonster(DungeonPosition dungeonLocation) {
		// try to register in that possie, otherwise just return
		dungeonLocation.level = currentLevel;
		Monster monster = createMonster(dungeonLocation);
		putMonsterOnMap(monster, dungeonLocation);

	}

	protected void putMonsterOnMap(Monster monster, DungeonPosition dungeonPosition) {
		if (whatIsAtLocation(dungeonPosition) == AtLocation.FREE) {
			if (monster.getPosition().distanceTo(map.startPoint) > 2) {
				mobs.add(monster);
				map.location(dungeonPosition).creature = monster;
			}
		}	
	}
	
	protected Monster createMonster(DungeonPosition dungeonLocation) {
		return new Monster(Monster.getMonsterId(currentLevel, false), currentLevel, dungeonLocation, this, this, turnProcessor);
	}

	protected void placeSwarm(DungeonPosition pos) {
		int monsterType = Monster.getMonsterId(currentLevel / 2, true);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				DungeonPosition monsterPosition = new DungeonPosition(pos.x + dx, pos.y + dy, 1,currentLevel);
				Monster monster = new Monster(monsterType,currentLevel,monsterPosition,this, this, turnProcessor);
				putMonsterOnMap(monster, monsterPosition);
			}
		}
	}
	
	
	@Override
	public void creatureMove(int sequenceNumber, Character releventCharacter, Creature actingCreature, DungeonPosition fromPosition,
			DungeonPosition toPosition, int direction, MoveType moveType, IAnimListener completeListener) {

		// update the position of the creature in the model.
		map.location(fromPosition).setCreature(null);
		map.location(toPosition).setCreature(actingCreature);
		actingCreature.setPosition(toPosition);
		
		// creature has been moved in the model, but does it need to be animated?
		// setting the position above has 
		if (dungeonEventListener != null && releventCharacter != null) {
			System.out.println("SN:"+sequenceNumber + " creatureMove-" + moveType);
			
			if (currentlyFocussedCharacter != null) {
				// So we have a character in mind that the monster moving toward or away from.(relevent character)
				// That might not be the currently focussed character, but if both source and dest are
				// visible to the currently focussed character, then there is no need to change focus.
				if (!positionIsInLOSOfCharacter(currentlyFocussedCharacter, fromPosition) ||
						!positionIsInLOSOfCharacter(currentlyFocussedCharacter, toPosition)) {
					// so at least one of the positions is not in the current focus.
					// therefore its most correct to move the focus to the relevant character.
					if (currentlyFocussedCharacter != releventCharacter) {
						changeCurrentCharacterFocus(sequenceNumber, releventCharacter);
					}
				}					
			}
			
			// ok, so presumably we can see this move, one way or the other.
			dungeonEventListener.creatureMove(sequenceNumber, releventCharacter, actingCreature, fromPosition, toPosition, direction, moveType, completeListener);
		} else {
			// we wont animate, but call things that might be waiting on the animation to compelte anyway.
			if (completeListener != null) {
				completeListener.animEvent();
			}
		}
	}
	
	@Override
	public void meleeAttack(int sequenceNumber, Character releventCharacter, Creature attackingCreature, DungeonPosition targetPosition) {
		if (dungeonEventListener != null && releventCharacter != null) {
			changeCurrentCharacterFocus(sequenceNumber, releventCharacter);
			System.out.println("SN:"+sequenceNumber + " meleeAttack");
			dungeonEventListener.meleeAttack(sequenceNumber, releventCharacter, attackingCreature, targetPosition);
		}
	}

	@Override
	public void rangedAttack(int sequenceNumber, Character releventCharacter, Creature attackingCreature, AbilityType abilityType, int damageType, DungeonPosition targetPosition) {
		if (dungeonEventListener != null && releventCharacter != null) {
			changeCurrentCharacterFocus(sequenceNumber, releventCharacter);
			System.out.println("SN:"+sequenceNumber + " rangedAttack");
			dungeonEventListener.rangedAttack(sequenceNumber, releventCharacter, attackingCreature, abilityType, damageType, targetPosition);
			}
	}
	
	@Override
	public void invokeAbility(int sequenceNumber, Character releventCharacter, Creature actingCreature, DungeonPosition targetPosition, Data ability) {
		changeCurrentCharacterFocus(sequenceNumber, releventCharacter);
		if (dungeonEventListener != null && releventCharacter != null) {
			System.out.print("SN:"+sequenceNumber + " invoke abaility");
			dungeonEventListener.invokeAbility(sequenceNumber, releventCharacter, actingCreature, targetPosition, ability);
		}
	}
	
	@Override
	public void objectDrop(int sequenceNumber, Creature releventCharacter, Ability abilityObjectDropped, DungeonPosition position) {
		map.location(position).dropItem(abilityObjectDropped);
		dungeonEventListener.objectDrop(sequenceNumber, releventCharacter, abilityObjectDropped, position);
		if (position == eyePos) {
			if (eyeDetailsListener != null) {
				eyeDetailsListener.UIInfoChanged();
			}
		}
	}

	@Override
	public void objectPickup(int sequenceNumber, Character releventCharacter, Ability abilityObjectPickedUp, DungeonPosition position) {
		map.location(position).pickupItem(abilityObjectPickedUp);
		dungeonEventListener.objectPickup(sequenceNumber, releventCharacter, abilityObjectPickedUp, position);
		if (position == eyePos) {
			if (eyeDetailsListener != null) {
				eyeDetailsListener.UIInfoChanged();
			}
		}
	}

	@Override
	public void changeCurrentCharacterFocus(int sequenceNumber, Character newFocusCharacter) {
		if (currentlyFocussedCharacter != newFocusCharacter) {
			currentlyFocussedCharacter = newFocusCharacter;
			if (dungeonEventListener != null) {
				System.out.println("SN:"+sequenceNumber + " changeCurrentCharcterFocus");
				mapEventListener.changeCurrentCharacterFocus(sequenceNumber, newFocusCharacter);
			}
		}
	}

	@Override
	public void fallIntoLevel(int sequenceNumber, Character fallingCharacter) {
		map.location(map.startPoint).setCreature(fallingCharacter);
		fallingCharacter.shadowMap.setMap(map, map.startPoint, 5);
		fallingCharacter.setPosition(map.startPoint);
		shadowMaps.put(fallingCharacter, fallingCharacter.shadowMap);
		if (dungeonEventListener != null) {
			System.out.println("SN:"+sequenceNumber + " fallIntoLevel");
			dungeonEventListener.fallIntoLevel(sequenceNumber, fallingCharacter);
		}
	}

	@Override
	public void goDownStairs(int sequenceNumber, Character actingCreature, IAnimListener completeListener) {

		cleanUpAfterCharacterLeavesMap((Character)actingCreature);
		
		final Creature downer = actingCreature;
		final IAnimListener complete = completeListener;
		if (dungeonEventListener != null) {
			System.out.println("SN:"+sequenceNumber + " down stairs");
			dungeonEventListener.goDownStairs(sequenceNumber, actingCreature, new IAnimListener () {
				public void animEvent() {
					map.location(downer.getPosition()).setCreature(null);
					complete.animEvent();
				}
			});
		}
	}

	@Override
	public void creatureDies(int sequenceNumber, Character releventCharacter, Creature deadCreature) {
		final DungeonPosition deadPosition = deadCreature.getPosition();
		final Creature deader = deadCreature;
		boolean inLOS = false;
		if (deadCreature instanceof Character) {
			changeCurrentCharacterFocus(sequenceNumber, (Character)deadCreature);
			cleanUpAfterCharacterLeavesMap((Character)deadCreature);
			inLOS = true;
		} else {
			if (currentlyFocussedCharacter != null) {  // we use currently focussed here because relevant character = closest character
				inLOS = positionIsInLOSOfCharacter(currentlyFocussedCharacter, deadPosition);
			}
		}

		if (dungeonEventListener != null && releventCharacter != null && inLOS) {
			System.out.println("SN:"+sequenceNumber + " creatureDies");
			dungeonEventListener.creatureDies(sequenceNumber, releventCharacter, deadCreature, deadPosition, new IAnimListener () {
				public void animEvent() {
					map.location(deader.getPosition()).setCreature(null);
				}
			});
		} else {
			map.location(deader.getPosition()).setCreature(null);
		}
	}

	@Override
	public void damageInflicted(int sequenceNumber, Character releventCharacter, DungeonPosition targetPosition, int damageType, int damageAmount) {
		
		if (dungeonEventListener != null && releventCharacter != null && positionIsInLOSOfCharacter(releventCharacter, targetPosition)) {
			System.out.println("SN:"+sequenceNumber + " damageInflicted");
			dungeonEventListener.damageInflicted(sequenceNumber, releventCharacter, targetPosition, damageType, damageAmount);
		}
	}

	// get rid of the current character from the map and the current set of shadowmaps, so it is invisible to monsters
	// but delay removing the creature fromthe Locations so that the map can snapshot the final view of the dead
	// character until focus changes to the next character.
	protected void cleanUpAfterCharacterLeavesMap(Character character) {
		if (currentlyFocussedCharacter == character) {
			currentlyFocussedCharacter = null;
		}
		
		final ShadowMap deadCharactersMap = shadowMaps.get(character);
		shadowMaps.remove(character);
		
		mapEventListener.retainViewUntilNextFocusChange(deadCharactersMap, new UIInfoListener () {
			public void UIInfoChanged() {
				deadCharactersMap.emptyShadowMap(false);
			}
		});
	}
	
	@Override
	public void missed(int sequenceNumber, Character releventCharacter, DungeonPosition targetPosition) {
		if (dungeonEventListener != null && releventCharacter != null) {
			System.out.println("SN:"+sequenceNumber + " missed");
			dungeonEventListener.missed(sequenceNumber, releventCharacter, targetPosition);
		}
	}

	@Override
	public void gameOver() {
		dungeonEventListener.gameOver();
		map.addLight(new Light(currentFocus, 5, Light.CHAR_LIGHT_STRENGTH, false));
	}
	
	@Override
	public void abilityAdded(int sequenceNumber, Character releventCharacter, Vector<AbilityEffectType> abilityEfectType, DungeonPosition targetPosition) {
		if (dungeonEventListener != null && releventCharacter != null && positionIsInLOSOfCharacter(releventCharacter, targetPosition)) {
			for (AbilityEffectType effect : abilityEfectType) {
				printEff(effect);
				System.out.println("SN:"+sequenceNumber + " abilityAdded-");
				printEff(effect);System.out.println();
				dungeonEventListener.abilityAdded(sequenceNumber, releventCharacter, effect, targetPosition);
			}
		}
	}

	@Override
	public void abilityResisted(int sequenceNumber, Character releventCharacter, Vector<AbilityEffectType> abilityEfectType, DungeonPosition targetPosition) {
		if (dungeonEventListener != null && releventCharacter != null && positionIsInLOSOfCharacter(releventCharacter, targetPosition)) {
			for (AbilityEffectType effect : abilityEfectType) {
				printEff(effect);
				System.out.println("SN:"+sequenceNumber + " abilityREsisted-");
				printEff(effect);System.out.println();
				dungeonEventListener.abilityResisted(sequenceNumber, releventCharacter, effect, targetPosition);
			}
		}
	}

	@Override
	public void explosion(int sequenceNumber, Character releventCharacter, DungeonPosition targetPosition, int damageType, int range) {
		if (dungeonEventListener != null && releventCharacter != null) {
			System.out.println("SN:"+sequenceNumber + " explosion");
			dungeonEventListener.explosion(sequenceNumber, releventCharacter, targetPosition, damageType, range);
		}
	}

	@Override
	public void waitingForAnimToComplete(int sequenceNumber, IAnimListener animCompleteListener) {
		if (dungeonEventListener != null) {
			//System.out.print("SN:"+sequenceNumber + " waitingForAnimToComplete");
			dungeonEventListener.waitingForAnimToComplete(sequenceNumber, animCompleteListener);
		}
	}

	@Override
	public void setCharacterisUsingEye(boolean usingEye, DungeonPosition position, boolean showEyeAnim) {
		// turn off previous eye pos if relevent
		if (eyePos != null) {
			dungeonEventListener.usingEye(eyePos, false);
		}
		
		// turn on new eye pos if relevent.
		if (dungeonEventListener != null) {
			if (usingEye && showEyeAnim) {
				dungeonEventListener.usingEye(position, true);
			} 	
		}
		eyePos = position;
		eyeDetailsListener.UIInfoChanged();
	}

	@Override
	public boolean touchEvent(TouchEvent event) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onChangeToEyeDetails(UIInfoListener listener) {
		this.eyeDetailsListener = listener;
	}

	@Override
	public ItemList getItemsAtEyepos() {
		if (eyePos != null) {
			return map.location(eyePos).getItemList();
		} else {
			return new ItemList();
		}
	}

	@Override
	public Creature getCreatureAtEyePos() {
		if (eyePos != null) {
			return map.location(eyePos).creature;
		} else {
			return null;
		}
	}

	@Override
	public ItemList getItemsAtPosition(DungeonPosition position) {
		return map.location(position).getItemList();   
	}
	
	@Override
	public AtLocation whatIsAtLocation(DungeonPosition position) {
		if (map.inBounds(position) == false) {
			return AtLocation.WALL;
		} else {
			return map.location(position.x, position.y).whatIsAtLocation();
		}
	}

	@Override
	public Creature getCreatureAtLocation(DungeonPosition position) {
		return map.location(position).creature;
	}

	@Override
	public Character findClosestCharacterInSight(DungeonPosition position, Creature askingCreature) {
		Location location = map.location(position);
		return location.getClosestVisibleCharacter();
	}

	@Override
	public List<Creature> getCreaturesVisibleFrom(DungeonPosition mapPosition, int range) {
		ShadowMap shadowMap = new ShadowMap();
		shadowMap.setMap(map, mapPosition, range);
		return shadowMap.getVisibleCreatures();
	}

	@Override
	public boolean isEntranceFree() {
		if (map.location(map.startPoint).creature == null) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public LinkedList<Monster> getAllMonsters() {
		return mobs;
	}

	@Override
	public Character leaderModeOK() {
		// TODO inefficient really.
		for (Character character : shadowMaps.keySet()) {
			ShadowMap shadowMap = shadowMaps.get(character);
			if (shadowMap.monsterVisible()) {
				return character;
			}
		}
		
		return null;
	}



	
	private void printAt(AbilityType abilityType) {
	switch (abilityType) {
		case WEAPON:
			System.out.print("WEAPON");
			break;
		case AMULET:
			System.out.print("AMULET");
			break;
		case RANGED:
			System.out.print("RANGED");
			break;
		case WAND:
			System.out.print("WAND");
			break;
		case MAGIC_ITEM:
			System.out.print("MAGIC_ITEM");
			break;
		case ABILITY:
			System.out.print("ABILITY");
			break;
		default:
			System.out.print("something fucked up");
			break;
	}
}


private void printDam(int damType) {
	switch (damType) {
		case AbilityCommand.NO_PHYSICAL_ATTACK:
			System.out.print("MAGICAL");
			break;
		case AbilityCommand.CHEMICAL_ATTACK:
			System.out.print("CHEMICAL");
			break;
		case AbilityCommand.ENERGY_ATTACK:
			System.out.print("ENERGY");
			break;
		case AbilityCommand.HARD_ATTACK:
			System.out.print("HARD");
			break;
		case AbilityCommand.SHARP_ATTACK:
			System.out.print("SHARP");
			break;
		default:
			System.out.print("something fucked up");
			break;
	}
}
private void printEff(AbilityEffectType abilityEfectType) {
	switch (abilityEfectType) {
		case POISON:
			System.out.print("POISON");
			break;
		case BLESSING:
			System.out.print("BLESSING");
			break;
		case HEALING:
			System.out.print("HEALING");
			break;
		case PROTECTION:
			System.out.print("PROTECTION");
			break;
		case CURSE:
			System.out.print("CURSE");
			break;
		case SPEED:
			System.out.print("SPEED");
			break;
		case SLOW:
			System.out.print("SLOW");
			break;
		case HOLD:
			System.out.print("HOLD");
			break;
		case ATTACK:
			System.out.print("ATTACK");
			break;
		case RESIST_POISON:
			System.out.print("RESIST_POISON");
			break;
		case RESIST_HELD:
			System.out.print("RESIST_HELD");
			break;
		case RESIST_STUN:
			System.out.print("RESIST_STUN");
			break;
		case STUNNED:
			System.out.print("STUNNED");
			break;
		default:
			System.out.print("NONE_REALLY (show nothing, but look into why this is sent)");
			break;
	}
}
	

	@Override
	public Collection<Character> getCharactersVisibleFrom(DungeonPosition focusPosition, int range) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean positionIsInLOSOfCharacter(Character character, DungeonPosition position) {
		ShadowMap shadowMap = shadowMaps.get(character);
		Location location = null;
		if (map.inBounds(position)) {
			location = map.location(position);
		}
		if (shadowMap == null || location == null) {
			return false;
		} else {
			return shadowMap.locationIsVisible(location);
		}
	}

	@Override
	public boolean isCreatureOnStairs(Creature creature) {
		if (creature.getPosition().equals(map.exitPoint)) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isCreatureNearEntrance(DungeonPosition creaturePosition)
	{
		if (creaturePosition.distanceTo(map.startPoint) < 4) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Map getMap() {
		return map;
	}

	@Override
	public void onVisibleDungeonEvent(IDungeonPresentationEventListener listener) {
		this.dungeonEventListener = listener;
	}
	
	@Override
	public void onMapEvent(IMapPresentationEventListener listener) {
		this.mapEventListener = listener;
	}

	@Override
	public Character getCurrentFocusedCharacter() {
		return currentlyFocussedCharacter;
	}


	@Override
	public void highlightTile(DungeonPosition position, boolean showIt) {
		// turn off previous eye pos if relevent
		dungeonEventListener.usingEye(position, showIt);
	}

}
