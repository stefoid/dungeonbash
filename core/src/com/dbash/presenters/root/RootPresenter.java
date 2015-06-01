package com.dbash.presenters.root;

import java.util.LinkedList;

import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEvent;
import com.dbash.models.TouchEvent.TouchType;
import com.dbash.models.TouchEventListener;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.CameraViewPort;
import com.dbash.platform.SizeCalculator;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.dungeon.DungeonAreaPresenter;
import com.dbash.presenters.tabs.TabbedDataAreaPresenter;
import com.dbash.util.Rect;


public class RootPresenter implements InputProcessor, TouchEventProvider {

	private TabbedDataAreaPresenter	dataAreaPresenter;
	private DungeonAreaPresenter	dungeonAreaPresenter;
	private OverlayQueues			overlayQueues;
	private Rect 					dataArea;
	private Rect 					dungeonArea;
	private Rect					screenArea;
	private GameStatePresenter		gameStatePresenter;
	private EffectScroller 			effectScroller;
	
	UIDepend 						gui;
	PresenterDepend 				model;

	public RootPresenter(UIDepend gui, PresenterDepend model) {
		this.gui = gui;
		this.model = model;
		init();
	}

	public void draw(SpriteBatch spriteBatch, boolean drawDungeon) {
		
		if (drawDungeon) {
			dungeonAreaPresenter.draw(spriteBatch);
		}
		
		dataAreaPresenter.draw(spriteBatch);

		// popups always appear over the top of everything else. (using dataArea presenters cameraViewPort
		overlayQueues.draw(spriteBatch, 0, 0);
		effectScroller.draw(spriteBatch, 0, 0);
		spriteBatch.end();
		
		gui.audio.processVolume();
	}

