package com.dbash.models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import com.dbash.platform.TextResourceIdentifier;
import com.dbash.presenters.dungeon.CreaturePresenter;
import com.dbash.util.Randy;
import com.dbash.util.SequenceNumber;


public abstract class Creature implements IPresenterCreature
{
	// INTERFACE
	public static final int	MAX_SPEED		= 10;
	
	public enum CreatureType {
		CHARACTER,
		MONSTER
	}

// ATTRIBUTES
	
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
	
	// creatures current health points
	public int						health;								// the

	// creatures current magic points
	public int						magic;									// the
	
	public Data					creature;
	public List<Ability>					abilities;
	int						myId;
	public int				state;
	static boolean			dataInitialized	= false;

	static protected Vector	creatureData	= new Vector(30, 5);

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
	
	@Override
	public void setCreaturePresenter(CreaturePresenter creaturePresenter) {
		this.creaturePresenter = creaturePresenter;
	}
	
	@Override
	public CreaturePresenter getCreaturePresenter() {
		return creaturePresenter;
	}
	
	@Override
	public String toString() {
		return super.toString() + ";" + getCreature().name + "; (" + health + "/" + calculateMaxHealth() + ")";
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

		for (int i = 0; i < getCreature().abilityIds.length; i++)
		{
			if (getCreature().abilityIds[i] != -1)
			{
				Ability a = new Ability(getCreature().abilityIds[i], this, p.level, dungeonEvents, dungeonQuery);
				addAbility(a);
			}
		}

		// set the default melee attack, armour and amulet for this creature
		AbilityCommand command = new AbilityCommand(AbilityCommand.SET, 0, getCreature().head, getCreature().hands, getCreature().humanoid);
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

		// position.
		out.writeObject(mapPosition);

		// 5.3] Each Creature saves its list of Abilities
		unsetAbilities(true);
		out.writeInt(abilities.size());
		for (Ability ability : abilities) {
			ability.persist(out);
		}	
	}
	
	private void unsetAbilities(boolean leaveSet)
	{
		// go through abilities and unset them all to get rid of added abilities
		// so they wont be added twice on reload
		for (int i = abilities.size() - 1; i >= 0; i--)
		{
			Ability a = (Ability) abilities.get(i);
			if (a.set)
			{
				a.unset(this); // will destroy any ability associated with this thing
				a.set = leaveSet; // reset just the setting indicator so it will
									// be saved and restored properly.
			}
		}

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

	    // position
	    mapPosition = (DungeonPosition) in.readObject();
	    
		// read abilities now
		int numberOfAbilities = in.readInt();
		for (int i=0; i < numberOfAbilities; i++)
		{
			Ability ability = new Ability(in, this, dungeonEvents, dungeonQuery);
			abilities.add(ability);
		}
	}

	// this function sets all the data that can be derived form the creature id
	private void setCreatureData(int creatureId)
	{
		speed = getCreature().speed;
		attackSkill = getCreature().attackSkill;
		defenceSkill = getCreature().defenceSkill;
		// image = gameControl.getImageFromGif(creature.gifName);
		myId = creatureId;
		maximumHealth = getCreature().maximumHealth;
		maximumMagic = getCreature().maximumMagic;

		// create an ability vector for the abilities to go in
		abilities = new LinkedList<Ability>();
	}

	public boolean isReadyForTurn()
	{
		int speedCheck = modifyValue(AbilityCommand.MODIFY_SPEED, speed) + missedTurns; 

		// every time this creature misses a turn, it is more likely to get a turn next time.
		// this is to help prevent randomness depriving a creature of a turn for an undesirably long time
	
        if (Randy.getRand(1, MAX_SPEED) <= speedCheck)
        {
        	missedTurns = 0;
        	return true;
        }
        else
        {
        	missedTurns += 2;
        	return false;
        }
	}
	
	// these are for a CreaturePresenter to observe changes to the highlight status of the creature.
	// the highlight status will only be manipulated by Characters.  For monsters it will remain at default NO_HIGLIGHT.
	public void onChangeToVisualStatus(UIInfoListener listener)
	{
		visualStatusListener = listener;
	}
	
	public HighlightStatus getHighlightStatus()
	{
		return highlightStatus;
	}
	
