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
import com.dbash.presenters.root.GameStatePresenter;
import com.dbash.presenters.root.HighlightAnimView;
import com.dbash.presenters.root.PowerupOverlayPresenter;
import com.dbash.presenters.root.tutorial.TutorialPresenter;
import com.dbash.presenters.widgets.TabPresenter;
import com.dbash.util.EventBus;
import com.dbash.util.Rect;

public class PowerupTab extends TabPresenter {

	private PowerupListPresenter listPresenter;
	private ImageView itemTabImage;
	private IPresenterTurnState turnState;
	private UIDepend gui;
	private AnimationView tabButtonAnim;
	
	public PowerupTab(PresenterDepend model, final UIDepend gui, TouchEventProvider touchEventProvider, Rect tabArea, Rect bodyArea) {
		super(model, gui, touchEventProvider, tabArea, bodyArea);
		itemTabImage = new ImageView(gui, "POWERUP_TAB_IMAGE", tabArea);
		backImageCurrent = new ImageView(gui, "POWERUP_TAB_ON_IMAGE", tabArea);
		backImageNotCurrent = new ImageView(gui, "POWERUP_TAB_OFF_IMAGE", tabArea);
		
		listPresenter = new  PowerupListPresenter(model, gui, touchEventProvider, bodyArea);
		turnState = model.presenterTurnState;
		this.gui = gui;
		final Rect animArea = this.tabArea;
		
		EventBus.getDefault().onEvent(GameStatePresenter.POWERUP_TAB_BUTTON_ON_EVENT, this, new IEventAction() {
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
		
		EventBus.getDefault().onEvent(GameStatePresenter.POWERUP_TAB_BUTTON_OFF_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (tabButtonAnim != null) {
					tabButtonAnim.stopPlaying();
					tabButtonAnim = null;
				}
			}
		});
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
		itemTabImage.draw(spriteBatch);
		
		if (shouldDrawBody) {
			listPresenter.draw(spriteBatch, x , y);
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
