package com.dbash.presenters.tabs;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.AbilityInfo;
import com.dbash.models.Character;
import com.dbash.models.CreatureStats;
import com.dbash.models.IPresenterTurnState;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEventProvider;
import com.dbash.models.UIInfoListener;
import com.dbash.platform.ImageView;
import com.dbash.platform.TextImageView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.TabPresenter;
import com.dbash.util.Rect;


	public class EffectTab extends TabPresenter {

		private EffectListPresenter listPresenter;
		private ImageView abilityTabImage;
		private TextImageView abilityTabText;
		private IPresenterTurnState turnState;
		private UIDepend gui;
		 
		public EffectTab(PresenterDepend model, UIDepend gui, TouchEventProvider touchEventProvider, Rect tabArea, Rect bodyArea) {
			super(model, gui, touchEventProvider, tabArea, bodyArea);
			abilityTabImage = new ImageView(gui, "EFFECT_TAB_IMAGE", tabArea);
			backImageCurrent = new ImageView(gui, "EFFECT_TAB_ON_IMAGE", tabArea);
			backImageNotCurrent = new ImageView(gui, "EFFECT_TAB_OFF_IMAGE", tabArea);
			Rect textArea = new Rect(tabArea, .2f, .2f, .3f, .35f);
			abilityTabText = new TextImageView(gui,gui.numberFont, "", textArea);
			listPresenter = new  EffectListPresenter(model, gui, touchEventProvider, bodyArea);
			turnState = model.presenterTurnState;
			this.gui = gui;
			
			newCharacter(turnState.getCurrentCharacter());
			
			// Subscribe to changes to the current character.
			turnState.onChangeToCurrentCharacter(new UIInfoListener() {
				public void UIInfoChanged() {
					Character character = turnState.getCurrentCharacter();
					newCharacter(character);
				}
			});
		}

		// When there is  new character, get that Characters stats
		protected void newCharacter(Character character)
		{	
			if (character.isPlayerCharacter()) {
				final Character currentCharacter = character;
				
				// the effect tab is could be updated when the character stats change
				character.onChangeToCharacterStats((new UIInfoListener() {
					public void UIInfoChanged() {
						CreatureStats stats = currentCharacter.getCharacterStats();
						updateImage(stats);
					}
				}));
				
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
		}
	}