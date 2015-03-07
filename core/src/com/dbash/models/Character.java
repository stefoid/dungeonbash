package com.dbash.models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Vector;

import com.dbash.models.Ability.AbilityType;
import com.dbash.models.Creature.StealthStatus;
import com.dbash.models.IDungeonQuery.AtLocation;
import com.dbash.models.IPresenterTurnState.LeaderStatus;
import com.dbash.presenters.tabs.AbilitySelectionList;
import com.dbash.util.L;
import com.dbash.util.Randy;
import com.dbash.util.SequenceNumber;

@SuppressWarnings("unused")
/* Players control characters, so there is no AI like a Monster, but there could be multiple player input required
 * to fully define a character action. Player input that can end a character's turn is: 
 * 
 * a) swiping to move or attack a monster
 * b) tapping a target for the currently selected targetable ability
 * c) pressing 'pass' or 'go down stairs'
 * 
 * The sequence of events for a character turn is:
 * 
 * 1) TurnProcessor sets the character as the current character
 * 2) TurnProcessor calls processTurn on the character
 * 3) The character performs preliminary calculations, such as applying effects of current abilities, etc...
 * 4) If leader mode is on, and this character is a follower, have it make an automatic following move.
 * 5) If leader mode is on, and this character is the leader, it may have to wait for followers to catch up.
 * 6) If the character hasnt died, follower-moved, or leader-waited, the game waits for further player input
 *    If the former, processTurn returns false, or if it has to wait for player input, it returns true.
 * 7) Player input of a type that will not end the characters turn may occur, such as
 *    - inventory manipulation
 *    - using the eye
 *    - selecting the 'current ability'
 * 8) Player input that will end the turn occurs (i.e. a, b or c above)
 * 9) Character processes the input, generates whatever dungeon events it produces and calls 
 *    characterEe TurnProcessor:endTurn.
 */

public class Character extends Creature implements IPresenterCharacter {
	public static final boolean LOG = true && L.DEBUG;
	
	private class BestDir {
		public BestDir () {
			direction = DungeonPosition.NO_DIR;
		}
		
		public void setDirection (int direction) {
			this.direction = direction;
			if (direction == DungeonPosition.NO_DIR) {
				hasFreeOption = false;
			} else {
				hasFreeOption = true;
			}
		}
		
		public boolean hasFreeOption;
		public boolean didComplain;
		public int direction;
	}
	final Character me = this;
	static int charCounter = 1;
	
	private class Complaint {
		public int complaints = 0;
		
		public void complain() {

			complaints++;
			if (turnProcessor.getCurrentLeader() == me) {
				if (LOG) L.log("COMPLAINT "+complaints);
			}
		}
		
		public void stopComplaining() {
			complaints = 0;
		}
		
		public boolean isComplaining() {
			if (complaints > 0) {
				return true;
			} else {
				return false;
			}
		}
		
		public boolean isFedUp() {
			if (complaints > 9) {
				return true;
			} else {
				return false;
			}
		}
	}
	boolean amSolo;
	
	
	protected ShouldMoveStrategy shouldMove = new ShouldMoveStrategy();
	//Sprite manSprite = FileManager.createSpriteFromSingleImage("res/fresh/b.png", 1.0f);
	LinkedList<DungeonPosition> path = new LinkedList<DungeonPosition>();
	boolean amActiveFollower;
	Character theLeader;
	// This characters personal shadowmap.
	public ShadowMap shadowMap;
	boolean isAlive = true;
	private Complaint mood = new Complaint();
	
	boolean inLOSOfMonster;   // Simply if it is in line of sight, hidden or not
	boolean currentlySeenByMonster;  // If there is a monster curently seeing it.
	boolean amClosestToMonster;
	
	// INTERFACE
	public Character(int creatureId, DungeonPosition p, int charNumber, IDungeonEvents dungeonEvents, 
			IDungeonQuery dungeonQuery, TurnProcessor turnProcessor) {
		super(creatureId, p, dungeonEvents, dungeonQuery, turnProcessor); // this calls the constructor of the Creature class
		
		amActiveFollower = false;
		initChar(turnProcessor);
	}
	
	public Character(ObjectInputStream stream, IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery, TurnProcessor turnProcessor) throws IOException, ClassNotFoundException {
		super(stream, dungeonEvents, dungeonQuery, turnProcessor);
		int selIndex = stream.readInt();
		if (selIndex >= 0) {
			currentSelectedAbility = abilities.get(selIndex);
		}
		this.turnProcessor = turnProcessor;
		initChar(turnProcessor);
	}

	public void leaderStatusOff() {
		setAutomaticLeaderTargetPosition(null); 
	}
	
	private void initChar(final TurnProcessor turnProcessor) {
		makeNewBagsOfListeners();
		effectListListeners = new UIInfoListenerBag();
		creatureStats.isCharacter = true;
		shadowMap = new ShadowMap(this);
		charCount = charCounter;
		charCounter++;
	}
	
	// save the character to the stream in the same order that it will be read
	public void persist(ObjectOutputStream out) throws IOException {
		out.writeObject(CreatureType.CHARACTER);
		super.persist(out);
		int selectedIndex = -1;
		for (int i = 0; i<abilities.size();i++) {
			if (abilities.get(i) == currentSelectedAbility) {
				selectedIndex = i;
			}
		}
		out.writeInt(selectedIndex);  // currently selected ability index
	}

