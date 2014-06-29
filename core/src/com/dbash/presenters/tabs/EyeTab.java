package com.dbash.presenters.tabs;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.ImageView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.TabPresenter;
import com.dbash.util.Rect;

public class EyeTab extends TabPresenter {

	private EyeDetailListPresenter listPresenter;
	private ImageView eyeTabImage;
	private PresenterDepend model;

	public EyeTab(PresenterDepend model, UIDepend gui, TouchEventProvider touchEventProvider, Rect tabArea, Rect bodyArea) {
		super(model, gui, touchEventProvider, tabArea, bodyArea);
		eyeTabImage = new ImageView(gui, "EYE_TAB_IMAGE", tabArea);
		backImageCurrent = new ImageView(gui, "EYE_TAB_ON_IMAGE", tabArea);
		backImageNotCurrent = new ImageView(gui, "EYE_TAB_OFF_IMAGE", tabArea);
		this.model = model;
		listPresenter = new  EyeDetailListPresenter(model, gui, touchEventProvider, bodyArea);
	}

	@Override
	public void setCurrent() {
		super.setCurrent();
		alertToUsingEye(true);
		listPresenter.activate();
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
	}
	
	private void alertToUsingEye(boolean usingEye) {
		model.presenterTurnState.usingEye(usingEye);
//		dbash.orig.Character character = model.presenterTurnState.getCurrentCharacter();
//		character.setCharacterisUsingEye(usingEye);
	}
}
