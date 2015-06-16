package com.dbash.models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import com.dbash.models.Ability.AbilityType;
import com.dbash.models.Ability.StatAbilityInfo;
import com.dbash.models.IDungeonQuery.AtLocation;
import com.dbash.models.Location.RoughTerrainType;
import com.dbash.platform.TextResourceIdentifier;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.dungeon.CreaturePresenter;
import com.dbash.presenters.dungeon.MapPresenter;
import com.dbash.presenters.root.tutorial.TutorialPresenter;
import com.dbash.util.EventBus;
import com.dbash.util.L;
import com.dbash.util.Randy;
import com.dbash.util.SequenceNumber;

@SuppressWarnings("unused")

public abstract class Creature implements IPresenterCreature
{
	public static final boolean LOG = true && L.DEBUG;
	
	public static enum CreatureSize {
		SMALL,
		MEDIUM,
		HUGE
	}
	
	public enum StealthStatus
	{
		HIDING(1),
		HIDING_POSSIBLE(2),
		HIDING_IMPOSSIBLE(3);
		
        int id;
        private StealthStatus(int id){this.id = id;}
        public int getvalue(){return id;}
        public static StealthStatus fromInt(int id) {
            switch (id) {
            case  1: 
            	return HIDING;
            case  2: 
            	return HIDING_POSSIBLE;
            default:
            	break;
            }
            return HIDING_IMPOSSIBLE;
        }
	}
	
	public static int SMALL_SIZE = 8;
	public static int HUGE_SIZE = 24;
	
	public static class DistComparator implements Comparator<Creature> {
		
		DungeonPosition posi;
		boolean smallestFirst;
		
		public DistComparator(DungeonPosition posi, boolean smallestFirst) {
			this.posi = posi;
		}
		
		@Override
		public int compare(Creature o1, Creature o2) {
			int d1 = o1.getPosition().distanceTo(posi);
			int d2 = o2.getPosition().distanceTo(posi);
			if (smallestFirst) {
				return d1 - d2;
			} else {
				return d2 - d1;
			}
		}
	}
	
	// INTERFACE
	public static final int	MAX_SPEED		= 10;
	
	public class CanMoveStrategy {
		public boolean checkMove(int intendedDirection, boolean canBeChar, DungeonPosition targetPos) {
			return canMove(intendedDirection, canBeChar);
		}
	}
	
	static int getIdForName(String name) {
		initializeData();
		for (Data data : creatureData){
			if (data.name.equalsIgnoreCase(name)) {
				return creatureData.indexOf(data);
			}
		}
		return 0;
	}
	
	public enum CreatureType {
		CHARACTER,
		MONSTER
	}

	protected CanMoveStrategy canMove = new CanMoveStrategy();
	protected Light light = null;
	protected StealthStatus stealthStatus;
	protected ArrayList<Ability> possiblePowerups;
	protected ArrayList<String> upgradeTypes;
	
	// instance data
	protected HighlightStatus highlightStatus;
	
	// the creatures current maximum health points is increased as it gets more experience
	int						maximumHealth;							

	// creatures current maximum magic points is increased as it gets more experience
	int						maximumMagic;							// the
	
	// the creatures current experience points
	int						experience;							
	int						expRoot;
	
	// how often does it get to have a turn?
	int						speed;									
	
	// the creatures current attack skill is increased as it gets more experience
	int						attackSkill;							
	
	// creatures current defence skill is increased as it gets more experience
	int						defenceSkill;							// the
	
	int stealth;
	
	int detect;
	
	// creatures current health points
	public int						health;								// the

	// creatures current magic points
	public int						magic;									// the
	
	public Data					creature;
	public List<Ability>					abilities;
	int						myId;
	public int				state;
	static boolean			dataInitialized	= false;

	static protected Vector<Data>	creatureData	= new Vector<Data>(30, 5);

	public static int uniqueIdCounter = 0;
	
	private boolean dead;

	public CreatureStats creatureStats = new CreatureStats();
	
	private int missedTurns;
	
	protected IDungeonQuery	dungeonQuery;
	protected IDungeonEvents dungeonEvents;
	protected TurnProcessor turnProcessor;
	
	protected UIInfoListener visualStatusListener;
	protected DungeonPosition mapPosition;
	protected CreaturePresenter creaturePresenter;
	protected boolean creatureIsStable = false;
	
	public int uniqueId;
	
	public boolean canFly;
	public boolean canCharge;
	
	public CreatureSize creatureSize;
	
	@Override
	public CreaturePresenter getCreaturePresenter(UIDepend gui, PresenterDepend model, MapPresenter mapPresenter) {
		if (creaturePresenter == null) {
			creaturePresenter = new CreaturePresenter(gui, model, this, mapPresenter);
		}
		return creaturePresenter;
	}

	public Creature(int creatureId, DungeonPosition p, IDungeonEvents dungeonEvents, 
			IDungeonQuery dungeonQuery, TurnProcessor turnProcessor)
	{
		this.dungeonEvents = dungeonEvents;
		this.dungeonQuery = dungeonQuery;
		this.turnProcessor = turnProcessor;
		highlightStatus = HighlightStatus.NO_HIGHLIGHT;
		setDead(false);
		initializeData();

		// now initilize this particular creature
		setCreature((Data) creatureData.elementAt(creatureId));
		setCreatureData(creatureId);
		uniqueId = uniqueIdCounter++;
		experience = 0;
		expRoot = 0;
		stealthStatus = StealthStatus.HIDING_POSSIBLE;

		for (int i = 0; i < getCreature().abilityIds.length; i++) {
			if (getCreature().abilityIds[i] != -1) {
				Ability a = new Ability(getCreature().abilityIds[i], this, p.level, dungeonEvents, dungeonQuery);
				addAbility(a, null);
			}
		}

		// set the default melee attack, armour and amulet for this creature
		AbilityCommand command = new AbilityCommand(AbilityCommand.SET, 0, getCreature().head, getCreature().hands, getCreature().humanoid);
		broadcastAbilityCommand(command);
		// unset any selectable cooldowns that might have accidently been set by the aobve command.
		command = new AbilityCommand(AbilityCommand.CLEAR, 0, 1, 1, 1);
		broadcastAbilityCommand(command);
		
		// now calculate initial health and magic according to this creatures
		// initial experience and abilities
		health = maximumHealth;
		magic = maximumMagic;
		
		setPosition(p); // will alert the creaturePresenter to the postion
	}

	public void persist(ObjectOutputStream out) throws IOException {
		//	5.1] Each Creature saves its type in the subclass - MONSTER OR CHARACTER
	
		// then...  unqiueId
		out.writeInt(uniqueId);
		out.writeObject(highlightStatus);
		out.writeInt(myId);

		// now write the variable attributes
		out.writeInt(experience); // the creatures current experience points
		out.writeInt(expRoot);
		out.writeInt(health); // the creatures current health points
		out.writeInt(magic); // the creatures current magic points
		out.writeInt(missedTurns);
		out.writeInt(stealthStatus.getvalue());

		// position.
		out.writeObject(mapPosition);

		// 5.3] Each Creature saves its list of Abilities
		ArrayList<Ability> unsetAbilities = unsetAbilities(true);
		out.writeInt(abilities.size());
		for (Ability ability : abilities) {
			ability.persist(out);
		}	
		
		// reset the abilities that were unset during the save, in case we resume without quitting.
		for (Ability ability : unsetAbilities) {
			AbilityCommand ac = new AbilityCommand(AbilityCommand.SET, 0, 1, 1, 1);
			ability.executeCommandValue(ac, this);
		}

	}
	
