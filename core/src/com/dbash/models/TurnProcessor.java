package com.dbash.models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import com.dbash.models.Creature.StealthStatus;
import com.dbash.presenters.root.GameOverOverlayPresenter;
import com.dbash.presenters.root.GameStatePresenter;
import com.dbash.presenters.root.tutorial.TutorialPresenter;
import com.dbash.util.EventBus;
import com.dbash.util.L;
import com.dbash.util.SequenceNumber;
import com.me.dbash.Dbash;

@SuppressWarnings("unused")

public class TurnProcessor implements IPresenterTurnState {
	public static final boolean LOG = true && L.DEBUG;
	
	public static final int NUM_CHARS = 3;
	
	public static enum GameState {
		NO_SAVED_GAME,
		NEW_GAME,
		START_GAME,
		GAME_OVER
	};
	
	private IDungeonEvents dungeonEvents;
	private IDungeonQuery dungeonQuery;
	private IDungeonControl dungeon;
	private int level;
	private Creature currentCreature;
	private int creatureTurn;
	private Character currentLeader;
	private Character currentCharacter; // the character whose turn is now
										// active
	private UIInfoListenerBag currentCharacterListeners;
	private UIInfoListenerBag leaderStatusListeners;
	private UIInfoListenerBag soloStatusListeners;
	private LeaderStatus leaderStatus;
	private boolean pauseTurnProcessing;
	private boolean acceptInput;
	private Character nobody;
	private GameState gameState;
	private boolean usingEye;
	private boolean soloStatus;
	private boolean firstCharToDrop;
	public static final int NO_CURRENT_CREATURE = -1;
	public static final int INITIAL_EXP = 500;
	public static final int EXP_PER_LEVEL = 220;
	public GameStats gameStats;

	// Lists to maintain what is going on.
	// allCreatures is a list of every monster and every alive character. We
	// iterate over this list to give everything a chance to act.
	// allCharacters contains all the alive characters.
	// Characters are added to the map from the falling-in list, and leave the
	// dungeon by going to the falling-out list.
	// Thus when we go down a level, the new falling in becomes the old falling
	// out, and falling out is reset to empty.
	// We know when all characters have left the map when the size of falling
	// out is equal to the size of allCharacters.
	// We know when the game is over when the size of allCharacters = 0;
	public AllCreatures allCreatures;
	public Vector<Character> allCharacters;
	public Vector<Character> charactersFallingIn;
	public Vector<Character> charactersFallingOut;

	public TurnProcessor(IDungeonEvents dungeonEvents,
			IDungeonQuery dungeonQuery, IDungeonControl dungeon, Dbash dbash) {
		initOneTime(dungeonEvents, dungeonQuery, dungeon);
		setGameState(GameState.NO_SAVED_GAME);
		currentCharacter = nobody;
		leaderStatus = LeaderStatus.NONE;
		gameStats = new GameStats();
	}

	// We set up the lists for this level.
	private void startNewLevel(int level, boolean tutorial) {
		gameStats.newLevel(level);
		pauseTurnProcessing = false;
		acceptInput = false;
		
		if (tutorial) {
			dungeon.createTutorialLevel(this);
		} else {
			dungeon.createLevel(this, level);
		}
		
		setCurrentCharacter(nobody);
		firstCharToDrop = true;

		charactersFallingIn = charactersFallingOut;
		charactersFallingOut = new Vector<Character>();
		allCreatures = new AllCreatures();
		allCreatures.addAll(charactersFallingIn);
		allCreatures.addAll(dungeonQuery.getAllMonsters());
		addExperience(level * EXP_PER_LEVEL);
	}

	public void resume() {
		// pass dungeon list of characters on map
		Vector<Character> charactersOnMap = new Vector<Character>();
		charactersOnMap.addAll(allCharacters);
		for (Character c : charactersFallingIn) {
			charactersOnMap.remove(c);
		}
		for (Character c : charactersFallingOut) {
			charactersOnMap.remove(c);
		}

		dungeon.resume(this, level, allCreatures, charactersOnMap);

		currentCharacter.calculateHighlightAbility();
		setCurrentCharacter(currentCharacter);
		if (currentCharacter != nobody) {
			currentCharacter.changeToHighlighted();
			waitForPlayerInput();
		}

		processLeaderMode();
		
		if (getGameState() == GameState.NO_SAVED_GAME) {
			doNewGame();
		} else {
			sendGameStateEvent();
		}
		
		if (L.TUTORIAL_MODE) {
			startGame(getTutorialCharacters(), true);
		}

	}

