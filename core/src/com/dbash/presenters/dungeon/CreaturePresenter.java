package com.dbash.presenters.dungeon;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.Character;
import com.dbash.models.Creature;
import com.dbash.models.Data;
import com.dbash.models.Dungeon;
import com.dbash.models.Dungeon.MoveType;
import com.dbash.models.DungeonPosition;
import com.dbash.models.IAnimListener;
import com.dbash.models.IPresenterCreature;
import com.dbash.models.IPresenterCreature.HighlightStatus;
import com.dbash.models.Light;
import com.dbash.models.Monster;
import com.dbash.models.PresenterDepend;
import com.dbash.models.UIInfoListener;
import com.dbash.platform.AnimationView;
import com.dbash.platform.Audio;
import com.dbash.platform.ImageView;
import com.dbash.platform.TextImageView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.AnimOp;
import com.dbash.util.Rect;


// CreaturePresenters listen to their creature model object for changes in visual status such as position and highlight
// However, they also get told to play animation events from the dungeon model. 
//
//- For these animation events, dungeon passes it to creaturePresenter
//- CreaturePresenter then creates AnimationView of itself walking form its previous location to its new location, or whatever
//- While this animation is in effect, CreaturePresenter does not draw itself at the new location.
//- when animation finished playing, CreaturePresenter now draws itself in its new location.
//
// Why doesnt the creature tell the creaturePresetner to animate directly?  because only the MapPresenter actually knows
// if a particular tile and/or needs to bother to show an animation or not, because it knows where the viewport currently is.
//
//DRAWING CREATURES
// Location and hence LocationPresenter gets 'realtime' model updates of creatures locations, and MapPresenter only
// tells those tiles that are visible in the shadowmap(s) it is currently displaying to draw creatures.  i.e. only those
// creatures either sitting in a visible location get to draw themselves.
// Creature presenter takes care of the nasty business of working out whether to draw a static image of the creature when 
// asked to, or to display an animation of the creature moving form one tile to the next when it gets a move command.
// the tricky bit there is that sometimes the move command is queued, so a static image must be drawn while waiting for the anim to play
// but it cant be a static image in a position 'ahead' of where an unplayed animation will display it.
//
// display state for a creature is -
// STATIONARY - display static image at model [dest] position
// WAITING TO FALL - do not display anything
// WAITING_TO_MOVE - display at [source] position because model had already updated [dest] position]
// WAITING_TO_ATTACK - display static at [dest] position.
// FOLLOWER_MOVE_AFTER_FALL - edge case.  we are waiting to move, but we are also waiting to fall in.  dont display anything
// ANIMATING - display animation instead of static image.
public class CreaturePresenter {
	
	public enum VisualState {
		STATIONARY,
		WAITING_TO_FALL,
		WAITING_TO_MOVE, 
		WAITING_TO_ATTACK,
		WAITING_TO_DIE, 
		WAITING_TO_GO_DOWN_STAIRS,
		ANIMATING    // any animation that is not moving or falling
	}
	
	// this is the position of the creature according to the presenter
	private DungeonPosition sourcePosition;  // recorded when a move command is received.
	private DungeonPosition destPosition; // always updated by observing changes in the model (presenterCreature)
	private VisualState visualState;
	
	private UIDepend gui;
	private PresenterDepend model;
	private AnimationView highlightAnimation;
	private IPresenterCreature creature;
	private ImageView staticImage;
	private ImageView shadowImage;
	private Rect area;
	private MapPresenter mapPresenter;
	private String name;
	private ImageView highlightImage;
	private AnimationView myPreviousAnim;
	private Light light;
	private boolean isCharging;
	
	public CreaturePresenter(UIDepend gui, PresenterDepend model, IPresenterCreature creature, MapPresenter mapPresenter) {
		this.gui = gui;
		this.creature = creature;
		this.name = creature.getNameUnderscore();
		this.model = model;
		this.myPreviousAnim = null;
		isCharging = false;
		
		if (creature instanceof Character) {
			this.visualState = VisualState.WAITING_TO_FALL;
			light = new Light(creature.getPosition(), 5, Light.CHAR_LIGHT_STRENGTH, false); // Characters have lights.
		} else {
			this.visualState = VisualState.STATIONARY;
		}
		   
		// register for changes to the underlying model..
		final IPresenterCreature theCreature = creature;
		creature.onChangeToVisualStatus(new UIInfoListener() {
			public void UIInfoChanged() {
				destPosition = theCreature.getPosition();
				updateStaticImageArea();
				updateHighlightAnimation();
			}
		});
		
		this.mapPresenter = mapPresenter;
		this.area = new Rect(0,0,0,0);
		this.sourcePosition = creature.getPosition();
		this.destPosition = creature.getPosition();
		highlightImage = new ImageView(gui, "RANGED_DOT", this.area);
		updateHighlightAnimation();
		setStaticImage(DungeonPosition.SOUTH);
	}
	public CreaturePresenter(){};
	
