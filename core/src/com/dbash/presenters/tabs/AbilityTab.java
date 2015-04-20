package com.dbash.presenters.tabs;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.IEventAction;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.AnimationView;
import com.dbash.platform.ImageView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.root.HighlightAnimView;
import com.dbash.presenters.root.tutorial.TutorialPresenter;
import com.dbash.presenters.widgets.TabPresenter;
import com.dbash.util.EventBus;
import com.dbash.util.Rect;

	public class AbilityTab extends TabPresenter {

		private AbilitySelectionListPresenter listPresenter;
		private ImageView abilityTabImage;
		private AnimationView tabButtonAnim;
		private UIDepend gui;

		public AbilityTab(PresenterDepend model, final UIDepend gui, TouchEventProvider touchEventProvider, Rect tabArea, Rect bodyArea) {
			super(model, gui, touchEventProvider, tabArea, bodyArea);
			abilityTabImage = new ImageView(gui, "ABILITY_TAB_IMAGE", tabArea);
			backImageCurrent = new ImageView(gui, "ABILITY_TAB_ON_IMAGE", tabArea);
			backImageNotCurrent = new ImageView(gui, "ABILITY_TAB_OFF_IMAGE", tabArea);
			listPresenter = new  AbilitySelectionListPresenter(model, gui, touchEventProvider, bodyArea);
			this.gui = gui;
			final Rect animArea = this.tabArea;
			
			EventBus.getDefault().onEvent(TutorialPresenter.ABILITY_TAB_BUTTON_ON_EVENT, this, new IEventAction() {
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
			
			EventBus.getDefault().onEvent(TutorialPresenter.ABILITY_TAB_BUTTON_OFF_EVENT, this, new IEventAction() {
				@Override
				public void action(Object param) {
					if (tabButtonAnim != null) {
						tabButtonAnim.stopPlaying();
						tabButtonAnim = null;
					}
				}
			});
			
			EventBus.getDefault().onEvent(TutorialPresenter.ALL_BUTTON_ANIMS_OFF, this, new IEventAction() {
				@Override
				public void action(Object param) {
					EventBus.getDefault().event(TutorialPresenter.ABILITY_TAB_BUTTON_OFF_EVENT, null);
				}
			});
		}

		@Override
		public void setCurrent() {
			super.setCurrent();
			listPresenter.activate();
			EventBus.getDefault().event(TutorialPresenter.ABILITY_TAB_ON_EVENT, null);
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
			
			if (shouldDrawBody) {
				listPresenter.draw(spriteBatch, x, y);
			}
			
			if (tabButtonAnim != null) {
				tabButtonAnim.draw(spriteBatch);
			}
		}
	}