	private ArrayList<Ability> unsetAbilities(boolean leaveSet) {
		ArrayList<Ability> unsetAbilities  = new ArrayList<Ability>();
		// go through abilities and unset them all to get rid of added abilities
		// so they wont be added twice on reload
		for (int i = abilities.size() - 1; i >= 0; i--) {
			Ability a = (Ability) abilities.get(i);
			if (a.set) {
				unsetAbilities.add(a);
				a.unset(this); // will destroy any ability associated with this thing
				a.set = leaveSet; // reset just the setting indicator so it will
									// be saved and restored properly.
			}
		}
		return unsetAbilities;
	}
	
	// this creates a new creature from the input stream. It assumes that the
	// creator has already read in the position
	// and will register it if neccessary with the L at that position
	public Creature(ObjectInputStream in, IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery, TurnProcessor turnProcessor) throws IOException, ClassNotFoundException
	{
		// call the normal construction sequence
		this.dungeonEvents = dungeonEvents;
		this.dungeonQuery = dungeonQuery;
		this.turnProcessor = turnProcessor;
		
		initializeData();
		uniqueId = in.readInt();
		highlightStatus =  (HighlightStatus) in.readObject();
		myId = in.readInt();

		// set the static data and make the ability vector
		setCreature((Data) creatureData.elementAt(myId));
		setCreatureData(myId);

		// now read and set the variable attributes
		experience = in.readInt(); // the creatures current experience points
		expRoot = in.readInt();
		health = in.readInt(); // the creatures current healthpoints
		magic = in.readInt(); // the creatures current magic/points
	    missedTurns = in.readInt();
	    stealthStatus = StealthStatus.fromInt(in.readInt());

	    // position
	    mapPosition = (DungeonPosition) in.readObject();
	    
		// read abilities now
		int numberOfAbilities = in.readInt();
		for (int i=0; i < numberOfAbilities; i++) {
			Ability ability = new Ability(in, this, dungeonEvents, dungeonQuery);
			abilities.add(ability);
		}
		
		setAbilityFlags();
	}

	// this function sets all the data that can be derived form the creature id
	private void setCreatureData(int creatureId) {
		speed = getCreature().speed;
		attackSkill = getCreature().attackSkill;
		defenceSkill = getCreature().defenceSkill;
		stealth = getCreature().stealth;
		detect = getCreature().detect;
		
		// image = gameControl.getImageFromGif(creature.gifName);
		myId = creatureId;
		maximumHealth = getCreature().maximumHealth;
		maximumMagic = getCreature().maximumMagic;

		creatureSize = CreatureSize.MEDIUM;
		
		if (maximumHealth <= SMALL_SIZE) {
			creatureSize = CreatureSize.SMALL;
		}
		
		if (maximumHealth >= HUGE_SIZE) {
			creatureSize = CreatureSize.HUGE;
		}
		
		// create an ability vector for the abilities to go in
		abilities = new LinkedList<Ability>();
		
		setPossiblePowerups();
	}

	protected void setPossiblePowerups() {
		possiblePowerups = new ArrayList<Ability>();
		upgradeTypes = new ArrayList<String>();
		upgradeTypes.add(Ability.HEALTH_UPGRADE_TAG);
		upgradeTypes.add(Ability.MAGIC_UPGRADE_TAG);
		upgradeTypes.add(Ability.ATTACK_UPGRADE_TAG);
		upgradeTypes.add(Ability.DEFEND_UPGRADE_TAG);
		upgradeTypes.add(Ability.SPEED_UPGRADE_TAG);
		upgradeTypes.add(Ability.STEALTH_UPGRADE_TAG);
		
		for (String powerupName : creature.powerups.split("\\.")) {
			int abilityId = Ability.getIdForName(powerupName);
			if (abilityId >= 0) {
				Ability ability = new Ability(abilityId, null, 1, dungeonEvents, dungeonQuery);
				StatAbilityInfo si = ability.getStatInfo();
				if (si == null) {
					possiblePowerups.add(ability);
				} else {
					upgradeTypes.add(si.statType);
				}
			}
		}
	}
	
	public boolean isReadyForTurn() {
		int speedCheck = calculateSpeed() + missedTurns; 

		// every time this creature misses a turn, it is more likely to get a turn next time.
		// this is to help prevent randomness depriving a creature of a turn for an undesirably long time
	
        if (Randy.getRand(1, MAX_SPEED) <= speedCheck || missedTurns >= 20) {
        	missedTurns = 0;
        	return true;
        } else {
        	missedTurns += 2;
        	return false;
        }
	}
	
	public HighlightStatus getHighlightStatus() {
		return highlightStatus;
	}
	
	public abstract void processTurn();
	public abstract boolean canSkipTurn();
	// calculates changes to creature stats, and returns true if creature still alive, false otherwise
	// called by processTurn.
	public boolean processStatsForTurn() {
		// all creatures get a chance to modify their stats first, according
		// to their current abilities.
		// first of all, health and magic get a 1:5 chance of increasing,
		// all creatures get this
		int maxHealth = calculateMaxHealth();
		int maxMagic = calculateMaxMagic();

		if (Randy.getRand(1, 50) < maxHealth)
			health++;

		if (Randy.getRand(1, 50) < maxMagic)
			magic++;

		// next some abilities might modify health and magic, such as
		// healing powers or poison
		health = modifyHealth();
		magic = modifyMagic();

		// lastly, we have to put a cap on it
		if (health > maxHealth)
			health = maxHealth;

		if (magic > maxMagic)
			magic = maxMagic;

		if (magic < 1)
			magic = 1;

		if (health < 1) {
			death();
			return false;
		}
		
		setCreatureStats();
		boolean tempAbilityActive = calculateHighlightAbility();
		
		// set a flag that could help monsters not bother having their turn.
		if (tempAbilityActive || health < maxHealth || magic < maxMagic) {
			creatureIsStable = false;
		} else {
			creatureIsStable = true;
		}
		
		// If we get to here, we have to wait for player input
		if (isNotHiding() == false) {
			addAmbushAbility();
		}
		
		return true;
	}

	private void setCreatureStats() {
		creatureStats.name = creature.name;
		int maxHealth = calculateMaxHealth();
		int maxMagic = calculateMaxMagic();
		creatureStats.health = health;
		creatureStats.maxHealth = maxHealth;
		creatureStats.magic = magic;
		creatureStats.maxMagic = maxMagic;
		creatureStats.experience = experience; 
	}
	
	private DungeonPosition		tempPos = new DungeonPosition(0, 0);
	/**
	 * Is it possible to move to the direction indicated?  And is it OK to move into a chaacter?
	 */
	public boolean canMove(int intendedDirection, boolean canBeChar)
	{
		tempPos.x = mapPosition.x;  // this function gets called a lot so this is a little efficiency thing.
		tempPos.y = mapPosition.y;
		tempPos.applyDirection(tempPos, intendedDirection);
		
		switch (dungeonQuery.whatIsAtLocation(tempPos)) {
			case MONSTER:
			case WALL:
				return false;
			case CHARACTER:
				if (canBeChar) {
					return true;
				} else {
					return false;
				}
			case HOLE:
				return creatureCanFly();
			default:
				return true;		
		}
	}
	

