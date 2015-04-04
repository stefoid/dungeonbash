package com.dbash.presenters.root.tutorial;

import com.dbash.models.IEventAction;
import com.dbash.models.IPresenterTurnState;
import com.dbash.models.PresenterDepend;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.root.OverlayPresenter;
import com.dbash.util.EventBus;
import com.dbash.util.L;

public class TutorialPresenter {
	public static final boolean LOG = true && L.DEBUG;
	
	public static final String DROPPING_IN_EVENT = "DROPPING_IN_EVENT";
	public static final String MOVE = "MOVE";
	public static final String ANIM_LEADER_BUTTON_ON_EVENT = "ANIM_LEADER_BUTTON_EVENT";
	public static final String ANIM_LEADER_BUTTON_OFF_EVENT = "ANIM_LEADER_BUTTON_OFF_EVENT";
	public static final String ANIM_SOLO_BUTTON_ON_EVENT = "ANIM_SOLO_BUTTON_ON_EVENT";
	public static final String ANIM_SOLO_BUTTON_OFF_EVENT = "ANIM_SOLO_BUTTON_OFF_EVENT";
	public static final String ANIM_PASS_BUTTON_ON_EVENT = "ANIM_PASS_BUTTON_ON_EVENT";
	public static final String ANIM_PASS_BUTTON_OFF_EVENT = "ANIM_PASS_BUTTON_OFF_EVENT";
	public static final String ANIM_STEALTH_BUTTON_ON_EVENT = "ANIM_STEALTH_BUTTON_ON_EVENT";
	public static final String ANIM_STEALTH_BUTTON_OFF_EVENT = "ANIM_STEALTH_BUTTON_OFF_EVENT";
	public static final String LEADER_ON_EVENT = "LEADER_ON_EVENT";
	public static final String PASS_ON_EVENT = "PASS_ON_EVENT";
	public static final String STEALTH_ON_EVENT = "STEALTH_ON_EVENT";
	public static final String SOLO_ON_EVENT = "SOLO_ON_EVENT";
	
	public static final String SET_MOVE_COUNT = "SET_MOVE_COUNT";
	
	public static final int MOVE_TURNS = 4;
	public static final int SOLO_TURNS = 8;
	public static final int LEADER_TURNS = 12;
	
	EventBus eventBus;
	UIDepend gui;
	int moves = 0;
	private IPresenterTurnState turnProcessor;
	
	public TutorialPresenter(PresenterDepend model, UIDepend gui) {
		this.eventBus = EventBus.getDefault();
		this.gui = gui;
		this.turnProcessor = model.presenterTurnState;
		init();
	}
	
	private void init() {
		final TutorialPresenter tutorialPresenter = this;
		
		eventBus.onEvent(SET_MOVE_COUNT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("SET_MOVE_COUNT");
				moves = (Integer) param;
			}
		});
		
		eventBus.onEvent(DROPPING_IN_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("DROPPING_IN_EVENT");
				removePresenters();
				popPresenterPar(DROPPING_IN_EVENT, new DroppingInPresenter());
				eventBus.removeListener(DROPPING_IN_EVENT, tutorialPresenter);
			}
		});
		
		eventBus.onEvent(MOVE, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("MOVE");
				
				switch (moves) {
					case MOVE_TURNS:
						popPresenterPar(MOVE, new PassingPresenter());
						break;
					case SOLO_TURNS:
						popPresenterPar(MOVE, new SoloPresenter());
						break;
					case LEADER_TURNS:
						popPresenterPar(MOVE, new LeaderModePresenter());
						break;
					default:
						break;
				}
				
				moves++;
			}
		});

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
	
	private void popPresenterPar(String tutorialEvent,OverlayPresenter overlayPresenter) {
		gui.overlayQueues.addParallel(overlayPresenter);
		turnProcessor.saveGame(tutorialEvent, moves);
	}
	
	private void popPresenterSeq(String tutorialEvent, OverlayPresenter overlayPresenter) {
		gui.overlayQueues.addSequential(overlayPresenter);
		turnProcessor.saveGame(tutorialEvent, moves);
	}
	
	private void removePresenters() {
		gui.overlayQueues.removeAll();
	}
	
	public void onDestroy() {
		EventBus.getDefault().removeAll(this);
	}
}