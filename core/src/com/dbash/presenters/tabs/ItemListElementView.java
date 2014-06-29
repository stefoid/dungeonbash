package com.dbash.presenters.tabs;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.AbilityInfo;
import com.dbash.platform.ImageView;
import com.dbash.platform.UIDepend;
import com.dbash.util.Rect;


public class ItemListElementView extends AbilityTypeListElement {
	
	// Item background
	protected ImageView	carriedBackground;
	protected ImageView dungeonBackground;
	
	
	public ItemListElementView(UIDepend gui, AbilityInfo abilityInfo, Rect area) {
		super(gui, abilityInfo, area);
		
		// set effects and position
		if (abilityInfo.abilityEffects != null) {
			setEffects(leftSide * area.width + iconSpacer * iconArea.width);
		}
		
		carriedBackground = new ImageView(gui, "ITEM_CARRIED_IMAGE", area);
		dungeonBackground = new ImageView(gui, "ITEM_ON_FLOOR_IMAGE", area);

		
		//if (abilityInfo.isUsableByOwner == false) {
	}

	
	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		
		// Draw background
		if (abilityInfo.isCarried) {
			carriedBackground.setPos(x, y);
			carriedBackground.draw(spriteBatch);
		} else {
			dungeonBackground.setPos(x, y);
			dungeonBackground.draw(spriteBatch);
		} 
		
		super.draw(spriteBatch, x, y);
		
		abilityType.draw(spriteBatch, x, y);
		
		// item effects
		if (abilityEffects != null) {
			for (ImageView effect : abilityEffects) {
				effect.draw(spriteBatch, x, y);
			}
		}
	}
}