	private void makeNewBagsOfListeners() 
	{
		abilityListListeners = new UIInfoListenerBag();
		characterStatListeners = new UIInfoListenerBag();
		itemListListeners = new UIInfoListenerBag();
		stealthStatusListeners = new UIInfoListenerBag();
	}

	
	private void alertThatIAmTheCurrentCharacter()
	{
		turnProcessor.setCurrentCharacter(this);
		dungeonEvents.changeCurrentCharacterFocus(SequenceNumber.getNext(), this);
		
		// if the eye tab is on, set yourself as the eye target without eye animation.
		if (characterIsUsingEye) {
			dungeonEvents.setCharacterisUsingEye(true, mapPosition, false);
		}
	}
	
	@Override
	public void processTurn() 
	{	
		// Process the creature turn, to test for death due to poison or whatever.
		boolean creatureStillAlive = processStatsForTurn();
		if (creatureStillAlive == false) {
			turnProcessor.characterEndsTurn(this);
			return;
		}

		// If solo mode is on, but it aint me, then defend.
		if (turnProcessor.getSoloStatus()) {
			if (getSolo() == false) {
				addDefendingAbility();
				turnProcessor.characterEndsTurn(this);
				return;
			}
		}
		
		SequenceNumber.bumpSequenceNumberforNewCharacter();
		
		// This is only set to true when a character makes an active following move
		amActiveFollower = false;
		
		// Tell whoever that a character is having a turn, unless they are a follower.
		theLeader = turnProcessor.getCurrentLeader();
			if ((theLeader == null) || (theLeader == this)) {
				alertThatIAmTheCurrentCharacter();
			}
		
		// Determine if an automatic-turn happens: (follower movement or leader-waiting or leader movement)
		if (madeAutomaticTurn(theLeader)) {
			turnProcessor.characterEndsTurn(this);
			return;
		}
		
		changeToHighlighted();
		turnProcessor.waitForPlayerInput();
		return;
	}
		
	public void changeToHighlighted() {
		highlightStatus = HighlightStatus.CURRENTLY_IN_FOCUS;
		if (visualStatusListener != null) {
			visualStatusListener.UIInfoChanged();
		}
	}
	
	/**
	 * Characters that are the leader have a special case - the leader shouldnt process his move while he is
	 * still animating, or everything goes to shit.  However, if leadermode is off because the leader saw a monster,
	 * the leaders turn should be processed.
	 */
	@Override
	public boolean isReadyForTurn() {
		if (finishedAnimatingAutomaticMove) {
			return super.isReadyForTurn();
		} else {
			if (turnProcessor.getCurrentLeader() == null) {
				return super.isReadyForTurn();
			}
			return false;
		}
	}
	
	// If some kind of leader-related automatic action occurs, do it, and return true;
	private boolean madeAutomaticTurn(Character theLeader) 
	{
		boolean madeAutomaticTurn = false;
		
		// No leader related stuff, so we need to wait for player input.
		if (theLeader == null) {
			return madeAutomaticTurn;  
		}
		
		if (theLeader == this) {
			madeAutomaticTurn = doLeaderProcessing();
		} else {
			madeAutomaticTurn = true;
			doFollowerProcessing(theLeader);  // follower processing always ends that characters turn.
		}
			
		if (madeAutomaticTurn == false) {
			mood.stopComplaining();
		}
		
		return madeAutomaticTurn;
	}
	
	public void setPosition(DungeonPosition newPosition) {
		super.setPosition(newPosition);
		if (shadowMap != null) {
			shadowMap.updateCenterPos(newPosition);
		}
	}
	
	private static final int pathLength = 32;
	
	// Each character keeps a list of positions of the path it has traveled, so followers can track it down.
	private void updatePath(DungeonPosition position)
	{	
		if (path.size() == 0) {
			path.add(0, position);
		}
		
		DungeonPosition latestPosition = path.getFirst();
		
		// If the characters position has changed add it to the list.
		if (position.equals(latestPosition) == false) {
			path.add(0, position);
		}
		
		// cap the path at 'pathLength' positions.
		while (path.size() > pathLength) {
			path.remove(pathLength);
		}
	}
	
	// This is so followers can follow the leader.  Return the position that is the nearest to the characters current position
	// from its recent path, otherwise return null;
	public DungeonPosition getLatestPathPositionIcanSee(Character them)
	{
		// first check the current position
		if (dungeonQuery.positionIsInLOSOfCharacter(them, mapPosition)) {
			return mapPosition;
		}
		
		// otherwise go through the list, in order of most recent
		for (DungeonPosition pos : path) {
			if (dungeonQuery.positionIsInLOSOfCharacter(them, pos)) {
				return pos;
			}
		}
		
		return null;
	}

	public boolean isActiveFollower()
	{
		return amActiveFollower;
	}
	
