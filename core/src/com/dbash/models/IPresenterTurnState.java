package com.dbash.models;

import java.util.ArrayList;
import java.util.List;

import com.dbash.presenters.root.tutorial.TutorialPresenter;



// the interface for Presenters to talk to the model about things that affect the gamestate
// like a character passing their turn
// possibly leader  mode?

public interface IPresenterTurnState {

	public enum LeaderStatus{
		LEADER_DISABLED,
		NO_LEADER,
		HAVE_LEADER,
		NONE
	}
	
	// Overall gamestate
	public void mainMenuStartGameSelected();  // main menu button
	public void cancelNewGameSelected();
	public List<Character> createRandomCharacters();
	public List<Character> getTutorialCharacters();
	public void startGame(List<Character> characters, boolean tutorialMode);
	public void infoSelected();
	public void saveGame(TutorialPresenter.State state);
	public List<Character> getAllCharacters();
	public void powerUpComplete();
	
	// Current character related
	public Character getCurrentCharacter();
	public Character getCharacterForTouchEvents();
	public void setCurrentCharacter(Character character);
	public void setCurrentCharacterOutsideOfTurnProcessing(Character character);
	public boolean itemPickupSelected(Character character, Ability ability);
	public boolean itemDropSelected();
	public boolean abilitySelected(Character character, Ability ability);
	
	// data header area buttons - all of these affect turn-processing
	public void passTurnSelected();
	public void leaderModeToggleSelected();
	public void stairDescendSelected();
	public void soloSelected();
	public void stealthSelected();
	
	// Leader mode
	public void onChangeToLeaderStatus(UIInfoListener listener);
	public LeaderStatus getLeaderStatus();
	
	public void onChangeToSoloStatus(UIInfoListener listener);
	public boolean getSoloStatus();
	
	// eye mode
	public void usingEye(boolean usingEye);
	
	// XP / powerupstate
	public int getSpentXp();
	public int getTotalXp();
	public void setSpentXp(int spentXp);
}
