package com.dbash.models;
public	 class Data
{
	// abaility data
	public String		name;
	public String		tag;
	public int			colour;
	public int			needs;
	public int			invokingStrategy;	// what happens when this ability is selected?
	public int			duration; // if -1 duration is permanent, otherwise duration counts down each tick(), then destroys itself		
	public int			physicalItem;  // does this ability represent a visible, physical item? 
	public int			magicCost;
	public boolean		aimed;
	public int			value;	   // how powerful is this ability?  if a physical item, what level should it appear in the dungeon?
	public boolean		offensive; // is this ability used against others, or on oneself?
	public int			command[];			// what command, if any, does this ability respond to?
	public int			executeStrategy[];	// what kind of operation does this ability perform in response to an execute command?
	public int			executeParam1[];	// parameter 1 for execute strategy.  i.e. for attack, param 1 = attack type
	public int			executeParam2[];	// parameter 2 for execute strategy.  i.e. for attack, param 2 = attack damage
	public int			executeParam3[];	// parameter 3 for execute strategy.  i.e. for attack, param 3 = ability id if part of attack		
	public int			executeParam4[];	// parameter 4 for execute strategy.  i.e. for attack, param 4 = range of area effect	


	// creature data
	//public String		name;
	//public String		gifName;
	//public int		colour;
	public int 			head;					// can it wear an amulet?
	public int 			hands;					// can it wield a melee weapon, wand or missile weapon or read a scroll?
	public int 			humanoid;				// can it wear armour?	
	public int 			swarm;					// is this creature a type that congregates in a group?

	// the above attributes are fixed for the creature, while those below may change so need instance variables
	public int			maximumHealth;			
	public int			maximumMagic;					
	public int			speed;					
	public int			attackSkill;			
	public int			defenceSkill;	
	//public int		value;
	public int 			stealth;
	public int 			detect;
	public int 			starter;
	public int[]		abilityIds;

}





