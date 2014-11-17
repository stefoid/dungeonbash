package com.dbash.platform;

import java.util.Vector;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.IAnimListener;
import com.dbash.presenters.widgets.AnimOp;
import com.dbash.util.Rect;
import com.dbash.util.Tween;


// movement is from the start x,y to the end x,y
// scaling is from the start width/height to the end width/height.
// the animation will stop playing when the period is elapsed and *wont draw itself*.
// 
public class AnimationView  extends AnimOp {
	
	// current state variables
	// These can vary over the period of a single animation
	Tween xTween, yTween, wTween, hTween;
	Tween alphaTween;
	Tween rotationTween;
	Tween frameTween;  // for transitioning from frame to frame
	
	public static final int LOOP_FOREVER = -1;
	int currentRepetition;  // counts down to 0 and then animation ends.  Can set to LOOP_FOREVER.
	
	String name;
	Rect startRect;
	Rect endRect;
	float startAlpha;
	float endAlpha;
	float period;
	public Rect currentArea;
	Rect clipBox = null;
	boolean staticFrame = false;
	float startRotation;
	float endRotation;
	
	Vector<ImageView> frames; 
	ImageView image;  // the current frame
	UIDepend gui;
	
	// The constructor takes the usual parameters.  sets up initial conditions.  initRepetition sets up one animation cycle.
	// the completeListener is called at the end of the entire animation sequence which includes and multiple repetions.
	// whereas the %complete listeners are called with respect to completion of one cycle.
	public AnimationView(UIDepend gui, String name ,Rect startRect, Rect endRect, float startAlpha, float endAlpha, 
			float period, int repetitions, IAnimListener completeListener) {
		
		super();  // init the AnimOp
		init(gui, startRect, endRect, startAlpha, endAlpha, period, repetitions, completeListener);
		frames = makeFrames(name);
	}
	
	// This is basically just to animate a TextImageView.
	public AnimationView(UIDepend gui, ImageView image ,Rect startRect, Rect endRect, float startAlpha, float endAlpha, 
			float period, int repetitions, IAnimListener completeListener) {
		super();  // init the AnimOp
		init(gui, startRect, endRect, startAlpha, endAlpha, period, repetitions, completeListener);
		frames = new Vector<ImageView>();
		frames.add(image);
	}
	
	protected void init(UIDepend gui,Rect startRect, Rect endRect, float startAlpha, float endAlpha, 
			float period, int repetitions, IAnimListener completeListener) {
		
		xTween = new Tween();
		yTween = new Tween();
		wTween = new Tween();
		hTween = new Tween();
		alphaTween = new Tween();
		frameTween = new Tween();
		rotationTween = new Tween();
		currentArea = new Rect(0,0,0,0);
		
		this.animating = false; // default to not playing.
		this.currentRepetition = repetitions;
		this.period = period;
		
		this.startRect = new Rect(startRect);
		this.endRect = new Rect(endRect);
		this.startAlpha = startAlpha;
		this.endAlpha = endAlpha;
		
		this.gui = gui;
		
		if (completeListener != null) {
			super.onComplete(completeListener);
		}
	}
	
	// get the list of images ready to show
	protected Vector<ImageView> makeFrames(String name) {
		int numFrames = gui.spriteManager.maxFrame(name);
		Vector<ImageView> theFrames = new Vector<ImageView>();
		for (int i=1; i<= numFrames; i++) {
			theFrames.add(new ImageView(gui, name, i, startRect));
		}
		
		return theFrames;
	}
	
	// set up the animation for the start of an animation cycle.
	// puts the tweens at the start values, and resets any listener triggers (by calling the AnimOp super)
	private void initCycle() {
		setRects(startRect, endRect, period);
		alphaTween.init(startAlpha, endAlpha, period, null);
		rotationTween.init(startRotation, endRotation, period, null);
		
		if (staticFrame == false) {
			frameTween.init(0, frames.size()-0.01f, period, null);
		}
		
		// set the image to the first frame;
		image = frames.get(0);
		
		super.animationCycleStarted(period);
	}
	
	// sets the current Area to the tween calculated values.
	protected void updateAreaAndRotation() {
		currentArea.x = xTween.getValue();
		currentArea.y = yTween.getValue();
		currentArea.width = wTween.getValue();
		currentArea.height = hTween.getValue();
		image.setArea(currentArea);
		image.setRotation(rotationTween.getValue() % 360);
	}
	
	// call to clip the drawing of this animation within the specified rect.
	public void setClipRect(Rect clipBox) {
		this.clipBox = new Rect(clipBox);
	}
	
	// call to only show the one frame even though there could be multiple frames in the file system
	public void staticFrameOnly() {
		staticFrame = true;
	}
	
	public void setAlpha(float startAlpha ,float endAlpha, float period) {
		alphaTween.init(startAlpha, endAlpha, period, null);
	}
	
	public void setRects(Rect startRect, Rect endRect, float period) {
		xTween.init(startRect.x, endRect.x, period, null);
		yTween.init(startRect.y, endRect.y, period, null);
		wTween.init(startRect.width, endRect.width, period, null);
		hTween.init(startRect.height, endRect.height, period, null);
	}
	
	// call this to set up a rotation if you want.
	public void setRotation(float startRotation, float endRotation, float period) {
		this.startRotation = startRotation;
		this.endRotation = endRotation;
		rotationTween.init(startRotation, endRotation, period, null);
	}
	
	@Override
	protected void cycleCompleted() {
		if (currentRepetition != LOOP_FOREVER) {
			currentRepetition--;
		}
		
		if (currentRepetition != 0 ) {
			// go for another repetition
			initCycle();
		} else {
			stopPlaying();
		}
	}
	
	// This will make the animation appear and start doing its thing
	// until this is called, it wont do anything.
	// If you want to chain the start of this animation, make a call to this in the complete callback of some other animation.
	public void startPlaying() {
		super.startPlaying();
		initCycle();
	}
	
	// This will make the animation *not draw*, fire its complete listener, and not do anything anymore.
	// This is automatically called when the number of animation repetitions has been reached
	// or you can call it from an external source to finish an animation.
	public void stopPlaying() {
		super.animationStopped();
	}
	
	public void draw(SpriteBatch spriteBatch) {
		if (animating) {
			// draw here.
			updateAreaAndRotation();
			
			if (clipBox != null) {
				gui.cameraViewPort.startClipping(spriteBatch, clipBox);
				drawTheFrame(spriteBatch);
				gui.cameraViewPort.endClipping(spriteBatch);
			} else {
				drawTheFrame(spriteBatch);
			}
			
			// then apply the delta
			float dt = Gdx.graphics.getDeltaTime();
			xTween.deltaTime(dt);
			yTween.deltaTime(dt);
			wTween.deltaTime(dt);
			hTween.deltaTime(dt);
			alphaTween.deltaTime(dt);
			rotationTween.deltaTime(dt);
			frameTween.deltaTime(dt);
			int frame = (int) frameTween.getValue();
			image = frames.get(frame);
			
			super.draw(spriteBatch);
		}
	}
	
	
	protected void drawTheFrame(SpriteBatch spriteBatch) {
		image.draw(spriteBatch, alphaTween.getValue());
	}
}