	boolean lastPause = false;
	private boolean creatureMoved = false;
	// This will process any automatic actions, such as an entire monster turn
	// or any part of a character
	// turn that can proceed without further player input.
	// When player input is required, this call does nothing until mustWait is
	// set back to true by
	// one of the player inputs that will cause the current characters turn to
	// proceed, and then end.
	public void gameLogicLoop() {
		if (getGameState() != GameState.NO_SAVED_GAME) {
			if (creatureMoved) {
				creatureMoved = false;
				processLeaderMode();
				dungeonEvents.processCharacterStealth();
			}
			
			if (pauseTurnProcessing != lastPause) {
				// if (LOG)
				// Logger.log("PAUSED : "+lastPause+" >> "+pauseTurnProcessing);
				lastPause = pauseTurnProcessing;
			}

			if (pauseTurnProcessing == false) {
				processNextCreature();
			}
		}
	}
	
	// The TurnProcessor iterates over the entire list, giving each creature a
	// chance to act.
	// If there are falling characters, the first falling character in the queue
	// is a special case and is processed
	// at the start of each full list cycle.
	// The chance to act is based on the creatures speed. If it misses its
	// chance, its chance is increased for next turn, so
	// eventually, even the slowest creatures get a turn, although faster
	// creatures may have had several turns by then.
	// Falling into the level is not considered to be a Characters complete
	// action - when the character appears on the map
	// the player gets to have a turn.
	//
	// turn processor calls on those creatures who are able to have their turn,
	// to take their turn
	// if a creature doesnt have a turn, it keeps going
	// if they do have their turn, they call creature.endTurn() , which amongst
	// other things calls waitForAnimsToFinish()
	// at the end of it which sets the turn processor into a wait state, and
	// issues a dummy animation callback to dungeon
	// if the creature is a character that has to wait for player input, it
	// calls waitForPlayerInput() at that point
	private void processNextCreature() {
		if (creatureTurn >= allCreatures.size())
			creatureTurn = 0;

		if (allCreatures.size() == 0) {
			currentCreature = null;
			return; // I guess its possible to destroy all creatures on a level
					// with a burst.
		}

		// processing a single creature per draw cycle takes a long time to
		// process a lot of creatures because drawing takes some time.
		// so this loop will skip all stable, non-interesting monster turns in
		// one go
		do {
			// get the next creature
			currentCreature = (Creature) allCreatures.elementAt(creatureTurn);
			// increment counter for next time
			creatureTurn++;
			if (creatureTurn >= allCreatures.size())
				creatureTurn = 0;

			// if all characters are dead, return false or nothing will ever
			// happen.
			if (allCharacters.size() == 0) {
				break;
			}
		} while (currentCreature.canSkipTurn());

		// Dont call isReadyForTurn, until we have established that a falling
		// character can fall in.
		boolean isFalling = charactersFallingIn.contains(currentCreature);
		boolean canFallIn = false;

		// If it is a falling Character, is it the first falling character and
		// is the entrance free?
		if (isFalling) {
			if (currentCreature == charactersFallingIn.elementAt(0)
					&& dungeonQuery.isEntranceFree()) {
				canFallIn = true;
			} else {
				return; // Dont test isReadyForTurn, because this Character isnt
						// in a position to fall in yet.
			}
		}

		// OK, so now whatever it is can test to have a turn.
		if (currentCreature.isReadyForTurn()) {
			Character theChar;

			// at this point, we could be in solo mode, so some tests have to be
			// made.
			if (currentCreature instanceof Character) {
				theChar = (Character) currentCreature;
				dungeonEvents.currentCharacterHavingTurn((Character)currentCreature);
				if (needToSetSoloCharacter()) {
					theChar.setSolo(true);
				}

				// character cant fall in if we are in solo mode and they are
				// not the solo character.
				if (soloStatus == false || theChar.getSolo()) {
					// If a lucky falling character is ready and able to fall
					// in, then tell the dungeon about it.
					if (canFallIn) {
						int level = -1;
						if (firstCharToDrop) {
							level = this.level;
							firstCharToDrop = false;
						}
						charactersFallingIn.removeElement(currentCreature); // no longer falling
						currentCreature.stealthStatus = StealthStatus.HIDING;
						dungeonEvents.fallIntoLevel(SequenceNumber.getNext(),
								(Character) currentCreature, level); // when a character falls in, it get a chance to act (wait for player control)
					}
				}
			} else {
				dungeonEvents.currentCharacterHavingTurn(null);
			}

			currentCreature.processTurn();
			return;
		}

		return;
	}

