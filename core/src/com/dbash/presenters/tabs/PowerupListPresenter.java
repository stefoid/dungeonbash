package com.dbash.presenters.tabs;
import java.util.ArrayList;

import com.dbash.models.Ability;
import com.dbash.models.AbilityInfo;
import com.dbash.models.Character;
import com.dbash.models.IEventAction;
import com.dbash.models.PowerupList;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEventProvider;
import com.dbash.models.UIInfoListener;
import com.dbash.platform.Audio;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.IListElement;
import com.dbash.presenters.widgets.ISelectionListener;
import com.dbash.presenters.widgets.ListPresenter;
import com.dbash.util.EventBus;
import com.dbash.util.Rect;


// Takes a list of AbilityInfo derived from the owners carried physical items, and combines it with
// a list of items at the owners location in the dungeon, and allows the player to drop items from the former
// and pick up items from the latter.
// displays it as a ScrollingList of ListElements.
public class PowerupListPresenter extends ListPresenter{
	
	public PowerupListPresenter(PresenterDepend model, UIDepend gui, TouchEventProvider touchEventProvider, Rect area) {
		super(model, gui, touchEventProvider, area);
		
		EventBus.getDefault().onEvent(Character.POWERUP_LIST_CHANGED, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if ((Character) param == currentCharacter) {
					listInfoUpdate();
				}
			}
		});
	}
	
	// Called when the underlying ability list in the model changes in some way.
	// We take the new list of AbilityInfo, create a new list of AbilityListElements out of it and
	// tell our ScrollingList to use that.
	@Override
	public void listInfoUpdate() {
		PowerupList purchasedPowerupList;
		PowerupList availablePowerupList;
		// Make up the ListElements to feed the ScrollingList
		ArrayList<IListElement> elements = new ArrayList<IListElement>();
		final Character character = model.presenterTurnState.getCurrentCharacter();
		availablePowerupList = character.getAvailablePowerupList();
		purchasedPowerupList = character.getPurchasedPowerupList();
		int index = 0;
		
		for (AbilityInfo abilityInfo : purchasedPowerupList) {
			final PowerupListElementView element = new PowerupListElementView(gui, character, abilityInfo, elementArea, index++);
			element.addToList(elements);

			// add the closure to the element about what to do if it is selected.
			final Ability ability = abilityInfo.ability;
			element.onSelection(new ISelectionListener() {
				public void processSelection() {
					if (ability != null) {
						saveListPosition();  // do this first because processing the ability will create a new list
						character.sellPowerup(ability);
						gui.audio.playSound(Audio.COINS);
						element.abilityInfo.isAvailable = true;
						element.setBackgroundImage();
						scrollItem(element);
					} else {
						gui.audio.playSound(Audio.NEGATIVE);
					}
				}
			});
		}
		
		for (AbilityInfo abilityInfo : availablePowerupList) {
			// add the closure to the element about what to do if it is selected.
			final Ability ability = abilityInfo.ability;
			final boolean canAfford = abilityInfo.isAffordable;
			
			final PowerupListElementView element = new PowerupListElementView(gui, character, abilityInfo, elementArea, index++);
			element.addToList(elements);
			element.onSelection(new ISelectionListener() {
				public void processSelection() {
					if (ability != null) { 
						if (canAfford == false) {
							gui.audio.playSound(Audio.NEGATIVE);
						} else {
							saveListPosition();  // do this first because processing the ability will create a new list
							character.buyPowerup(ability);
							gui.audio.playSound(Audio.CASH_REGISTER);
							element.abilityInfo.isAvailable = false;
							element.setBackgroundImage();
							scrollItem(element);
						}
					}
				}
			});
		}
		
		// if the item list is totally empty, put some text there to say that so it doesnt look dumb
		if (elements.size() == 0) {
			AbilityInfo firstInfo = new AbilityInfo("No powerups left", false);
			firstInfo.isCarried = true;
			firstInfo.isAffordable = true;
			firstInfo.isAvailable= true;
			PowerupListElementView first = new PowerupListElementView(gui, null, firstInfo, elementArea, index++);
			first.addToList(elements);
		}
		
		// fill up to min elements by adding empty ones.
		while (elements.size() < gui.sizeCalculator.MIN_ELEMENTS) {
			IListElement.EmptyListElement emptyItem = new IListElement.EmptyListElement(gui, "POWERUP_AVAILABLE_IMAGE", elementArea, null, 0);
			emptyItem.addToList((elements));
		}
		
		float listPos = characters.get(character);
		scrollingList.setListElements(elements, listPos);
	}
	
	/**
	 * This element has to scroll from its old position to the position it will be in the new list that results.
	 * To do that, we make a new list and determine the position first.
	 * Then we add an animation to the ScollingListView from the old position to the new one.
	 */
	public void scrollItem(PowerupListElementView element) {
		// Build new list.
		Character character = model.presenterTurnState.getCurrentCharacter();
		PowerupList newCombinedList = character.getPurchasedPowerupList();
		PowerupList newAvailablePowerupList = character.getAvailablePowerupList(); 
		for (AbilityInfo abilityInfo : newAvailablePowerupList) {
			newCombinedList.add(abilityInfo);
		}
		
		// Now find position of ability within that.
		int count = 0;
		for (AbilityInfo abilityInfo : newCombinedList) {
			if (abilityInfo.ability.getId() == element.abilityInfo.ability.getId()) {
				break;
			} else {
				count++;
			}
		}
		
		// tell the scrolling list to animate that element.
		scrollingList.scrollElement(element, element.index, count);
	}
	
}
