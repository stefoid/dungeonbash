package com.dbash.presenters.dungeon;

import java.util.HashMap;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.Ability;
import com.dbash.models.Ability.AbilityEffectType;
import com.dbash.models.Ability.AbilityType;
import com.dbash.models.AbilityCommand;
import com.dbash.models.Character;
import com.dbash.models.Creature;
import com.dbash.models.Data;
import com.dbash.models.Dungeon;
import com.dbash.models.Dungeon.MoveType;
import com.dbash.models.DungeonPosition;
import com.dbash.models.IAnimListener;
import com.dbash.models.IDungeonEvents;
import com.dbash.models.IDungeonPresentationEventListener;
import com.dbash.models.IPresenterCharacter;
import com.dbash.models.IPresenterTurnState;
import com.dbash.models.IPresenterTurnState.LeaderStatus;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEvent;
import com.dbash.models.TouchEventListener;
import com.dbash.models.TouchEventProvider;
import com.dbash.models.UIInfoListener;
import com.dbash.platform.AnimationView;
import com.dbash.platform.Audio;
import com.dbash.platform.TextImageView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.AnimOp;
import com.dbash.presenters.widgets.AnimQueue;
import com.dbash.util.L;
import com.dbash.util.Rect;

// This is the top level presenter of the Dungeon side of things - it handles touch events for the dungeon, and
// sends draw commands to the MapPresenter.
// DungeonAreaPresenter has a queue of animations that get drawn every frame.  There is no such thing as an invisible animation.  
// if the animation is not visible to a character in focus
// then it wont be created in the first place, because Dungeon model will not send it to DungeonMapPresenter.
// DungeonAreaPresenter knows how to turn dungeon event(s) into dungeon animation(s), either sequentially or in parallel.
//
// DungeonAreaPresenter delegates creature-related events such as falling in, movement and attacks to the appropriate Creature
// presenter.  The MapPresetner takes care of focus changes and scrolling.
// What is left is basically special effects - explosions and damage indications and the like.  These are the ones that
// DungeonAreaPresener knows how to turn into queued AniamtionViews.
public class DungeonAreaPresenter implements  TouchEventListener, IDungeonPresentationEventListener {

	public static final boolean LOG = false && L.DEBUG;
	
	private static final float multiplier = 1f;
	public final static float scrollPeriod = 0.5f * multiplier;
	public final static float walkPeriod = 0.7f * multiplier;
	public final static float leaderModeWalkPeriod = 0.7f * multiplier;
	public final static float attackPeriod = 0.4f * multiplier;
	public final static float chargePeriod = 0.4f * multiplier;
	public final static float fallPeriod = 0.3f * multiplier;
	public final static float damagePeriod = 0.7f * multiplier;
	public final static float burstPeriod = 0.3f * multiplier;
	public final static float abilityPeriod = 0.8f * multiplier;
	public final static float passPeriod = 0.4f * multiplier;
	public final static float missedPeriod = 0.4f * multiplier;
	public final static float highlightPeriod = 1.2f * multiplier;
	public final static float deathPeriod = 1.0f * multiplier;
	public final static float skullPeriod = 0.7f * multiplier;
	public final static float knockbackPeriod = 0.55f * multiplier;
	public final static float shudderPeriod = 0.34f * multiplier;
	public final static float effectMsgPeriod = 1f * multiplier;
	
	private UIDepend				gui;
	private PresenterDepend 		model;
	private MapPresenter			mapPresenter;
	private AnimQueue				animQueue;
	private AnimQueue				effectAnimQueue;
	private HashMap<Character, EffectPresenter> effectPresenters;

	// The area passed to this presenter, like any presenter, is where it is suppsoed to draw in 'world' coordinates.
	// the cameraViewport passed in the gui dependencies is the the one used to draw this presenter and its children.
	public DungeonAreaPresenter(UIDepend gui, final PresenterDepend model, TouchEventProvider touchEventProvider, Rect area) {
		this.animQueue = new AnimQueue();
		this.effectAnimQueue = new AnimQueue();
		effectAnimQueue.setMaxAnims(12);
		this.gui = new UIDepend(gui);
		this.effectPresenters = new HashMap<Character, EffectPresenter>();
		this.model = model;
		this.model.animQueue = animQueue;
		mapPresenter = new MapPresenter(this.gui, model, touchEventProvider, area);
		model.presenterDungeon.onVisibleDungeonEvent(this);  // tell the dungeon model to send pressie events your way.
		model.presenterTurnState.onChangeToLeaderStatus(new UIInfoListener() {
			public void UIInfoChanged() {
				changeInLeaderStatus(model.presenterTurnState.getLeaderStatus());
			}
		});
		setDetails(touchEventProvider, area);
	}

