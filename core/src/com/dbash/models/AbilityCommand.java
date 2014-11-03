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
	
	public static final int		MELEE_ATTACK = 8;
	public static final int		SET = 9;   // used to set this ability as the 'used' ability of its type.
	
	public static final int		MODIFY_SPEED = 10;
	public static final int		MODIFY_HEALTH = 11;
	public static final int		MODIFY_MAX_HEALTH = 12;
	public static final int		MODIFY_MAGIC = 13;
	public static final int		MODIFY_MAX_MAGIC = 14;
	public static final int		MODIFY_ATTACK_SKILL = 15;
	public static final int		MODIFY_DEFENCE_SKILL = 16;
	public static final int		MODIFY_STEALTH = 22;
	public static final int		MODIFY_DETECT = 23;

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
	public static final int CHEMICAL_ATTACK = 4;
	public static final int ENERGY_ATTACK = 3;
	public static final int HARD_ATTACK = 1;
	public static final int SHARP_ATTACK = 2;
	public Ability ability;
	public int damage;
	public int skill;
	public int type;


	public AbilityCommand(int attackType, int attackDamage, boolean meleeAttack, Ability ability)
	{
		type = attackType;
		damage = attackDamage;
		this.ability = ability;
		melee = meleeAttack;
	}

	// METHODS
	public AbilityCommand(int command) {
		name = command;
		value = 0;
		head = true;
		hands = true;
		humanoid = true;
	}

}

