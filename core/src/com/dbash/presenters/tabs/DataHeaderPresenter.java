package com.dbash.presenters.tabs;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.Character;
import com.dbash.models.CreatureStats;
import com.dbash.models.IPresenterTurnState;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEventProvider;
import com.dbash.models.UIInfoListener;
import com.dbash.platform.ImagePatchView;
import com.dbash.platform.ImageView;
import com.dbash.platform.TextView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.ButtonView;
import com.dbash.presenters.widgets.IClickListener;
import com.dbash.util.Rect;
import com.dbash.util.Rect.HAlignment;
import com.dbash.util.Rect.VAlignment;


/* 
 * This is the area at the top of the data side of the screen that is always visible regardless of what tab
 * Is currently being displayed.  It contains:
 * 		leader-mode toggle button 
 * 		go down stairs button
 * 		pass turn button
 * 
 * It also has the most important summary of the current character in focus, namely: 
 * 		current HP/MP
 */
public class DataHeaderPresenter {
	
	Rect area;
	private IPresenterTurnState presenterTurnState;
	private PresenterDepend mod;
	
	// views
	private ButtonView leaderToggleButton;
	private ButtonView passTurnButton;
	private ButtonView goDownButton;
	private ButtonView soloButton;
	private ImageView background;
	private TextView healthText;
	private TextView magicText;
	private ImageView healthIcon;
	private ImageView magicIcon;
	private ImagePatchView border;
	
	
	public DataHeaderPresenter(PresenterDepend model, UIDepend gui, TouchEventProvider touchEventProvider, Rect area) {
		
		this.area = new Rect(area);
		this.presenterTurnState = model.presenterTurnState;
		this.mod = model;
		
		// set up views;
		this.background = new ImageView(gui, "HEADER_IMAGE", area);
		this.border = new ImagePatchView(gui, "9patchborder", area); 
		// buttons
		float centre = area.width / 2;
		float space = area.width / 3.3f;
		float buttonWidth = area.width * .25f;  // each button is 25% of the width of the entire area
		float buttonHeight = buttonWidth;  // square buttons
		float buttonY = area.y + area.height*.95f - buttonHeight;   // 5% from the top
		float buttonX = area.x + centre - buttonWidth/2 - space;
		Rect buttonArea = new Rect (buttonX, buttonY, buttonWidth, buttonHeight);
		
		// stats
		float iconSize = area.height * .3f;
		float iconY = area.y + area.height * .09f;
		float iconX = area.x + area.width * .06f;
		Rect heartArea = new Rect (iconX, iconY, iconSize, iconSize);
		healthIcon = new ImageView(gui, "HEART_IMAGE", heartArea);

		Rect textArea = new Rect (heartArea, 0f, 0f, .015f, 0.15f);
		textArea.x += heartArea.width * 1.0f;
		textArea.width *= 2.6f;
		healthText = new TextView(gui,gui.numericalFonts, new String(""), textArea, HAlignment.CENTER, VAlignment.CENTER, Color.RED);
		
		heartArea.x += area.width*.45f;
		magicIcon = new ImageView(gui, "MAGIC_STAR_IMAGE", heartArea);
		textArea.x += area.width*.42f;
		magicText = new TextView(gui,gui.numericalFonts, new String(""), textArea, HAlignment.CENTER, VAlignment.CENTER, Color.BLUE);
		
		
		// put the buttons in place.
		leaderToggleButton = new ButtonView(gui, touchEventProvider, buttonArea, "LEADER_ON_IMAGE", 
				"LEADER_OFF_IMAGE", "LEADER_DISABLED_IMAGE");
		leaderToggleButton.onClick( new IClickListener() {
			public void processClick() {
				presenterTurnState.LeaderModeToggleSelected();
			}
		});
		
		buttonArea.x += space;
		passTurnButton = new ButtonView(gui, touchEventProvider, buttonArea, "PASS_TURN_ON_IMAGE", 
				"PASS_TURN_OFF_IMAGE", "PASS_TURN_OFF_IMAGE");
		passTurnButton.onClick( new IClickListener() {
			public void processClick() {
				presenterTurnState.passTurnSelected();
			}
		});
		
		buttonArea.x += space;
		goDownButton = new ButtonView(gui, touchEventProvider, buttonArea, "STAIRS_ON_IMAGE", 
				"STAIRS_OFF_IMAGE", "STAIRS_DISABLED_IMAGE");
		goDownButton.onClick( new IClickListener() {
			public void processClick() {
				presenterTurnState.stairDescendSelected();
			}
		});
		
		soloButton = new ButtonView(gui, touchEventProvider, buttonArea, "SOLO_ON_IMAGE", 
				"SOLO_OFF_IMAGE", "SOLO_OFF_IMAGE");
		soloButton.onClick( new IClickListener() {
			public void processClick() {
				presenterTurnState.soloSelected();
			}
		});
		
		setup();
	}
	