	// Need a function here that takes a target position and returns a direction to move to get to that position
	// and whether or not it can be reached immediately or requires a complaint to a character in the way.
	// A path free of other characters is always chosen first.
	// If a complaint was necessary, then it will complain.
	private BestDir calcBestDir(DungeonPosition targetPosition, boolean iAmLeader) {
		BestDir bestDir = new BestDir();
		
		if (mapPosition.equals(targetPosition)) {
			return bestDir;
		}
		
		bestDir.setDirection(findBestDirection(targetPosition, false, shouldMove));
		
		// If the target position is the 1 move away and it is occupied by a character,
		// ignore any free option and  just wait and complain.
		if (targetPosition.equals(leaderTargetPos) && targetPosition.distanceTo(mapPosition) == 1) {
			if (dungeonQuery.whatIsAtLocation(targetPosition) == AtLocation.CHARACTER) {
				bestDir.setDirection(DungeonPosition.NO_DIR);
				if (iAmLeader) {
					bestDir.didComplain = true;
					mood.complain();
				}
				return bestDir;
			}
		} 

		if (bestDir.hasFreeOption == false) {
			bestDir.setDirection(findBestDirection(targetPosition, true, shouldMove));
			if (bestDir.hasFreeOption) {
				// there is a way, but it is occupied by a character.
				bestDir.hasFreeOption = false; // well, there is a way, but it is blocked, so set this false.
				if (iAmLeader) {
					bestDir.didComplain = true;
					mood.complain();
				}
			}
		}
		
		return bestDir;
	}
	
	public boolean isComplaining() {
		return mood.isComplaining();
	}
	
	/**
	 * The Leader can basically do anything - move, complain, wait.  If one of these automatic leader decisions 
	 * is made, this returns true.
	 * But if he determines that the situation is hopeless and there is no way to automatically resolve it
	 * without player input, this function should return false.  Leader mode will still continue, but turn
	 * processing will pause for player input for the leader.
	 * 
	 * The aim of the automatic decision making is to navigate himself and/or the team (depending on solo-mode)
	 * to the designated AutomaticLeaderTarget tile.  Once the leader hits that spot, the target tile can be
	 * cleared and the Leader will wait for player input until there is a new Leader target designated.
	 * 
	 * 1) keep chugging towards the target position relentlessly until you get on it, and then wait there, 
	 * having turns, until there are no active followers.
	 * 2) if the position you want to move TO is the target position, then complain if there is a character in 
	 * the way, even if BestDir says there is a free spot.
	 * 3) In the above situation, ask the character if it is possible for him to move at all.  
	 * If it gets the answer NO like - 3 times in a row or something, then its time to give up
	 */
	private boolean doLeaderProcessing()
	{		
		boolean giveUpAndWaitForPlayerInput = false;
		BestDir bestDir = new BestDir();
		
		// First step is o decide whether to wait, move or give up.
		if  (isThereAnAutomaticLeaderTarget()) {
			if (mapPosition.equals(leaderTargetPos)) {
				if (turnProcessor.anyActiveFollowers() == false) {
					// give up
					giveUpAndWaitForPlayerInput = true;
					clearAutomaticLeaderTarget();
				}
			} else {
				if (dungeonQuery.positionIsInLOSOfCharacter(this, (leaderTargetPos))) {
					bestDir = calcBestDir(leaderTargetPos, true);
				}
				
				// If we have been stuck waiting on followers to get out of the way a lot, give up.
				if (mood.isFedUp()) {
					giveUpAndWaitForPlayerInput = true;
					clearAutomaticLeaderTarget();
				}
				
				// Is there somewhere to move to?
				if (bestDir.hasFreeOption) {
					performLeaderMovement(new DungeonPosition(mapPosition, bestDir.direction), bestDir.direction);
					mood.stopComplaining();
				} else {
					if (bestDir.didComplain == false) {
						clearAutomaticLeaderTarget();
					}
				}
			}
		} else {
			giveUpAndWaitForPlayerInput = true;
		}
		
		return !giveUpAndWaitForPlayerInput;
	}
	
