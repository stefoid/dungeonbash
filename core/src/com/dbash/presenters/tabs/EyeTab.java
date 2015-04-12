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

public class EyeTab extends TabPresenter {

	private EyeDetailListPresenter listPresenter;
	private ImageView eyeTabImage;
	private PresenterDepend model;
	private AnimationView tabButtonAnim;

	public EyeTab(PresenterDepend model, final UIDepend gui, TouchEventProvider touchEventProvider, Rect tabArea, Rect bodyArea) {
		super(model, gui, touchEventProvider, tabArea, bodyArea);
		eyeTabImage = new ImageView(gui, "EYE_TAB_IMAGE", tabArea);
		backImageCurrent = new ImageView(gui, "EYE_TAB_ON_IMAGE", tabArea);
		backImageNotCurrent = new ImageView(gui, "EYE_TAB_OFF_IMAGE", tabArea);
		this.model = model;
		listPresenter = new  EyeDetailListPresenter(model, gui, touchEventProvider, bodyArea);
		final Rect animArea = this.tabArea;
		
		EventBus.getDefault().onEvent(TutorialPresenter.EYE_TAB_BUTTON_ON_EVENT, this, new IEventAction() {
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
		
		EventBus.getDefault().onEvent(TutorialPresenter.EYE_TAB_BUTTON_OFF_EVENT, this, new IEventAction() {
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
		alertToUsingEye(true);
		listPresenter.activate();
		EventBus.getDefault().event(TutorialPresenter.EYE_TAB_ON_EVENT, null);
	}
	
	@Override
	public void unsetCurrent() {
		super.unsetCurrent();
		alertToUsingEye(false);
		listPresenter.deactivate();
	}

	@Override
	public void drawTab(SpriteBatch spriteBatch, float x, float y) {
		// super will draw tab background (different for current/non current)
		super.drawTab(spriteBatch, x, y);
		eyeTabImage.draw(spriteBatch);
		
		if (shouldDrawBody) {
			listPresenter.draw(spriteBatch, x, y);
		}
		
		if (tabButtonAnim != null) {
			tabButtonAnim.draw(spriteBatch);
		}
	}
	
	private void alertToUsingEye(boolean usingEye) {
		model.presenterTurnState.usingEye(usingEye);
	}
}
