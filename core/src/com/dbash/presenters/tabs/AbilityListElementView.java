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
	protected ImageView reticuleIcon;
	
	public AbilityListElementView(UIDepend gui, AbilityInfo abilityInfo, Rect area) {
		super(gui, abilityInfo, area);
		
		// TODO here we turn the ability info into a a specific bunch of text and icons at positions relative
		// to the bottom-left of the list element area.  +ve is up, -ve is down.
		elementBackground = new ImageView(gui, "ABILITY_AVAILABLE_IMAGE", area);
		selectedBackground = new ImageView(gui, "ABILITY_SELECTED_IMAGE", area);
		cantUseBackground = new ImageView(gui, "ABILITY_DISABLED_IMAGE", area);
		
		Rect targetArea = new Rect(iconArea);
		targetArea.x = area.x + iconArea.width * 2.5f;
		
		if (abilityInfo.targetable) {
			targetIcon = new ImageView(gui, "TARGET_IMAGE", targetArea);
		} else {
			targetIcon = null;
		}
		
		if (abilityInfo.aimed && abilityInfo.targetable) {
			Rect reitculeArea = new Rect(iconArea);
			reitculeArea.x = targetArea.x + targetArea.width * 1.5f;
			reticuleIcon = new ImageView(gui, "RETICULE_IMAGE", reitculeArea);
		} else if (abilityInfo.burstEffect) {
			Rect reitculeArea = new Rect(iconArea);
			reitculeArea.x = targetArea.x + targetArea.width * 1.5f;
			reticuleIcon = new ImageView(gui, "BURST_ICON", reitculeArea);
		} else {
			reticuleIcon = null;
		}
	}
	
	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		
		// Draw background
		if (abilityInfo.currentySelected || (abilityInfo.isSetable && abilityInfo.isSet)) {
			selectedBackground.setPos(x, y);
			selectedBackground.draw(spriteBatch);
		} else if (abilityInfo.enoughMagic == false || abilityInfo.isUsableByOwner == false){
			cantUseBackground.setPos(x, y);
			cantUseBackground.draw(spriteBatch);
		} else {
			elementBackground.setPos(x, y);
			elementBackground.draw(spriteBatch);
		}
		
		super.draw(spriteBatch, x, y);
		
		// ability type
		abilityType.draw(spriteBatch, x, y);
		
		// target
		if (targetIcon != null) {
			targetIcon.draw(spriteBatch, x, y);
		}
		
		if (reticuleIcon != null) {
			reticuleIcon.draw(spriteBatch, x, y);
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

	@Override
	public void setAnimating() {
	}
}
