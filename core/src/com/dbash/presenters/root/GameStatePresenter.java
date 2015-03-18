package com.dbash.presenters.root;

import com.dbash.models.GameStats;
import com.dbash.models.IEventAction;
import com.dbash.models.IPresenterTurnState;
import com.dbash.models.TurnProcessor;
import com.dbash.platform.UIDepend;
import com.dbash.util.EventBus;

public class GameStatePresenter {

	EventBus eventBus;
	UIDepend gui;
	GameOverOverlayPresenter gameOverPresenter;
	NewGameOverlayPresenter newGamePresenter;
	
	public GameStatePresenter(UIDepend gui) {
		this.eventBus = EventBus.getDefault();
		this.gui = gui;
		init();
	}
	
	private void init() {
		
		eventBus.onEvent(TurnProcessor.NO_SAVED_GAME_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				removePresenters();
				newGamePresenter = new NewGameOverlayPresenter((IPresenterTurnState) param);
				gui.overlayQueues.addSequential(newGamePresenter);
			}
		});
		
		eventBus.onEvent(TurnProcessor.NEW_GAME_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				removePresenters();
				newGamePresenter = new NewGameOverlayPresenter((IPresenterTurnState) param);
				gui.overlayQueues.addSequential(newGamePresenter);
				
			}
		});
		
		eventBus.onEvent(TurnProcessor.START_GAME_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				removePresenters();
			}
		});
		
		eventBus.onEvent(TurnProcessor.GAME_OVER_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				removePresenters();
				gameOverPresenter = new GameOverOverlayPresenter((GameStats) param);
				gui.overlayQueues.addSequential(gameOverPresenter);
			}
		});
	}
	
	private void removePresenters() {
		if (gameOverPresenter != null) {
			gui.overlayQueues.remove(gameOverPresenter);
		}
		if (newGamePresenter != null) {
			gui.overlayQueues.remove(newGamePresenter);
		}
	}
}
