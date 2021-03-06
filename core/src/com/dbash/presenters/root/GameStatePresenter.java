package com.dbash.presenters.root;

import com.dbash.models.GameStats;
import com.dbash.models.IEventAction;
import com.dbash.models.IPresenterTurnState;
import com.dbash.models.PresenterDepend;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.root.tutorial.TutorialPresenter;
import com.dbash.util.EventBus;
import com.dbash.util.L;

public class GameStatePresenter {

	public static final boolean LOG = false && L.DEBUG;
	
	EventBus eventBus;
	UIDepend gui;
	TutorialPresenter tutorialPresenter;
	OverlayPresenter powerupPresenter;
	PresenterDepend model;

	public static final String GAME_OVER_EVENT = "gameover";
	public static final String START_TUTORIAL_EVENT = "starttutorialgame";
	public static final String START_GAME_EVENT = "startgame";
	public static final String NEW_GAME_EVENT = "newgame";
	public static final String NO_SAVED_GAME_EVENT = "nosavedgame";
	public static final String BROKEN_GAME_EVENT = "nosavedgame";
	public static final String TUTORIAL_OVER_EVENT = "tutorialover";
	public static final String POWERUP_START = "POWERUP_START";
	public static final String POWERUP_OVER = "POWERUP_OVER";
	public static final String POWERUP_TAB_ADD = "POWERUP_TAB_ADD";
	public static final String POWERUP_TAB_REMOVE = "POWERUP_TAB_REMOVE";
	public static final String POWERUP_TAB_BUTTON_OFF_EVENT = "POWERUP_TAB_BUTTON_OFF_EVENT";
	public static final String POWERUP_TAB_BUTTON_ON_EVENT = "POWERUP_TAB_BUTTON_ON_EVENT";
	
	
	public GameStatePresenter(PresenterDepend model, UIDepend gui) {
		this.eventBus = EventBus.getDefault();
		this.gui = gui;
		this.model = model;
		init();
	}
	
	private void init() {
		
		eventBus.onEvent(GameStatePresenter.NO_SAVED_GAME_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("NO_SAVED_GAME_EVENT");
				removePresenters();
				OverlayPresenter newGamePresenter = new NewGameOverlayPresenter((IPresenterTurnState) param, true);
				gui.overlayQueues.addSequential(newGamePresenter);
			}
		});
		
		eventBus.onEvent(GameStatePresenter.NEW_GAME_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("NEW_GAME_EVENT");
				removePresenters();
				OverlayPresenter newGamePresenter = new NewGameOverlayPresenter((IPresenterTurnState) param, false);
				gui.overlayQueues.addSequential(newGamePresenter);
				eventBus.event(POWERUP_TAB_ADD, true);
			}
		});
		
		eventBus.onEvent(GameStatePresenter.START_GAME_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("START_GAME_EVENT");
				removePresenters();
			}
		});
		
		eventBus.onEvent(GameStatePresenter.START_TUTORIAL_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("START_TUTORIAL_EVENT");
				tutorialPresenter = new TutorialPresenter(model, gui);
			}
		});
		
		eventBus.onEvent(GameStatePresenter.GAME_OVER_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("GAME_OVER_EVENT");
				removePresenters();
				OverlayPresenter gameOverPresenter = new GameOverOverlayPresenter((GameStats) param);
				gui.overlayQueues.addSequential(gameOverPresenter);
			}
		});
		
		eventBus.onEvent(GameStatePresenter.TUTORIAL_OVER_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("TUTORIAL_OVER_EVENT");
				removePresenters();
			}
		});
		
		eventBus.onEvent(GameStatePresenter.POWERUP_START, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("POWERUP_START");
				removePresenters();
				powerupPresenter = new PowerupOverlayPresenter((IPresenterTurnState) param);
				gui.overlayQueues.addSequential(powerupPresenter);
			}
		});
		
		eventBus.onEvent(GameStatePresenter.POWERUP_OVER, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("POWERUP_OVER");
				removePresenters();
				model.presenterTurnState.powerUpComplete();
			}
		});
	}
	
	private void removePresenters() {
		EventBus.getDefault().event(TutorialPresenter.TUTORIAL_RESTART, null);
		gui.overlayQueues.removeAll();
		if (tutorialPresenter != null) {
			tutorialPresenter.onDestroy();
		}
		tutorialPresenter = null;
		
		if (powerupPresenter != null) {
			powerupPresenter.destroy();
			EventBus.getDefault().event(POWERUP_TAB_REMOVE, null);
		}
		powerupPresenter = null;
	}
}
