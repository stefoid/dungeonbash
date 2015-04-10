package com.dbash.presenters.tabs;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.AbilityInfo;
import com.dbash.models.Character;
import com.dbash.models.CreatureStats;
import com.dbash.models.IEventAction;
import com.dbash.models.IPresenterTurnState;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEventProvider;
import com.dbash.models.UIInfoListener;
import com.dbash.platform.AnimationView;
import com.dbash.platform.ImageView;
import com.dbash.platform.TextImageView;
import com.dbash.platform.TextView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.root.tutorial.TutorialPresenter;
import com.dbash.presenters.widgets.TabPresenter;
import com.dbash.util.EventBus;
import com.dbash.util.Rect;
import com.dbash.util.Rect.HAlignment;
import com.dbash.util.Rect.VAlignment;

	public class ItemTab extends TabPresenter {

		private ItemListPresenter listPresenter;
		private ImageView itemTabImage;
		private TextView capacityText;
		private IPresenterTurnState turnState;
		private UIDepend gui;
		private AnimationView tabButtonAnim;
		
		public ItemTab(PresenterDepend model, final UIDepend gui, TouchEventProvider touchEventProvider, Rect tabArea, Rect bodyArea) {
			super(model, gui, touchEventProvider, tabArea, bodyArea);
			itemTabImage = new ImageView(gui, "INVENTORY_TAB_IMAGE", tabArea);
			backImageCurrent = new ImageView(gui, "INVENTORY_TAB_ON_IMAGE", tabArea);
			backImageNotCurrent = new ImageView(gui, "INVENTORY_TAB_OFF_IMAGE", tabArea);
			
			Rect textArea = new Rect(tabArea, .2f, .2f, .3f, .35f);
			capacityText = new TextView(gui, "", textArea, Color.WHITE);
			listPresenter = new  ItemListPresenter(model, gui, touchEventProvider, bodyArea);
			turnState = model.presenterTurnState;
			this.gui = gui;
			final Rect animArea = this.tabArea;
			
			newCharacter(turnState.getCurrentCharacter());
			
			// Subscribe to changes to the current character.
			turnState.onChangeToCurrentCharacter(new UIInfoListener() {
				public void UIInfoChanged() {
					Character character = turnState.getCurrentCharacter();
					newCharacter(character);
				}
			});
			
			EventBus.getDefault().onEvent(TutorialPresenter.ITEM_TAB_BUTTON_ON_EVENT, this, new IEventAction() {
				@Override
				public void action(Object param) {
					Rect fromRect = new Rect(animArea, .6f);
					Rect toRect = new Rect(animArea, 1.4f);
					if (tabButtonAnim != null) {
						tabButtonAnim.stopPlaying();
					}
					tabButtonAnim = new AnimationView(gui, "missed", fromRect, toRect, 0.6f, 0f, 1f, 0, null);
					tabButtonAnim.startPlaying();
				}
			});
			
			EventBus.getDefault().onEvent(TutorialPresenter.ITEM_TAB_BUTTON_OFF_EVENT, this, new IEventAction() {
				@Override
				public void action(Object param) {
					if (tabButtonAnim != null) {
						tabButtonAnim.stopPlaying();
						tabButtonAnim = null;
					}
				}
			});
		}

		// When there is  new character, get that Characters stats
		protected void newCharacter(Character character)
		{	
			if (character.isPlayerCharacter()) {
				final Character currentCharacter = character;
				
				// the effect tab is could be updated when the character stats change
				character.onChangeToInventory((new UIInfoListener() {
					public void UIInfoChanged() {
						updateCapacity(currentCharacter);
					}
				}));
				updateCapacity(currentCharacter);
			}
		}
		
		private void updateCapacity(Character currentCharacter) {
			String capacityString = "FULL";
			if (currentCharacter.getCapacity() == 0) {
				capacityString = "N/A";
			}
			int held = currentCharacter.getNumberOfPhysicalItemsCarried();
			int free = currentCharacter.getCapacity() - held ;
			if (free > 0) {
				capacityString = ""+held+"/"+currentCharacter.getCapacity();
			}
			capacityText = new TextView(gui, null, capacityString, new Rect(tabArea, .05f, .05f, .4f, .37f), HAlignment.CENTER, VAlignment.CENTER, Color.BLACK);
		}
		
		@Override
		public void setCurrent() {
			super.setCurrent();
			listPresenter.activate();
			EventBus.getDefault().event(TutorialPresenter.ITEM_TAB_ON_EVENT, null);
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
			itemTabImage.draw(spriteBatch);
			
			if (shouldDrawBody) {
				capacityText.setColor(Color.BLACK);
				capacityText.draw(spriteBatch, x, y);
				capacityText.setColor(Color.WHITE);
				capacityText.draw(spriteBatch, x-2, y+2);
				listPresenter.draw(spriteBatch, x , y);
			}
			
			if (tabButtonAnim != null) {
				tabButtonAnim.draw(spriteBatch);
			}
		}
	}
