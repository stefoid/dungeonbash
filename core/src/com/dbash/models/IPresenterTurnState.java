package com.dbash.models;

import java.util.ArrayList;
import java.util.List;



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
	public List<Character> createRandomCharacters();
	public List<Character> getTutorialCharacters();
	public void startGame(List<Character> characters, boolean tutorialMode);
	public void infoSelected();
	
	// Current character related
	public void onChangeToCurrentCharacter(UIInfoListener listener);
	public Character getCurrentCharacter();
	public Character getCharacterForTouchEvents();
	public void setCurrentCharacter(Character character);
	
	// data header area buttons - all of these affect turn-processing
	public void passTurnSelected();
	public void leaderModeToggleSelected();
	public void stairDescendSelected();
	public void soloSelected();
	
	// Leader mode
	public void onChangeToLeaderStatus(UIInfoListener listener);
	public LeaderStatus getLeaderStatus();
	
	public void onChangeToSoloStatus(UIInfoListener listener);
	public boolean getSoloStatus();
	
	// eye mode
	public void usingEye(boolean usingEye);
}