	public void draw(SpriteBatch spriteBatch) {
		gui.cameraViewPort.use(spriteBatch);
		spriteBatch.begin();
		mapPresenter.draw(spriteBatch);  // will draw the map, and creatures on the map 
		animQueue.draw(spriteBatch); // will draw characters and so on.
		effectAnimQueue.draw(spriteBatch);  // draws chaacter stat effects
		mapPresenter.refreshLighting();
		spriteBatch.end();
	}

	// The Dungeon Area presenter's primary job
	@Override
	public boolean touchEvent(TouchEvent event) {
		
		switch (event.getTouchType()) {
			case DOWN:
			case MOVE:
				break;
			case CLICK:
			{
				IPresenterTurnState tp = model.presenterTurnState;
				DungeonPosition position = mapPresenter.convertXYToDungeonPosition(event.getX(), event.getY());
				Character charPresenter = tp.getCharacterForTouchEvents();
				charPresenter.targetTileSelected(position);
			}
				break;
			case UP:
			case UP_INSIDE:
			{
				DungeonPosition position = mapPresenter.convertXYToDungeonPosition(event.getX(), event.getY());
				IPresenterCharacter charPresenter = model.presenterTurnState.getCharacterForTouchEvents();
				charPresenter.movementGesture(event.getMoveDirection(), position);
				break;
			}
		}
		
		return true;
	}
	
	public void setDetails(TouchEventProvider touchEventProvider, Rect area) {
		// We are the last port of call for any touches in the data area.  Probably not needed.
		touchEventProvider.addTouchEventListener(this, area, gui.cameraViewPort.viewPort);
	}
	
	// Dungeon has already moved the creature from its source destination to its target destination in the model
	// which will have updated the LocationPresenter.
	// But here we need to tell the creaturePresenter itself so it can perform an animation rather than just draw a static image at the destination
	@Override
	public void creatureMove(int sequenceNumber, Character releventCharacter, Creature actingCreature, DungeonPosition fromPosition, DungeonPosition toPosition, int direction, 
			Dungeon.MoveType moveType, Character currentFocussedCharacter, IAnimListener completeListener) {
		CreaturePresenter creaturePresenter = actingCreature.getCreaturePresenter(gui, model, mapPresenter);
		
		float moveTime;
		boolean shouldScrollWithCharacter = true;
		switch (moveType) {
			case CHARGE_MOVE:
				moveTime = DungeonAreaPresenter.chargePeriod;
				break;
			case KNOCKBACK_MOVE:
				moveTime = DungeonAreaPresenter.knockbackPeriod;
				shouldScrollWithCharacter = false;
				break;
			case FOLLOWER_MOVE:
				moveTime = DungeonAreaPresenter.leaderModeWalkPeriod;
				break;
			case LEADER_MOVE:
				moveTime = DungeonAreaPresenter.leaderModeWalkPeriod;
				break;
			case SHUDDER_MOVE:
				moveTime = DungeonAreaPresenter.shudderPeriod;
				shouldScrollWithCharacter = false;
				break;
			default:
				moveTime = DungeonAreaPresenter.walkPeriod;
				break;
		}
		
		creaturePresenter.creatureMove(sequenceNumber, fromPosition, toPosition, direction, moveType, moveTime, completeListener);
		
		// if the focused *character* is actually the one moving, then the focus has to move with it.
		// unless knockback or shudder. (doesnt work well for explosions where multiple characters may be knocked back
		// simultaneously.)
		// this is the only instance of the focus changing without an explicit command from the turn processor.
		if (currentFocussedCharacter == actingCreature && shouldScrollWithCharacter) {
			mapPresenter.animatedFocusChange(sequenceNumber, releventCharacter.shadowMap, releventCharacter.getPosition(), moveTime*.85f, true, null);
		}
	}
	
	@Override
	public void creatureMovedOutOfLOS(int sequenceNumber, Creature actingCreature, DungeonPosition fromPosition, final DungeonPosition toPosition, int direction, MoveType moveType)  {
		CreaturePresenter creaturePresenter = actingCreature.getCreaturePresenter(gui, model, mapPresenter);
		creaturePresenter.creatureMovedOutOfLOS(sequenceNumber, fromPosition, toPosition, direction, moveType);
	}
	
