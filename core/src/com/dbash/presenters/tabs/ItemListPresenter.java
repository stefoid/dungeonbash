package com.dbash.presenters.tabs;
import java.util.ArrayList;

import com.dbash.models.Ability;
import com.dbash.models.AbilityInfo;
import com.dbash.models.Character;
import com.dbash.models.ItemList;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEventProvider;
import com.dbash.models.UIInfoListener;
import com.dbash.platform.Audio;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.IListElement;
import com.dbash.presenters.widgets.ISelectionListener;
import com.dbash.presenters.widgets.ListPresenter;
import com.dbash.util.Rect;


// Takes a list of AbilityInfo derived from the owners carried physical items, and combines it with
// a list of items at the owners location in the dungeon, and allows the player to drop items from the former
// and pick up items from the latter.
// displays it as a ScrollingList of ListElements.
public class ItemListPresenter extends ListPresenter{
	
	private ItemList characterItemList;
	private ItemList dungeonItemList;
	
	public ItemListPresenter(PresenterDepend model, UIDepend gui, TouchEventProvider touchEventProvider, Rect area) {
		super(model, gui, touchEventProvider, area);
	}
	
	// Called when the underlying ability list in the model changes in some way.
	// We take the new list of AbilityInfo, create a new list of AbilityListElements out of it and
	// tell our ScrollingList to use that.
	@Override
	public void listInfoUpdate() {
		// Make up the ListElements to feed the ScrollingList
		ArrayList<IListElement> elements = new ArrayList<IListElement>();
		final Character character = model.presenterTurnState.getCurrentCharacter();
		dungeonItemList = model.presenterDungeon.getItemsAtPosition(character.getPosition());
		characterItemList = character.getItemList();
		int index = 0;
		// add the things on the dungeon floor at this spot.
		for (AbilityInfo abilityInfo : dungeonItemList) {
			// add the closure to the element about what to do if it is selected.
			final Ability ability = abilityInfo.ability;
			abilityInfo.isUsableByOwner = character.canUseAbility(ability);
			abilityInfo.canBeCarried = character.canCarry(ability);
			final ItemListElementView element = new ItemListElementView(gui, character, abilityInfo, elementArea, index++);
			element.addToList(elements);
			element.onSelection(new ISelectionListener() {
				public void processSelection() {
					if (ability != null) {
						boolean pickupAllowed = character.itemPickupSelected(ability);
						if (pickupAllowed == false) {
							gui.audio.playSound(Audio.NEGATIVE);
						} else {
							element.abilityInfo.isCarried = true;
							element.setBackgroundImage();
							scrollItem(element);
						}
					}
				}
			});
		}
		
		// add the character inventory to the list.
		for (AbilityInfo abilityInfo : characterItemList) {
			final ItemListElementView element = new ItemListElementView(gui, character, abilityInfo, elementArea, index++);
			element.addToList(elements);

			// add the closure to the element about what to do if it is selected.
			final Ability ability = abilityInfo.ability;
			element.onSelection(new ISelectionListener() {
				public void processSelection() {
					if (ability != null) {
						saveListPosition();  // do this first because processing the ability will create a new list
						character.itemDropSelected(ability);
						element.abilityInfo.isCarried = false;
						element.setBackgroundImage();
						scrollItem(element);
					}
				}
			});
		}
		
		// if the item list is totally empty, put some text there to say that so it doesnt look dumb
		if (elements.size() == 0) {
			AbilityInfo firstInfo = new AbilityInfo("No items carried");
			firstInfo.isCarried = true;
			ItemListElementView first = new ItemListElementView(gui, null, firstInfo, elementArea, index++);
			first.addToList(elements);
		}
		
		// fill up to min elements by adding empty ones.
		while (elements.size() < gui.sizeCalculator.MIN_ELEMENTS) {
			IListElement.EmptyListElement emptyItem = new IListElement.EmptyListElement(gui, "ITEM_CARRIED_IMAGE", elementArea, null, 0);
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
	public void scrollItem(ItemListElementView element) {
		// Build new list.
		Character character = model.presenterTurnState.getCurrentCharacter();
		ItemList newItemList = model.presenterDungeon.getItemsAtPosition(character.getPosition());
		ItemList charItems = character.getItemList();
		for (AbilityInfo abilityInfo : charItems) {
			newItemList.add(abilityInfo);
		}
		
		// Now find position of ability within that.
		int count = 0;
		for (AbilityInfo abilityInfo : newItemList) {
			if (abilityInfo.ability == element.abilityInfo.ability) {
				break;
			} else {
				count++;
			}
		}
		
		// tell the scrolling list to animate that element.
		scrollingList.scrollElement(element, element.index, count);
	}
	
	@Override
	protected void newCharacter(Character character)
	{	
		super.newCharacter(character);
		
		character.onChangeToInventory((new UIInfoListener() {
			public void UIInfoChanged() {
				listInfoUpdate();
			}
		}));
	}
	
}