	public void resume() {
		this.visualState = VisualState.STATIONARY;
		mapPresenter.addLight(light);
	}
	
	private void updateHighlightAnimation() {
		final float startScale = .7f;
		final float endScale = .9f;
		final float startAlpha = .2f;
		final float endAlpha = .3f;
		final float halfAlpha = startAlpha + (endAlpha*2 - startAlpha)/2;
		final Rect fromRect = new Rect(this.area, startScale);
		final Rect toRect = new Rect(this.area, endScale);
		final Rect halfRect = new Rect(this.area, (startScale+endScale)/2f);
		highlightAnimation = new AnimationView(gui, "RANGED_DOT", fromRect, toRect, startAlpha, endAlpha*2, DungeonAreaPresenter.highlightPeriod, -1, null);
		// listen to itself for 50% wax/wane cycle
		highlightAnimation.onPercentComplete(50, new IAnimListener() {
			public void animEvent() {
				highlightAnimation.setAlpha(halfAlpha, startAlpha, DungeonAreaPresenter.highlightPeriod/2);
				highlightAnimation.setRects(halfRect, fromRect, DungeonAreaPresenter.highlightPeriod/2);
			}
		});
		highlightAnimation.startPlaying();  // plays endlessly.  draw controlled by this presenter.
	}
	
	private void setStaticImage(int dir) {
		String staticImageString = getFullName(name, "walk", dir);
		staticImage = new ImageView(gui, staticImageString, 1, area);
		shadowImage = new ImageView(gui, "shadow", 1, area);
		updateStaticImageArea();
	}
	
	private void updateStaticImageArea() {
		switch (visualState) {
			case WAITING_TO_MOVE:
			case WAITING_TO_DIE:
				area = makeDrawingRectFromPosition(sourcePosition);
				break;
			default:
				area = makeDrawingRectFromPosition(destPosition);
				break;
		}

		staticImage.setArea(area);
		shadowImage.setArea(area);
		Rect hArea = new Rect(area, 0.75f);
		highlightImage.setArea(hArea);
	}
	
	// static images drawn here, whereas animated stuff delegated to the animqueue which will call draw when it wants to.
	public void draw(SpriteBatch spriteBatch, float alpha) {
		switch (visualState) {
			case STATIONARY:
			case WAITING_TO_MOVE:
			case WAITING_TO_ATTACK:
			case WAITING_TO_DIE:
			case WAITING_TO_GO_DOWN_STAIRS:
				if (creature.getHighlightStatus() == HighlightStatus.HIGHLIGHTED) {  
					highlightImage.draw(spriteBatch, 0.18f*alpha);
				} else if (creature.getHighlightStatus() == HighlightStatus.CURRENTLY_IN_FOCUS) {
					highlightAnimation.draw(spriteBatch);
				}
			
				shadowImage.draw(spriteBatch, alpha);
				staticImage.draw(spriteBatch, alpha);
				break;
			case WAITING_TO_FALL:
				break;
			case ANIMATING:
				break;
			default:
				break;			
		}
	}

	// Dungeon instructs this CreaturePresenter to play this animation rather than draw its normal static image.
	// Because it knows when a creature moving is visible.
	//
	// Its possible if the creature is a following character that it could get multiple moves commands, which means they have to clean up 
	// after themselves correctly with respect to setting the creature location and state and so on.
	//
	// Falling in and moving are the two commands that move the creature in the model.  In the case of falling, its
	// from nowhere to the landing spot, and in the case of moving its from the source tile to the target tile.
	// While waiting for these animations takes place, the creature must be statically drawn in the source position (or not
	// at all if falling).  
	//
	// When the move command is received, the move command must update the statically drawn image with its own source
	// position, and when multiple move commands are received, the latter move commands must add a complete block to the 
	// preceding one that sets the source position to its own source position.
	int moves_waiting = 0;
	
