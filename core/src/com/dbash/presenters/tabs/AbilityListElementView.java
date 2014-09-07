package com.dbash.presenters.tabs;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.AbilityInfo;
import com.dbash.platform.ImageView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.IListElement;
import com.dbash.util.Rect;


public class AbilityListElementView extends AbilityTypeListElement {
	
	// Ability background
	protected ImageView	elementBackground;
	protected ImageView selectedBackground;
	protected ImageView cantUseBackground;
	protected ImageView targetIcon;
	
	public AbilityListElementView(UIDepend gui, AbilityInfo abilityInfo, Rect area) {
		super(gui, abilityInfo, area);
		
		// TODO here we turn the ability info into a a specific bunch of text and icons at positions relative
		// to the bottom-left of the list element area.  +ve is up, -ve is down.
		elementBackground = new ImageView(gui, "ABILITY_AVAILABLE_IMAGE", area);
		selectedBackground = new ImageView(gui, "ABILITY_SELECTED_IMAGE", area);
		cantUseBackground = new ImageView(gui, "ABILITY_DISABLED_IMAGE", area);
		
		if (abilityInfo.targetable) {
			Rect targetArea = new Rect(iconArea);
			targetArea.x = area.x + iconArea.width * 2.5f;
			targetIcon = new ImageView(gui, "TARGET_IMAGE", targetArea);
		} else {
			targetIcon = null;
		}
	}
	
	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		
		// Draw background
		if (abilityInfo.currentySelected) {
			selectedBackground.setPos(x, y);
			selectedBackground.draw(spriteBatch);
		} else if (abilityInfo.enoughMagic ){
			elementBackground.setPos(x, y);
			elementBackground.draw(spriteBatch);
		} else {
			cantUseBackground.setPos(x, y);
			cantUseBackground.draw(spriteBatch);
		}
		
		super.draw(spriteBatch, x, y);
		
		// ability type
		abilityType.draw(spriteBatch, x, y);
		
		// target
		if (abilityInfo.targetable) {
			targetIcon.draw(spriteBatch, x, y);
		}
		
		// magic cost
		if (magicIcon != null) {
			magicIcon.draw(spriteBatch, x, y);
			magicCost.draw(spriteBatch, x, y);
		}
	}
	
	@Override
	public void addToList(ArrayList<IListElement> list) {
		list.add(this);
	}
	
	@Override
	public void clearDrawFlag() {
		drawFlag = false;
	}
}
