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
	
	EventBus eventBus;
	UIDepend gui;
	int moves = 0;
	
	public TutorialPresenter(UIDepend gui) {
		this.eventBus = EventBus.getDefault();
		this.gui = gui;
		init();
	}
	
	private void init() {
		final TutorialPresenter tutorialPresenter = this;
		
		eventBus.onEvent(DROPPING_IN_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("DROPPING_IN_EVENT");
				removePresenters();
				OverlayPresenter newGamePresenter = new DroppingInPresenter();
				gui.overlayQueues.addParallel(newGamePresenter);
				eventBus.removeListener(DROPPING_IN_EVENT, tutorialPresenter);
			}
		});
		
		eventBus.onEvent(MOVE, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("MOVE");
				moves++;
				switch (moves) {
					case 4:
						OverlayPresenter passing = new PassingPresenter();
						gui.overlayQueues.addParallel(passing);
						break;
					case 7:
						OverlayPresenter solo = new SoloPresenter();
						gui.overlayQueues.addParallel(solo);
						break;
					case 11:
						OverlayPresenter leader = new LeaderModePresenter();
						gui.overlayQueues.addParallel(leader);
						break;
					default:
						break;
				}
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
	
	private void removePresenters() {
		gui.overlayQueues.removeAll();
	}
}