	// Will find the best direction to move in for this creature to get from where it currently is, to
	// the position passed in, taking into account obstacles appropriate for Monsters or Characters.
	// i.e. a monster will try to pass through (attack) a character whereas another character wont.
	// If there is no possible way to move semi-directly towards the target, this function will return NO_DIR
	// If the position passed in is null, you get random direction
	// It calls should move which indicates that if the position it moves to means the target wont be visible when
	// it gets there, then it wont offer that as an option.
	public int findBestDirection(DungeonPosition characterPosition, boolean canBeCharacter, CanMoveStrategy s)
	{
		int direction = DungeonPosition.NO_DIR;
		
		if (null == characterPosition)
		{
			direction = Randy.getRand(DungeonPosition.WEST, DungeonPosition.SOUTHWEST);
			if (canMove(direction, canBeCharacter)) {
				return direction;
			} else {
				return DungeonPosition.NO_DIR;
			}
		}

		// means move up or down.  unless there is something in the way, in which case deviate around it.
		if (characterPosition.x == mapPosition.x)
		{
			if (characterPosition.y > mapPosition.y)
			{
				if (s.checkMove(DungeonPosition.NORTH, canBeCharacter, characterPosition))
					direction = DungeonPosition.NORTH;
				else if (s.checkMove(DungeonPosition.NORTHWEST, canBeCharacter, characterPosition))				
					direction = DungeonPosition.NORTHWEST;
				else if (s.checkMove(DungeonPosition.NORTHEAST, canBeCharacter, characterPosition))
					direction = DungeonPosition.NORTHEAST;		
			}
			else
			{
				if (s.checkMove(DungeonPosition.SOUTH, canBeCharacter, characterPosition))
					direction = DungeonPosition.SOUTH;
				else if (s.checkMove(DungeonPosition.SOUTHWEST, canBeCharacter, characterPosition))			
					direction = DungeonPosition.SOUTHWEST;
				else if (s.checkMove(DungeonPosition.SOUTHEAST, canBeCharacter, characterPosition))
					direction = DungeonPosition.SOUTHEAST;
			}
		}
		// character is west of current position
		else if (characterPosition.x < mapPosition.x)
		{
			if (characterPosition.y == mapPosition.y)
			{
				if (s.checkMove(DungeonPosition.WEST, canBeCharacter, characterPosition))
					direction = DungeonPosition.WEST;
				else if (s.checkMove(DungeonPosition.SOUTHWEST, canBeCharacter, characterPosition))				
					direction = DungeonPosition.SOUTHWEST;
				else if (s.checkMove(DungeonPosition.NORTHWEST, canBeCharacter, characterPosition))
					direction = DungeonPosition.NORTHWEST;
			}
			else if (characterPosition.y > mapPosition.y)  // character is north of current pos
			{
				if (s.checkMove(DungeonPosition.NORTHWEST, canBeCharacter, characterPosition))
					direction = DungeonPosition.NORTHWEST;
				else if (s.checkMove(DungeonPosition.WEST, canBeCharacter, characterPosition))			
					direction = DungeonPosition.WEST;
				else if (s.checkMove(DungeonPosition.NORTH, canBeCharacter, characterPosition))
					direction = DungeonPosition.NORTH;
			}
			else
			{
				if (s.checkMove(DungeonPosition.SOUTHWEST, canBeCharacter, characterPosition))
					direction = DungeonPosition.SOUTHWEST;
				else if (s.checkMove(DungeonPosition.WEST, canBeCharacter, characterPosition))				
					direction = DungeonPosition.WEST;
				else if (s.checkMove(DungeonPosition.SOUTH, canBeCharacter, characterPosition))
					direction = DungeonPosition.SOUTH;
			}
		}
		// character is east of current position
		else if (characterPosition.x > mapPosition.x)
		{
			if (characterPosition.y == mapPosition.y)
			{
				if (s.checkMove(DungeonPosition.EAST, canBeCharacter, characterPosition))
					direction = DungeonPosition.EAST;
				else if (s.checkMove(DungeonPosition.SOUTHEAST, canBeCharacter, characterPosition))				
					direction = DungeonPosition.SOUTHEAST;
				else if (s.checkMove(DungeonPosition.NORTHEAST, canBeCharacter, characterPosition))
					direction = DungeonPosition.NORTHEAST;
			}
			else if (characterPosition.y > mapPosition.y)  // character is north of current pos
			{
				if (s.checkMove(DungeonPosition.NORTHEAST, canBeCharacter, characterPosition))
					direction = DungeonPosition.NORTHEAST;
				else if (s.checkMove(DungeonPosition.NORTH, canBeCharacter, characterPosition))				
					direction = DungeonPosition.NORTH;
				else if (s.checkMove(DungeonPosition.EAST, canBeCharacter, characterPosition))
					direction = DungeonPosition.EAST;
			}
			else
			{
				if (s.checkMove(DungeonPosition.SOUTHEAST, canBeCharacter, characterPosition))
					direction = DungeonPosition.SOUTHEAST;
				else if (s.checkMove(DungeonPosition.SOUTH, canBeCharacter, characterPosition))				
					direction = DungeonPosition.SOUTH;
				else if (s.checkMove(DungeonPosition.EAST, canBeCharacter, characterPosition))
					direction = DungeonPosition.EAST;
			}
		}

		return direction;

	}

	protected int oppositeDirection(int direction)
	{
		DungeonPosition pos = mapPosition;

		// Identify the exact opposition direction
		switch (direction) {
			case DungeonPosition.NORTH:
				pos = new DungeonPosition(mapPosition, DungeonPosition.SOUTH);
				break;
			case DungeonPosition.SOUTH:
				pos = new DungeonPosition(mapPosition, DungeonPosition.NORTH);
				break;
			case DungeonPosition.EAST:
				pos = new DungeonPosition(mapPosition, DungeonPosition.WEST);
				break;
			case DungeonPosition.WEST:
				pos = new DungeonPosition(mapPosition, DungeonPosition.EAST);
				break;
			case DungeonPosition.NORTHEAST:
				pos = new DungeonPosition(mapPosition, DungeonPosition.SOUTHWEST);
				break;
			case DungeonPosition.NORTHWEST:
				pos = new DungeonPosition(mapPosition, DungeonPosition.SOUTHEAST);
				break;
			case DungeonPosition.SOUTHEAST:
				pos = new DungeonPosition(mapPosition, DungeonPosition.NORTHWEST);
				break;
			case DungeonPosition.SOUTHWEST:
				pos = new DungeonPosition(mapPosition, DungeonPosition.NORTHEAST);
				break;
		}

		// find the best path to get there
		return findBestDirection(pos, true, canMove);
	}
	
	@Override
	public String getNameUnderscore() {
		String underName = creature.name.replace(" ", "_");
		return underName;
	}
	
	public DungeonPosition getPosition() {
		return mapPosition;
	}
	
