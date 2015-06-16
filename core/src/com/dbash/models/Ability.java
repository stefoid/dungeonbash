package com.dbash.models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Vector;

import com.dbash.platform.TextResourceIdentifier;
import com.dbash.util.L;
import com.dbash.util.Randy;
import com.dbash.util.SequenceNumber;


@SuppressWarnings({ "unchecked", "rawtypes" })
public class Ability 
{
	public static final boolean LOG = false && L.DEBUG;
	
	// INTERFACE	
	public static final int     RANDOM_ITEM = -2;
	public static final int     RANDOM_MAGIC = -3;
	public static final int		RESISTED = -1;
	
	IDungeonEvents dungeonEvents;
	IDungeonQuery dungeonQuery;
	
	public static String WEAPON_TAG = "aa";
	public static String ARMOR_TAG = "ab";
	public static String WAND_TAG = "ac";
	public static String RANGED_TAG = "ad";
	public static String AMULET_TAG = "af";
	public static String MAGIC_TAG = "ma";
	public static String ROUGH_TERRAIN_TAG = "rough";
	public static String FLIGHT_TAG = "flight";
	public static String HOLE_TAG = "hole";
	public static String CHARGE_TAG = "charge";
	public static String KNOCKBACK_TAG = "knockback";
	public static String KNOCKEDBACK_TAG = "knockedback";
	public static String SLOWED_TAG = "slowed";
	public static String HELD_TAG = "held";
	public static String STUNNED_TAG = "stunned";
	public static String DEFEND_TAG = "defend";
	public static String INVOKE_TAG = "invoke";  // means invoke a just added ability (oneshot) immediately after adding it.
	public static String COVER_TAG = "cover";
	public static String DASH_TAG = "dash";
	public static String SETABLE_TAG = "setable";
	public static String NO_HIGHLIGHT_TAG = "nohighlight";
	public static String AMBUSH_TAG = "ambush";
	public static String UPGRADE_TAG = "upgrade";
	public static String HEALTH_UPGRADE_TAG = "health";
	public static String MAGIC_UPGRADE_TAG = "magic";
	public static String ATTACK_UPGRADE_TAG = "attack";
	public static String DEFEND_UPGRADE_TAG = "defence";
	public static String SPEED_UPGRADE_TAG = "speed";
	public static String STEALTH_UPGRADE_TAG = "stealth";
	
//	public static enum StatType {
//        HEALTH("health"), 
//        MAGIC("magic"), 
//        ATTACK("attack"), 
//        DEFEND("defence"),
//        STEALTH("stealth"),
//        SPEED("speed");
//        
//        private String value;
//
//        private StatType(String value) {
//                this.value = value;
//        }
//        
//        private static final HashMap<String, StatType> _map = new HashMap<String, StatType>();
//        static {
//            for (StatType statType : StatType.values())
//                _map.put(statType.value, statType);
//        }
//
//        public static StatType from(String value) {
//            return _map.get(value);
//        }
//        
//        public String getValue() {
//        	return value;
//        }
//	}; 
	
	public enum AbilityType {
		WEAPON(3),
		ARMOR(6),
		AMULET(4),
		RANGED(1),
		WAND(2),
		MAGIC_ITEM(1),
		ORB(6),
		POTION(5),
		ABILITY(1),
		DE_NADA(8),
		DASH(9);
		
		public int val;
		private AbilityType(int val) {
			this.val = val;
		}
	}

	public enum AbilityEffectType {
		POISON,
		BLESSING,
		HEALING,
		PROTECTION,
		ATTACK,
		CURSE,
		SPEED,
		SLOW,
		HOLD,
		FLIGHT,
		CHARGE,
		KNOCKBACK,
		STUNNED,
		RESIST_POISON,
		RESIST_HELD,
		RESIST_STUN,
		RESIST_HARD,
		RESIST_SHARP,
		RESIST_CHEMICAL,
		RESIST_ENERGY,
		RESIST_KNOCKBACK,
		RESIST_ALL,
		DEFENDING,
		HIDING,
		MISSILE_DEFENCE,
		RESIST_BURST,
		DASH,
		NONE_REALLY
	}
	
	public HashMap<String, Object> dynamicParams = new HashMap<String, Object>();
	public int usageCount = 0;  // Every time an ability is used, this number is incremented for ordering the selection list.
	
	public int turnsUntilCooldown;
	
	public static int getIdForName(String name) {
		for (int i=0; i<abilityData.size();i++) {
			if (abilityData.get(i).name.equals(name)) {
				return i;
			}
		}
		
		return -1;
	}
	
	public Ability(Ability theAbility) {
		this(theAbility.myId, theAbility.owned, 1, theAbility.dungeonEvents, theAbility.dungeonQuery);
	}
	
