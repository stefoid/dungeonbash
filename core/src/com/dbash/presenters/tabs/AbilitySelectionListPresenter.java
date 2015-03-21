package com.dbash.presenters.tabs;
import java.util.ArrayList;

import com.dbash.models.Ability;
import com.dbash.models.AbilityInfo;
import com.dbash.models.Character;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEventProvider;
import com.dbash.models.UIInfoListener;
import com.dbash.platform.Audio;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.IListElement;
import com.dbash.presenters.widgets.ISelectionListener;
import com.dbash.presenters.widgets.ListPresenter;
import com.dbash.util.Rect;


// Takes a list of AbilityInfo derived from the owners abilities and 
// displays it as a ScrollingList of ListElements.
public class AbilitySelectionListPresenter extends ListPresenter{
	
	protected AbilitySelectionList abilityInfoList;
	
	public AbilitySelectionListPresenter(PresenterDepend model, UIDepend gui, TouchEventProvider touchEventProvider, Rect area) {
		super(model, gui, touchEventProvider, area);
	}
	
	// Called when the underlying ability list in the model changes in some way.
	// We take the new list of AbilityInfo, create a new list of AbilityListElements out of it and
	// tell our ScrollingList to use that.
	@Override
	public void listInfoUpdate() {
		// Make up the ListElements to feed the ScrollingList
		ArrayList<IListElement> elements = new ArrayList<IListElement>();
		Character character = model.presenterTurnState.getCurrentCharacter();
		abilityInfoList = character.getAbilitySelectionList();
		
		for (AbilityInfo abilityInfo : abilityInfoList) {
			AbilityListElementView element = new AbilityListElementView(gui, abilityInfo, elementArea);
			element.addToList(elements);

			// add the closure to the element about what to do if it is selected.
			final Character owner = abilityInfoList.owner;
			final Ability ability = abilityInfo.ability;
			final String soundEffect;
			if (abilityInfo.oneShot) {
				soundEffect = null;
			} else if (abilityInfo.isUsableByOwner == false){ 
				soundEffect = Audio.NEGATIVE;
			} else {
				switch (abilityInfo.abilityType) {
					case WEAPON:
						soundEffect = Audio.ITEM_SELECT_MELEE;
						break;
					case RANGED:
					case WAND:
						soundEffect = Audio.ITEM_SELECT_PROJ;
						break;
					case ARMOR:
						soundEffect = Audio.ITEM_PICKUP;
						break;
					default:
						soundEffect = Audio.ITEM_SELECT_ACC;
					break;
				}		
			}

			element.onSelection(new ISelectionListener() {
				public void processSelection() {
					if (ability != null) {
						saveListPosition();  // do this first because processing the ability will create a new list
						boolean canSelect = model.presenterTurnState.abilitySelected(owner, ability);
						if (canSelect) {
							if (soundEffect != null) {
								gui.audio.playSound(soundEffect);
							}
						} else {
							gui.audio.playSound(Audio.NEGATIVE);
						}
						
					}
				}
			});
		}
		
		// fill up to min elements by adding empty ones.
		while (elements.size() < gui.sizeCalculator.MIN_ELEMENTS) {
			IListElement.EmptyListElement emptyItem = new IListElement.EmptyListElement(gui, "ABILITY_AVAILABLE_IMAGE", elementArea, null, 0);
			emptyItem.addToList((elements));
		}
		
		float listPos = characters.get(character);
		scrollingList.setListElements(elements, listPos);
	}
	
	@Override
	protected void newCharacter(Character character)
	{	
		super.newCharacter(character);
		
		character.onChangeToAbilitySelectionList((new UIInfoListener() {
			public void UIInfoChanged() {
				listInfoUpdate();
			}
		}));
	}
}