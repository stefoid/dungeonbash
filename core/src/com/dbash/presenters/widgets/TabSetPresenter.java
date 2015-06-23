package com.dbash.presenters.widgets;

import java.util.LinkedList;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.IEventAction;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEvent;
import com.dbash.models.TouchEvent.TouchType;
import com.dbash.models.TouchEventListener;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.SizeCalculator;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.root.GameStatePresenter;
import com.dbash.presenters.tabs.AbilityTab;
import com.dbash.presenters.tabs.EffectTab;
import com.dbash.presenters.tabs.EyeTab;
import com.dbash.presenters.tabs.ItemTab;
import com.dbash.presenters.tabs.MenuTab;
import com.dbash.presenters.tabs.PowerupTab;
import com.dbash.util.EventBus;
import com.dbash.util.L;
import com.dbash.util.Rect;


public class TabSetPresenter implements TouchEventListener{

	protected static final boolean LOG = false;
	
	private enum SwipeState {
		NONE,
		LEFT,
		RIGHT
	}
	
	private enum TabType {
		EYE_TAB,
		ITEM_TAB,
		ABILITY_TAB,
		EFFECT_TAB,
		MENU_TAB,
		POWERUP_TAB
	}

	public static final int powerupIndex = 1;
	
	List<TabPresenter>	tabs = new LinkedList<TabPresenter>();
	private TabPresenter currentTab;
	private TabPresenter swipedTab;
	private TabPresenter uncoveredTab;
	private SwipeState swipeState = SwipeState.NONE; 
	private float swipePosX;
	private float swipeVelocity;
	private Rect bodyArea;
	private float minSwipeAmount;
	private float swipeDist;
	private PresenterDepend model;
	private UIDepend gui;
	private TouchEventProvider touchEventProvider;
	private Rect tabArea;
	
	public void create(PresenterDepend model,  UIDepend gui, TouchEventProvider touchEventProvider, Rect area) {
		this.model = model;
		this.gui = gui;
		this.touchEventProvider = touchEventProvider;

		// The tab keys take 15% of the bottom of the entire area
		tabArea = new Rect(area, 0, 0, SizeCalculator.LIST_AREA_SCALE, 0);
		tabArea.width = area.width / 5;
		
		this.minSwipeAmount = area.width / 20f;//gui.sizeCalculator.MIN_DRAG_PIXELS*2f;  // 4mm
		this.swipeDist = area.width / 48f;
		// the tab pane area takes up the remaining 85%
		bodyArea = new Rect(area, 0, 0, 0, SizeCalculator.TAB_BUTTON_SCALE);
		
		// tabSetPresenter listens in the body area for side-swipes so it can the active tab away.
		touchEventProvider.addTouchEventListener(this, bodyArea, gui.cameraViewPort.viewPort);
		
		// shift each tab across by the tab width
		addTab(TabType.EYE_TAB, 0);
		addTab(TabType.ITEM_TAB, 1);
		addTab(TabType.ABILITY_TAB, 2);
		addTab(TabType.EFFECT_TAB, 3);
		addTab(TabType.MENU_TAB, 4);
		
		swipedTab = null;
		setCurrentTab(MenuTab.class);
		
		setEventListeners();
	}
	

	private void removeTab(Class<? extends TabPresenter> tabType) {
		TabPresenter tab = findTab(tabType);
		if (tab != null) {
			tabs.remove(tab);
			tab.unsetCurrent();
			tab.onDestroy();
		}
	}
	
	private TabPresenter findTab(Class<? extends TabPresenter> tabType) {
		TabPresenter rTab = null;
		for (TabPresenter tab : tabs) {
			if (tab.getClass() == tabType) {
				rTab = tab;
			}
		}
		return rTab;
	}
	
	private void addTab(TabType tabType, int index) {
		TabPresenter tab = null;
		Rect theTabArea = new Rect(tabArea);
		theTabArea.x += (tabArea.width * index);
		
		switch (tabType) {
			case EYE_TAB:
				tab = new EyeTab(model, gui, touchEventProvider, theTabArea, bodyArea);
				break;
			case ITEM_TAB:
				tab = new ItemTab(model, gui, touchEventProvider, theTabArea, bodyArea);
				break;
			case ABILITY_TAB:
				tab = new AbilityTab(model, gui, touchEventProvider, theTabArea, bodyArea);
				break;
			case EFFECT_TAB:
				tab = new EffectTab(model, gui, touchEventProvider, theTabArea, bodyArea);
				break;
			case MENU_TAB:
				tab = new MenuTab(model, gui, touchEventProvider, theTabArea, bodyArea);
				break;
			case POWERUP_TAB:
				tab = new PowerupTab(model, gui, touchEventProvider, theTabArea, bodyArea);
				break;
			default:
				break;
		}
		
		if (tab != null) {
			tabs.add(tab);
			final TabPresenter thisTab = tab;  
			// When this tab is clicked, set itself to current.
			tab.onClick(new IClickListener() {
				public void processClick() {
					setCurrentTab(thisTab);
				}
			});
		}
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
	
	public void setCurrentTab(Class<? extends TabPresenter> tabType) {
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
			swipeVelocity = -swipeDist;
			index++;
			if (index >= size) {
				index = 0;
			}
		} else {
			swipeVelocity = swipeDist;
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
				if (event.dx < -minSwipeAmount) {
					swipeState = SwipeState.LEFT;
					startSwipe();
				}
				
				if (event.dx > minSwipeAmount) {
					swipeState = SwipeState.RIGHT;
					startSwipe();
				}
				
				return true;
			}
		}
	
		return true;	
	}

	private void setEventListeners() {
		EventBus eventBus = EventBus.getDefault();
		
		eventBus.onEvent(GameStatePresenter.POWERUP_START, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("POWERUP_START");
				boolean setTabCurrent = false;
				if (currentTab.getClass() == ItemTab.class) {
					setTabCurrent = true;
				}
				removeTab(ItemTab.class);
				addTab(TabType.POWERUP_TAB, powerupIndex);
				if (setTabCurrent) {
					setCurrentTab(findTab(PowerupTab.class)); // arbitrarily set tab back to ability tab for now.
				}
			}
		});
		
		eventBus.onEvent(GameStatePresenter.POWERUP_REMOVE, this, new IEventAction() {
			@Override
			public void action(Object param) {
				if (LOG) L.log("POWERUP_REMOVE");
				boolean setTabCurrent = false;
				if (currentTab.getClass() == PowerupTab.class) {
					setTabCurrent = true;
				}
				removeTab(PowerupTab.class);
				addTab(TabType.ITEM_TAB, powerupIndex);
				if (setTabCurrent) {
					setCurrentTab(ItemTab.class); // arbitrarily set tab back to ability tab for now.
				}
			}
		});
	}
}
