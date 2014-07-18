package com.dbash.models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import com.dbash.models.Creature.CreatureType;
import com.dbash.util.SequenceNumber;
import com.me.dbash.Dbash;

public class TurnProcessor implements IPresenterTurnState {
	
	private IDungeonEvents dungeonEvents;
	private IDungeonQuery dungeonQuery;
	private IDungeonControl dungeon;
	private int level;
	private Creature currentCreature;
	private int creatureTurn;
	private Character currentLeader;
	private Character currentCharacter;  // the character whose turn is now active
	private UIInfoListenerBag currentCharacterListeners;
	private UIInfoListenerBag leaderStatusListeners;
	private UIInfoListenerBag gameInProgressListeners;
	private LeaderStatus leaderStatus;
	private boolean pauseTurnProcessing;  
	private boolean acceptInput;
	private Character nobody;
	private boolean gameInProgress;
	private boolean usingEye;
	public static final int NO_CURRENT_CREATURE = -1;
	private Dbash app;
	
	// Lists to maintain what is going on.
	// allCreatures is a list of every monster and every alive character.  We iterate over this list to give everything a chance to act.
	// allCharacters contains all the alive characters.
	// Characters are added to the map from the falling-in list, and leave the dungeon by going to the falling-out list.
	// Thus when we go down a level, the new falling in becomes the old falling out, and falling out is reset to empty.
	// We know when all characters have left the map when the size of falling out is equal to the size of allCharacters.
	// We know when the game is over when the size of allCharacters = 0;
	public AllCreatures allCreatures;
	public Vector<Character> allCharacters;
	public Vector<Character> charactersFallingIn;
	public Vector<Character> charactersFallingOut;
	
	public TurnProcessor(IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery, IDungeonControl dungeon, Dbash dbash) {
		initOneTime(dungeonEvents, dungeonQuery, dungeon);
		setGameInProgress(false);
		currentCharacter = nobody;
		leaderStatus = LeaderStatus.NONE;
		this.app= dbash;
	}
	
	public void startNewGame() {
		setGameInProgress(true);
		currentLeader = null;
		leaderStatus = LeaderStatus.NONE;
		level = 1;
		dungeon.restart();
		startNewLevel(level);
	}
	
	// We set up the lists for this level.
	private void startNewLevel(int level)
	{
		pauseTurnProcessing = false;
		acceptInput = false;  
		dungeon.createLevel(this, level);
		setCurrentCharacter(nobody);
		
		if (level == 1) {
			allCharacters = new Vector<Character>();
			allCharacters.addAll(createRandomCharacters());
			charactersFallingIn = new Vector<Character>();
			charactersFallingIn.addAll(allCharacters);
		} else {
			charactersFallingIn = charactersFallingOut;
		}

		charactersFallingOut = new Vector<Character>();
		allCreatures = new AllCreatures();
		allCreatures.addAll(charactersFallingIn);
		allCreatures.addAll(dungeonQuery.getAllMonsters());
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
		
		setCurrentCharacter(currentCharacter);
		if (currentCharacter != nobody) {
			currentCharacter.changeToHighlighted();
			waitForPlayerInput();
		}
		
		getCurrentLeader();  // will update leader status and alert observers
	}
	
	@Override
	public void stairDescendSelected() {
		if (acceptInput == false) {  // in case pounding on the descend key.  cheap hack.
			return;
		}
		
		// adjust the lists
		allCreatures.remove(currentCharacter);
		charactersFallingOut.add(currentCharacter);

		// was that character the leader?
		if (currentLeader == currentCharacter) {
			currentLeader = null;
			leaderStatus = LeaderStatus.NO_LEADER;
			leaderStatusListeners.alertListeners();
		}
		
		acceptInput = false;
		dungeonEvents.goDownStairs(SequenceNumber.getNext(), currentCharacter, new IAnimListener() {
			// was that the last character to descend?  If so, go to the next level
			public void animEvent() {
				acceptInput = true;
				if (allCharacters.size() == charactersFallingOut.size()) {
					level++;
					startNewLevel(level);
				}
			}
		});
		
		characterEndsTurn(currentCharacter);
	}
	
	// returns true if the position passed in is far from all characters
	// and far from the stairs (if a creature skips a turn whilst camping on the stairs, then the next character cannot
	// fall-in)
	public boolean amFarFromCharacters(DungeonPosition position) {
		for (Character character : allCharacters) {
			if (position.distanceTo(character.getPosition()) < 5 + 3) {  
				return false;
			}
			if (dungeonQuery.isCreatureNearEntrance(position)) {  
				return false;
			} 
		}
		return true;
	}
	boolean lastPause = false;
	// This will process any automatic actions, such as an entire monster turn or any part of a character
	// turn that can proceed without further player input.
	// When player input is required, this call does nothing until mustWait is set back to true by
	// one of the player inputs that will cause the current characters turn to proceed, and then end.
	public void gameLogicLoop()
	{
		if (pauseTurnProcessing != lastPause) {
			//System.out.println("PAUSED : "+lastPause+" >> "+pauseTurnProcessing);
			lastPause = pauseTurnProcessing;
		}
		
		
		if (pauseTurnProcessing == false) {
			processNextCreature();  
		}
	}
	
