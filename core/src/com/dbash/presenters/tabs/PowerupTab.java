package com.dbash.presenters.tabs;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.Character;
import com.dbash.models.IEventAction;
import com.dbash.models.IPresenterTurnState;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEventProvider;
import com.dbash.models.UIInfoListener;
import com.dbash.platform.AnimationView;
import com.dbash.platform.ImageView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.root.HighlightAnimView;
import com.dbash.presenters.root.tutorial.TutorialPresenter;
import com.dbash.presenters.widgets.TabPresenter;
import com.dbash.util.EventBus;
import com.dbash.util.Rect;

public class PowerupTab extends TabPresenter {

	private ItemListPresenter listPresenter;
	private ImageView itemTabImage;
	private IPresenterTurnState turnState;
	private UIDepend gui;
	private AnimationView tabButtonAnim;
	
	public PowerupTab(PresenterDepend model, final UIDepend gui, TouchEventProvider touchEventProvider, Rect tabArea, Rect bodyArea) {
		super(model, gui, touchEventProvider, tabArea, bodyArea);
		itemTabImage = new ImageView(gui, "POWERUP_TAB_IMAGE", tabArea);
		backImageCurrent = new ImageView(gui, "POWERUP_TAB_ON_IMAGE", tabArea);
		backImageNotCurrent = new ImageView(gui, "POWERUP_TAB_OFF_IMAGE", tabArea);
		
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
				tabButtonAnim = new HighlightAnimView(gui, fromRect, toRect);
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
		
		EventBus.getDefault().onEvent(TutorialPresenter.TUTORIAL_RESTART, this, new IEventAction() {
			@Override
			public void action(Object param) {
				EventBus.getDefault().event(TutorialPresenter.ITEM_TAB_BUTTON_OFF_EVENT, null);
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
					//updateCapacity(currentCharacter);
				}
			}));
			//updateCapacity(currentCharacter);
		}
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
			listPresenter.draw(spriteBatch, x , y);
		}
		
		if (tabButtonAnim != null) {
			tabButtonAnim.draw(spriteBatch);
		}
	}
}
