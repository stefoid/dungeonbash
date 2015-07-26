package com.dbash.presenters.root.tutorial;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.IEventAction;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.root.OverlayPresenter;
import com.dbash.util.EventBus;
import com.dbash.util.L;
import com.dbash.util.Rect;

public abstract class TutorialPopupPresenter extends OverlayPresenter {
	public static final boolean LOG = false && L.DEBUG;
	
	public static float RESTART_PERIOD = 10;
	
	protected float restartTime;
	protected ArrayList<OverlayPresenter> myOverlays;
	
	@Override
	public void init(UIDepend gui) {
		restartTime = Gdx.graphics.getDeltaTime() + RESTART_PERIOD;
		myOverlays = new ArrayList<OverlayPresenter>();
	}
	
	@Override
	public void start(Rect area, TouchEventProvider touchEventProvider) {
		final String cname = this.getClass().getName();
		
		EventBus.getDefault().onEvent(TutorialPresenter.TUTORIAL_RESTART, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("dismissing: %s", cname);
				destroyFadeBoxes();
				dismiss();
			}
		});
	}

//	protected void testRestart() {
//		if (Gdx.graphics.getDeltaTime() > restartTime) {
//			restartTime = Gdx.graphics.getDeltaTime() + RESTART_PERIOD;
//			EventBus.getDefault().event(TutorialPresenter.TUTORIAL_RESTART, null);
//		}
//	}
	
	protected void addFadeBoxSeq(OverlayPresenter presenter) {
		myOverlays.add(presenter);
		gui.overlayQueues.addSequential(presenter);
	}
	
	protected void addFadeBoxPar(OverlayPresenter presenter) {
		myOverlays.add(presenter);
		gui.overlayQueues.addParallel(presenter);
	}
	
	protected void destroyFadeBoxes() {
		for (OverlayPresenter overlayPresenter : myOverlays) {
			overlayPresenter.dismiss();
		}
		myOverlays.clear();
	}
	
}
