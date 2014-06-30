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
public class EyeDetailListPresenter extends ListPresenter{
	
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
		
		int extraElements = (int) (gui.sizeCalculator.ELEMENTS_PER_SCREEN/2);
		
		// Add creature details elements, plus empty list elements to act as spacers..
		if (creature != null) {
			for (int i=0; i<extraElements; i++) {
				elements.add(new IListElement.EmptyListElement(gui, null, elementArea));
			}
			Rect creatureArea = new Rect(elementArea);
			creatureArea.height *= (extraElements+1);
			CreatureListElementView creatureElement = new CreatureListElementView(gui, creature.getCreatureStats(), creatureArea);
			elements.add(creatureElement);
		}
		
		// add the things on the dungeon floor at this spot.
		for (AbilityInfo abilityInfo : dungeonItemList) {
			// add the closure to the element about what to do if it is selected.
			abilityInfo.isUsableByOwner = true;  // set this so as not to show the diasbled status
			ItemListElementView element = new ItemListElementView(gui, abilityInfo, elementArea);
			elements.add(element);
		}
		
		// fill up to min elements by adding empty ones.
		while (elements.size() < gui.sizeCalculator.MIN_ELEMENTS) {
			elements.add(new IListElement.EmptyListElement(gui, "ITEM_ON_FLOOR_IMAGE", elementArea));
		}
		
		// always draws at the same position it already was at before.
		scrollingList.setListElements(elements, scrollingList.getListPosition());
	}
}