	// OK, so we can do one of three things, in order of preference.
	// a) respond to a complain from the leader to get out of the way if within 2 squares radius
	//    or if the leader is complaining and you are outside of 2 squares radius.  sit still until he stops complaining
	// b) walk directly to the leader
	// c) walk directly to the last known position of the leader, via their path
	// d) do nothing
	
//	1) If the leader is complaining (Or I am in the target position), and I am withing 2 squares of him, then move away from him, otherwise do nothing.  ** done **
//	2) If I am outside of 2 squares and the leader isnt complaining, then follow.
//	3) If I cant see the leader or any position in the leaders path, then flag as not active follower and do nothing until the situation resolves.
//	4) If I am within 2 squares, and the leader ISNT complaining , then become 'not an active follower'.
	private void doFollowerProcessing(Character theLeader)
	{
		DungeonPosition targetPosition = null;
		DungeonPosition leaderTrailPos = theLeader.getLatestPathPositionIcanSee(this);
		if (leaderTrailPos == null) {
			leaderTrailPos = theLeader.getPosition();
		}
		int distanceToLeader = mapPosition.distanceTo(theLeader.getPosition());
		
		amActiveFollower = true;  // Tell the leader there is at least one active follower.
		BestDir bestDir = calcBestDir(leaderTrailPos, false);
		boolean iAmOnLeaderTarget = mapPosition.equals(leaderTargetPos);
		
		// is the leader complaining?  handle that as a priority.
		// if I am close, try to run away from the leader.
		if (theLeader.isComplaining() || iAmOnLeaderTarget) {
			if (distanceToLeader <= 2 || iAmOnLeaderTarget) {
				targetPosition = getOutOfTheWay(theLeader.getPosition(), theLeader.getAutomaticLeaderTarget());
			} 
		} else {
			if (distanceToLeader > 1) {
				int d = findBestDirection(leaderTrailPos, false, shouldMove);
				if (d != DungeonPosition.NO_DIR) {
					targetPosition = new DungeonPosition(mapPosition, d);
				}
			} 
		}
		
		// Having got to here, we either have a free option, or we cant move at all.
		if (targetPosition != null) {
			//DungeonPosition newPosition =  new DungeonPosition(mapPosition, bestDir.direction);
			updatePath(targetPosition);
			Character releventChar = null;
			// can the leader see this?
			// visible to the currently focused character, then there is no need to change focus.
			if (dungeonQuery.positionIsInLOSOfCharacter(theLeader, mapPosition) ||
					dungeonQuery.positionIsInLOSOfCharacter(theLeader, targetPosition)) {
				releventChar = theLeader;
			}

			finishedAnimatingAutomaticMove = false;
			dungeonEvents.creatureMove(SequenceNumber.getNext(), releventChar, this, mapPosition, targetPosition, bestDir.direction,  Dungeon.MoveType.FOLLOWER_MOVE, 
					new IAnimListener() {
						public void animEvent() {
							finishedAnimatingAutomaticMove = true;
					}});
		} else {
			amActiveFollower = false;  // no trail to follow.  Im lost.  Or close enough.
		}
	}
	
	/**
	 * The direction of the swipe and target tile that the player raised his finger on is passed in.
	 * If the current character is the leader, we try and work out a way to navigate to that tagret tile,
	 * otherwise its just a normal move in the direction passed in.
	 * 
	 * If the gesture is valid, characteEndsTurn() must be called.
	 */
	boolean finishedAnimatingAutomaticMove = true;
	@Override
	public void movementGesture(int direction, DungeonPosition targetPosition) {
		if (isThereAnAutomaticLeaderTarget()) {
			return;
		}
		
		boolean interpretedAsLeaderGesture = doLeaderGuestureProcessing(direction, targetPosition);
		
		if (interpretedAsLeaderGesture) {
			turnProcessor.characterEndsTurn(this);
		} else {
			// Otherwise just a bog standard move.
			DungeonPosition position = new DungeonPosition(mapPosition, direction);
			switch (dungeonQuery.whatIsAtLocation(position))
			{	
			    case HOLE:
			    	if (canFly) {
			    		boolean didSpecialMove = performCharge(position, direction, AtLocation.MONSTER, this);
			    		
			    		if (!didSpecialMove) {
			    			didSpecialMove = performDash(position, direction, this);
			    		}
			    		
						if (!didSpecialMove) {
							dungeonEvents.creatureMove(SequenceNumber.getNext(), this, this, mapPosition, position, direction, Dungeon.MoveType.NORMAL_MOVE, null);
						}
						updatePath(position);
						turnProcessor.characterEndsTurn(this);
			    	}
			    	break;
				case FREE:
		    		boolean didSpecialMove = performCharge(position, direction, AtLocation.MONSTER, this);
		    		
		    		if (!didSpecialMove) {
		    			didSpecialMove = performDash(position, direction, this);
		    		}
		    		
					if (!didSpecialMove) {
						dungeonEvents.creatureMove(SequenceNumber.getNext(), this, this, mapPosition, position, direction, Dungeon.MoveType.NORMAL_MOVE, null);
					}
					
					updatePath(position);
					turnProcessor.characterEndsTurn(this);
					break;
				case MONSTER:
					makeMeleeAttack(dungeonQuery.getCreatureAtLocation(position));
					turnProcessor.characterEndsTurn(this);
					break;
				default:
					break;
			}
		}
	}
	
	
	/**
	 * If this character is the leader, use the target position, if the finger is lifted on a spot that is
	 * either FREE or occupied by a CHARACTER.  Or a HOLE if they are a flyer.
	 */
	public boolean doLeaderGuestureProcessing(int direction, DungeonPosition targetPosition) {
		if (turnProcessor.getCurrentLeader() != this) {
			return false;
		}
		
		if (dungeonQuery.positionIsInLOSOfCharacter(this, (targetPosition)) == false) {
			return false;
		}
		
		BestDir bestDir = new BestDir();
		
		if (isValidLeaderTile(targetPosition)) {
			setAutomaticLeaderTargetPosition(targetPosition);
			bestDir = calcBestDir(targetPosition, true);
			if (bestDir.hasFreeOption) {
				int dir = bestDir.direction;
				DungeonPosition newPos = new DungeonPosition(mapPosition, dir);
				performLeaderMovement(newPos, dir);
			}
			
			return true;
		}
		
		return false;
	}
	
