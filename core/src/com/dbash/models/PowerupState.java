package com.dbash.models;

import java.util.ArrayList;

public class PowerupState {
	
	ArrayList<Ability> buyableAbilities;
	ArrayList<Ability> boughtAbilities;
	
	
	public PowerupState() {
		
	}
	
	
	/**
	 * to determine the list of abilities that can be purchased we need to :
	 * 1) creature must have a list of purchasable abilities
	 * w) given the list of purchasable abilities, subtract those that are allready in the creatures ability list
	 * 2) stat abilities are implied.  All creatures have access to these.  They are tagged with stat, and the stat type.
	 * 3) work out the next stat increase ability give the one for each stat that the creature allready has, if any
	 * 	THIS is the initial 'BUYABLE' list for powerup state.
	 * 
	 * 5) when a player buys an ability it is added to the creature, but also to to the 'bought' ability list, and it
	 * is subtracted from the 'buyable' list.
	 * 6) if it is a stat ability, another new stat ability is added to the 'buyable' list - the next one in line.
	 * 7) a player can unbuy an ability in the 'bought' list, in which case it is removed form the characetr and put back into the
	 * 'buyable' list.
	 * 8) if it is a stat ability, any stat ability of that type is removed form the 'buyable' list first.
	 * 
	 */
	 
	
}
