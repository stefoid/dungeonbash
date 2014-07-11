package com.dbash.presenters.widgets;

import java.util.LinkedList;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.IPresenterTurnState;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEvent;
import com.dbash.models.UIInfoListener;
import com.dbash.models.TouchEvent.TouchType;
import com.dbash.models.TouchEventListener;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.SizeCalculator;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.tabs.AbilityTab;
import com.dbash.presenters.tabs.EffectTab;
import com.dbash.presenters.tabs.EyeTab;
import com.dbash.presenters.tabs.InventoryTab;
import com.dbash.presenters.tabs.MenuTab;
import com.dbash.util.Rect;


public class TabSetPresenter implements TouchEventListener{

	private enum SwipeState {
		NONE,
		LEFT,
		RIGHT
	}
	
	List<TabPresenter>	tabs = new LinkedList<TabPresenter>();
	private TabPresenter currentTab;
	private TabPresenter swipedTab;
	private TabPresenter uncoveredTab;
	private SwipeState swipeState = SwipeState.NONE; 
	private float swipePosX;
	private float swipeVelocity;
	private Rect bodyArea;
	private float swipeAmount;
	
	public void create(PresenterDepend model,  UIDepend gui, TouchEventProvider touchEventProvider, Rect area) {

		// The tab keys take 15% of the bottom of the entire area
		Rect tabArea = new Rect(area, 0, 0, SizeCalculator.LIST_AREA_SCALE, 0);
		tabArea.width = area.width / 5;
		
		this.swipeAmount = gui.sizeCalculator.MIN_DRAG_PIXELS*3f;  // 6mm
		
		// the tab pane area takes up the remaining 85%
		bodyArea = new Rect(area, 0, 0, 0, SizeCalculator.TAB_BUTTON_SCALE);
		
		// tabSetPresenter listens in the body area for side-swipes so it can the active tab away.
		touchEventProvider.addTouchEventListener(this, bodyArea, gui.cameraViewPort.viewPort);
		
		// shift each tab across by the tab width
		configTab(new EyeTab(model, gui, touchEventProvider, tabArea, bodyArea));
		tabArea.x += tabArea.width;
		configTab(new InventoryTab(model, gui, touchEventProvider, tabArea, bodyArea));
		tabArea.x += tabArea.width;
		configTab(new AbilityTab(model, gui, touchEventProvider, tabArea, bodyArea));
		tabArea.x += tabArea.width;
		configTab(new EffectTab(model, gui, touchEventProvider, tabArea, bodyArea));
		tabArea.x += tabArea.width;
		configTab(new MenuTab(model, gui, touchEventProvider, tabArea, bodyArea));
		
		swipedTab = null;
		final IPresenterTurnState turnProcessor = model.presenterTurnState;
		turnProcessor.onChangeToGameInProgress(new UIInfoListener() {
			public void UIInfoChanged() {
				if (turnProcessor.gameInProgress() == false) {
					setTab(MenuTab.class);
				}
			}
		});
		
		if (turnProcessor.gameInProgress() == false) {
			setTab(MenuTab.class);
		} else {
			setTab(AbilityTab.class);
		}
	}
	

	private void configTab(TabPresenter tab) {
		tabs.add(tab);
		final TabPresenter thisTab = tab;  // must be final for benefit of the 'closure'
		// When this tab is clicked, set itself to current.
		tab.onClick(new IClickListener() {
			public void processClick() {
				setCurrentTab(thisTab);
			}
		});
	}

	private void setCurrentTab(TabPresenter tab) {
		if (currentTab != tab) {
			if (currentTab != null) {
				currentTab.unsetCurrent();
			}
			tab.setCurrent();
			currentTab = tab;			
		}
	}
	
	public void draw(SpriteBatch spriteBatch) {
		for (TabPresenter tab : tabs) {
			if (tab != swipedTab) {
				tab.drawTab(spriteBatch, 0, 0);
			}
			
			// draw the swiped tab last
			if (swipedTab != null) {
				if (Math.abs(swipePosX) >= bodyArea.width) {
					swipedTab.setShouldDrawBody(false);
					swipedTab = null;
					swipeState = SwipeState.NONE;
					setCurrentTab(uncoveredTab);
				} else {
					swipedTab.drawTab(spriteBatch, swipePosX, 0);
					swipePosX += swipeVelocity;
				}
			}
		}
	}
	
	public void setTab(Class<? extends TabPresenter> tabType) {
		for (TabPresenter tab : tabs) {
			if (tabType.isInstance(tab)) {
				setCurrentTab(tab);
				return;
			}
		}
	}

	// When a swipe starts, we make one of the underlying tabs current so it will draw
	private void startSwipe()
	{
		swipedTab = currentTab;
		int index = tabs.indexOf(currentTab);
		int size = tabs.size();
		
		if (swipeState == SwipeState.LEFT) {
			swipeVelocity = -7f;
			index++;
			if (index >= size) {
				index = 0;
			}
		} else {
			swipeVelocity = 7f;
			index--;
			if (index < 0) {
				index = size-1;
			}
		}
		
		swipePosX = 0;
		uncoveredTab = tabs.get(index);
		setCurrentTab(uncoveredTab);  // will unset the current (swipe) tab, and tell the uncovered tab to refresh its state
		// but for the animation, we want no tab current, but both tabs drawing.
		uncoveredTab.unsetCurrent();
		currentTab = null;
		swipedTab.setShouldDrawBody(true);
		uncoveredTab.setShouldDrawBody(true);
	}
	
	@Override
	// only interested in side-swipes if not already swipe in progress
	public boolean touchEvent(TouchEvent event) {
		if (swipeState == SwipeState.NONE) {
			if (event.getTouchType() == TouchType.MOVE) {
				if (event.dx < -swipeAmount) {
					swipeState = SwipeState.LEFT;
					startSwipe();
				}
				
				if (event.dx > swipeAmount) {
					swipeState = SwipeState.RIGHT;
					startSwipe();
				}
				
				return true;
			}
		}
	
		return true;	
	}

}