	public void creatureMove(int sequenceNumber, DungeonPosition fromPosition, final DungeonPosition toPosition, int direction, 
			MoveType moveType, float moveTime, IAnimListener animCompleteListener) {
		//final CreaturePresenter selfi = this;
		final DungeonPosition newSourcePos = fromPosition;
		if (moveType == Dungeon.MoveType.CHARGE_MOVE) {
			isCharging = true;
		}
		//System.out.printlnln(this+" command to move with moves waiting " + moves_waiting++ +" and state "+visualState.toString());
		// depends on the state as to what we do - can be multiple follower mode moves, falling in etc...
		// these are the possible states for a single character that has been sent a MOVE command:
		switch (visualState) {
			
			// if we are waiting to move, or moving, we need to add to its complete block to set up the right.
			case WAITING_TO_MOVE:
			case WAITING_TO_FALL: 
			case ANIMATING:
				myPreviousAnim.onComplete( new IAnimListener() {
					public void animEvent() {
						sourcePosition = newSourcePos;
						visualState = VisualState.WAITING_TO_MOVE;
						updateStaticImageArea();
					}
				});
				break;
				
			// stationary the move command can set the source position straight away.
			case STATIONARY: 
				myPreviousAnim = null; // start of a new sequence of moves
				sourcePosition = newSourcePos;
				visualState = VisualState.WAITING_TO_MOVE;
				updateStaticImageArea();
			default:
				break;
		}
		
		// set up animation
		String animToUse = "walk";
		if (isCharging) {
			animToUse = "attack";
		}
		final Rect toRect = makeDrawingRectFromPosition(toPosition);
		final Rect fromRect = makeDrawingRectFromPosition(fromPosition);
		final IAnimListener tpListen = animCompleteListener;
		final int dir = direction;
		final AnimationView moveAnim = new AnimationView(gui, getFullName(name, animToUse, direction), fromRect, toRect, 1f, 1f, moveTime, 1, new IAnimListener() {
			public void animEvent() {
				visualState = VisualState.STATIONARY;
				moves_waiting--;
				//System.out.printlnln("" + selfi + " ending");
				setStaticImage(dir);
				if (tpListen != null) {
					tpListen.animEvent(); // tell anyone who cares
				}
				if (light != null) {
					mapPresenter.moveLight(light, toPosition);
				}
			}
		});
		
		changeStateToAnimatingWhenStarted(moveAnim);
		
		// how and to what we chain this move anim depends on these rules:
		// 1. if the current move is a LEADER_MODE move, or a normal move, its chained sequentially to the last 
		//    anim in the queue
		// 2. if the current move is a FOLLOWER move, if myPreviousMove is also a FOLLOWER or FALL, it is chained to
		//    that animation sequentially, otherwise it is chained to the last anim in the queue concurrently.
		moveAnim.sequenceNumber = sequenceNumber;
		switch (moveType) {
			case FOLLOWER_MOVE:
				moveAnim.animType = AnimOp.AnimType.FOLLOWER_MOVE;
				if (myPreviousAnim != null && (myPreviousAnim.animType == AnimOp.AnimType.FOLLOWER_MOVE ||
					myPreviousAnim.animType == AnimOp.AnimType.FALL_IN)) {
					// chain sequentially to the previous anim for this particular
					model.animQueue.add(moveAnim, false);
					if (visualState == VisualState.WAITING_TO_MOVE) {
						//System.out.printlnln(this+" will fail "+"prev type "+myPreviousAnim.animType+" completed "+myPreviousAnim.hasCompleted);
						
					}
					myPreviousAnim.onComplete(new IAnimListener() {
						public void animEvent() {
							//System.out.printlnln( selfi + " starting chained move for follower");
							moveAnim.startPlaying();
						}
					});
				} else {
					//System.out.printlnln("this is the one fialing to start");
					model.animQueue.chainConcurrent(moveAnim, 20f, false); // slight delay in following for asthetic reasons
				}
				break;
			case LEADER_MOVE:
				moveAnim.animType = AnimOp.AnimType.LEADER_MOVE;
				model.animQueue.add(moveAnim, false);
				moveAnim.startPlaying();
				//model.animQueue.chainConcurrent(moveAnim, 0f, false);
				break;
			default:
				moveAnim.animType = AnimOp.AnimType.MOVE;
				model.animQueue.chainSequential(moveAnim, false);
				break;
		}
		
		// Moving shadow animation
		final AnimationView shadowAnim = new AnimationView(gui, "shadow", fromRect, toRect, 1f, 1f, moveTime, 1, null);
		shadowAnim.staticFrameOnly();
		model.animQueue.add(shadowAnim, false);
		shadowAnim.animType = AnimOp.AnimType.SHADOW;
		
		// cued to start and run when the creature moves
		moveAnim.onStart(new IAnimListener() {
			public void animEvent() {
				shadowAnim.startPlaying();
			}
		});
		
		myPreviousAnim = moveAnim;
	}
	