	public abstract void processTurn();
	public abstract boolean canSkipTurn();
	// calculates changes to creatrue stats, and returns true if creature still alive, false otherwise
	// called by processTurn.
	public boolean processStatsForTurn()
	{
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
		
		creatureStats.health = health;
		creatureStats.maxHealth = maxHealth;
		creatureStats.magic = magic;
		creatureStats.maxMagic = maxMagic;
		creatureStats.experience = experience;
		boolean tempAbilityActive = processAbilities();
		
		// set a flag that could help monsters not bother having their turn.
		if (tempAbilityActive || health < maxHealth || magic < maxMagic) {
			creatureIsStable = false;
		} else {
			creatureIsStable = true;
		}
		
		return true;
	}

	
	public abstract boolean canMove(int intendedDirection);

	
	// Will find the best direction to move in for this creature to get from where it currently is, to
	// the position passed in, taking into account obstacles appropriate for Monsters or Characters.
	// i.e. a monster will try to pass through (attack) a character whereas another character wont.
	// If there is no possible way to move semi-directly towards the target, this function will return NO_DIR
	// If the position passed in is null, you get random direction
	public int findBestDirection(DungeonPosition characterPosition)
	{
		int direction = DungeonPosition.NO_DIR;

		if (null == characterPosition)
		{
			direction = Randy.getRand(DungeonPosition.WEST, DungeonPosition.SOUTHWEST);
			if (canMove(direction)) {
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
				if (canMove(DungeonPosition.NORTH))
					direction = DungeonPosition.NORTH;
				else if (canMove(DungeonPosition.NORTHWEST))				
					direction = DungeonPosition.NORTHWEST;
				else if (canMove(DungeonPosition.NORTHEAST))
					direction = DungeonPosition.NORTHEAST;	
			}
			else
			{
				if (canMove(DungeonPosition.SOUTH))
					direction = DungeonPosition.SOUTH;
				else if (canMove(DungeonPosition.SOUTHWEST))				
					direction = DungeonPosition.SOUTHWEST;
				else if (canMove(DungeonPosition.SOUTHEAST))
					direction = DungeonPosition.SOUTHEAST;
			}
		}
		// character is west of current position
		else if (characterPosition.x < mapPosition.x)
		{
			if (characterPosition.y == mapPosition.y)
			{
				if (canMove(DungeonPosition.WEST))
					direction = DungeonPosition.WEST;
				else if (canMove(DungeonPosition.SOUTHWEST))				
					direction = DungeonPosition.SOUTHWEST;
				else if (canMove(DungeonPosition.NORTHWEST))
					direction = DungeonPosition.NORTHWEST;
			}
			else if (characterPosition.y > mapPosition.y)  // character is north of current pos
			{
				if (canMove(DungeonPosition.NORTHWEST))
					direction = DungeonPosition.NORTHWEST;
				else if (canMove(DungeonPosition.WEST))				
					direction = DungeonPosition.WEST;
				else if (canMove(DungeonPosition.NORTH))
					direction = DungeonPosition.NORTH;
			}
			else
			{
				if (canMove(DungeonPosition.SOUTHWEST))
					direction = DungeonPosition.SOUTHWEST;
				else if (canMove(DungeonPosition.WEST))				
					direction = DungeonPosition.WEST;
				else if (canMove(DungeonPosition.SOUTH))
					direction = DungeonPosition.SOUTH;
			}
		}
		// character is east of current position
		else if (characterPosition.x > mapPosition.x)
		{
			if (characterPosition.y == mapPosition.y)
			{
				if (canMove(DungeonPosition.EAST))
					direction = DungeonPosition.EAST;
				else if (canMove(DungeonPosition.SOUTHEAST))				
					direction = DungeonPosition.SOUTHEAST;
				else if (canMove(DungeonPosition.NORTHEAST))
					direction = DungeonPosition.NORTHEAST;
			}
			else if (characterPosition.y > mapPosition.y)  // character is north of current pos
			{
				if (canMove(DungeonPosition.NORTHEAST))
					direction = DungeonPosition.NORTHEAST;
				else if (canMove(DungeonPosition.NORTH))				
					direction = DungeonPosition.NORTH;
				else if (canMove(DungeonPosition.EAST))
					direction = DungeonPosition.EAST;
			}
			else
			{
				if (canMove(DungeonPosition.SOUTHEAST))
					direction = DungeonPosition.SOUTHEAST;
				else if (canMove(DungeonPosition.SOUTH))				
					direction = DungeonPosition.SOUTH;
				else if (canMove(DungeonPosition.EAST))
					direction = DungeonPosition.EAST;
			}
		}

		return direction;

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
		if (visualStatusListener != null) {
			visualStatusListener.UIInfoChanged();
		}
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

		experience += ((exp * percentage) / 100);
		expRoot = (int)Math.sqrt(experience);// pre-calculate the squre root
												// of the experience which is
												// used in many places

		// we need to modify the creatures health and maxheath based on initial
		// experience
		if (initialExp)
		{
			health = calculateMaxHealth();
			magic = calculateMaxMagic();
		}
		
		creatureStats.experience = experience;
	}

