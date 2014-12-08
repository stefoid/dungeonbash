package com.dbash.presenters.widgets;

import java.util.Vector;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.IAnimListener;
import com.dbash.util.L;
import com.dbash.util.Tween;

@SuppressWarnings("unused")
// An AnimOp is used to help schedule animations by providing observable events for starting, stopping and incremental completion.
// Really, it doesnt have to be an animation - any process that can start, run for a period of time and then stop could use or extend
// AnimOp to allow other things to observe those states.
//
// But anyway, we are using it for animations.  Animations that arent 'owned' by anything, such as special effects, can be instantiated, configured
// to start when some event happens, and then put onto an AnimQueue where they can run and other things can observe their completion state.
// 
//There are three observable progression properties of Animation operations: start, complete and percentage complete.
//Start and End are with respect to the animation as a whole, whereas (by convention) percentage complete applies 
//to the progress of one cycle of animation.  i.e. if the animation runs multiple cycles, these will fire multiple times
//as each repeated cycle progresses from 0 to 100%.  If you only have one cycle of course then 0%=start and 100%=stop
//
// There are some other public properties you can set on an animOp to help other things decide what to do when this animation 
// progresses, such as a sequence number and an animation type.
//
// AnimationView extends AnimOp, and also DungeonMapPresenter extends it, so it can schedule scrolling the map and have other things 
// observe scrolling completion.  You can also add an AnimOp instance itself to an AnimQueue, if you just want something 
// to wait on a certain animation completing and then go away.
//
// How to use:   
// pretty much the only essential thing the subclass needs to do is (1) override startPlaying, and to call 
//  (2) animationCycleStarted at the start of every cycle, including the first.  And (3)call super.draw() when drawn.
// and (4) call animationStopped() when done.
public class AnimOp {
	
	public static final boolean LOG = false && L.DEBUG;
	
	public static enum AnimType {
		ABILITY_ADD,
		ABILITY_RESIST,
		MOVE,
		FOLLOWER_MOVE,
		LEADER_MOVE,
		CHARGE_MOVE,
		FALL_IN,
		GO_DOWN_STAIRS,
		MELEE_ATTACK,
		RANGED_ATTACK,
		DAMAGE,
		DAMAGE_NUM,
		DEATH,
		SINKING,
		EXPLOSION,
		SCROLLING,
		MISSED,
		SHADOW,
		TARGETED,
		KNOCKBACK_MOVE,
		EFFECT_MSG,
		DEFAULT
	}
	
	public static class PercentCompleteListener {
		float percent;
		IAnimListener listener;
		boolean haveTriggered;
		
		public PercentCompleteListener(float percent, IAnimListener listener) {
			this.percent = percent;
			this.listener = listener;
			haveTriggered = false;
		}
		
		public void alertListener(float currentPercent) {
			if (LOG) L.log("alertListener-animOp: %s", this);
			
			if (haveTriggered == false) {
				if (currentPercent >= percent) {
					listener.animEvent();
					haveTriggered = true;
				}
			}
		}
		
		public void resetTrigger() {
			haveTriggered = false;
		}
	}
	
	// stuff you can read about this animOp.
	public int sequenceNumber;
	public AnimType animType;
	public boolean animating;
	public boolean owned;
	public boolean hasCompleted;
	public Object creator;
	
	// private stuff.
	protected float nextCompleteTrigger;
	protected Tween completeTween;
	protected Vector<IAnimListener> completeListeners;
	protected Vector<IAnimListener> startListeners;
	protected Vector<PercentCompleteListener> percentCompleteListeners;
	
	public AnimOp() {
		this.percentCompleteListeners = new Vector<PercentCompleteListener>();
		this.completeListeners = new Vector<IAnimListener>();
		this.startListeners = new Vector<IAnimListener>();
		this.completeTween = new Tween();
		this.animating = false;
		this.owned = false;  // this is to indicate to an AnimQueue that it should call draw on this Op if it is unowned.
		this.hasCompleted = false;  // this is an indication to the AnimQueue that the animation can be safely removed from the queue.
		this.animType = AnimType.DEFAULT;
	}
	