	public boolean anyActiveFollowers() {
		for (Character c : allCharacters) {
			if (c.isActiveFollower()) {
				return true;
			}
		}

		return false;
	}

	// When the Character has ended a turn dependent on player input and/or
	// animation completeness
	// it calls this to get the GameLogicLoop back in action.
	public void characterEndsTurn(Creature character) {
		currentLeader = getCurrentLeader(); // a character has done something,
											// maybe a move, so has it resulted
											// in leader mode going off?
		character.endTurn();
		setCurrentCharacter(nobody);
	}

	// this is called to pause turn processing until all animations to do with
	// the current characters turn have finished playing.
	// We dont want this to happen if we are in leader mode because then the
	// followers will have to wait.
	// but in that situation, we still want input turned off until animations
	// are finished.
	public void waitForAnimsToFinish() {
		acceptInput = false;

		if (currentLeader != null) {
			pauseTurnProcessing = false; // to turn it off in case we were
											// waiting for player input previous
											// to this character
			// ending its turn
		} else {
			waitForAnim(null);// TODO
		}
	}

	// characters call this to pause turn processing until player has directed
	// character action.
	// turn processing will be turned on again by the callback employed by
	// waitForAnimsToFinish
	public void waitForPlayerInput() {
		if (LOG)
			L.log("waiting for player input");
		pauseTurnProcessing = true;
		dungeonEvents.waitingForAnimToComplete(SequenceNumber.getNext(),
				new IAnimListener() {
					public void animEvent() {
						acceptInput = true;
					}
				});
	}

	protected void waitForAnim(IAnimListener listener) {
		pauseTurnProcessing = true;
		acceptInput = false;
		final IAnimListener l = listener;
		dungeonEvents.waitingForAnimToComplete(SequenceNumber.getNext(),
				new IAnimListener() {
					public void animEvent() {
						pauseTurnProcessing = false;
						acceptInput = true;
						if (l != null) {
							l.animEvent();
						}
					}
				});
	}

	// So when the current character is changed, any presenters listening will
	// read the new character, update their
	// pat of the UI, and listen to that character for changes to its details
	// that are relevant for them.
	@Override
	public void setCurrentCharacter(Character character) {
		currentCharacter = character;
		currentCharacter.setCharacterisUsingEye(usingEye);
		currentCharacterListeners.alertListeners();
	}
	
	@Override
	public void setCurrentCharacterForNewGame(Character character) {
		currentCharacter = character;
		currentCharacterListeners.alertListeners();
	}

	// returns true if the character should drop its stuff, false otherwise.
	public boolean characterDied(Character character) {
		allCharacters.remove(character);
		allCreatures.remove(character);
		if (currentLeader == character) {
			currentLeader = null;
			clearLeaderMode();
		}

		if (character.getSolo()) {
			setSoloMode(false);
		}

		dungeonEvents.creatureDies(SequenceNumber.getCurrent() + 2, character,
				character);

		if (allCharacters.size() == 0) {
			// game over man, game over.
			dungeon.gameOver();
			setGameState(GameState.GAME_OVER);
			gameStats.delay = 2;
			EventBus.getDefault().event(GameStatePresenter.GAME_OVER_EVENT, gameStats);
		} else
		// What if that was the last character on this level?
		// If so, go to the next level
		if (allCharacters.size() == charactersFallingOut.size()) {
			dungeonEvents.waitingForAnimToComplete(SequenceNumber.getCurrent(),
					new IAnimListener() {
						public void animEvent() {
							level++;
							startNewLevel(level, false);
						}
					});
			return false;
		}
		return true;
	}