	// when falling into a level, we dont have anywhere visible to come from, so we turn off static drawing of this
	// creature as soon as we get this event, then turn it back on again after the animation is completed.
	public void fallIntoLevel(int sequenceNumber, final Character fallingCharacter, int level) {
		myPreviousAnim = null;  // if we are falling, then there was no previous move command.
		visualState = VisualState.WAITING_TO_FALL;
		Rect toRect = makeDrawingRectFromPosition(fallingCharacter.getPosition());
		Rect fromRect = new Rect(toRect);
		fromRect.y += toRect.height*4;
		destPosition = fallingCharacter.getPosition();
		
		final int dir = DungeonPosition.SOUTH;
		AnimationView fallAnim = new AnimationView(gui, getFullName(name, "walk", DungeonPosition.SOUTH), fromRect, toRect, 0.1f, 1f, DungeonAreaPresenter.fallPeriod, 1,  new IAnimListener() {
			public void animEvent() {
				visualState = VisualState.STATIONARY;  // we assume stationary.
				setStaticImage(dir);
				mapPresenter.moveLight(light, fallingCharacter.getPosition());
			}
		});

		fallAnim.onComplete(new IAnimListener() {
			public void animEvent() {
				gui.audio.playSound(Audio.DROP_IN);
			}
		});
		
		changeStateToAnimatingWhenStarted(fallAnim);
		fallAnim.sequenceNumber = sequenceNumber;
		fallAnim.animType = AnimOp.AnimType.FALL_IN;
		
		model.animQueue.chainSequential(fallAnim, false);
		myPreviousAnim = fallAnim;
		
		if (level > 0) {
			Rect fromNumRect = new Rect(toRect, .25f, .25f, 0f, 0f);
			Rect toNumRect = new Rect(fromNumRect,7f);
			TextImageView levelNumber = new TextImageView(gui, gui.numberFont, String.valueOf(level), fromNumRect);
			AnimationView levelAnim = new AnimationView(gui, levelNumber, fromNumRect, toNumRect, 1f, 0f, .5f, 1, null);
			model.animQueue.chainSequential(levelAnim, false);
		}
	}
	
	// TODO not happy with the look of this, really.  I might change it to facing north, then 'stepping' down the stairs
	// with an animation that sinks into the ground in discrete jumps and darkens a little with each one.
	// that means a change to ImageView to allow variable darkening of the image, which isnt a bad idea.
	// and include in the animationView a 'darkTween' maybe?  leave until latter.
	public void goDownStairs(int sequenceNumber, Character actingCreature, IAnimListener completeListener) {
		visualState = VisualState.WAITING_TO_GO_DOWN_STAIRS;  
		updateStaticImageArea();
		
		final IAnimListener tpListen = completeListener;
		Rect fromRect = new Rect(makeDrawingRectFromPosition(actingCreature.getPosition()));
		Rect toRect = new Rect(fromRect, 0.4f);
		AnimationView downAnim = new AnimationView(gui, staticImage.name, fromRect, toRect, 1f, .3f, 1f, 1, new IAnimListener() {
			public void animEvent() {
				if (tpListen != null) {
					tpListen.animEvent(); // tell anyone who cares
				}
				mapPresenter.removeLight(light);
			}
		});
		
		downAnim.onStart(new IAnimListener() {
			public void animEvent() {
				gui.audio.playSound(Audio.STAIR_DOWN);
			}
		});
		
		downAnim.setRotation(0, 360*3, 1);  // rotate 3 times
		
		changeStateToAnimatingWhenStarted(downAnim);
		downAnim.sequenceNumber = sequenceNumber;
		downAnim.animType = AnimOp.AnimType.GO_DOWN_STAIRS;
		
		// chain concurrently with same SN otherwise sequentially.
		model.animQueue.chainSequential(downAnim, false); 
	}
	
	public void creatureMeleeAttack(int sequenceNumber, DungeonPosition fromPosition, DungeonPosition targetPosition, int direction, IAnimListener animCompleteListener) {
		if (isCharging) {
			isCharging = false;
			return;
		}
		
		final Rect fromRect = makeDrawingRectFromPosition(fromPosition);
		switch (visualState) {
			case STATIONARY:
				visualState = VisualState.WAITING_TO_ATTACK;
				break;
			default:
				break;
		}
		
		final IAnimListener tpListen = animCompleteListener;
		final int dir = direction;
		final Rect toRect;
		if (creature instanceof Monster) {
			toRect = new Rect(fromRect, 1.34f);
		} else {
			toRect = new Rect(fromRect);
		}
		AnimationView attackAnim = new AnimationView(gui, getFullName(name, "attack", direction), fromRect, toRect, 1f, 1f, DungeonAreaPresenter.attackPeriod, 1, new IAnimListener() {
			public void animEvent() {
				visualState = VisualState.STATIONARY;
				setStaticImage(dir);
				if (tpListen != null) {
					tpListen.animEvent(); // tell anyone who cares
				}
			}
		});

		attackAnim.sequenceNumber = sequenceNumber;
		attackAnim.animType = AnimOp.AnimType.MELEE_ATTACK;
		changeStateToAnimatingWhenStarted(attackAnim);
		
		model.animQueue.chainSequential(attackAnim, false);
	}
	