	public void setPosition(DungeonPosition newPosition) {
		mapPosition = new DungeonPosition(newPosition);
		// remove existing cover effects.
		Iterator<Ability> iter = abilities.iterator();
		while (iter.hasNext()) {
			Ability ability = iter.next();
			if (ability.hasTag(Ability.COVER_TAG)) {
				iter.remove();
			}
		}
	
		// add new cover effects
		if (dungeonQuery != null && creatureCanFly() == false && creatureSize != CreatureSize.HUGE) {
			RoughTerrainType roughTerrainType = dungeonQuery.getTerrainAtLocation(mapPosition);
			if (roughTerrainType != null) {
				String size = "medium";
				if (creatureSize == CreatureSize.SMALL) {
					size = "small";
				}
				switch (roughTerrainType) {
				case BONES:
					Ability bones = new Ability(Ability.getIdForName("cover (bones) "+size), this, 1, dungeonEvents, dungeonQuery);
					addAbility(bones, null);
					break;
				case ROCKS:
					Ability rocks = new Ability(Ability.getIdForName("cover (rocks) "+size), this, 1, dungeonEvents, dungeonQuery);
					addAbility(rocks, null);
					break;
				default:
					break;
				}
			}
		}
	}
	
	protected boolean canChargeAcross(DungeonPosition position, boolean creatureCanCharge) {
		boolean result = false;
		
		if (creatureCanCharge) {
			if (dungeonQuery.getLocation(position).hasRoughTerrain()) {
				result = creatureCanFly();
			} else {
				result = true;
			}
		}
		return result;
	}
	
	protected boolean canDashInto(DungeonPosition position) {
		boolean result = false;
		AtLocation atLocation = dungeonQuery.whatIsAtLocation(position);
		if (atLocation == AtLocation.HOLE) {
			result = creatureCanFly();
		} else if (atLocation == AtLocation.FREE) {
			result = true;
		}
		return result;
	}
	
	protected Ability canDash() {
		Ability result = null;
		for (Ability ability : abilities) {
			if (ability.hasTag(Ability.DASH_TAG)) {
				if (ability.set) {
					result = ability;
				}
			}
		}
		return result;
	}
	
	protected boolean performCharge(DungeonPosition position, int direction, AtLocation targetType, Character releventCharacter) {
		
		if (isNotHiding() == false) {
			return false; // hiding creatures dont charge.
		}
		
		DungeonPosition furtherPosition = new DungeonPosition(position, direction);
		if (dungeonQuery.whatIsAtLocation(furtherPosition) == targetType && canChargeAcross(position, canCharge)) {
			notHiding(releventCharacter);
			dungeonEvents.creatureMove(SequenceNumber.getNext(), releventCharacter, this, mapPosition, position, direction, Dungeon.MoveType.CHARGE_MOVE, null);
			makeMeleeAttack(dungeonQuery.getCreatureAtLocation(furtherPosition));
			return true;
		}
		
		return false;
	}

	// this is only going to be called if the dash ability should be set
	protected boolean performDash(DungeonPosition position, int direction, Character releventCharacter) {
		if (LOG) L.log("position: %s", position);
		DungeonPosition furtherPosition = new DungeonPosition(position, direction);
		Ability dashAbility = canDash();
		if (dashAbility != null && canChargeAcross(position, true) && canDashInto(furtherPosition)) {
			notHiding(releventCharacter);
			dungeonEvents.creatureMove(SequenceNumber.getNext(), releventCharacter, this, mapPosition, furtherPosition, direction, Dungeon.MoveType.NORMAL_MOVE, null);
			dashAbility.set = true;
			dashAbility.executeAbility();
			return true;
		}
		return false;
	}
	
	public void addExperience(int exp, boolean initialExp)
	{
		// modify the added experience depending on the type of creature
		int percentage = 100;

		if (getCreature().humanoid == 0)
			percentage += 10;

		if (getCreature().hands == 0)
			percentage += 25;

		if (getCreature().head == 0)
			percentage += 15;

		experience += exp;
		int xp = ((experience * percentage) / 100);
		expRoot = (int)Math.sqrt(xp);// pre-calculate the squre root of the experience which is used in many places

		// we need to modify the creatures health and maxheath based on initial experience
		if (initialExp) {
			health = calculateMaxHealth();
			magic = calculateMaxMagic();
		}
		
		creatureStats.experience = experience;
	}

	// if you want to make the attack disregard defenders skill, simply set the
	// attackers skill level to 32000
	public int respondAttack(AbilityCommand attack, Creature attacker) {
		// If monster hasnt had a turn yet (say after new game load, it may need to find the closest character to respond to an attack
		// so lets set that now.
		if (getReleventCharacter() == null) {
			setReleventCharacter();
		}
		
		int defenceSkill = 0;
		if (attack.ability != null && attack.ability.isAimed() && attack.ability.isTargetable()) {
			defenceSkill = calculateDefenceAgainstMissilesSkill();
		} else {
			defenceSkill = calculateDefenceSkill();
		}
		
		int rndAttack = Randy.getRand(1, attack.skill);
		int rndDefence = Randy.getRand(1, defenceSkill);
		
		if (LOG) L.log("ATTACK!! rndAttack:%s/%s rndDefence: %s/%s", rndAttack, attack.skill, rndDefence, defenceSkill);
		
		if (rndAttack > rndDefence) // attack hits
		{
			int newDamage = attack.damage;
			int abilityCommand = AbilityCommand.NO_PHYSICAL_ATTACK;

			if (attack.type == AbilityCommand.HARD_ATTACK) {
				abilityCommand = AbilityCommand.RESIST_HARD;
			}

			if (attack.type == AbilityCommand.CHEMICAL_ATTACK) {
				abilityCommand = AbilityCommand.RESIST_CHEMICAL;
			}

			if (attack.type == AbilityCommand.SHARP_ATTACK) {
				abilityCommand = AbilityCommand.RESIST_SHARP;
			}

			if (attack.type == AbilityCommand.ENERGY_ATTACK) {
				abilityCommand = AbilityCommand.RESIST_ENERGY;
			}

			if (abilityCommand != AbilityCommand.NO_PHYSICAL_ATTACK) {
				// apply damage reduce against burst with cover
				if (LOG) L.log("before damage reduce newDamage: %s", newDamage);
				if (attack.ability != null && attack.ability.isBurstEffect()) {
					newDamage = reduceDamage(AbilityCommand.RESIST_BURST, newDamage);
					if (LOG) L.log("burst reduced newDamage: %s", newDamage);
				}
				
				// reduce damage by any ability which reduces damage of that type
				newDamage = reduceDamage(abilityCommand, newDamage);
				if (LOG) L.log("final newDamage: %s", newDamage);
			} 

			if (newDamage < 1) {
				newDamage = 0;
			}
			
			if (newDamage > 99)
				newDamage = 99;

			// there is some kind of attack
			if (abilityCommand != AbilityCommand.NO_PHYSICAL_ATTACK) {	
				// Tell the gui about the damage
				dungeonEvents.damageInflicted(SequenceNumber.getCurrent(), attacker.getReleventCharacter(), mapPosition, attack.type, newDamage);	
			}
			
			if (attack.abilityToAdd != null) {
				if (attacker.giveAbility(this, attack.abilityToAdd, this.mapPosition, 0, attacker)) {
					// hitTime = 400;
					// give attacker experience for inflicting ability
					//attacker.addExperience(getExpValue() / 30 + 1, false); 
				}
			}

			health -= newDamage;
			
			if (health < 1) {
				// give killer the experience for killing you
				turnProcessor.addExperience(getExpValue());
				death();
				return 0;
			}

			return newDamage;
		}

		// indicates a miss
		dungeonEvents.missed(SequenceNumber.getCurrent(), attacker.getReleventCharacter(), mapPosition);
		
		return -1;
	}
	