	private void setup()
	{
		// Subscribe to changes to the current character.
		newCharacter(mod.presenterTurnState.getCurrentCharacter());
		mod.presenterTurnState.onChangeToCurrentCharacter(new UIInfoListener() {
			public void UIInfoChanged() {
				Character character = mod.presenterTurnState.getCurrentCharacter();
				if (character.isPlayerCharacter()) {
					newCharacter(character);
				}
			}
		});
		
		// Subscribe to changes in leader status
		mod.presenterTurnState.onChangeToLeaderStatus(new UIInfoListener() {
			public void UIInfoChanged() {
				processLeaderStatus();
			}
		});
		
		// Subscribe to changes in leader status
		mod.presenterTurnState.onChangeToSoloStatus(new UIInfoListener() {
			public void UIInfoChanged() {
				processSoloStatus();
			}
		});
		
		processLeaderStatus();
		processSoloStatus();
	}
	
	private void newCharacter(Character character)
	{
		final Character currentCharacter = character;
		
		character.onChangeToCharacterStats((new UIInfoListener() {
			public void UIInfoChanged() {
				CreatureStats stats = currentCharacter.getCharacterStats();
				updateStats(stats);
			}
		}));
		
		CreatureStats stats = currentCharacter.getCharacterStats();
		updateStats(stats);
		
		// update button status for this character
		boolean onStairs = currentCharacter.isCharacterOnStairs();
		if (onStairs) {
			soloButton.setEnabled(false);
			goDownButton.setEnabled(true);
		} else {
			soloButton.setEnabled(true);
			goDownButton.setEnabled(false);
		}
	}
	
	public void updateStats(CreatureStats stats)
	{
		healthText.setText(stats.health+"/"+stats.maxHealth);
		magicText.setText(stats.magic+"/"+stats.maxMagic);
	}
	
	protected void processSoloStatus() {
		boolean soloStatus = mod.presenterTurnState.getSoloStatus();
		soloButton.setState(soloStatus);
	}
	
	protected  void processLeaderStatus() {

		switch(mod.presenterTurnState.getLeaderStatus()) {
			case NO_LEADER:
				leaderToggleButton.setEnabled(true);
				leaderToggleButton.setState(false);
				break;
			case HAVE_LEADER:
				leaderToggleButton.setEnabled(true);
				leaderToggleButton.setState(true);
				//new PopupPresenter("test popup", "EFFECT_IMAGE", "You can attack a monster standing next to a character.  But that would be unneccessarilly cruel by any account.  nevertheless I imagine some nasty types might attempt such a thing! these peope should not be trsuted.");
				break;
			case LEADER_DISABLED:
			default:
				leaderToggleButton.setEnabled(false);
				leaderToggleButton.setState(false);
				break;
		}
	}
	
	public void draw(SpriteBatch spriteBatch) {
		background.draw(spriteBatch);
		leaderToggleButton.draw(spriteBatch);
		passTurnButton.draw(spriteBatch);
		if (goDownButton.isEnabled()) {
			goDownButton.draw(spriteBatch);
		} else {
			soloButton.draw(spriteBatch);
		}
		healthText.draw(spriteBatch);
		magicText.draw(spriteBatch);
		healthIcon.draw(spriteBatch);
		magicIcon.draw(spriteBatch);
		border.draw(spriteBatch);
	}
	
}
