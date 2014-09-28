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
	protected ImageView outlineImage;
	public int index = 0;
	
	public ItemListElementView(UIDepend gui, Character currentCharacter, AbilityInfo abilityInfo, Rect area, int index) {
		super(gui, abilityInfo, area);
		
		this.index = index;

		// set effects and position
		if (abilityInfo.abilityEffects != null) {
			setEffects(leftSide * area.width + iconSpacer * iconArea.width);
		}

		outlineImage = new ImageView(gui, "ELEMENT_BORDER", area);
		setBackgroundImage();
	}
	
	public void setBackgroundImage() {
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
		
		// Draw background and possible border
		outlineImage.draw(spriteBatch, x, y);
		backgroundImage.draw(spriteBatch, x, y);
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
	
	@Override
	public void setAnimating() {
//		Rect area = new Rect(this.area, 0.025f, 0.025f, 0.035f, 0.035f);
//		backgroundImage.setArea(area);
	}
}
