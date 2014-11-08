package com.dbash.models;



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
	public void startGameSelected();
	public boolean gameInProgress();
	public void onChangeToGameInProgress(UIInfoListener listener); 
	public void infoSelected();
	
	// Current character related
	public void onChangeToCurrentCharacter(UIInfoListener listener);
	public Character getCurrentCharacter();
	public Character getCharacterForTouchEvents();
	
	// data header area buttons - all of these affect turn-processing
	public void passTurnSelected();
	public void LeaderModeToggleSelected();
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
