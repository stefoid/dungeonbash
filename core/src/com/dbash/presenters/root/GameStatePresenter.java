package com.dbash.presenters.root;

import com.dbash.models.GameStats;
import com.dbash.models.IEventAction;
import com.dbash.models.TurnProcessor;
import com.dbash.platform.UIDepend;
import com.dbash.util.EventBus;

public class GameStatePresenter {

	EventBus eventBus;
	UIDepend gui;
	GameOverOverlayPresenter gameOverPresenter;
	
	public GameStatePresenter(UIDepend gui) {
		this.eventBus = EventBus.getDefault();
		this.gui = gui;
		init();
	}
	
	private void init() {
		eventBus.onEvent(TurnProcessor.GAME_OVER_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				gameOverPresenter = new GameOverOverlayPresenter((GameStats) param);
				gui.overlayQueues.addSequential(gameOverPresenter);
			}
		});
		
		eventBus.onEvent(TurnProcessor.NEW_GAME_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (gameOverPresenter != null) {
					gui.overlayQueues.remove(gameOverPresenter);
				}
			}
		});
		
//		eventBus.onEvent(TurnProcessor.NEW_GAME_EVENT, this, new IEventAction() {
//			@Override
//			public void action(Object param) {
//				if (gameOverPresenter != null) {
//					gui.overlayQueues.remove(gameOverPresenter);
//				}
//			}
//		});
//		
//		eventBus.onEvent(TurnProcessor.NEW_GAME_EVENT, this, new IEventAction() {
//			@Override
//			public void action(Object param) {
//				if (gameOverPresenter != null) {
//					gui.overlayQueues.remove(gameOverPresenter);
//				}
//			}
//		});
	}
}