	// The root presenter is the boss of the display, so this is where you set up the sizes of everything, by defining cameraViewPort(s) and Presenter(s).
	//
	// basically each cameraViewport is a window onto a different virtual world that will be visible in a certain area of the screen when you tell objects to draw themselves
	// using that cameraViewport (if it is positioned such that it can see that object)
	//
	// It is important to note that the area passed to a CameraViewPort defines the area on the screen that subsequent drawing to that camera
	// will  appear in [defined in ints].  While the area passed to presenters is the world (or model) coords where the presenter should position itself. [floats]
	private void init() {	    
		screenArea = SizeCalculator.getScreenSize();
		overlayQueues = new OverlayQueues();
		gui.overlayQueues = overlayQueues;
		
//		// We create three viewports so that we can the dungeon map and data area can be moved around just by adjusting the Viewport x and y offsets.
//		// Means we can swap them for lefties...
//		// The third viewport is the entire screen for popups.
		
		// the dungeon area only takes up the left part of the screen
		final float DAM = 0.5f;  // data area modifier (percentage of dungeon map width)
		this.dungeonArea = new Rect(screenArea);
		// We want the ideal data area to be 50% of the dungeon width.  If there isnt enought screen space, we
		// will shrink the dungeon area.  dw + dw*.55 = sw    
		dungeonArea.width = screenArea.width / (1f + DAM);
		if (dungeonArea.width > screenArea.height) {
			dungeonArea.width = screenArea.height;
		}
		dungeonArea.height = dungeonArea.width; // dungeon area is a square
		
		// data area.
		this.dataArea = new Rect(screenArea);
		dataArea.width = dungeonArea.width * DAM;
		dataArea.height = dungeonArea.height;
		
		// find Viewport offsets to center dungeon and data areas on physical screen.
		float totalWidth = dungeonArea.width + dataArea.width;
		float xOffset = (screenArea.width-totalWidth)/2;	
		float yOffset = (screenArea.height-dungeonArea.height)/2;
		
		Rect dataAreaViewPort = new Rect(dataArea);
		dataAreaViewPort.x += xOffset + dungeonArea.width;
		dataAreaViewPort.y += yOffset;
		
		// Great, the final drawing area is decided, we can set the height for list purpose (size calculator)
		gui.sizeCalculator.setAreas(dataArea, dungeonArea);
		
		// first viewport
		CameraViewPort dataViewPort = new CameraViewPort(dataAreaViewPort);
		gui.cameraViewPort = dataViewPort;
		dataAreaPresenter = new TabbedDataAreaPresenter(gui, model, this, dataArea);  // the area passed to the presenter is the area it draws in the 'world'
		
		// second viewport
		// By creating the dungeon area after the tabbed data area, it will process touch events first which will be more efficient
		// than having to check the touch areas of the entire tabbed data area before trying the dungeon area, for clicks inside the dungeon area.
		// Dungeon Area Viewport can move according to offset and nothing else has to change.
		Rect dungeonAreaViewPort = new Rect(dungeonArea);
		dungeonAreaViewPort.x += xOffset;
		dungeonAreaViewPort.y += yOffset;
		CameraViewPort dungeonViewPort = new CameraViewPort(dungeonAreaViewPort);
		gui.cameraViewPort = dungeonViewPort;
		dungeonAreaPresenter = new DungeonAreaPresenter(gui, model, this, dungeonArea);	 // the area passed to the presenter is the area it draws in the 'world'
	
		// third viewport 
		Rect overlayArea = new Rect(0,0, dungeonArea.width+dataArea.width, dungeonArea.height);
		gui.overlayQueues.init(gui, overlayArea, this);
		overlayArea.x += xOffset;
		overlayArea.y += yOffset;
		CameraViewPort wholeScreenViewPort = new CameraViewPort(overlayArea);
		gui.cameraViewPort = wholeScreenViewPort;
		gameStatePresenter = new GameStatePresenter(model, gui);
		
		effectScroller = new EffectScroller(gui, dungeonArea);
	}
	
	
//	A TouchProvider reference gets passed down the UI chain from parent to child
//	Touch listeners register their interest in TouchEvents with the provider, passing their callback class and their touchable area.  listeners are inserted at the top of the providers queue rather than added to the bottom, so that when a touchEvent happens the listeners at the bottom of the UI tree (those added last) get a chance to handle touch events first.
//	When a touch event happens, the overlay manager (touch provider) delivers it only to those listeners that are directly affected (it checks the position of the touch against their registered touchable area).  If they handle the event then it is finished there, but if they dont, the touch provider continues to search for other matching areas, which, being later in the queue, will be further up the view hierarchy.  So the most specific widgets (buttons, tabs, etc...) are given a chance to handle touches before less specific widgets (containers, lists etc..) get a chance.  
//	The pros of this method are that working out and delivering touch events to interested parties is all done in one place, pulled from the bottom of the view hierarchy rather than being pushed down at each level.
//	The rules for touches, like if the touch DOWN was handled by a particular widget then that widget should receive subsequent MOVES and touch UPS regardless of where they land, can be handled in one spot at the top of the tree, rather than in many potential spots throughout the tree.
//	If a particular parent wants to handle touch events on behalf of its children, because perhaps its children are constantly moving, like the scrolling list, then that can still happen.  All it needs is the List to register for the events and list elements dont.  But for buttons, tabs and other static touchable areas, its best if the actual touchable thing at the bottom of the view tree receives it directly.  For most buttons and other clickable things, all that is required is for the listening callback to answer true that it handled the down event, and actually perform its function on the subsequent touch UP.
//	Obviously things need to deregister themselves as touch listeners before they are cleaned up.
//	What should be the top level rules for delivering events?
//	1) touch DOWN are delivered to the first listener in the queue whose area contains the touch.  If that listener does not want to handle the event, it is delivered to the next matching listener and so on up the queue until the touch event is handled or ignored totally.
//	2) If a touch DOWN  _is_ handled, that listener is remembered and subsequent MOVES  are delivered to that listener *only* until a touch UP occurs.
//	3) If the the touch UP  occurs inside the remembered listeners area, it is delivered to the listener, otherwise it is not, and the system goes back to waiting for the next touch DOWN
	
	
	// All this is quite similar to what has already been implemented, but in a generic way = the overlay doesnt need to know
	// the identity of touch listeners, and it doesnt have to push the events to them - they pull them when they want to.
	// Possibly the dungeon/playing field or whatever could be modified to use this system anonymously.
	private class TEListener 
	{
		Rect area;
		TouchEventListener listener;
		Rect viewPort;
		
		public TEListener(TouchEventListener listener, Rect area, Rect viewPort) {
			this.area = new Rect(area);
			this.listener = listener;
			this.viewPort = viewPort;
		}
		
		// this comparison compares the touch event position after adjusting for the listeners viewport to
		// determine if the touch falls inside the listeners area
		// but it returns false if it falls outside of the viewport itself (out of bounds)
		boolean isInside(TouchEvent touchEvent) { 
			if (viewPort.isInside(touchEvent.screenX, touchEvent.screenY)) {
				return area.isInside(touchEvent.screenX - viewPort.getX(), touchEvent.screenY - viewPort.getY());
			} else {
				return false;
			}
		}
	}
	
