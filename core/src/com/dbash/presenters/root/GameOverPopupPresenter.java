package com.dbash.presenters.root;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.TouchEvent;
import com.dbash.platform.TextView;
import com.dbash.util.Rect;
import com.dbash.util.Rect.HAlignment;
import com.dbash.util.Rect.VAlignment;


public class GameOverPopupPresenter extends PopupPresenter {

	TextView gameOverText;
	
	public GameOverPopupPresenter() {
		super();
		this.controller = popupController;
		this.popupId = "GameOver";
		this.touchEventProvider = popupController.getTouchEventProvider();
		this.gui = popupController.getGuiDependencies();
		this.area = new Rect(gui.sizeCalculator.dungeonArea);
		
		init();
		popupController.popupCreated(this, popupId);  // tell the popup controller about me.
	}

	@Override
	public void init() {
		// Needs to swallow all touches to the screen to be modal
		touchEventProvider.addTouchEventListener(this, null, gui.cameraViewPort.viewPort);  //null area means entire screen
		
		Rect gameOverRect = new Rect(area, .5f);
		gameOverRect.y += area.height/3;
		gameOverText = new TextView(gui, null, "Game Over", gameOverRect, HAlignment.CENTER, VAlignment.CENTER, Color.WHITE);
	}
	
	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		gameOverText.draw(spriteBatch, x, y);
	}
	
	@Override
	public boolean touchEvent(TouchEvent event) {
		touchEventProvider.removeTouchEventListener(this);
		controller.popupDismissed(popupId, true);
		return true;
	}
}
