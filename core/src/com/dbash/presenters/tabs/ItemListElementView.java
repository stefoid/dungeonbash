package com.dbash.presenters.tabs;

import java.util.ArrayList;

import com.dbash.models.Ability;
import com.dbash.models.Character;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.AbilityInfo;
import com.dbash.platform.ImageView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.IListElement;
import com.dbash.util.Rect;


public class ItemListElementView extends AbilityTypeListElement {
	
	// Item background
	protected ImageView backgroundImage;
	
	
	public ItemListElementView(UIDepend gui, Character currentCharacter, AbilityInfo abilityInfo, Rect area) {
		super(gui, abilityInfo, area);
		
		// set effects and position
		if (abilityInfo.abilityEffects != null) {
			setEffects(leftSide * area.width + iconSpacer * iconArea.width);
		}
		
		String dbImage = "ABILITY_DISABLED_IMAGE";

		if (abilityInfo.isCarried) {
			dbImage = "ITEM_CARRIED_IMAGE";
		} else if (abilityInfo.canBeCarried) {
			dbImage = "ITEM_ON_FLOOR_IMAGE";
		} 
		
		backgroundImage = new ImageView(gui, dbImage, area);
	}

	
	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		
		// Draw background
		backgroundImage.setPos(x, y);
		backgroundImage.draw(spriteBatch);
		
		super.draw(spriteBatch, x, y);
		
		abilityType.draw(spriteBatch, x, y);
		
		// item effects
		if (abilityEffects != null) {
			for (ImageView effect : abilityEffects) {
				effect.draw(spriteBatch, x, y);
			}
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
