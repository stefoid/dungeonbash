package com.dbash.presenters.root;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.TouchEvent;
import com.dbash.models.TouchEventListener;
import com.dbash.models.TouchEventProvider;
import com.dbash.models.TurnProcessor;
import com.dbash.platform.Audio;
import com.dbash.platform.ImagePatchView;
import com.dbash.platform.ImageView;
import com.dbash.platform.TextView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.ButtonView;
import com.dbash.presenters.widgets.CheckBoxView;
import com.dbash.presenters.widgets.IClickListener;
import com.dbash.util.Rect;
import com.dbash.util.Rect.HAlignment;
import com.dbash.util.Rect.VAlignment;


public class NewGameOverlayPresenter extends OverlayPresenter implements TouchEventListener {
	
	ImageView backgroundImage;
	ImagePatchView mainBorder;
	TextView chooseText;
	ImagePatchView[] charBorders;
	ImageView[] charImages;
	CheckBoxView tutorialButton;
	ButtonView startGameButton;
	ButtonView newCharsButton;
	ButtonView cancelButton;
	
	TurnProcessor turnProcessor;

	
	public NewGameOverlayPresenter(TurnProcessor turnProcessor) {
		this.turnProcessor = turnProcessor;
	}
	
	@Override
	public void init(UIDepend gui) {
		this.gui = gui;
	}
	
	@Override
	public void start(Rect theArea, TouchEventProvider touchEventProvider) {
		this.touchEventProvider = touchEventProvider;
		this.area = theArea;
		
		// Needs to swallow all touches to the screen to be modal
		touchEventProvider.addTouchEventListener(this, null, gui.cameraViewPort.viewPort);  //null area means entire screen
		
		Rect backgroundArea = new Rect(area, .04f, .04f, .1f, .1f);
		this.backgroundImage = new ImageView(gui, "PORTRAIT_IMAGE", backgroundArea);
		this.mainBorder = new ImagePatchView(gui, "9patchborder", backgroundArea);
		
		Rect chooseRect = new Rect(area, .5f);
		chooseRect.y += area.height/3;
		chooseText = new TextView(gui, null, "Choose your team", chooseRect, HAlignment.CENTER, VAlignment.CENTER, Color.WHITE);
		
		charBorders = new ImagePatchView[TurnProcessor.NUM_CHARS];
		Rect charBorderArea = new Rect(area);
		charBorderArea.width /= 5;
		charBorderArea.height /= 3;
		charBorderArea.x += (charBorderArea.width*.4);
		charBorderArea.y += (charBorderArea.height);
		for (int i=0; i<charBorders.length;i++) {
			charBorders[i] = new ImagePatchView(gui, "9patchborder", charBorderArea);
			charBorderArea.x += charBorderArea.width * 1.2;
		}
		
		float buttonSpacerX = area.width/20;
		float buttonSpacerY = area.height/20;
		Rect buttonArea = new Rect(backgroundArea.x+buttonSpacerX, backgroundArea.y+buttonSpacerY, area.height/7, area.height/7);
		
		cancelButton = new ButtonView(gui, touchEventProvider, buttonArea, "SOLO_ON_IMAGE", 
				"SOLO_OFF_IMAGE", "SOLO_OFF_IMAGE", Audio.CLICK);
		cancelButton.onClick( new IClickListener() {
			public void processClick() {
				//presenterTurnState.soloSelected();
			}
		});
		
		buttonArea.x = (float) (backgroundArea.width - buttonArea.width*2.5);
		
		tutorialButton = new CheckBoxView(gui, touchEventProvider, buttonArea, true);
		tutorialButton.onClick( new IClickListener() {
			public void processClick() {
				//presenterTurnState.soloSelected();
			}
		});
		
		buttonArea.x += (float) (buttonArea.width*1.5);
		
		startGameButton = new ButtonView(gui, touchEventProvider, buttonArea, "SOLO_ON_IMAGE", 
				"SOLO_OFF_IMAGE", "SOLO_OFF_IMAGE", Audio.CLICK);
		startGameButton.onClick( new IClickListener() {
			public void processClick() {
				//presenterTurnState.soloSelected();
			}
		});
		
		buttonArea.y = charBorderArea.y + charBorderArea.height/2;
		
		newCharsButton = new ButtonView(gui, touchEventProvider, buttonArea, "SOLO_ON_IMAGE", 
				"SOLO_OFF_IMAGE", "SOLO_OFF_IMAGE", Audio.CLICK);
		newCharsButton.onClick( new IClickListener() {
			public void processClick() {
				//presenterTurnState.soloSelected();
			}
		});
		
//		Rect levelRect = new Rect(area);
//		levelRect.height /= 20;
//		levelRect.y = gameOverRect.y + levelRect.height*2;
//		levelText = new TextView(gui, null, "You reached level "+gameStats.level, levelRect, HAlignment.CENTER, VAlignment.CENTER, Color.RED);
//		
//		Rect monRect = new Rect(levelRect);
//		monRect.y -= (monRect.height * 1.5f);
//		monText = new TextView(gui, null, "You killed "+gameStats.monstersKilled+ " monsters", monRect, HAlignment.CENTER, VAlignment.CENTER, Color.CYAN);
//		
//		Rect xpRect = new Rect(monRect);
//		xpRect.y -= (xpRect.height * 1.5f);
//		xpText = new TextView(gui, null, "You got "+gameStats.xp+ " XP", xpRect, HAlignment.CENTER, VAlignment.CENTER, Color.GREEN);
	}
	
	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y) {
			backgroundImage.draw(spriteBatch, x, y);
			mainBorder.draw(spriteBatch, x, y);
			chooseText.draw(spriteBatch, x, y);
			for (ImagePatchView border : charBorders) {
				border.draw(spriteBatch, x, y);
			}
			cancelButton.draw(spriteBatch, x, y);
			tutorialButton.draw(spriteBatch, x, y);
			startGameButton.draw(spriteBatch, x, y);
			newCharsButton.draw(spriteBatch, x, y);
	}
	
	@Override
	public boolean touchEvent(TouchEvent event) {
		touchEventProvider.removeTouchEventListener(this);
		dismiss();
		return true;
	}

}
