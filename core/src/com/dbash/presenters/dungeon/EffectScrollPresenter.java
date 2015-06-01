package com.dbash.presenters.dungeon;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.dbash.models.AbilityInfo;
import com.dbash.models.Character;
import com.dbash.models.DungeonPosition;
import com.dbash.models.EffectList;
import com.dbash.models.IEventAction;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TurnProcessor;
import com.dbash.models.UIInfoListener;
import com.dbash.platform.AnimationView;
import com.dbash.platform.TextImageView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.AnimOp;
import com.dbash.presenters.widgets.AnimQueue;
import com.dbash.util.EventBus;
import com.dbash.util.L;
import com.dbash.util.Rect;

@SuppressWarnings("unused")

/**
 * This registers for changes to the effect list of the creature it displays, and displays those changes with an animation.
 * display is done through throwing animation on the anim queue so no draw function required.
 */
public class EffectScrollPresenter {
	public static final boolean LOG = true && L.DEBUG;
	
	
	private UIDepend gui;
	private Character character;
	private AnimQueue animQueue;
	private boolean isCurrent;
	private EffectList effectList;
	private Rect area;
	
	public EffectScrollPresenter(UIDepend gui, Character theCharacter, Rect area, AnimQueue animQueue) {
		this.gui = gui;
		this.character = theCharacter;
		this.animQueue = animQueue;
		this.isCurrent = false;
		this.area = area;
		
		EventBus.getDefault().onEvent(Character.EFFECT_LIST_CHANGED, this, new IEventAction() {
			@Override
			public void action(Object param) {
				Character c = (Character) param;
				if (character == c) {
					if (LOG) L.log("character :%s", character);
					if (isCurrent) {
						processEffectList();
					} else {
						effectList = character.getEffectList();
					}
				}
			}
		});
		
		EventBus.getDefault().onEvent(Character.EFFECT_LIST_RESET, this, new IEventAction() {
			@Override
			public void action(Object param) {
				Character c = (Character) param;
				if (character == c) {
					effectList = character.getEffectList();
				}
			}
		});
	}
	
	protected void processEffectList() {
		if (LOG) L.log("character :%s", character);
		EffectList oldList = effectList;
		effectList = character.getEffectList();
		
		// process the difference between the two effects lists and display each an animation.
		for (AbilityInfo abilityInfo : effectList) {
			int dif = oldList.difference(abilityInfo);
			if (dif != 0) {
				if (LOG) L.log("mod %s %s", abilityInfo.statText, abilityInfo.statValue);
				createAnim(abilityInfo, dif);
			}
		}
	}
	
	private void createAnim(AbilityInfo abilityInfo, int dif) {
		
		Rect fromRect = new Rect(area, 0.07f);
		float height = fromRect.height;
		fromRect = new Rect(fromRect, 5);
		float yOffset = fromRect.height/1.4f;
		Rect toRect = new Rect(fromRect);
		fromRect.height = height;
		fromRect.y += yOffset;
		toRect.y = toRect.y + toRect.height - height + yOffset;
		toRect.height = height;
		
		if (LOG) L.log("fromRect: %s, toRect: %s", fromRect, toRect);
		
		BitmapFont font = gui.defaultFonts.get(gui.defaultFonts.size() -1);
		TextImageView message = new TextImageView(gui, font, abilityInfo.name, fromRect);
		AnimationView messageAnim = new AnimationView(gui, message, fromRect, toRect, .3f, 1f, DungeonAreaPresenter.effectMsgPeriod, 1, null);
		messageAnim.animType = AnimOp.AnimType.EFFECT_MSG;
		messageAnim.sequenceNumber = 0;
		messageAnim.creator = this;

		// damage due to bursts is a special case where the damage should play near the end of the burst, but not after it
		animQueue.chainConcurrentWithLast(messageAnim, 40f, false);
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
