package com.dbash.presenters.root;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.TouchEvent;
import com.dbash.platform.TextView;
import com.dbash.util.Rect;


public class GameOverPopupPresenter extends PopupPresenter {

	TextView gameOverText;
	
	public GameOverPopupPresenter() {
		super("GameOver", null, null);
		popupController.popupCreated(this, popupId);  // tell the popup controller about me.
	}

	@Override
	public void init() {
		
		// Needs to swallow all touches to the screen to be modal
		touchEventProvider.addTouchEventListener(this, null, gui.cameraViewPort.viewPort);  //null area means entire screen
		
		Rect gameOverRect = new Rect(gui.sizeCalculator.dungeonArea, .3f);
		gameOverRect.y += area.height/3;
		gameOverText = new TextView(gui, "Game Over", gameOverRect, Color.WHITE);
		
//		// This is a 'show this popup next time' checkbox, which is always marked OK if the popup actually gets shown.
//		Rect checkBoxRect = new Rect(area, 0.1f, 0.75f, 0.75f, 0.1f);
//		checkBoxRect.height = checkBoxRect.width; // square it up
//		checkBox = new CheckBoxView(gui, touchEventProvider, checkBoxRect, true);
//		
//		Rect okButtonRect = new Rect(area, 0.75f, 0.1f, 0.75f, 0.1f);
//		okButtonRect.height = okButtonRect.width; // square it up
//		okButton = new ButtonView(gui, touchEventProvider, okButtonRect, "CONFIRM_SELECTED_IMAGE", "CONFIRM_BUTTON_IMAGE", "CONFIRM_BUTTON_IMAGE");
//		controller = popupController;
//		final String id = popupId;
//		final TouchEventListener me = this;
//		final CheckBoxView box = checkBox;
//		okButton.onClick( new IClickListener() {
//			public void processClick() {
//				touchEventProvider.removeTouchEventListener(me);
//				controller.popupDismissed(id, box.getState());
//			}
//		});
		
		//Rect  = new Rect(area, 0.05f, 0.05f, 0.03f, 0.35f);
		//textBox = new TextBoxView(gui, text, textBoxRect, Rect.HAlignment.CENTER, Rect.VAlignment.CENTER, Color.BLACK);
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
