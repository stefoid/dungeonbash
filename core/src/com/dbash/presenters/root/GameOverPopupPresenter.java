package com.dbash.presenters.root;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.GameStats;
import com.dbash.models.TouchEvent;
import com.dbash.platform.TextView;
import com.dbash.util.Rect;
import com.dbash.util.Rect.HAlignment;
import com.dbash.util.Rect.VAlignment;


public class GameOverPopupPresenter extends PopupPresenter {

	final static String POPUP_ID = "GameOver";
	
	TextView gameOverText;
	TextView levelText;
	TextView xpText;
	TextView monText;
	GameStats gameStats;
	
	public GameOverPopupPresenter(GameStats gameStats) {
		super();
		this.gameStats = gameStats;
		this.controller = popupController;
		this.popupId = POPUP_ID;
		this.touchEventProvider = popupController.getTouchEventProvider();
		this.gui = popupController.getGuiDependencies();
		this.area = new Rect(gui.sizeCalculator.dungeonArea);
		
		init();
		popupController.popupCreated(this, popupId);  // tell the popup controller about me.
	}

	public static void clear() {
		popupController.popupDismissed(POPUP_ID, true);
	}
	
	@Override
	public void init() {
		// Needs to swallow all touches to the screen to be modal
		touchEventProvider.addTouchEventListener(this, null, gui.cameraViewPort.viewPort);  //null area means entire screen
		
		Rect gameOverRect = new Rect(area, .5f);
		gameOverRect.y += area.height/4;
		gameOverText = new TextView(gui, null, "Game Over", gameOverRect, HAlignment.CENTER, VAlignment.CENTER, Color.WHITE);
		
		Rect levelRect = new Rect(area);
		levelRect.height /= 20;
		levelRect.y = gameOverRect.y + levelRect.height*2;
		levelText = new TextView(gui, null, "You reached level "+gameStats.level, levelRect, HAlignment.CENTER, VAlignment.CENTER, Color.RED);
		
		Rect monRect = new Rect(levelRect);
		monRect.y -= (monRect.height * 1.5f);
		monText = new TextView(gui, null, "You killed "+gameStats.monstersKilled+ " monsters", monRect, HAlignment.CENTER, VAlignment.CENTER, new Color(0f, 0f, 1f, 1));
		
		Rect xpRect = new Rect(monRect);
		xpRect.y -= (xpRect.height * 1.5f);
		xpText = new TextView(gui, null, "You got "+gameStats.xp+ " XP", xpRect, HAlignment.CENTER, VAlignment.CENTER, new Color(0, .8f, 0, 1));
	}
	
	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		gameOverText.draw(spriteBatch, x, y);
		levelText.draw(spriteBatch, x, y);
		monText.draw(spriteBatch, x, y);
		xpText.draw(spriteBatch, x, y);
	}
	
	@Override
	public boolean touchEvent(TouchEvent event) {
		touchEventProvider.removeTouchEventListener(this);
		controller.popupDismissed(popupId, true);
		return true;
	}
}