	// The TurnProcessor iterates over the entire list, giving each creature a chance to act.
	// If there are falling characters, the first falling character in the queue is a special case and is processed 
	// at the start of each full list cycle.
	// The chance to act is based on the creatures speed.  If it misses its chance, its chance is increased for next turn, so
	// eventually, even the slowest creatures get a turn, although faster creatures may have had several turns by then.
	// Falling into the level is not considered to be a Characters complete action - when the character  appears on the map
	// the player gets to have a turn.
	//
	//  turn processor calls on those creatures who are able to have their turn, to take their turn
	//	if a creature doesnt have a turn, it keeps going
	//	if they do have their turn, they call creature.endTurn() , which amongst other thins calls waitForAnimsToFinish()
	//  at the end of it which sets the turn processor into a wait state, and issues a dummy animation callback to dungeon
	//	if the creature is a character that has to wait for player input, it calls waitForPlayerInput() at that point
	private void processNextCreature() 
	{
        if (creatureTurn >= allCreatures.size())
            creatureTurn = 0;

		if (allCreatures.size() == 0)
		{
			currentCreature = null;
			return;  // I guess its possible to destroy all creatures on a level with a burst.
		}
	
		// processing a single creature per draw cycle takes a long time to process a lot of creatures because drawing takes some time.
		// so this loop will skip all stable, non-interesting monster turns in one go
        do {
            // get the next creature
            currentCreature = (Creature) allCreatures.elementAt(creatureTurn);
    		// increment counter for next time
    		creatureTurn++;
            if (creatureTurn >= allCreatures.size())
                creatureTurn = 0;
            
         // if all characters are dead, return false or nothing will ever happen.
            if (allCharacters.size() == 0) {
            	break;
            }
        } while (currentCreature.canSkipTurn());

        
        // Dont call isReadyForTurn, until we have established that a falling character can fall in.
        boolean isFalling = charactersFallingIn.contains(currentCreature);
        boolean canFallIn = false;
        
        // If it is a falling Character, is it the first falling character and is the entrance free?
		if (isFalling) {
			if (currentCreature == charactersFallingIn.elementAt(0) && 	dungeonQuery.isEntranceFree()) {
				canFallIn = true;
			} else {
				return;  // Dont test isReadyForTurn, because this Character isnt in a position to fall in yet.
			}
		}

		// OK, so now whatever it is can test to have a turn.
    	if (currentCreature.isReadyForTurn()) {  
    		
    		// If a lucky falling character is ready and able to fall in, then tell the dungeon about it.
    		if (canFallIn) {
    			charactersFallingIn.removeElement(currentCreature);  // no longer falling
    			dungeonEvents.fallIntoLevel(SequenceNumber.getNext(), (Character)currentCreature); // when a character falls in, it get a chance to act (wait for player control)
        	}
    		
    		currentCreature.processTurn(); //  is this right?
    		return;
    	}
    	
    	return;
	}
	
	public boolean anyActiveFollowers()
	{
		for (Character c : allCharacters) {
			if (c.isActiveFollower()) {
				return true;
			}
		}
		
		return false;
	}

	
	// When the Character has ended a turn dependent on player input and/or animation completeness
	// it calls this to get the GameLogicLoop back in action.
	public void characterEndsTurn(Creature character)
	{
		currentLeader = getCurrentLeader();  // a character has done something, maybe a move, so has it resulted in leader mode going off?
		character.endTurn();
		setCurrentCharacter(nobody);
	}
	
	// this is called to pause turn processing until all animations to do with the current characters turn have finished playing.
	// We dont want this to happen if we are in leader mode because then the followers will have to wait.
	// but in that sitaution, we still want input turned off until animations are finished.
	public void waitForAnimsToFinish() {
		acceptInput = false; 
		
		if (currentLeader != null) {
			pauseTurnProcessing = false;  // to turn it off in case we were waiting for player input previous to this character
			// ending its turn
		} else {
			waitForAnim(null);// TODO
		}
	}
	
