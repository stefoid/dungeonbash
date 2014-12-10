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
public class ItemList extends ArrayList<AbilityInfo>{
	
	public Character owner;
	boolean includeRoughTerrain;
	
	public ItemList(Character owner, boolean includeRoughTerrain) {
		
		super();
		
		this.owner = owner;
		this.includeRoughTerrain = includeRoughTerrain;
		
		for (Ability ability : owner.abilities) {
			
			// Cant be used directly by the owning character, so doesnt belong in the ability selection list.
			if (ability.isPhysical()) {
				if (ability.isRoughTerrain() && includeRoughTerrain == false) {
					return;
				}
				AbilityInfo info = new AbilityInfo(ability, owner);
				
				// Sort list according to type
				//info.sortValue = info.abilityType.val;
				
				add(info);
			}
		}
		
		// Now sort the list according to usageCount and that is the order presented to the player.
		//Collections.sort(this);
	}
	
	// Can just instantiate the list and add abilities to it one at a time, or in a vector below
	public ItemList(boolean includeRoughTerrain) {
		super();
		this.includeRoughTerrain = includeRoughTerrain;
	}
	
	public ItemList(ItemList parent) {
		owner = parent.owner;
		includeRoughTerrain = parent.includeRoughTerrain;
		addAll(parent);
	}
	
	public void addAbility(Ability ability) {
		if (ability.isPhysical() || (ability.isRoughTerrain() && includeRoughTerrain)) {

			AbilityInfo info = new AbilityInfo(ability, null);
			
			// its possible to order abilities by value maybe?  There is a value stat.
			
//			if (info.equipped) {
//				info.usageCount += 200000;
//			}
//			} else if (info.usableByThisCreature) {
//				info.usageCount += 100000;
//			}
			
			add(info);
		}
	}
	
	// This list is of physical items at a specific dungeon location (i.e. the eye location).
	public ItemList(List<Ability> abilities, boolean includeRoughTerrain) {
		super();
		
		this.includeRoughTerrain = includeRoughTerrain;
		
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