	private void performLeaderMovement(DungeonPosition newPos, int direction) {
		finishedAnimatingAutomaticMove = false;
		dungeonEvents.creatureMove(SequenceNumber.getNext(), this, this, mapPosition, newPos, direction, Dungeon.MoveType.LEADER_MOVE,
				new IAnimListener() {
					public void animEvent() {
						finishedAnimatingAutomaticMove = true;
					}});
		updatePath(newPos);
	}
	
	// Given the leader position, work out the best free place to move to, to be out of the leaders way
	// return null if you cant.  The best way to do that is to work out the opposite direction to the leader
	// and make that as the place you want to move to.
	protected DungeonPosition getOutOfTheWay(DungeonPosition leaderPosition, DungeonPosition leaderTarget) {
		DungeonPosition targetPosition = null;	
		DungeonPosition oppPosition = null;
		
		// find best direction to get to leader that isnt blocked by a wall.
		int direction = findBestDirection(leaderPosition, true, canMove);
		// find the direction to move that is free and in the opposite direction to that just calculated.
		direction = oppositeDirection(direction); 
		if (direction != DungeonPosition.NO_DIR) {
			oppPosition = new DungeonPosition(mapPosition, direction);
		} 
		
		// How many free spaces are there? assign targetPostiion in order of least preference first.
		ArrayList<DungeonPosition> freespots = new ArrayList<DungeonPosition>();
		boolean addFreeSpot = true;
		for (int i=0; i<8; i++) {
			if (canMove(i, false)) {
				DungeonPosition freePos = new DungeonPosition(mapPosition, i);
				if (freePos.equals(leaderTarget)) {
					targetPosition = leaderTarget;
					addFreeSpot = false;
				} 
				if (freePos.equals(oppPosition)) {
					targetPosition = oppPosition;
					addFreeSpot = false;
				} 	
				if (addFreeSpot) {
					freespots.add(freePos);
				}
			}
		}
		
		// the list now contains spots that arent the opposite direction or the proposed leader target
		// so take one of those in preference.
		int size = freespots.size();
		if (size>0) {
			int randomSpot = Randy.getRand(0, freespots.size()-1);
			targetPosition = freespots.get(randomSpot);
		}
		
		return targetPosition;
	}
	
	DungeonPosition leaderTargetPos = null;
	
	private void setAutomaticLeaderTargetPosition(DungeonPosition position) {
		if (leaderTargetPos != null && !leaderTargetPos.equals(position)) {
			dungeonEvents.highlightTile(leaderTargetPos, false);
		}
		
		if (position != null) {
			dungeonEvents.highlightTile(position, true);
		}
		
		leaderTargetPos = position;
	}
	
	private void clearAutomaticLeaderTarget() {
		setAutomaticLeaderTargetPosition(null);  // cannot move towards the target any more.
	}
	
	public boolean isThereAnAutomaticLeaderTarget() {
		if (leaderTargetPos == null) {
			return false;
		} else {
			return true;
		}
	}
	
	public void leaderModeCleared() {
		clearAutomaticLeaderTarget();
		theLeader = null;
	}
	
	public DungeonPosition getAutomaticLeaderTarget() {
		return leaderTargetPos;
	}
	
	public boolean getSolo() {
		return amSolo;
	}
	
	public void setSolo(boolean solo) {
		if (solo == false) {
			amActiveFollower = false;
		}
		amSolo = solo;
	}
	
	private DungeonPosition tempPos = new DungeonPosition(0,0);
	
	// This uses the canMove strategy , but adds a twist - even if the character could move to the position
	// if the position isnt visbile from the current position, they they Shouldnt.
	
	public class ShouldMoveStrategy extends CanMoveStrategy {
		
		public boolean checkMove(int intendedDirection, boolean canBeChar, DungeonPosition targetPos) {

			if (canMove(intendedDirection, canBeChar)) {
				tempPos.x = mapPosition.x;  // this function gets called a lot so this is a little efficiency thing.
				tempPos.y = mapPosition.y;
				tempPos.applyDirection(tempPos, intendedDirection);
				Map map = dungeonQuery.getMap();
				ShadowMap sm = new ShadowMap();
				sm.setMap(map, tempPos, 5);
				return sm.positionIsVisible(targetPos);
			} else {
				return false;
			}
		}
	}

	
	// ATTRIBUTES
	public static final int NO_DIR = -1;
	private static final int MAX_ITEMS = 10;
	public Ability		currentSelectedAbility = null;

	public DungeonPosition leaderPos; // if you are a follower, and you cant see the
								// leader, this is the postion he was last seen,
								// so head there

	private boolean characterIsUsingEye = false;
	
	private UIInfoListenerBag abilityListListeners;
	private UIInfoListenerBag effectListListeners;
	private UIInfoListenerBag characterStatListeners;
	private UIInfoListenerBag itemListListeners;
	private UIInfoListenerBag stealthStatusListeners;
	
	// METHODS
	@Override
	protected void death() {
		effectListListeners.clear();
		super.death();
		isAlive = false;
		boolean shouldDropstuff = turnProcessor.characterDied(this);
		if (shouldDropstuff) {
			dungeonEvents.waitingForAnimToComplete(SequenceNumber.getCurrent(), new IAnimListener() {
				public void animEvent() {
					dropAllPhysicalItems();
				}
			});
		}
	}

