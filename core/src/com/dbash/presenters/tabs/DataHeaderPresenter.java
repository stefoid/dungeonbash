package com.dbash.presenters.tabs;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.Character;
import com.dbash.models.Creature;
import com.dbash.models.CreatureStats;
import com.dbash.models.IEventAction;
import com.dbash.models.IPresenterTurnState;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEventProvider;
import com.dbash.models.UIInfoListener;
import com.dbash.platform.AnimationView;
import com.dbash.platform.Audio;
import com.dbash.platform.ImagePatchView;
import com.dbash.platform.ImageView;
import com.dbash.platform.TextView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.root.HighlightAnimView;
import com.dbash.presenters.root.tutorial.TutorialPresenter;
import com.dbash.presenters.widgets.ButtonView;
import com.dbash.presenters.widgets.IClickListener;
import com.dbash.util.EventBus;
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
	
	UIDepend gui;
	Rect area;
	private IPresenterTurnState presenterTurnState;
	private PresenterDepend mod;
	
	// views
	private ButtonView leaderToggleButton;
	private ButtonView passTurnButton;
	private ButtonView goDownButton;
	private ButtonView soloButton;
	private ButtonView stealthToggleButton;
	private ImageView background;
	private TextView healthText;
	private TextView magicText;
	private ImageView healthIcon;
	private ImageView magicIcon;
	private ImagePatchView border;
	private AnimationView leaderButtonAnim;
	private AnimationView soloButtonAnim;
	private AnimationView passButtonAnim;
	private AnimationView stealthButtonAnim;
	
	public DataHeaderPresenter(PresenterDepend model, UIDepend gui, TouchEventProvider touchEventProvider, Rect area) {
	
		this.gui = gui;
		this.area = new Rect(area);
		this.presenterTurnState = model.presenterTurnState;
		this.mod = model;
		
		// set up views;
		this.background = new ImageView(gui, "HEADER_IMAGE", area);
		this.border = new ImagePatchView(gui, "9patchborder", area); 
		
		// buttons
		float centre = area.width / 2;
		float space = area.width / 4f;
		float buttonWidth = area.width * .25f;  // each button is 25% of the width of the entire area
		float buttonHeight = buttonWidth;  // square buttons
		float buttonY = area.y + area.height*.95f - buttonHeight;   // 5% from the top
		float buttonX = area.x;// + buttonWidth/2;
		Rect buttonArea = new Rect (buttonX, buttonY, buttonWidth, buttonHeight);
		this.leaderButtonAnim = null;
		
		// stats
		float iconSize = area.height * .3f;
		float iconY = area.y + area.height * .09f;
		float iconX = area.x + area.width * .06f;
		Rect heartArea = new Rect (iconX, iconY, iconSize, iconSize);
		healthIcon = new ImageView(gui, "HEART_IMAGE", heartArea);

		Rect textArea = new Rect (heartArea, 0f, 0f, .015f, 0.15f);
		textArea.x += heartArea.width * 1.0f;
		textArea.width *= 2.5f;
		healthText = new TextView(gui,gui.numericalFonts, new String(""), textArea, HAlignment.CENTER, VAlignment.CENTER, Color.RED);
		
		heartArea.x += area.width*.45f;
		magicIcon = new ImageView(gui, "MAGIC_STAR_IMAGE", heartArea);
		textArea.x += area.width*.42f;
		magicText = new TextView(gui,gui.numericalFonts, new String(""), textArea, HAlignment.CENTER, VAlignment.CENTER, Color.BLUE);
		
		// put the buttons in place.
		passTurnButton = new ButtonView(gui, touchEventProvider, buttonArea, "PASS_TURN_ON_IMAGE", 
				"PASS_TURN_OFF_IMAGE", "PASS_TURN_OFF_IMAGE", null);
		passTurnButton.onClick( new IClickListener() {
			public void processClick() {
				presenterTurnState.passTurnSelected();
			}
		});
		
		buttonArea.x += space;
		
		stealthToggleButton = new ButtonView(gui, touchEventProvider, buttonArea, "SNEAK_SELECTED_IMAGE", 
				"SNEAK_ENABLED_IMAGE", "SNEAK_DISABLED_IMAGE", Audio.CLICK);
		stealthToggleButton.onClick( new IClickListener() {
			public void processClick() {
				presenterTurnState.stealthSelected();
			}
		});
		
		buttonArea.x += space;
		
		leaderToggleButton = new ButtonView(gui, touchEventProvider, buttonArea, "LEADER_ON_IMAGE", 
				"LEADER_OFF_IMAGE", "LEADER_DISABLED_IMAGE", Audio.CLICK);
		leaderToggleButton.onClick( new IClickListener() {
			public void processClick() {
				presenterTurnState.leaderModeToggleSelected();
			}
		});
		
		buttonArea.x += space;
		
		soloButton = new ButtonView(gui, touchEventProvider, buttonArea, "SOLO_ON_IMAGE", 
				"SOLO_OFF_IMAGE", "SOLO_OFF_IMAGE", Audio.CLICK);
		soloButton.onClick( new IClickListener() {
			public void processClick() {
				presenterTurnState.soloSelected();
			}
		});
		
		goDownButton = new ButtonView(gui, touchEventProvider, buttonArea, "STAIRS_ON_IMAGE", 
				"STAIRS_OFF_IMAGE", "STAIRS_DISABLED_IMAGE", Audio.CLICK);
		goDownButton.onClick( new IClickListener() {
			public void processClick() {
				presenterTurnState.stairDescendSelected();
			}
		});
		
		setup();
	}
	

	private void setup() {
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
		
		EventBus.getDefault().onEvent(TutorialPresenter.ANIM_LEADER_BUTTON_ON_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				Rect fromRect = new Rect(leaderToggleButton.getArea(), .6f);
				Rect toRect = new Rect(leaderToggleButton.getArea(), 1.4f);
				if (leaderButtonAnim != null) {
					leaderButtonAnim.stopPlaying();
				}
				leaderButtonAnim = new HighlightAnimView(gui, fromRect, toRect);
				leaderButtonAnim.startPlaying();
			}
		});
		
		EventBus.getDefault().onEvent(TutorialPresenter.ANIM_LEADER_BUTTON_OFF_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (leaderButtonAnim != null) {
					leaderButtonAnim.stopPlaying();
					leaderButtonAnim = null;
				}
			}
		});
		
		EventBus.getDefault().onEvent(TutorialPresenter.ANIM_SOLO_BUTTON_ON_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				Rect fromRect = new Rect(soloButton.getArea(), .6f);
				Rect toRect = new Rect(soloButton.getArea(), 1.4f);
				if (soloButtonAnim != null) {
					soloButtonAnim.stopPlaying();
				}
				soloButtonAnim = new HighlightAnimView(gui, fromRect, toRect);
				soloButtonAnim.startPlaying();
			}
		});
		
		EventBus.getDefault().onEvent(TutorialPresenter.ANIM_SOLO_BUTTON_OFF_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (soloButtonAnim != null) {
					soloButtonAnim.stopPlaying();
					soloButtonAnim = null;
				}
			}
		});
		
		EventBus.getDefault().onEvent(TutorialPresenter.ANIM_PASS_BUTTON_ON_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				Rect fromRect = new Rect(passTurnButton.getArea(), .6f);
				Rect toRect = new Rect(passTurnButton.getArea(), 1.4f);
				if (passButtonAnim != null) {
					passButtonAnim.stopPlaying();
				}
				passButtonAnim = new HighlightAnimView(gui, fromRect, toRect);
				passButtonAnim.startPlaying();
			}
		});
		
		EventBus.getDefault().onEvent(TutorialPresenter.ANIM_PASS_BUTTON_OFF_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (passButtonAnim != null) {
					passButtonAnim.stopPlaying();
					passButtonAnim = null;
				}
			}
		});
		
		EventBus.getDefault().onEvent(TutorialPresenter.ANIM_STEALTH_BUTTON_ON_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				Rect fromRect = new Rect(stealthToggleButton.getArea(), .6f);
				Rect toRect = new Rect(stealthToggleButton.getArea(), 1.4f);
				if (stealthButtonAnim != null) {
					stealthButtonAnim.stopPlaying();
				}
				stealthButtonAnim = new HighlightAnimView(gui, fromRect, toRect);
				stealthButtonAnim.startPlaying();
			}
		});
		
		EventBus.getDefault().onEvent(TutorialPresenter.ANIM_STEALTH_BUTTON_OFF_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (stealthButtonAnim != null) {
					stealthButtonAnim.stopPlaying();
					stealthButtonAnim = null;
				}
			}
		});
		
		EventBus.getDefault().onEvent(TutorialPresenter.ALL_BUTTON_ANIMS_OFF, this, new IEventAction() {
			@Override
			public void action(Object param) {
				EventBus.getDefault().event(TutorialPresenter.ANIM_LEADER_BUTTON_OFF_EVENT, null);
				EventBus.getDefault().event(TutorialPresenter.ANIM_PASS_BUTTON_OFF_EVENT, null);
				EventBus.getDefault().event(TutorialPresenter.ANIM_SOLO_BUTTON_OFF_EVENT, null);
				EventBus.getDefault().event(TutorialPresenter.ANIM_STEALTH_BUTTON_OFF_EVENT, null);
			}
		});
		
		processLeaderStatus();
		processSoloStatus();
	}
	
	private void newCharacter(Character character) {
		final Character currentCharacter = character;
		
		character.onChangeToCharacterStats((new UIInfoListener() {
			public void UIInfoChanged() {
				CreatureStats stats = currentCharacter.getCharacterStats();
				updateStats(stats);
			}
		}));
		
		character.onChangeToStealthStatus((new UIInfoListener() {
			public void UIInfoChanged() {
				processStealthStatus(currentCharacter.getStealthStatus());
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
	
		processStealthStatus(currentCharacter.getStealthStatus());
	}
	
	public void updateStats(CreatureStats stats) {
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
				break;
			case LEADER_DISABLED:
			default:
				leaderToggleButton.setEnabled(false);
				leaderToggleButton.setState(false);
				break;
		}
	}
	
	protected  void processStealthStatus(Creature.StealthStatus stealthStatus) {

		switch(stealthStatus) {
			case HIDING_POSSIBLE:
				stealthToggleButton.setEnabled(true);
				stealthToggleButton.setState(false);
				break;
			case HIDING:
				stealthToggleButton.setEnabled(true);
				stealthToggleButton.setState(true);
				break;
			case HIDING_IMPOSSIBLE:
			default:
				stealthToggleButton.setEnabled(false);
				stealthToggleButton.setState(false);
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
		stealthToggleButton.draw(spriteBatch);
		healthText.draw(spriteBatch);
		magicText.draw(spriteBatch);
		healthIcon.draw(spriteBatch);
		magicIcon.draw(spriteBatch);
		border.draw(spriteBatch);
		
		if (leaderButtonAnim != null) {
			leaderButtonAnim.draw(spriteBatch);
		}
		if (stealthButtonAnim != null) {
			stealthButtonAnim.draw(spriteBatch);
		}
		if (passButtonAnim != null) {
			passButtonAnim.draw(spriteBatch);
		}
		if (soloButtonAnim != null) {
			soloButtonAnim.draw(spriteBatch);
		}
	}
	
}
