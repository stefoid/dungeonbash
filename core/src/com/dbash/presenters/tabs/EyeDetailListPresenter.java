package com.dbash.presenters.tabs;

import java.util.ArrayList;

import com.dbash.models.AbilityInfo;
import com.dbash.models.Creature;
import com.dbash.models.ItemList;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEventProvider;
import com.dbash.models.UIInfoListener;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.IListElement;
import com.dbash.presenters.widgets.ListPresenter;
import com.dbash.util.Rect;


// Takes a list of AbilityInfo derived from the owners carried physical items, and combines it with
// a list of items at the owners location in the dungeon, and allows the player to drop items from the former
// and pick up items from the latter.
// displays it as a ScrollingList of ListElements.
public class EyeDetailListPresenter extends ListPresenter {
	
	private ItemList dungeonItemList;
	
	public EyeDetailListPresenter(PresenterDepend model, UIDepend gui, TouchEventProvider touchEventProvider, Rect area) {
		super(model, gui, touchEventProvider, area);
	}

	@Override
	protected void setup()
	{
		// Subscribe to changes to the eye details in the dungeon.
		model.presenterDungeon.onChangeToEyeDetails(new UIInfoListener() {
			public void UIInfoChanged() {
				listInfoUpdate();
			}
		});
		listInfoUpdate();
	}
	
	// Called when the underlying ability list in the model changes in some way.
	// We take the new list of AbilityInfo, create a new list of AbilityListElements out of it and
	// tell our ScrollingList to use that.
	@Override
	public void listInfoUpdate() {
		// Make up the ListElements to feed the ScrollingList
		ArrayList<IListElement> elements = new ArrayList<IListElement>();
		Creature creature = model.presenterDungeon.getCreatureAtEyePos();
		dungeonItemList = model.presenterDungeon.getItemsAtEyepos();

		// Add creature details element
		if (creature != null) {
			Rect creatureArea = new Rect(elementArea);
			CreatureListElementView creatureElement = new CreatureListElementView(gui, creature.getCreatureStats(), creatureArea);
			creatureElement.addToList(elements);
		}
		
		// add the things on the dungeon floor at this spot.
		for (AbilityInfo abilityInfo : dungeonItemList) {
			// add the closure to the element about what to do if it is selected.
			abilityInfo.isUsableByOwner = true;  // set this so as not to show the diasbled status
			ItemListElementView itemElement = new ItemListElementView(gui, abilityInfo, elementArea);
			itemElement.addToList(elements);
		}
		
		// fill up to min elements by adding empty ones.
		while (elements.size() < gui.sizeCalculator.MIN_ELEMENTS) {
			IListElement.EmptyListElement emptyItem = new IListElement.EmptyListElement(gui, "ITEM_ON_FLOOR_IMAGE", elementArea);
			emptyItem.addToList((elements));
		}
		
		// always draws at the same position it already was at before.
		scrollingList.setListElements(elements, scrollingList.getListPosition());
	}
}
