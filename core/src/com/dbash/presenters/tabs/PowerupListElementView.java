package com.dbash.presenters.tabs;

import java.util.ArrayList;

import com.dbash.models.AbilityCommand;
import com.dbash.models.Character;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.AbilityInfo;
import com.dbash.platform.ImageView;
import com.dbash.platform.TextView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.IListElement;
import com.dbash.util.Rect;
import com.dbash.util.Rect.HAlignment;
import com.dbash.util.Rect.VAlignment;


public class PowerupListElementView extends AbilityTypeListElement {
	
	// Ability background
	protected ImageView	backgroundImage;
	protected ImageView selectedBackground;
	protected ImageView cantUseBackground;
	protected ImageView targetIcon;
	protected ImageView reticuleIcon;
	protected ImageView damageIcon;
	protected TextView damageNumber;
	protected TextView	xpCostText;
	public int index = 0;
	
	public PowerupListElementView(UIDepend gui, Character character, AbilityInfo abilityInfo, Rect area, int index) {
		super(gui, abilityInfo, area);
		
		// set effects and position
		if (abilityInfo.abilityEffects != null) {
			setEffects(leftSide * area.width);
		}
		
		this.index = index;
		Rect damageArea = getIconArea(0);
		Rect targetArea = getIconArea(1);
		
		if (abilityInfo.targetable) {
			targetIcon = new ImageView(gui, "TARGET_IMAGE", targetArea);
		} else {
			targetIcon = null;
		}
		
		Rect reticuleArea = getIconArea(2);
		
		if (abilityInfo.aimed && abilityInfo.targetable) {
			reticuleIcon = new ImageView(gui, "RETICULE_IMAGE", reticuleArea);
		} else if (abilityInfo.burstEffect) {
			reticuleIcon = new ImageView(gui, "BURST_ICON", reticuleArea);
		} else {
			reticuleIcon = null;
		}
		
		if (abilityInfo.damageType > 0 && abilityInfo.damageType < 5) {
			int damageAmount = abilityInfo.meleeDamage;
			if (abilityInfo.missileDamage > 0) {
				damageAmount = abilityInfo.missileDamage;
			}
			
			Rect numRect = new Rect(damageArea, HAlignment.CENTER, VAlignment.TOP, 1.3f*damageArea.width, 0.67f*damageArea.height);
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
		}
		
		float y = abilityName.area.y;
		float h = abilityName.area.height;
		Rect numRect1 = new Rect(area, .5f, .05f, 0f, 0f);
		numRect1.y = y;
		numRect1.height = h;
		
		xpCostText = new TextView(gui, null,""+abilityInfo.xpCost+" XP", numRect1, HAlignment.RIGHT, VAlignment.CENTER, Color.WHITE);
		//outlineImage = new ImageView(gui, "ELEMENT_BORDER", area);
		setBackgroundImage();
	}
	
	public void setBackgroundImage() {
		String dbImage = "ABILITY_DISABLED_IMAGE";

		if (abilityInfo.isAvailable) {
			if (abilityInfo.isAffordable == false) {
				dbImage = "ABILITY_DISABLED_IMAGE";
			} else {
				dbImage = "POWERUP_AVAILABLE_IMAGE";
			}
		} else {
			dbImage = "POWERUP_BOUGHT_IMAGE";
		} 
		
		backgroundImage = new ImageView(gui, dbImage, area);
	}
	
	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		
		backgroundImage.draw(spriteBatch, x, y);
		super.draw(spriteBatch, x, y);
		xpCostText.draw(spriteBatch, x, y);
		
		// item effects
		if (abilityEffects != null && abilityEffects.size() > 0) {
			for (ImageView effect : abilityEffects) {
				effect.draw(spriteBatch, x, y);
			}
		} else {
			abilityType.draw(spriteBatch, x, y);
		}
		
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

/*
public class owerupListElementView extends AbilityTypeListElement {
	
	// Item background
	protected ImageView backgroundImage;
	protected ImageView outlineImage;
	public int index = 0;
	
	protected TextView	xpCostText;
	
	public PowerupListElementView(UIDepend gui, Character currentCharacter, AbilityInfo abilityInfo, Rect area, int index) {
		super(gui, abilityInfo, area);
		
		this.index = index;
		Rect r = getIconArea(0);
		// set effects and position
		if (abilityInfo.abilityEffects != null) {
			setEffects(leftSide * area.width);
		}

		float y = abilityName.area.y;
		float h = abilityName.area.height;
		Rect numRect = new Rect(area, .5f, .05f, 0f, 0f);
		numRect.y = y;
		numRect.height = h;
		
		xpCostText = new TextView(gui, null,""+abilityInfo.xpCost+" XP", numRect, HAlignment.RIGHT, VAlignment.CENTER, Color.WHITE);
		outlineImage = new ImageView(gui, "ELEMENT_BORDER", area);
		setBackgroundImage();
	}
	
	public void setBackgroundImage() {
		String dbImage = "ABILITY_DISABLED_IMAGE";

		if (abilityInfo.isAvailable) {
			if (abilityInfo.isAffordable == false) {
				dbImage = "ABILITY_DISABLED_IMAGE";
			} else {
				dbImage = "POWERUP_AVAILABLE_IMAGE";
			}
		} else {
			dbImage = "POWERUP_BOUGHT_IMAGE";
		} 
		
		backgroundImage = new ImageView(gui, dbImage, area);
	}
	
	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		
		// Draw background and possible border
		outlineImage.draw(spriteBatch, x, y);
		backgroundImage.draw(spriteBatch, x, y);
		super.draw(spriteBatch, x, y);
		xpCostText.draw(spriteBatch, x, y);
		
		// item effects
		if (abilityEffects != null && abilityEffects.size() > 0) {
			for (ImageView effect : abilityEffects) {
				effect.draw(spriteBatch, x, y);
			}
		} else {
			abilityType.draw(spriteBatch, x, y);
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
*/