	public int getExpValue() {
		return 10 * calcLevel(myId) + experience / 20;
	}

	public int getLevelValue() {
		return calcLevel(myId);
	}

	public boolean addAbility(Ability ability, Creature giver)
	{
		if (LOG) L.log("ability: %s, giver: %s", ability,giver);
		// first work out whether you have any ability which will prevent this
		// ability from being added (such as resist poison)
		AbilityCommand ac = new AbilityCommand(AbilityCommand.RESIST_ABILITY, ability.myId, 1, 1, 1);
		
		for (int i = 0; i < abilities.size(); i++) {
			if (abilities.get(i).executeCommandValue(ac, this) == Ability.RESISTED) {
				if (giver != null) {
					dungeonEvents.abilityResisted(SequenceNumber.getCurrent()+1, giver.getReleventCharacter(), ability.getEffectType(), mapPosition);
				}
				return false;
			}
		}
		
		if (ability.isRoughTerrain()) {
			if (creatureCanFly()) {
				return false;
			}
		}
		
		if (ability.hasTag(Ability.KNOCKEDBACK_TAG)) {
			performKnockback(ability, giver);
			if (giver != null) {
				dungeonEvents.abilityAdded(SequenceNumber.getCurrent()+1, giver.getReleventCharacter(), ability.getEffectType(), mapPosition);
			}
			return true;
		}	
	
		if (LOG) L.log("adding ability: %s to creature:%s", ability , this);
		abilities.add(ability);
		setAbilityFlags();
		ability.setOwned(this, true);
		
		// OK, what if this is an ability that should be invoked straight away (ie. wand of health?)
		if (ability.dynamicParams.containsKey(Ability.INVOKE_TAG)) {
			ability.abilitySelected(this);
		} else {
			if (giver != null) {
				dungeonEvents.abilityAdded(SequenceNumber.getCurrent()+1, giver.getReleventCharacter(), ability.getEffectType(), mapPosition);
			}
			
			if (mapPosition != null && dungeonQuery.getTerrainAtLocation(mapPosition) == RoughTerrainType.HOLE) {
				if (creatureCanFly() == false) {
					death();
				}
			}
		}
		
		return true;
	}

	public void removeAbility(String name) {
		Ability remove = null;
		for (Ability ability : abilities) {
			if (ability.ability.name.equals(name)) {
				remove = ability;
				break;
			}
		}
		if (remove != null) {
			abilities.remove(remove);
		}
	}
	
	public void broadcastAbilityCommand(AbilityCommand command) {
		for (int i = 0; i < abilities.size(); i++) {
			abilities.get(i).executeCommandValue(command, this);
		}
	}

	public void destroyAbility(Ability ability) {
		abilities.remove(ability);
		setAbilityFlags();
	}
	
	private void setAbilityFlags() {
		canFly = false;
		canCharge = false;
		
		for (Ability ability : abilities) {
			if (ability.hasTag(Ability.FLIGHT_TAG)) {
				canFly = true;
			}
			
			if (ability.hasTag(Ability.CHARGE_TAG)) {
				canCharge = true;
			}
		}
	}
	
	protected boolean creatureHasAbilityTag(String tag) {
		for (Ability ability : abilities) {
			if (ability.hasTag(tag)) {
				return true;
			}
		}
		
		return false;
	}
	
	// when an ability has an instant effect that modifies an attribute
	// absolutely, it should call this
	// updating stats is in response to abilities, which dont have an instant effect on current health and magic
	// current stats are only modified when the creature has its turn.  
	public boolean updateStats(Ability ab, DungeonPosition pos)
	{
		int oldHealth = health;
		int oldMagic = magic;
		boolean someEffect = false;
		int maxHealth = calculateMaxHealth();
		int maxMagic = calculateMaxMagic();

		// if stats change as a result of a setter toggle, then current health and magic should not change
		// only max values should change.  However, if an ability such as a potion is applied, then they can and
		// should.
		if (ab != null) {
			health = modifyHealth();
			
			if (health > maxHealth)
				health = maxHealth;
					
			if (health != oldHealth) // has some sort of effect
				someEffect = true;
			
			magic = modifyMagic();

			if (magic > maxMagic)
				magic = maxMagic;
		
			if (magic != oldMagic) // has some sort of effect
				someEffect = true;
		}

		if ((maxHealth != creatureStats.maxHealth) || (maxMagic != creatureStats.maxMagic))
			someEffect = true;
		
		if (someEffect) {
			creatureStats.health = health;
			creatureStats.maxHealth = maxHealth;
			creatureStats.magic = magic;
			creatureStats.maxMagic = maxMagic;
			
			// its debatable whether we want to show an effect if it didnt change the stats.  Like a health potion when you are at full health
			// if we DONT, move the dungen event line of code below to here
		}
		
		creatureStats.experience = experience;
		
		if (calculateHighlightAbility()) {
			someEffect = true;
		}
		
		// at the moment, show it going off anyway
		if (ab != null) {
			dungeonEvents.abilityAdded(SequenceNumber.getNext(), this.getReleventCharacter(), ab.getEffectType(), pos);
		}
		
		return someEffect;
	}

	/**
	 * Knockback has been applied to the creature.  
	 */
	private void performKnockback(Ability ability, Creature attacker) {
		DungeonPosition targetPos = (DungeonPosition) ability.dynamicParams.get(Ability.TARGET_POS);
		DungeonPosition sourcePos = attacker.getPosition();
		if (targetPos == null) {
			targetPos = sourcePos;
		}
		int knockbackDir;
		
		if (targetPos.equals(mapPosition)) {
			knockbackDir = calcMostAccurateDir(sourcePos, mapPosition);
		} else {
			knockbackDir = calcMostAccurateDir(targetPos, mapPosition);
		}
		
		if (knockbackDir == DungeonPosition.NO_DIR) {
			knockbackDir = Randy.getRand(DungeonPosition.WEST, DungeonPosition.SOUTHWEST);
		}
		
		//lets just move
		DungeonPosition oldPosition = mapPosition;
		DungeonPosition newPosition = new DungeonPosition(mapPosition, knockbackDir);
		int damageType = AbilityCommand.HARD_ATTACK;
		int collisionDamage = calculateBaseHealth()/4+1;
		int myCollisionDamage = reduceDamage(AbilityCommand.RESIST_HARD, collisionDamage);
		AbilityCommand collideCommand = new AbilityCommand(AbilityCommand.HARD_ATTACK, collisionDamage, true, null, null);
		collideCommand.skill = 30000;  // auto hit
		Creature hitCreature = dungeonQuery.getCreatureAtLocation(newPosition);
		dungeonEvents.damageInflicted(SequenceNumber.getCurrent(), getReleventCharacter(), oldPosition, AbilityCommand.KNOCKBACK, IDungeonEvents.NO_DAMAGE);
		switch (dungeonQuery.whatIsAtLocation(newPosition)) {
			case FREE:
				dungeonEvents.creatureMove(SequenceNumber.getCurrent(), getReleventCharacter(), this, mapPosition, newPosition, knockbackDir,  Dungeon.MoveType.KNOCKBACK_MOVE, null);
				break;
			case MONSTER:
				health -= myCollisionDamage;
				dungeonEvents.creatureMove(SequenceNumber.getCurrent(), getReleventCharacter(), this, mapPosition, mapPosition, knockbackDir,  Dungeon.MoveType.SHUDDER_MOVE, null);
				dungeonEvents.damageInflicted(SequenceNumber.getCurrent(), attacker.getReleventCharacter(), oldPosition, damageType, myCollisionDamage);	
				hitCreature.respondAttack(collideCommand, attacker);
				break;
			case CHARACTER:
				health -= myCollisionDamage;
				dungeonEvents.creatureMove(SequenceNumber.getCurrent(), getReleventCharacter(), this, mapPosition, mapPosition, knockbackDir,  Dungeon.MoveType.SHUDDER_MOVE, null);
				dungeonEvents.damageInflicted(SequenceNumber.getCurrent(), attacker.getReleventCharacter(), oldPosition, damageType, myCollisionDamage);	
				hitCreature.respondAttack(collideCommand, attacker);
				break;
			case WALL:
				health -= myCollisionDamage;
				dungeonEvents.creatureMove(SequenceNumber.getCurrent(), getReleventCharacter(), this, mapPosition, mapPosition, knockbackDir,  Dungeon.MoveType.SHUDDER_MOVE, null);
				dungeonEvents.damageInflicted(SequenceNumber.getCurrent(), attacker.getReleventCharacter(), oldPosition, damageType, myCollisionDamage);	
				break;
			case HOLE:
				dungeonEvents.creatureMove(SequenceNumber.getCurrent(), getReleventCharacter(), this, mapPosition, newPosition, knockbackDir,  Dungeon.MoveType.KNOCKBACK_MOVE, null);
				if (creatureCanFly() == false) {
					death();
				}
				break;
		}
	}
	