	public void monsterDied(Creature creature, Character observer) {
		allCreatures.remove(creature);
		dungeonEvents.creatureDies(SequenceNumber.getCurrent() + 2, observer,
				creature);
	}

	private boolean noDuplicate(List<Character> theChars, Character character) {
		for (Character c : theChars) {
			if (character.myId == c.myId) {
				return false;
			}
		}
		return true;
	}

	public List<Character> createRandomCharacters() {
		List<Character> theChars = new LinkedList<Character>();

		// The dungeon will fill in the position when the creature falls into
		// the level. This
		// is just a placeholder.
		DungeonPosition p = new DungeonPosition(0, 0); // will init to level 1.

		// First character must be humanoid
		boolean foundChar = false;
		while (foundChar == false) {
			Character c = Character.getNewCharacter(p, 1, dungeonEvents,
					dungeonQuery, this);
			if (c.isHumanid() && noDuplicate(theChars, c)) {
				theChars.add(c);
				foundChar = true;
			}
		}

		// 2nd character cant have hands
		foundChar = false;
		while (foundChar == false) {
			Character c = Character.getNewCharacter(p, 2, dungeonEvents,
					dungeonQuery, this);
			if (c.hasHands() == false && noDuplicate(theChars, c)) {
				theChars.add(c);
				foundChar = true;
			}
		}

		// 3rd character can be anything
		foundChar = false;
		while (foundChar == false) {
			Character c = Character.getNewCharacter(p, 3, dungeonEvents,
					dungeonQuery, this);
			if (noDuplicate(theChars, c)) {
				theChars.add(c);
				foundChar = true;
			}
		}

		if (L.TESTCHARS) {
			theChars.clear();
			p.level = 20;
			theChars.add(new Character(Creature.getIdForName(L.c1), p, 1, dungeonEvents, dungeonQuery, this));
			theChars.add(new Character(Creature.getIdForName(L.c2), p, 2, dungeonEvents, dungeonQuery, this));
			theChars.add(new Character(Creature.getIdForName(L.c3), p, 3, dungeonEvents, dungeonQuery, this));
			Creature c = theChars.get(0);
			c.addAbility(new Ability(Ability.getIdForName("wand of slow"), null, 20, dungeonEvents, dungeonQuery), null);
			c.addAbility(new Ability(Ability.getIdForName("amulet of wizardy"), null, 20, dungeonEvents, dungeonQuery), null);
			c.addAbility(new Ability(Ability.getIdForName("bow"), null, 20, dungeonEvents, dungeonQuery), null);c.addAbility(new Ability(Ability.getIdForName("wand of smiting"), null, 20, dungeonEvents, dungeonQuery), null);
			c.addAbility(new Ability(Ability.getIdForName("wand of sun flare"), null, 20, dungeonEvents, dungeonQuery), null);
		
		}
		// c.addAbility(new Ability(69, null, 20, dungeonEvents, dungeonQuery));
		// c.addAbility(new Ability(143, null, 20, dungeonEvents,
		// dungeonQuery));
		// c.addAbility(new Ability(112, null, 20, dungeonEvents,
		// dungeonQuery));
		// c.addAbility(new Ability(105, null, 20, dungeonEvents,
		// dungeonQuery));
		// c.addAbility(new Ability(58, null, 20, dungeonEvents, dungeonQuery));
		// // 149 + 75
		// // test
		//
		// for (Character ch : theChars) {
		// ch.health = 1;
		// ch.maximumHealth = 1;
		// }

		return theChars;
	}

	// IPresenterTurn State interface
	@Override
	public void onChangeToCurrentCharacter(UIInfoListener listener) {
		currentCharacterListeners.add(listener);
	}

	@Override
	public Character getCurrentCharacter() {
		return currentCharacter;
	}

