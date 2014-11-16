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
import com.dbash.models.IDungeonPresentationEventListener.DeathType;
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
import com.dbash.util.Logger;
import com.dbash.util.Rect;


// The model can fire multiple events about a creature to its presenter, but animations take a while to play
// these so the state of the presenter, including position, can get out of phase with the model state
// until those animations have had a chance to play out.  
//
// So an animation has a start position and an end position, and updates the presenter when that 
// animation starts and finishes.  Basically when an aniamtion starts the state of the presenter is showing the animation
// , not showing a static image, until the animation finishes.
//
// Why doesnt the creature tell the creaturePresetner to animate directly?  because only the MapPresenter actually knows
// if a particular tile and/or needs to bother to show an animation or not, because it knows where the viewport currently is.
//
// One hiccup is that events come in that result in animations, and these animations have position, but if no aniamtions
// are supposed to be drawn, the creature model can still update its position via a listener.  The problem is when
// both of these things happen simultaneously, there can be some glitchiness.
//
//DRAWING CREATURES
// Location and hence LocationPresenter gets 'realtime' model updates of creatures locations, and MapPresenter only
// tells those tiles that are visible in the shadowmap(s) it is currently displaying to draw creatures.  i.e. only those
// creatures either sitting in a visible location get to draw themselves.
// Creature presenter takes care of the nasty business of working out whether to draw a static image of the creature when 
// asked to, or to display an animation of the creature moving form one tile to the next when it gets a move command.
// the tricky bit there is that sometimes the move command is queued, so a static image must be drawn while waiting for the anim to play
// but it cant be a static image in a position 'ahead' of where an unplayed animation will display it.
public class CreaturePresenter {
	
	public enum VisualState {
		SHOW_STATIC,
		SHOW_NOTHING,
		SHOW_ANIMATION    
	}
	
	private UIDepend gui;
	private PresenterDepend model;
	
	private DungeonPosition currentVisualPosition;  
	private VisualState visualState;
	private AnimationView highlightAnimation;
	private IPresenterCreature creature;
	private ImageView staticImage;
	private ImageView shadowImage;
	private Rect area;
	private MapPresenter mapPresenter;
	private String name;
	private ImageView highlightImage;
	private Light light;
	private boolean isCharging;
	
	public CreaturePresenter(UIDepend gui, PresenterDepend model, IPresenterCreature creature, MapPresenter mapPresenter) {
		this.gui = gui;
		this.creature = creature;
		this.name = creature.getNameUnderscore();
		this.model = model;
		isCharging = false;
		
		if (creature instanceof Character) {
			this.visualState = VisualState.SHOW_NOTHING;
			light = new Light(creature.getPosition(), 5, Light.CHAR_LIGHT_STRENGTH, false); // Characters have lights.
		} else {
			this.visualState = VisualState.SHOW_STATIC;
		}
		
		this.mapPresenter = mapPresenter;
		this.area = new Rect(0,0,0,0);
		this.currentVisualPosition = creature.getPosition();
		highlightImage = new ImageView(gui, "RANGED_DOT", this.area);
		updateHighlightAnimation(currentVisualPosition);
		setStaticImage(DungeonPosition.SOUTH);
		updateStaticImageArea();
	}
	public CreaturePresenter(){};
	
	public void resume() {
		this.visualState = VisualState.SHOW_STATIC;
		mapPresenter.addLight(light);
	}
	
	private void updateHighlightAnimation(DungeonPosition position) {
		Rect highlightArea = makeDrawingRectFromPosition(position);
		final float startScale = .7f;
		final float endScale = .9f;
		final float startAlpha = .2f;
		final float endAlpha = .3f;
		final float halfAlpha = startAlpha + (endAlpha*2 - startAlpha)/2;
		final Rect fromRect = new Rect(highlightArea, startScale);
		final Rect toRect = new Rect(highlightArea, endScale);
		final Rect halfRect = new Rect(highlightArea, (startScale+endScale)/2f);
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
		area = makeDrawingRectFromPosition(currentVisualPosition);
		staticImage.setArea(area);
		shadowImage.setArea(area);
		Rect hArea = new Rect(area, 0.75f);
		highlightImage.setArea(hArea);
	}
	
