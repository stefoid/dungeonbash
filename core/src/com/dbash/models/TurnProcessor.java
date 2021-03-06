package com.dbash.models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
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
	public static final boolean LOG = false && L.DEBUG;
	
	public static final int NUM_CHARS = 3;
	public static String AVAILABLE_XP_EVENT = "SPENT_XP_EVENT"; 
	public static String CURRENT_CHARACTER_CHANGED = "CURRENT_CHARACTER_CHANGED"; 
	
	public static enum GameState {
		NO_SAVED_GAME,
		NEW_GAME,
		GAME_IN_PROGRESS,
		POWERUP,
		GAME_OVER
	};
	
	private IDungeonEvents dungeonEvents;
	private IDungeonQuery dungeonQuery;
	private IDungeonControl dungeon;
	private Dbash dbash;
	private int level;
	private Creature currentCreature;
	private int creatureTurn;
	private Character currentLeader;
	private Character leaderWhoPassed;
	private Character currentCharacter; // the character whose turn is now
										// active
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
	public static final int INITIAL_EXP = 0;
	public static final int EXP_PER_LEVEL = 400;
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
		this.dbash = dbash;
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
		addExperience(getLevelXp(level));
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
		
		switch(getGameState()) {
		case NO_SAVED_GAME:
			doNewGame();
			break;
		case POWERUP:
			startPowerup();
			break;
		default:
			sendGameStateEvent();
			break;
		}
		
		if (L.TUTORIAL_MODE) {
			startGame(getTutorialCharacters(), true);
		}
		
		if (tutorialMode) {
			EventBus.getDefault().event(GameStatePresenter.START_TUTORIAL_EVENT, null);
			EventBus.getDefault().event(TutorialPresenter.SET_INITIAL_STATE, tutorialState);
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
				//if (LOG) L.log("PAUSED : "+lastPause+" >> "+pauseTurnProcessing);
				lastPause = pauseTurnProcessing;
			}

			if (gameState != GameState.POWERUP && pauseTurnProcessing == false) {
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
			//if (LOG) L.log("testing currentCreature: %s", currentCreature);
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
		boolean isFallingIn = charactersFallingIn.contains(currentCreature);
		boolean isFallingOut = charactersFallingOut.contains(currentCreature);
		boolean canFallIn = false;

		// If it is a falling Character, is it the first falling character and
		// is the entrance free?
		if (isFallingIn) {
			if (currentCreature == charactersFallingIn.elementAt(0)
					&& dungeonQuery.isEntranceFree()) {
				canFallIn = true;
			} else {
				return; // Dont test isReadyForTurn, because this Character isnt
						// in a position to fall in yet.
			}
		}

		// OK, so now whatever it is can test to have a turn.
		if (!isFallingOut && currentCreature.isReadyForTurn()) {
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
			
			boolean toggleLeaderMode = leaderWhoPassed != null && leaderWhoPassed != currentCreature;
			
			if (numberOfCharactersOnMap() < 2) {
				if (leaderWhoPassed != null && !charactersFallingOut.contains(leaderWhoPassed)) {
					leaderWhoPassed = null;
					toggleLeaderMode = false;
				} 
			}
			
			currentCreature.processTurn();

			if (toggleLeaderMode && leaderStatus == LeaderStatus.NO_LEADER && currentCreature instanceof Character) {
				leaderModeToggleSelected();
				leaderWhoPassed = null;
			}
			return;
		}

		return;
	}

	public Character getLeaderWhoPassed() {
		return leaderWhoPassed;
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
		if (LOG) L.log("waiting for player input");
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
		EventBus.getDefault().event(CURRENT_CHARACTER_CHANGED, character);
	}
	
	@Override
	public void setCurrentCharacterOutsideOfTurnProcessing(Character character) {
		currentCharacter = character;
		EventBus.getDefault().event(CURRENT_CHARACTER_CHANGED, character);
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
							rememberState();
							startPowerup();
//							level++;
//							startNewLevel(level, false);
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
		
		if (L.json.has("first_character")) {
			String charName = L.json.getString("first_character");
			Character c = new Character(Creature.getIdForName(charName), p, 1, dungeonEvents, dungeonQuery, this);
			theChars.add(c);
			foundChar = true;
		}
		
		while (foundChar == false) {
			Character c = Character.getNewCharacter(p, 1, dungeonEvents, dungeonQuery, this);
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
			c.addAbility(new Ability(Ability.getIdForName("dwarf hammer"), null, 20, dungeonEvents, dungeonQuery), null);
//			c.addAbility(new Ability(Ability.getIdForName("poison spit"), null, 20, dungeonEvents, dungeonQuery), null);
//			c.addAbility(new Ability(Ability.getIdForName("light health potion"), null, 20, dungeonEvents, dungeonQuery), null);c.addAbility(new Ability(Ability.getIdForName("wand of smiting"), null, 20, dungeonEvents, dungeonQuery), null);
//			c.addAbility(new Ability(Ability.getIdForName("healing prayer"), null, 20, dungeonEvents, dungeonQuery), null);
		
		}

		return theChars;
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

	private int numberOfCharactersOnMap() {
		return allCharacters.size() - charactersFallingIn.size() - charactersFallingOut.size();
	}

	private void setSoloMode(boolean solo) {
		soloStatus = solo;
		for (Character character : allCharacters) {
			character.setSolo(false);
		}
		
		currentLeader = getCurrentLeader();
		if (solo == true) {
			if (currentLeader != null) {
				currentLeader.setSolo(true);
			} else {
				if (acceptInput && currentCreature instanceof Character) {
					currentCharacter.setSolo(true);
				}
			}
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
		
		if (gameState != GameState.GAME_IN_PROGRESS) {
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
			leaderWhoPassed = null;
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
		
		if (gameState != GameState.GAME_IN_PROGRESS) {
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
		
		if (gameState != GameState.GAME_IN_PROGRESS) {
			return;
		}
		
		if (!soloStatus && currentLeader == currentCharacter && numberOfCharactersOnMap() > 1) {
			leaderModeToggleSelected();
			leaderWhoPassed = currentCharacter;
		}
		
		currentCharacter.defending();
	}
	
	@Override
	public void stairDescendSelected() {
		EventBus.getDefault().event(TutorialPresenter.LEADER_ON_EVENT, null);
		if (gameState != GameState.GAME_IN_PROGRESS) {
			return;
		}
		if (acceptInput == false || currentCharacter == nobody) { // in case pounding n th descent key cheap hack
			return;
		}

		// clear that characters solo, but dont clear solo mode if its been on.
		if (currentCharacter.getSolo()) {
			currentCharacter.setSolo(false);
		}

		charactersFallingOut.add(currentCharacter);

		if (currentLeader == currentCharacter) {
			if (numberOfCharactersOnMap() > 0) {
				leaderModeToggleSelected();
				leaderWhoPassed = currentCharacter;
			} else {
				clearLeaderMode();
			}
		} 

		acceptInput = false;
		final IPresenterTurnState tp = this;
		dungeonEvents.goDownStairs(SequenceNumber.getNext(), currentCharacter,
				new IAnimListener() {
					// was that the last character to descend? If so, go to the
					// next level
					public void animEvent() {
						acceptInput = true;
						if (allCharacters.size() == charactersFallingOut.size()) {
							rememberState();
							startPowerup();
						}
					}
				});

		characterEndsTurn(currentCharacter);
	}
	
	@Override
	public boolean itemPickupSelected(Character character, Ability ability) {
		if (gameState != GameState.GAME_IN_PROGRESS) {
			return false;
		}
		
		return character.itemPickupSelected(ability);
	}
	
	@Override
	public boolean itemDropSelected() {
		if (gameState != GameState.GAME_IN_PROGRESS) {
			return false;
		} else {
			return true;
		}
	}
	
	@Override
	public boolean abilitySelected(Character character, Ability ability) {
		if (gameState != GameState.GAME_IN_PROGRESS) {
			return false;
		}
		
		return character.abilitySelected(ability);
	}
	
	// When a character is having a turn, they will ask this, initiating a
	// leadership validity check.
	public Character getCurrentLeader() {
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
			case GAME_IN_PROGRESS:
				event = GameStatePresenter.START_GAME_EVENT;
				break;
			case POWERUP:
				event = GameStatePresenter.POWERUP_START;
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
		if (gameState != GameState.NO_SAVED_GAME) {
			currentCharacter.setCharacterisUsingEye(usingEye);
		}
	}

	private void initOneTime(IDungeonEvents dungeonEvents,
			IDungeonQuery dungeonQuery, IDungeonControl dungeon) {
		// Do the one-time stuff.
		Creature.initializeData();
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
		
		tutorialMode = (Boolean) in.readObject();
		tutorialState = (TutorialPresenter.State) in.readObject();

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
			currentCreature = allCreatures.getCreatureByUniqueId(currentCreatureId);
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
			int uId = in.readInt();
			Character c = (Character) allCreatures.getCreatureByUniqueId(uId);
			charactersFallingOut.add(c);
		}

		// leader Status and current Leader.
		leaderStatus = (LeaderStatus) in.readObject();
		int leaaderId = in.readInt();
		if (leaderStatus == LeaderStatus.HAVE_LEADER) {
			currentLeader = (Character) allCreatures.getCreatureByUniqueId(leaaderId);
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
		
		out.writeObject(tutorialMode);
		if (tutorialMode) {
			out.writeObject(tutorialState);
		} else {
			out.writeObject(TutorialPresenter.State.NO_TUTORIAL);
		}

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
			out.writeInt(character.uniqueId);
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
	
	public void turnLeaderModeNow() {
		if (currentLeader != null) {
			leaderStatus = LeaderStatus.LEADER_DISABLED;
			currentLeader.leaderStatusOff();
			currentLeader = null;
			leaderStatusListeners.alertListeners();
		}
	}
	
	public void processLeaderMode() {
		if (currentLeader == null) {
			leaderStatus = LeaderStatus.NO_LEADER;
		} else {
			leaderStatus = LeaderStatus.HAVE_LEADER;
		}
		
		Character sawMonster = dungeonQuery.leaderModeOK();
		if (LOG) L.log("saw monster: %s",  sawMonster);
		if (sawMonster != null) {
			leaderStatus = LeaderStatus.LEADER_DISABLED;
			if (currentLeader != null) {
				if (LOG) L.log("%s saw monster", currentLeader);
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

		theChars.add(new Character(Creature.getIdForName("halfling"), p, 1, dungeonEvents, dungeonQuery, this));
		theChars.add(new Character(Creature.getIdForName("halfling"), p, 2, dungeonEvents, dungeonQuery, this));
		theChars.add(new Character(Creature.getIdForName("halfling"), p, 3, dungeonEvents, dungeonQuery, this));
		Creature c = theChars.get(0);
//		c.removeAbility("sling");
//		c.addAbility(new Ability(Ability.getIdForName("extra_magic"), null, 20, dungeonEvents, dungeonQuery), null);
		return theChars;
	}

	public Boolean tutorialMode = false;
	public boolean getTutorialMode() {
		return tutorialMode;
	}
	
	@Override
	public void startGame(List<Character> characters, boolean tutorialMode) {
		if (L.TEST_EXP) {
			for (int i=1; i<= Dungeon.FINAL_LEVEL; i++) {
				dungeon.createLevel(this, i);
				int levelXp = getLevelXp(i);
				int monXp = ((Dungeon) dungeon).getXpMonsters();
				gameStats.xpGiven(monXp);
				gameStats.xpGiven(levelXp);
				L.log("LEVEL# %s.  LEVEL XP = %s, MON XP = %s.  LEVEL_TOTAL: %s,   TOTAL XP: %s",
						i, levelXp, monXp, levelXp+monXp, getTotalXp());
			}
			System.exit(1);
		} 
		
		this.tutorialMode = tutorialMode;
		allCharacters = new Vector<Character>();
		allCharacters.addAll(characters);
		addInitialExperience();
		charactersFallingOut = new Vector<Character>();
		charactersFallingOut.addAll(allCharacters);
		gameStats = new GameStats();
		setGameState(GameState.GAME_IN_PROGRESS);
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

	@Override
	public List<Character> getAllCharacters() {
		return allCharacters;
	}
	
	TutorialPresenter.State tutorialState;
	@Override
	public void saveGame(TutorialPresenter.State state) {

		this.tutorialState = state;
		dbash.saveGame();
	}
	
	Character previousCurrentCharacter = null;
	GameState previousGameState = GameState.NO_SAVED_GAME;
	
	private void rememberState() {
		previousGameState = gameState;
		previousCurrentCharacter = getCurrentCharacter();
	}
	
	@Override
	public void mainMenuStartGameSelected() {
		switch (gameState) {
		case NEW_GAME:
		case NO_SAVED_GAME:
			break;
		default:
			doNewGame();
			break;
		}
	}
	
	private void doNewGame() {
		rememberState();
		setGameState(GameState.NEW_GAME);
		sendGameStateEvent();
	}
	
	@Override
	public void cancelNewGameSelected() {
		switch (previousGameState) {
		case NO_SAVED_GAME:
			break;
		case POWERUP:
			//setCurrentCharacter(previousCurrentCharacter);
			currentCharacter = previousCurrentCharacter;
			startPowerup();
			break;
		default:
			setGameState(previousGameState);
			setCurrentCharacter(previousCurrentCharacter);
			sendGameStateEvent();
			break;
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
	
	@Override
	public int getSpentXp() {
		if (gameState != GameState.NEW_GAME) {
			return gameStats.spentXp;
		} else {
			return 0;
		}
	}

	@Override
	public int getTotalXp() {
		if (gameState != GameState.NEW_GAME) {
			return gameStats.xp + L.EXTRA_XP;
		} else {
			return 0;
		}
	}

	@Override
	public void setSpentXp(int spentXp) {
		gameStats.spentXp = spentXp;
		EventBus.getDefault().event(AVAILABLE_XP_EVENT, getTotalXp() - spentXp);
	}

	public void startPowerup() {	
		for (Character character : allCharacters) {
			character.startPowerup();
		}
		setGameState(GameState.POWERUP);
		sendGameStateEvent();
		EventBus.getDefault().event(GameStatePresenter.POWERUP_TAB_ADD, null);
		EventBus.getDefault().event(GameStatePresenter.POWERUP_TAB_BUTTON_ON_EVENT, null);
	}
	
	@Override 
	public void powerUpComplete() {
		gameState = GameState.GAME_IN_PROGRESS;
		currentCharacter = previousCurrentCharacter;
		level++;
		startNewLevel(level, false);
	}
	
	private int getLevelXp(int level) {
		return EXP_PER_LEVEL;
	}
}
