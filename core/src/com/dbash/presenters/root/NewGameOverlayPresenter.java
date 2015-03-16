package com.dbash.presenters.root;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.Character;
import com.dbash.models.CreatureStats;
import com.dbash.models.IPresenterTurnState;
import com.dbash.models.TouchEvent;
import com.dbash.models.TouchEventListener;
import com.dbash.models.TouchEventProvider;
import com.dbash.models.TurnProcessor;
import com.dbash.platform.Audio;
import com.dbash.platform.ImagePatchView;
import com.dbash.platform.ImageView;
import com.dbash.platform.TextView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.tabs.CreatureListElementView;
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
	CreatureListElementView[] charImages;
	CheckBoxView tutorialButton;
	ButtonView startGameButton;
	ButtonView newCharsButton;
	ButtonView cancelButton;
	
	IPresenterTurnState turnProcessor;
	
	List<Character> characters;
	
	public NewGameOverlayPresenter(IPresenterTurnState turnProcessor) {
		this.turnProcessor = turnProcessor;
	}
	
	@Override
	public void init(UIDepend gui) {
		this.gui = gui;
	}
	
	@Override
	public void start(Rect theArea, TouchEventProvider touchEventProvider) {
		this.touchEventProvider = touchEventProvider;
		this.area = new Rect(gui.sizeCalculator.dungeonArea, .04f, .04f, .1f, .1f);
		
		// Needs to swallow all touches to the screen to be modal
		touchEventProvider.addTouchEventListener(this, null, gui.cameraViewPort.viewPort);  //null area means entire screen
		
		this.backgroundImage = new ImageView(gui, "PORTRAIT_IMAGE", area);
		this.mainBorder = new ImagePatchView(gui, "9patchborder", area);
		
		Rect chooseRect = new Rect(area, .5f);
		chooseRect.y += area.height/3;
		chooseText = new TextView(gui, null, "Choose your team", chooseRect, HAlignment.CENTER, VAlignment.CENTER, Color.WHITE);
		
		charBorders = new ImagePatchView[TurnProcessor.NUM_CHARS];
		Rect charBorderArea = new Rect(area);
		charBorderArea.width /= 3.5;
		charBorderArea.height /= 3;
		
		float charSpacer = (area.width - charBorderArea.width*3)/4;
		charBorderArea.x += charSpacer;
		charBorderArea.y += (charBorderArea.height);
		
		for (int i=0; i<charBorders.length;i++) {
			charBorders[i] = new ImagePatchView(gui, "9patchborder", charBorderArea);
			charBorderArea.x += (charBorderArea.width + charSpacer);
		}
		
		float buttonSpacerX = area.width/20;
		float buttonSpacerY = area.height/20;
		Rect buttonArea = new Rect(area.x+buttonSpacerX, area.y+buttonSpacerY, area.height/7, area.height/7);
		
		cancelButton = new ButtonView(gui, touchEventProvider, buttonArea, "SOLO_ON_IMAGE", 
				"SOLO_OFF_IMAGE", "SOLO_OFF_IMAGE", Audio.CLICK);
		cancelButton.onClick( new IClickListener() {
			public void processClick() {
				destroy();
			}
		});
		
		buttonArea.x = (float) (area.width - buttonArea.width*2.5);
		
		tutorialButton = new CheckBoxView(gui, touchEventProvider, buttonArea, true);
		
		buttonArea.x += (float) (buttonArea.width*1.5);
		
		startGameButton = new ButtonView(gui, touchEventProvider, buttonArea, "SOLO_ON_IMAGE", 
				"SOLO_OFF_IMAGE", "SOLO_OFF_IMAGE", Audio.CLICK);
		startGameButton.onClick( new IClickListener() {
			public void processClick() {
				turnProcessor.startGame(characters,  tutorialButton.getState());
				destroy();
			}
		});
		
		buttonArea.y = area.y + area.height - buttonArea.height - buttonSpacerY;
		
		newCharsButton = new ButtonView(gui, touchEventProvider, buttonArea, "SOLO_ON_IMAGE", 
				"SOLO_OFF_IMAGE", "SOLO_OFF_IMAGE", Audio.CLICK);
		newCharsButton.onClick( new IClickListener() {
			public void processClick() {
				createCharImages();
			}
		});

		createCharImages();
	}
	
	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y) {
			backgroundImage.draw(spriteBatch, x, y);
			mainBorder.draw(spriteBatch, x, y);
			chooseText.draw(spriteBatch, x, y);
			for (int i=0; i<TurnProcessor.NUM_CHARS; i++) {
				charImages[i].draw(spriteBatch, x, y);
				//charBorders[i].draw(spriteBatch, x, y);
			}
			cancelButton.draw(spriteBatch, x, y);
			tutorialButton.draw(spriteBatch, x, y);
			startGameButton.draw(spriteBatch, x, y);
			newCharsButton.draw(spriteBatch, x, y);
	}
	
	@Override
	public boolean touchEvent(TouchEvent event) {
		destroy();
		return true;
	}
	
	private void createCharImages() {
		characters = turnProcessor.createRandomCharacters();
		charImages = new CreatureListElementView[TurnProcessor.NUM_CHARS];
		int i = 0;
		for (Character character : characters) {
			CreatureStats stats = character.getCharacterStats();
			charImages[i] = new CreatureListElementView(gui, stats, charBorders[i].getArea(), false);
			i++;
		}
	}
	
	private void destroy() {
		touchEventProvider.removeTouchEventListener(this);
		dismiss();
	}

}