	@Override
	public void meleeAttack(int sequenceNumber, Character releventCharacter, Creature attackingCreature, DungeonPosition targetPosition) {
		DungeonPosition fromPosition = attackingCreature.getPosition();
		CreaturePresenter creaturePresenter = attackingCreature.getCreaturePresenter(gui, model, mapPresenter);
		int dir = fromPosition.getDirection(targetPosition);
		creaturePresenter.creatureMeleeAttack(sequenceNumber, fromPosition, targetPosition, dir, null);
	}
	
	@Override
	public void fallIntoLevel(int sequenceNumber, Character fallingCharacter, int level) {
		CreaturePresenter creaturePresenter = fallingCharacter.getCreaturePresenter(gui, model, mapPresenter);
		creaturePresenter.fallIntoLevel(sequenceNumber, fallingCharacter, level);
	}
	
	@Override
	public void creatureDies(int sequenceNumber, Character releventCharacter, Creature deadCreature, DungeonPosition deadPosition, DeathType deathType, IAnimListener completeListener) {
		CreaturePresenter creaturePresenter = deadCreature.getCreaturePresenter(gui, model, mapPresenter);
		creaturePresenter.creatureDies(sequenceNumber, deadCreature, deadPosition, deathType, completeListener);
	}
	
	@Override
	public void goDownStairs(int sequenceNumber, Character actingCreature, IAnimListener completeListener) {
		CreaturePresenter creaturePresenter = actingCreature.getCreaturePresenter(gui, model, mapPresenter);
		creaturePresenter.goDownStairs(sequenceNumber, actingCreature, completeListener);
	}
	
	@Override
	public void objectDrop(int sequenceNumber, Creature releventCharacter, Ability abilityObjectDropped, DungeonPosition position) {
		gui.audio.playSound(Audio.ITEM_DROP);
	}

	@Override
	public void objectPickup(int sequenceNumber, Character releventCharacter, Ability abilityObjectPickedUp, DungeonPosition position) {
		gui.audio.playSound(Audio.ITEM_PICKUP);
	}
	
	@Override
	// When the anim queue is empty, the waiting callback will be fired and the turn prcoessor can continue;
	public void waitingForAnimToComplete(int sequenceNumber, IAnimListener animCompleteListener) {
		AnimOp animOp = model.animQueue.getLast();
		if (animOp == null) {
			//if (LOG) Logger.log(" - empty so calling back now");
			animCompleteListener.animEvent();
		} else {
			//if (LOG) Logger.log(" - callback attached to last thing "+animOp.getDesc());
			final IAnimListener callback = animCompleteListener;
			animOp.onComplete(new IAnimListener() {
				public void animEvent() {
					callback.animEvent();
				}
			});
		}
	}

	@Override
	public void rangedAttack(int sequenceNumber, Character releventCharacter, Creature attackingCreature, AbilityType abilityType, int damageType, DungeonPosition targetPosition) {
		DungeonPosition fromPos = attackingCreature.getPosition();
		LocationPresenter fromLoc = mapPresenter.locationPresenter(fromPos);
		Rect fromRect = new Rect(fromLoc.getScreenArea(), 0.35f); // from rect = center of from tile only 10% size
		LocationPresenter toLoc = mapPresenter.locationPresenter(targetPosition);
		Rect toRect = new Rect(toLoc.getScreenArea(), 0.35f);
		int distance = fromPos.distanceTo(targetPosition);
		int numberOfDots = 2 + (int)(distance * 1.4f);
		float dx = (float)(toRect.x - fromRect.x)/(float)numberOfDots;
		float dy = (float)(toRect.y - fromRect.y)/(float)numberOfDots;
		final String soundEffect;
		if (abilityType == AbilityType.WAND /*|| abilityType == AbilityType.ABILITY*/) {
			soundEffect = Audio.ZAP;
		} else {
			soundEffect = Audio.RANGED_ATTACK;
		}
		// make a bunch of dots and add them to the queue , each one chained to the previous one.
		for (int i=0; i<numberOfDots; i++) {
			fromRect.x += dx;
			fromRect.y += dy;
			AnimationView dotAnim = new AnimationView(gui, "RANGED_DOT", fromRect, new Rect(fromRect, .2f), .8f, 0f, .3f, 1, null);
			if (i==0) {
				dotAnim.onStart(new IAnimListener() {
					public void animEvent() {
						gui.audio.playSound(soundEffect);
					}
				});
				model.animQueue.chainSequential(dotAnim, false);
			} else {
				model.animQueue.chainConcurrentWithLast(dotAnim, 20, false);
			}
		}
	}

