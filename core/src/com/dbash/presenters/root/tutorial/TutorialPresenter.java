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
	
	public static final String ON_ENTRY_EVENT = "ON_ENTRY_EVENT";
	public static final String DROPPING_IN_EVENT = "DROPPING_IN_EVENT";
	public static final String MOVE_EVENT = "MOVE";
	public static final String ANIM_LEADER_BUTTON_ON_EVENT = "ANIM_LEADER_BUTTON_EVENT";
	public static final String ANIM_LEADER_BUTTON_OFF_EVENT = "ANIM_LEADER_BUTTON_OFF_EVENT";
	public static final String LEADER_ON_EVENT = "LEADER_ON_EVENT";
	public static final String ANIM_SOLO_BUTTON_ON_EVENT = "ANIM_SOLO_BUTTON_ON_EVENT";
	public static final String ANIM_SOLO_BUTTON_OFF_EVENT = "ANIM_SOLO_BUTTON_OFF_EVENT";
	public static final String SOLO_ON_EVENT = "SOLO_ON_EVENT";
	public static final String ANIM_PASS_BUTTON_ON_EVENT = "ANIM_PASS_BUTTON_ON_EVENT";
	public static final String ANIM_PASS_BUTTON_OFF_EVENT = "ANIM_PASS_BUTTON_OFF_EVENT";
	public static final String PASS_ON_EVENT = "PASS_ON_EVENT";
	public static final String ANIM_STEALTH_BUTTON_ON_EVENT = "ANIM_STEALTH_BUTTON_ON_EVENT";
	public static final String ANIM_STEALTH_BUTTON_OFF_EVENT = "ANIM_STEALTH_BUTTON_OFF_EVENT";
	public static final String STEALTH_ON_EVENT = "STEALTH_ON_EVENT";
	public static final String EYE_TAB_BUTTON_ON_EVENT = "EYE_TAB_BUTTON_ON_EVENT";
	public static final String EYE_TAB_BUTTON_OFF_EVENT = "EYE_TAB_BUTTON_OFF_EVENT";
	public static final String EYE_TAB_ON_EVENT = "EYE_TAB_ON_EVENT";
	public static final String ITEM_TAB_BUTTON_ON_EVENT = "ITEM_TAB_BUTTON_ON_EVENT";
	public static final String ITEM_TAB_BUTTON_OFF_EVENT = "ITEM_TAB_BUTTON_OFF_EVENT";
	public static final String ITEM_TAB_ON_EVENT = "ITEM_TAB_ON_EVENT";
	public static final String ABILITY_TAB_BUTTON_ON_EVENT = "ABILITY_TAB_BUTTON_ON_EVENT";
	public static final String ABILITY_TAB_BUTTON_OFF_EVENT = "ABILITY_TAB_BUTTON_OFF_EVENT";
	public static final String ABILITY_TAB_ON_EVENT = "ABILITY_TAB_ON_EVENT";
	public static final String EFFECT_TAB_BUTTON_ON_EVENT = "EFFECT_TAB_BUTTON_ON_EVENT";
	public static final String EFFECT_TAB_BUTTON_OFF_EVENT = "EFFECT_TAB_BUTTON_OFF_EVENT";
	public static final String EFFECT_TAB_ON_EVENT = "EFFECT_TAB_ON_EVENT";
	
	public static final String ALL_BUTTON_ANIMS_OFF = "ALL_BUTTON_ANIMS_OFF";
	
	public static final String ABILITY_USED_EVENT = "ABILITY_USED_EVENT";
	public static final String ITEM_PICKED_UP_EVENT = "ITEM_PICKED_UP";
	public static final String CHARACTER_IN_LOS_EVENT = "CHARACTER_IN_LOS";
	public static final String CREATURE_DIED_EVENT = "CREATURE_DIED_EVENT";
	public static final String TILE_CLICKED_EVENT = "TILE_CLICKED_EVENT";
	public static final String SET_INITIAL_STATE = "SET_INITIAL_STATE";
	
	public enum State {
		INITIAL_STATE,
		DROPPING_IN_STATE,
		PASSING_STATE,
		SOLO_STATE,
		LEADER_STATE,
		FIGHTING_STATE,
		PICKUP_STATE,
		ITEM_STATE,
		ABILITY_STATE,
		EFFECT_STATE,
		RANGED_STATE
	}
	
	EventBus eventBus;
	UIDepend gui;
	int moves = 0;
	int passes = 0;
	private IPresenterTurnState turnProcessor;
	private State state = State.INITIAL_STATE;
	
	public TutorialPresenter(PresenterDepend model, UIDepend gui) {
		this.eventBus = EventBus.getDefault();
		this.gui = gui;
		this.turnProcessor = model.presenterTurnState;
		init();
	}

	private void initialState(String event, Object param) {
		if (event.equals(SET_INITIAL_STATE)) {
			newState((State) param, null);
		} else if (event.equals(DROPPING_IN_EVENT)) {
			newState(State.DROPPING_IN_STATE, param);
		}
	}
	
	private void droppingInState(String event, Object param) {
		if (event.equals(ON_ENTRY_EVENT)) {
			removePresenters();
			popPresenterPar(new DroppingInPresenter());
		} else if (event.equals(MOVE_EVENT)) {
			if (moves == 4) {
				newState(State.PASSING_STATE, param);
			}
		}
	}
	
	private void passingState(String event, Object param) {
		if (event.equals(ON_ENTRY_EVENT)) {
			popPresenterPar(new PassingPresenter());
			passes = 0;
		} else if (event.equals(PASS_ON_EVENT)) {
			passes++;
			if (passes == 4) {
				newState(State.SOLO_STATE, param);
			}
		}
	}
	
	private void soloState(String event, Object param) {
		if (event.equals(ON_ENTRY_EVENT)) {
			popPresenterPar(new SoloPresenter());
		} else if (event.equals(MOVE_EVENT)) {
			if (moves == 4) {
				newState(State.LEADER_STATE, param);
			}
		}
	}
	
	private void leaderState(String event, Object param) {
		if (event.equals(ON_ENTRY_EVENT)) {
			if (turnProcessor.getSoloStatus()) {
				turnProcessor.soloSelected();
			}
			popPresenterPar(new LeaderModePresenter());
		} else if (event.equals(CHARACTER_IN_LOS_EVENT)) {
			newState(State.FIGHTING_STATE, param);
		}
	}
	
	private void fightingState(String event, Object param) {
		if (event.equals(ON_ENTRY_EVENT)) {
			popPresenterPar(new FightingPresenter());
		} else if (event.equals(CREATURE_DIED_EVENT)) {
			newState(State.PICKUP_STATE, param);
		}
	}
	
	int tilesClicked;
	private void pickupState(String event, Object param) {
		if (event.equals(ON_ENTRY_EVENT)) {
			tilesClicked = 0;
			popPresenterPar(new PickupPresenter());
		} else if (event.equals(TILE_CLICKED_EVENT)) {
			tilesClicked++;
			if (tilesClicked == 4) {
				newState(State.ITEM_STATE, param);
			}
		}
	}
	
	int itemsPickedUp = 0;
	private void itemState(String event, Object param) {
		if (event.equals(ON_ENTRY_EVENT)) {
			itemsPickedUp = 0;
			popPresenterPar(new ItemPresenter());
		} 
		else if (event.equals(ITEM_PICKED_UP_EVENT)) {
			itemsPickedUp++;
			if (itemsPickedUp == 4) {
				newState(State.ABILITY_STATE, param);
			}
		}
	}
	
	int abilitiesUsed = 0;
	private void abilityState(String event, Object param) {
		if (event.equals(ON_ENTRY_EVENT)) {
			abilitiesUsed = 0;
			popPresenterPar(new AbilityPresenter());
		} 
		else if (event.equals(ABILITY_USED_EVENT)) {
			abilitiesUsed++;
			if (abilitiesUsed == 4) {
				newState(State.EFFECT_STATE, param);
			}
		}
	}
	
	private void effectState(String event, Object param) {
		if (event.equals(ON_ENTRY_EVENT)) {
			popPresenterPar(new EffectPresenter());
		} else if (event.equals(CHARACTER_IN_LOS_EVENT)) {
			newState(State.RANGED_STATE, param);
		}
	}
	
	private void rangedState(String event, Object param) {
		if (event.equals(ON_ENTRY_EVENT)) {
			popPresenterPar(new RangedPresenter());
		} else if (event.equals(CHARACTER_IN_LOS_EVENT)) {
		//	newState(State.RANGED_STATE, param);
		}
	}
	
	private void stateEvent(String event, Object param) {
		switch (state) {
			case INITIAL_STATE:
				initialState(event, param);
				break;
			case DROPPING_IN_STATE:
				droppingInState(event, param);
				break;
			case PASSING_STATE:
				passingState(event, param);
				break;
			case SOLO_STATE:
				soloState(event, param);
				break;
			case LEADER_STATE:
				leaderState(event, param);
				break;
			case FIGHTING_STATE:
				fightingState(event, param);
				break;
			case PICKUP_STATE:
				pickupState(event, param);
				break;
			case ITEM_STATE:
				itemState(event, param);
				break;
			case ABILITY_STATE:
				abilityState(event, param);
				break;
			case EFFECT_STATE:
				effectState(event, param);
				break;
			case RANGED_STATE:
				rangedState(event, param);
				break;
			default:
				break;
		}
	}
	
	private void init() {
		eventBus.onEvent(SET_INITIAL_STATE, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("SET_INITIAL_STATE");
				stateEvent(SET_INITIAL_STATE, param);
			}
		});
		
		eventBus.onEvent(DROPPING_IN_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("DROPPING_IN_EVENT");
				stateEvent(DROPPING_IN_EVENT, param);
			}
		});
		
		eventBus.onEvent(MOVE_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("MOVE_EVENT");
				moves++;
				stateEvent(MOVE_EVENT, param);
			}
		});
		
		eventBus.onEvent(PASS_ON_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("PASS_ON_EVENT");
				stateEvent(PASS_ON_EVENT, param);
			}
		});
		
		eventBus.onEvent(CHARACTER_IN_LOS_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("CHARACTER_IN_LOS_EVENT");
				stateEvent(CHARACTER_IN_LOS_EVENT, param);
			}
		});
		
		eventBus.onEvent(CREATURE_DIED_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("CREATURE_DIED_EVENT");
				stateEvent(CREATURE_DIED_EVENT, param);
			}
		});
		
		eventBus.onEvent(TILE_CLICKED_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("TILE_CLICKED_EVENT");
				stateEvent(TILE_CLICKED_EVENT, param);
			}
		});
		
		eventBus.onEvent(ITEM_PICKED_UP_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("ITEM_PICKED_UP_EVENT");
				stateEvent(ITEM_PICKED_UP_EVENT, param);
			}
		});
		
		eventBus.onEvent(ABILITY_USED_EVENT, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("ABILITY_USED_EVENT");
				stateEvent(ABILITY_USED_EVENT, param);
			}
		});
	}
	
	private void newState(State state, Object param) {
		moves = 0;
		this.state = state;
		stateEvent(ON_ENTRY_EVENT, param);
		turnProcessor.saveGame(state);
	}
	
	private void popPresenterPar(OverlayPresenter overlayPresenter) {
		gui.overlayQueues.addParallel(overlayPresenter);
	}
	
	private void popPresenterSeq(OverlayPresenter overlayPresenter) {
		gui.overlayQueues.addSequential(overlayPresenter);
	}
	
	private void removePresenters() {
		gui.overlayQueues.removeAll();
	}
	
	public void onDestroy() {
		EventBus.getDefault().removeAll(this);
	}
}