	// This will make the animation appear and start doing its thing
	// until this is called, it wont do anything.
	// If you want to chain the start of this animation, make a call to this in the complete callback of some other animation.
	public void startPlaying() {
		if (LOG) L.log("animOp: %s", this);
		animating = true;
		alertAnimStartListeners();
		//alertPercentageCompleteListeners(0f);  // TODO did I break anything?
	}
	
	// a subclass *must* call this to start a new cycle.
	protected void animationCycleStarted(float period) {
		if (LOG) L.log("animOp: %s", this);
		
		nextCompleteTrigger = 0;
		for (PercentCompleteListener pListener : percentCompleteListeners) {
			pListener.resetTrigger();
		}
		
		// Use tweens complete listener that tells us the cycle has ended.
		completeTween.init(0f, 100f, period, new IAnimListener() {
			public void animEvent() {
				cycleCompleted();
			}
		});
		
		alertPercentageCompleteListeners(0f);  // alert 
	}
	
	// subclass can call this to tell the AnimOp we have stopped.
	protected void animationStopped() {
		if (LOG) L.log("animOp: %s", this);
		
		animating = false;
		alertPercentageCompleteListeners(100f);
		alertAnimCompleteListeners();
		// now make sure all the listeners are cleared out, in case this aniamtion object is being re-used.
		// (hello dungeonmappresenter)
		
		percentCompleteListeners.clear();
		startListeners.clear();
		completeListeners.clear();
	}
	
	// when the cycle has ended the complete tween will call this
	// override if you want multi-cycle animations - the default behaviour implemented by this class is 1 cycle then stopped.
	protected void cycleCompleted() {
		animationStopped();
	}
	
	// the subclass should override this to actually draw something, and call this super as well.
	public void draw(SpriteBatch spriteBatch) {
		if (animating) {
			completeTween.deltaTime(Gdx.graphics.getDeltaTime());
			// trigger percentage of the cycle listeners
			// this will fire off to percentage complete listeners every 10%
			alertPercentageCompleteListeners(completeTween.getValue()); 	
		} 
	}
	
	
	// here are the various listeners.
	
	// add a listener to fire when any *cycle* repetition hits a 10% trigger, including the 0 or 100%.
	public void onPercentComplete(float percentage, IAnimListener listener) {
		PercentCompleteListener pListener = new PercentCompleteListener(percentage, listener);
		percentCompleteListeners.add(pListener);
	}
	
	// Alert listeners.  Rather than every single tick, lets only do this at 10% increments which should be
	// enough granularity
	protected void alertPercentageCompleteListeners(float currentPercent) {
		if (LOG) L.log("animOp: %s", this);
		
		if (currentPercent >= nextCompleteTrigger) {
			for (PercentCompleteListener pListener : percentCompleteListeners) {
				pListener.alertListener(currentPercent);
			}
			nextCompleteTrigger += 10.0f;  // our granularity will be 10%
		}
	}

	public void onStart(IAnimListener startListener) {
		// if the op has allready started, call this straight away , otherwise add the listener
		if (animating) {
			startListener.animEvent();
		} else {
			startListeners.add(startListener);
		}
	}
	
	protected void alertAnimStartListeners() {
		if (LOG) L.log("animOp: %s", this);
		
		for (IAnimListener l : startListeners) {
			l.animEvent();
		}
	}

	public void onComplete(IAnimListener completeListener) {
		completeListeners.add(completeListener);
	}
	
	protected void alertAnimCompleteListeners() {
		if (LOG) L.log("animOp: %s", this);
		
		for (IAnimListener l : completeListeners) {
			l.animEvent();
		}
	}
	
	public void setCreator(Object creator) {
		this.creator = creator;
	}
	
	public void cancel() {
		completeListeners.clear();
		startListeners.clear();
		percentCompleteListeners.clear();
	}
	
	public String toString() {
		return animType.toString();
	}
}