	@Override
	public void damageInflicted(int sequenceNumber, Character releventCharacter, Creature damagedCreature, DungeonPosition targetPosition, int damageType, int damageAmount) {

		if (damagedCreature != null) {
			// Let the creature persenter handle it.
			CreaturePresenter cp = damagedCreature.getCreaturePresenter(gui, model, mapPresenter);
			String damageName = getDamageName(damageType);
			String sound = getSound(damageType);
			cp.damageInflicted(sequenceNumber, releventCharacter, targetPosition, damageType, damageName, damagePeriod, damageAmount, sound);
		} else {
			LocationPresenter loc = mapPresenter.locationPresenter(targetPosition);
			Rect fromRect = new Rect(loc.getScreenArea(), 0.8f);
			Rect toRect = new Rect(fromRect,1.5f);
			AnimationView damage = new AnimationView(gui, getDamageName(damageType), fromRect, toRect, 1f, 0.3f, damagePeriod, 1, null);
			addDamageSoundEffect(damage, damageType);
			AnimationView damageNum = null;
			if (damageAmount != IDungeonEvents.NO_DAMAGE) {
				TextImageView numbers = new TextImageView(gui, gui.numberFont, String.valueOf(damageAmount), fromRect);
				Rect fromNumRect = new Rect(fromRect, .34f, .34f, .25f, .25f);
				Rect toNumRect = new Rect(fromNumRect, 1.5f);
				damageNum = new AnimationView(gui, numbers, fromNumRect, toNumRect, 1f, 1f, damagePeriod, 1, null);
				damage.sequenceNumber = damageNum.sequenceNumber = sequenceNumber;
				damage.animType = damageNum.animType = AnimOp.AnimType.DAMAGE;
			}
			
			// damage due to bursts is a special case where the damage should play near the end of the burst, but not after it
			if (model.animQueue.getLastType() == AnimOp.AnimType.EXPLOSION) {
				model.animQueue.chainConcurrentWithLast(damage, 50f, false);
			} else {
				model.animQueue.chainConcurrentWithSn(damage, false);
			}
			
			// chain concurrently with same sn otherwise sequentially
			if (damageNum != null) {
				model.animQueue.chainConcurrentWithSn(damageNum, false);
			}
		}
	}

	public static String getDamageName(int damageType) {
		String name;
		switch (damageType) {
			case AbilityCommand.CHEMICAL_ATTACK:
				name = "burst_chemical";
				break;
			case AbilityCommand.ENERGY_ATTACK:
				name = "burst_energy";
				break;
			case AbilityCommand.HARD_ATTACK:
				name = "burst_hard";
				break;
			case AbilityCommand.SHARP_ATTACK: 
				name = "burst_sharp";
				break;
			case AbilityCommand.KNOCKBACK: 
				name = "burst_knock";
				break;
			case AbilityCommand.NO_PHYSICAL_ATTACK:
			default:
				name = "burst_magic_alt";
				break;
		}
		
		return name;
	}
	
	protected String getEffectName(AbilityEffectType abilityEfectType) {
		String name;
		switch (abilityEfectType) {
			case POISON:
				name = "POISON_IMAGE";
				break;
			case BLESSING:
				name = "BLESSING_IMAGE";
				break;
			case HEALING:
				name = "HEALING_IMAGE";
				break;
			case PROTECTION:
				name = "PROTECTION_IMAGE";
				break;
			case DEFENDING:
				name = "PROTECTION_IMAGE";
				break;
			case CURSE:
				name = "CURSE_IMAGE";
				break;
			case SPEED:
				name = "SPEED_IMAGE";
				break;
			case SLOW:
				name = "SLOW_IMAGE";
				break;
			case HOLD:
				name = "HELD_IMAGE";
				break;
			case ATTACK:
				name = "ATTACK_IMAGE";
				break;
			case RESIST_POISON:
				name = "RESIST_POISON_IMAGE";
				break;
			case RESIST_HELD:
				name = "RESIST_HOLD_IMAGE";
				break;
			case RESIST_STUN:
				name = "RESIST_STUN_IMAGE";
				break;
			case STUNNED:
				name = "STUNNED_IMAGE";
				break;
			default:
				name = "TARGET_IMAGE"; // TODO
				break;
		}
		
		return name;
	}
	