	protected int calcMostAccurateDir(DungeonPosition sourcePos, DungeonPosition destPos) {

		if (sourcePos.equals(destPos)) {
			return DungeonPosition.NO_DIR;
		}
		
		int difX = destPos.x - sourcePos.x;
		int difY = destPos.y - sourcePos.y;
		int abDifX = Math.abs(difX);
		int abDifY = Math.abs(difY);
		
		// NORTH OR SOUTH
		if (abDifY >= 2*abDifX) {
			if (difY >= 0) {
				return DungeonPosition.NORTH;
			} else {
				return DungeonPosition.SOUTH;
			}
		}
		
		// EAST OR WEST
		if (abDifX >= 2*abDifY) {
			if (difX >= 0) {
				return DungeonPosition.EAST;
			} else {
				return DungeonPosition.WEST;
			}
		}
		
		// NORTHEAST OR SOUTHEAST
		if (difX >= 0) {
			if (difY >= 0) {
				return DungeonPosition.NORTHEAST;
			} else {
				return DungeonPosition.SOUTHEAST;
			}
		}
		
		// NORTHWEST OR SOUTHWEST
		if (difX < 0) {
			if (difY >= 0) {
				return DungeonPosition.NORTHWEST;
			} else {
				return DungeonPosition.SOUTHWEST;
			}
		}

		return DungeonPosition.NO_DIR;
	}
	
	// The highlighted ability effect will be either the latest ability added to the character, or the temporary ability
	// with the least turns left to countDown
	// returns true if highlighted ability has changed
	protected abstract boolean calculateHighlightAbility();
	
	
	// ATTRIBUTES

	// name, gifname, colour, head, hands, humanoid, swarm, maxHealth, maxMagic,
	// speed, attack, defence, n x (abilities *)
	/*
	 * static String creatures =
	 * "human warrior,a,7,1,1,1,0,10,10,4,10,10,1,2,3,13,16,15,*"// human
	 * warrior: bash , sword , leather armor , wand: smashing , amulet: foulness
	 * , poison blast , +"troll,b,7,1,1,1,0,25,5,3,10,5,1,4,*"// troll: bash ,
	 * giant club , +"wild dog,c,6,1,0,0,1,5,3,5,5,9,5,*"// wild dog: bite , ;
	 */
	
	// METHODS
	protected int calculateAttackSkill() {
		int newAttack = attackSkill + (attackSkill * getExpFactor()) / 100;
		return modifyValue(AbilityCommand.MODIFY_ATTACK_SKILL, newAttack);
	}
	
	public int calculateDefenceSkill() {
		int newDefence = defenceSkill + (defenceSkill * getExpFactor()) / 100;
		int dd = modifyValue(AbilityCommand.MODIFY_DEFENCE_SKILL, newDefence); 
		return dd;
	}
	
	public int calculateDefenceAgainstMissilesSkill() {
		int defence = calculateDefenceSkill();	
		defence = modifyValue(AbilityCommand.MODIFY_MISSILE_DEFENCE, defence); 
		return defence;
	}

	protected int modifyHealth() {
		return modifyValue(AbilityCommand.MODIFY_HEALTH, health);
	}

	protected int modifyMagic() {
		return modifyValue(AbilityCommand.MODIFY_MAGIC, magic);
	}

	public int calculateMaxHealth() {
		return modifyValue(AbilityCommand.MODIFY_MAX_HEALTH, calculateBaseHealth());
	}
	
	public int calculateBaseHealth() {
		return  maximumHealth + (maximumHealth * getExpFactor()) / 100;
	}

	public int calculateMaxMagic() {
		int newMaxMag = maximumMagic + (maximumMagic * getExpFactor()) / 100;
		return modifyValue(AbilityCommand.MODIFY_MAX_MAGIC, newMaxMag);
	}

	public int calculateSpeed() {
		return modifyValue(AbilityCommand.MODIFY_SPEED, speed);
	}
	
	public int calculateStealth() {
		return modifyValue(AbilityCommand.MODIFY_STEALTH, stealth);
	}
	
	public int calculateDetect() {
		return modifyValue(AbilityCommand.MODIFY_DETECT, detect);
	}
	
	public int getExpFactor() {
		return expRoot;
	}
	
	public boolean isSolid() {
		return true;
	}

	public boolean giveAbility(Creature target, Ability ab, DungeonPosition pos, int magicCost, Creature giver) {
		boolean addedOk = true;
		
		if (target != null){
			addedOk = target.addAbility(ab, giver);
			
			// There is an edge case where adding the creature adds an ability to itself, which will end its turn,
			// and in the end turn processing, the tic counter for temp abilities is decremented, effectively
			// giving the ability 1 less turn that it should.  So we increment the tick counter here first.
			if ((target == this) && (ab.ability.duration > 0)) {
				ab.changeTicksLeft(1);  // add 1 to the tick counter
			}
		}
		
		return addedOk;
	}

	// When a creature uses magic, by virtue of executing an ability, this is called.
	public void usedMagic(int magicCost) {
		magic -= magicCost;
		
		if (magic < 0) {
			magic = 0;
		}
	}
	