	public boolean isAlive() {
		return isAlive;
	}
	
	public static Character getNewCharacter( DungeonPosition p, int index, IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery, TurnProcessor turnProcessor) {
		boolean resultOK = false;
		int random = 0;
		
		while (!resultOK) {
			random = Randy.getRand(1, creatureData.size() - 1);
			Data cd = (Data) creatureData.elementAt(random);
			if (cd.starter != 0) {
				resultOK = true;
			}
		}
		
		return new Character(random, p, index, dungeonEvents, dungeonQuery, turnProcessor);
	}

	public boolean makeAttack(AbilityCommand attack, Creature target, DungeonPosition targetPosition, boolean useSkill, int magicCost, int magicUse) {
		// perform the attack
		boolean result = super.makeAttack(attack, target, targetPosition, useSkill, magicCost, magicUse);
		return result;
	}

	public boolean giveAbility(Creature target, Ability ab, DungeonPosition pos, int magicCost, Creature giver) {
		return super.giveAbility(target, ab, pos, magicCost, giver);
	}

	public boolean isPlayerCharacter() {
		return true;
	}
	
	// Rather than scatter calls to this around Character and Monster, we will make it public
	// and have TurnProcessor call it when it wants to move onto the next creature.
	@Override
	public void endTurn() {
		highlightStatus = HighlightStatus.HIGHLIGHTED;
		if (visualStatusListener != null) {
			visualStatusListener.UIInfoChanged();
		}
		super.endTurn();
		makeNewBagsOfListeners(); // easiest way of throwing away old listeners without concurrency issues.
	}

	@Override
	public void onChangeToAbilitySelectionList(UIInfoListener listener) {
		abilityListListeners.add(listener);
	}

	@Override
	public AbilitySelectionList getAbilitySelectionList() {
		return new AbilitySelectionList(this);
	}

	@Override
	// Ability: targetSelected will execute a targeted ability
	// 
	public boolean abilitySelected(Ability ability) {		
		
		// Things with a magic cost that is too much - no need to process any further - cant use em.
		if (ability.isEnoughMagic(magic) == false) {
			return false;
		}
		
		if (ability.isCool() == false) {
			return false;
		}
		
		effectListListeners.resetList();
		
		// This will set/unset selectable items such as melee weapons and armor.
		// It will activate instant or oneshot items
		// if the ability is targetable, we simply r it as the current
		// the above actions will alter the abilities/effects on the character if it is a setter
		
		boolean abilityWasSetter = ability.abilitySelected(this);  
		
		// If not a setter, then we should record it as the current selected ability,
		// as long as selecting it didnt destroy it (oneshot).
		// if it is a setter, it should update stats  
		if (abilityWasSetter) {
			trySetDefaultMelee();
			updateStats(null, null);  // this will update the character stats and effects due to the selection change
		} else {
			if (ability.isInstant() == false) {
				if (abilities.contains(ability)) {  // toggle the selection status of the ability
					if (currentSelectedAbility != ability) {
						currentSelectedAbility = ability;
					} else {
						currentSelectedAbility = null;
					}
				}
			}
		}
		
		// alert the list to reflect changes in the model.
		abilityListListeners.alertListeners();
		
		// alert effectpresenter to change.
		effectListListeners.alertListeners();
		
		// if a one-shot, then thats it.
		if (ability.invokeFinishesTurn()) {
			turnProcessor.characterEndsTurn(this);
		}
		
		return true;
	}

	@Override
	// If the touch is within LOS of this character, if we are using the eye we tell the dungeon we are looking
	// at that position, otherwise if we have a selected targetable ability that we can use, we use it against
	// that position.  simple.
	public void targetTileSelected(DungeonPosition position) {
		if (LOG) L.log("targetTileselected "+isPlayerCharacter());
		if (dungeonQuery.positionIsInLOSOfCharacter(this, position)) {
			if (canSetLeaderModeTarget(position) == false ) {
				if (characterIsUsingEye) {
					dungeonEvents.setCharacterisUsingEye(true, position, true);  // set the eye position, showing eye animation
				} else if ((currentSelectedAbility != null) && (currentSelectedAbility.isEnoughMagic(magic))){
					// unfortuantely it is possible to target a dead creature, so check first
					boolean canHitTarget = true;
					Creature targetCreature = dungeonQuery.getCreatureAtLocation(position);
					if (targetCreature != null && targetCreature.isDead()) {
						canHitTarget = false;
					}
					if (canHitTarget) {
						currentSelectedAbility.targetSelected(position);
						if (currentSelectedAbility.isCool() == false) {
							currentSelectedAbility = null;
						}
						turnProcessor.characterEndsTurn(this);
					} else {
						if (LOG) L.log("WOULD HAVE CRAHSED");
					}
				}
			}	
		}
	}
	
	public boolean canSetLeaderModeTarget(DungeonPosition position) {
		boolean result = false;
		if (isThereAnAutomaticLeaderTarget()) {
			result = true;
			if (isValidLeaderTile(position)) {
				setAutomaticLeaderTargetPosition(position);
			}
		}
		return result;
	}
	
