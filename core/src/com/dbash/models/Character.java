package com.dbash.models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;

import com.dbash.models.Ability.AbilityType;
import com.dbash.models.IDungeonQuery.AtLocation;
import com.dbash.presenters.tabs.AbilitySelectionList;
import com.dbash.util.Randy;
import com.dbash.util.SequenceNumber;


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
		public Character inTheWayCharacter;
	}
	
	//Sprite manSprite = FileManager.createSpriteFromSingleImage("res/fresh/b.png", 1.0f);
	LinkedList<DungeonPosition> path = new LinkedList<DungeonPosition>();
	boolean amActiveFollower;
	Character theLeader;
	// This characters personal shadowmap.
	public ShadowMap shadowMap;
	boolean isAlive = true;
	
	// INTERFACE
	public Character(int creatureId, DungeonPosition p, int charNumber, IDungeonEvents dungeonEvents, 
			IDungeonQuery dungeonQuery, TurnProcessor turnProcessor) {
		super(creatureId, p, dungeonEvents, dungeonQuery, turnProcessor); // this calls the constructor of the Creature class
		
		this.displayedNumber = charNumber;
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
		setTargetPosition(null); 
	}
	
	private void initChar(final TurnProcessor turnProcessor) {
		makeNewBagsOfListeners();
		creatureStats.isCharacter = true;
		shadowMap = new ShadowMap(this);
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
		effectListListeners = new UIInfoListenerBag();
		characterStatListeners = new UIInfoListenerBag();
		itemListListeners = new UIInfoListenerBag();
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

		// This is only set to true when a character makes an active following move
		amActiveFollower = false;
		
		// Tell whoever that a character is having a turn, unless they are a follower.
		theLeader = turnProcessor.getCurrentLeader();
			if ((theLeader == null) || (theLeader == this)) {
				alertThatIAmTheCurrentCharacter();
			}
		
		// Determine if an automatic-turn happens: (follower movement or leader-waiting)
		if (madeAutomaticTurn(theLeader)) {
			turnProcessor.characterEndsTurn(this);
			return;
		}
		
		// If we get to here, we have to wait for player input
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
			
		return madeAutomaticTurn;
	}

	// Need a function here that takes a target position and returns a direction to move to get to that position
	// and whether or not it can be reached immediately or requires a complaint to a character in the way.
	// A path free of other characters is always chosen first.
	// If a complaint was neccessary, then it will complain.
	private BestDir calcBestDir(DungeonPosition targetPosition) {
		BestDir bestDir = new BestDir();
		bestDir.setDirection(findBestDirection(targetPosition, false));
		if (bestDir.hasFreeOption == false) {
			bestDir.setDirection(findBestDirection(targetPosition, true));
			if (bestDir.hasFreeOption) {
				// there is a way, but it is occupied by a character.
				bestDir.hasFreeOption = false; // well, there is a way, but it is blocked, so set this false.
				DungeonPosition desiredPosition = new DungeonPosition(mapPosition, bestDir.direction);
				bestDir.inTheWayCharacter = (Character) dungeonQuery.getCreatureAtLocation(desiredPosition);
				bestDir.inTheWayCharacter.characterIsInWay(bestDir.direction);
				bestDir.didComplain = true;
			}
		}
		
		return bestDir;
	}
	
	
	// OK, so we can do one of three things, in order of preference.
	// a) respond to a complain from another character to move in a specific direction.
	// b) walk directly to the leader
	// c) walk directly to the last known position of the leader, via their path
	// d) do nothing
	private void doFollowerProcessing(Character theLeader)
	{
		BestDir bestDir = new BestDir();
		DungeonPosition targetPosition;

		// Tell the leader there is at least one active follower.
		amActiveFollower = true;
		
		// If the character was asked to move, try that first
		if (askedDirection != DungeonPosition.NO_DIR) {
			targetPosition = new DungeonPosition(mapPosition, askedDirection);
			bestDir = calcBestDir(targetPosition);
			if (bestDir.hasFreeOption) {
				askedDirection = DungeonPosition.NO_DIR;  // will move to a clear spot, so complaint heeded.
 			}
			if (bestDir.didComplain) {
				return;  // we have to move in a certain position, but its blocked, so complain and wait.
			}
		} 

		// if the character was asked to move, but there is no way, or the character was not asked to move
		// then set the targetPosition based on the Leader instead, and try to move or complain.
		if (bestDir.hasFreeOption == false) {
			int distanceToLeader = mapPosition.distanceTo(theLeader.getPosition());
			// ask the leader for the nearest position to his current one from his path that can be seen from here 
			targetPosition = theLeader.getLatestPathPositionIcanSee(this);
			
			if (((targetPosition != null) && (distanceToLeader > 1))) {
				bestDir = calcBestDir(targetPosition);
				if (bestDir.didComplain) {
					return;  // we have to move in a certain position, but its blocked, so complain and wait.
				}
			}
		}
		
		// Having got to here, we either have a free option, or we cant move at all.
		if (bestDir.hasFreeOption) {
			DungeonPosition newPosition =  new DungeonPosition(mapPosition, bestDir.direction);
			updatePath(newPosition);
			Character releventChar = null;
			// can the leader see this?
			// visible to the currently focused character, then there is no need to change focus.
			if (dungeonQuery.positionIsInLOSOfCharacter(theLeader, mapPosition) ||
					dungeonQuery.positionIsInLOSOfCharacter(theLeader, newPosition)) {
				releventChar = theLeader;
			}
			dungeonEvents.creatureMove(SequenceNumber.getNext(), releventChar, this, mapPosition, newPosition, bestDir.direction,  Dungeon.MoveType.FOLLOWER_MOVE, null);
		} else {
			amActiveFollower = false;  // no trail to follow.  Im lost.
		}
	}
	
	public void setPosition(DungeonPosition newPosition) {
		super.setPosition(newPosition);
		if (shadowMap != null) {
			shadowMap.updateCenterPos(newPosition);
		}
	}
	
	private static final int pathLength = 20;
	
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
	
	private void setTargetPosition(DungeonPosition position) {
		if (leaderTargetPos != null && !leaderTargetPos.equals(position)) {
			dungeonEvents.highlightTile(leaderTargetPos, false);
		}
		
		if (position != null) {
			dungeonEvents.highlightTile(position, true);
		}
		
		leaderTargetPos = position;
	}
	
	// Sometimes the leader has to wait for his slow followers to catch up, and sometimes they are close by so he can
	// move (under player direction or unless he is himself marching on his way to leaderTargetPos, in which case he 
	// will automatically move towards that instead.)
	// returns true if the character made an automatic turn, false otherwise.
	private boolean doLeaderProcessing()
	{
		// Leader processing does not occur if the leader is still moving, so we dont get ahead of ourselves.
		if (leaderFinishedMoving == false) {
			return true;
		}
		
		// a.  Are there any followers in LOS?  If not, wait for player input (return false)
		List<Creature> creaturesInSight = dungeonQuery.getCreaturesVisibleFrom(mapPosition, 5);  
		int greatestDistance = 0;
		for (Creature creature : creaturesInSight ) {
			if ((creature instanceof Character) && (creature != this)) { // we have another character in sight
				// Work out which visible characters are farthest away.
				int distance = mapPosition.distanceTo(creature.getPosition());
				if (distance > greatestDistance) {
					greatestDistance = distance;
				}
			}
		}
		
		boolean madeAutomaticTurn = true;
		
		// There are no active followers out there, so no need to wait, because nothing will happen. 
		if (turnProcessor.anyActiveFollowers() == false) {
			madeAutomaticTurn = false;
		}
		
		// b.  If there are, are they close?  If all followers are <= 2 tiles away then wait for player input (return false)
		if ((greatestDistance > 0) && (greatestDistance < 3)) { 
			madeAutomaticTurn = false;
		}
		
		boolean complained = false;
		
		// c. If we are in a position where normally the leader would wait for player input
		// (made Automatic turn = false) then test
		// to see whether we can move towards leaderTargetPos instead.
		if  ( leaderTargetPos != null) {
			if (mapPosition.equals(leaderTargetPos)) {
				setTargetPosition(null); 
			} else {
				int direction = DungeonPosition.NO_DIR;
				if (dungeonQuery.positionIsInLOSOfCharacter(this, (leaderTargetPos))) {
					direction = findBestDirection(leaderTargetPos, false);
					if (direction == DungeonPosition.NO_DIR) {
						direction = findDirectionOfCharacterInWay(leaderTargetPos);
						if (direction != DungeonPosition.NO_DIR) {
							// We have just identified that the way home has a character in front of it.
							turnProcessor.complain(mapPosition, direction);
							complained = true;
							madeAutomaticTurn = true;
						}
					}
				}
				
				// We have either complained, got stuck, or have a way to move.
				if (complained == false) {
					if (direction == DungeonPosition.NO_DIR) {
						setTargetPosition(null);  // cannot move towards the target any more.
					} else {
						DungeonPosition newPos = new DungeonPosition(mapPosition, direction);
						leaderFinishedMoving = false;
						dungeonEvents.creatureMove(SequenceNumber.getNext(), this, this, mapPosition, newPos, direction, Dungeon.MoveType.LEADER_MOVE,
								new IAnimListener() {
									public void animEvent() {
										leaderFinishedMoving = true;
									}});
						updatePath(leaderTargetPos);
						madeAutomaticTurn = true;
					}
				}
			}
		}
		
		return madeAutomaticTurn;
	}
	
	int askedDirection = DungeonPosition.NO_DIR;
	/**
	 * When any character wants to move in a direction, it sends this message to a character in its way.
	 */
	public void getOutOfTheWay(int direction) {
		askedDirection = direction;
	}
	

	// ATTRIBUTES
	public static final int NO_DIR = -1;

	private static final int MAX_ITEMS = 10;

	public Ability		currentSelectedAbility = null;
	

	private int charId;

	public DungeonPosition leaderPos; // if you are a follower, and you cant see the
								// leader, this is the postion he was last seen,
								// so head there

	//public static SoundControl soundControl;

	private int displayedNumber;

	private boolean characterIsUsingEye = false;
	
	private UIInfoListenerBag abilityListListeners;
	private UIInfoListenerBag effectListListeners;
	private UIInfoListenerBag characterStatListeners;
	private UIInfoListenerBag itemListListeners;
	
	public int getDisplayedNumber() {
		return displayedNumber;
	}
	
	// METHODS
	@Override
	protected void death() {

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
	
	public static Character getNewCharacter(int maxLevel, int minLevel, boolean humanoid, DungeonPosition p,
			int index, IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery, TurnProcessor turnProcessor) {
		boolean resultOK = false;
		int random = 0;
		
		while (!resultOK) {
			random = Randy.getRand(1, creatureData.size() - 1);
			
			Data cd = (Data) creatureData.elementAt(random);

			if ((humanoid && cd.humanoid == 1) || !humanoid) {
				if ((cd.value <= maxLevel) && (cd.value >= minLevel))
					resultOK = true;
			}
		}
		
		return new Character(random, p, index, dungeonEvents, dungeonQuery, turnProcessor);
	}

	public boolean makeAttack(AbilityCommand attack, Creature target,
			DungeonPosition targetPosition, boolean useSkill, int magicCost,
			int magicUse) {
		// perform the attack
		boolean result = super.makeAttack(attack, target, targetPosition,
				useSkill, magicCost, magicUse);
		
		return result;
	}

	public boolean giveAbility(Creature target, Ability ab, DungeonPosition pos,
			int magicCost, Creature giver) {
		return super.giveAbility(target, ab, pos, magicCost, giver);
	}


	private int getNumberOfPhysicalItemsCarried() {
		int items = 0;

		for (Ability ability : abilities) {
			if (ability.isPhysical()) {
				items++;
			}
		}
		return items;
	}

	public boolean isPlayerCharacter()
	{
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

	public void setDisplayedNumber(int i) {
		this.displayedNumber = i;
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
		
		// This will set/unset selectable items such as melee weapons and armor.
		// It will activate instant or oneshot items
		// if the ability is targetable, we simply r it as the current
		// the above actions will alter the abilities/effects on the character if it is a setter
		
		boolean abilityWasSetter = ability.abilitySelected(this);  
		
		// If not a setter, then we should record it as the current selected ability,
		// as long as selecting it didnt destroy it (oneshot).
		// if it is a setter, it should update stats  TODO
		if (abilityWasSetter) {
			trySetDefaultMelee();
			updateStats(null, null);  // this will update the character stats and effects due to the selection change
			//effectListListeners.alertListeners();
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
		if (dungeonQuery.positionIsInLOSOfCharacter(this, position)) {
			if (characterIsUsingEye) {
				dungeonEvents.setCharacterisUsingEye(true, position, true);  // set the eye position, showing eye animation
			} else {
				if ((currentSelectedAbility != null) && (currentSelectedAbility.isEnoughMagic(magic))){
					currentSelectedAbility.targetSelected(position);
					turnProcessor.characterEndsTurn(this);
				}
			}	
		}
	}

	/**
	 * If the character is the leader, then the up position becomes important, as the leader will try to move directly
	 * to that location.
	 */
	DungeonPosition leaderTargetPos = null;
	boolean leaderFinishedMoving = true;
	@Override
	public void movementGesture(int direction, DungeonPosition targetPosition) {
		theLeader = turnProcessor.getCurrentLeader();
		
		AtLocation targetTileType = dungeonQuery.whatIsAtLocation(targetPosition);
		
		if (theLeader == this && targetTileType == AtLocation.FREE) {
			if (dungeonQuery.positionIsInLOSOfCharacter(this, (targetPosition))) {
				direction = findBestDirection(targetPosition, false);
			}
		}
		
		IAnimListener complete = null;
		
		DungeonPosition position = new DungeonPosition(mapPosition, direction);
		switch (dungeonQuery.whatIsAtLocation(position))
		{	
			case FREE:
				Dungeon.MoveType mType = Dungeon.MoveType.NORMAL_MOVE;
				if (theLeader == this) {
					mType = Dungeon.MoveType.LEADER_MOVE;
					if (targetTileType == AtLocation.FREE) {
						setTargetPosition(targetPosition);
						leaderFinishedMoving = false;
						//System.out.println("GES leaderFinisedMoving = false;");
						complete = new IAnimListener() {
							public void animEvent() {
								leaderFinishedMoving = true;
								//System.out.println("GES leaderFinisedMoving = false;");
							}};
					}
				}
				dungeonEvents.creatureMove(SequenceNumber.getNext(), this, this, mapPosition, position, direction, mType, complete);
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

	public boolean characterIsInWay(int intendedDirection)
	{
		switch (dungeonQuery.whatIsAtLocation(new DungeonPosition(mapPosition, intendedDirection))) {
			case CHARACTER:
				return true;
			default:
				return false;		
		}
	}
	
	// First step would be to use findBestDirection to get a free path, and if thats not possible, find out if
	// there is a character in the way, by calling this, and the direction the character is in.
	// yes, I know, pass strategy in or whatever,  fuck it Im tired.
	public int findDirectionOfCharacterInWay(DungeonPosition characterPosition)
	{
		int direction = DungeonPosition.NO_DIR;

		// means move up or down.  unless there is something in the way, in which case deviate around it.
		if (characterPosition.x == mapPosition.x)
		{
			if (characterPosition.y > mapPosition.y)
			{
				if (characterIsInWay(DungeonPosition.NORTH))
					direction = DungeonPosition.NORTH;
				else if (characterIsInWay(DungeonPosition.NORTHWEST))				
					direction = DungeonPosition.NORTHWEST;
				else if (characterIsInWay(DungeonPosition.NORTHEAST))
					direction = DungeonPosition.NORTHEAST;	
			}
			else
			{
				if (characterIsInWay(DungeonPosition.SOUTH))
					direction = DungeonPosition.SOUTH;
				else if (characterIsInWay(DungeonPosition.SOUTHWEST))				
					direction = DungeonPosition.SOUTHWEST;
				else if (characterIsInWay(DungeonPosition.SOUTHEAST))
					direction = DungeonPosition.SOUTHEAST;
			}
		}
		// character is west of current position
		else if (characterPosition.x < mapPosition.x)
		{
			if (characterPosition.y == mapPosition.y)
			{
				if (characterIsInWay(DungeonPosition.WEST))
					direction = DungeonPosition.WEST;
				else if (characterIsInWay(DungeonPosition.SOUTHWEST))				
					direction = DungeonPosition.SOUTHWEST;
				else if (characterIsInWay(DungeonPosition.NORTHWEST))
					direction = DungeonPosition.NORTHWEST;
			}
			else if (characterPosition.y > mapPosition.y)  // character is north of current pos
			{
				if (characterIsInWay(DungeonPosition.NORTHWEST))
					direction = DungeonPosition.NORTHWEST;
				else if (characterIsInWay(DungeonPosition.WEST))				
					direction = DungeonPosition.WEST;
				else if (characterIsInWay(DungeonPosition.NORTH))
					direction = DungeonPosition.NORTH;
			}
			else
			{
				if (characterIsInWay(DungeonPosition.SOUTHWEST))
					direction = DungeonPosition.SOUTHWEST;
				else if (characterIsInWay(DungeonPosition.WEST))				
					direction = DungeonPosition.WEST;
				else if (characterIsInWay(DungeonPosition.SOUTH))
					direction = DungeonPosition.SOUTH;
			}
		}
		// character is east of current position
		else if (characterPosition.x > mapPosition.x)
		{
			if (characterPosition.y == mapPosition.y)
			{
				if (characterIsInWay(DungeonPosition.EAST))
					direction = DungeonPosition.EAST;
				else if (characterIsInWay(DungeonPosition.SOUTHEAST))				
					direction = DungeonPosition.SOUTHEAST;
				else if (characterIsInWay(DungeonPosition.NORTHEAST))
					direction = DungeonPosition.NORTHEAST;
			}
			else if (characterPosition.y > mapPosition.y)  // character is north of current pos
			{
				if (characterIsInWay(DungeonPosition.NORTHEAST))
					direction = DungeonPosition.NORTHEAST;
				else if (characterIsInWay(DungeonPosition.NORTH))				
					direction = DungeonPosition.NORTH;
				else if (characterIsInWay(DungeonPosition.EAST))
					direction = DungeonPosition.EAST;
			}
			else
			{
				if (characterIsInWay(DungeonPosition.SOUTHEAST))
					direction = DungeonPosition.SOUTHEAST;
				else if (characterIsInWay(DungeonPosition.SOUTH))				
					direction = DungeonPosition.SOUTH;
				else if (characterIsInWay(DungeonPosition.EAST))
					direction = DungeonPosition.EAST;
			}
		}

		return direction;
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
		return new ItemList(this);
	}

	@Override
	public boolean itemPickupSelected(Ability ability) {
		boolean pickupAllowed = true;

		// first check carry limit
		int items = getNumberOfPhysicalItemsCarried();
		if (items >= MAX_ITEMS)
			pickupAllowed = false;

		if ((creature.hands == 0) && (ability.ability.needs > Ability.NEEDS_HEAD))  //  if you got no hands, and the thing needs hands, then tough
			pickupAllowed = false;
		
		if (pickupAllowed) {
			dungeonEvents.objectPickup(SequenceNumber.getNext(), this, ability, mapPosition);
			abilities.add(ability);
			ability.setOwned(this, true);
			itemListListeners.alertListeners();
		} 
		
		return pickupAllowed;
	}

	// Characters neveer skip turns.
	public boolean canSkipTurn() {
		return false;
	}
	
	// The highlighted ability effect will be either the latest ability added to the character, or the temporary ability
	// with the least turns left to countDown
	// returns true if highlighted ability has changed
	protected boolean processAbilities()
	{
		EffectList effects = new EffectList(this);
		
		Ability.AbilityEffectType oldHighlightedAbility = creatureStats.abilityEffectType;
		
		AbilityInfo highlightAbility = null;
		int countDown = 1000;
		
		for (AbilityInfo abilityInfo : effects) {
			// select the last added non-temporary ability
			if (countDown == 1000 && abilityInfo.isStat == false) {
				highlightAbility = abilityInfo; 
			}
			
			// but if there is at least one temp ability, that takes precedence
			if (abilityInfo.expireTime > 0) {
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
	}

}
