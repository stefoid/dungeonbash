package com.dbash.presenters.tabs;
import java.util.ArrayList;
import java.util.Vector;

import com.dbash.models.Ability.AbilityEffectType;
import com.dbash.models.AbilityInfo;
import com.dbash.models.Character;
import com.dbash.models.EffectList;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.IListElement;
import com.dbash.presenters.widgets.ListPresenter;
import com.dbash.util.Rect;


// Takes a list of AbilityInfo derived from the owners abilities and 
// displays it as a ScrollingList of ListElements.
public class EffectListPresenter extends ListPresenter{
	
	private EffectList abilityInfoList;
	
	public EffectListPresenter(PresenterDepend model, UIDepend gui, TouchEventProvider touchEventProvider, Rect area) {
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
		abilityInfoList = character.getEffectList();
		
		for (AbilityInfo abilityInfo : abilityInfoList) {
			EffectListElementView element = new EffectListElementView(gui, abilityInfo, elementArea);
			elements.add(element);
		}
		
		// if the effect list is totally empty, put some text there to say that so it doesnt look dumb
		if (elements.size() == 0) {
			AbilityInfo firstInfo = new AbilityInfo("No effects");
			// Make the effect list have an 'ability effect' in it, because the star looks nice!
			firstInfo.abilityEffects = new Vector<AbilityEffectType>();
			firstInfo.abilityEffects.add(AbilityEffectType.NONE_REALLY);
			EffectListElementView first = new EffectListElementView(gui, firstInfo, elementArea);
			elements.add(first);
		}
		
		// fill up to min elements by adding empty ones.
		while (elements.size() < gui.sizeCalculator.MIN_ELEMENTS) {
			elements.add(new IListElement.EmptyListElement(gui, "EFFECT_IMAGE", elementArea));
		}
		
		float listPos = characters.get(character);
		scrollingList.setListElements(elements, listPos);
	}
	
	@Override
	protected void newCharacter(Character character)
	{	
		super.newCharacter(character);
		
		
		// Actually, no need for this, since you cant change anything while viewing this tab.  right no at least.
//		character.onChangeToEffectList((new UIInfoListener() {
//			public void UIInfoChanged() {
//				listInfoUpdate();
//			}
//		}));
	}
}
