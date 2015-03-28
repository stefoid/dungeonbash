package com.dbash.presenters.root.tutorial;

import com.dbash.platform.UIDepend;
import com.dbash.util.EventBus;
import com.dbash.util.L;

public class TutorialPresenter {

	public static final boolean LOG = true && L.DEBUG;
	
	EventBus eventBus;
	UIDepend gui;
	
	public TutorialPresenter(UIDepend gui) {
		this.eventBus = EventBus.getDefault();
		this.gui = gui;
		init();
	}
	
	private void init() {
		
//		eventBus.onEvent(TurnProcessor.NO_SAVED_GAME_EVENT, this, new IEventAction() {
//			@Override
//			public void action(Object param) {
//				if (LOG) L.log("NO_SAVED_GAME_EVENT");
//				removePresenters();
//				OverlayPresenter newGamePresenter = new NewGameOverlayPresenter((IPresenterTurnState) param, true);
//				gui.overlayQueues.addSequential(newGamePresenter);
//			}
//		});
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