	@Override
	public void missed(int sequenceNumber, Character releventCharacter, DungeonPosition targetPosition) {
		LocationPresenter loc = mapPresenter.locationPresenter(targetPosition);
		Rect fromRect = new Rect(loc.getScreenArea());
		Rect toRect = new Rect(fromRect, 1.5f);
		AnimationView missed = new AnimationView(gui, "missed", fromRect, toRect, 0.6f, 0f, missedPeriod, 1, null);
		missed.onStart(new IAnimListener() {
			public void animEvent() {
				gui.audio.playSound(Audio.MISS);
			}
		});
		missed.sequenceNumber = sequenceNumber;
		missed.animType = AnimOp.AnimType.MISSED;
		model.animQueue.chainSequential(missed, false);
	}

	@Override
	public void invokeAbility(int sequenceNumber, Character releventCharacter, Creature actingCreature, DungeonPosition targetPosition, Data ability) {
		CreaturePresenter creaturePresenter = actingCreature.getCreaturePresenter(gui, model, mapPresenter);
		creaturePresenter.invokeAbility(sequenceNumber, actingCreature, targetPosition, ability);
	}

	@Override
	public void abilityAdded(int sequenceNumber, Character releventCharacter, AbilityEffectType abilityEfectType, DungeonPosition targetPosition) {
		LocationPresenter loc = mapPresenter.locationPresenter(targetPosition);
		Rect fromRect = new Rect(loc.getScreenArea());
		float effectPeriod = abilityPeriod;
		if (abilityEfectType == AbilityEffectType.DEFENDING) {
			effectPeriod = passPeriod;
		}
		AnimationView added = new AnimationView(gui, getEffectName(abilityEfectType), fromRect, fromRect, 1f, 0.3f, effectPeriod, 1, null);
		abilityAddedSound(added, abilityEfectType);
		added.sequenceNumber = sequenceNumber;
		added.animType = AnimOp.AnimType.ABILITY_ADD;
		
		// damage due to bursts is a special case where the damage should play near the end of the burst, but not after it
		if (model.animQueue.getLastType() == AnimOp.AnimType.EXPLOSION) {
			model.animQueue.chainConcurrentWithLast(added, 50f, false);
		} else {
			model.animQueue.chainConcurrentWithSn(added, false);
		}
	}
	
	protected void abilityAddedSound(AnimationView anim, AbilityEffectType abilityEfectType) {
		final String sound;
		switch (abilityEfectType) {
			case DEFENDING:
				sound = Audio.DEFEND;
				break;
			case POISON:
			case CURSE:
			case SLOW:
			case HOLD:
			case STUNNED:
				sound = Audio.BAD_EFFECT;
				break;
			default:
				sound = Audio.GOOD_EFFECT;
				break;
		}
		
		anim.onStart(new IAnimListener() {
			public void animEvent() {
				gui.audio.playSound(sound);
			}
		});
	}

	@Override
	public void abilityResisted(int sequenceNumber, Character releventCharacter, AbilityEffectType abilityEfectType, DungeonPosition targetPosition) {
		LocationPresenter loc = mapPresenter.locationPresenter(targetPosition);
		Rect fromRect = new Rect(loc.getScreenArea());
		int cycles = 8;
		final float totalPeriod = abilityPeriod/(cycles+2);
		// cycle it a number of times
		final AnimationView resisted = new AnimationView(gui, getEffectName(abilityEfectType), fromRect, fromRect, 1f, 1f, totalPeriod, cycles, null);
		// listen to itself for 50% blink cycle
		resisted.onPercentComplete(50, new IAnimListener() {
			public void animEvent() {
				resisted.setAlpha(0f, 0f, totalPeriod/2);
			}
		});
		resisted.onStart(new IAnimListener() {
			public void animEvent() {
				gui.audio.playSound(Audio.RESIST);
			}
		});

		resisted.sequenceNumber = sequenceNumber;
		resisted.animType = AnimOp.AnimType.ABILITY_RESIST;
		
		// chain concurrently with same sn otherwise sequentially
		model.animQueue.chainConcurrentWithSn(resisted, false);
	}