	private boolean isValidLeaderTile(DungeonPosition position) {
		AtLocation targetTileType = dungeonQuery.whatIsAtLocation(position);
		
		boolean validPos = false;
		switch (targetTileType) {
			case FREE:
				validPos = true;
				break;
			case CHARACTER:
				validPos = true;
				break;
			case HOLE:
				if (creatureCanFly()) {
					validPos = true;
				}
				break;
			default:
				break;
		}
		
		return validPos;
	}
	
	@Override
	// called when the eye tab is opened.  character is using eye, but hasnt targeted anything
	// so eye animation doesnt need to be shown
	public void setCharacterisUsingEye(boolean usingEye) {
		characterIsUsingEye = usingEye;
		dungeonEvents.setCharacterisUsingEye(usingEye, mapPosition, false);
	}

	@Override
	public Character getReleventCharacter() {
		return this;
	}

	@Override
	public void onChangeToCharacterStats(UIInfoListener listener) {
		characterStatListeners.add(listener);
	}

	@Override
	public void onChangeToStealthStatus(UIInfoListener listener) {
		stealthStatusListeners.add(listener);
	}
	
	@Override
	public CreatureStats getCharacterStats() {
		return getCreatureStats();
	}

	@Override
	public boolean updateStats(Ability ab, DungeonPosition pos) {
		boolean statsChanged = super.updateStats(ab, pos);
		
		if (statsChanged) {
			characterStatListeners.alertListeners();
		}
		
		return statsChanged;
	}

	@Override
	public void onChangeToEffectList(UIInfoListener listener) {
		effectListListeners.add(listener);
	}

	@Override
	public EffectList getEffectList() {
		return new EffectList(this);
	}

	@Override
	public void onChangeToInventory(UIInfoListener listener) {
		itemListListeners.add(listener);
	}

	@Override
	public ItemList getItemList() {
		return new ItemList(this, false);
	}

	@Override
	public void performPickup(Ability ability) {
		if (itemPickupSelected(ability)) {
			dungeonEvents.objectPickup(SequenceNumber.getNext(), this, ability, mapPosition);
			abilities.add(0, ability);
			ability.setOwned(this, true);
			itemListListeners.alertListeners();
		} 
	}
	
	@Override	
	public boolean itemPickupSelected(Ability ability) {
		if (getNumberOfPhysicalItemsCarried() < getCapacity()) {
			return canCarry(ability);
		} else {
			return false;
		}
	}
	
	public int getNumberOfPhysicalItemsCarried() {
		int items = 0;

		for (Ability ability : abilities) {
			if (ability.isPhysical()) {
				items++;
			}
		}
		return items;
	}
	
	public int getCapacity() {
		int capacity = MAX_ITEMS;
		if (creature.hands == 0) {
			capacity = 1;
		}
		if (creature.head == 0) {
			capacity = 0;
		}
		return capacity;
	}

	public boolean canCarry(Ability ability) {
		if ((creature.hands == 0) && (ability.ability.needs > Ability.NEEDS_HEAD))  //  if you got no hands, and the thing needs hands, then tough
			return false;
		else
			return true;
	}
	
	// Characters neveer skip turns.
	public boolean canSkipTurn() {
		return false;
	}
	
	// The highlighted ability effect will be either the latest ability added to the character, or the temporary ability
	// with the least turns left to countDown
	// returns true if highlighted ability has changed
	public boolean calculateHighlightAbility()
	{
		EffectList effects = new EffectList(this);
		
		Ability.AbilityEffectType oldHighlightedAbility = creatureStats.abilityEffectType;
		
		AbilityInfo highlightAbility = null;
		int countDown = 1000;
		
		for (AbilityInfo abilityInfo : effects) {
			// select the last added non-temporary ability
			if (countDown == 1000 && abilityInfo.isStat == false && abilityInfo.restrictFromHighlight == false) {
				highlightAbility = abilityInfo; 
			}
			
			// but if there is at least one temp ability, that takes precedence
			if (abilityInfo.expireTime > 0 && abilityInfo.restrictFromHighlight == false) {
				if (abilityInfo.expireTime < countDown) {
					countDown = abilityInfo.expireTime;
					highlightAbility = abilityInfo;  
				}
			}
		}
		
		creatureStats.abilityCountdown = 0;
		
		if (highlightAbility != null) {
			creatureStats.abilityEffectType = highlightAbility.abilityEffects.firstElement();
			creatureStats.abilityCountdown = highlightAbility.expireTime;
		} else {
			creatureStats.abilityEffectType = null;
			creatureStats.abilityCountdown = 0;
		}
		
		if (oldHighlightedAbility == creatureStats.abilityEffectType) {
			return false;
		} else {
			return true;
		}
	}
	
	protected void trySetDefaultMelee() {
		Ability defaultMelee = null;
		boolean meleeEquiped = false;
		for (Ability ability : abilities) {
			if (ability.getAbilityType() == AbilityType.WEAPON) {
				if (defaultMelee == null) {
					defaultMelee = ability;
				}
				if (ability.set) {
					meleeEquiped = true;
				}
			}
		}
		
		if (meleeEquiped == false && defaultMelee != null ){
			defaultMelee.abilitySelected(this);
		}
	}
	