	@Override
	/**
	 * If accept input is off, only return the currentLeader if we are travelling to a waypoint,
	 * so that we can update the waypoint.
	 */
	public Character getCharacterForTouchEvents() {
		if (acceptInput) {
			return currentCharacter;
		} else {
			if (currentLeader != null
					&& currentLeader.isThereAnAutomaticLeaderTarget()) {
				return currentLeader;
			} else {
				return nobody;
			}
		}
	}

	public void addExperience(int exp) {
		for (Character character : allCharacters) {
			character.addExperience(exp, false);
		}
		gameStats.xpGiven(exp);
	}
	
	public void addInitialExperience() {
		for (Character character : allCharacters) {
			character.addExperience(INITIAL_EXP, true);
		}
		gameStats.xpGiven(INITIAL_EXP);
	}

	private void setSoloMode(boolean solo) {

		soloStatus = solo;

		for (Character character : allCharacters) {
			character.setSolo(false);
		}

		// Works out whether leader mode is possible.
		currentLeader = getCurrentLeader();

		if (solo == true) {
			if (currentLeader != null) {
				currentLeader.setSolo(true);
			} else {
				if (acceptInput && currentCreature instanceof Character) {
					currentCharacter.setSolo(true);
					if (leaderStatus != LeaderStatus.LEADER_DISABLED) {
						leaderModeToggleSelected();
					}
				}
			}
		} else {
			clearLeaderMode();
		}
		soloStatusListeners.alertListeners();
	}

	/**
	 * returns true if solo status is set, but no character is currently marked
	 * as the solo character.
	 */
	private boolean needToSetSoloCharacter() {
		if (soloStatus) {
			for (Character character : allCharacters) {
				if (character.getSolo()) {
					return false;
				}
			}
			return true;
		}

		return false;
	}

	/**
	 * Clear all the characters solo status, then set the solo status for
	 * TPaccordingly. When a character is about to have a turn, if
	 * 'needToSetSoloCharacter' returns true, it will be set at that time. The
	 * only exception is if the there is a current leader, inwhich case that
	 * character is the solo immeidately.
	 */
	@Override
	public void soloSelected() {
		if (!soloStatus) {
			EventBus.getDefault().event(TutorialPresenter.SOLO_ON_EVENT, null);
		}
		
		if (gameState != GameState.START_GAME) {
			return;
		}
		setSoloMode(!soloStatus);
	}
	
	@Override
	public void leaderModeToggleSelected() {
		if (currentLeader == null) {
			if (currentCharacter.isPlayerCharacter()) {
				currentLeader = currentCharacter;
				leaderStatus = LeaderStatus.HAVE_LEADER;
				if (LOG) L.log("current leader is " + currentLeader.creature.name);
				EventBus.getDefault().event(TutorialPresenter.LEADER_ON_EVENT, null);
			}
		} else {
			currentLeader.leaderModeCleared();
			currentLeader = null;
			leaderStatus = LeaderStatus.NO_LEADER;
			if (LOG) L.log("no current leader");
		}

		leaderStatusListeners.alertListeners();
	}

	@Override
	public void stealthSelected() {
		Character character = getCurrentCharacter();
		
		if (character != null && character.getStealthStatus() == Creature.StealthStatus.HIDING_POSSIBLE) {
			EventBus.getDefault().event(TutorialPresenter.STEALTH_ON_EVENT, null);
		}
		
		if (gameState != GameState.START_GAME) {
			return;
		}
		
		if (character != null) {
			character.stealthToggleSelected();
		}
	}
	
	@Override
	public void passTurnSelected() {
		if (LOG) L.log("Pass turn pressed");
		EventBus.getDefault().event(TutorialPresenter.PASS_ON_EVENT, null);
		EventBus.getDefault().event(TutorialPresenter.MOVE, null);
		
		if (gameState != GameState.START_GAME) {
			return;
		}
		currentCharacter.defending();
	}
	