	protected void addDamageSoundEffect(AnimationView anim, int damageType) {
		final String sound = getSound(damageType);
		
		anim.onStart(new IAnimListener() {
			public void animEvent() {
				gui.audio.playSound(sound);
			}
		});
	}
	
	protected String getSound(int damageType) {
		String sound;
		switch (damageType) {
		case AbilityCommand.CHEMICAL_ATTACK:
			sound = Audio.CHEMICAL_HIT;
			break;
		case AbilityCommand.ENERGY_ATTACK:
			sound = Audio.ENERGY_HIT;
			break;
		case AbilityCommand.HARD_ATTACK:
			sound = Audio.HARD_HIT;
			break;
		case AbilityCommand.SHARP_ATTACK: 
			sound = Audio.SHARP_HIT;
			break;
		case AbilityCommand.KNOCKBACK: 
			sound = Audio.KNOCK_HIT;
			break;
		case AbilityCommand.NO_PHYSICAL_ATTACK:
		default:
			sound = Audio.BAD_EFFECT;
			break;
		}
		return sound;
	}
	
	protected void addBurstSoundEffect(AnimationView anim, int damageType) {
		final String sound;
		switch (damageType) {
			case AbilityCommand.CHEMICAL_ATTACK:
				sound = Audio.CHEMICAL_BURST;
				break;
			case AbilityCommand.ENERGY_ATTACK:
				sound = Audio.ENERGY_BURST;
				break;
			case AbilityCommand.HARD_ATTACK:
				sound = Audio.HARD_BURST;
				break;
			case AbilityCommand.SHARP_ATTACK: 
				sound = Audio.SHARP_BURST;
				break;
			case AbilityCommand.KNOCKBACK: 
				sound = Audio.KNOCK_BURST;
				break;
			case AbilityCommand.NO_PHYSICAL_ATTACK:
			default:
				sound = Audio.BAD_EFFECT;
				break;
		}
		
		anim.onStart(new IAnimListener() {
			public void animEvent() {
				gui.audio.playSound(sound);
			}
		});
	}
	
	@Override
	public void explosion(int sequenceNumber, Character releventCharacter, DungeonPosition targetPosition, int damageType, int range) {
		LocationPresenter loc = mapPresenter.locationPresenter(targetPosition);
		if (range == -1) {
			range = 5;
		}
		Rect fromRect = new Rect(loc.getScreenArea());
		Rect toRect = new Rect(fromRect, 1.5f+range*2.4f);
		AnimationView burst = new AnimationView(gui, getDamageName(damageType), fromRect, toRect, 1f, 0f, burstPeriod, 1, null);
		addBurstSoundEffect(burst, damageType);
		burst.sequenceNumber = sequenceNumber;
		burst.animType = AnimOp.AnimType.EXPLOSION;
		model.animQueue.chainSequential(burst, false);
	}


	@Override
	public void usingEye(DungeonPosition position, boolean showEyeAnim) {
		LocationPresenter loc = mapPresenter.locationPresenter(position);
		loc.showEyeAnimation(showEyeAnim);
	}

	@Override
	public void gameOver() {
		model.animQueue.getLast().onComplete(new IAnimListener() {
			public void animEvent() {
				gui.audio.stopMusic();
				gui.audio.playSound(Audio.GAME_OVER_TUNE);
			}
		});
	}
	
	@Override
	public void changeInLeaderStatus(LeaderStatus status) {
		switch (status) {
			case HAVE_LEADER:
			case NO_LEADER:
				gui.audio.playMusic(Audio.MOVING_THEME, true);
				break;
			case LEADER_DISABLED:
				gui.audio.playMusic(Audio.BATTLE_THEME, true);
				break;
			case NONE:
				break;
		}
	}
	
	@Override
	public void newCharacterFocus(Character newFocusCharacter) {
		if (LOG) L.log("newFocusCharacter: %s", newFocusCharacter);
		EffectPresenter effectPresenter = effectPresenters.get(newFocusCharacter);
		if (effectPresenter == null) {
			effectPresenter = new EffectPresenter(gui, model, newFocusCharacter, mapPresenter, effectAnimQueue);
			effectPresenters.put(newFocusCharacter, effectPresenter);
		}
		
		// make the current Effect presenter... current.
		for (EffectPresenter ep : effectPresenters.values()) {
			ep.clearCurrent();
		}
		effectPresenter.setCurrent();
	}

	@Override
	public void clearAnimations() {
		animQueue.clearAll();
	}
}

