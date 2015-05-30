package com.dbash.presenters.tabs;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.AbilityInfo;
import com.dbash.models.Character;
import com.dbash.models.CreatureStats;
import com.dbash.models.IEventAction;
import com.dbash.models.IPresenterTurnState;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEventProvider;
import com.dbash.models.TurnProcessor;
import com.dbash.models.UIInfoListener;
import com.dbash.platform.AnimationView;
import com.dbash.platform.ImageView;
import com.dbash.platform.TextImageView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.root.HighlightAnimView;
import com.dbash.presenters.root.tutorial.TutorialPresenter;
import com.dbash.presenters.widgets.TabPresenter;
import com.dbash.util.EventBus;
import com.dbash.util.Rect;


public class EffectTab extends TabPresenter {

	private EffectListPresenter listPresenter;
	private ImageView abilityTabImage;
	private TextImageView abilityTabText;
	private IPresenterTurnState turnState;
	private UIDepend gui;
	private AnimationView tabButtonAnim;
	private Character currentCharacter;
	 
	public EffectTab(PresenterDepend model, final UIDepend gui, TouchEventProvider touchEventProvider, Rect tabArea, Rect bodyArea) {
		super(model, gui, touchEventProvider, tabArea, bodyArea);
		abilityTabImage = new ImageView(gui, "EFFECT_TAB_IMAGE", tabArea);
		backImageCurrent = new ImageView(gui, "EFFECT_TAB_ON_IMAGE", tabArea);
		backImageNotCurrent = new ImageView(gui, "EFFECT_TAB_OFF_IMAGE", tabArea);
		Rect textArea = new Rect(tabArea, .2f, .2f, .3f, .35f);
		abilityTabText = new TextImageView(gui,gui.numberFont, "", textArea);
		listPresenter = new  EffectListPresenter(model, gui, touchEventProvider, bodyArea);
		turnState = model.presenterTurnState;
		this.gui = gui;
		final Rect animArea = this.tabArea;
		
		newCharacter(turnState.getCurrentCharacter());
		
		// Subscribe to changes to the current character.
		EventBus.getDefault().onEvent(TurnProcessor.CURRENT_CHARACTER_CHANGED, this, new IEventAction() {
			@Override
			public void action(Object param) {
				Character character = (Character) param;
				if (character.isPlayerCharacter()) {
					newCharacter(character);
				}
			}
		});
		
		EventBus.getDefault().onEvent(Character.STAT_LIST_CHANGED, this, new IEventAction() {
			@Override
			public void action(Object param) {
				Character character = (Character) param;
				if (character == currentCharacter) {
					CreatureStats stats = character.getCharacterStats();
					updateImage(stats);
				}
			}
		});
		
		EventBus.getDefault().onEvent(TutorialPresenter.EFFECT_TAB_BUTTON_ON_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				Rect fromRect = new Rect(animArea, .6f);
				Rect toRect = new Rect(animArea, 1.4f);
				if (tabButtonAnim != null) {
					tabButtonAnim.stopPlaying();
				}
				tabButtonAnim = new HighlightAnimView(gui, fromRect, toRect);
				tabButtonAnim.startPlaying();
			}
		});
		
		EventBus.getDefault().onEvent(TutorialPresenter.EFFECT_TAB_BUTTON_OFF_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (tabButtonAnim != null) {
					tabButtonAnim.stopPlaying();
					tabButtonAnim = null;
				}
			}
		});
		
		EventBus.getDefault().onEvent(TutorialPresenter.TUTORIAL_RESTART, this, new IEventAction() {
			@Override
			public void action(Object param) {
				EventBus.getDefault().event(TutorialPresenter.EFFECT_TAB_BUTTON_OFF_EVENT, null);
			}
		});
	}

	// When there is  new character, get that Characters stats
	protected void newCharacter(Character character) {	
		if (character.isPlayerCharacter()) {
			currentCharacter = character;
			CreatureStats stats = currentCharacter.getCharacterStats();
			updateImage(stats);
		}
	}
	
	
	public void updateImage(CreatureStats stats) {
		String imageToUse = AbilityInfo.getImageForEffectType(stats.abilityEffectType);
		String numberToUse;
		
		if (imageToUse == null) {
			imageToUse = "EFFECT_TAB_IMAGE"; 
		} 
		
		if (stats.abilityCountdown > 0) {
			numberToUse = ""+stats.abilityCountdown;
		} else {
			numberToUse = "";
		}
		abilityTabText = new TextImageView(gui, gui.numberFont, numberToUse, new Rect(tabArea, 0.34f));
		abilityTabImage = new ImageView(gui, imageToUse, tabArea);
	}
	
	@Override
	public void setCurrent() {
		super.setCurrent();
		listPresenter.activate();
		EventBus.getDefault().event(TutorialPresenter.EFFECT_TAB_ON_EVENT, null);
	}
	
	@Override
	public void unsetCurrent() {
		super.unsetCurrent();
		listPresenter.deactivate();
	}

	@Override
	public void drawTab(SpriteBatch spriteBatch, float x, float y) {
		// super will draw tab background (different for current/non current)
		super.drawTab(spriteBatch, x, y);
		abilityTabImage.draw(spriteBatch);
		
		// draw the number in two tone offset for better visibility
		abilityTabText.draw(spriteBatch);
		
		if (shouldDrawBody) {
			listPresenter.draw(spriteBatch, x, y);
		}
		
		if (tabButtonAnim != null) {
			tabButtonAnim.draw(spriteBatch);
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		listPresenter.onDestroy();
	}
}