	@Override
	public void stairDescendSelected() {
		EventBus.getDefault().event(TutorialPresenter.LEADER_ON_EVENT, null);
		if (gameState != GameState.START_GAME) {
			return;
		}
		if (acceptInput == false || currentCharacter == nobody) { // in case pounding n th descent key cheap hack
			return;
		}

		// clear that characters solo, but dont clear solo mode if its been on.
		if (currentCharacter.getSolo()) {
			currentCharacter.setSolo(false);
		}

		// adjust the lists
		allCreatures.remove(currentCharacter);
		charactersFallingOut.add(currentCharacter);

		// was that character the leader?
		if (currentLeader == currentCharacter) {
			clearLeaderMode();
		}

		acceptInput = false;
		dungeonEvents.goDownStairs(SequenceNumber.getNext(), currentCharacter,
				new IAnimListener() {
					// was that the last character to descend? If so, go to the
					// next level
					public void animEvent() {
						acceptInput = true;
						if (allCharacters.size() == charactersFallingOut.size()) {
							level++;
							startNewLevel(level, false);
						}
					}
				});

		characterEndsTurn(currentCharacter);
	}
	
	@Override
	public boolean itemPickupSelected(Character character, Ability ability) {
		if (gameState != GameState.START_GAME) {
			return false;
		}
		
		return character.itemPickupSelected(ability);
	}
	
	@Override
	public boolean itemDropSelected() {
		if (gameState != GameState.START_GAME) {
			return false;
		} else {
			return true;
		}
	}
	
	@Override
	public boolean abilitySelected(Character character, Ability ability) {
		if (gameState != GameState.START_GAME) {
			return false;
		}
		
		return character.abilitySelected(ability);
	}
	
	// When a character is having a turn, they will ask this, initiating a
	// leadership validity check.
	public Character getCurrentLeader() {
//		if (currentLeader == null) {
//			leaderStatus = LeaderStatus.NO_LEADER;
//		} else {
//			leaderStatus = LeaderStatus.HAVE_LEADER;
//		}
		
//		Character sawMonster = dungeonQuery.leaderModeOK();
//		if (sawMonster != null) {
//
//			leaderStatus = LeaderStatus.LEADER_DISABLED;
//			if (currentLeader != null) {
//				savedLeader = currentLeader;
//				currentLeader.leaderStatusOff();
//				currentLeader = null;
//			}
//		}
//
//		leaderStatusListeners.alertListeners();

		return currentLeader;
	}

	/**
	 * if Leader mode is on, this turns it off, either to disabled or enabled
	 * but off
	 */
	public void clearLeaderMode() {
		currentLeader = getCurrentLeader();
		if (currentLeader != null) {
			leaderModeToggleSelected();
		}
	}

	@Override
	public void onChangeToLeaderStatus(UIInfoListener listener) {
		leaderStatusListeners.add(listener);
	}

	@Override
	public LeaderStatus getLeaderStatus() {
		return leaderStatus;
	}

	@Override
	public void onChangeToSoloStatus(UIInfoListener listener) {
		soloStatusListeners.add(listener);
	}

	@Override
	public boolean getSoloStatus() {
		return soloStatus;
	}

	public GameState getGameState() {
		return gameState;
	}
	
	private void sendGameStateEvent() {
		String event = GameStatePresenter.NO_SAVED_GAME_EVENT;
		Object param = this;
		switch (gameState) {
			case NO_SAVED_GAME:
				event = GameStatePresenter.NO_SAVED_GAME_EVENT;
				break;
			case NEW_GAME:
				event = GameStatePresenter.NEW_GAME_EVENT;
				break;
			case START_GAME:
				event = GameStatePresenter.START_GAME_EVENT;
				break;
			case GAME_OVER:
				event = GameStatePresenter.GAME_OVER_EVENT;
				param = gameStats;
				default:
					break;
		}
		
		
		EventBus.getDefault().event(event, param);
	}

	public void setGameState(GameState gameState) {
		this.gameState = gameState;
	}

	@Override
	public void usingEye(boolean usingEye) {
		this.usingEye = usingEye;
		currentCharacter.setCharacterisUsingEye(usingEye);
	}