	public Ability(int	abilityId, Creature isOwned, int level, IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery) {
		this.dungeonEvents = dungeonEvents;
		this.dungeonQuery = dungeonQuery;
		
		myId = abilityId;
		
		if (dataInitialized == false) {
			initializeData();  // read all ability data from string
			dataInitialized = true;
		}

		// now initilize this particular ability
		if (abilityId < -1) {
			// we have to choose a random item appropriate for the level that we are on
			boolean 	itemChosen = false;
			
			while (!itemChosen) {
				int abId = Randy.getRand(0, abilityData.size()-1);
				ability = (Data) abilityData.elementAt(abId);
				myId = abId;

				// must be a physcial item at least
				if (ability.physicalItem == 1) {
					// calculate the power of this item.  is it appropriate for the level specified?
					if (calcValue(abId) <= level) {
						// ok to use this item, it is not too powerful
						if (abilityId == RANDOM_MAGIC) {
							if (ability.magicCost > 0) {
								itemChosen = true;
							}
							else if (ability.needs == NEEDS_HEAD)  {// must be an amulet of some sort
								// an amulet can be a magic or mundane random item
								itemChosen = true;
							}
						}
						else {
							if (ability.magicCost < 1) {
								itemChosen = true;
							}
						}	
					}
				}
			}
		}
		else { // just use the exact specified ability
			ability = (Data) abilityData.elementAt(abilityId);
		}

		// initialize according to the ability selected
		tickCounter = ability.duration;
		numberOfCommands = ability.command.length;
		setOwned(isOwned, true);
		setAbilityType();
		setAbilityEffectType();
		turnsUntilCooldown = 0;
	}


	public void persist(ObjectOutputStream out) throws IOException {
		out.writeInt(myId);
		out.writeInt(tickCounter);
		out.writeInt(turnsUntilCooldown);
		out.writeInt(numberOfCommands); 
		out.writeObject(set);
	}


	public Ability(ObjectInputStream in, Creature owned, IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery) throws IOException, ClassNotFoundException {	
		this.dungeonEvents = dungeonEvents;
		this.dungeonQuery = dungeonQuery;
		
		if (dataInitialized == false) {
			initializeData();  // read all ability data from string
			dataInitialized = true;
		}
		
		myId = in.readInt();
		ability = (Data) abilityData.elementAt(myId);
		this.owned = owned;

		// now write the variable attributes
		tickCounter = in.readInt();
		turnsUntilCooldown = in.readInt();
		numberOfCommands = in.readInt();			
		boolean isSet = (Boolean) in.readObject();
		
		if (isSet) {
			AbilityCommand ac = new AbilityCommand(AbilityCommand.SET, 0, 1, 1, 1);
			executeCommandValue(ac, owned);
		} else {
			unset(owned);
		}	
		
		setAbilityType();
		setAbilityEffectType();
	}

	public boolean tick() {
		if (ability.duration > 0)  // means a temporary ability
		{
			tickCounter--;

			if (tickCounter <= 0)
				return true;
		}

		if (isCooldownAbility() && !isCool()) {
			turnsUntilCooldown--;
		}
		
		return false;
	}
	
	public void changeTicksLeft(int delta) {
		tickCounter += delta;
	}

	public boolean isSelectable() {
		if (ability.invokingStrategy == NOT_SELECTABLE)
			return false;
		else
			return true;
	}
	
	public boolean isInstant() {	
		if ((ability.invokingStrategy == INSTANT_ONESHOT) || (ability.invokingStrategy == INSTANT_ABILITY))
			return true;
		else
			return false;
	}