	public void creatureDies(int sequenceNumber, Creature deadCreature, DungeonPosition deathPosition, IAnimListener completeListener) {
		sourcePosition = deathPosition;
		visualState = VisualState.WAITING_TO_DIE;
		updateStaticImageArea();
		
		final IAnimListener tpListen = completeListener;
		Rect fromRect = new Rect(makeDrawingRectFromPosition(deathPosition), 0.6f);
		Rect toRect = new Rect(fromRect, 1.6f);
		toRect.y += fromRect.height*.8f;
		AnimationView deathAnim = new AnimationView(gui, "DEATH", fromRect, toRect, 1f, 0f, 0.7f, 1, new IAnimListener() {
			public void animEvent() {
				if (tpListen != null) {
					tpListen.animEvent(); // tell anyone who cares
				}
				mapPresenter.removeLight(light);
			}
		});
		
		deathAnim.onStart(new IAnimListener() {
			public void animEvent() {
				gui.audio.playSound(Audio.DEATH);
			}
		});
		
		changeStateToAnimatingWhenStarted(deathAnim);
		deathAnim.sequenceNumber = sequenceNumber;
		deathAnim.animType = AnimOp.AnimType.DEATH;
		
		// and an animation of the ceature sinking into the ground played at the same time
		Rect fromRectC = makeDrawingRectFromPosition(deathPosition);
		Rect toRectC = new Rect(fromRectC);
		toRectC.y -= toRectC.height;
		AnimationView deathAnimC = new AnimationView(gui, staticImage.name, fromRectC, toRectC, 1f, 1f, 1f, 1, null);
		deathAnimC.sequenceNumber = sequenceNumber;
		deathAnimC.animType = AnimOp.AnimType.SINKING;
		deathAnimC.staticFrameOnly();
		deathAnimC.setClipRect(fromRectC);
		
		// chain concurently with same SN otherwise sequentially.
		model.animQueue.chainConcurrentWithSn(deathAnimC, false); // the creature sinking into the ground 
		model.animQueue.chainConcurrentWithSn(deathAnim, false); // the winged skull
	}
	

	public void invokeAbility(int sequenceNumber, Creature actingCreature, DungeonPosition targetPosition, Data ability) {
		// TODO Auto-generated method stub
		
	}
	
	// attach a call that turns off static drawing of this creature when this animation starts to play.
	// (the animation complete block will turn on static images again probably (unless the creature dies)
	// by setting to the appropriate state.
	private void changeStateToAnimatingWhenStarted(AnimationView animView) {
		//final CreaturePresenter selfi = this;
		animView.onStart(new IAnimListener() {
			public void animEvent() {
				//System.out.printlnln(selfi+" started anim");
				visualState = VisualState.ANIMATING;		
			}
		});
	}
	
	// this is the area of a creature which is slightly larger than the tile it is on.
	private Rect makeDrawingRectFromPosition(DungeonPosition position) {
		LocationPresenter locPres = mapPresenter.locationPresenter(position);
		Rect tileArea = locPres.getScreenArea();
		Rect rect = new Rect(tileArea, 1.25f);
		rect.y = tileArea.y;
		return rect;
	}
	
	private String getFullName(String name, String action, int dir) {
		String fullName = "m_" + name + "_" + action;
		String dirString;
		switch (dir) {
			case DungeonPosition.NORTH:
				dirString = "_rear";
				break;
			case DungeonPosition.SOUTHWEST:
			case DungeonPosition.WEST:
			case DungeonPosition.NORTHWEST:
				dirString = "_west";
				break;
			case DungeonPosition.NORTHEAST:
			case DungeonPosition.SOUTHEAST:
			case DungeonPosition.EAST:
				dirString = "_east";
				break;
			case DungeonPosition.SOUTH:
			default:
				dirString = "_front";
				break;
		}
		
		return fullName.concat(dirString);
	}
	
}
