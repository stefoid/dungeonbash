package com.dbash.presenters.tabs;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.AbilityCommand;
import com.dbash.models.AbilityInfo;
import com.dbash.platform.ImageView;
import com.dbash.platform.TextImageView;
import com.dbash.platform.TextView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.dungeon.DungeonAreaPresenter;
import com.dbash.presenters.widgets.IListElement;
import com.dbash.util.Rect;
import com.dbash.util.Rect.HAlignment;
import com.dbash.util.Rect.VAlignment;


public class AbilityListElementView extends AbilityTypeListElement {
	
	// Ability background
	protected ImageView	elementBackground;
	protected ImageView selectedBackground;
	protected ImageView cantUseBackground;
	protected ImageView targetIcon;
	protected ImageView reticuleIcon;
	protected ImageView damageIcon;
	protected TextView damageNumber;
	
	public AbilityListElementView(UIDepend gui, AbilityInfo abilityInfo, Rect area) {
		super(gui, abilityInfo, area);
		
		// TODO here we turn the ability info into a a specific bunch of text and icons at positions relative
		// to the bottom-left of the list element area.  +ve is up, -ve is down.
		elementBackground = new ImageView(gui, "ABILITY_AVAILABLE_IMAGE", area);
		selectedBackground = new ImageView(gui, "ABILITY_SELECTED_IMAGE", area);
		cantUseBackground = new ImageView(gui, "ABILITY_DISABLED_IMAGE", area);
		
		
		Rect damageArea = new Rect(iconArea);
		damageArea.x = area.x + iconArea.width * 2f;
		
		Rect targetArea = new Rect(iconArea);
		targetArea.x = damageArea.x + damageArea.width * 1.5f;
		
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
		
		if (abilityInfo.damageType > 0 && abilityInfo.damageType < 5) {
			int damageAmount = abilityInfo.meleeDamage;
			if (abilityInfo.missileDamage > 0) {
				damageAmount = abilityInfo.missileDamage;
			}
			
			Rect numRect = new Rect(damageArea, HAlignment.CENTER, VAlignment.CENTER, 1.3f*damageArea.width, 0.8f*damageArea.height);
			Color col = Color.RED;
			switch (abilityInfo.damageType) {
				case AbilityCommand.CHEMICAL_ATTACK:
					col = Color.GREEN;
					break;
				case AbilityCommand.ENERGY_ATTACK:
					col = Color.YELLOW;
					break;
				case AbilityCommand.SHARP_ATTACK: 
					col = Color.CYAN;
					break;
				default:
					col = Color.RED;
					break;
			}
			damageNumber = new TextView(gui, null,""+damageAmount, numRect, HAlignment.CENTER, VAlignment.CENTER, col);
			//damageNumber = new TextImageView(gui, gui.numberFont, String.valueOf(damageAmount), damageNumberArea);
			//damageIcon = new ImageView(gui, DungeonAreaPresenter.getDamageName(abilityInfo.damageType), damageArea);
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
		
		if (damageNumber != null) {
			//damageIcon.draw(spriteBatch, x, y);
			damageNumber.draw(spriteBatch, x, y);
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