	private void initOneTime(IDungeonEvents dungeonEvents,
			IDungeonQuery dungeonQuery, IDungeonControl dungeon) {
		// Do the one-time stuff.
		Creature.initializeData();
		currentCharacterListeners = new UIInfoListenerBag();
		leaderStatusListeners = new UIInfoListenerBag();
		soloStatusListeners = new UIInfoListenerBag();
		nobody = new NoCharacter();
		gameState = GameState.NO_SAVED_GAME;
		usingEye = false;
		acceptInput = false;
		soloStatus = false;
		this.dungeonEvents = dungeonEvents;
		this.dungeonQuery = dungeonQuery;
		this.dungeon = dungeon;
	}

	public TurnProcessor() {

	}

	public void load(ObjectInputStream in, IDungeonEvents dungeonEvents,
			IDungeonQuery dungeonQuery, IDungeonControl dungeon,
			AllCreatures allCreatures) throws IOException,
			ClassNotFoundException {

		allCharacters = new Vector<Character>();
		charactersFallingIn = new Vector<Character>();
		charactersFallingOut = new Vector<Character>();
		this.allCreatures = allCreatures;

		gameStats = new GameStats(in);
		setGameState((GameState) in.readObject());

		// start reading saved game
		level = in.readInt();

		// read all Characters into their array for latter reference.
		int chars = in.readInt();
		for (int i = 0; i < chars; i++) {
			int uId = in.readInt();
			Character c = (Character) allCreatures.getCreatureByUniqueId(uId);
			if (c != null) {
				allCharacters.add(c);
			}
		}

		// current creatures whose turn it is, and also currentCharacter
		int currentCreatureId = in.readInt();
		if (currentCreatureId == NO_CURRENT_CREATURE) {
			currentCreature = null;
		} else {
			currentCreature = allCreatures
					.getCreatureByUniqueId(currentCreatureId);
			if (currentCreature instanceof Character) {
				currentCharacter = (Character) currentCreature;
			} else {
				currentCharacter = nobody;
			}
			creatureTurn = allCreatures.getIndex(currentCreature);
		}

		// read all Characters falling in
		int fallIn = in.readInt();
		for (int i = 0; i < fallIn; i++) {
			int uId = in.readInt();
			Character c = (Character) allCreatures.getCreatureByUniqueId(uId);
			charactersFallingIn.add(c);
		}

		// read all Characters falling out
		int fallOut = in.readInt();
		for (int i = 0; i < fallOut; i++) {
			// Creature.CreatureType t = (CreatureType) in.readObject(); // we
			// know its a character, so read that enumeration first
			Character c = new Character(in, dungeonEvents, dungeonQuery, this);
			charactersFallingOut.add(c);
			allCharacters.add(c);
		}

		// leader Status and current Leader.
		leaderStatus = (LeaderStatus) in.readObject();
		int leaaderId = in.readInt();
		if (leaderStatus == LeaderStatus.HAVE_LEADER) {
			currentLeader = (Character) allCreatures
					.getCreatureByUniqueId(leaaderId);
		} else {
			currentLeader = null;
		}
	}

	public void persist(ObjectOutputStream out) throws IOException {
		gameStats.persist(out);
		
		if (gameState == GameState.NEW_GAME) {
			if (LOG) L.log("was NEW_GAME");
			gameState = previousGameState;  // dont restart with new game on. 
		}
		
		if (LOG) L.log("gameState: %s",  gameState);
		out.writeObject(gameState);

		// 1) turn processor saves level number
		out.writeInt(level);

		// 1.1 Write out allCharacter Ids
		out.writeInt(allCharacters.size());
		for (Character character : allCharacters) {
			out.writeInt(character.uniqueId);
		}

		// 2) turn processor saves creature whose turn it is by unique id, and
		// the highest uniqueIdCounter
		if (currentCreature == null) {
			out.writeInt(NO_CURRENT_CREATURE);
		} else {
			out.writeInt(currentCreature.uniqueId);
		}

		// 2.1) turn processor saves 0-n falling-in characters
		out.writeInt(charactersFallingIn.size());
		for (Character character : charactersFallingIn) {
			out.writeInt(character.uniqueId);
		}

		// 2.2) turn processor saves 0-n falling-out characters
		out.writeInt(charactersFallingOut.size());
		for (Character character : charactersFallingOut) {
			character.persist(out);
		}

		// set the current leader status and leader character
		out.writeObject(leaderStatus);
		if (currentLeader == null) {
			out.writeInt(NO_CURRENT_CREATURE);
		} else {
			out.writeInt(currentLeader.uniqueId);
		}
	}

