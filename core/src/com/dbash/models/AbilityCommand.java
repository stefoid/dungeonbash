package com.dbash.models;

public class AbilityCommand 
{
	// INTERFACE

	// execute commands
	public static final int		RESIST_HARD = 1;
	public static final int		RESIST_SHARP = 2;
	public static final int		RESIST_ENERGY = 3;
	public static final int		RESIST_CHEMICAL = 4;	
	
	public static final int		CLEAR_ARMOR = 5;  // used to make this ability not the used ability of its type
	public static final int		CLEAR_AMULET = 6;
	public static final int		CLEAR_MELEE = 7;
	public static final int		CLEAR_NONE = 26;  // dont clear any other ability when set
	public static final int		CLEAR = 27;  // clear the setness of this ability only.
	
	public static final int		MELEE_ATTACK = 8;
	public static final int		SET = 9;   // used to set this ability as the 'used' ability of any type.
	
	public static final int		MODIFY_SPEED = 10;
	public static final int		MODIFY_HEALTH = 11;
	public static final int		MODIFY_MAX_HEALTH = 12;
	public static final int		MODIFY_MAGIC = 13;
	public static final int		MODIFY_MAX_MAGIC = 14;
	public static final int		MODIFY_ATTACK_SKILL = 15;
	public static final int		MODIFY_DEFENCE_SKILL = 16;
	public static final int		MODIFY_STEALTH = 22;
	public static final int		MODIFY_DETECT = 23;
	public static final int		MODIFY_MISSILE_DEFENCE = 24;
	public static final int		RESIST_BURST = 25;
	
	public static final int		INVOKE = 18;
	public static final int		EXECUTE = 19;
	public static final int		CANCEL = 20;

	public static final int 	RESIST_ABILITY = 21;


	public AbilityCommand()
	{

	}

	public AbilityCommand(int commandName, int commandValue, int hasHead, int hasHands, int isHumanoid)
	{
		name = commandName;
		value =  commandValue;
		if (hasHead > 0)
			head = true;
		else
			head = false;
		if (hasHands > 0)
			hands = true;
		else
			hands = false;
		if (isHumanoid > 0)
			humanoid = true;
		else
			humanoid = false;
	}

	// ATTRIBUTES
	public int 		name;
	public int		value;
	public boolean	head;
	public boolean  hands;
	public boolean	humanoid;
	// INTERFACE
	public static final int		NO_PHYSICAL_ATTACK = 0;
	public boolean melee;
	public static final int HARD_ATTACK = 1;
	public static final int SHARP_ATTACK = 2;
	public static final int ENERGY_ATTACK = 3;
	public static final int CHEMICAL_ATTACK = 4;
	public static final int KNOCKBACK = 5;
	public Ability abilityToAdd;
	public Ability ability; // attacking ability
	public int damage;
	public int skill;
	public int type;

	public AbilityCommand(int attackType, int attackDamage, boolean meleeAttack, Ability abilityToAdd, Ability attackingAbility)
	{
		type = attackType;
		damage = attackDamage;
		this.abilityToAdd = abilityToAdd;
		melee = meleeAttack;
		this.ability = attackingAbility;
	}

	// METHODS
	public AbilityCommand(int command) {
		name = command;
		value = 0;
		head = true;
		hands = true;
		humanoid = true;
	}

	@Override
	public String toString() {
		String result;
		switch (name) {
			case		RESIST_HARD: result = "RESIST_HARD"; break;
			case		RESIST_SHARP: result = "RESIST_SHARP"; break;
			case		RESIST_ENERGY: result = "RESIST_ENERGY"; break;
			case		RESIST_CHEMICAL: result = "RESIST_CHEMICAL"; break;
			case		CLEAR_ARMOR: result = "CLEAR_ARMOR"; break;
			case		CLEAR_AMULET: result = "CLEAR_AMULET"; break;
			case		CLEAR_MELEE: result = "CLEAR_MELEE"; break;
			case		MELEE_ATTACK: result = "MELEE_ATTACK"; break;
			case		SET: result = "SET"; break;
			case		MODIFY_SPEED: result = "MODIFY_SPEED"; break;
			case		MODIFY_HEALTH: result = "MODIFY_HEALTH"; break;
			case		MODIFY_MAX_HEALTH: result = "MODIFY_MAX_HEALTH"; break;
			case		MODIFY_MAGIC: result = "MODIFY_MAGIC"; break;
			case		MODIFY_MAX_MAGIC: result = "MODIFY_MAX_MAGIC"; break;
			case		MODIFY_ATTACK_SKILL: result = "MODIFY_ATTACK_SKILL"; break;
			case		MODIFY_DEFENCE_SKILL: result = "MODIFY_DEFENCE_SKILL"; break;
			case		MODIFY_STEALTH: result = "MODIFY_STEALTH"; break;
			case		MODIFY_DETECT: result = "MODIFY_DETECT"; break;
			case		INVOKE: result = "INVOKE"; break;
			case		EXECUTE: result = "EXECUTE"; break;
			case		CANCEL: result = "CANCEL"; break;
			case 		RESIST_ABILITY: result = "RESIST_ABILITY"; break;
			case		MODIFY_MISSILE_DEFENCE: result = "MODIFY_MISSILE_DEFENCE"; break;
			case 		RESIST_BURST: result = "RESIST_BURST"; break;
			default: result = "unknwon"; break;
		}
		
		return result;
	}
}

