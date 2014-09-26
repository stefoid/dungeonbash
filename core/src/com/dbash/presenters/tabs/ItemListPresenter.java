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
		Character currentCharacter = model.presenterTurnState.getCurrentCharacter();
		dungeonItemList = model.presenterDungeon.getItemsAtPosition(currentCharacter.getPosition());
		characterItemList = currentCharacter.getItemList();
		
		final Character character = model.presenterTurnState.getCurrentCharacter();
		
		// add the things on the dungeon floor at this spot.
		for (AbilityInfo abilityInfo : dungeonItemList) {
			// add the closure to the element about what to do if it is selected.
			final Ability ability = abilityInfo.ability;
			abilityInfo.isUsableByOwner = character.canUseAbility(ability);
			abilityInfo.canBeCarried = character.canCarry(ability);
			ItemListElementView element = new ItemListElementView(gui, character, abilityInfo, elementArea);
			element.addToList(elements);
			element.onSelection(new ISelectionListener() {
				public void processSelection() {
					if (ability != null) {
						boolean pickupAllowed = character.itemPickupSelected(ability);
						if (pickupAllowed == false) {
							gui.audio.playSound(Audio.NEGATIVE);
						}
					}
				}
			});
		}
		
		// add the character inventory to the list.
		for (AbilityInfo abilityInfo : characterItemList) {
			ItemListElementView element = new ItemListElementView(gui, character, abilityInfo, elementArea);
			element.addToList(elements);

			// add the closure to the element about what to do if it is selected.
			final Ability ability = abilityInfo.ability;
			element.onSelection(new ISelectionListener() {
				public void processSelection() {
					if (ability != null) {
						saveListPosition();  // do this first because processing the ability will create a new list
						character.itemDropSelected(ability);
					}
				}
			});
		}
		
		// if the item list is totally empty, put some text there to say that so it doesnt look dumb
		if (elements.size() == 0) {
			AbilityInfo firstInfo = new AbilityInfo("No items carried");
			firstInfo.isCarried = true;
			ItemListElementView first = new ItemListElementView(gui, null, firstInfo, elementArea);
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
