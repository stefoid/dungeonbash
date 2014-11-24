package com.dbash.presenters.dungeon;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.AbilityInfo;
import com.dbash.models.Character;
import com.dbash.models.Creature;
import com.dbash.models.Data;
import com.dbash.models.UIInfoListener;
import com.dbash.models.Dungeon.MoveType;
import com.dbash.models.DungeonPosition;
import com.dbash.models.EffectList;
import com.dbash.models.IAnimListener;
import com.dbash.models.IDungeonPresentationEventListener.DeathType;
import com.dbash.models.IPresenterCreature;
import com.dbash.models.IPresenterCreature.HighlightStatus;
import com.dbash.models.Light;
import com.dbash.models.Monster;
import com.dbash.models.PresenterDepend;
import com.dbash.platform.AnimationView;
import com.dbash.platform.Audio;
import com.dbash.platform.ImageView;
import com.dbash.platform.TextImageView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.AnimOp;
import com.dbash.presenters.widgets.AnimQueue;
import com.dbash.util.L;
import com.dbash.util.Rect;

@SuppressWarnings("unused")

/**
 * This registers for changes to the effect list of the creature it displays, and displays those changes with an animation.
 * display is done through throwing animation on the anim queue so no draw function required.
 */
public class EffectPresenter {
	public static final boolean LOG = true && L.DEBUG;
	
	private UIDepend gui;
	private PresenterDepend model;
	private Character character;
	private Rect area;
	private AnimQueue animQueue;
	private boolean isCurrent;
	private EffectList effectList;

	
	public EffectPresenter(UIDepend gui, PresenterDepend model, Character theCharacter, Rect area, AnimQueue animQueue) {
		this.gui = gui;
		this.character = theCharacter;
		this.model = model;
		this.area = area;
		this.animQueue = animQueue;
		this.isCurrent = false;
		
		character.onChangeToEffectList(new UIInfoListener() {
			@Override
			public void UIInfoChanged() {
				if (LOG) L.log("character :%s", character);
				if (isCurrent) {
					processEffectList();
				} else {
					effectList = character.getEffectList();
				}
			}
		});
	}
	
	protected void processEffectList() {
		if (LOG) L.log("character :%s", character);
		ArrayList<AbilityInfo> addList = new ArrayList<AbilityInfo>();
		ArrayList<AbilityInfo> subList = new ArrayList<AbilityInfo>();
		EffectList oldList = effectList;
		effectList = character.getEffectList();
		
		// process the difference between the two effects lists and display each an animation.
		for (AbilityInfo abilityInfo : oldList) {
			if (effectList.isInList(abilityInfo) == false) {
				subList.add(abilityInfo);
				if (LOG) L.log("subbed %s", abilityInfo);
			}
		}
		
		for (AbilityInfo abilityInfo : effectList) {
			if (oldList.isInList(abilityInfo) == false) {
				addList.add(abilityInfo);
				if (LOG) L.log("added %s", abilityInfo);
			}
		}
		
		
	}
	
	public void setCurrent() {
		if (LOG) L.log("char: %s", character);
		this.isCurrent = true;
		this.effectList = character.getEffectList();
	}
	
	public void clearCurrent() {
		if (LOG) L.log("char: %s", character);
		this.isCurrent = false;
	}
}