	// characters call this to pause turn processing until player has directed character action.
	// turn processing will be turned on again by the callback employed by waitForAnimsToFinish
	public void waitForPlayerInput() {
		System.out.println("waiting for player input");
		pauseTurnProcessing = true;
		dungeonEvents.waitingForAnimToComplete(SequenceNumber.getNext(), new IAnimListener() {
			public void animEvent() {
				acceptInput = true; 
			}
		});
	}
	
	protected void waitForAnim(IAnimListener listener) {
		pauseTurnProcessing = true;
		acceptInput = false;  
		final IAnimListener l = listener;
		dungeonEvents.waitingForAnimToComplete(SequenceNumber.getNext(), new IAnimListener() {
			public void animEvent() {
				pauseTurnProcessing = false;
				acceptInput = true;  
				if (l != null) {
					l.animEvent();
				}
			}
		});
	}
	
	// So when the current character is changed, any presenters listening will read the new character, update their
	// pat of the UI, and listen to that character for changes to its details that are relevant for them.
	public void setCurrentCharacter(Character character) {
		currentCharacter = character;		
		currentCharacter.setCharacterisUsingEye(usingEye);
		currentCharacterListeners.alertListeners();
	}
	
	// returns true if the character shoulddrop its stuff, false otherwise.
	public boolean characterDied(Character character)
	{
		allCharacters.remove(character);
		allCreatures.remove(character);
		if (currentLeader == character) {
			currentLeader = null;
			leaderStatus = LeaderStatus.NO_LEADER;
			leaderStatusListeners.alertListeners();
		}
		
		dungeonEvents.creatureDies(SequenceNumber.getCurrent()+2, character, character);
		
		if (allCharacters.size() == 0) {
			// game over man, game over.
			dungeon.gameOver();
			setGameInProgress(false);
		} else
		// What if that was the last character on this level?
		// If so, go to the next level
		if (allCharacters.size() == charactersFallingOut.size()) {
			dungeonEvents.waitingForAnimToComplete(SequenceNumber.getCurrent(), new IAnimListener() {
				public void animEvent() {
					level++;
					startNewLevel(level);
				}
			});
			return false;
		}
		return true;
	}
	
	public void monsterDied(Creature creature, Character observer)
	{
		allCreatures.remove(creature);
		dungeonEvents.creatureDies(SequenceNumber.getCurrent()+2, observer, creature);
	}
	
	private List<Character> createRandomCharacters() {
		List<Character> retval = new LinkedList<Character>();

		// The dungeon will fill in the position when the creature falls into the level.  This
		// is just a placeholder.
		DungeonPosition p = new DungeonPosition(0, 0);  //will init to level 1.

		int teamLevelPoints = 9;

		for (int i = 0; i < 3; i++) {
			Character chr;
			
		   if (i == 0) {
			   chr = Character.getNewCharacter(5, 2, true, p, i+1, dungeonEvents, dungeonQuery, this);
		   }
		   else if (i == 1) {   
			   int nextValue = teamLevelPoints-2;
			   if (nextValue < 2)
				   nextValue = 2;
			   chr = Character.getNewCharacter(nextValue, 2, false, p, i+1, dungeonEvents, dungeonQuery, this);
		   } else {
			   chr = Character.getNewCharacter(teamLevelPoints, teamLevelPoints, false, p, i+1, dungeonEvents, dungeonQuery, this); 
		   }
			
			chr.addExperience(1000, true);
			chr.setDisplayedNumber(i + 1);
			teamLevelPoints -= chr.getLevelValue();
			retval.add(chr);
		}
		
		// test
		retval.clear();
		p.level = 20;
		retval.add(new Character(29, p, 1, dungeonEvents, dungeonQuery, this));
		retval.add(new Character(29, p, 2, dungeonEvents, dungeonQuery, this));
		retval.add(new Character(29, p, 3, dungeonEvents, dungeonQuery, this));
		Creature c = retval.get(0);
		//c.addAbility(new Ability(50, null, 20, dungeonEvents, dungeonQuery));
		c.addAbility(new Ability(69, null, 20, dungeonEvents, dungeonQuery));
		c.addAbility(new Ability(143, null, 20, dungeonEvents, dungeonQuery));
		c.addAbility(new Ability(112, null, 20, dungeonEvents, dungeonQuery));
		c.addAbility(new Ability(105, null, 20, dungeonEvents, dungeonQuery));
		c.addAbility(new Ability(58, null, 20, dungeonEvents, dungeonQuery));
		// 149 + 75 
		// test

		return retval;
	}

	// IPresenterTurn State  interface
	@Override
	public void onChangeToCurrentCharacter(UIInfoListener listener) {
		currentCharacterListeners.add(listener);
	}

	@Override
	public Character getCurrentCharacter() {
		return currentCharacter;
	}

