package com.dbash.presenters.tabs;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.AbilityInfo;
import com.dbash.platform.ImageView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.IListElement;
import com.dbash.util.Rect;


public class EffectListElementView extends AbilityTypeListElement {
	
	// Effect background
	protected ImageView	elementBackground;
	
	public EffectListElementView(UIDepend gui, AbilityInfo abilityInfo, Rect area) {
		super(gui, abilityInfo, area);
		
		// set effects and position
		if (abilityInfo.abilityEffects != null) {
			setEffects(leftSide * area.width);
		}
		
		elementBackground = new ImageView(gui, "EFFECT_IMAGE", area);
			
	}
	
	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		
		// Draw background
		elementBackground.setPos(x, y);
		elementBackground.draw(spriteBatch);
		
		super.draw(spriteBatch, x, y);
		
		// effects
		if (abilityInfo.abilityEffects != null) {
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