	public boolean isBurstEffect() {
		for (int i=0; i<6; i++) {
			if (ability.command[i] == AbilityCommand.EXECUTE)  // does the command sent from the Creature match a command that this ability responds to?
			{
				if (ability.executeParam4[i] != 0) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean isPhysical() {
		if (ability.physicalItem == 0)
			return false;
		else
			return true;
	}
	
	public int getTurnsUntilCooldown() {
		return turnsUntilCooldown;
	}
	
	public boolean isCooldownAbility() {
		if (ability.cooldown > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public int getCooldownPeriod() {
		return ability.cooldown;
	}
	
	public boolean isCool() {
		if (turnsUntilCooldown > 0) {
			return false;
		} else {
			return true;
		}
	}
	
	public void setOwned(Creature owner, boolean isOwned) {
		if (owner != null)
		{
			if (isOwned == false)
				unset(owner);
		}
		
		owned = owner;
		set = false;
	}

	// return -1 means 'none'
	public int findAbilityAddedWhenEquipped() {
		int abilityAdded = -1;

		for (int i=0; i<numberOfCommands; i++) {
			if (ability.command[i] == AbilityCommand.SET) {
				if (ability.executeStrategy[i] == SELECT) {	
					if (ability.executeParam2[i] >= 0) {
						abilityAdded = ability.executeParam2[i];
					}
				}
			}
		}
		
		return abilityAdded;
	}
	
	// This makes this ability copy itself and add the copy to the creature.
	// used by rough terrain.
	public void applyToCreature(Creature target) {
		Ability copy = new Ability(this);
		target.addAbility(copy, null);
	}
	
	public int executeCommandValue(AbilityCommand	command, Creature owner) {
		//if (LOG) L.log("command: %s", command.name);
		
		int newValue = command.value;

		if (command.name == AbilityCommand.CANCEL) {
			return cancelAbilityInProgress();
		}

		if (meetsNeeds(command.head, command.hands, command.humanoid) == false) {		
			return newValue;
		}

		if  (command.name == AbilityCommand.INVOKE) {
			return  processInvoke(command, owner);
		}

		for (int i=0; i<numberOfCommands; i++)
		{
			if (command.name == ability.command[i])  // does the command sent from the Creature match a command that this ability responds to?
			{
				if (command.name == AbilityCommand.RESIST_ABILITY)
				{
					if (command.value == ability.executeParam1[i])
						return RESISTED;
				}
			
				if (ability.executeStrategy[i] == SELECT) 
				{
					AbilityCommand	newCommand = new AbilityCommand(0,0,1,1,1);
					newCommand.name = ability.executeParam1[i];   // CLEAR ARMOUR, CLEAR MELEE, etc...
					owner.broadcastAbilityCommand(newCommand);
					
					if (ability.executeParam2[i] >= 0) {// -1 means 'no ability'
						addedAbility = new Ability(ability.executeParam2[i], null, 0, dungeonEvents, dungeonQuery);
						owner.addAbility(addedAbility, null);
					}
					
					set = true;
					return newValue;
				}
				
				if (ability.executeStrategy[i] == DESELECT) {
					if (LOG) L.log("deselecting");
					unset(owner);
					return newValue;
				}

				if (isUsed() == false)
					return newValue;
			
				if (ability.executeStrategy[i] == VALUE_MODIFIER) { // change the value by the specified amount
					newValue += ability.executeParam1[i];
					if (LOG) L.log(ability.toString());
					if (LOG) L.log("newValue: %s", newValue);
				}
				else if (ability.executeStrategy[i] == VALUE_SETTER) { // set the value to the specified amount, if the value is smaller than the ability
					if (newValue < ability.executeParam1[i])
						newValue = ability.executeParam1[i];
					if (LOG) L.log(ability.toString());
					if (LOG) L.log("newValue: %s", newValue);
				} 
			}
		}

		return newValue;
	}
	
	public int executeCommandValueMultiply(AbilityCommand	command, Creature owner) {
		int newValue = command.value;

		for (int i=0; i<numberOfCommands; i++)
		{
			if (command.name == ability.command[i])  // does the command sent from the Creature match a command that this ability responds to?
			{
				if (isUsed() == false)
					return newValue;
			
				if (ability.executeStrategy[i] == VALUE_MULTIPLIER) { // set the value to the specified amount, if the value is smaller than the ability
					newValue *= ability.executeParam1[i];
					newValue /= 100;
					if (LOG) L.log(ability.toString());
					if (LOG) L.log("newValue: %s", newValue);
				}
			}
		}

		return newValue;
	}
	
	// This ability is being used.  Determine what type of visual thing should be shown.
	// This shows the initial activation of the ability, such as swinging a sword or whatever.
	private void fireAbilityUseEvent(Creature user, DungeonPosition pos, int command, int damageType) {	
		Character releventCharacter = user.getReleventCharacter();
		
		if ((ability.invokingStrategy == INSTANT_ABILITY) || (ability.invokingStrategy == INSTANT_ONESHOT)) {
			user.notHiding(releventCharacter);
			dungeonEvents.invokeAbility(SequenceNumber.getNext(), releventCharacter, user, pos, ability);
		} else if (ability.invokingStrategy == TARGETABLE_ABILITY) {
			user.notHiding(releventCharacter);
			dungeonEvents.rangedAttack(SequenceNumber.getNext(), releventCharacter, user, abilityType, damageType, pos);
		}
		
		if (command == AbilityCommand.MELEE_ATTACK) {
			user.notHiding(releventCharacter);
			dungeonEvents.meleeAttack(SequenceNumber.getNext(), releventCharacter, user, pos);
		}		
	}
	
	// TODO this is where use of all ablities ultimately get processed
	
	// This is the attempted execution of any ability against a target creature or location.
	// It returns true if the ability use did happen, false otherwise 
	// even if the ability did happen, that doesnt mean it had the desired effect, however.
	public boolean executeCommandTarget(AbilityCommand	command, Creature targetCreature, DungeonPosition pos, Creature attackingCreature) {
		if (meetsNeeds(command.head, command.hands, command.humanoid) == false)
			return false;

		if (isUsed() == false)
			return false;

		if (isCooldownAbility() && !isCool())
			return false;
		
		int range = 0;
		boolean melee = false;
		int damageType = AbilityCommand.NO_PHYSICAL_ATTACK;
		int	magicCost = ability.magicCost;
		
		for (int i=0; i<numberOfCommands; i++)
		{
			if (command.name == ability.command[i])  // does the command sent from the Creature match a command that this ability responds to?
			{
				HashSet<Creature> targets = new HashSet<Creature>();

				// copy initial target if it is a TARGETABLE ability or MELEE ATTACK
				if ((ability.invokingStrategy == TARGETABLE_ABILITY) || (command.name == AbilityCommand.MELEE_ATTACK)) {
					if (targetCreature != null) {
						targets.add(targetCreature);
					}
				} else if (ability.offensive == false) {
					// if this directed at oneself, or is it offensive?
					// add the creature itself as a target of its own spell
					targets.add(attackingCreature);					
				}

				// set the damage type, if any
				damageType = ability.executeParam1[i];

				// alert the GUI to the creature using an ability, if relevant.
				fireAbilityUseEvent(attackingCreature, pos, command.name, damageType);
				
				// get targets for the range of the effect
				if (ability.executeParam4[i] != 0) {// if this is a ranged effect, add other targets in range 
					range = ability.executeParam4[i];

					dungeonEvents.explosion(SequenceNumber.getNext(), attackingCreature.getReleventCharacter(), 
							 pos, damageType, range);

					if (range == Map.LOS) {    // ability code uses -1 as LSO, so we have to convert it to 0 for level
						range = Map.RANGE;// TODO RANGE L.VIEWPORT_LOS;
					}
					
					for (Creature visibleCreature : dungeonQuery.getCreaturesVisibleFrom(pos, range)) {
						// if the ability we are using is targetable, we add the attacking creature to the list also. otherwise we dont
						if ((attackingCreature != visibleCreature) || (ability.invokingStrategy == TARGETABLE_ABILITY)) {
								targets.add(visibleCreature);
						}
					}
				}

				// Sort the creatures in order of furtherst to closest.
				Creature.DistComparator dComp = new Creature.DistComparator(pos, false);
				ArrayList<Creature> sortedTargets = new ArrayList<Creature>();
				sortedTargets.addAll(targets);
				Collections.sort(sortedTargets, dComp);
				if (LOG) L.log("%s", sortedTargets);
				// now apply the effect to the list of target creatures
				for (Creature theCreature : sortedTargets) {
					DungeonPosition thePos = theCreature.getPosition();
					
					if (ability.executeStrategy[i] == ATTACKER)  // tell the creature to make a certain type of attack
					{
						Ability a = null;
						
						if (ability.executeParam3[i] >= 0)  { // -1 means add no ability
							a = new Ability(ability.executeParam3[i], theCreature, 1, dungeonEvents, dungeonQuery);  // this is the ability to add to a creature as part of the attack
							a.dynamicParams.put(Ability.TARGET_POS, pos);
						}

						if (command.name == AbilityCommand.MELEE_ATTACK) {
							melee = true;
						}
						
						AbilityCommand attack = new AbilityCommand(damageType, ability.executeParam2[i], melee, a, this);	// type, damage, melee? and ability
						boolean result = attackingCreature.makeAttack(attack, theCreature, thePos, ability.aimed, magicCost, ability.magicCost);

						if (result == false) {  // if an attack could not be made due to not enough magic left, bail out of this attack
							return true;
						} 
					}
					else if (ability.executeStrategy[i] == ABILITY_ADDER)  // add a certain ability to the target
					{
						Ability  ab = new Ability(ability.executeParam1[i], theCreature, 1, dungeonEvents, dungeonQuery); // will be owned by the target
						ab.dynamicParams.put(Ability.TARGET_POS, pos);
						if (hasTag(Ability.INVOKE_TAG)) {
							ab.dynamicParams.put(INVOKE_TAG, INVOKE_TAG);
						}
						attackingCreature.giveAbility(theCreature, ab, thePos, magicCost, attackingCreature);
					}
				}
				
				dungeonEvents.explosionOver();
				
				// Looks like the ability was activated, so apply magic cost
				attackingCreature.usedMagic(magicCost);
				
				if (isCooldownAbility()) {
					turnsUntilCooldown = ability.cooldown + 1;
					set = false;
				}
				
				return true;
			}
		}

		return false;
	}	

    public void targetSelected(DungeonPosition position) {	
		Creature targCreature = dungeonQuery.getCreatureAtLocation(position);
		
    	// now process the ability on the selected target.  A targetable ability only has an EXECUTE strategy
		AbilityCommand  command = new AbilityCommand(AbilityCommand.EXECUTE, 0, 1, 1, 1);  // we have already verified that we can use this ability
		executeCommandTarget(command, targCreature, position, owned);
    }	
    
    public void executeAbility() {
		AbilityCommand  command = new AbilityCommand(AbilityCommand.EXECUTE, 0, 1, 1, 1);  // we have already verified that we can use this ability
		executeCommandTarget(command, owned, owned.mapPosition, owned);
    }
    
    // i.e. dash ability
//    public void wasUsed() {
//		if (isCooldownAbility()) {
//			turnsUntilCooldown = ability.cooldown + 1;
//			set = false;
//		}
//    }

    public int getAbilityDamage() {
    	int i = 0;
    	for (int strategy : ability.executeStrategy) {
    		if (strategy == ATTACKER) {
    			return ability.executeParam2[i];
    		}
    		i++;
    	}
    	return 0;
    }
    
    public int getAbilityDamageType() {
    	int i = 0;
    	for (int strategy : ability.executeStrategy) {
    		if (strategy == ATTACKER) {
    			return ability.executeParam1[i];
    		}
    		i++;
    	}
    	return 0;
    }
    
	// experience value calculator
    public static int calcValue(int abilityId) {
		Data ad = (Data) abilityData.elementAt(abilityId);
		return ad.value;
    }

    public boolean isEnoughMagic(int magic) {
    	if (magic < ability.magicCost) {
    		return false;
    	} else {
    		return true;
    	}
    }
    
    public boolean isMagical() {
    	if (ability.magicCost > 0) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    public boolean isTargetable() {
    	if (ability.invokingStrategy == TARGETABLE_ABILITY) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
	public boolean isAimed() {
		return ability.aimed;
	}
    
    public boolean isRoughTerrain() {
    	if (getTag() != null && hasTag(Ability.ROUGH_TERRAIN_TAG)) {
    		return true;
    	} else {
    		return false;
    	}
    }
	public boolean invokeFinishesTurn()
	{
		if ((ability.invokingStrategy == INSTANT_ABILITY) ||
		(ability.invokingStrategy == INSTANT_ONESHOT))
			return true;
		else
			return false;
	}

	// ATTRIBUTES
	// instance data
	public Data 			ability;  // points to the AbilityData entry which describes how this ability works
	private int				tickCounter;	   // if the ability has limited duration, this variable counts down with every tick until it must destroy itself.
	public Creature			owned;			   // is this object owned or independent?
	private int				numberOfCommands;  // the number of commands in the command array that this ability responds to
	
	// invoking Strategy
	public static final int		NOT_SELECTABLE = 0;
	public static final int		INSTANT_ABILITY = 1;
	public static final int		TARGETABLE_ABILITY = 2;
	public static final int		SELECTABLE = 3;
	public static final int  	INSTANT_ONESHOT = 4;  // just like INSTANT_ABILITY, but disappears after use

	// execute strategy
	public static final int		NO_STRATEGY = 0;
	public static final int		VALUE_MODIFIER = 1;  // add or subtract your value to the passed in value
	public static final int		VALUE_SETTER = 2;   // if the passed -in value is < your value, increase it to your value
	public static final int		ATTACKER = 3;  // generate an attack
	public static final int		ABILITY_ADDER = 4;  // generate an ability
	public static final int		DESELECT = 5; // when command recieved, deselect yourself
	public static final int		SELECT = 6; // when command recieved, select yourself & deselect others
	public static final int		VALUE_MULTIPLIER = 7; // multiply the passed in value by x%

	// needs 
	public static final int		NEEDS_HEAD = 1;
	public static final int		NEEDS_HANDS = 2;  
	public static final int		NEEDS_HUMANOID = 3;   

	// ability data related attributes	
	static boolean			dataInitialized = false;

	public boolean 			set = false;
	//private ScreenObject 	currentEyeTarget;
	private Ability 		addedAbility;
	public int				myId;
	
	static Vector<Data> 	abilityData = new Vector<Data>(30,5);	

	static int index;
	static int endIndex;

	private int readNextNum(String string) {
		int n;
		endIndex = string.indexOf(",",index);
		n = Integer.parseInt(string.substring(index, endIndex));
		index = endIndex+1;		
		return n;
	}
	
	// METHODS
	private int addNextAbility(String string, int startIndex) {
		index = startIndex;
		Data  	ad = new Data();

		// read name 1
		endIndex = string.indexOf(",",index);
		ad.name = string.substring(index, endIndex);
		index = endIndex+1;

		if (LOG) L.log("name:%s",ad.name);
		
		// read gifName 2
		endIndex = string.indexOf(",",index);
		ad.tag = string.substring(index, endIndex);
		index = endIndex+1;

		// read powerups 3  (does not apply to abilities)
		endIndex = string.indexOf(",",index);
		ad.powerups = string.substring(index, endIndex);
		index = endIndex+1;

		// read needs 4
		ad.needs = readNextNum(string);

		// read invokingStrategy 5
		ad.invokingStrategy = readNextNum(string);

		// read duration 6
		ad.duration = readNextNum(string);

		// read physicalItem 7
		ad.physicalItem = readNextNum(string);

		// read magicItem 8
		ad.magicCost = readNextNum(string);

		// read aimed 9
		endIndex = string.indexOf(",",index);
		if (Integer.parseInt(string.substring(index, endIndex)) == 1)
			ad.aimed = true;
		else
			ad.aimed = false;
		index = endIndex+1;

		// read value 10
		ad.value = readNextNum(string);

		// read offensive 11
		endIndex = string.indexOf(",",index);
		if (Integer.parseInt(string.substring(index, endIndex)) == 1)
			ad.offensive = true;
		else
			ad.offensive = false;
		index = endIndex+1;

		// read cooldown 12
		ad.cooldown = readNextNum(string);
		
		abilityData.addElement(ad);
		
		// read an arbitary number of execute command responses.  we will settle for 6 maximum at the moment
		ad.command = new int[6];
		for (int i=0; i<6;i++) ad.command[i] = -1; // unknown command
		ad.executeStrategy = new int[6];
		ad.executeParam1 = new int[6];
		ad.executeParam2 = new int[6];
		ad.executeParam3 = new int[6];
		ad.executeParam4 = new int[6];

		
		int finalIndex = string.indexOf("*",index);  // finalIndex represents the last position, so when index == finalIndex, its over
		int i=0;

		while (index != finalIndex) {
			ad.command[i] = readNextNum(string);

			ad.executeStrategy[i] = readNextNum(string);

			ad.executeParam1[i] = readNextNum(string);

			ad.executeParam2[i] = readNextNum(string);

			ad.executeParam3[i] = readNextNum(string);

			ad.executeParam4[i] = readNextNum(string);

			i++;
		}

		return finalIndex+1;
	}	

//	public static ArrayList<StatAbilityInfo> getStatPowerups() {
//		return possibleStatPowerups;
//	}
	
	public static class UpgradeAbilityInfo {
		public int id;
		public String upgradeType;
		public int level;
	}
	
	private void initializeData() {
		int 	index = 0;
		String	abilities = new TextResourceIdentifier("a.txt").getFileContents();
		
		while (index < abilities.length()) {
			index = addNextAbility(abilities, index);
		}
	}
	
	public static int getUpgradeId(String upgradeType, int level) {
		for (int i=0; i<abilityData.size();i++) {
			UpgradeAbilityInfo si = getUpgradeInfo(i);
			if (si != null) {
				if (si.upgradeType.equals(upgradeType) && si.level == level) {
					return si.id;
				}
			}
		}
		return -1;
	}
	
	public static UpgradeAbilityInfo getUpgradeInfo(int id) {
		String tagString = abilityData.get(id).tag;
		String[] tags = tagString.split("\\.");
		for (String tag : tags) {
			if (tag.startsWith(UPGRADE_TAG)) {   
				String[] typeTags = tag.split("-");
				UpgradeAbilityInfo si = new UpgradeAbilityInfo();
				si.id = id;
				si.upgradeType = typeTags[1];
				si.level = Integer.parseInt(typeTags[2]);
				return si;
			}
		}
		
		return null;
	}

	public UpgradeAbilityInfo getUpgradeInfo() {
		return Ability.getUpgradeInfo(myId);
	}

	public boolean meetsNeeds(boolean head, boolean hands, boolean humanoid) {
		boolean result = true;
	
		if (ability.needs == NEEDS_HEAD)
			result = head;

		if (ability.needs == NEEDS_HANDS)
			result = hands;

		if (ability.needs == NEEDS_HUMANOID)
			result = humanoid;

		return result;
	}

	public boolean isUsed() {
		if (needsSetting() == true)
			return set;
		else
			return true;
	}	

	public int getXpCost() {
		return ability.value;
	}
	
	protected boolean needsSetting() {
		if ((ability.invokingStrategy == SELECTABLE) || 
		(ability.invokingStrategy == INSTANT_ABILITY) || 
		(ability.invokingStrategy == INSTANT_ONESHOT))
			return true;
		else
			return false;
	}

	public String getTag() {
		return ability.tag;
	}
	
	public boolean hasTag(String theTag) {
      for (String tag: getTags()) {
			if (tag.equals(theTag)) {
				return true;
			}
       }
		
		return false;
	}
	
	public String[] getTags() {
		return ability.tag.split("\\.");
	}
	
	// processes the actual effect of the ability by sending it the invoke command.
	// instants and one-shots will go off.
	// equipables will toggle their selection status
	// returns true if ability action was to set/unset.
	public boolean abilitySelected(Creature owner) {
		AbilityCommand command = new AbilityCommand(AbilityCommand.INVOKE, 0, owner.creature.head, owner.creature.hands, owner.creature.humanoid);	
		int result = executeCommandValue(command, owner);
		if (LOG) L.log("result of invoke: %s", result);
		if (result == 0) {
			return true;
		} else {
			return false;
		}
	}
	
	protected int processInvoke(AbilityCommand command, Creature owner) {
		int result = -1;
		if (LOG) L.log("command: %s, owner: %s", command, owner);
		AbilityCommand	newCommand = new AbilityCommand(0,0,1,1,1);

		if (ability.invokingStrategy == SELECTABLE) {
			if (set) {
				unset(owner);
			} else {
				newCommand.name = AbilityCommand.SET;
				executeCommandValue(newCommand, owner);
			}
			
			result = 0;
		}

		if ((ability.invokingStrategy == INSTANT_ABILITY) ||
		    (ability.invokingStrategy == INSTANT_ONESHOT)) {
			set = true;			  // ability becomes active

	    	// now process the EXECUTE on this instant ability - maybe it will respond to that
			newCommand.name = AbilityCommand.EXECUTE;
			executeCommandTarget(newCommand, null, owner.getPosition(), owner); // instant abilities will apply either to the owner, or to all creatures in range		

			owner.updateStats(this, owner.getPosition());  // this will produce effects on the user -  which this ability may respond to

			set = false;  // ability becomes inactive once more

			if (ability.invokingStrategy == INSTANT_ONESHOT) {
				owner.destroyAbility(this);  // ability disappears
			}
		}

		return result;
	}

	private  int cancelAbilityInProgress() {
		return 0;
	}

	public void unset(Creature  owner) {
		set = false;
		
		if (addedAbility != null)
			owner.destroyAbility(addedAbility);

		addedAbility = null;
	}
	
	private AbilityType abilityType;
	private Vector<AbilityEffectType> abilityEffectType;
	// ATTRIBUTES
	public final static String TARGET_POS = "targetpos";
	
	// To set the general type of image, we have to investiagte the effects of the ability on the creature stats.
	// abilities can modify...
	// 1. health up or down  (healing or poison)
	// 2. attack skill up or down (blessing or curse)
	// 3. defence skill up or down (blessing or curse)
	// 4. magic up or down (blessing or curse)
	// 5. speed up or down (speed or slow)
	// 6. one time absolute speed change (hold)
	//
	// To determine which of these the ability does, we test to see if it responds to the various commands we are unterested in.
	public boolean testEffectType(int i, int commandName)
	{
		if (commandName == ability.command[i]) {
			return true;
		}
		
		return false;
	}
	
	public Vector<AbilityEffectType> getEffectType() {
		return abilityEffectType;
	}
	
	private final static String HELD = "held";
	private final static String STUN = "stunned";
	private final static String KNOCKEDBACK = "knockedback";
	
	private void setAbilityEffectType()
	{
		// The HashSet wont take duplicates, which is possible for some abilities, but redundant for display purposes.
		LinkedHashSet<AbilityEffectType> abilityEffectTypeHash = new LinkedHashSet<AbilityEffectType>();
		
		boolean hard=false;
		boolean sharp=false;
		boolean chem=false;
		boolean energy=false;
		
		int heldId = Ability.getIdForName(HELD);
		int knockbackId = Ability.getIdForName(KNOCKEDBACK);
		int stunnedId = Ability.getIdForName(STUN);
		
		for (int i=0; i<numberOfCommands; i++) {
	
			// resist damage item effects
			if (testEffectType(i, AbilityCommand.RESIST_HARD)) {
				abilityEffectTypeHash.add(AbilityEffectType.RESIST_HARD);
				hard = true;
			}

			if (testEffectType(i, AbilityCommand.RESIST_SHARP)) {
				abilityEffectTypeHash.add(AbilityEffectType.RESIST_SHARP);
				sharp = true;
			}
			
			if (testEffectType(i, AbilityCommand.RESIST_ENERGY)) {
				abilityEffectTypeHash.add(AbilityEffectType.RESIST_ENERGY);
				energy = true;
			}
			
			if (testEffectType(i, AbilityCommand.RESIST_CHEMICAL)) {
				abilityEffectTypeHash.add(AbilityEffectType.RESIST_CHEMICAL);
				chem = true;
			}
			
			if (testEffectType(i, AbilityCommand.RESIST_BURST)) {
				abilityEffectTypeHash.add(AbilityEffectType.RESIST_BURST);
				chem = true;
			}
			
			if (testEffectType(i, AbilityCommand.MODIFY_MISSILE_DEFENCE)) {
				abilityEffectTypeHash.add(AbilityEffectType.MISSILE_DEFENCE);
				chem = true;
			}
			
			// speed related
			if (testEffectType(i, AbilityCommand.MODIFY_SPEED) ) {
				if (ability.executeParam1[i] > 0) {
					abilityEffectTypeHash.add(AbilityEffectType.SPEED);
				} else if (hasTag(SLOWED_TAG)) {
					abilityEffectTypeHash.add(AbilityEffectType.SLOW);
				} else if (hasTag(STUNNED_TAG)) {
					abilityEffectTypeHash.add(AbilityEffectType.STUNNED);
				} else if (hasTag(HELD_TAG)) {
					abilityEffectTypeHash.add(AbilityEffectType.HOLD);
				} else {
					abilityEffectTypeHash.add(AbilityEffectType.SLOW);
				}
			}
			
//			if (isRoughTerrain() && hasTag(HOLE_TAG) == false) {
//				abilityEffectTypeHash.add(AbilityEffectType.SLOW);
//			} else if (testEffectType(i, AbilityCommand.MODIFY_SPEED) ) {
//				if (ability.executeParam1[i] > 0) {
//					abilityEffectTypeHash.add(AbilityEffectType.SPEED);
//				} else if (ability.executeParam1[i] < -5) {
//					if (ability.name.equals(HELD)) {
//						abilityEffectTypeHash.add(AbilityEffectType.HOLD);
//					} else {
//						abilityEffectTypeHash.add(AbilityEffectType.STUNNED);
//					}
//				} else {
//					abilityEffectTypeHash.add(AbilityEffectType.SLOW);
//				}
//			}
//			
//			if (hasTag(SLOWED_TAG)) {
//				abilityEffectTypeHash.add(AbilityEffectType.SLOW);
//			}
			
			// health related
			if (testEffectType(i, AbilityCommand.MODIFY_HEALTH) || testEffectType(i, AbilityCommand.MODIFY_MAX_HEALTH)) {
				if (ability.executeParam1[i] > 0) {
					abilityEffectTypeHash.add(AbilityEffectType.HEALING);
				} else {
					abilityEffectTypeHash.add(AbilityEffectType.POISON);
				}
			}
			
			// magic related
			if (testEffectType(i, AbilityCommand.MODIFY_MAGIC) || testEffectType(i, AbilityCommand.MODIFY_MAX_MAGIC)) {
				if (ability.executeParam1[i] > 0) {
					abilityEffectTypeHash.add(AbilityEffectType.BLESSING);
				} else {
					abilityEffectTypeHash.add(AbilityEffectType.CURSE);
				}
			}
			
			// protection related
			if (testEffectType(i, AbilityCommand.MODIFY_DEFENCE_SKILL)) {
				if (ability.executeParam1[i] > 0) {
					abilityEffectTypeHash.add(AbilityEffectType.PROTECTION);
				} else {
					abilityEffectTypeHash.add(AbilityEffectType.CURSE);
				}
			}
			
			// resistance related
			if (testEffectType(i, AbilityCommand.RESIST_ABILITY)) {
				if (ability.executeParam1[i] == heldId) {
					abilityEffectTypeHash.add(AbilityEffectType.RESIST_HELD);
				} else if (ability.executeParam1[i] == stunnedId) {
					abilityEffectTypeHash.add(AbilityEffectType.RESIST_STUN);
				} else if (ability.executeParam1[i] == knockbackId) {
					abilityEffectTypeHash.add(AbilityEffectType.RESIST_KNOCKBACK);
				} else {
					abilityEffectTypeHash.add(AbilityEffectType.RESIST_POISON);
				}
			}
			
			// attack related
			if (testEffectType(i, AbilityCommand.MODIFY_ATTACK_SKILL)) {
				if (ability.executeParam1[i] > 0) {
					abilityEffectTypeHash.add(AbilityEffectType.ATTACK);
				} else {
					abilityEffectTypeHash.add(AbilityEffectType.CURSE);
				}
			}
			
			// stealth related
			if (testEffectType(i, AbilityCommand.MODIFY_STEALTH)) {
				if (ability.executeParam1[i] > 0) {
					abilityEffectTypeHash.add(AbilityEffectType.HIDING);
				} else {
					abilityEffectTypeHash.add(AbilityEffectType.CURSE);
				}
			}
			
			// detect related
			if (testEffectType(i, AbilityCommand.MODIFY_DETECT)) {
				if (ability.executeParam1[i] > 0) {
					abilityEffectTypeHash.add(AbilityEffectType.ATTACK);
				} else {
					abilityEffectTypeHash.add(AbilityEffectType.CURSE);
				}
			}
		}
		
		// If its empty, put in a placeholder
//		if (abilityEffectTypeHash.size() == 0) {
//			abilityEffectTypeHash.add(AbilityEffectType.NONE_REALLY);
//		}
		
		if (hasTag(CHARGE_TAG)) {
			abilityEffectTypeHash.add(AbilityEffectType.CHARGE);
		}
		
		if (hasTag(KNOCKBACK_TAG)) {
			abilityEffectTypeHash.add(AbilityEffectType.KNOCKBACK);
		}
		
		if (hasTag(FLIGHT_TAG)) {
			abilityEffectTypeHash.add(AbilityEffectType.FLIGHT);
		}
		
		if (hasTag(DASH_TAG)) {
			abilityEffectTypeHash.add(AbilityEffectType.DASH);
		}
		
		// replace resist all 4 types with 'resist all'
		if (hard && sharp && energy && chem) {
			abilityEffectTypeHash.remove(AbilityEffectType.RESIST_HARD);
			abilityEffectTypeHash.remove(AbilityEffectType.RESIST_ENERGY);
			abilityEffectTypeHash.remove(AbilityEffectType.RESIST_SHARP);
			abilityEffectTypeHash.remove(AbilityEffectType.RESIST_CHEMICAL);
			abilityEffectTypeHash.add(AbilityEffectType.RESIST_ALL);
			
		}
		// Vectorize it
		abilityEffectType = new Vector(abilityEffectTypeHash);
	}
	
	public AbilityType getAbilityType() {
		return abilityType;
	}
	
	private void setAbilityType()
	{	
		AbilityType abilityType = AbilityType.ABILITY;
		
		if (hasTag(WEAPON_TAG)) {
			abilityType = AbilityType.WEAPON;
		} 
		else if (hasTag(ARMOR_TAG)) {
			abilityType = AbilityType.ARMOR;
		}
		else if (hasTag(WAND_TAG)) {
			abilityType = AbilityType.WAND;
		}
		else if (hasTag(RANGED_TAG)) {
			abilityType = AbilityType.RANGED;
		}
		else if (hasTag(AMULET_TAG)) {
			abilityType = AbilityType.AMULET;
		}
		else if (hasTag(DASH_TAG)) {
			abilityType = AbilityType.DASH;
		}
		else if (hasTag(MAGIC_TAG)) {
			abilityType = AbilityType.MAGIC_ITEM;
			
			// special case potions and orbs.   Only way to tell is to scan the name.  // TODO could use tag
			if (ability.name.contains("orb") || ability.name.contains("bomb") 
					|| ability.name.contains("sphere")	|| ability.name.contains("crystal")) {
				abilityType = AbilityType.ORB;
			} else if (ability.name.contains("potion")) {
				abilityType = AbilityType.POTION;
			}
		}
		
		this.abilityType = abilityType;
	}
	
	public int getAbilityDurationLeft() {
		return tickCounter;
	}

	public int getId() {
		return myId;
	}
	
	@Override
	public String toString() {
		return ability.name;
	}
}


//public boolean saveObject(DataOutputStream out) throws IOException
//{
//	//out.writeInt(ScreenObject.ABILITY_OBJECT);
//
//	//write out the position
//	//mapPosition.saveObject(out);
//
//	// write the ability id
//	out.writeInt(myId);
//
//	// now write the variable attributes
//	out.writeInt(numberOfCommands); 
//	out.writeInt(tickCounter);	   
//	out.writeInt(set);
//	
//	if (set)
//		out.writeInt(1);
//	else
//		out.writeInt(0);
//
//		return true;
//}


