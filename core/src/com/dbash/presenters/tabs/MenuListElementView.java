package com.dbash.presenters.tabs;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEventProvider;
import com.dbash.models.UIInfoListener;
import com.dbash.platform.Audio;
import com.dbash.platform.ImagePatchView;
import com.dbash.platform.ImageView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.ButtonView;
import com.dbash.presenters.widgets.IClickListener;
import com.dbash.presenters.widgets.IListElement;
import com.dbash.presenters.widgets.ISelectionListener;
import com.dbash.presenters.widgets.SliderView;
import com.dbash.util.L;
import com.dbash.util.Rect;

@SuppressWarnings("unused")

public class MenuListElementView implements IListElement {
	public static final boolean LOG = false && L.DEBUG;
	ISelectionListener selectionListener;
	
	protected UIDepend gui;
	protected int extraElements;
	protected Rect elementArea;
	private ImageView menuBackground;
	private ButtonView startButton;
	public ButtonView helpButton;
	private SliderView fxSlider;
	private SliderView musicSlider;
	boolean drawFlag;
	//private ButtonView tutorialOnButton;
	//private ButtonView tutorialOffButton;
	private Rect bodyArea;
	TouchEventProvider touchEventProvider;
	protected ImagePatchView border;
	PresenterDepend model;
	
	/**
	 * Read the width and expand vertically to the number of elements you require by adding empty ones.
	 */
	public MenuListElementView(PresenterDepend modelPres, UIDepend gui, Rect nominalArea, TouchEventProvider touchEventProvider) {
		this.model = modelPres;
		this.gui = gui;
		this.touchEventProvider = touchEventProvider;
		// record the element area, but expand ourselves veritcally by N element areas as required.
		extraElements = (int) (gui.sizeCalculator.MIN_ELEMENTS-1);
		elementArea = new Rect(nominalArea);
		bodyArea = new Rect(nominalArea);
		bodyArea.height *= (extraElements+1);

		if (LOG) {
			L.log("extra "+extraElements);
			L.log("nominal="+nominalArea.toString());
			L.log("body="+bodyArea.toString());
		}
		
		final Audio audio = gui.audio;
		
		Rect sliderRect1 = new Rect(bodyArea, .65f, .2f, .05f, .4f);
		fxSlider = new SliderView(gui, touchEventProvider, sliderRect1, 2f, "VOLUME_SLIDER_IMAGE", "FX_SLIDER_BUTTON");
		gui.audio.fxVolumeListeners.add(new UIInfoListener() {
			public void UIInfoChanged() {
				fxSlider.setSliderPosition(audio.getFxVolume()*100f);
			}	
		});
		
		fxSlider.onSliderChange (new ISelectionListener() {
			public void processSelection() {
				int volume = (int) (fxSlider.getSliderPosition());
				audio.setFxVolume((float) (volume)/100f);
			}
		});
		
		Rect sliderRect2 = new Rect(bodyArea, .2f, .65f, .05f, .4f);
		musicSlider = new SliderView(gui, touchEventProvider, sliderRect2, 2f, "VOLUME_SLIDER_IMAGE", "MUSIC_SLIDER_BUTTON");
		gui.audio.musicVolumeListeners.add(new UIInfoListener() {
			public void UIInfoChanged() {
				musicSlider.setSliderPosition(audio.getMusicVolume()*100f);
			}	
		});
		
		musicSlider.onSliderChange (new ISelectionListener() {
			public void processSelection() {
				int volume = (int) (musicSlider.getSliderPosition());
				audio.setMusicVolume((float) (volume)/100f);
			}
		});
		
//			Rect tuteModeRect = new Rect(bodyArea, .1f, .55f, .05f, .75f);
//			tutorialOnButton = new ButtonView(gui, touchEventProvider, tuteModeRect, "TUTE_ON_SELECTED_IMAGE", 
//					"TUTE_ON_BUTTON_IMAGE", "TUTE_ON_BUTTON_IMAGE");
//			tutorialOnButton.onClick( new IClickListener() {
//				public void processClick() {
//					PopupPresenter.showAllPopups();
//				}
//			});
	//	
//			tuteModeRect = new Rect(bodyArea, .55f, .1f, .05f, .75f);
//			tutorialOffButton = new ButtonView(gui, touchEventProvider, tuteModeRect, "TUTE_OFF_SELECTED_IMAGE", 
//					"TUTE_OFF_BUTTON_IMAGE", "TUTE_OFF_BUTTON_IMAGE");
//			tutorialOffButton.onClick( new IClickListener() {
//				public void processClick() {
//					PopupPresenter.hideAllPopups();
//				}
//			});
		
		Rect quitRect = new Rect(bodyArea, .6f, .15f, .75f, .05f);
		helpButton = new ButtonView(gui, touchEventProvider, quitRect, "INFO_SELECTED_IMAGE", 
				"INFO_BUTTON_IMAGE", "INFO_BUTTON_IMAGE", Audio.CLICK);
		
		menuBackground = new ImageView(gui, "MENU_BGROUND_IMAGE", bodyArea);
		
		doStartButton();
//		model.presenterTurnState.onChangeToGameInProgress(new UIInfoListener() {
//			public void UIInfoChanged() {
//				doStartButton();
//			}
//		});
	}
		
	private void doStartButton() 
	{
		Rect startRect = new Rect(bodyArea, .15f, .6f, .75f, .05f);
		
		if (model.presenterTurnState.gameInProgress()) {
			startButton = new ButtonView(gui, touchEventProvider, startRect, "RESTART_SELECTED_IMAGE", 
					"RESTART_BUTTON_IMAGE", "RESTART_BUTTON_IMAGE", Audio.CLICK);
		} else {
			startButton = new ButtonView(gui, touchEventProvider, startRect, "START_SELECTED_IMAGE", 
					"START_BUTTON_IMAGE", "START_BUTTON_IMAGE", Audio.CLICK);
		}
		
		startButton.onClick( new IClickListener() {
			public void processClick() {
				model.presenterTurnState.mainMenuStartGameSelected();
			}
		});
	}

	@Override
	public void gotSelection(float x, float y) {
	}

	@Override
	public void onSelection(ISelectionListener selectionListener) {
	}

	@Override
	public void addToList(ArrayList<IListElement> list) {
		for (int i=0; i<extraElements; i++) {
			float parentYOffset = (i - extraElements)*elementArea.height;
			IListElement.EmptyListElement empty = new IListElement.EmptyListElement(gui, null, elementArea, this, parentYOffset);
			empty.addToList((list));
		}
		list.add(this);
	}

	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		if (drawFlag == false) {
			drawFlag = true;
			menuBackground.draw(spriteBatch, x, y);
			startButton.draw(spriteBatch, x, y);
			helpButton.draw(spriteBatch, x, y);
		//		tutorialOnButton.draw(spriteBatch, x, y);
		//		tutorialOffButton.draw(spriteBatch, x, y);
			fxSlider.draw(spriteBatch, x, y);
			musicSlider.draw(spriteBatch, x, y);
		}
	}
	
	public void activate() {
		helpButton.setEnabledAndTouch(true, true);
		startButton.setEnabledAndTouch(true, true);
		fxSlider.activate();
		musicSlider.activate();
	}

	public void deactivate() {
		fxSlider.deactivate();
		musicSlider.deactivate();
		helpButton.setEnabledAndTouch(false, false);
		startButton.setEnabledAndTouch(false, false);
	}
	
	@Override
	public void clearDrawFlag() {
		drawFlag = false;
	}

	@Override
	public void setAnimating() {
	}
}