	public void creatureMoved() {
		creatureMoved = true;
	}
	
	public void processLeaderMode() {
		if (currentLeader == null) {
			leaderStatus = LeaderStatus.NO_LEADER;
		} else {
			leaderStatus = LeaderStatus.HAVE_LEADER;
		}
		
		Character sawMonster = dungeonQuery.leaderModeOK();
		if (sawMonster != null) {
			leaderStatus = LeaderStatus.LEADER_DISABLED;
			if (currentLeader != null) {
				currentLeader.leaderStatusOff();
				currentLeader = null;
			}
		}

		leaderStatusListeners.alertListeners();
	}
	
	@Override
	public void infoSelected() {
		// app.quit();
	}

	@Override
	public List<Character> getTutorialCharacters() {
		List<Character> theChars = new LinkedList<Character>();

		// The dungeon will fill in the position when the creature falls into the level. This is just a placeholder.
		DungeonPosition p = new DungeonPosition(0, 0); // will init to level 1.

		theChars.add(new Character(Creature.getIdForName("dwarf"), p, 1, dungeonEvents, dungeonQuery, this));
		theChars.add(new Character(Creature.getIdForName("acid blob"), p, 2, dungeonEvents, dungeonQuery, this));
		theChars.add(new Character(Creature.getIdForName("wizard"), p, 3, dungeonEvents, dungeonQuery, this));
		Creature c = theChars.get(0);
//		c.addAbility(new Ability(Ability.getIdForName("wand of slow"), null, 20, dungeonEvents, dungeonQuery), null);
//		c.addAbility(new Ability(Ability.getIdForName("amulet of wizardy"), null, 20, dungeonEvents, dungeonQuery), null);
//		c.addAbility(new Ability(Ability.getIdForName("bow"), null, 20, dungeonEvents, dungeonQuery), null);c.addAbility(new Ability(Ability.getIdForName("wand of smiting"), null, 20, dungeonEvents, dungeonQuery), null);
//		c.addAbility(new Ability(Ability.getIdForName("wand of sun flare"), null, 20, dungeonEvents, dungeonQuery), null);

		return theChars;
	}

	@Override
	public void startGame(List<Character> characters, boolean tutorialMode) {
		allCharacters = new Vector<Character>();
		allCharacters.addAll(characters);
		addInitialExperience();
		charactersFallingOut = new Vector<Character>();
		charactersFallingOut.addAll(allCharacters);
		gameStats = new GameStats();
		setGameState(GameState.START_GAME);
		EventBus.getDefault().event(GameStatePresenter.START_GAME_EVENT, this);
		if (tutorialMode) {
			EventBus.getDefault().event(GameStatePresenter.START_TUTORIAL_EVENT, this);
		}

		currentLeader = null;
		leaderStatus = LeaderStatus.NONE;
		level = 1;
		dungeon.restart();
		startNewLevel(level, tutorialMode);
		setSoloMode(false);
	}

	Character previousCurrentCharacter = null;
	GameState previousGameState = GameState.NO_SAVED_GAME;
	
	private void rememberState() {
		previousGameState = gameState;
		previousCurrentCharacter = getCurrentCharacter();
	}
	
	@Override
	public void mainMenuStartGameSelected() {
		if (gameState != GameState.NEW_GAME) {
			doNewGame();
		}
	}
	
	private void doNewGame() {
		rememberState();
		setGameState(GameState.NEW_GAME);
		sendGameStateEvent();
	}
	
	@Override
	public void cancelNewGameSelected() {
		if (previousGameState == GameState.NO_SAVED_GAME) {
			return;
		}
		
		setGameState(previousGameState);
		setCurrentCharacter(previousCurrentCharacter);
		sendGameStateEvent();
	}
}
