package com.dbash.presenters.dungeon;

import java.util.Vector;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.Character;
import com.dbash.models.DungeonPosition;
import com.dbash.models.IAnimListener;
import com.dbash.models.IMapPresentationEventListener;
import com.dbash.models.Light;
import com.dbash.models.Location;
import com.dbash.models.LocationInfo;
import com.dbash.models.Map;
import com.dbash.models.PresenterDepend;
import com.dbash.models.ShadowMap;
import com.dbash.models.TouchEventProvider;
import com.dbash.models.UIInfoListener;
import com.dbash.models.UIInfoListenerBag;
import com.dbash.models.UILocationInfoListener;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.AnimOp;
import com.dbash.util.L;
import com.dbash.util.Rect;
import com.dbash.util.Tween;

@SuppressWarnings("unused")
// This class draws the dungeon map and smoothly scrolls it when required.
// it talks to the dungeon simulation through the presenter dungeon interface.
// it defers drawing Locations to individual LocationPresenters, and creatures to creaturePresenters.
// but it knows about layout and dimensions and so on.
//
// mappresenter is an AnimOp so it can add itself to the anim queue so that things can schedule themselves wrt. scrolling.
// 
public class MapAnim extends AnimOp {
	public static final boolean LOG = true && L.DEBUG;

	DungeonPosition targetDungeonPosition;
	protected Tween xTween;
	protected Tween yTween;
	protected Tween currentShadowMapTween;
	protected MapPresenter mapPresenter;
	protected float period;
	
	public MapAnim(Tween currentShadowMapTween, Rect viewPos, Rect targetPoint, float period, MapPresenter mapPresenter) {
		super();  
		
		this.mapPresenter = mapPresenter;
		this.currentShadowMapTween = currentShadowMapTween;
		this.creator = mapPresenter; // identifies this anim op in the queue
		this.animType = AnimType.SCROLLING;  // andthe type
		this.period = period;
		xTween = new Tween();
		yTween = new Tween();
		xTween.init(viewPos.x, targetPoint.x, period, null);
		yTween.init(viewPos.y, targetPoint.y, period, null);
	}
	
	
	// viewPos is where the dungeon camera is at any given time, so we have to draw enough tiles around that point to show the map
	// LocationPresenters are just squares of tilesize, starting from 0,0 in the bottom left corner, its no probs.
	public void draw(SpriteBatch spriteBatch) {
		// do scrolling if needed. 
		if (animating) {
			super.draw(spriteBatch);  // this is only to tick the animOps deltaTime for listeners
			float dTime = Gdx.graphics.getDeltaTime();
			xTween.deltaTime(dTime);
			yTween.deltaTime(dTime);
			currentShadowMapTween.deltaTime(dTime);
			mapPresenter.moveView(xTween.getValue(), yTween.getValue());
		}
	}
	
	// This is the override of the AnimOp function.
	public void startPlaying() {
		if (LOG) L.log("");
		
		super.startPlaying();
		super.animationCycleStarted(period);
	}
	
	
	@Override
	protected void animationStopped() {
		super.animationStopped();
		if (LOG) L.log("");
	}
	
	@Override
	public void onPercentComplete(float percentage, IAnimListener listener) {
		super.onPercentComplete(percentage, listener);
		if (LOG) L.log("");
	}
//	
//	@Override
//	protected void alertPercentageCompleteListeners(float currentPercent) {
//		if (currentPercent >= nextCompleteTrigger) {
//			for (PercentCompleteListener pListener : percentCompleteListeners) {
//				if (LOG) L.log("percent listener alerted");
//			}
//		}
//		super.alertPercentageCompleteListeners(currentPercent);
//	}
}
