package com.dbash.presenters.root.tutorial;

import com.dbash.models.IEventAction;
import com.dbash.models.IPresenterTurnState;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.root.OverlayPresenter;
import com.dbash.util.EventBus;
import com.dbash.util.L;

public class TutorialPresenter {
	public static final boolean LOG = true && L.DEBUG;
	
	public static final String DROPPING_IN_EVENT = "DROPPING_IN_EVENT";
//	public static final String START_TUTORIAL_EVENT = "starttutorialgame";
//	public static final String START_GAME_EVENT = "startgame";
//	public static final String NEW_GAME_EVENT = "newgame";
//	public static final String NO_SAVED_GAME_EVENT = "nosavedgame";
//	public static final String TUTORIAL_OVER_EVENT = "tutorialover";
	
	EventBus eventBus;
	UIDepend gui;
	
	public TutorialPresenter(UIDepend gui) {
		this.eventBus = EventBus.getDefault();
		this.gui = gui;
		init();
	}
	
	private void init() {
		
		eventBus.onEvent(DROPPING_IN_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("DROPPING_IN_EVENT");
				removePresenters();
				OverlayPresenter newGamePresenter = new DroppingInPresenter();
				gui.overlayQueues.addSequential(newGamePresenter);
			}
		});
//		
//		eventBus.onEvent(TurnProcessor.NEW_GAME_EVENT, this, new IEventAction() {
//			@Override
//			public void action(Object param) {
//				if (LOG) L.log("NEW_GAME_EVENT");
//				removePresenters();
//				OverlayPresenter newGamePresenter = new NewGameOverlayPresenter((IPresenterTurnState) param, false);
//				gui.overlayQueues.addSequential(newGamePresenter);
//				
//			}
//		});
//		
//		eventBus.onEvent(TurnProcessor.START_GAME_EVENT, this, new IEventAction() {
//			@Override
//			public void action(Object param) {
//				if (LOG) L.log("START_GAME_EVENT");
//				removePresenters();
//			}
//		});
//		
//		eventBus.onEvent(TurnProcessor.GAME_OVER_EVENT, this, new IEventAction() {
//			@Override
//			public void action(Object param) {
//				if (LOG) L.log("GAME_OVER_EVENT");
//				removePresenters();
//				OverlayPresenter gameOverPresenter = new GameOverOverlayPresenter((GameStats) param);
//				gui.overlayQueues.addSequential(gameOverPresenter);
//			}
//		});
	}
	
	private void removePresenters() {
		gui.overlayQueues.removeAll();
	}
}