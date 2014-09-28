package com.dbash.presenters.tabs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.presenters.widgets.AnimOp;
import com.dbash.presenters.widgets.IListElement;
import com.dbash.util.Tween;

public class ElementAnimator extends AnimOp {

	IListElement element;
	float period;
	Tween yTween;
	float startY;
	float endY;
	float x;
	
	/**
	 * This is about the simplest override of an AnimOp there is.  A oneshot with a single tween.
	 */
	public ElementAnimator(IListElement element, float x, float period, float startY, float endY) {
		this.element = element;
		this.period = period;
		this.yTween = new Tween();
		this.x = x;
		this.startY = startY;
		this.endY = endY;
	}
	
	@Override
	public void startPlaying() {
		yTween.init(startY, endY, period, null);
		super.startPlaying();
		super.animationCycleStarted(period);
	}
	
	@Override
	public void draw(SpriteBatch spritebatch) {
		if (animating) {
			yTween.deltaTime(Gdx.graphics.getDeltaTime());	
			element.draw(spritebatch, x, yTween.getValue());
		}

		super.draw(spritebatch);
	}
}