	// if you want to make the attack disregard defenders skill, simply set the
	// attackers skill level to 32000
	public int respondAttack(AbilityCommand attack, Creature attacker)
	{
		if (Randy.getRand(1, attack.skill) > Randy.getRand(1, calculateDefenceSkill())) // attack hits
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
				// reduce damage by any ability which reduces damage of that type
				newDamage = (newDamage * (100 - modifyValue(abilityCommand, 0))) / 100;
			} 

			if (newDamage < 1) {
				newDamage = 0;
			}

			if (newDamage > 0) {
				// give attacker the experience for causing damage
				attacker.addExperience(getExpValue() / 50 + 1, false);
			}
			
			if (newDamage > 99)
				newDamage = 99;

			// there is some kind of attack
			if (abilityCommand != AbilityCommand.NO_PHYSICAL_ATTACK)
			{
				//makeHit(attack.type, newDamage, mapPosition, hitTime);
				
				// Tell the gui about the damage
				dungeonEvents.damageInflicted(SequenceNumber.getCurrent(), attacker.getReleventCharacter(), mapPosition, attack.type, newDamage);	
			}
			
			if (attack.ability != null)
			{
				if (attacker.giveAbility(this, attack.ability, this.mapPosition, 0, attacker))
				{
					// hitTime = 400;
					// give attacker experience for inflicting ability
					attacker.addExperience(getExpValue() / 30 + 1, false); 
				}
			}

			health -= newDamage;
			if (health < 1) {
				// give killer the experience for killing you
				attacker.addExperience(getExpValue(), false);
				death();
				return 0;
			}

