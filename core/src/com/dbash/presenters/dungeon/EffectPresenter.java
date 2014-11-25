package com.dbash.presenters.dungeon;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
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
	private AnimQueue animQueue;
	private boolean isCurrent;
	private EffectList effectList;
	private MapPresenter mapPresenter;
	
	public EffectPresenter(UIDepend gui, PresenterDepend model, Character theCharacter, MapPresenter mapPresenter, AnimQueue animQueue) {
		this.gui = gui;
		this.character = theCharacter;
		this.model = model;
		this.mapPresenter = mapPresenter;
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
		
		Rect fromRect = getTileArea(character.getPosition());
		float height = fromRect.height;
		fromRect = new Rect(fromRect, 5);
		float yOffset = fromRect.height/2;
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
	
	private Rect getTileArea(DungeonPosition position) {
		LocationPresenter locPres = mapPresenter.locationPresenter(position);
		return locPres.getScreenArea();
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
