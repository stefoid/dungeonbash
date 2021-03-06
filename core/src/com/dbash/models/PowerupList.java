package com.dbash.models;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


//each Character has a AbilitySelectionList that has the following responsibilities.
//
//.  as the list is specifically designed to be consumed by a Presenter, the info it contains is geared towards player consumption. 'AbilityInfo'.
//.  maintains a specifically ordered list of AbilityInfo meant for the player to interact with.
//.  encapsulates the logic to order the list.
//.  registers observers who want to know when the list content changes
//.  modifies the list according to events from the character such as an ability being acquired, dropped, used, equipped, etc�

// This list is of physical items carried by the creature.
@SuppressWarnings("serial")
public class PowerupList extends ArrayList<AbilityInfo>{
	
	public Character owner;
	
	public PowerupList(Character owner, List<Ability> powerups, boolean available, int availableXp) {
		
		super();
		
		this.owner = owner;
		
		//AbilityInfo info = new AbilityInfo("fred");
		//add(info);
		// Sort list according to type
		//info.sortValue = info.abilityType.val;
		
		for (Ability ability : powerups) {
			AbilityInfo info = new AbilityInfo(ability, owner);
			info.isAvailable = available;
			if (available && info.xpCost > availableXp) {
				info.isAffordable = false;
			}
			add(info);
		}
		
		// Now sort the list according to usageCount and that is the order presented to the player.
		//Collections.sort(this);
	}
	
	// Can just instantiate the list and add abilities to it one at a time, or in a vector below
	public PowerupList() {
		super();
	}
	
	public PowerupList(PowerupList parent) {
		owner = parent.owner;
		addAll(parent);
	}
	
	public void addAbility(Ability ability) {
		//if (ability.isPhysical() || (ability.isRoughTerrain() && includeRoughTerrain)) {

			AbilityInfo info = new AbilityInfo(ability, null);
			
			// its possible to order abilities by value maybe?  There is a value stat.
			
//			if (info.equipped) {
//				info.usageCount += 200000;
//			}
//			} else if (info.usableByThisCreature) {
//				info.usageCount += 100000;
//			}
			
			add(info);
	//	}
	}
	
	// This list is of physical items at a specific dungeon location (i.e. the eye location).
	public PowerupList(List<Ability> abilities) {
		super();
		
		for (Ability ability : abilities) {
				addAbility(ability);
		}
		
		// Now sort the list according to usageCount and that is the order presented to the player.
		//sort();
	}
	
	public void sort() {
		Collections.sort(this);
	}
}