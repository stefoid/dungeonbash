package com.dbash.models;
import java.util.ArrayList;


//each Character has a AbilitySelectionList that has the following responsibilities.
//
//.  as the list is specifically designed to be consumed by a Presenter, the info it contains is geared towards player consumption. 'AbilityInfo'.
//.  maintains a specifically ordered list of AbilityInfo meant for the player to interact with.
//.  encapsulates the logic to order the list.
//.  registers observers who want to know when the list content changes
//.  modifies the list according to events from the character such as an ability being acquired, dropped, used, equipped, etc�


// This list is of the effects currently affecting the character.
public class EffectList extends ArrayList<AbilityInfo>{
	
	public Creature owner;
	
	public EffectList(Creature owner) {
		
		super();
		
		this.owner = owner;
		for (Ability ability : owner.abilities) {
			
			// Effects are anything that that currently has an effect on the character, such as a non-physical, non-selectable
			// ability, or a currently-equipped item that has intrinsic effects of its own, such as armor.
			AbilityInfo info = new AbilityInfo(ability, owner);
			
			// 'Effects' are non-physical, non-selectable abilities.
			if (!ability.isSelectable() && (!ability.isPhysical())) {
				
				// effects are currently not sorted into other order than what they come in.
				
				add(info);
				
			} else if (info.equipped) {
				if (info.abilityEffects.size() > 0) {
					add(info);
				}
			}
		}
		
		// Now sort the list according to usageCount and that is the order presented to the player.
		//Collections.sort(this);
	}
}