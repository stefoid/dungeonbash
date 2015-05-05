package com.dbash.presenters.root;

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
import com.dbash.presenters.widgets.IClickListener;
import com.dbash.util.Rect;
import com.dbash.util.Rect.HAlignment;
import com.dbash.util.Rect.VAlignment;


public class PowerupOverlayPresenter extends OverlayPresenter implements TouchEventListener {
	
	private class CharacterView {
		public Rect area;
		public ButtonView button;
		public ImagePatchView border;
		public CreatureListElementView charImage;
		
		public CharacterView(UIDepend gui, Rect area, Character character, TouchEventProvider touchEventProvider) {
			this.area = area;
			button = new ButtonView(gui, touchEventProvider, area, "NO_IMAGE","NO_IMAGE","NO_IMAGE", Audio.CLICK);
			border = new ImagePatchView(gui, "9patchborder", area);
			CreatureStats stats = character.getCharacterStats();
			charImage = new CreatureListElementView(gui, stats, area, false);
		}
		
		public void onClick(IClickListener listener) {
			button.onClick(listener);
		}
		
		public void draw(SpriteBatch spriteBatch, float x, float y) {
			charImage.draw(spriteBatch, x, y);
			if (button.getState()) {
				border.draw(spriteBatch, x, y);
			}
		}
		
		public void set(){
			button.setState(true);
		}
		
		public void clear() {
			button.setState(false);
		}
		
		public void destroy() {
			button.removeYourself();
		}
	}
	
	ImageView backgroundImage;
	ImagePatchView mainBorder;
	TextView xpText;
	CharacterView[] charViews;
	ButtonView okButton;
	//ButtonView cancelButton;
	
	IPresenterTurnState turnProcessor;
	List<Character> characters;
	
	public PowerupOverlayPresenter(IPresenterTurnState turnProcessor, List<Character> characters) {
		this.turnProcessor = turnProcessor;
		this.characters = characters;
	}
	
	@Override
	public void init(UIDepend gui) {
		this.gui = gui;
	}
	
	@Override
	public void start(Rect theArea, TouchEventProvider touchEventProvider) {
		this.touchEventProvider = touchEventProvider;
		this.area = new Rect(gui.sizeCalculator.dungeonArea, .04f, .04f, .1f, .1f);
		
		// Needs to swallow all touches to the dungeon area 
		touchEventProvider.addTouchEventListener(this, gui.sizeCalculator.dungeonArea, gui.cameraViewPort.viewPort);  
		
		this.backgroundImage = new ImageView(gui, "PORTRAIT_IMAGE", area);
		this.mainBorder = new ImagePatchView(gui, "9patchborder", area);
		
		Rect chooseRect = new Rect(area, .5f);
		chooseRect.y += area.height/3;
		xpText = new TextView(gui, null, "Choose your team", chooseRect, HAlignment.CENTER, VAlignment.CENTER, Color.WHITE);
	
		createNewChars();
		
		float buttonSpacerX = area.width/20;
		float buttonSpacerY = area.height/20;
		Rect buttonArea = new Rect(area.x+buttonSpacerX, area.y+buttonSpacerY, area.height/7, area.height/7);
		
//		cancelButton = new ButtonView(gui, touchEventProvider, buttonArea, "CANCEL_DOWN_IMAGE", 
//				"CANCEL_IMAGE", "CANCEL_IMAGE", Audio.CLICK);
//		cancelButton.onClick( new IClickListener() {
//			public void processClick() {
//				turnProcessor.cancelNewGameSelected();
//			}
//		});
		
		buttonArea.x = (float) (area.width - buttonArea.width*2.5);	
		buttonArea.x += (float) (buttonArea.width*1.5);
		
		okButton = new ButtonView(gui, touchEventProvider, buttonArea, "START_GAME_DOWN_IMAGE", 
				"START_GAME_IMAGE", "START_GAME_IMAGE", Audio.CLICK);
		okButton.onClick( new IClickListener() {
			public void processClick() {
				// TODO
			}
		});

		createNewChars();
	}
	
	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y) {
			backgroundImage.draw(spriteBatch, x, y);
			mainBorder.draw(spriteBatch, x, y);
			xpText.draw(spriteBatch, x, y);
			for (int i=0; i<TurnProcessor.NUM_CHARS; i++) {
				charViews[i].draw(spriteBatch, x, y);
			}
			//cancelButton.draw(spriteBatch, x, y);
			okButton.draw(spriteBatch, x, y);
	}
	
	@Override
	public boolean touchEvent(TouchEvent event) {
		return true;  // swallow touches to the dungeon area.
	}
	
	private void createNewChars() {
		
		if (charViews == null) {
			charViews = new CharacterView[TurnProcessor.NUM_CHARS];
		}
		
		killCharButtons();
		
		Rect charArea = new Rect(area);
		charArea.width /= 3.5;
		charArea.height /= 3;
		
		float charSpacer = (area.width - charArea.width*3)/4;
		charArea.x += charSpacer;
		charArea.y += (charArea.height);
		
		for (int i=0; i<charViews.length;i++) {
			final Character character = characters.get(i);
			charViews[i] = new CharacterView(gui, charArea, character, touchEventProvider);
			final CharacterView theCharView = charViews[i];
			charViews[i].onClick(new IClickListener() {
				@Override
				public void processClick() {
					for (CharacterView c : charViews) {
						if (c == theCharView) {
							c.set();
							turnProcessor.setCurrentCharacterOutsideOfTurnProcessing(characters.get(0));
						} else {
							c.clear();
						}
					}
				}
			});
			charArea.x += (charArea.width + charSpacer);
		}
		
		charViews[0].set();
		turnProcessor.setCurrentCharacterOutsideOfTurnProcessing(characters.get(0));
	}
	
	private void killCharButtons() {
		for (CharacterView c : charViews) {
			if (c != null) {
				c.destroy();
			}
		}
	}
	
	public void destroy() {
		//cancelButton.removeYourself();
		okButton.removeYourself();
		killCharButtons();
		touchEventProvider.removeTouchEventListener(this);
	}

}