	@Override
	public void itemDropSelected(Ability ability) {
		if (currentSelectedAbility == ability) {
			currentSelectedAbility = null;
		}
		dropObject(ability);
		trySetDefaultMelee();
		itemListListeners.alertListeners();
		
		// if the item dropped was equipped, it could affect headline stats and highlighted ability
		updateStats(null, null);
	}
	
	@Override
	public boolean isCharacterOnStairs() {
		return dungeonQuery.isCreatureOnStairs(this);
	}

	public void resume(Map map) {
		shadowMap.setMap(map, mapPosition, 5);
		creaturePresenter.resume();
		testStealth();
	}


	@Override
	public int respondAttack(AbilityCommand attack, Creature attacker) {
		if (LOG) L.log("attacker: %s", attacker);
		int result = super.respondAttack(attack, attacker);
		
		if (result >= 0) {
			characterStatListeners.alertListeners();
		}
		
		return result;
	}
	
	int charCount;

	@Override
	public String toString() {
		return getCreature().name+"C"+charCount+")";
	}
	
	private void addDefendingAbility() {
		Ability defendingAbility = new Ability(Ability.getIdForName("defending"), null, 1, dungeonEvents, dungeonQuery);
		addAbility(defendingAbility, null);
		defendingAbility.abilitySelected(this);
	}
	
	public void defending() {
		addDefendingAbility();
		Vector<Ability.AbilityEffectType> shield = new Vector<Ability.AbilityEffectType>();
		shield.add(Ability.AbilityEffectType.DEFENDING);
		dungeonEvents.abilityAdded(SequenceNumber.getNext(), this, shield, mapPosition);
		turnProcessor.characterEndsTurn(this);
	}
	
	public void stealthToggleSelected() {
		if (stealthStatus == StealthStatus.HIDING) {
			notHiding(this);
		} else {
			hide(this);
		}

		stealthStatusListeners.alertListeners();
	}
	
	private boolean fallComplete = true;
	
	public void fallStarted() {
		fallComplete = false;
	}
	
	public void fallComplete() {
		fallComplete = true;
	}
	
	public void testStealth() {
		if (!fallComplete) {
			return;
		}
		
		boolean canHide = canHide();
		
		switch (stealthStatus) {
		case HIDING:
			if (canHide == false) {
				notHiding(this);
				stealthStatus = StealthStatus.HIDING_IMPOSSIBLE;
				stealthStatusListeners.alertListeners();
				// special case when a hidden character is uncovered, we have to reset its own visual flags.
//				if (currentlySeenByMonster == false) {
//					dungeonQuery.checkForCloseToMonster(this);
//				}
			}
			break;
		case HIDING_POSSIBLE:
			if (canHide == false) {
				stealthStatus = StealthStatus.HIDING_IMPOSSIBLE;
				stealthStatusListeners.alertListeners();
			}
			break;
		case HIDING_IMPOSSIBLE:
			if (canHide) {
				stealthStatus = StealthStatus.HIDING_POSSIBLE;
				stealthStatusListeners.alertListeners();
			}
			break;
		}
		
		if (stealthStatus != StealthStatus.HIDING && currentlySeenByMonster) {
			setCharacterLight(true);
		} else {
			setCharacterLight(false);
		}
	}
	
	public boolean canHide() {
		if (inLOSOfMonster) {
			return super.canHide();
		} else {
			return true;
		}
	}
	
	private void setCharacterLight(boolean on) {
		if (on) {
			float lightStrength = Light.SPOTTED_CHARCTER_LIGHT_STRENGTH;
			if (amClosestToMonster) {
				lightStrength = Light.CLOSEST_CHARCTER_LIGHT_STRENGTH;
			} 
			
			if (light == null || light.getStrength() != lightStrength) {
				light = new Light(mapPosition, Light.WALL_TORCH_RANGE, lightStrength, false);
				creaturePresenter.lightChanged();
			}
		} else {
			if (light != null) {
				light = null;
				creaturePresenter.lightChanged();
			}
		}
	}
	
	@Override
	public Light getLight() {
		return light;
	}
	
	public void  setCurrentlySeenByMonster(boolean val) {
		currentlySeenByMonster = val;
	}
	
	public boolean getCurrentlySeenByMonster() {
		return currentlySeenByMonster;
	}
	
	public void  setInLOSOfMonster(boolean val) {
		inLOSOfMonster = val;
	}
	
	public boolean getInLOSOfMonster() {
		return inLOSOfMonster;
	}
	
	public void setAmClosestToMonster(boolean val) {
		amClosestToMonster = val;
	}

}

///**
// * return 0 if there are no characters in LOS
// * otherwise return the distance to the one that is furthest away
// */
//private int distanceToFurthestCharacterInLOS() {
//	List<Creature> creaturesInSight = dungeonQuery.getCreaturesVisibleFrom(mapPosition, 5);  
//	int greatestDistance = 0;
//	for (Creature creature : creaturesInSight ) {
//		if ((creature instanceof Character) && (creature != this)) { // we have another character in sight
//			// Work out which visible characters are farthest away.
//			int distance = mapPosition.distanceTo(creature.getPosition());
//			if (distance > greatestDistance) {
//				greatestDistance = distance;
//			}
//		}
//	}
//	return greatestDistance;
//}