			return newDamage;
		}

		// indicates a miss
		dungeonEvents.missed(SequenceNumber.getCurrent(), attacker.getReleventCharacter(), mapPosition);
		
		return -1;
	}

	public int getExpValue()
	{
		return 10 * calcLevel(myId) + experience / 20;
	}

	public int getLevelValue()
	{
		return calcLevel(myId);
	}

	public boolean addAbility(Ability ability)
	{
		// first work out whether you have any ability which will prevent this
		// ability from being added (such as resist poison)
		AbilityCommand ac = new AbilityCommand(AbilityCommand.RESIST_ABILITY, ability.myId, 1, 1, 1);

		for (int i = 0; i < abilities.size(); i++)
		{
			if (abilities.get(i).executeCommandValue(ac, this) == Ability.RESISTED)
				return false;
		}

		abilities.add(ability);
		ability.setOwned(this, true);
		return true;
	}

	public void broadcastAbilityCommand(AbilityCommand command)
	{
		for (int i = 0; i < abilities.size(); i++)
		{
			abilities.get(i).executeCommandValue(command, this);
		}
	}

	public void destroyAbility(Ability ability)
	{
		abilities.remove(ability);
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
		
		if (processAbilities()) {
			someEffect = true;
		}
		
		// at the moment, show it going off anyway
		if (ab != null) {
			dungeonEvents.abilityAdded(SequenceNumber.getNext(), this.getReleventCharacter(), ab.getEffectType(), pos);
		}
		
		return someEffect;
	}

	// The highlighted ability effect will be either the latest ability added to the character, or the temporary ability
	// with the least turns left to countDown
	// returns true if highlighted ability has changed
	protected abstract boolean processAbilities();
	
	
	// ATTRIBUTES

	

	/*
	 * colour numbers BLACK (=0) BLUE (=1) LIME (=2) AQUA (=3) RED (=4) FUCHSIA
	 * (=5) YELLOW (=6) WHITE (=7) GRAY (=8) NAVY (=9) GREEN (=10) TEAL (=11)
	 * MAROON (=12) PURPLE (=13) OLIVE (=14) SILVER (=15)
	 */

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
	protected int calculateAttackSkill()
	{
		int newAttack = attackSkill + (attackSkill * expRoot) / 100;

		return modifyValue(AbilityCommand.MODIFY_ATTACK_SKILL, newAttack);

	}

	protected int calculateDefenceSkill()
	{
		int newDefence = defenceSkill + (defenceSkill * expRoot) / 100;

		return modifyValue(AbilityCommand.MODIFY_DEFENCE_SKILL, newDefence);
	}

	protected int modifyHealth()
	{
		return modifyValue(AbilityCommand.MODIFY_HEALTH, health);
	}

	protected int modifyMagic()
	{
		return modifyValue(AbilityCommand.MODIFY_MAGIC, magic);
	}

	public int calculateMaxHealth()
	{
		int newMaxHealth = maximumHealth + (maximumHealth * expRoot) / 100;

		return modifyValue(AbilityCommand.MODIFY_MAX_HEALTH, newMaxHealth);
	}

	public int calculateMaxMagic()
	{
		int newMaxMag = maximumMagic + (maximumMagic * expRoot) / 100;

		return modifyValue(AbilityCommand.MODIFY_MAX_MAGIC, newMaxMag);
	}

	public boolean isSolid()
	{
		return true;
	}

	public boolean giveAbility(Creature target, Ability ab, DungeonPosition pos, int magicCost, Creature giver)
	{
		boolean addedOk = true;

		if (target != null){
			addedOk = target.addAbility(ab);
			
			// There is an edge case where adding the creature adds an ability to itself, which will end its turn,
			// and in the end turn processing, the tic counter for temp abilities is decremented, effectively
			// giving the ability 1 less turn that it should.  So we increment the tick counter here first.
			if ((target == this) && (ab.ability.duration > 0)) {
				ab.changeTicksLeft(1);  // add 1 to the tick counter
			}
		}

		if (addedOk) {
			dungeonEvents.abilityAdded(SequenceNumber.getCurrent()+1, giver.getReleventCharacter(), ab.getEffectType(), pos);
		} else {
			// indicates a resisted ability
			dungeonEvents.abilityResisted(SequenceNumber.getCurrent()+1, giver.getReleventCharacter(), ab.getEffectType(), pos);
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

//		if (magic >= magicCost)
//		{
//			magic -= magicCost;

			// calculate damage before changing attack skill to enormous
			attack.skill = calculateAttackSkill();

			if (attack.type != AbilityCommand.NO_PHYSICAL_ATTACK)
			{
				// rules for damage:
				// 1. creatures size only affects damage of melee attacks
				if (attack.melee)
				{
					attack.damage += (calculateMaxHealth() * calculateMaxHealth()) / 100;
				}

				// 2. creatures attack skill only affects damage of aimed
				// attacks
				// 3. skill effect on damage is much less than size affect on
				// damage

				if (useSkill)
				{
					attack.damage += attack.skill / 8;
				}

				// 4. creatures magical capability (maxMagic) effects damage of
				// magical attacks (that have a magic cost)
				if (magicUse > 0)
				{
					attack.damage += calculateMaxMagic() / 6;
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
				if (!useSkill)
					attack.skill = 32000; // sure thing

				target.respondAttack(attack, this);
			}
//		}
//		else
//		{
//			result = false;
//			// dungeonQuery.getSoundControl().playSound(GameControl.BADSOUND);
//		}

		return result;

	}

	static int	index;
	static int	endIndex;

	private static int readNextNum(String string)
	{
		int n;
		endIndex = string.indexOf(",", index);
		n = Integer.parseInt(string.substring(index, endIndex));
		index = endIndex + 1;
		return n;
	}

	private static int addNextCreature(String string, int startIndex)
	{
		index = startIndex;
		Data cd = new Data();

		// read name
		endIndex = string.indexOf(",", index);
		cd.name = string.substring(index, endIndex);
		index = endIndex + 1;

		// read gif filename
		endIndex = string.indexOf(",", index);
		cd.gifName = string.substring(index, endIndex);
		index = endIndex + 1;

		// read gif colour
		cd.colour = readNextNum(string);

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

		do
		{
			endIndex = abilities.indexOf(",", index);
			if (endIndex > -1)
			{
				cd.abilityIds[i] = Integer.parseInt(abilities.substring(index, endIndex));
				i++;
				index = endIndex + 1;
			}
		} while (endIndex != -1);

		return finalIndex;
	}

	public static void initializeData()
	{
		if (dataInitialized == false)
		{
			int index = 0;
			String creatures = new TextResourceIdentifier("c.txt").getFileContents();

			while (index < creatures.length())
			{
				index = addNextCreature(creatures, index);
			}

			dataInitialized = true;
		}
	}

	/*
	 * public static void printAll() { for (int i=0; i<creatureData.size(); i++)
	 * { CreatureData cd = (CreatureData) creatureData.elementAt(i);
	 * System.out.println(cd.name + "level = " + calcLevel(i)); }
	 * 
	 * }
	 */
	protected int modifyValue(int commandName, int commandValue)
	{
		AbilityCommand command = new AbilityCommand(commandName, commandValue, getCreature().head, getCreature().hands, getCreature().humanoid);

		for (int i = 0; i < abilities.size(); i++)
		{
			command.value = abilities.get(i).executeCommandValue(command, this);
		}

		return command.value;
	}

	protected static int calcLevel(int id)
	{
		Data cd = (Data) creatureData.elementAt(id);

		return cd.value;

	}
	
	protected boolean makeMeleeAttack(Creature target)
	{
		boolean attackComplete = false;

		AbilityCommand command = new AbilityCommand(AbilityCommand.MELEE_ATTACK, 0, getCreature().head, getCreature().hands, getCreature().humanoid);

		// first try to attack with the currently selected melee ability
		for (int i = 0; !attackComplete && i < abilities.size(); i++)
		{
			// We dont tell the dungeon about the attack here, becaues the Ability will do that...
			attackComplete = abilities.get(i).executeCommandTarget(command, target, target.getPosition(), this);
		}
		
		return attackComplete;  // not required any more, but I cant be bothered removing it.  Just dont pay it no mind.
	}
	
	protected void death()
	{
		unsetAbilities(false);
		
		if (this.myId == 0) // Nashkur the evil wizard is dead!!!
			;//dungeonQuery.playerWins();
	}

	public void dropAllPhysicalItems() {
		// Drop all physical ability items
		List<Ability> newlyDroppedObjects = new LinkedList<Ability>();
		for (Ability ability : abilities) {
			if (ability.isPhysical())
				newlyDroppedObjects.add(ability);
		}
		for (Ability ability : newlyDroppedObjects) {
			dropObject(ability);
		}
	}
	
	// Sequence number has no relevance,
	protected void dropObject(Ability ability)
	{
		abilities.remove(ability);
		ability.setOwned(this, false);
		dungeonEvents.objectDrop(SequenceNumber.getCurrent(), this, ability, mapPosition);
	}

	// Whenever a creature ends its turn, this is called to wind down temporary abilities,
	// such as the effects of speed potions or poison, etc...
	public void endTurn()
	{
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
		
		return ability.meetsNeeds(hasHead, hasHands, isHumanoid);
	}

	
	public CreatureStats getCreatureStats()
	{
		// initialize for first time
		if (creatureStats.name == null) {
			creatureStats.name = creature.name;
			int maxHealth = calculateMaxHealth();
			int maxMagic = calculateMaxMagic();
			creatureStats.health = health;
			creatureStats.maxHealth = maxHealth;
			creatureStats.magic = magic;
			creatureStats.maxMagic = maxMagic;
			creatureStats.experience = experience;
		}
		return creatureStats;
	}
	
	public boolean hasEnoughMagic(Ability ability) {
		if (ability.ability.magicCost > magic) {
			return false;
		} else {
			return true;
		}
	}
	


}