	// static images drawn here, whereas animated stuff delegated to the animqueue which will call draw when it wants to.
	public void draw(SpriteBatch spriteBatch, float alpha) {
		if (visualState == VisualState.SHOW_STATIC) {
			if (creature.getHighlightStatus() == HighlightStatus.HIGHLIGHTED) {  
				highlightImage.draw(spriteBatch, 0.18f*alpha);
			} else if (creature.getHighlightStatus() == HighlightStatus.CURRENTLY_IN_FOCUS) {
				highlightAnimation.draw(spriteBatch);
			}
			
			shadowImage.draw(spriteBatch, alpha);
			staticImage.draw(spriteBatch, alpha);
		}
	}

	// attach a call that turns off static drawing of this creature when this animation starts to play.
	// (the animation complete block will turn on static images again probably (unless the creature dies)
	// by setting to the appropriate state.
	private void configureAnimation(AnimationView animView) {
		animView.setCreator(this);
		animView.onStart(new IAnimListener() {
			public void animEvent() {
				visualState = VisualState.SHOW_ANIMATION;		
			}
		});
	}
	
	private void updateToStaticWhenStopped(final AnimationView animView, final DungeonPosition animEndPosition, final Integer dir) {
		animView.onComplete(new IAnimListener() {
			public void animEvent() {
				Boolean adjustHighlightLocation = false;
				if (currentVisualPosition != animEndPosition) {
					adjustHighlightLocation = true;
				}
				visualState = VisualState.SHOW_STATIC;
				currentVisualPosition = animEndPosition;
				if (dir != null) {
					setStaticImage(dir);
				} else {
					updateStaticImageArea();
				}
				if (adjustHighlightLocation) {
					updateHighlightAnimation(currentVisualPosition);
				}
				if (light != null) {
					mapPresenter.moveLight(light, animEndPosition);
				}		
			}
		});
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
	
	public void creatureMove(int sequenceNumber, DungeonPosition fromPosition, final DungeonPosition toPosition, int direction, 
			MoveType moveType, float moveTime, IAnimListener animCompleteListener) {

		// Work out what animation to play for the movement.
		String animToUse = "walk";
		switch (moveType) {
			case CHARGE_MOVE:
				isCharging = true;
				animToUse = "attack";
				break;
			case KNOCKBACK_MOVE:
				direction = DungeonPosition.oppositeDirection(direction);
				setStaticImage(direction);
				break;
			default:
				animToUse = "walk";
				break;
		}
		
		// Construct the move animation and add start and end strategies.
		final Rect toRect = makeDrawingRectFromPosition(toPosition);
		final Rect fromRect = makeDrawingRectFromPosition(fromPosition);
		final IAnimListener tpListen = animCompleteListener;
		final AnimationView moveAnim = new AnimationView(gui, getFullName(name, animToUse, direction), fromRect, toRect, 1f, 1f, moveTime, 1, new IAnimListener() {
			public void animEvent() {
				if (tpListen != null) {
					tpListen.animEvent(); // tell anyone who cares
				}
			}
		});
		
		configureAnimation(moveAnim);
		updateToStaticWhenStopped(moveAnim, toPosition, direction);
		
		// how and to what we chain this move anim depends on these rules:
		// 1. if the current move is a LEADER_MODE move, or a normal move, its chained sequentially to the last 
		//    anim in the queue
		// 2. if the current move is a FOLLOWER move, if myPreviousMove is also a FOLLOWER or FALL, it is chained to
		//    that animation sequentially, otherwise it is chained to the last anim in the queue concurrently.
		moveAnim.sequenceNumber = sequenceNumber;
		switch (moveType) {
			case FOLLOWER_MOVE:
				AnimationView myPreviousAnim = (AnimationView) model.animQueue.getLastByCreator(this);
				moveAnim.animType = AnimOp.AnimType.FOLLOWER_MOVE;
				if (myPreviousAnim != null) {
					model.animQueue.chainSequntialToOp(moveAnim, myPreviousAnim);
				} else {
					model.animQueue.chainConcurrentWithLast(moveAnim, 20f, false); // slight delay in following for asthetic reasons
				}
				break;
			case KNOCKBACK_MOVE:
				moveAnim.animType = AnimOp.AnimType.KNOCKBACK_MOVE;
				moveAnim.staticFrameOnly();
				model.animQueue.chainConcurrentWithSn(moveAnim, false);
				//model.animQueue.drawBeneath(moveAnim);  // so it gets drawn beneath the damage animation
				break;
			case LEADER_MOVE:
				moveAnim.animType = AnimOp.AnimType.LEADER_MOVE;
				model.animQueue.add(moveAnim, false);
				moveAnim.startPlaying();
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
	}
	
	// This is an indication to the creaturePresenter of a move event that cant be seen
	public void creatureMovedOutOfLOS(int sequenceNumber, DungeonPosition fromPosition, final DungeonPosition toPosition, int direction, MoveType moveType) {
		currentVisualPosition = toPosition;
		updateStaticImageArea();
		updateHighlightAnimation(toPosition);
	}
	
	// when falling into a level, we dont have anywhere visible to come from, so we turn off static drawing of this
	// creature as soon as we get this event, then turn it back on again after the animation is completed.
	public void fallIntoLevel(int sequenceNumber, final Character fallingCharacter, int level) {
		visualState = VisualState.SHOW_NOTHING;
		Rect toRect = makeDrawingRectFromPosition(fallingCharacter.getPosition());
		Rect fromRect = new Rect(toRect);
		fromRect.y += toRect.height*4;
		
		AnimationView fallAnim = new AnimationView(gui, getFullName(name, "walk", DungeonPosition.SOUTH), fromRect, toRect, 0.1f, 1f, DungeonAreaPresenter.fallPeriod, 1,null);

		configureAnimation(fallAnim);
		updateToStaticWhenStopped(fallAnim, fallingCharacter.getPosition(), DungeonPosition.SOUTH);
		
		fallAnim.onComplete(new IAnimListener() {
			public void animEvent() {
				gui.audio.playSound(Audio.DROP_IN);
			}
		});
		
		fallAnim.sequenceNumber = sequenceNumber;
		fallAnim.animType = AnimOp.AnimType.FALL_IN;
		model.animQueue.chainSequential(fallAnim, false);
		
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
	public void goDownStairs(int sequenceNumber, Character actingCreature, final IAnimListener completeListener) {
		
		Rect fromRect = new Rect(makeDrawingRectFromPosition(actingCreature.getPosition()));
		Rect toRect = new Rect(fromRect, 0.4f);
		AnimationView downAnim = new AnimationView(gui, staticImage.name, fromRect, toRect, 1f, .3f, 1f, 1, new IAnimListener() {
			public void animEvent() {
				if (completeListener != null) {
					completeListener.animEvent(); // tell anyone who cares
				}
				mapPresenter.removeLight(light);
			}
		});
		downAnim.setRotation(0, 360*3, 1);  // rotate 3 times
		
		downAnim.onStart(new IAnimListener() {
			public void animEvent() {
				gui.audio.playSound(Audio.STAIR_DOWN);
			}
		});
		
		configureAnimation(downAnim);
		downAnim.sequenceNumber = sequenceNumber;
		downAnim.animType = AnimOp.AnimType.GO_DOWN_STAIRS;
		
		// chain concurrently with same SN otherwise sequentially.
		model.animQueue.chainSequential(downAnim, false); 
	}
	
	public void creatureMeleeAttack(int sequenceNumber, DungeonPosition fromPosition, DungeonPosition targetPosition, int direction, final IAnimListener animCompleteListener) {
		if (isCharging) {
			isCharging = false;
			return;
		}
		
		final Rect fromRect = makeDrawingRectFromPosition(fromPosition);
		final Rect toRect;
		if (creature instanceof Monster) {
			toRect = new Rect(fromRect, 1.34f);
		} else {
			toRect = new Rect(fromRect);
		}
		AnimationView attackAnim = new AnimationView(gui, getFullName(name, "attack", direction), fromRect, toRect, 1f, 1f, DungeonAreaPresenter.attackPeriod, 1, new IAnimListener() {
			public void animEvent() {
				if (animCompleteListener != null) {
					animCompleteListener.animEvent(); // tell anyone who cares
				}
			}
		});

		configureAnimation(attackAnim);
		updateToStaticWhenStopped(attackAnim, fromPosition, direction);
		attackAnim.sequenceNumber = sequenceNumber;
		attackAnim.animType = AnimOp.AnimType.MELEE_ATTACK;
		model.animQueue.chainSequential(attackAnim, false);
	}
	
	public void creatureDies(int sequenceNumber, Creature deadCreature, DungeonPosition deathPosition, DeathType deathType, final IAnimListener completeListener) {
		if (Logger.DEBUG) Logger.log("creatureDies called for :" + this);
		AnimationView deathAnim = null;
		
		if (deathType == DeathType.HOLE) {
			// spiralling down a hole.
			Rect fromRect = new Rect(makeDrawingRectFromPosition(deadCreature.getPosition()));
			Rect toRect = new Rect(fromRect, 0.2f);
			toRect.y -= fromRect.height/2;
			deathAnim = new AnimationView(gui, staticImage.name, fromRect, toRect, 1f, .3f, DungeonAreaPresenter.deathPeriod, 1, new IAnimListener() {
				public void animEvent() {
					if (completeListener != null) {
						completeListener.animEvent(); // tell anyone who cares
					}
					mapPresenter.removeLight(light);
				}
			});
			deathAnim.setRotation(0, 360*1.5f, 1);  // rotate 2 times
		} else {
			// Animation of creature sinking into the ground.
			Rect fromRectC = makeDrawingRectFromPosition(deathPosition);
			Rect toRectC = new Rect(fromRectC);
			toRectC.y -= toRectC.height;
			deathAnim = new AnimationView(gui, staticImage.name, fromRectC, toRectC, 1f, 1f, DungeonAreaPresenter.deathPeriod, 1, null);
			deathAnim.sequenceNumber = sequenceNumber;
			deathAnim.animType = AnimOp.AnimType.SINKING;
			deathAnim.staticFrameOnly();
			deathAnim.setClipRect(fromRectC);
		}

		deathAnim.onStart(new IAnimListener() {
			public void animEvent() {
				gui.audio.playSound(Audio.DEATH);
			}
		});
		
		configureAnimation(deathAnim);
		deathAnim.sequenceNumber = sequenceNumber;
		deathAnim.animType = AnimOp.AnimType.DEATH;
		
		// put a popping skull on top.
		Rect fromRect = new Rect(makeDrawingRectFromPosition(deathPosition), 0.6f);
		Rect toRect = new Rect(fromRect, 1.6f);
		toRect.y += fromRect.height*.8f;
		AnimationView skullAnim = new AnimationView(gui, "DEATH", fromRect, toRect, 1f, 0f, DungeonAreaPresenter.skullPeriod, 1, new IAnimListener() {
			public void animEvent() {
				if (completeListener != null) {
					completeListener.animEvent(); // tell anyone who cares
				}
			}
		});
		
		skullAnim.sequenceNumber = sequenceNumber;
		
		// chain concurently with same SN otherwise sequentially.
		model.animQueue.chainConcurrentWithSn(deathAnim, false); // the creature sinking into the ground or spiral.
		model.animQueue.chainConcurrentWithSn(skullAnim, false); // the  skull
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
	
	public void invokeAbility(int sequenceNumber, Creature actingCreature, DungeonPosition targetPosition, Data ability) {
		// we dont have an animtion for this yet.
	}
	
}
