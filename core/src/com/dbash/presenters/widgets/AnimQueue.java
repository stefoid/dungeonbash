package com.dbash.presenters.widgets;

import java.util.Iterator;
import java.util.LinkedList;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.IAnimListener;
import com.dbash.util.L;

@SuppressWarnings("unused")
// This queue is used by things to get alerted to when an animation has started, stopped, or reached some percentage 
// of progress in an animation cycle.  An AnimOp is put on the queue to represent an animation that may play at some stage.
// the intention is that animations can be chained to go off when animations already in progress have
// reached certain stages of progress.
// 
// the queue works with animOps which dont actually do anything except keep track of how complete an animation is, and allow things to
// observe animation progression events.  You have to extend an AnimOp to actually do something.  AnimationView already does that.
// 
// This queue runs all AnimOps in it concurrently - i.e. anything in it gets a call to its draw() function.  Its up to the underlying object to decide
// if it has actually started (i.e. actually do something when draw is called).
//
// the queue will itself listen to all objects in it and remove them when they are complete.
//
// To schedule AnimOps in the queue, you chain their complete listeners sequentially or concurrently however you like.  The AnimQueue doesnt
// care - it will keep calling draw on all things in the queue until they say they are complete.
//
// so the summary is that AnimOps added to the queue will have their draw() function called constantly until they report they are complete,
// at which point they will be removed.
//
// NOTE: that an AnimOp in the queue can be owned by something else that is responsible for calling draw.  AnimQueue will only call draw on
// Ops that are not owned.  This is so different things in the queue can be drawn at different stages in the draw cycle.
// it doesnt actually make any dfference who calls draw an an AnimOp.  It willstill just do its thing.  The only reason the queue
// has the option of calling draw is so you can have unowned 'set and forget' animations that you can hand off to the queue and 
// be done with them.
public class AnimQueue {
	public static final boolean LOG = false && L.DEBUG;
	
	protected LinkedList<AnimOp> queue;
	protected int maxSize = 1000;
	
	public AnimQueue() {
		queue = new LinkedList<AnimOp>();
	}
	
	// appends it to the end of the list.  pass true for 'owned' if you want to control when draw is called on the AnimOp yourself.
	// otherwise the queue will call it.
	public void add(final AnimOp animOp, boolean owned) {
		if (queue.size() >= maxSize) {
			return;
		}
		queue.add(animOp);
		animOp.hasCompleted = false;
		animOp.owned = owned;
		animOp.onComplete(new IAnimListener() {
			public void animEvent() {
				animOp.hasCompleted = true;  // not safe to actually delete here, as we might be iterating over the list inside draw()
			}
		});
	}
	
	public void draw(SpriteBatch spriteBatch) {
		for (Iterator<AnimOp> it = queue.iterator(); it.hasNext(); ) {  
		    AnimOp animOp = it.next(); 
		    
		    if (animOp.hasCompleted) {
		    	it.remove();  // safely ask the iterator to remove this because its done.
		    } else if(animOp.owned == false) {  // only call draw on unowned animOps.
		    	animOp.draw(spriteBatch);
		    }
		}  
	}
	
	public void setMaxAnims(int size) {
		maxSize = size;
	}
	
	// chain this anim to the end of the queue, configured to start when the last one completes
	// or immediately if the queue is empty
	public void chainSequential(final AnimOp animOp, boolean owner) {
		AnimOp lastAnimOp = getLast();
		add(animOp, owner);
		
		// if the queue is empty 
		if (lastAnimOp == null) {
			animOp.startPlaying();
		} else {
			lastAnimOp.onComplete(new IAnimListener() {
				public void animEvent() {
					animOp.startPlaying();
				}
			});
		}
	}
	
	// similar to the sequential one, except the animation will start playing on a certain percentage complete, rather than 
	// after it totally completes.
	public void chainConcurrentWithLast(final AnimOp animOp, float percentage, boolean owner) {
		AnimOp lastAnimOp = getLast();
		add(animOp, owner);
		
		// if the queue is empty 
		if (lastAnimOp == null) {
			animOp.startPlaying();
		} else {
			lastAnimOp.onPercentComplete(percentage, new IAnimListener() {
				public void animEvent() {
					animOp.startPlaying();
				}
			});
		}
	}
	
