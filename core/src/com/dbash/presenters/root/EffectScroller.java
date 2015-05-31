package com.dbash.presenters.root;

import java.util.HashMap;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.Character;
import com.dbash.models.IEventAction;
import com.dbash.models.TouchEventProvider;
import com.dbash.models.TurnProcessor;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.dungeon.EffectScrollPresenter;
import com.dbash.presenters.widgets.AnimQueue;
import com.dbash.util.EventBus;
import com.dbash.util.L;
import com.dbash.util.Rect;

public class EffectScroller {

	private HashMap<Character, EffectScrollPresenter> effectPresenters;
	private AnimQueue effectAnimQueue;
	Rect dungeonArea;
	UIDepend gui;
	
	public EffectScroller(UIDepend gui, Rect dungeonArea) {
		this.gui = gui;
		this.dungeonArea = dungeonArea;
		this.effectAnimQueue = new AnimQueue();
		effectAnimQueue.setMaxAnims(12);
		this.effectPresenters = new HashMap<Character, EffectScrollPresenter>();
		
		EventBus.getDefault().onEvent(TurnProcessor.CURRENT_CHARACTER_CHANGED, this, new IEventAction() {
			@Override
			public void action(Object param) {
				Character character = (Character) param;
				if (character.isPlayerCharacter()) {
					newCharacter(character);
				}
			}
		});
	}
	
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		effectAnimQueue.draw(spriteBatch);  // draws character stat effects
	}
	
	public void newCharacter(Character character) {

		EffectScrollPresenter effectPresenter = effectPresenters.get(character);
		if (effectPresenter == null) {
			effectPresenter = new EffectScrollPresenter(gui, character, dungeonArea, effectAnimQueue);
			effectPresenters.put(character, effectPresenter);
		}

		// make the current Effect presenter... current.
		for (EffectScrollPresenter ep : effectPresenters.values()) {
			ep.clearCurrent();
		}
		effectPresenter.setCurrent();
	}

}


