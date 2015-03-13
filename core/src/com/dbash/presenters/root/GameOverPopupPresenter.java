package com.dbash.presenters.root;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Timer;
import com.dbash.models.GameStats;
import com.dbash.models.OverlayPresenter;
import com.dbash.models.TouchEvent;
import com.dbash.models.TouchEventListener;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.ImageView;
import com.dbash.platform.TextView;
import com.dbash.platform.UIDepend;
import com.dbash.util.Rect;
import com.dbash.util.Rect.HAlignment;
import com.dbash.util.Rect.VAlignment;


public class GameOverPopupPresenter extends OverlayPresenter implements TouchEventListener {

	final static String POPUP_ID = "GameOver";
	
	ImageView backgroundImage;
	TextView gameOverText;
	TextView levelText;
	TextView xpText;
	TextView monText;
	GameStats gameStats;
	boolean startDrawing;
	
	public GameOverPopupPresenter(GameStats gameStats) {
		this.startDrawing = false;
		this.gameStats = gameStats;
	}
	
	@Override
	public void init(UIDepend gui) {
		this.gui = gui;
	}
	
	@Override
	public void start(Rect area, TouchEventProvider touchEventProvider) {
		this.touchEventProvider = touchEventProvider;
		this.area = new Rect(gui.sizeCalculator.dungeonArea);
		Rect backgroundArea = new Rect(area, .04f, .04f, .15f, .35f);
		this.backgroundImage = new ImageView(gui, "GAME_OVER_BACKGROUND", backgroundArea);
		
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
		monText = new TextView(gui, null, "You killed "+gameStats.monstersKilled+ " monsters", monRect, HAlignment.CENTER, VAlignment.CENTER, Color.CYAN);
		
		Rect xpRect = new Rect(monRect);
		xpRect.y -= (xpRect.height * 1.5f);
		xpText = new TextView(gui, null, "You got "+gameStats.xp+ " XP", xpRect, HAlignment.CENTER, VAlignment.CENTER, Color.GREEN);
	
		Timer.Task task = new Timer.Task() {
			@Override
			public void run() {
				startDrawing = true;
			}
		};
		
		Timer.instance().scheduleTask(task, 2);
	}
	
	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		if (startDrawing) {
			backgroundImage.draw(spriteBatch, x, y);
			gameOverText.draw(spriteBatch, x, y);
			levelText.draw(spriteBatch, x, y);
			monText.draw(spriteBatch, x, y);
			xpText.draw(spriteBatch, x, y);
		}
	}
	
	@Override
	public boolean touchEvent(TouchEvent event) {
		touchEventProvider.removeTouchEventListener(this);
		dismiss();
		return true;
	}

}