	private LinkedList<TEListener> teListeners	= new LinkedList<TEListener>();
	TEListener gotDownListener = null;
	TouchEvent previousTouchEvent = null;
	
	// This adjusts the recorded touch position of the touchEvent for the viewport position of the listener
	// so that the listener sees the touch relative to its own viewport coords rather than the entire screen
	private boolean handTouchEventToListener(TEListener tel, TouchEvent touchEvent) {
		touchEvent.adjustForViewPort(tel.viewPort); // adjust the internal touch location for the listeners viewport position
		return tel.listener.touchEvent(touchEvent);
	}
	
	// If a touchdown is followed quickly by a touchup and there isnt much movement between, then we will call
	// that a click.
	public boolean handleTouchEvent(TouchEvent touchEvent) {
		switch (touchEvent.getTouchType()) {
			case DOWN:
				previousTouchEvent = touchEvent;
				for (TEListener tel : teListeners) {   
					if (tel.isInside(touchEvent)) {   // Look through all listeners for one the touch is inside of.
						if (handTouchEventToListener(tel, touchEvent)) {  // If it handles the touch event, then remember it.
							gotDownListener = tel;
							break;
						}  // if not handled, check higher up the view tree.
					}
				}
				break;
			case MOVE:
				if (gotDownListener != null) {
					touchEvent.setTouchMemory(previousTouchEvent);
					touchEvent.hasMoved = true;
					boolean consumed = handTouchEventToListener(gotDownListener, touchEvent);
					// If a move wasnt consumed by the downListener, let other things handle it.
					if (consumed == false) {
						for (TEListener tel : teListeners) {   
							if (tel != gotDownListener && tel.isInside(touchEvent)) {   // Look through all listeners for one the touch is inside of.
								if (handTouchEventToListener(tel, touchEvent)) {  // If it handles the touch event, finish
									break;
								}  // if not handled, check higher up the view tree.
							}
						}
					}
					previousTouchEvent = touchEvent;
				}
				break;
			case UP:
				if (gotDownListener != null) {
					touchEvent.setTouchMemory(previousTouchEvent);
					if (gotDownListener.isInside(touchEvent)) {
						touchEvent.setTouchType(TouchType.UP_INSIDE);
						
						// There could be a difference between moving around inside an area and a click.
						if (touchEvent.hasMoved == false || 
								touchEvent.getTotalMaxMovement() < gui.sizeCalculator.MIN_DRAG_PIXELS) {
							touchEvent.setTouchType(TouchType.CLICK);
						}
					}
					handTouchEventToListener(gotDownListener, touchEvent);
				}
				break;
			case CLICK:
				break;
			case UP_INSIDE:
				break;
			default:
				break;
		}
		
		return true;
	}
	
	@Override
	public void addTouchEventListener(TouchEventListener listener, Rect area, Rect viewport) {
		Rect touchArea = area;
		
		if (area == null) {
			touchArea = screenArea;
		}
		
		TEListener tel = new TEListener(listener, touchArea, viewport);
		teListeners.addFirst(tel);
	}
	
	@Override
	public void removeTouchEventListener(TouchEventListener listener)
	{
		for (TEListener tel : teListeners) {
			if (tel.listener == listener) {
				teListeners.remove(tel);
				if (gotDownListener == tel) {
					gotDownListener = null;
				}
				break;
			}
		}
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		handleTouchEvent(new TouchEvent(screenX, screenArea.height - screenY));
		return true;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		handleTouchEvent(new TouchEvent(screenX, screenArea.height - screenY, TouchType.MOVE));
		return true;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		handleTouchEvent(new TouchEvent(screenX, screenArea.height - screenY, TouchType.UP));
		return true;
	}
	
	@Override
	public boolean keyDown(int keycode) {
		// a=29, z = 54,    r=46, z=34,   x=52
//		if (LOG) Logger.log("keycode : "+keycode);
//
//		IPresenterTurnState t = model.presenterTurnState;
//		switch (keycode) {
//			case 29:
//				t.setAlpha(t.getAlpha()+.05f);
//				break;
//			case 54:
//				t.setAlpha(t.getAlpha()-.05f);
//				break;
//			case 46:
//				t.setRange(t.getRange()+.2f);
//				break;
//			case 34:
//				t.setRange(t.getRange()-.2f);
//				break;
//			case 52:
//				t.setUseBlack(!t.getUseBlack());
//				break;
//			default:
//				break;
//		}
//		
//		if (LOG) Logger.log("alpha : "+t.getAlpha()+"  range: "+t.getRange());
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		return false;
	}

}