	//  Will start immediately if queue empty 
	// Otherwise will attempt to chain concurrently with an animation of the same sequence number as it.
	// Otherwise will chain sequentially after last op in queue.
	public void chainConcurrentWithSn(final AnimOp animOp, boolean owner) {
		AnimOp lastAnimOp = getLast();
		AnimOp sameSnOp = getLast(animOp.sequenceNumber);
		add(animOp, owner);
		// if the queue is empty 
		if (lastAnimOp == null) {
			animOp.startPlaying();
		} else {
			if (sameSnOp != null) {
				sameSnOp.onStart(new IAnimListener() {
					public void animEvent() {
						animOp.startPlaying();
					}
				});
			} else {
				lastAnimOp.onComplete(new IAnimListener() {
					public void animEvent() {
						animOp.startPlaying();
					}
				});
			}
		}
	}
	
	//  Will start immediately if queue empty 
	// Otherwise will attempt to chain concurrently with the last anim belonging to the same creator.
	public void chainConcurrentWithMyLast(final AnimOp animOp, Object creator, float percentage, boolean owner) {
		AnimOp lastAnimOp = getLastByCreator(creator, null);
		add(animOp, owner);
		
		// if the queue is empty 
		if (lastAnimOp == null) {
			animOp.startPlaying();
		} else {
			lastAnimOp.onPercentComplete(percentage, new IAnimListener() {
				public void animEvent() {
					animOp.startPlaying();
				}
			});
		}
	}

	//  Will start immediately if queue empty 
	// Otherwise will attempt to chain sequntially with the last anim belonging to the same creator.
	// if there is none, chain sequentially to the last anim in the queue
	public void chainSequentialWithMyLast(final AnimOp animOp, Object creator, boolean owner) {
		AnimOp lastAnimOp = getLastByCreator(creator, null);
		
		if (lastAnimOp == null) {
			chainSequential(animOp, owner);
		} else {
			add(animOp, owner);
			lastAnimOp.onComplete(new IAnimListener() {
				public void animEvent() {
					animOp.startPlaying();
				}
			});
		}
	}
	
	public void chainSequntialToOp(final AnimOp thisOp, AnimOp afterThisOp) {
		add(thisOp, false);
		afterThisOp.onComplete(new IAnimListener() {
			public void animEvent() {
				thisOp.startPlaying();
			}
		});
	}
	
	// returns the last non-completed op in the queue
	public AnimOp getLast() {
		AnimOp op = null;
		
		if (queue.size() > 0) {
			op = queue.getLast();
			if (op.hasCompleted) {
				op = null;
			}
		}
		
		return op;
	}
	
	// returns the last non-completed op in the queue
	public AnimOp getLastByCreator(Object creator, AnimOp.AnimType type) {
			int i = queue.size()-1;
			
			while (i >= 0) {
				AnimOp op = queue.get(i);
				//if (LOG) Logger.log("asked crea: "+creator+" crea: "+op.creator+ " comp: "+op.hasCompleted+" type: "+op.animType);
				if (op.creator == creator && !op.hasCompleted) {
					if (type == null || (type != null && op.animType == type)) {
						return op;
					}
				}
				i--;
			}

			return null;
		}
	
	// returns the last non-completed op it finds with the same sequence number, or null.
	public AnimOp getLast(int sequenceNumber) {
		
		int i = queue.size()-1;
		
		while (i >= 0) {
			AnimOp op = queue.get(i);
			if (op.sequenceNumber == sequenceNumber && !op.hasCompleted) {
				return op;
			}
			i--;
		}

		return null;
	}

	public AnimOp.AnimType getLastType() {
		AnimOp op = getLast();
		if (op != null) {
			return op.animType;
		} else {
			return AnimOp.AnimType.DEFAULT;
		}
	}
	
	public void drawBeneath(AnimOp animOp) {
		if (queue.contains(animOp)) {
			queue.remove(animOp);
			queue.addFirst(animOp);
		}
	}
	
	public void clearAll() {
		for (Iterator<AnimOp> it = queue.iterator(); it.hasNext(); ) {  
		    AnimOp animOp = it.next(); 
		    animOp.cancel();
		} 
		queue.clear();
	}
	
}