	// this is the function that most abilities will call to carry out their
	// effect on a target or targets
	// it can be as simple as a melee attack on the target creature, or it may
	// involve a magic cost to the attacker
	// also, if the attack is successful, it may be a physical attack or the
	// addition of an ability, of possibly both.
	public boolean makeAttack(AbilityCommand attack, Creature target, DungeonPosition targetPosition, boolean useSkill, int magicCost, int magicUse)
	{
		boolean result = true;

		// calculate damage before changing attack skill to enormous
		attack.skill = calculateAttackSkill();

		if (attack.type != AbilityCommand.NO_PHYSICAL_ATTACK)
		{
			// rules for damage:
			// 1. creatures size only affects damage of melee attacks
			if (attack.melee) {
				attack.damage += calculateSizeDamageBonus();
			}

			// 2. creatures attack skill only affects damage of aimed
			// attacks
			// 3. skill effect on damage is much less than size affect on
			// damage

			if (useSkill) {
				Ability.AbilityType abilityType = attack.ability.getAbilityType();
				if (abilityType == AbilityType.RANGED || abilityType == AbilityType.WAND) {
					attack.damage += calculateSkillMissileDamageBonus();
				} else {
					attack.damage += calculateSkillDamageBonus();
				}
			}
			
			// 4. creatures magical capability (maxMagic) effects damage of
			// magical attacks (that have a magic cost)  [knockback excluded]
			if (attack.type != AbilityCommand.KNOCKBACK && magicUse > 0) {
				attack.damage += calculateMagicDamageBonus();
			}
		}

		// TODO really?  there is no real need for this.
		if (target == null) // this is an attack on empty air. too bad, it still counts
		{
			if (attack.type != AbilityCommand.NO_PHYSICAL_ATTACK)
			{
			//	makeHit(attack.type, attack.damage, targetPosition, 800);
				// dungeonQuery.getSoundControl().playSound(GameControl.HITSOUND);
			}
			else
			{
			//	dungeonEvents.abilityAdded(SequenceNumber.getCurrent(), giver.getReleventCharacter(), abilityEfectType, mapPosition, null);
			}
		}
		else
		{
			if (!useSkill) {
				attack.skill = 32000; // sure thing
			}

			attack.skill += L.TEST_SKILL_BONUS;
			target.respondAttack(attack, this);
		}

		return result;
	}

	static int	index;
	static int	endIndex;

	protected int calculateSizeDamageBonus() {
		int bonus = (calculateMaxHealth() * calculateMaxHealth()) / 150;
		return bonus;
	}
	
	protected int calculateSkillDamageBonus() {
		int bonus = calculateAttackSkill() / 5;
		return bonus;
	}
	
	protected int calculateMagicDamageBonus() {
		int bonus = calculateMaxMagic() / 6;
		return bonus;
	}
	
	public int calculateMeleeDamage() {
		Ability theAbility = null;
		
		// First find the set weapon.
		for (Ability ability : abilities) {
			if (ability.isUsed()) {
				if (ability.getAbilityType() == AbilityType.WEAPON) {
					theAbility = ability;
				}
			}
		}
		
		return calculateMeleeDamageForAbility(theAbility);
	}
	
	public int calculateMeleeDamageForAbility(Ability theAbility) {
		int damage = 0;
		if (theAbility != null) {
			damage = theAbility.getAbilityDamage();
			damage += calculateSizeDamageBonus() + calculateSkillDamageBonus();
		}
		
		return damage;
	}
	
	public int calculateMissileDamage(Ability theAbility) {
		
		int damage = 0;
		if (theAbility != null) {
			damage = theAbility.getAbilityDamage();
			
			if (damage > 0) {
				if (theAbility.isAimed()) {
					damage += calculateSkillMissileDamageBonus();
				}

				if (theAbility.isMagical()) {
					damage += calculateMagicDamageBonus();
				}
			}
		}
		
		return damage;
	}
	
	private int calculateSkillMissileDamageBonus() {
		float mod = .75f;
		if (hasAmbushActive()) {
			mod = .4f;
		}
		return (int) (calculateSkillDamageBonus() * mod);
	}
	
	public boolean hasAmbushActive() {
		for (Ability ability : abilities) {
			if (ability.hasTag(Ability.AMBUSH_TAG)) {
				return true;
			}
		}
		return false;
	}
	
	private static int readNextNum(String string) {
		int n;
		endIndex = string.indexOf(",", index);
		n = Integer.parseInt(string.substring(index, endIndex));
		index = endIndex + 1;
		return n;
	}

	private static int addNextCreature(String string, int startIndex) {
		index = startIndex;
		Data cd = new Data();

		// read name
		endIndex = string.indexOf(",", index);
		cd.name = string.substring(index, endIndex);
		index = endIndex + 1;

		// read gif filename
		endIndex = string.indexOf(",", index);
		cd.tag = string.substring(index, endIndex);
		index = endIndex + 1;

		// read gif colour
		endIndex = string.indexOf(",", index);
		cd.powerups = string.substring(index, endIndex);
		index = endIndex + 1;

		// read head
		cd.head = readNextNum(string);

		// read hands
		cd.hands = readNextNum(string);

		// read humanoid
		cd.humanoid = readNextNum(string);

		// read swarm
		cd.swarm = readNextNum(string);

		// read Max Health
		cd.maximumHealth = readNextNum(string);

		// read Max Magic
		cd.maximumMagic = readNextNum(string);

		// read speed
		cd.speed = readNextNum(string);

		// read attack
		cd.attackSkill = readNextNum(string);

		// read defence
		cd.defenceSkill = readNextNum(string);

		// read value
		cd.value = readNextNum(string);
		
		// read stealth
		cd.stealth = readNextNum(string);

		// read detect
		cd.detect = readNextNum(string);
		
		// read starter
		cd.starter = readNextNum(string);

		creatureData.addElement(cd);

		// read an arbitary number of abilitiy ids
		cd.abilityIds = new int[10];
		for (int i = 0; i < 10; i++)
			cd.abilityIds[i] = -1;

		endIndex = string.indexOf("*", index);
		int finalIndex = endIndex + 1;
		String abilities = string.substring(index, endIndex);

		index = 0;
		int i = 0;

		do {
			endIndex = abilities.indexOf(",", index);
			if (endIndex > -1) {
				cd.abilityIds[i] = Integer.parseInt(abilities.substring(index, endIndex));
				i++;
				index = endIndex + 1;
			}
		} while (endIndex != -1);

		return finalIndex;
	}

	public static void initializeData() {
		if (dataInitialized == false) {
			int index = 0;
			String creatures = new TextResourceIdentifier("c.txt").getFileContents();

			while (index < creatures.length()) {
				index = addNextCreature(creatures, index);
			}

			dataInitialized = true;
		}
	}

	/*
	 * public static void printAll() { for (int i=0; i<creatureData.size(); i++)
	 * { CreatureData cd = (CreatureData) creatureData.elementAt(i);
	 * if (LOG) Logger.log(cd.name + "level = " + calcLevel(i)); }
	 * 
	 * }
	 */
	protected int modifyValue(int commandName, int commandValue) {
		AbilityCommand command = new AbilityCommand(commandName, commandValue, getCreature().head, getCreature().hands, getCreature().humanoid);

		// first we apply multiply abilitities 
		for (int i = 0; i < abilities.size(); i++) {
			command.value = abilities.get(i).executeCommandValueMultiply(command, this);
		}
		
		// then add to the result.
		for (int i = 0; i < abilities.size(); i++) {
			command.value = abilities.get(i).executeCommandValue(command, this);
		}

		return command.value;
	}

	protected int reduceDamage(int damageType, int damageValue) {
		int damage = damageValue * (100 - modifyValue(damageType, 0)) / 100;
		return damage;
	}
	
