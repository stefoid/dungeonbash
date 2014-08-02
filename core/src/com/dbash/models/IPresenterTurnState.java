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
	public void quitSelected();
	
	// Current character related
	public void onChangeToCurrentCharacter(UIInfoListener listener);
	public Character getCurrentCharacter();
	public Character getCharacterForTouchEvents();
	
	// data header area buttons - all of these affect turn-processing
	public void passTurnSelected();
	public void LeaderModeToggleSelected();
	public void stairDescendSelected();
	public boolean lederIsSoloing();
	
	// Leader mode
	public void onChangeToLeaderStatus(UIInfoListener listener);
	public LeaderStatus getLeaderStatus();
	
	// eye mode
	public void usingEye(boolean usingEye);
	
	public float getRange();
	public float getAlpha();
	public boolean getUseBlack();
	public void setRange(float range);
	public void setAlpha(float alpha);
	public void setUseBlack(boolean useBlack);
	
}
