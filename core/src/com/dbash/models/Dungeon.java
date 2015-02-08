package com.dbash.models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import com.dbash.models.Ability.AbilityEffectType;
import com.dbash.models.Ability.AbilityType;
import com.dbash.models.IDungeonPresentationEventListener.DeathType;
import com.dbash.models.Location.RoughTerrainType;
import com.dbash.util.L;
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
//- c) when a monster attacks (melee/ranged) or invokes an ability, the focus must move to the relevant characters. (dungeon).
public class Dungeon implements IDungeonControl, IDungeonEvents,
								IDungeonQuery, IPresenterDungeon {
	
	public static final boolean LOG = false && L.DEBUG;
	
	public enum MoveType {
		NORMAL_MOVE,
		LEADER_MOVE,
		FOLLOWER_MOVE,
		KNOCKBACK_MOVE,
		CHARGE_MOVE,
		SHUDDER_MOVE
	}
	
	private class Explosion {
		public DungeonPosition center;
		public boolean hasFocusedOnCenter;
		
		public Explosion(DungeonPosition center) {
			this.center = center;
			hasFocusedOnCenter = false;
		}
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
	Character characterHavingTurn;
	Explosion explosion;
	
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
		if (dungeonEventListener != null) {
			dungeonEventListener.clearAnimations();
		}
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
			if (dungeonEventListener != null) {
				dungeonEventListener.newCharacterFocus(currentlyFocussedCharacter);
			}
		} else {
			// Create a new shadowmap that sees everything in its range
			ShadowMap shadowMap = new ShadowMap();
			shadowMap.setMap(map, currentFocus, 5);
			setMapFocus(currentFocus, shadowMap);
		}
	}
	
	protected void initLevel() {
		mobs = new LinkedList<Monster>();
		eyePos = null;
		characterHavingTurn = null;
	}
	
	
	@Override
	public void createLevel(TurnProcessor turnProcessor, int level)
	{
		this.turnProcessor = turnProcessor;
		if (L.DEBUG) {
			currentLevel = L.LEVEL;
		} else {
			currentLevel = level;
		}
		
		// will create a map of Locations all ready to use.
		map = new Map(currentLevel, this, this);
		initLevel();
		// If its the last currentLevel, put nashkur at the exit instead of an exit.
		
		switch (currentLevel) {
			case Dungeon.FINAL_LEVEL:
				Monster nashkur = new Monster(Creature.getIdForName("nashkur"), currentLevel, map.exitPoint, this, this, turnProcessor);
				map.location(map.exitPoint).creature = nashkur;
				mobs.add(nashkur);
				break;
			case 1:
				Monster mon1 = new Monster(Creature.getIdForName("gnome"), currentLevel, map.exitPoint, this, this, turnProcessor);
				map.location(map.exitPoint).creature = mon1;
				//mon1.addAbility(new Ability(Ability.getIdForName("wand of percussion"), null, 20, this, this), null); // TODO
				mobs.add(mon1);
				break;
			case 2:
				Monster mon2 = new Monster(Creature.getIdForName("halfling"), currentLevel, map.exitPoint, this, this, turnProcessor);
				map.location(map.exitPoint).creature = mon2;
				mobs.add(mon2);
				break;
			case 3:
				Monster mon3 = new Monster(Creature.getIdForName("dwarf"), currentLevel, map.exitPoint, this, this, turnProcessor);
				map.location(map.exitPoint).creature = mon3;
				mobs.add(mon3);
				break;
			case 4:
				Monster mon4 = new Monster(Creature.getIdForName("wizard"), currentLevel, map.exitPoint, this, this, turnProcessor);
				map.location(map.exitPoint).creature = mon4;
				mobs.add(mon4);
				break;
			case 5:
				placeSwarm(map.exitPoint, Creature.getIdForName("crazed minion"));
				break;
			default:
				break;
		}
		
		if (currentLevel != Dungeon.FINAL_LEVEL) {
			map.location(map.exitPoint).setAsExit();
		}
		

		// must set the map now so that it can observe changes to Locations as monsters are added to the currentLevel.
		mapEventListener.setMap(map);

		// TOTO turn off monsters to test character anim for now
		// add monsters to currentLevel
		for (int i = 0; i < ((map.height - 11) * 2); i++)
		{
			if ((currentLevel > 4) && (Randy.getRand(1, 10) == 1))
			{
				placeSwarm(map.roomPoints[Randy.getRand(0, map.roomPoints.length - 1)], null);
			}
			else
			{
				try {
					placeMonster(map.getRandomPoint(true));
				} catch (Map.MapException e) {
					
				}
			}
		}

		ShadowMap shadowMap = new ShadowMap();
		shadowMap.setMap(map, map.startPoint, Map.RANGE);
		setMapFocus(map.startPoint, shadowMap);
	}
	
	private void setMapFocus(DungeonPosition pos, ShadowMap shadowMap) {
		currentFocus = new DungeonPosition(pos);
		mapEventListener.instantFocusChange(pos, shadowMap);
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

	protected void placeSwarm(DungeonPosition pos, Integer monsterType) {
		if (monsterType == null) {
			monsterType = Monster.getMonsterId(currentLevel / 2, true);
		}

		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				DungeonPosition monsterPosition = new DungeonPosition(pos.x + dx, pos.y + dy, 1,currentLevel);
				Monster monster = new Monster(monsterType,currentLevel,monsterPosition,this, this, turnProcessor);
				putMonsterOnMap(monster, monsterPosition);
			}
		}
	}
	
	
	@Override
	public void creatureMove(int sequenceNumber, Character releventCharacter, final Creature actingCreature, final DungeonPosition fromPosition,
			final DungeonPosition toPosition, int direction, MoveType moveType, final IAnimListener completeListener) {
		
		// update the position of the creature in the model.
		final Location toLocation = map.location(toPosition);
		final Location fromLocation = map.location(fromPosition);
		fromLocation.setCreature(null);   					// Does not update LocationPresetner
		toLocation.moveCreature(actingCreature);  			// Does not update LocationPresetner
		actingCreature.setPosition(toPosition);
		
		if (LOG) L.log("SN: %s, moveType %s, nominalChar:%s, actingCreature:%s, focussedChar: %s", 
				sequenceNumber, moveType, releventCharacter, releventCharacter, currentlyFocussedCharacter); 
		
		// creature has been moved in the model, but does it need to be animated?
		// setting the position above has 
		if (dungeonEventListener != null && releventCharacter != null) {
			if (currentlyFocussedCharacter != null) {
				// So we have a character in mind that the monster moving toward or away from.(relevent character)
				// That might not be the currently focussed character, but if both source and dest are
				// visible to the currently focussed character, then there is no need to change focus.
				// Unless it is charging a character.
				if (moveType == MoveType.CHARGE_MOVE || !positionIsInLOSOfCharacter(currentlyFocussedCharacter, fromPosition) ||
						!positionIsInLOSOfCharacter(currentlyFocussedCharacter, toPosition)) {
					// so at least one of the positions is not in the current focus.
					// therefore its most correct to move the focus to the relevant character.
					if (currentlyFocussedCharacter != releventCharacter) {
						changeCurrentCharacterFocus(sequenceNumber, releventCharacter);
					}
				} 					
			}
			
			
			// ok, so presumably we can see this move, one way or the other.
			final IAnimListener moveListener = new IAnimListener() {
				@Override
				public void animEvent() {
					fromLocation.updatePresenter();  // we dont update the presenters until the animation has finished playing.
					toLocation.updatePresenter();
					if (completeListener != null) {
						completeListener.animEvent();
					}
				}
			};
			
			dungeonEventListener.creatureMove(sequenceNumber, releventCharacter, actingCreature, fromPosition, toPosition, direction, moveType, currentlyFocussedCharacter, moveListener);
			if (actingCreature == currentlyFocussedCharacter) {
				currentFocus = toPosition;  // this stops an annoying glitch
			}
		} else {
			// we wont animate, but creature presenter needs to be updated.
			dungeonEventListener.creatureMovedOutOfLOS(sequenceNumber, actingCreature, fromPosition, toPosition, direction, moveType);
			fromLocation.updatePresenter();
			toLocation.updatePresenter();
			if (completeListener != null) {
				completeListener.animEvent();
			}
		}
	}
	
	@Override
	public void meleeAttack(int sequenceNumber, Character releventCharacter, Creature attackingCreature, DungeonPosition targetPosition) {
		if (LOG) L.log("SN: %s, releventCharacter:%s, actingCreature:%s", sequenceNumber, releventCharacter, attackingCreature); 
		
		if (dungeonEventListener != null && releventCharacter != null) {
			changeCurrentCharacterFocus(sequenceNumber, releventCharacter);
			dungeonEventListener.meleeAttack(sequenceNumber, releventCharacter, attackingCreature, targetPosition);
		}
	}

	@Override
	public void rangedAttack(int sequenceNumber, Character releventCharacter, Creature attackingCreature, AbilityType abilityType, int damageType, DungeonPosition targetPosition) {
		if (LOG) L.log("sn: %s, releventCharacter: %s, attackingCreature: %s", sequenceNumber, releventCharacter, attackingCreature);
		if (dungeonEventListener != null && releventCharacter != null) {
			changeCurrentCharacterFocus(sequenceNumber, releventCharacter);
			dungeonEventListener.rangedAttack(sequenceNumber, releventCharacter, attackingCreature, abilityType, damageType, targetPosition);
		}
	}
	
	@Override
	public void invokeAbility(int sequenceNumber, Character releventCharacter, Creature actingCreature, DungeonPosition targetPosition, Data ability) {

		changeCurrentCharacterFocus(sequenceNumber, releventCharacter);
		if (dungeonEventListener != null && releventCharacter != null) {
			if (LOG) L.log("SN:"+sequenceNumber + " invoke abaility");
			dungeonEventListener.invokeAbility(sequenceNumber, releventCharacter, actingCreature, targetPosition, ability);
		}
	}
	
	@Override
	public void objectDrop(int sequenceNumber, Creature releventCharacter, Ability abilityObjectDropped, DungeonPosition position) {
		
		if (LOG) L.log("dropping "+abilityObjectDropped.ability.name);
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

	/**
	 * The explosion stuff isnt neccessary because a ranged attack is always at a creature.  so even if it affects multiple creatures,
	 * it is the ranged attack or melee attack that causes a focus change, not damage or abilities being added.
	 * the only other thing is creatures moving can cause focus to change, but not knockback or shudder which was causing
	 * a problem when multiple characters were caught in a brst knockback.
	 */
	@Override
	public void changeCurrentCharacterFocus(int sequenceNumber, Character newFocusCharacter) {
		
		boolean changeFocus = false;
		if (LOG) L.log("newFocusCharacter: %s", newFocusCharacter);
//		if (explosion != null) {
//			if (explosion.hasFocusedOnCenter == false) {
//				explosion.hasFocusedOnCenter = true;
//				// change focus to position, not character.
//				if (LOG) L.log("foucssing on position: %s", explosion.center);
//				mapEventListener.changeFocusToPosition(sequenceNumber, explosion.center);
//			}
//		} else {
			// Only change the currently focussed character if a monster is having a turn, or it is a request to change focus to the
			// actual character that is having a turn.
			if (characterHavingTurn == null || characterHavingTurn == newFocusCharacter) {
				changeFocus = true;
			} 
			
			// But not if that character is already focussed, unless it has been shifted when it wasnt its tun, like knockback.
			if (newFocusCharacter == currentlyFocussedCharacter) {
				if (currentFocus.equals(newFocusCharacter.getPosition()) == false) {
					changeFocus = true;
				} else {
					changeFocus = false;  // dont change focus if the character is allready in postion or moving to that position.
				}
			}
			
			if (LOG) L.log("changeFocus: %s", changeFocus);
			
			if (changeFocus) {
				currentlyFocussedCharacter = newFocusCharacter;
				currentFocus = newFocusCharacter.getPosition();
				if (dungeonEventListener != null) {
					if (LOG) L.log("SN:%s, newFocusCharacter:%s", sequenceNumber, newFocusCharacter);
					mapEventListener.changeCurrentCharacterFocus(sequenceNumber, newFocusCharacter);
					dungeonEventListener.newCharacterFocus(newFocusCharacter);
				}
			}
//		}
	}

	@Override
	public void fallIntoLevel(int sequenceNumber, Character fallingCharacter, int level) {
		if (LOG) L.log("SN:%s, fallingCharacter: %s",sequenceNumber, fallingCharacter);
		
		map.location(map.startPoint).setCreatureAndUpdatePresenter(fallingCharacter);
		fallingCharacter.shadowMap.setMap(map, map.startPoint, 5);
		fallingCharacter.setPosition(map.startPoint);
		shadowMaps.put(fallingCharacter, fallingCharacter.shadowMap);
		if (dungeonEventListener != null) {
			dungeonEventListener.fallIntoLevel(sequenceNumber, fallingCharacter, level);
		}
	}

	@Override
	public void goDownStairs(int sequenceNumber, Character actingCreature, IAnimListener completeListener) {

		characterLeavesMap((Character)actingCreature);
		
		final Creature downer = actingCreature;
		final IAnimListener complete = completeListener;
		if (dungeonEventListener != null) {
			if (LOG) L.log("SN:"+sequenceNumber + " down stairs");
			dungeonEventListener.goDownStairs(sequenceNumber, actingCreature, new IAnimListener () {
				public void animEvent() {
					map.location(downer.getPosition()).setCreatureAndUpdatePresenter(null);
					complete.animEvent();
				}
			});
		}
	}

	@Override
	public void creatureDies(int sequenceNumber, Character releventCharacter, Creature deadCreature) {

		if (LOG) L.log("releventCharacter: %s", releventCharacter); 
		final DungeonPosition deadPosition = deadCreature.getPosition();
		final Creature deader = deadCreature;
		boolean inLOS = false;
		if (deadCreature instanceof Character) {
			changeCurrentCharacterFocus(sequenceNumber, releventCharacter);
			characterLeavesMap((Character)deadCreature);
			inLOS = true;
		} else {
			if (releventCharacter != null) {  // we use currently focussed here because relevant character = closest character
				inLOS = positionIsInLOSOfCharacter(releventCharacter, deadPosition);
			}
		}
		
		if (dungeonEventListener != null && releventCharacter != null && inLOS) {
			if (LOG) L.log("SN:"+sequenceNumber + " creatureDies");
			DeathType deathType = DeathType.NORMAL;
			if (getTerrainAtLocation(deadPosition) == RoughTerrainType.HOLE) {
				deathType = DeathType.HOLE;
			}
			dungeonEventListener.creatureDies(sequenceNumber, releventCharacter, deadCreature, deadPosition, deathType, new IAnimListener () {
				public void animEvent() {
					map.location(deader.getPosition()).setCreatureAndUpdatePresenter(null);
				}
			});
		} else {
			map.location(deader.getPosition()).setCreatureAndUpdatePresenter(null);
		}
	}

	@Override
	public void damageInflicted(int sequenceNumber, Character releventCharacter, DungeonPosition targetPosition, int damageType, int damageAmount) {

		if (LOG) L.log("relevantChar " + releventCharacter); 
		if (dungeonEventListener != null && releventCharacter != null && positionIsInLOSOfCharacter(releventCharacter, targetPosition)) {
			Creature damagedCreature = getCreatureAtLocation(targetPosition);
			dungeonEventListener.damageInflicted(sequenceNumber, releventCharacter, damagedCreature, targetPosition, damageType, damageAmount);
		}
	}

	// get rid of the current character from the map and the current set of shadowmaps, so it is invisible to monsters
	protected void characterLeavesMap(Character character) {
		if (LOG) L.log("");
		if (currentlyFocussedCharacter == character) {
			currentlyFocussedCharacter = null;
		}
		shadowMaps.remove(character);
		mapEventListener.updateMapPresentation();
	}
	
	@Override
	public void missed(int sequenceNumber, Character releventCharacter, DungeonPosition targetPosition) {

		if (dungeonEventListener != null && releventCharacter != null) {
			if (LOG) L.log("SN:"+sequenceNumber + " missed");
			dungeonEventListener.missed(sequenceNumber, releventCharacter, targetPosition);
		}
	}

	@Override
	public void gameOver() {
		// TODO would be nicer if this happened after the character died.
		map.refreshLighting();  
		mapEventListener.updateMapPresentation();
		dungeonEventListener.gameOver();
	}
	
	@Override
	public void abilityAdded(int sequenceNumber, Character releventCharacter, Vector<AbilityEffectType> abilityEfectType, DungeonPosition targetPosition) {
		if (dungeonEventListener != null && releventCharacter != null && positionIsInLOSOfCharacter(releventCharacter, targetPosition)) {
			for (AbilityEffectType effect : abilityEfectType) {
				if (LOG) L.log("SN:%s, effect: %s" , sequenceNumber, effect);
				dungeonEventListener.abilityAdded(sequenceNumber, releventCharacter, effect, targetPosition);
			}
		}
	}

	@Override
	public void abilityResisted(int sequenceNumber, Character releventCharacter, Vector<AbilityEffectType> abilityEfectType, DungeonPosition targetPosition) {
		if (dungeonEventListener != null && releventCharacter != null && positionIsInLOSOfCharacter(releventCharacter, targetPosition)) {
			for (AbilityEffectType effect : abilityEfectType) {
				if (LOG) L.log("SN:%s, effect: %s" , sequenceNumber, effect);
				dungeonEventListener.abilityResisted(sequenceNumber, releventCharacter, effect, targetPosition);
			}
		}
	}

	@Override
	public void explosion(int sequenceNumber, Character releventCharacter, DungeonPosition targetPosition, int damageType, int range) {
		explosion = new Explosion(targetPosition);
		if (dungeonEventListener != null && releventCharacter != null) {
			if (LOG) L.log("SN: %s, damageType: %s",sequenceNumber , damageType);
			dungeonEventListener.explosion(sequenceNumber, releventCharacter, targetPosition, damageType, range);
		}
	}

	@Override
	public void explosionOver() {
		if (LOG) L.log("");
		explosion = null;
	}
	
	@Override
	public void waitingForAnimToComplete(int sequenceNumber, IAnimListener animCompleteListener) {
		if (dungeonEventListener != null) {
			//if (LOG) L.log("SN:"+sequenceNumber + " waitingForAnimToComplete");
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
		return false;
	}

	@Override
	public void onChangeToEyeDetails(UIInfoListener listener) {
		this.eyeDetailsListener = listener;
	}

	@Override
	public ItemList getItemsAtEyepos() {
		if (eyePos != null) {
			return map.location(eyePos).getItemList(true);
		} else {
			return new ItemList(true);
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
		return map.location(position).getItemList(false);   
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
	public Location.RoughTerrainType getTerrainAtLocation(DungeonPosition position) {
		if (map.inBounds(position) == false) {
			return null;
		} else {
			return map.location(position.x, position.y).getRoughTerrain();
		}
	}
	
	@Override
	public Location getLocation(DungeonPosition position) {
		if (map.inBounds(position) == false) {
			return null;
		} else {
			return map.location(position.x, position.y);
		}
	}

	@Override
	public Creature getCreatureAtLocation(DungeonPosition position) {
		return map.location(position).creature;
	}

	@Override
	public Character findClosestCharacterInSight(DungeonPosition position, Creature askingCreature) {
		Location location = map.location(position);
		int rangeOfClosestChar = 100;
		Character closestVisibleChar = null;
		
		// Each entry in this list of shadowmaps is a character that can see the monster, and hence, vice-versa.
		for (ShadowMap shadowMap : shadowMaps.values()) {
			if (shadowMap.locationIsVisible(location)) {
				Character character = shadowMap.owner;
				if (character != null) {
					int d = position.distanceToSpecial(character.getPosition());
					if (d < rangeOfClosestChar && character.isAlive()) {
						rangeOfClosestChar = d;
						closestVisibleChar = character;
					}
				}
			}
		}
		
		return closestVisibleChar;
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

	@Override
	public boolean positionIsInLOSOfCharacter(Character character, DungeonPosition position) {
		ShadowMap shadowMap = character.shadowMap;
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


	@Override
	public void currentCharacterHavingTurn(Character character) {
		characterHavingTurn = character;
	}

}