	// To calculate current protection values
	public int calcProtection(int damageType) {		
		return 100-reduceDamage(damageType, 100);
	}
	
	protected static int calcLevel(int id) {
		Data cd = (Data) creatureData.elementAt(id);
		return cd.value;
	}
	
	protected boolean makeMeleeAttack(Creature target) {
		boolean attackComplete = false;
		
		AbilityCommand command = new AbilityCommand(AbilityCommand.MELEE_ATTACK, 0, getCreature().head, getCreature().hands, getCreature().humanoid);

		// first try to attack with the currently selected melee ability
		for (int i = 0; !attackComplete && i < abilities.size(); i++) {
			// We dont tell the dungeon about the attack here, becaues the Ability will do that...
			attackComplete = abilities.get(i).executeCommandTarget(command, target, target.getPosition(), this);
		}
		
		return attackComplete;  // not required any more, but I cant be bothered removing it.  Just dont pay it no mind.
	}
	
	protected void death() {
		unsetAbilities(false);
		setDead(true);
		if (this.myId == 0) // Nashkur the evil wizard is dead!!!
			;//dungeonQuery.playerWins();
	}

	public void dropAllPhysicalItems() {
		if (dungeonQuery.whatIsAtLocation(mapPosition) != AtLocation.HOLE) {
			// Drop all physical ability items
			List<Ability> newlyDroppedObjects = new LinkedList<Ability>();
			for (Ability ability : abilities) {
				if (ability.isPhysical()) {
					if (LOG) L.log("dropAllpysical: Ill drop "+ability.ability.name);
					newlyDroppedObjects.add(ability);
				}
			}
			for (Ability ability : newlyDroppedObjects) {
				if (LOG) L.log("newlyDroppedObjects: Ill drop "+ability.ability.name);
				dropObject(ability);
			}
		}
		
		if (getClass() == Monster.class) {
			EventBus.getDefault().event(TutorialPresenter.MONSTER_DIED_EVENT, null);
		}
	}
	
	// Sequence number has no relevance,
	protected void dropObject(Ability ability) {
		abilities.remove(ability);
		ability.setOwned(this, false);
		dungeonEvents.objectDrop(SequenceNumber.getCurrent(), this, ability, mapPosition);
	}

	// Whenever a creature ends its turn, this is called to wind down temporary abilities,
	// such as the effects of speed potions or poison, etc...
	public void endTurn() {
		// this can get called by the character 'nobody'
		if (turnProcessor != null) {
			turnProcessor.waitForAnimsToFinish();
		}
	
		killTempAbilities();
	}
	
	protected void killTempAbilities() {
		for (int i = abilities.size() - 1; i >= 0; i--) {
			Ability ability = abilities.get(i);
			if (ability.tick() == true) // ability needs destroying
				destroyAbility(ability);
		}
	}

	public Data getCreature() {
		return creature;
	}

	public void setCreature(Data creature) {
		this.creature = creature;
	}

	public boolean isDead() {
		return dead;
	}

	public void setDead(boolean dead) {
		this.dead = dead;
	}
	
	public abstract Character getReleventCharacter();
	public void setReleventCharacter() {
	}
	
	public boolean canUseAbility(Ability ability) {
		boolean hasHead = true;
		boolean hasHands = true;
		boolean isHumanoid = true;
		
		if (getCreature().head == 0)
			hasHead = false;
		
		if (getCreature().hands == 0)
			hasHands = false;
		
		if (getCreature().humanoid == 0)
			isHumanoid = false;
		
		boolean usable = ability.meetsNeeds(hasHead, hasHands, isHumanoid);
		
		if (ability.isCooldownAbility() && !ability.isCool()) {
			usable = false;
		}
		
		return usable;
	}

	public boolean hasHead() {
		if (getCreature().head == 0)
			return false;
		else
			return true;
	}
	
	public boolean hasHands() {
		if (getCreature().hands == 0)
			return false;
		else
			return true;
	}
	
	public boolean isHumanid() {
		if (getCreature().humanoid == 0)
			return false;
		else
			return true;
	}
	
	protected boolean creatureCanFly() {
		if (creatureHasAbilityTag(Ability.STUNNED_TAG)) {
			return false;
		}
		return canFly;
	}
	
	public CreatureStats getCreatureStats()
	{
		setCreatureStats();  // make sure they are up to date.
		return creatureStats;
	}
	
	public boolean hasEnoughMagic(Ability ability) {
		if (ability.ability.magicCost > magic) {
			return false;
		} else {
			return true;
		}
	}
	
	public Light getLight() {
		float lightStrength = (float) detect / 6f;
		if (lightStrength > Light.MAX_CREATURE_LIGHT_STRENGTH) {
			lightStrength = Light.MAX_CREATURE_LIGHT_STRENGTH;
		}
		if (lightStrength < Light.MIN_DETECT_LIGHT) {
			lightStrength = Light.MIN_DETECT_LIGHT;
		}
		light = new Light(getPosition(), Light.CHAR_LIGHT_RANGE, lightStrength, false); 
		return light;
	}
	
	public StealthStatus getStealthStatus() {
		return stealthStatus;
	}
	
	@Override
	public boolean isNotHiding() {
		if (stealthStatus == StealthStatus.HIDING) {
			return false;
		} else {
			return true;
		}
	}
	
	public void hide(Character releventCharacter) {
		stealthStatus = StealthStatus.HIDING;
		Ability hidingAbility = new Ability(Ability.getIdForName("hiding"), null, 1, dungeonEvents, dungeonQuery);
		addAbility(hidingAbility, null);
		Vector<Ability.AbilityEffectType> tiptoe = new Vector<Ability.AbilityEffectType>();
		dungeonEvents.processCharacterStealth();
		tiptoe.add(Ability.AbilityEffectType.HIDING);
		dungeonEvents.abilityAdded(SequenceNumber.getNext(), releventCharacter, tiptoe, mapPosition);
		turnProcessor.characterEndsTurn(this);
	}
	
	public void notHiding(Character releventCharacter) {
		if (stealthStatus == StealthStatus.HIDING) {
			stealthStatus = StealthStatus.HIDING_POSSIBLE;
			dungeonEvents.processCharacterStealth();
			dungeonEvents.creatureFound(SequenceNumber.getNext(), releventCharacter, this);
		}
	}
	
	protected void addAmbushAbility() {
		Ability ambushAbility = new Ability(Ability.getIdForName("ambush"), null, 1, dungeonEvents, dungeonQuery);
		addAbility(ambushAbility, null);
	}
	
	// The more visible the tile, the harder it is to hide
	public boolean wasAccidentlyDiscovered;
	public void wasAccidentlyDiscovered() {
		wasAccidentlyDiscovered = true;
	}
	public boolean canHide() {
		boolean result = false;
		if (wasAccidentlyDiscovered) {
			wasAccidentlyDiscovered = false;
			return false;
		}
		
		Location location = dungeonQuery.getLocation(mapPosition);
		float tint = location.getTint();
		int visibility = (int) (tint / 0.03f) - 5;  // 0.3 to 1... = 5 to 28
		if ((calculateStealth() + L.TEST_STEALTH_BONUS) >= visibility) {
			result = true;
		}
		return result;
	}
	
	@Override
	public String toString() {
		return getCreature().name;
	}
}