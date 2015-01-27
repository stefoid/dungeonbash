package com.dbash.presenters.tabs;
import java.util.ArrayList;
import java.util.Collections;

import com.dbash.models.Ability;
import com.dbash.models.AbilityInfo;
import com.dbash.models.Character;


//each Character has a AbilitySelectionList that has the following responsibilities.
//
//.  as the list is specifically designed to be consumed by a Presenter, the info it contains is geared towards player consumption. 'AbilityInfo'.
//.  maintains a specifically ordered list of AbilityInfo meant for the player to interact with.
//.  encapsulates the logic to order the list.
//.  registers observers who want to know when the list content changes
//.  modifies the list according to events from the character such as an ability being acquired, dropped, used, equipped, etc�

@SuppressWarnings("serial")
public class AbilitySelectionList extends ArrayList<AbilityInfo> {
	
	public Character owner;
	
	public AbilitySelectionList(Character owner) {
		
		super();
		
		this.owner = owner;
		for (Ability ability : owner.abilities) {
			
			// && (owner.canUseAbility(ability))
			
			// Cant be used directly by the owning character, so doesnt belong in the ability selection list.
			if (ability.isSelectable()) {
				AbilityInfo info = new AbilityInfo(ability, owner);
				
				// Sort list according to type
				info.sortValue = info.abilityType.val * 1000;  // x10 so I can insert thigns between other things after this
				info.sortValue += info.ability.myId; // group identical things together
				if (ability.hasTag(Ability.DASH_TAG)) {
					info.sortValue = 0;
				}
				add(info);
			}
		}
		
		// re-value abilities that are linked to other abilities such as Foul Gas to a Foul amulet.
		for (AbilityInfo info : this) {
			if (info.creates >= 0) {
				setSortValue(info.creates, info.sortValue);
			}
		}
		
		// Now sort the list according to usageCount and that is the order presented to the player.
		Collections.sort(this);
	}
	
	private void setSortValue(int abilityId, int newSortValue) {
		for (AbilityInfo info : this) {
			if (info.ability.myId == abilityId) {
				info.sortValue = newSortValue + 1;  // put it after its creator in the list
			}
		}
	}
}