	@Override
	public Character getCharacterForTouchEvents() {
		if (acceptInput) {
			return currentCharacter;
		} else {
			return nobody;
		}
	}
	
	@Override
	public void passTurnSelected() {
		System.out.println("Pass turn pressed");
		characterEndsTurn(currentCharacter);
	}

	@Override
	public void LeaderModeToggleSelected() {
		if (currentLeader == null) {
			currentLeader = currentCharacter;
			leaderStatus = LeaderStatus.HAVE_LEADER;
			System.out.println("current leader is "+currentLeader.creature.name);
		} else {
			currentLeader = null;
			leaderStatus = LeaderStatus.NO_LEADER;
			System.out.println("no current leader");
		}
		
		leaderStatusListeners.alertListeners();
	}

	// When a character is having a turn, they will ask this, initiating a leadership validity check.
	public Character getCurrentLeader()
	{
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

		return currentLeader;
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
	public void onChangeToGameInProgress(UIInfoListener listener) {
		gameInProgressListeners.add(listener);
	}
	
	@Override
	public boolean gameInProgress() {
		return gameInProgress;
	}

	public void setGameInProgress(boolean gameInProgress) {
		this.gameInProgress = gameInProgress;
		gameInProgressListeners.alertListeners();
	}
	
	@Override
	public void startGameSelected() {
		startNewGame();
	}

	@Override
	public void usingEye(boolean usingEye) {
		this.usingEye = usingEye;
		currentCharacter.setCharacterisUsingEye(usingEye);
	}

	
	private float range;
	private float alpha;
	private boolean useBlack = false;
	
	@Override
	public float getRange() {
		return range;
	}


	@Override
	public float getAlpha() {
		return alpha;
	}


	@Override
	public boolean getUseBlack() {
		return useBlack;
	}

	public void setUseBlack(boolean useBlack) {
		this.useBlack = useBlack;
	}


	@Override
	public void setRange(float range) {
		this.range = range;
	}


	@Override
	public void setAlpha(float alpha) {
		this.alpha = alpha;
	}
	
	private void initOneTime(IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery, IDungeonControl dungeon) {
		// Do the one-time stuff.
		Creature.initializeData();
		currentCharacterListeners =  new UIInfoListenerBag();
		leaderStatusListeners =  new UIInfoListenerBag();
		gameInProgressListeners  =  new UIInfoListenerBag();
		nobody = new NoCharacter();
		gameInProgress = true;
		usingEye = false;
		acceptInput = false;
		this.dungeonEvents = dungeonEvents;
		this.dungeonQuery = dungeonQuery;
		this.dungeon = dungeon;
	}
	
	public TurnProcessor() {
		
	}
	
	public void load(ObjectInputStream in, IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery, 
			IDungeonControl dungeon, AllCreatures allCreatures) throws IOException, ClassNotFoundException {

		allCharacters = new Vector<Character>();
		charactersFallingIn = new Vector<Character>();
		charactersFallingOut = new Vector<Character>();
		this.allCreatures = allCreatures;
		
		setGameInProgress((Boolean)in.readObject());
		
		// start reading saved game
		level = in.readInt();
		
		// read all Characters into their array for latter reference.
		int chars = in.readInt();
		for (int i=0; i<chars; i++) {
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
		for (int i=0; i<fallIn; i++) {
			int uId = in.readInt();
			Character c = (Character) allCreatures.getCreatureByUniqueId(uId);
			charactersFallingIn.add(c);
		}
		
		// read all Characters falling out
		int fallOut = in.readInt();
		for (int i=0; i<fallOut; i++) {
			Creature.CreatureType t = (CreatureType) in.readObject();  // we know its a character, so read that enumeration first
			Character c = new Character(in, dungeonEvents, dungeonQuery, this);
			charactersFallingOut.add(c);
			allCharacters.add(c);
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
		out.writeObject(gameInProgress);
		
		//	1) turn processor saves level number
		out.writeInt(level);
		
		// 1.1 Write out allCharacter Ids
		out.writeInt(allCharacters.size());
		for (Character character : allCharacters) {
			out.writeInt(character.uniqueId);
		}
		
		//	2) turn processor saves creature whose turn it is by unique id, and the highest uniqueIdCounter
		if (currentCreature == null) {
			out.writeInt(NO_CURRENT_CREATURE);
		} else {
			out.writeInt(currentCreature.uniqueId);
		}
		

		//	2.1) turn processor saves 0-n falling-in characters
		out.writeInt(charactersFallingIn.size());
		for (Character character : charactersFallingIn) {
			out.writeInt(character.uniqueId);
		}

		//	2.2) turn processor saves 0-n falling-out characters
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

	@Override
	public void quitSelected() {
		app.quit();
	}



}